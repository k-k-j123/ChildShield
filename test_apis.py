import urllib.request
import json
import sys

BASE_URL = "http://localhost:8080"

tests = [
    ("Admin Login (/login)", "POST", "/login", {"username": "admin", "password": "admin123"}, 200),
    ("Get Dashboard (/dashboard)", "GET", "/dashboard", None, 200),
    ("List Villages (/api/villages)", "GET", "/api/villages", None, 200),
    ("List Health Workers (/api/admin/health-workers)", "GET", "/api/admin/health-workers", None, 200),
    ("List Guardians (/api/health-worker/guardians)", "GET", "/api/health-worker/guardians", None, 200),
    ("List Children (/api/health-worker/children)", "GET", "/api/health-worker/children", None, 200),
    ("List Vaccinations (/api/vaccinations)", "GET", "/api/vaccinations", None, 200),
    ("List Maternal Visits (/api/maternal-visits)", "GET", "/api/maternal-visits", None, 200),
    ("List Notifications (/api/notifications)", "GET", "/api/notifications", None, 200),
    ("Get Reports (/reports)", "GET", "/reports", None, 200),
    ("Get Profile (/profile)", "GET", "/profile", None, 200),
]

passed = 0
failed = 0

print("=== Starting ChildShield REST API Integration Tests ===")
for name, method, path, data, expected_status in tests:
    url = BASE_URL + path
    try:
        req_data = json.dumps(data).encode("utf-8") if data else None
        headers = {"Content-Type": "application/json"} if data else {}
        req = urllib.request.Request(url, data=req_data, headers=headers, method=method)
        with urllib.request.urlopen(req, timeout=5) as resp:
            status = resp.status
            if status == expected_status:
                print(f"[PASS] {name} -> Status {status}")
                passed += 1
            else:
                print(f"[FAIL] {name} -> Expected {expected_status}, got {status}")
                failed += 1
    except Exception as e:
        print(f"[FAIL] {name} -> Error: {e}")
        failed += 1

print("\n=== Test Report Summary ===")
print(f"Total: {passed + failed} | Passed: {passed} | Failed: {failed}")
sys.exit(0 if failed == 0 else 1)
