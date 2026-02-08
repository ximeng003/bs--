import sys
import json
import time
from typing import Any, Dict, Optional

def success(result: Dict[str, Any]) -> Dict[str, Any]:
    return {
        "status": "success",
        **result
    }

def failed(message: str, result: Optional[Dict[str, Any]] = None) -> Dict[str, Any]:
    base = {
        "status": "failed",
        "error": message
    }
    if result:
        base.update(result)
    return base

def resolve_path(data: Any, path: str) -> Any:
    if not path:
        return None
    if path.startswith("$."):
        path = path[2:]
    keys = [p for p in path.split(".") if p]
    cur = data
    for k in keys:
        if isinstance(cur, dict) and k in cur:
            cur = cur[k]
        else:
            return None
    return cur

def run_api(content: Dict[str, Any]) -> Dict[str, Any]:
    import requests
    method = (content.get("method") or "GET").upper()
    url = content.get("url")
    headers = {}
    for h in content.get("headers") or []:
        if h.get("active", True) and h.get("key"):
            headers[h["key"]] = h.get("value")
    params = {}
    for p in content.get("params") or []:
        if p.get("active", True) and p.get("key"):
            params[p["key"]] = p.get("value")
    body = content.get("body") or ""

    start = time.time()
    resp = requests.request(method, url, headers=headers, params=params, data=body)
    duration = int((time.time() - start) * 1000)

    result = {
        "durationMs": duration,
        "response": {
            "statusCode": resp.status_code,
            "headers": dict(resp.headers),
            "body": resp.text
        },
        "logs": f"{method} {url} -> {resp.status_code} ({duration}ms)"
    }

    assertions = content.get("assertions") or []
    for a in assertions:
        if not a.get("active", True):
            continue
        t = a.get("type")
        if t == "status":
            expected = int(a.get("value"))
            if resp.status_code != expected:
                return failed(f"status assert failed: expected {expected} got {resp.status_code}", result)
        elif t == "json":
            try:
                body_json = resp.json()
                actual = resolve_path(body_json, a.get("path") or "")
                if str(actual) != str(a.get("value")):
                    return failed(f"json assert failed at {a.get('path')}", result)
            except Exception as e:
                return failed(f"json assert error: {e}", result)
        elif t == "time":
            max_ms = int(a.get("value"))
            if duration > max_ms:
                return failed(f"time assert failed: {duration}ms > {max_ms}ms", result)
    return success(result)

def run_web(content: Dict[str, Any]) -> Dict[str, Any]:
    from selenium import webdriver
    from selenium.webdriver.chrome.options import Options
    ops = Options()
    ops.add_argument("--headless=new")
    driver = webdriver.Chrome(options=ops)
    start = time.time()
    logs = []
    try:
        for step in content.get("script", "").splitlines():
            s = step.strip()
            if not s:
                continue
            if s.startswith("打开URL:"):
                url = s.split(":", 1)[1].strip()
                driver.get(url)
                logs.append(f"open {url}")
            elif s.startswith("刷新页面"):
                driver.refresh()
                logs.append("refresh")
        duration = int((time.time() - start) * 1000)
        result = {"durationMs": duration, "logs": "\n".join(logs)}
        return success(result)
    except Exception as e:
        duration = int((time.time() - start) * 1000)
        return failed(str(e), {"durationMs": duration, "logs": "\n".join(logs)})
    finally:
        driver.quit()

def run_app(content: Dict[str, Any]) -> Dict[str, Any]:
    from appium import webdriver
    from appium.webdriver.common.appiumby import AppiumBy
    from selenium.webdriver.support.ui import WebDriverWait
    from selenium.webdriver.support import expected_conditions as EC

    def build_driver(appium_conf: Dict[str, Any]):
        remote_url = appium_conf.get("remoteUrl") or appium_conf.get("remote_url") or "http://127.0.0.1:4723"
        caps = appium_conf.get("capabilities") or {}
        return webdriver.Remote(remote_url, caps)

    def find_element(driver, step: Dict[str, Any]):
        by = (step.get("by") or "xpath").lower()
        value = step.get("value") or step.get("selector") or step.get("xpath")
        if not value:
            raise ValueError("selector is required")
        by_map = {
            "id": AppiumBy.ID,
            "xpath": AppiumBy.XPATH,
            "accessibility_id": AppiumBy.ACCESSIBILITY_ID,
            "class_name": AppiumBy.CLASS_NAME,
            "android_uiautomator": AppiumBy.ANDROID_UIAUTOMATOR,
            "ios_predicate": AppiumBy.IOS_PREDICATE,
            "ios_class_chain": AppiumBy.IOS_CLASS_CHAIN
        }
        return driver.find_element(by_map.get(by, AppiumBy.XPATH), value)

    def wait_element(driver, step: Dict[str, Any], timeout_ms: int):
        by = (step.get("by") or "xpath").lower()
        value = step.get("value") or step.get("selector") or step.get("xpath")
        if not value:
            time.sleep(timeout_ms / 1000)
            return
        by_map = {
            "id": AppiumBy.ID,
            "xpath": AppiumBy.XPATH,
            "accessibility_id": AppiumBy.ACCESSIBILITY_ID,
            "class_name": AppiumBy.CLASS_NAME,
            "android_uiautomator": AppiumBy.ANDROID_UIAUTOMATOR,
            "ios_predicate": AppiumBy.IOS_PREDICATE,
            "ios_class_chain": AppiumBy.IOS_CLASS_CHAIN
        }
        locator = (by_map.get(by, AppiumBy.XPATH), value)
        WebDriverWait(driver, timeout_ms / 1000).until(EC.presence_of_element_located(locator))

    appium_conf = content.get("appium") or {}
    steps = content.get("steps") or []
    start = time.time()
    logs = []
    driver = None
    try:
        driver = build_driver(appium_conf)
        for step in steps:
            action = (step.get("action") or "").lower()
            if action == "click":
                el = find_element(driver, step)
                el.click()
                logs.append("click")
            elif action == "input":
                el = find_element(driver, step)
                if step.get("clear", True):
                    el.clear()
                el.send_keys(step.get("text") or "")
                logs.append("input")
            elif action == "wait":
                ms = int(step.get("ms") or 1000)
                wait_element(driver, step, ms)
                logs.append(f"wait {ms}")
            elif action == "back":
                driver.back()
                logs.append("back")
            elif action == "launch":
                if hasattr(driver, "launch_app"):
                    driver.launch_app()
                logs.append("launch")
            elif action == "close":
                if hasattr(driver, "close_app"):
                    driver.close_app()
                logs.append("close")
            elif action == "screenshot":
                path = step.get("path") or step.get("file") or step.get("name") or "screenshot.png"
                driver.get_screenshot_as_file(path)
                logs.append(f"screenshot {path}")
            elif action == "assert_exists":
                find_element(driver, step)
                logs.append("assert_exists")
            else:
                raise ValueError(f"unsupported action: {action}")
        duration = int((time.time() - start) * 1000)
        result = {"durationMs": duration, "logs": "\n".join(logs)}
        return success(result)
    except Exception as e:
        duration = int((time.time() - start) * 1000)
        return failed(str(e), {"durationMs": duration, "logs": "\n".join(logs)})
    finally:
        if driver is not None:
            driver.quit()

def main():
    raw = sys.stdin.read()
    payload = json.loads(raw)
    typ = payload.get("type")
    content = payload.get("content") or {}
    try:
        if typ == "API":
            result = run_api(content)
        elif typ == "WEB":
            result = run_web(content)
        elif typ == "APP":
            result = run_app(content)
        else:
            result = failed(f"unsupported type: {typ}")
    except Exception as e:
        result = failed(str(e))
    print(json.dumps(result, ensure_ascii=False))

if __name__ == "__main__":
    main()
