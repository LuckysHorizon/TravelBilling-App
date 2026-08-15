import urllib.request
import urllib.error

def check_url(url):
    try:
        req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
        with urllib.request.urlopen(req) as response:
            content = response.read().decode('utf-8')
            print(f"URL: {url} -> Status: {response.status}")
            if "<div id=\"root\"></div>" in content or "Vite" in content or "React" in content:
                print("  -> Appears to be the SPA index.html")
            else:
                print("  -> Content does not look like the React SPA.")
    except urllib.error.HTTPError as e:
        print(f"URL: {url} -> Status: {e.code}")
    except Exception as e:
        print(f"URL: {url} -> Error: {e}")

base_url = "https://travelbilling.ramnetsolutions.online"
paths = ["/", "/login", "/dashboard", "/settings", "/assets/index.js"] # We don't know exact asset path but let's see

for path in paths:
    check_url(base_url + path)
