import requests
import json
import urllib.parse

# Login
login_resp = requests.post("http://localhost:8080/api/v1/auth/login", 
    json={"mobile": "13800000001", "verify_code": "123456"},
    headers={"X-Request-Id": "test-flow-login"})
print("1. Login Response:")
print(json.dumps(login_resp.json(), indent=2))

if login_resp.json()["code"] == 0:
    data = login_resp.json()["data"]
    
    # Get profile
    profile_resp = requests.get("http://localhost:8080/api/v1/tenant/profile",
        headers={
            "X-Tenant-Id": str(data["tenant_id"]),
            "Authorization": f"Bearer {data['access_token']}",
            "X-Request-Id": "test-flow-profile"
        })
    print("\n2. Profile Response:")
    print(json.dumps(profile_resp.json(), indent=2))
    
    # Test if user can access dashboard (frontend would check this)
    role_code = data['role_code']
    dashboard_roles = ["merchant_owner", "merchant_operator", "merchant_editor", "merchant_viewer"]
    
    # Mapping logic (from frontend)
    mapped_role = role_code if role_code != 'boss' else 'merchant_owner'
    
    print(f"\n3. Permission Check:")
    print(f"   Backend role_code: {role_code}")
    print(f"   Mapped role: {mapped_role}")
    print(f"   Can access dashboard: {mapped_role in dashboard_roles}")
    print(f"   Can access settings: {'merchant_owner' == mapped_role}")
