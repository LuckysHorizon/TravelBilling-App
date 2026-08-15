import urllib.request
import urllib.error
import ssl

def check_cors(url, origin):
    ctx = ssl.create_default_context()
    ctx.check_hostname = False
    ctx.verify_mode = ssl.CERT_NONE

    print(f"\n--- Testing Preflight OPTIONS to {url} with custom headers ---")
    req = urllib.request.Request(url, method="OPTIONS")
    req.add_header("Origin", origin)
    req.add_header("Access-Control-Request-Method", "POST")
    req.add_header("Access-Control-Request-Headers", "authorization, content-type, x-vercel-id")
    
    try:
        with urllib.request.urlopen(req, context=ctx, timeout=10) as response:
            print(f"Status: {response.status}")
            print(f"Access-Control-Allow-Origin: {response.headers.get('Access-Control-Allow-Origin')}")
            print(f"Access-Control-Allow-Headers: {response.headers.get('Access-Control-Allow-Headers')}")
    except urllib.error.HTTPError as e:
        print(f"Preflight Failed with Status: {e.code}")
    except Exception as e:
        print(f"Error: {e}")

url = "https://travelbilling-alb-512368256.ap-south-1.elb.amazonaws.com/api/auth/login"
origin = "https://travelbilling.ramnetsolutions.online"

check_cors(url, origin)
