import boto3
import json
import urllib.request
import urllib.error

def check_groq_key(secret_id, key_name):
    client = boto3.client('secretsmanager', region_name='ap-south-1')
    try:
        response = client.get_secret_value(SecretId=secret_id)
        secret_string = response.get('SecretString')
        if not secret_string:
            print(f"Secret {key_name} is empty or binary.")
            return

        try:
            secrets = json.loads(secret_string)
            api_key = secrets.get(key_name)
        except json.JSONDecodeError:
            # Maybe it's a raw string
            api_key = secret_string
            if "{" in api_key: 
                print(f"Secret {key_name} looks like JSON but failed to parse.")
                return

        if not api_key:
            print(f"Key {key_name} not found in secret {secret_id}")
            return

        req = urllib.request.Request(
            "https://api.groq.com/openai/v1/models",
            headers={"Authorization": f"Bearer {api_key}"}
        )
        try:
            with urllib.request.urlopen(req) as res:
                if res.status == 200:
                    print(f"GROQ KEY STATUS FOR {secret_id} ({key_name}): VALID")
                else:
                    print(f"GROQ KEY STATUS FOR {secret_id} ({key_name}): UNEXPECTED STATUS {res.status}")
        except urllib.error.HTTPError as e:
            if e.code in [401, 403]:
                print(f"GROQ KEY STATUS FOR {secret_id} ({key_name}): INVALID")
            else:
                print(f"GROQ KEY STATUS FOR {secret_id} ({key_name}): HTTP ERROR {e.code}")
    except Exception as e:
        print(f"Failed to check {secret_id}: {e}")

check_groq_key('travelbilling/groq-api-key', 'GROQ_API_KEY')
check_groq_key('travelbilling/prod', 'GROQ_API_KEY_FALLBACK')
