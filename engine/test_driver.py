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
    headless = content.get("headless")
    if headless is None:
        headless = True
    headless_flag = bool(headless)
    if headless_flag:
        ops.add_argument("--headless=new")
    ops.add_argument("--disable-gpu")
    ops.add_argument("--no-sandbox")
    ops.add_argument("--window-size=1280,800")
    
    driver = None
    logs_note = []
    logs_note.append(f"headless: {headless_flag}")
    
    # Check for user-provided driver path in content (optional)
    user_driver_path = content.get("driverPath") or content.get("driver_path")
    
    try:
        if user_driver_path and os.path.exists(user_driver_path):
            service = EdgeService(executable_path=user_driver_path)
            driver = EdgeDriver(service=service, options=ops)
            logs_note.append(f"driver: user-provided ({user_driver_path})")
        else:
            # 1) Try Selenium Manager (no executable_path) — best for version matching
            _sink = io.StringIO()
            with contextlib.redirect_stdout(_sink), contextlib.redirect_stderr(_sink):
                service = EdgeService()  # Selenium >=4.6 will download a compatible driver
                driver = EdgeDriver(service=service, options=ops)
            logs_note.append("driver: selenium-manager")
    except Exception as e1:
        # Try to clean selenium cache if it contains an old incompatible driver
        if "session not created" in str(e1).lower():
            try:
                cache_dir = os.path.join(os.path.expanduser("~"), ".cache", "selenium")
                if os.path.exists(cache_dir):
                    import shutil
                    # Only log, don't delete everything blindly, but we could target msedgedriver
                    ms_cache = os.path.join(cache_dir, "msedgedriver")
                    if os.path.exists(ms_cache):
                        shutil.rmtree(ms_cache)
                        logs_note.append("cleaned selenium cache for msedgedriver")
            except Exception:
                pass

        # 2) Fallback to packaged driver under engine/edgedriver_win64/msedgedriver.exe (offline)
        packaged = os.path.join(os.path.dirname(os.path.abspath(__file__)), "edgedriver_win64", "msedgedriver.exe")
        if os.path.exists(packaged):
            try:
                _sink = io.StringIO()
                with contextlib.redirect_stdout(_sink), contextlib.redirect_stderr(_sink):
                    service = EdgeService(executable_path=packaged)
                    driver = EdgeDriver(service=service, options=ops)
                logs_note.append("driver: packaged-edgedriver")
            except Exception as e2:
                # If fallback also fails, provide a comprehensive error message
                err_msg = str(e1)
                if "session not created" in err_msg.lower():
                    # Extract version mismatch info if possible
                    import re
                    match = re.search(r"supports Microsoft Edge version (\d+).*browser version is ([\d.]+)", err_msg)
                    if match:
                        v_driver = match.group(1)
                        v_browser = match.group(2)
                        raise Exception(f"【驱动版本不匹配】浏览器版本为 {v_browser}，但驱动仅支持 {v_driver}。由于当前环境无法自动下载驱动，请手动下载匹配的 msedgedriver.exe 并放置到 engine/edgedriver_win64/ 目录下。")
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
        if not headless_flag:
            try:
                pause_ms = content.get("pauseAfterMs")
                if pause_ms is None:
                    pause_ms = 2000
                time.sleep(int(pause_ms) / 1000)
            except Exception:
                pass
        driver.quit()

def run_app(content: Dict[str, Any]) -> Dict[str, Any]:
    from appium import webdriver
    from appium.webdriver.common.appiumby import AppiumBy
    from selenium.webdriver.support.ui import WebDriverWait
    from selenium.webdriver.support import expected_conditions as EC
    import re
    try:
        from appium.options.android import UiAutomator2Options
    except Exception:
        UiAutomator2Options = None

    def build_driver(appium_conf: Dict[str, Any]):
        remote_url = appium_conf.get("remoteUrl") or appium_conf.get("remote_url") or "http://127.0.0.1:4723"
        caps = appium_conf.get("capabilities") or {}
        # Normalize keys for W3C/Appium 2 (prefix appium:)
        normalized = {}
        for k, v in caps.items():
            key = k
            if k in ("automationName", "deviceName", "appPackage", "appActivity", "udid", "platformVersion", "chromedriverExecutable"):
                key = f"appium:{k}"
            normalized[key] = v
        # platformName should be non-prefixed
        if "platformName" in caps:
            normalized["platformName"] = caps["platformName"]
        # Fallback defaults
        normalized.setdefault("platformName", "Android")
        normalized.setdefault("appium:automationName", caps.get("automationName") or "UiAutomator2")
        # System app safety: avoid clearing data for com.android.* when user not specifying reset flags
        pkg = normalized.get("appium:appPackage") or caps.get("appPackage")
        if pkg and isinstance(pkg, str) and pkg.startswith("com.android."):
            normalized.setdefault("appium:noReset", True)
            normalized.setdefault("appium:fullReset", False)
        # Respect unprefixed reset flags if provided
        if "noReset" in caps and "appium:noReset" not in normalized:
            normalized["appium:noReset"] = caps.get("noReset")
        if "fullReset" in caps and "appium:fullReset" not in normalized:
            normalized["appium:fullReset"] = caps.get("fullReset")
        # Use Options API when available to avoid to_capabilities None issues
        try:
            if UiAutomator2Options is not None:
                opts = UiAutomator2Options()
                try:
                    opts.load_capabilities(normalized)
                except Exception:
                    # load_capabilities may expect dict with proper types; ensure dict
                    for k, v in normalized.items():
                        try:
                            opts.set_capability(k, v)
                        except Exception:
                            pass
                return webdriver.Remote(remote_url, options=opts)
            # Legacy fallback for very old client versions
            return webdriver.Remote(remote_url, normalized)
        except Exception as e:
            err_str = str(e).lower()
            if "failed to establish a new connection" in err_str or "connection refused" in err_str or "10061" in err_str:
                raise Exception(f"【Appium 服务未就绪】无法连接到 Appium 地址 {remote_url}。请确保已在本地启动 Appium Server (npm install -g appium; appium) 且端口正确。")
            raise e

    def _by_map():
        return {
            "id": AppiumBy.ID,
            "xpath": AppiumBy.XPATH,
            "accessibility_id": AppiumBy.ACCESSIBILITY_ID,
            "class_name": AppiumBy.CLASS_NAME,
            "android_uiautomator": AppiumBy.ANDROID_UIAUTOMATOR,
            "ios_predicate": AppiumBy.IOS_PREDICATE,
            "ios_class_chain": AppiumBy.IOS_CLASS_CHAIN,
            "css": getattr(AppiumBy, "CSS_SELECTOR", AppiumBy.XPATH),
            "css_selector": getattr(AppiumBy, "CSS_SELECTOR", AppiumBy.XPATH),
        }

    def _get_locator(step: Dict[str, Any]):
        by = (step.get("by") or "xpath").lower()
        value = step.get("value") or step.get("selector") or step.get("xpath")
        if value is None or str(value).strip() == "":
            raise ValueError("selector is required")
        return by, str(value)

    def _find_once(driver, by: str, value: str):
        m = _by_map()
        return driver.find_element(m.get(by, AppiumBy.XPATH), value)

    def _find_with_wait(driver, by: str, value: str, timeout_ms: int):
        ms = int(timeout_ms or 0)
        if ms <= 0:
            return _find_once(driver, by, value)
        end = time.time() + (ms / 1000.0)
        last_err = None
        while True:
            try:
                return _find_once(driver, by, value)
            except Exception as e:
                last_err = e
                if time.time() >= end:
                    raise last_err
                time.sleep(0.25)

    def find_element(driver, step: Dict[str, Any], timeout_ms: int = 0, logs_out=None):
        attempts = []
        by, value = _get_locator(step)
        attempts.append((by, value))
        try:
            return _find_with_wait(driver, by, value, timeout_ms)
        except Exception as e1:
            if platform_name == "android":
                def _try_scroll_once(hint: str, mode: str):
                    if hint is None:
                        return False
                    txt = str(hint)
                    if txt.strip() == "":
                        return False
                    escaped = txt.replace("\\", "\\\\").replace('"', '\\"')
                    if logs_out is not None:
                        logs_out.append(f"scroll_try {mode}Contains={txt}")
                    exprs = []
                    if mode == "text":
                        exprs.append(f'new UiScrollable(new UiSelector().scrollable(true)).scrollIntoView(new UiSelector().textContains("{escaped}"))')
                        exprs.append(f'new UiScrollable(new UiSelector().scrollable(true).instance(0)).scrollIntoView(new UiSelector().textContains("{escaped}"))')
                    else:
                        exprs.append(f'new UiScrollable(new UiSelector().scrollable(true)).scrollIntoView(new UiSelector().descriptionContains("{escaped}"))')
                        exprs.append(f'new UiScrollable(new UiSelector().scrollable(true).instance(0)).scrollIntoView(new UiSelector().descriptionContains("{escaped}"))')
                    for expr in exprs:
                        try:
                            _ = driver.find_element(AppiumBy.ANDROID_UIAUTOMATOR, expr)
                            if logs_out is not None:
                                logs_out.append(f"scroll_ok {mode}Contains={txt}")
                            return True
                        except Exception:
                            continue
                    if logs_out is not None:
                        logs_out.append(f"scroll_failed {mode}Contains={txt}")
                    return False

                def _extract_hint_from_xpath(xp: str):
                    s = str(xp)
                    m = re.search(r"contains\(\s*@text\s*,\s*'([^']+)'\s*\)", s)
                    if m:
                        return ("text", m.group(1))
                    m = re.search(r"contains\(\s*@content-desc\s*,\s*'([^']+)'\s*\)", s)
                    if m:
                        return ("desc", m.group(1))
                    m = re.search(r"@text\s*=\s*'([^']+)'", s)
                    if m:
                        return ("text", m.group(1))
                    m = re.search(r"@content-desc\s*=\s*'([^']+)'", s)
                    if m:
                        return ("desc", m.group(1))
                    return (None, None)

                if by == "xpath":
                    mode, hint = _extract_hint_from_xpath(value)
                    if mode is not None:
                        if _try_scroll_once(hint, "text" if mode == "text" else "desc"):
                            try:
                                return _find_with_wait(driver, by, value, timeout_ms)
                            except Exception:
                                pass
                        try:
                            escaped = str(hint).replace("\\", "\\\\").replace('"', '\\"')
                            if mode == "text":
                                uia = f'new UiSelector().textContains("{escaped}")'
                            else:
                                uia = f'new UiSelector().descriptionContains("{escaped}")'
                            attempts.append(("android_uiautomator", uia))
                            if logs_out is not None:
                                logs_out.append(f"uia_try {uia}")
                            return _find_with_wait(driver, "android_uiautomator", uia, timeout_ms)
                        except Exception:
                            pass
            fallbacks = step.get("fallbacks") or []
            if isinstance(fallbacks, list):
                for fb in fallbacks:
                    if not isinstance(fb, dict):
                        continue
                    fb_by = (fb.get("by") or "xpath").lower()
                    fb_val = fb.get("value") or fb.get("selector") or fb.get("xpath")
                    if fb_val is None or str(fb_val).strip() == "":
                        continue
                    fb_val = str(fb_val)
                    if fb_by == "xpath" and ("contains(text(),'//*" in fb_val or 'contains(text(),"//*' in fb_val):
                        continue
                    attempts.append((fb_by, fb_val))
                    try:
                        el = _find_with_wait(driver, fb_by, fb_val, timeout_ms)
                        if logs_out is not None:
                            logs_out.append(f"fallback {fb_by}={fb_val}")
                        return el
                    except Exception:
                        continue
            msg = "; ".join([f"{a}={b}" for a, b in attempts])
            raise Exception(f"element not found (tried {msg})") from e1

    def wait_element(driver, step: Dict[str, Any], timeout_ms: int, logs_out=None):
        value = step.get("value") or step.get("selector") or step.get("xpath")
        if value is None or str(value).strip() == "":
            time.sleep(int(timeout_ms or 0) / 1000)
            return
        find_element(driver, step, timeout_ms=int(timeout_ms or 0), logs_out=logs_out)

    def _current_context(driver):
        pkg = None
        act = None
        try:
            if hasattr(driver, "current_package"):
                pkg = driver.current_package
        except Exception:
            pkg = None
        try:
            if hasattr(driver, "current_activity"):
                act = driver.current_activity
        except Exception:
            act = None
        if pkg is None and act is None:
            return ""
        if pkg is None:
            return f"activity={act}"
        if act is None:
            return f"package={pkg}"
        return f"package={pkg} activity={act}"

    appium_conf = content.get("appium") or {}
    caps_for_platform = appium_conf.get("capabilities") or {}
    platform_name = str(caps_for_platform.get("platformName") or caps_for_platform.get("platform_name") or "Android").strip().lower()
    steps = content.get("steps") or []
    start = time.time()
    logs = []
    driver = None
    try:
        driver = build_driver(appium_conf)
        logs.append(f"steps_count={len(steps)}")
        ctx0 = _current_context(driver)
        if ctx0:
            logs.append(f"ctx {ctx0}")
        for step in steps:
            action = (step.get("action") or "").lower()
            if action == "click":
                by, value = _get_locator(step)
                logs.append(f"click {by}={value}")
                timeout_ms = int(step.get("timeoutMs") or step.get("timeout_ms") or 5000)
                el = find_element(driver, step, timeout_ms=timeout_ms, logs_out=logs)
                el.click()
                logs.append("click ok")
            elif action == "input":
                by, value = _get_locator(step)
                logs.append(f"input {by}={value}")
                timeout_ms = int(step.get("timeoutMs") or step.get("timeout_ms") or 5000)
                el = find_element(driver, step, timeout_ms=timeout_ms, logs_out=logs)
                if step.get("clear", True):
                    el.clear()
                el.send_keys(step.get("text") or "")
                logs.append("input ok")
            elif action == "wait":
                ms = int(step.get("ms") or 1000)
                v = step.get("value") or step.get("selector") or step.get("xpath")
                if v is None or str(v).strip() == "":
                    logs.append(f"wait {ms}")
                    wait_element(driver, step, ms, logs_out=logs)
                else:
                    by, value = _get_locator(step)
                    logs.append(f"wait {by}={value} {ms}")
                    wait_element(driver, step, ms, logs_out=logs)
                    logs.append("wait ok")
            elif action == "back":
                driver.back()
                logs.append("back")
            elif action == "launch":
                if hasattr(driver, "launch_app"):
                    driver.launch_app()
                logs.append("launch")
                ctx1 = _current_context(driver)
                if ctx1:
                    logs.append(f"ctx {ctx1}")
            elif action == "close":
                if hasattr(driver, "close_app"):
                    driver.close_app()
                logs.append("close")
            elif action == "screenshot":
                path = step.get("path") or step.get("file") or step.get("name") or "screenshot.png"
                driver.get_screenshot_as_file(path)
                logs.append(f"screenshot {path}")
            elif action == "assert_exists":
                by, value = _get_locator(step)
                logs.append(f"assert_exists {by}={value}")
                timeout_ms = int(step.get("timeoutMs") or step.get("timeout_ms") or 5000)
                find_element(driver, step, timeout_ms=timeout_ms, logs_out=logs)
                logs.append("assert_exists ok")
            else:
                raise ValueError(f"unsupported action: {action}")
        duration = int((time.time() - start) * 1000)
        result = {"durationMs": duration, "logs": "\n".join(logs)}
        return success(result)
    except Exception as e:
        duration = int((time.time() - start) * 1000)
        try:
            logs.append(f"[ERROR] {str(e)}")
        except Exception:
            pass
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
