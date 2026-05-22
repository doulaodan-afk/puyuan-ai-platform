import requests
import json

# Login
login_resp = requests.post("http://localhost:8080/api/v1/auth/login", 
    json={"mobile": "13800000001", "verify_code": "123456"},
    headers={"X-Request-Id": "test-001"})
print("Login Response:", json.dumps(login_resp.json(), indent=2))

# Get profile
data = login_resp.json()["data"]
profile_resp = requests.get("http://localhost:8080/api/v1/tenant/profile",
    headers={
        "X-Tenant-Id": str(data["tenant_id"]),
        "Authorization": f"Bearer {data['access_token']}",
        "X-Request-Id": "test-002"
    })
print("Profile Response:", json.dumps(profile_resp.json(), indent=2))
