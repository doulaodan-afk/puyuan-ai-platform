import requests
import json

# Login
login_resp = requests.post("http://localhost:8080/api/v1/auth/login", 
    json={"mobile": "13800000001", "verify_code": "123456"},
    headers={"X-Request-Id": "test-merchant-login"})
print("Login Response:")
print(json.dumps(login_resp.json(), indent=2))

if login_resp.json()["code"] == 0:
    data = login_resp.json()["data"]
    
    # Get profile
    profile_resp = requests.get("http://localhost:8080/api/v1/tenant/profile",
        headers={
            "X-Tenant-Id": str(data["tenant_id"]),
            "Authorization": f"Bearer {data['access_token']}",
            "X-Request-Id": "test-merchant-profile"
        })
    print("\nProfile Response:")
    print(json.dumps(profile_resp.json(), indent=2))
    print(f"\nExpected role: merchant_owner")
    print(f"Actual role: {data['role_code']}")
    print(f"Should map boss to merchant_owner")
