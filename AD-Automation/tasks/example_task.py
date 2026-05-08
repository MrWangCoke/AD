from playwright.sync_api import sync_playwright


def run_example_task():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=False)
        page = browser.new_page()

        try:
            page.goto("https://example.com", wait_until="networkidle")

            title = page.title()
            current_url = page.url

            print("自动化成功")
            print("页面标题:", title)
            print("当前地址:", current_url)

        except Exception as e:
            print("自动化失败：", e)

        finally:
            browser.close()
