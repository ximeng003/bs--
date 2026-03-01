
import requests
import json

url = "http://localhost:18080/api/auth/login"
headers = {"Content-Type": "application/json"}
data = {
    "username": "admin",
    "password": "123456"
}

try:
    response = requests.post(url, headers=headers, json=data)
    print(f"Status Code: {response.status_code}")
    print(f"Response Body: {response.text}")
except Exception as e:
    print(f"Error: {e}")
