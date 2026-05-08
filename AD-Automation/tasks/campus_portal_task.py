from playwright.sync_api import sync_playwright

from config import AUTH_STATE_PATH, HEADLESS, PORTAL_PASSWORD, PORTAL_USERNAME, TARGET_URL, USE_AUTH_STATE
from tasks.bastion_portal import click_connect_button, click_https_entry
from tasks.browser_session import launch_browser_context
from tasks.dom_utils import print_visible_text_diagnostics
from tasks.portal_login import click_local_verification_if_present, click_login, click_login_if_visible, fill_login_form


def open_campus_portal():
    with sync_playwright() as p:
        browser, context = launch_browser_context(p)
        page = context.new_page()

        try:
            page.goto(TARGET_URL, wait_until="domcontentloaded", timeout=30000)
            print("已打开校园网自动化页面")
            print("页面标题:", page.title())
            print("当前地址:", page.url)

            if USE_AUTH_STATE and AUTH_STATE_PATH.exists():
                print("已加载本地登录状态:", AUTH_STATE_PATH)
            else:
                if PORTAL_USERNAME and PORTAL_PASSWORD:
                    fill_login_form(page)
                    click_login(page)
                    click_local_verification_if_present(page)
                    click_login_if_visible(page)
                else:
                    print("未配置 PORTAL_USERNAME / PORTAL_PASSWORD。")
                    print("请在浏览器中手动输入账号密码并完成登录。")
                    input("手动完成登录后按 Enter 继续后续自动化...")

                if USE_AUTH_STATE:
                    input("登录成功后回到这里按 Enter 保存登录状态...")
                    AUTH_STATE_PATH.parent.mkdir(parents=True, exist_ok=True)
                    context.storage_state(path=str(AUTH_STATE_PATH))
                    print("登录状态已保存:", AUTH_STATE_PATH)

            try:
                click_https_entry(page)
                click_connect_button(page)
            except Exception as error:
                print("登录后的自动化步骤失败:", error)
                print_visible_text_diagnostics(page, ["HTTP(S)", "连接"])
                if not HEADLESS:
                    input("浏览器已保持打开，检查页面后按 Enter 关闭...")
                raise

            if not HEADLESS:
                input("浏览器将保持打开。完成查看后按 Enter 关闭...")
        finally:
            browser.close()
