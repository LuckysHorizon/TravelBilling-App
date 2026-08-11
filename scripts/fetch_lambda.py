import boto3
import urllib.request
import zipfile
import io
import os

client = boto3.client('lambda')
try:
    response = client.get_function(FunctionName='TravelBillingWake')
    code_url = response['Code']['Location']
    
    # Download the zip file
    req = urllib.request.Request(code_url)
    with urllib.request.urlopen(req) as resp:
        zip_data = resp.read()
        
    with zipfile.ZipFile(io.BytesIO(zip_data)) as z:
        for filename in z.namelist():
            if filename.endswith('.py'):
                print(f"--- {filename} ---")
                print(z.read(filename).decode('utf-8'))
                
except Exception as e:
    print(f"Error fetching lambda: {e}")
