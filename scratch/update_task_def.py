import boto3
import json

REGION = 'ap-south-1'
CLUSTER_NAME = 'travelbilling-cluster'
SERVICE_NAME = 'travelbilling-service'

ecs = boto3.client('ecs', region_name=REGION)

print("Fetching active task definition...")
response = ecs.describe_services(cluster=CLUSTER_NAME, services=[SERVICE_NAME])
task_def_arn = response['services'][0]['taskDefinition']

print(f"Current task definition: {task_def_arn}")
td_resp = ecs.describe_task_definition(taskDefinition=task_def_arn)
td = td_resp['taskDefinition']

print("Updating pdf-extractor image...")
for container in td['containerDefinitions']:
    if container['name'] == 'pdf-extractor':
        # Replace the sha256 or any tag with :latest
        base_image = container['image'].split('@')[0].split(':')[0]
        container['image'] = f"{base_image}:latest"
        print(f"New image: {container['image']}")

print("Registering new task definition...")
# Remove keys that cannot be passed to register_task_definition
keys_to_remove = ['taskDefinitionArn', 'revision', 'status', 'requiresAttributes', 'compatibilities', 'registeredAt', 'registeredBy']
for key in keys_to_remove:
    if key in td:
        del td[key]

new_td_resp = ecs.register_task_definition(**td)
new_task_def_arn = new_td_resp['taskDefinition']['taskDefinitionArn']
print(f"New task definition registered: {new_task_def_arn}")

print("Updating service...")
update_resp = ecs.update_service(
    cluster=CLUSTER_NAME,
    service=SERVICE_NAME,
    taskDefinition=new_task_def_arn,
    forceNewDeployment=True
)
print("Service updated successfully.")
