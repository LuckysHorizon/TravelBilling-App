import boto3
import json
import zipfile
import io
import time

def deploy():
    print("Starting AWS Deployment...")
    iam = boto3.client('iam')
    lam = boto3.client('lambda')
    dynamodb = boto3.client('dynamodb')
    events = boto3.client('events')
    sts = boto3.client('sts')
    
    account_id = sts.get_caller_identity()['Account']
    region = 'ap-south-1' # Hardcoded from existing lambda fetching
    
    # 1. Create DynamoDB Table
    print("Checking DynamoDB Table...")
    try:
        dynamodb.describe_table(TableName='TravelBillingActivity')
        print("Table already exists.")
    except dynamodb.exceptions.ResourceNotFoundException:
        print("Creating DynamoDB Table 'TravelBillingActivity'...")
        dynamodb.create_table(
            TableName='TravelBillingActivity',
            KeySchema=[{'AttributeName': 'id', 'KeyType': 'HASH'}],
            AttributeDefinitions=[{'AttributeName': 'id', 'AttributeType': 'S'}],
            BillingMode='PAY_PER_REQUEST'
        )
        print("Waiting for table to become ACTIVE...")
        waiter = dynamodb.get_waiter('table_exists')
        waiter.wait(TableName='TravelBillingActivity')
        print("Table is ACTIVE.")
        
    # 2. Update TravelBillingWake Role with DynamoDB Permissions
    # Fetch existing role ARN from existing Lambda
    wake_config = lam.get_function_configuration(FunctionName='TravelBillingWake')
    wake_role_arn = wake_config['Role']
    role_name = wake_role_arn.split('/')[-1]
    
    # We will attach an inline policy to this role for DynamoDB
    wake_policy = {
        "Version": "2012-10-17",
        "Statement": [
            {
                "Effect": "Allow",
                "Action": [
                    "dynamodb:PutItem",
                    "dynamodb:UpdateItem",
                    "dynamodb:GetItem"
                ],
                "Resource": f"arn:aws:dynamodb:{region}:{account_id}:table/TravelBillingActivity"
            }
        ]
    }
    
    print(f"Updating IAM Role {role_name} for TravelBillingWake with DynamoDB permissions...")
    iam.put_role_policy(
        RoleName=role_name,
        PolicyName='TravelBillingWakeDynamoDBPolicy',
        PolicyDocument=json.dumps(wake_policy)
    )
    
    # 3. Create TravelBillingAutoShutdown Role
    shutdown_role_name = 'TravelBillingAutoShutdownRole'
    try:
        iam.get_role(RoleName=shutdown_role_name)
        print("Shutdown Role already exists.")
    except iam.exceptions.NoSuchEntityException:
        print("Creating Shutdown IAM Role...")
        assume_role_policy = {
            "Version": "2012-10-17",
            "Statement": [
                {
                    "Effect": "Allow",
                    "Principal": {"Service": "lambda.amazonaws.com"},
                    "Action": "sts:AssumeRole"
                }
            ]
        }
        iam.create_role(
            RoleName=shutdown_role_name,
            AssumeRolePolicyDocument=json.dumps(assume_role_policy)
        )
        iam.attach_role_policy(
            RoleName=shutdown_role_name,
            PolicyArn='arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole'
        )
        # Inline policy for DynamoDB and ECS
        shutdown_policy = {
            "Version": "2012-10-17",
            "Statement": [
                {
                    "Effect": "Allow",
                    "Action": ["dynamodb:GetItem"],
                    "Resource": f"arn:aws:dynamodb:{region}:{account_id}:table/TravelBillingActivity"
                },
                {
                    "Effect": "Allow",
                    "Action": ["ecs:UpdateService"],
                    "Resource": f"arn:aws:ecs:{region}:{account_id}:service/travelbilling-cluster/travelbilling-service"
                }
            ]
        }
        iam.put_role_policy(
            RoleName=shutdown_role_name,
            PolicyName='TravelBillingAutoShutdownPolicy',
            PolicyDocument=json.dumps(shutdown_policy)
        )
        print("Waiting for IAM role to propagate...")
        time.sleep(10) # wait for role to propagate
        
    shutdown_role_arn = iam.get_role(RoleName=shutdown_role_name)['Role']['Arn']

    # 4. Prepare Wake Lambda Code
    wake_code = """import json
import boto3
import os
import time
from datetime import datetime

ecs = boto3.client("ecs", region_name="ap-south-1")
dynamodb = boto3.client("dynamodb", region_name="ap-south-1")

CLUSTER = os.environ.get("ECS_CLUSTER_NAME", "travelbilling-cluster")
SERVICE = os.environ.get("ECS_SERVICE_NAME", "travelbilling-service")
TABLE_NAME = os.environ.get("DYNAMODB_TABLE_NAME", "TravelBillingActivity")

def lambda_handler(event, context):
    try:
        body = {}
        if event.get("body"):
            body = json.loads(event["body"])
            
        action = body.get("action", "wake")
        
        headers = {
            "Content-Type": "application/json",
            "Access-Control-Allow-Origin": "*",
            "Cache-Control": "no-store"
        }
        
        if action == "wake":
            # Wake logic
            response = ecs.update_service(
                cluster=CLUSTER,
                service=SERVICE,
                desiredCount=1
            )
            
            # Also record activity
            current_time = int(time.time())
            dynamodb.put_item(
                TableName=TABLE_NAME,
                Item={
                    "id": {"S": "backend"},
                    "last_active": {"N": str(current_time)},
                    "timestamp": {"S": datetime.utcnow().isoformat()}
                }
            )
            
            service = response["service"]
            return {
                "statusCode": 200,
                "headers": headers,
                "body": json.dumps({
                    "status": "starting",
                    "desiredCount": service["desiredCount"],
                    "runningCount": service["runningCount"]
                })
            }
            
        elif action == "heartbeat":
            # Record latest activity timestamp
            current_time = int(time.time())
            dynamodb.put_item(
                TableName=TABLE_NAME,
                Item={
                    "id": {"S": "backend"},
                    "last_active": {"N": str(current_time)},
                    "timestamp": {"S": datetime.utcnow().isoformat()}
                }
            )
            return {
                "statusCode": 200,
                "headers": headers,
                "body": json.dumps({"status": "heartbeat_recorded"})
            }
            
        else:
            return {
                "statusCode": 400,
                "headers": headers,
                "body": json.dumps({"error": "Invalid action"})
            }
            
    except Exception as e:
        print(f"Error: {str(e)}")
        # Safe fallback, do not crash the caller
        return {
            "statusCode": 500,
            "headers": {
                "Content-Type": "application/json",
                "Access-Control-Allow-Origin": "*"
            },
            "body": json.dumps({"error": "Internal server error"})
        }
"""
    # Package Wake Code
    wake_zip = io.BytesIO()
    with zipfile.ZipFile(wake_zip, 'w') as z:
        z.writestr('wake_lambda.py', wake_code)
    wake_zip.seek(0)
    
    print("Updating TravelBillingWake lambda code and env vars...")
    lam.update_function_code(
        FunctionName='TravelBillingWake',
        ZipFile=wake_zip.read()
    )
    
    print("Waiting for TravelBillingWake update to complete...")
    while True:
        status = lam.get_function(FunctionName='TravelBillingWake')['Configuration']['LastUpdateStatus']
        if status == 'Successful':
            break
        elif status == 'Failed':
            raise Exception("Lambda update failed")
        time.sleep(2)
    lam.update_function_configuration(
        FunctionName='TravelBillingWake',
        Handler='wake_lambda.lambda_handler',
        Environment={
            'Variables': {
                'ECS_CLUSTER_NAME': 'travelbilling-cluster',
                'ECS_SERVICE_NAME': 'travelbilling-service',
                'DYNAMODB_TABLE_NAME': 'TravelBillingActivity'
            }
        }
    )
    
    # 5. Prepare Shutdown Lambda Code
    shutdown_code = """import json
import boto3
import os
import time
from datetime import datetime

ecs = boto3.client("ecs", region_name="ap-south-1")
dynamodb = boto3.client("dynamodb", region_name="ap-south-1")

CLUSTER = os.environ.get("ECS_CLUSTER_NAME", "travelbilling-cluster")
SERVICE = os.environ.get("ECS_SERVICE_NAME", "travelbilling-service")
TABLE_NAME = os.environ.get("DYNAMODB_TABLE_NAME", "TravelBillingActivity")

def lambda_handler(event, context):
    print("Running TravelBillingAutoShutdown...")
    try:
        response = dynamodb.get_item(
            TableName=TABLE_NAME,
            Key={"id": {"S": "backend"}}
        )
        
        if "Item" not in response:
            print("No activity record found. Doing nothing.")
            return
            
        last_active = int(response["Item"]["last_active"]["N"])
        current_time = int(time.time())
        diff_minutes = (current_time - last_active) / 60.0
        
        print(f"Last active: {diff_minutes:.2f} minutes ago.")
        
        if diff_minutes >= 15:
            print("Inactive for >= 15 minutes. Shutting down ECS service...")
            # Uncomment the below block when we want to fully activate shutdown
            # ecs.update_service(
            #    cluster=CLUSTER,
            #    service=SERVICE,
            #    desiredCount=0
            # )
            print("ECS service shutdown skipped for safety until manually activated.")
        else:
            print("Service is active. No action taken.")
            
    except Exception as e:
        print(f"Error during shutdown evaluation: {e}")
"""
    shutdown_zip = io.BytesIO()
    with zipfile.ZipFile(shutdown_zip, 'w') as z:
        z.writestr('shutdown_lambda.py', shutdown_code)
    shutdown_zip.seek(0)
    
    try:
        lam.get_function(FunctionName='TravelBillingAutoShutdown')
        print("TravelBillingAutoShutdown already exists, updating code...")
        lam.update_function_code(
            FunctionName='TravelBillingAutoShutdown',
            ZipFile=shutdown_zip.read()
        )
        print("Waiting for TravelBillingAutoShutdown update to complete...")
        while True:
            status = lam.get_function(FunctionName='TravelBillingAutoShutdown')['Configuration']['LastUpdateStatus']
            if status == 'Successful':
                break
            elif status == 'Failed':
                raise Exception("Lambda update failed")
            time.sleep(2)
        lam.update_function_configuration(
            FunctionName='TravelBillingAutoShutdown',
            Handler='shutdown_lambda.lambda_handler',
            Environment={
                'Variables': {
                    'ECS_CLUSTER_NAME': 'travelbilling-cluster',
                    'ECS_SERVICE_NAME': 'travelbilling-service',
                    'DYNAMODB_TABLE_NAME': 'TravelBillingActivity'
                }
            }
        )
    except lam.exceptions.ResourceNotFoundException:
        print("Creating TravelBillingAutoShutdown Lambda...")
        lam.create_function(
            FunctionName='TravelBillingAutoShutdown',
            Runtime='python3.11',
            Role=shutdown_role_arn,
            Handler='shutdown_lambda.lambda_handler',
            Code={'ZipFile': shutdown_zip.read()},
            Timeout=15,
            Environment={
                'Variables': {
                    'ECS_CLUSTER_NAME': 'travelbilling-cluster',
                    'ECS_SERVICE_NAME': 'travelbilling-service',
                    'DYNAMODB_TABLE_NAME': 'TravelBillingActivity'
                }
            }
        )

    # 6. Configure EventBridge
    print("Configuring EventBridge schedule...")
    rule_name = 'TravelBillingShutdownSchedule'
    events.put_rule(
        Name=rule_name,
        ScheduleExpression='rate(5 minutes)',
        State='ENABLED'
    )
    
    shutdown_lambda_arn = lam.get_function(FunctionName='TravelBillingAutoShutdown')['Configuration']['FunctionArn']
    
    events.put_targets(
        Rule=rule_name,
        Targets=[{
            'Id': 'TravelBillingShutdownTarget',
            'Arn': shutdown_lambda_arn
        }]
    )
    
    # Grant EventBridge permission to invoke the Lambda
    try:
        lam.add_permission(
            FunctionName='TravelBillingAutoShutdown',
            StatementId='AllowEventBridgeInvoke',
            Action='lambda:InvokeFunction',
            Principal='events.amazonaws.com',
            SourceArn=f"arn:aws:events:{region}:{account_id}:rule/{rule_name}"
        )
    except lam.exceptions.ResourceConflictException:
        print("EventBridge permission already exists.")

    print("AWS Deployment Complete.")

if __name__ == '__main__':
    deploy()
