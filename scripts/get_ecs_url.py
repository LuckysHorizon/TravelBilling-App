import boto3

ecs = boto3.client('ecs', region_name='ap-south-1')
ec2 = boto3.client('ec2', region_name='ap-south-1')
elbv2 = boto3.client('elbv2', region_name='ap-south-1')

# 1. Check Load Balancers
print("--- Load Balancers ---")
lbs = elbv2.describe_load_balancers()
for lb in lbs.get('LoadBalancers', []):
    print(f"Name: {lb['LoadBalancerName']}")
    print(f"DNSName: {lb['DNSName']}")

# 2. Check ECS Tasks for Public IPs
print("\n--- ECS Tasks ---")
tasks_response = ecs.list_tasks(cluster='travelbilling-cluster', serviceName='travelbilling-service')
tasks = tasks_response.get('taskArns', [])
if tasks:
    task_details = ecs.describe_tasks(cluster='travelbilling-cluster', tasks=tasks)
    for task in task_details.get('tasks', []):
        for attachment in task.get('attachments', []):
            if attachment['type'] == 'ElasticNetworkInterface':
                eni_id = next((detail['value'] for detail in attachment['details'] if detail['name'] == 'networkInterfaceId'), None)
                if eni_id:
                    eni_details = ec2.describe_network_interfaces(NetworkInterfaceIds=[eni_id])
                    public_ip = eni_details['NetworkInterfaces'][0].get('Association', {}).get('PublicIp')
                    print(f"Task ENI: {eni_id}, Public IP: {public_ip}")
else:
    print("No tasks running.")
