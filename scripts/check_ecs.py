import boto3

ecs = boto3.client('ecs', region_name='ap-south-1')

# Get the service
response = ecs.describe_services(
    cluster='travelbilling-cluster',
    services=['travelbilling-service']
)

task_def_arn = response['services'][0]['taskDefinition']

# Get the task definition
task_def = ecs.describe_task_definition(taskDefinition=task_def_arn)

# Print environment variables
for container in task_def['taskDefinition']['containerDefinitions']:
    print(f"Container: {container['name']}")
    print("Environment Variables:")
    for env in container.get('environment', []):
        print(f"  {env['name']}: {env['value']}")
    print("Secrets:")
    for secret in container.get('secrets', []):
        print(f"  {secret['name']}: {secret['valueFrom']}")
