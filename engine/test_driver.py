import sys
import json
import time
from typing import Any, Dict, Optional
from unittest.mock import MagicMock
import contextlib
import io
import os

# Patch broken selenium installation (missing modules)
def patch_selenium():
    modules_to_patch = [
        "selenium.webdriver.firefox",
        "selenium.webdriver.firefox.webdriver",
        "selenium.webdriver.firefox.options",
        "selenium.webdriver.firefox.service",
        "selenium.webdriver.firefox.firefox_profile",
        # "selenium.webdriver.edge",  <-- We now use Edge
        # "selenium.webdriver.edge.webdriver",
        # "selenium.webdriver.edge.options",
        # "selenium.webdriver.edge.service",
        "selenium.webdriver.ie",
        "selenium.webdriver.ie.webdriver",
        "selenium.webdriver.ie.options",
        "selenium.webdriver.ie.service",
        "selenium.webdriver.safari",
        "selenium.webdriver.safari.webdriver",
        "selenium.webdriver.safari.options",
        "selenium.webdriver.safari.service",
        "selenium.webdriver.webkitgtk",
        "selenium.webdriver.webkitgtk.webdriver",
        "selenium.webdriver.webkitgtk.options",
        "selenium.webdriver.webkitgtk.service",
        "selenium.webdriver.wpewebkit",
        "selenium.webdriver.wpewebkit.webdriver",
        "selenium.webdriver.wpewebkit.options",
        "selenium.webdriver.wpewebkit.service",
    ]
    for m in modules_to_patch:
        sys.modules[m] = MagicMock()

patch_selenium()

# Force robust IO encoding for all outputs
try:
    os.environ.setdefault("PYTHONIOENCODING", "utf-8:backslashreplace")
except Exception:
    pass

def success(result: Dict[str, Any]) -> Dict[str, Any]:
    return {
        "status": "success",
        **result
    }

def failed(message: str, result: Optional[Dict[str, Any]] = None) -> Dict[str, Any]:
    try:
        msg = message.encode("utf-8", "backslashreplace").decode("utf-8", "ignore")
    except Exception:
        msg = str(message)
    base = {
        "status": "failed",
        "error": msg
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

    body = content.get("body")
    json_body = None
    if isinstance(body, (dict, list)):
        json_body = body
        body = None
    else:
        body = body or ""

    start = time.time()
    if json_body is not None:
        resp = requests.request(method, url, headers=headers, params=params, json=json_body)
    else:
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
    # Prefer Selenium Manager to auto-resolve matching EdgeDriver
    from selenium.webdriver.edge.webdriver import WebDriver as EdgeDriver
    from selenium.webdriver.edge.options import Options
    from selenium.webdriver.edge.service import Service as EdgeService
    import os
    
    ops = Options()
    ops.add_argument("--headless=new")
    ops.add_argument("--disable-gpu")
    ops.add_argument("--no-sandbox")
    ops.add_argument("--window-size=1280,800")
    
    driver = None
    logs_note = []
    try:
        # 1) Try Selenium Manager (no executable_path) — best for version matching
        _sink = io.StringIO()
        with contextlib.redirect_stdout(_sink), contextlib.redirect_stderr(_sink):
            service = EdgeService()  # Selenium >=4.6 will download a compatible driver
            driver = EdgeDriver(service=service, options=ops)
        logs_note.append("driver: selenium-manager")
    except Exception as e1:
        # 2) Fallback to packaged driver under engine/edgedriver_win64/msedgedriver.exe (offline)
        packaged = os.path.join(os.path.dirname(os.path.abspath(__file__)), "edgedriver_win64", "msedgedriver.exe")
        if os.path.exists(packaged):
            try:
                _sink = io.StringIO()
                with contextlib.redirect_stdout(_sink), contextlib.redirect_stderr(_sink):
                    service = EdgeService(executable_path=packaged)
                    driver = EdgeDriver(service=service, options=ops)
                logs_note.append("driver: packaged-edgedriver")
            except Exception:
                # Re-raise original for clarity
                raise e1
        else:
            # No packaged driver, bubble up the selenium-manager error
            raise e1
        
    start = time.time()
    logs = []
    try:
        if logs_note:
            logs.extend(logs_note)
        # Support two modes: DSL or Python
        mode = (content.get("language") or content.get("mode") or "dsl").lower()
        if mode == "python":
            user_code = content.get("script") or ""
            import builtins
            from selenium.webdriver.common.by import By
            from selenium.webdriver.support.ui import WebDriverWait
            from selenium.webdriver.support import expected_conditions as EC
            step_no = {"n": 0}
            def _append(level: str, text: str):
                try:
                    logs.append(f"[{level}] {text}")
                except Exception:
                    pass
            def step(msg, level: str = "INFO"):
                try:
                    step_no["n"] += 1
                    _append(level, f"[步骤 {step_no['n']}] {msg}")
                except Exception:
                    _append(level, msg)
            def log(msg):
                try:
                    _append("INFO", str(msg))
                except Exception:
                    pass
            def assert_equal(expected, actual, context: str = ""):
                if str(expected) == str(actual):
                    _append("ASSERT", f"断言通过: {context} | 预期: {expected} | 实际: {actual}")
                    return True
                else:
                    _append("ASSERT", f"断言失败: {context} | 预期: {expected} | 实际: {actual}")
                    raise AssertionError(f"assert_equal failed: expect {expected} got {actual} ({context})")
            def assert_title_contains(keyword: str):
                k = str(keyword)
                if k in (driver.title or ""):
                    _append("ASSERT", f'断言通过: 标题包含 "{k}"')
                else:
                    _append("ASSERT", f'断言失败: 标题不包含 "{k}" | 实际: {driver.title}')
                    raise AssertionError(f"title not contains {k}")
            # Limit builtins available during exec for safety (lightweight)
            safe_builtins = {
                "True": True, "False": False, "None": None, "len": len, "range": range, "print": log
            }
            glb = {"__builtins__": safe_builtins, "By": By, "WebDriverWait": WebDriverWait, "EC": EC, "time": time}
            lcl = {"driver": driver, "log": log, "step": step, "assert_equal": assert_equal, "assert_title_contains": assert_title_contains}
            exec(compile(user_code, "<user_web_script>", "exec"), glb, lcl)
        else:
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
        safe_logs = "\n".join(logs)
        try:
            safe_logs = safe_logs.encode("utf-8", "backslashreplace").decode("utf-8", "ignore")
        except Exception:
            pass
        result = {"durationMs": duration, "logs": safe_logs}
        return success(result)
    except Exception as e:
        duration = int((time.time() - start) * 1000)
        safe_logs = "\n".join(logs)
        try:
            safe_logs = safe_logs.encode("utf-8", "backslashreplace").decode("utf-8", "ignore")
        except Exception:
            pass
        return failed(str(e), {"durationMs": duration, "logs": safe_logs})
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
    try:
        if hasattr(sys.stdout, "reconfigure"):
            sys.stdout.reconfigure(encoding="utf-8", errors="backslashreplace")
        if hasattr(sys.stderr, "reconfigure"):
            sys.stderr.reconfigure(encoding="utf-8", errors="backslashreplace")
    except Exception:
        pass
    # Read stdin as bytes and decode safely to avoid surrogate issues
    try:
        raw_bytes = sys.stdin.buffer.read()
        raw = raw_bytes.decode("utf-8", "replace")
    except Exception:
        raw = sys.stdin.read()
    try:
        payload = json.loads(raw)
    except Exception as e:
        result = failed(f"invalid payload: {e}", {"logs": raw})
        txt = json.dumps(result, ensure_ascii=False)
        try:
            sys.stdout.buffer.write(txt.encode('utf-8', 'backslashreplace'))
        except Exception:
            sys.stdout.write(txt)
        return
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
    txt = json.dumps(result, ensure_ascii=False)
    try:
        sys.stdout.buffer.write(txt.encode('utf-8', 'backslashreplace'))
    except Exception:
        sys.stdout.write(txt)

if __name__ == "__main__":
    main()
