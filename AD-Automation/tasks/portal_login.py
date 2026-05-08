# Auto-split from campus_portal_task.py. Keep behavior changes focused.

from config import PORTAL_PASSWORD, PORTAL_USERNAME
from tasks.dom_utils import click_visible_text_by_dom



def fill_login_form(page):
    page.locator("#uCode").wait_for(state="visible", timeout=15000)
    page.locator("#uCode").fill(PORTAL_USERNAME)
    page.locator("#pW").fill(PORTAL_PASSWORD)
    print("已从环境变量填入账号密码")



def click_local_verification_if_present(page):
    try:
        local_password_input = page.locator("#local_v")
        local_password_input.wait_for(state="visible", timeout=5000)
    except Exception:
        print("未检测到本地密码认证弹窗，继续执行")
        return

    for attempt in range(1, 6):
        print(f"检测到本地密码认证弹窗，尝试点击验证按钮（第 {attempt} 次）")
        clicked = click_local_verification_button_by_position(page)
        if not clicked:
            clicked = click_local_verification_button_by_dom(page)
        if not clicked:
            clicked = click_visible_text_by_dom(page, "验证", raise_on_missing=False)

        page.wait_for_timeout(1200)
        if not page.locator("#local_v").is_visible():
            print("本地密码认证弹窗已关闭")
            return

        if clicked:
            print("验证按钮已点击，但弹窗仍在，准备重试")
        else:
            print("未找到可点击的验证按钮，准备重试")

    raise RuntimeError("已多次点击验证，但本地密码认证弹窗仍未关闭，请检查页面是否卡住")



def click_local_verification_button_by_position(page):
    box = page.locator("#local_v").bounding_box()
    if not box:
        return False

    x = box["x"] + box["width"] + 55
    y = box["y"] + box["height"] / 2
    page.mouse.move(x, y)
    page.mouse.down()
    page.wait_for_timeout(80)
    page.mouse.up()
    print("已按本地密码输入框右侧位置点击验证")
    return True



def click_login(page):
    login_button = page.locator("button.login")
    login_button.wait_for(state="visible", timeout=10000)
    login_button.click()
    print("已点击登录按钮")
    try:
        page.wait_for_load_state("domcontentloaded", timeout=15000)
    except Exception:
        pass
    page.wait_for_timeout(1500)



def click_login_if_visible(page):
    login_button = page.locator("button.login")
    try:
        login_button.wait_for(state="visible", timeout=5000)
        login_button.click()
        print("已再次点击登录按钮")
        try:
            page.wait_for_load_state("domcontentloaded", timeout=15000)
        except Exception:
            pass
        page.wait_for_timeout(1500)
    except Exception:
        print("登录按钮已不可见，继续执行")



def click_local_verification_button_by_dom(page):
    return page.evaluate(
        """() => {
            const localInput = document.querySelector('#local_v');
            if (!localInput) return false;

            const inputRect = localInput.getBoundingClientRect();
            const candidates = Array.from(document.querySelectorAll(
                'button,a,input[type="button"],input[type="submit"],.btn,.verification,span,div'
            )).filter(el => {
                const style = window.getComputedStyle(el);
                const rect = el.getBoundingClientRect();
                const value = (el.innerText || el.value || el.textContent || '').replace(/\\s+/g, '');
                return style.display !== 'none'
                    && style.visibility !== 'hidden'
                    && rect.width > 0
                    && rect.height > 0
                    && value.includes('验证')
                    && Math.abs(rect.top - inputRect.top) < 90
                    && rect.left > inputRect.right - 20;
            });

            candidates.sort((left, right) => {
                const leftRect = left.getBoundingClientRect();
                const rightRect = right.getBoundingClientRect();
                const leftDistance = Math.abs(leftRect.top - inputRect.top) + Math.abs(leftRect.left - inputRect.right);
                const rightDistance = Math.abs(rightRect.top - inputRect.top) + Math.abs(rightRect.left - inputRect.right);
                return leftDistance - rightDistance;
            });

            const target = candidates[0];
            if (!target) return false;
            const clickable = target.closest('button,a,input[type="button"],input[type="submit"],.btn,.verification') || target;
            const rect = clickable.getBoundingClientRect();
            const x = rect.left + rect.width / 2;
            const y = rect.top + rect.height / 2;
            const events = ['mouseover', 'mousedown', 'mouseup', 'click'];
            for (const eventName of events) {
                clickable.dispatchEvent(new MouseEvent(eventName, {
                    bubbles: true,
                    cancelable: true,
                    view: window,
                    clientX: x,
                    clientY: y
                }));
            }
            return true;
        }"""
    )
