import requests
import json

# Query database directly
import pymysql
conn = pymysql.connect(host='127.0.0.1', port=3306, user='root', password='123456', database='puyuan_ai_mvp')
cursor = conn.cursor()
cursor.execute("SELECT id, mobile, nickname, role_code, status FROM user_account WHERE mobile='13800000001'")
print("Database query result:")
print(cursor.fetchall())
cursor.close()
conn.close()

# Login
login_resp = requests.post("http://localhost:8080/api/v1/auth/login", 
    json={"mobile": "13800000001", "verify_code": "123456"},
    headers={"X-Request-Id": "test-debug-001"})
print("\nLogin Response:", json.dumps(login_resp.json(), indent=2))
