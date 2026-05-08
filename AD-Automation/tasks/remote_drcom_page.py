# Auto-split from campus_portal_task.py. Keep behavior changes focused.

from config import (
    ACCOUNT_INPUT_IMAGE_PATH,
    CAPTCHA_INPUT_IMAGE_PATH,
    DX_CANDIDATE_IMAGE_PATH,
    REMOTE_AVATAR_IMAGE_PATH,
    REMOTE_BUSINESS_MENU_IMAGE_PATH,
    REMOTE_EDIT_PROFILE_IMAGE_PATH,
    REMOTE_LOGIN_BUTTON_IMAGE_PATH,
    REMOTE_SYSTEM_MENU_IMAGE_PATH,
)
from tasks.dom_utils import print_visible_text_diagnostics
from tasks.image_automation import click_image_center, locate_image_on_screen, pyautogui, save_pyautogui_screenshot



def select_saved_dx_account(page):
    print("准备在新标签页选择保存的 dx 账号")
    try:
        page.bring_to_front()
    except Exception:
        pass
    page.wait_for_timeout(1000)

    if wait_and_double_click_account_input_by_image(page, timeout_ms=45000):
        print("已优先通过图片双击用户名输入框")
    else:
        print_visible_text_diagnostics(page, ["用户名", "账号", "dx"])
        raise RuntimeError("等待用户名输入框图片超时")

    if click_dx_candidate_by_image(page):
        print("已通过图片点击 dx 候选账号")
        wait_for_manual_captcha(page)
        return

    if not wait_for_saved_dx_candidate(page, timeout_ms=3000, raise_on_timeout=False):
        print("未通过图片或 DOM 检测到 dx 候选，改用 PyAutoGUI 屏幕坐标再双击一次")
        if not double_click_login_username_input_by_pyautogui(page):
            print_visible_text_diagnostics(page, ["用户名", "账号", "dx"])
            raise RuntimeError("PyAutoGUI 未能点击新标签页中的用户名输入框")
        if click_dx_candidate_by_image(page):
            print("已通过图片点击 dx 候选账号")
            wait_for_manual_captcha(page)
            return
        wait_for_saved_dx_candidate(page, timeout_ms=7000)

    if click_saved_dx_candidate(page):
        print("已点击 dx 候选账号")
        wait_for_manual_captcha(page)
        return

    print("dx 候选已不再可见，按已选择继续")
    wait_for_manual_captcha(page)



def wait_for_manual_captcha(page):
    print("已点击 dx 候选账号，等待浏览器自动填入用户名和密码")
    page.wait_for_timeout(1500)
    captcha_code = input("请输入图形验证码，脚本将自动填入并点击登录: ").strip()
    if not captcha_code:
        raise RuntimeError("验证码不能为空")
    fill_remote_captcha_and_login(page, captcha_code)



def wait_for_login_username_input(page, timeout_ms=30000):
    print("等待新标签页用户名输入框加载...")
    deadline = timeout_ms
    interval = 500
    stable_count = 0
    last_box = None

    while deadline > 0:
        box = get_login_username_input_box(page)
        if box:
            current_box = (
                round(box["x"]),
                round(box["y"]),
                round(box["width"]),
                round(box["height"]),
            )
            stable_count = stable_count + 1 if current_box == last_box else 1
            last_box = current_box
            if stable_count >= 3:
                print("已检测到新标签页用户名输入框")
                return
        else:
            stable_count = 0
            last_box = None

        page.wait_for_timeout(interval)
        deadline -= interval

    print_visible_text_diagnostics(page, ["用户名", "账号", "user"])
    raise RuntimeError("等待新标签页用户名输入框超时")



def get_login_username_input_box(page):
    for frame in page.frames:
        try:
            box = frame.evaluate(
                """() => {
                    const target = findUsernameInput();
                    if (!target) return null;
                    const style = window.getComputedStyle(target);
                    const rect = target.getBoundingClientRect();
                    const centerX = rect.left + rect.width / 2;
                    const centerY = rect.top + rect.height / 2;
                    const elementAtCenter = document.elementFromPoint(centerX, centerY);
                    const canReceiveClick = elementAtCenter === target || target.contains(elementAtCenter);
                    if (style.display === 'none'
                        || style.visibility === 'hidden'
                        || Number(style.opacity) === 0
                        || rect.width <= 0
                        || rect.height <= 0
                        || rect.bottom < 0
                        || rect.right < 0
                        || rect.top > window.innerHeight
                        || rect.left > window.innerWidth
                        || !canReceiveClick) {
                        return null;
                    }
                    return { x: rect.left, y: rect.top, width: rect.width, height: rect.height };

                    function findUsernameInput() {
                        const accountInput = document.querySelector('input#account[name="account"], input#account');
                        if (accountInput) return accountInput;

                        const inputs = Array.from(document.querySelectorAll('input, textarea')).filter(el => {
                            const style = window.getComputedStyle(el);
                            const rect = el.getBoundingClientRect();
                            const type = (el.getAttribute('type') || '').toLowerCase();
                            return style.display !== 'none'
                                && style.visibility !== 'hidden'
                                && rect.width > 0
                                && rect.height > 0
                                && type !== 'hidden'
                                && type !== 'password';
                        });
                        return inputs.find(input => {
                            const text = [
                                input.id,
                                input.name,
                                input.placeholder,
                                input.getAttribute('title'),
                                input.getAttribute('aria-label'),
                                input.autocomplete
                            ].filter(Boolean).join(' ').replace(/\\s+/g, '').toLowerCase();
                            return text.includes('user')
                                || text.includes('account')
                                || text.includes('name')
                                || text.includes('username')
                                || text.includes('用户名')
                                || text.includes('账号')
                                || text.includes('账户');
                        }) || inputs[0];
                    }
                }"""
            )
        except Exception:
            box = None

        if box:
            return box

    return None



def wait_for_saved_dx_candidate(page, timeout_ms=10000, raise_on_timeout=True):
    print("等待 dx 候选账号加载...")
    deadline = timeout_ms
    interval = 300

    while deadline > 0:
        if has_saved_dx_candidate(page):
            print("已检测到 dx 候选账号")
            return
        page.wait_for_timeout(interval)
        deadline -= interval

    if not raise_on_timeout:
        return False

    print_visible_text_diagnostics(page, ["dx"])
    raise RuntimeError("等待 dx 候选账号超时")



def has_saved_dx_candidate(page):
    for frame in page.frames:
        try:
            exists = frame.evaluate(
                """() => Array.from(document.querySelectorAll(
                    'button,a,input[type="button"],input[type="submit"],.btn,span,div,li,td,[role="button"],[role="option"]'
                )).some(el => {
                    const style = window.getComputedStyle(el);
                    const rect = el.getBoundingClientRect();
                    const value = (el.innerText || el.value || el.textContent || el.getAttribute('title') || '').replace(/\\s+/g, '').toLowerCase();
                    return style.display !== 'none'
                        && style.visibility !== 'hidden'
                        && rect.width > 0
                        && rect.height > 0
                        && rect.bottom >= 0
                        && rect.top <= window.innerHeight
                        && value === 'dx';
                })"""
            )
        except Exception:
            exists = False

        if exists:
            return True

    return False



def has_login_username_input(page):
    for frame in page.frames:
        try:
            exists = frame.evaluate(
                """() => Boolean(findUsernameInput());

                function findUsernameInput() {
                    const accountInput = document.querySelector('input#account[name="account"], input#account');
                    if (accountInput) return accountInput;

                    const inputs = Array.from(document.querySelectorAll('input, textarea')).filter(el => {
                        const style = window.getComputedStyle(el);
                        const rect = el.getBoundingClientRect();
                        const type = (el.getAttribute('type') || '').toLowerCase();
                        return style.display !== 'none'
                            && style.visibility !== 'hidden'
                            && rect.width > 0
                            && rect.height > 0
                            && type !== 'hidden'
                            && type !== 'password';
                    });
                    return inputs.find(input => {
                        const text = [
                            input.id,
                            input.name,
                            input.placeholder,
                            input.getAttribute('title'),
                            input.getAttribute('aria-label'),
                            input.autocomplete
                        ].filter(Boolean).join(' ').replace(/\\s+/g, '').toLowerCase();
                        return text.includes('user')
                            || text.includes('account')
                            || text.includes('name')
                            || text.includes('username')
                            || text.includes('用户名')
                            || text.includes('账号')
                            || text.includes('账户');
                    }) || inputs[0];
                }"""
            )
        except Exception:
            exists = False

        if exists:
            return True

    return False



def double_click_login_username_input(page):
    for frame_index, frame in enumerate(page.frames):
        try:
            box = frame.evaluate(
                """() => {
                    const target = findUsernameInput();
                    if (!target) return null;
                    target.disabled = false;
                    target.readOnly = false;
                    target.removeAttribute('disabled');
                    target.removeAttribute('readonly');
                    target.scrollIntoView({ block: 'center', inline: 'center' });
                    target.focus();
                    const rect = target.getBoundingClientRect();
                    return { x: rect.left, y: rect.top, width: rect.width, height: rect.height };

                    function findUsernameInput() {
                        const accountInput = document.querySelector('input#account[name="account"], input#account');
                        if (accountInput) return accountInput;

                        const inputs = Array.from(document.querySelectorAll('input, textarea')).filter(el => {
                            const style = window.getComputedStyle(el);
                            const rect = el.getBoundingClientRect();
                            const type = (el.getAttribute('type') || '').toLowerCase();
                            return style.display !== 'none'
                                && style.visibility !== 'hidden'
                                && rect.width > 0
                                && rect.height > 0
                                && type !== 'hidden'
                                && type !== 'password';
                        });
                        return inputs.find(input => {
                            const text = [
                                input.id,
                                input.name,
                                input.placeholder,
                                input.getAttribute('title'),
                                input.getAttribute('aria-label'),
                                input.autocomplete
                            ].filter(Boolean).join(' ').replace(/\\s+/g, '').toLowerCase();
                            return text.includes('user')
                                || text.includes('account')
                                || text.includes('name')
                                || text.includes('username')
                                || text.includes('用户名')
                                || text.includes('账号')
                                || text.includes('账户');
                        }) || inputs[0];
                    }
                }"""
            )
        except Exception:
            box = None

        if box:
            x = box["x"] + box["width"] / 2
            y = box["y"] + box["height"] / 2
            page.mouse.move(x, y, steps=8)
            page.wait_for_timeout(150)
            page.mouse.click(x, y, click_count=3, delay=120)
            page.wait_for_timeout(300)
            print(f"已用鼠标在新标签页 Frame {frame_index} 点击三下用户名输入框")
            return True

    return False



def double_click_login_username_input_by_pyautogui(page):
    if pyautogui is None:
        print("未安装 PyAutoGUI，无法使用屏幕鼠标点击")
        return False

    try:
        page.bring_to_front()
    except Exception:
        pass

    if double_click_account_input_by_image(page):
        return True

    point = get_login_username_input_screen_point(page)
    if not point:
        return False

    pyautogui.PAUSE = 0.08
    x = int(point["screenX"])
    y = int(point["screenY"])
    print(f"PyAutoGUI 准备点击三下屏幕坐标: ({x}, {y})")
    pyautogui.moveTo(x, y, duration=0.25)
    pyautogui.click(x=x, y=y, clicks=3, interval=0.12)
    page.wait_for_timeout(600)
    return True



def wait_and_double_click_account_input_by_image(page, timeout_ms=45000):
    if not ACCOUNT_INPUT_IMAGE_PATH.exists():
        raise RuntimeError(f"缺少用户名输入框图片模板，请保存到: {ACCOUNT_INPUT_IMAGE_PATH}")

    print("等待用户名输入框图片加载，最长等待:", timeout_ms, "ms")
    deadline = timeout_ms
    interval = 700

    while deadline > 0:
        if double_click_account_input_by_image(page, save_not_found_screenshot=False):
            return True
        page.wait_for_timeout(interval)
        deadline -= interval

    save_pyautogui_screenshot("account_input_not_found")
    return False



def double_click_account_input_by_image(page, save_not_found_screenshot=True):
    if pyautogui is None:
        return False
    if not ACCOUNT_INPUT_IMAGE_PATH.exists():
        print("未找到用户名输入框图片模板:", ACCOUNT_INPUT_IMAGE_PATH)
        return False

    print("开始根据图片查找用户名输入框:", ACCOUNT_INPUT_IMAGE_PATH)
    print("当前屏幕尺寸:", pyautogui.size())
    deadline = 10000
    interval = 500
    while deadline > 0:
        match = locate_image_on_screen(ACCOUNT_INPUT_IMAGE_PATH)

        if match:
            center = pyautogui.center(match)
            print(f"已根据图片找到用户名输入框，1 秒后点击三下: ({center.x}, {center.y})")
            page.wait_for_timeout(1000)
            pyautogui.moveTo(center.x, center.y, duration=0.25)
            pyautogui.click(x=center.x, y=center.y, clicks=3, interval=0.12)
            print("已用 PyAutoGUI 点击三下用户名输入框")
            page.wait_for_timeout(1200)
            return True

        page.wait_for_timeout(interval)
        deadline -= interval

    print("根据图片未找到用户名输入框")
    if save_not_found_screenshot:
        save_pyautogui_screenshot("account_input_not_found")
    return False



def click_dx_candidate_by_image(page):
    if pyautogui is None:
        return False
    if not DX_CANDIDATE_IMAGE_PATH.exists():
        print("未找到 dx 候选图片模板，请保存到:", DX_CANDIDATE_IMAGE_PATH)
        return False

    print("开始根据图片查找 dx 候选账号:", DX_CANDIDATE_IMAGE_PATH)
    deadline = 10000
    interval = 300
    while deadline > 0:
        match = locate_image_on_screen(DX_CANDIDATE_IMAGE_PATH)

        if match:
            center = pyautogui.center(match)
            print(f"已根据图片找到 dx 候选，准备点击: ({center.x}, {center.y})")
            pyautogui.moveTo(center.x, center.y, duration=0.2)
            pyautogui.click(x=center.x, y=center.y)
            print("已用 PyAutoGUI 点击 dx 候选")
            page.wait_for_timeout(800)
            return True

        page.wait_for_timeout(interval)
        deadline -= interval

    print("根据图片未找到 dx 候选账号")
    save_pyautogui_screenshot("dx_candidate_not_found")
    return False



def fill_remote_captcha_and_login(page, captcha_code):
    if pyautogui is None:
        raise RuntimeError("未安装 PyAutoGUI，无法填入远程验证码")

    print("准备填入验证码:", captcha_code)
    if not click_image_center(CAPTCHA_INPUT_IMAGE_PATH, "验证码输入框", timeout_ms=20000):
        raise RuntimeError(f"未找到验证码输入框图片模板或屏幕匹配失败: {CAPTCHA_INPUT_IMAGE_PATH}")

    pyautogui.hotkey("ctrl", "a")
    pyautogui.write(captcha_code, interval=0.04)
    page.wait_for_timeout(300)
    print("已填入验证码")

    if not click_image_center(REMOTE_LOGIN_BUTTON_IMAGE_PATH, "远程登录按钮", timeout_ms=20000):
        raise RuntimeError(f"未找到远程登录按钮图片模板或屏幕匹配失败: {REMOTE_LOGIN_BUTTON_IMAGE_PATH}")

    print("已点击远程登录按钮")
    page.wait_for_timeout(1500)
    navigate_remote_profile_menu(page)



def navigate_remote_profile_menu(page):
    print("准备进入远程系统修改资料页面")
    steps = [
        (REMOTE_AVATAR_IMAGE_PATH, "小人头像", 45000),
        (REMOTE_SYSTEM_MENU_IMAGE_PATH, "认证计费管理系统", 45000),
        (REMOTE_BUSINESS_MENU_IMAGE_PATH, "业务管理", 45000),
        (REMOTE_EDIT_PROFILE_IMAGE_PATH, "修改资料", 45000),
    ]

    for image_path, label, timeout_ms in steps:
        if not click_image_center(image_path, label, timeout_ms=timeout_ms):
            raise RuntimeError(f"未找到或未能点击{label}: {image_path}")
        page.wait_for_timeout(1200)

    print("已进入修改资料页面")



def get_login_username_input_screen_point(page):
    viewport_origin = get_page_viewport_screen_origin(page)
    if not viewport_origin:
        print("未能获取浏览器视口屏幕坐标")
        return None

    for frame_index, frame in enumerate(page.frames):
        box = get_login_username_input_box_in_frame(frame)
        if not box:
            continue

        frame_offset = {"x": 0, "y": 0}
        if frame != page.main_frame:
            try:
                frame_element_box = frame.frame_element().bounding_box()
                if frame_element_box:
                    frame_offset = {
                        "x": frame_element_box["x"],
                        "y": frame_element_box["y"],
                    }
            except Exception:
                frame_offset = {"x": 0, "y": 0}

        center_x = frame_offset["x"] + box["x"] + box["width"] / 2
        center_y = frame_offset["y"] + box["y"] + box["height"] / 2
        screen_x = viewport_origin["x"] + center_x
        screen_y = viewport_origin["y"] + center_y
        print(f"已换算用户名输入框 Frame {frame_index} 的屏幕坐标")
        return {"screenX": screen_x, "screenY": screen_y}

    return None



def get_page_viewport_screen_origin(page):
    try:
        return page.evaluate(
            """() => {
                const leftChrome = Math.max(0, (window.outerWidth - window.innerWidth) / 2);
                const topChrome = Math.max(0, window.outerHeight - window.innerHeight - leftChrome);
                return {
                    x: window.screenX + leftChrome,
                    y: window.screenY + topChrome
                };
            }"""
        )
    except Exception:
        return None



def get_login_username_input_box_in_frame(frame):
    try:
        return frame.evaluate(
            """() => {
                const target = findUsernameInput();
                if (!target) return null;
                target.disabled = false;
                target.readOnly = false;
                target.removeAttribute('disabled');
                target.removeAttribute('readonly');
                target.scrollIntoView({ block: 'center', inline: 'center' });
                target.focus();
                const rect = target.getBoundingClientRect();
                return { x: rect.left, y: rect.top, width: rect.width, height: rect.height };

                function findUsernameInput() {
                    const accountInput = document.querySelector('input#account[name="account"], input#account');
                    if (accountInput) return accountInput;

                    const inputs = Array.from(document.querySelectorAll('input, textarea')).filter(el => {
                        const style = window.getComputedStyle(el);
                        const rect = el.getBoundingClientRect();
                        const type = (el.getAttribute('type') || '').toLowerCase();
                        return style.display !== 'none'
                            && style.visibility !== 'hidden'
                            && rect.width > 0
                            && rect.height > 0
                            && type !== 'hidden'
                            && type !== 'password';
                    });
                    return inputs.find(input => {
                        const text = [
                            input.id,
                            input.name,
                            input.placeholder,
                            input.getAttribute('title'),
                            input.getAttribute('aria-label'),
                            input.autocomplete
                        ].filter(Boolean).join(' ').replace(/\\s+/g, '').toLowerCase();
                        return text.includes('user')
                            || text.includes('account')
                            || text.includes('name')
                            || text.includes('username')
                            || text.includes('用户名')
                            || text.includes('账号')
                            || text.includes('账户');
                    }) || inputs[0];
                }
            }"""
        )
    except Exception:
        return None



def click_saved_dx_candidate(page):
    for frame_index, frame in enumerate(page.frames):
        try:
            clicked = frame.evaluate(
                """() => {
                    const candidates = Array.from(document.querySelectorAll(
                        'button,a,input[type="button"],input[type="submit"],.btn,span,div,li,td,[role="button"],[role="option"]'
                    )).filter(el => {
                        const style = window.getComputedStyle(el);
                        const rect = el.getBoundingClientRect();
                        const value = (el.innerText || el.value || el.textContent || el.getAttribute('title') || '').replace(/\\s+/g, '').toLowerCase();
                        return style.display !== 'none'
                            && style.visibility !== 'hidden'
                            && rect.width > 0
                            && rect.height > 0
                            && value === 'dx';
                    });

                    candidates.sort((left, right) => {
                        const leftRect = left.getBoundingClientRect();
                        const rightRect = right.getBoundingClientRect();
                        return leftRect.top - rightRect.top;
                    });

                    const target = candidates[0];
                    if (!target) return false;
                    const clickable = target.closest('button,a,[role="button"],[role="option"],li,.btn') || target;
                    clickable.scrollIntoView({ block: 'center', inline: 'center' });
                    const rect = clickable.getBoundingClientRect();
                    const x = rect.left + rect.width / 2;
                    const y = rect.top + rect.height / 2;
                    for (const eventName of ['mouseover', 'mousedown', 'mouseup', 'click']) {
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
        except Exception:
            clicked = False

        if clicked:
            print(f"已在新标签页 Frame {frame_index} 点击 dx 候选账号")
            return True

    return False
