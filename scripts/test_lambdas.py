import boto3
import json

lam = boto3.client('lambda')

print("1. Invoking TravelBillingWake (action: wake)")
res_wake = lam.invoke(
    FunctionName='TravelBillingWake',
    Payload=json.dumps({"body": json.dumps({"action": "wake"})})
)
print(res_wake['Payload'].read().decode('utf-8'))

print("\n2. Invoking TravelBillingWake (action: heartbeat)")
res_hb = lam.invoke(
    FunctionName='TravelBillingWake',
    Payload=json.dumps({"body": json.dumps({"action": "heartbeat"})})
)
print(res_hb['Payload'].read().decode('utf-8'))

print("\n3. Invoking TravelBillingAutoShutdown")
res_shut = lam.invoke(
    FunctionName='TravelBillingAutoShutdown',
    Payload=json.dumps({})
)
print("Shutdown triggered. Check CloudWatch logs for output.")
