import boto3

REGION = 'ap-south-1'
elbv2 = boto3.client('elbv2', region_name=REGION)
lbs = elbv2.describe_load_balancers()

for lb in lbs['LoadBalancers']:
    if 'travelbilling' in lb['LoadBalancerName'].lower():
        print(f"Updating ALB: {lb['LoadBalancerName']}")
        response = elbv2.modify_load_balancer_attributes(
            LoadBalancerArn=lb['LoadBalancerArn'],
            Attributes=[
                {
                    'Key': 'idle_timeout.timeout_seconds',
                    'Value': '180'
                }
            ]
        )
        print(f"Success: {response}")
