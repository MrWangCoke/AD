# Auto-split from campus_portal_task.py. Keep behavior changes focused.

from config import AUTH_STATE_PATH, BROWSER_CHANNEL, CHROME_CDP_URL, HEADLESS, USE_AUTH_STATE



def launch_browser_context(playwright):
    if CHROME_CDP_URL:
        try:
            browser = playwright.chromium.connect_over_cdp(CHROME_CDP_URL)
            if not browser.contexts:
                raise RuntimeError("已连接 Chrome 调试端口，但没有可用浏览器上下文")
            print("已连接本机 Chrome:", CHROME_CDP_URL)
            return browser, browser.contexts[0]
        except Exception as error:
            print(f"未能连接本机 Chrome 调试端口 {CHROME_CDP_URL}，改用 Playwright 浏览器:", error)

    launch_kwargs = {"headless": HEADLESS}
    if BROWSER_CHANNEL:
        launch_kwargs["channel"] = BROWSER_CHANNEL

    try:
        browser = playwright.chromium.launch(**launch_kwargs)
        if BROWSER_CHANNEL:
            print("已使用浏览器通道:", BROWSER_CHANNEL)
    except Exception as error:
        if not BROWSER_CHANNEL:
            raise
        print(f"未能启动 {BROWSER_CHANNEL}，改用 Playwright Chromium:", error)
        browser = playwright.chromium.launch(headless=HEADLESS)

    context_kwargs = {}
    if USE_AUTH_STATE and AUTH_STATE_PATH.exists():
        context_kwargs["storage_state"] = str(AUTH_STATE_PATH)

    return browser, browser.new_context(**context_kwargs)
