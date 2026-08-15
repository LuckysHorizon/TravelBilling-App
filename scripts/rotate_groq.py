import boto3
import json
import os

client = boto3.client('secretsmanager', region_name='ap-south-1')
ecs = boto3.client('ecs', region_name='ap-south-1')

# 1. Backup old keys
print("Backing up old keys to local variables...")
old_groq = client.get_secret_value(SecretId='travelbilling/groq-api-key')['SecretString']
old_prod = client.get_secret_value(SecretId='travelbilling/prod')['SecretString']

# 2. Get valid key from environment
valid_key = os.environ.get('GROQ_API_KEY')
if not valid_key:
    raise ValueError("GROQ_API_KEY not found in local environment")

# 3. Update travelbilling/groq-api-key (Used by pdf-extractor container)
print("Updating travelbilling/groq-api-key...")
try:
    old_groq_json = json.loads(old_groq)
    old_groq_json['GROQ_API_KEY'] = valid_key
    new_groq_str = json.dumps(old_groq_json)
except:
    new_groq_str = valid_key

res1 = client.put_secret_value(SecretId='travelbilling/groq-api-key', SecretString=new_groq_str)
print(f"travelbilling/groq-api-key VersionId: {res1['VersionId']}")

# 4. Update travelbilling/prod (Used by agent-llm container)
print("Updating travelbilling/prod...")
try:
    old_prod_json = json.loads(old_prod)
    old_prod_json['GROQ_API_KEY_FALLBACK'] = valid_key
    new_prod_str = json.dumps(old_prod_json)
except:
    new_prod_str = valid_key

res2 = client.put_secret_value(SecretId='travelbilling/prod', SecretString=new_prod_str)
print(f"travelbilling/prod VersionId: {res2['VersionId']}")

# 5. Force ECS Deployment
print("Forcing new ECS deployment for travelbilling-service...")
deploy_res = ecs.update_service(
    cluster='travelbilling-cluster',
    service='travelbilling-service',
    forceNewDeployment=True
)
print("Deployment triggered successfully.")
