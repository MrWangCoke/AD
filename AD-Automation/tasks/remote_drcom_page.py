# Auto-split from campus_portal_task.py. Keep behavior changes focused.

import importlib.util
import json
import time
from datetime import datetime

from config import (
    ACCOUNT_INPUT_IMAGE_PATH,
    ACCOUNT_BOUND_WARNING_IMAGE_PATH,
    BASE_DIR,
    BROADBAND_ACCOUNT_INPUT_IMAGE_PATH,
    BROADBAND_PASSWORD_INPUT_IMAGE_PATH,
    CAPTCHA_INPUT_IMAGE_PATH,
    CONFIRM_BUTTON_IMAGE_PATH,
    DX_CANDIDATE_IMAGE_PATH,
    QUERY_BUTTON_IMAGE_PATH,
    REMOTE_AVATAR_IMAGE_PATH,
    REMOTE_BUSINESS_MENU_IMAGE_PATH,
    REMOTE_EDIT_PROFILE_IMAGE_PATH,
    REMOTE_LOGIN_BUTTON_IMAGE_PATH,
    REMOTE_SYSTEM_MENU_IMAGE_PATH,
    OPERATION_SUCCESS_IMAGE_PATH,
    STUDENT_ID_INPUT_IMAGE_PATH,
    TICKET_CURSOR_PATH,
    USER_LIST_IMAGE_PATH,
)
from tasks.dom_utils import click_visible_text_by_dom, print_visible_text_diagnostics
from tasks.image_automation import (
    click_image_center,
    format_remaining_seconds,
    locate_image_on_screen,
    pyautogui,
    save_pyautogui_screenshot,
)
from tasks.ticket_queue import PendingTicket, fetch_next_ticket_from_db, update_ticket_status_in_db


CAPTCHA_TEST_DIR = BASE_DIR / "CaptchaTest"
CAPTCHA_PREDICT_SCRIPT_PATH = CAPTCHA_TEST_DIR / "predict.py"
CAPTCHA_CAPTURE_DIR = BASE_DIR / "output"
CAPTCHA_COMBINED_IMAGE_PATH = CAPTCHA_CAPTURE_DIR / "remote_captcha_combo.png"
CAPTCHA_CROP_IMAGE_PATH = CAPTCHA_CAPTURE_DIR / "remote_captcha.png"
# 直接对齐 deep-learning/captcha/collect_data.py 的训练图规格。
CAPTCHA_INPUT_TEMPLATE_WIDTH = 286
CAPTCHA_CAPTURE_GAP = 6
CAPTCHA_IMAGE_WIDTH = 128
CAPTCHA_IMAGE_HEIGHT = 44
CAPTCHA_TOP_OFFSET = 2
CAPTCHA_COMBINED_WIDTH = CAPTCHA_IMAGE_WIDTH
CAPTCHA_COMBINED_HEIGHT = CAPTCHA_IMAGE_HEIGHT
_captcha_predict_function = None

RESULT_SUCCESS = "自动化处理完成"
RESULT_STUDENT_NOT_FOUND = "学号未查询到"
RESULT_CONTACT_STAFF = "请联系工作人员"



def cleanup_stale_captcha_images():
    CAPTCHA_CAPTURE_DIR.mkdir(parents=True, exist_ok=True)
    patterns = (
        "remote_captcha*.png",
        "remote_captcha_combo*.png",
    )
    removed_count = 0

    for pattern in patterns:
        for file_path in CAPTCHA_CAPTURE_DIR.glob(pattern):
            try:
                file_path.unlink()
                removed_count += 1
            except Exception as error:
                print("清理旧验证码截图失败:", file_path, error)

    if removed_count:
        print(f"已清理旧验证码截图 {removed_count} 张")


def select_saved_dx_account(page):
    cleanup_stale_captcha_images()
    print("开始处理远程页登录")
    try:
        page.bring_to_front()
    except Exception:
        pass
    page.wait_for_timeout(1000)

    if wait_and_double_click_account_input_by_image(page, timeout_ms=45000):
        print("已激活用户名输入框")
    else:
        print_visible_text_diagnostics(page, ["用户名", "账号", "dx"])
        raise RuntimeError("等待用户名输入框图片超时")

    if click_dx_candidate_by_image(page):
        print("已选中保存的 dx 账号")
        wait_for_manual_captcha(page)
        return

    if not wait_for_saved_dx_candidate(page, timeout_ms=3000, raise_on_timeout=False):
        print("未通过图片或 DOM 检测到 dx 候选，改用 PyAutoGUI 屏幕坐标再双击一次")
        if not double_click_login_username_input_by_pyautogui(page):
            print_visible_text_diagnostics(page, ["用户名", "账号", "dx"])
            raise RuntimeError("PyAutoGUI 未能点击新标签页中的用户名输入框")
        if click_dx_candidate_by_image(page):
            print("已选中保存的 dx 账号")
            wait_for_manual_captcha(page)
            return
        wait_for_saved_dx_candidate(page, timeout_ms=7000)

    if click_saved_dx_candidate(page):
        print("已选中保存的 dx 账号")
        wait_for_manual_captcha(page)
        return

    print("dx 候选已不再可见，按已选择继续")
    wait_for_manual_captcha(page)



def wait_for_manual_captcha(page):
    print("准备自动识别验证码")
    page.wait_for_timeout(1500)

    try:
        captcha_code, cleanup_paths = recognize_remote_captcha(page)
        print(f"自动识别成功，验证码为: {captcha_code}")
    except Exception as error:
        print(f"自动识别失败，改为手动输入: {error}")
        print("请手动输入验证码")
        cleanup_paths = []
        captcha_code = input("请输入图形验证码: ").strip()
        if not captcha_code:
            raise RuntimeError("验证码不能为空")

    fill_remote_captcha_and_login(page, captcha_code, cleanup_paths=cleanup_paths)


def recognize_remote_captcha(page):
    print("准备自动识别验证码")
    capture_info = capture_remote_captcha_images(page)
    predict = load_captcha_predict_function()
    crop_path = capture_info["crop_path"]
    print("开始调用验证码识别模型:", crop_path)
    captcha_code = (predict(str(crop_path)) or "").strip()
    if len(captcha_code) != 4 or not captcha_code.isdigit():
        raise RuntimeError(f"验证码识别结果无效: {captcha_code!r}")
    print("验证码识别完成，结果为:", captcha_code)
    return captcha_code, capture_info["cleanup_paths"]



def capture_remote_captcha_images(page):
    if pyautogui is None:
        raise RuntimeError("未安装 PyAutoGUI，无法截取验证码图片")

    try:
        page.bring_to_front()
    except Exception:
        pass

    match = wait_for_captcha_input_image(page, timeout_ms=20000)
    captcha_region = get_captcha_capture_region(match)
    CAPTCHA_CAPTURE_DIR.mkdir(parents=True, exist_ok=True)
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    run_combo_path = CAPTCHA_CAPTURE_DIR / f"remote_captcha_combo_{timestamp}.png"
    run_crop_path = CAPTCHA_CAPTURE_DIR / f"remote_captcha_{timestamp}.png"

    print(
        "开始截取验证码区域:",
        f"left={captcha_region['left']}, top={captcha_region['top']}, "
        f"width={captcha_region['width']}, height={captcha_region['height']}",
    )
    captcha_image = pyautogui.screenshot(
        region=(
            captcha_region["left"],
            captcha_region["top"],
            captcha_region["width"],
            captcha_region["height"],
        )
    )
    captcha_image.save(str(CAPTCHA_COMBINED_IMAGE_PATH))
    captcha_image.save(str(run_combo_path))
    captcha_image.save(str(CAPTCHA_CROP_IMAGE_PATH))
    captcha_image.save(str(run_crop_path))
    print("已保存验证码图片:", CAPTCHA_CROP_IMAGE_PATH)
    print("已保存本次验证码图片:", run_crop_path)
    return {
        "crop_path": run_crop_path,
        "cleanup_paths": [run_combo_path, run_crop_path],
    }


def get_remote_login_button_search_region(captcha_input_match):
    screen_size = pyautogui.size()
    left = max(0, int(round(captcha_input_match.left - CAPTCHA_IMAGE_WIDTH)))
    top = max(0, int(round(captcha_input_match.top - CAPTCHA_IMAGE_HEIGHT)))
    width = min(screen_size.width - left, CAPTCHA_COMBINED_WIDTH + CAPTCHA_IMAGE_WIDTH)
    height = min(screen_size.height - top, CAPTCHA_COMBINED_HEIGHT + CAPTCHA_IMAGE_HEIGHT)
    print(
        "登录按钮搜索区域:",
        f"left={left}, top={top}, width={width}, height={height}"
    )
    return (left, top, width, height)


def wait_for_captcha_input_image(page, timeout_ms=20000):
    if not CAPTCHA_INPUT_IMAGE_PATH.exists():
        raise RuntimeError(f"缺少验证码输入框图片模板，请保存到: {CAPTCHA_INPUT_IMAGE_PATH}")

    # 这里不是直接识别验证码数字，而是先找“验证码输入框”这一行的模板图。
    # 找到输入框后，再根据它的位置去推算右侧验证码图片的截图区域。
    print(f"开始定位验证码输入框 [{CAPTCHA_INPUT_IMAGE_PATH.name}]")
    deadline = timeout_ms
    interval = 500
    next_progress_ms = timeout_ms

    while deadline > 0:
        match = locate_image_on_screen(CAPTCHA_INPUT_IMAGE_PATH)
        if match:
            print(
                "已定位验证码输入框:",
                f"left={match.left}, top={match.top}, width={match.width}, height={match.height}",
            )
            return match
        if deadline <= next_progress_ms:
            print(f"等待验证码输入框加载中，剩余约 {format_remaining_seconds(deadline)} 秒")
            next_progress_ms -= 2000
        page.wait_for_timeout(interval)
        deadline -= interval

    save_pyautogui_screenshot("captcha_input_not_found")
    raise RuntimeError("等待验证码输入框图片超时")



def get_captcha_capture_region(captcha_input_match):
    screen_size = pyautogui.size()
    scale = captcha_input_match.width / CAPTCHA_INPUT_TEMPLATE_WIDTH
    # 方法1：向左移动验证码宽度的 1/10
    left = int(round(captcha_input_match.left + captcha_input_match.width + CAPTCHA_CAPTURE_GAP * scale - CAPTCHA_IMAGE_WIDTH * scale * 0.1))
    top = int(round(captcha_input_match.top + CAPTCHA_TOP_OFFSET * scale))
    width = max(1, int(round(CAPTCHA_IMAGE_WIDTH * scale)))
    height = max(1, int(round(CAPTCHA_IMAGE_HEIGHT * scale)))
    right = left + width
    bottom = top + height

    if left < 0 or top < 0 or right > screen_size.width or bottom > screen_size.height:
        raise RuntimeError(
            "计算出的验证码截图区域超出屏幕范围: "
            f"left={left}, top={top}, right={right}, bottom={bottom}, "
            f"screen={screen_size.width}x{screen_size.height}"
        )

    return {
        "left": left,
        "top": top,
        "width": width,
        "height": height,
    }



def load_captcha_predict_function():
    global _captcha_predict_function

    if _captcha_predict_function is not None:
        print("验证码识别模型已加载，复用现有实例")
        return _captcha_predict_function

    if not CAPTCHA_PREDICT_SCRIPT_PATH.exists():
        raise RuntimeError(f"未找到验证码识别脚本: {CAPTCHA_PREDICT_SCRIPT_PATH}")

    print("开始加载验证码识别脚本:", CAPTCHA_PREDICT_SCRIPT_PATH)
    spec = importlib.util.spec_from_file_location("remote_captcha_predict", str(CAPTCHA_PREDICT_SCRIPT_PATH))
    if spec is None or spec.loader is None:
        raise RuntimeError(f"无法加载验证码识别脚本: {CAPTCHA_PREDICT_SCRIPT_PATH}")

    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)

    predict = getattr(module, "predict", None)
    if predict is None:
        raise RuntimeError(f"验证码识别脚本中未找到 predict 函数: {CAPTCHA_PREDICT_SCRIPT_PATH}")

    _captcha_predict_function = predict
    print("验证码识别脚本加载完成")
    return _captcha_predict_function



def cleanup_captcha_images(cleanup_paths):
    if not cleanup_paths:
        return

    for file_path in cleanup_paths:
        try:
            if file_path.exists():
                file_path.unlink()
                print("已删除临时验证码图片:", file_path)
        except Exception as error:
            print("删除临时验证码图片失败:", file_path, error)


def has_visible_text(page, text, prefer_exact_match=False):
    target_texts = text if isinstance(text, list) else [text]
    options = {
        "targetTexts": [value.replace(" ", "").lower() for value in target_texts],
        "preferExactMatch": prefer_exact_match,
    }

    for frame in page.frames:
        try:
            found = frame.evaluate(
                """options => {
                    const candidates = Array.from(document.querySelectorAll(
                        'button,a,input[type="button"],input[type="submit"],.btn,.verification,span,div,td,th,li,tr,[role="button"],[role="menuitem"],body'
                    ));
                    return candidates.some(el => {
                        const style = window.getComputedStyle(el);
                        const rect = el.getBoundingClientRect();
                        const value = [
                            el.innerText,
                            el.value,
                            el.textContent,
                            el.getAttribute('title'),
                            el.getAttribute('aria-label')
                        ].filter(Boolean).join(' ').replace(/\\s+/g, '').toLowerCase();
                        return style.display !== 'none'
                            && style.visibility !== 'hidden'
                            && rect.width > 0
                            && rect.height > 0
                            && rect.bottom >= 0
                            && rect.top <= window.innerHeight
                            && value.length <= 300
                            && options.targetTexts.some(targetText => (
                                options.preferExactMatch ? value === targetText : value.includes(targetText)
                            ));
                    });
                }""",
                options,
            )
        except Exception:
            found = False

        if found:
            return True

    return False



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
    next_progress_ms = timeout_ms

    while deadline > 0:
        if has_saved_dx_candidate(page):
            print("已检测到 dx 候选账号")
            return
        if deadline <= next_progress_ms:
            print(f"等待 dx 候选账号中，剩余约 {format_remaining_seconds(deadline)} 秒")
            next_progress_ms -= 2000
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

    print("等待用户名输入框出现")
    deadline = timeout_ms
    interval = 700
    next_progress_ms = timeout_ms

    while deadline > 0:
        if double_click_account_input_by_image(page, save_not_found_screenshot=False):
            return True
        if deadline <= next_progress_ms:
            print(f"等待用户名输入框加载中，剩余约 {format_remaining_seconds(deadline)} 秒")
            next_progress_ms -= 3000
        page.wait_for_timeout(interval)
        deadline -= interval

    print("等待用户名输入框超时")
    save_pyautogui_screenshot("account_input_not_found")
    return False



def double_click_account_input_by_image(page, save_not_found_screenshot=True):
    if pyautogui is None:
        return False
    if not ACCOUNT_INPUT_IMAGE_PATH.exists():
        print("缺少用户名输入框图片模板:", ACCOUNT_INPUT_IMAGE_PATH.name)
        return False

    print(f"开始定位用户名输入框 [{ACCOUNT_INPUT_IMAGE_PATH.name}]")
    deadline = 10000
    interval = 500
    while deadline > 0:
        match = locate_image_on_screen(ACCOUNT_INPUT_IMAGE_PATH)

        if match:
            center = pyautogui.center(match)
            print(f"已定位用户名输入框，三击坐标: ({center.x}, {center.y})")
            page.wait_for_timeout(1000)
            pyautogui.moveTo(center.x, center.y, duration=0.25)
            pyautogui.click(x=center.x, y=center.y, clicks=3, interval=0.12)
            page.wait_for_timeout(1200)
            return True

        page.wait_for_timeout(interval)
        deadline -= interval

    if save_not_found_screenshot:
        print("未定位到用户名输入框")
        save_pyautogui_screenshot("account_input_not_found")
    return False



def double_click_account_input_by_captcha_anchor(page, save_not_found_screenshot=True):
    if pyautogui is None:
        return False
    if not CAPTCHA_INPUT_IMAGE_PATH.exists():
        print("未找到验证码输入框图片模板，无法通过它反推用户名输入框:", CAPTCHA_INPUT_IMAGE_PATH)
        return False

    print("开始根据验证码输入框位置反推用户名输入框:", CAPTCHA_INPUT_IMAGE_PATH)
    deadline = 6000
    interval = 400

    while deadline > 0:
        match = locate_image_on_screen(CAPTCHA_INPUT_IMAGE_PATH)
        if match:
            target_x = int(round(match.left + match.width * 0.65))
            target_y = int(round(match.top + match.height / 2 - ((match.height + 10) * 2)))
            print(
                "已定位验证码输入框，准备反推点击用户名输入框:",
                f"captcha=({match.left}, {match.top}, {match.width}, {match.height}) -> "
                f"username=({target_x}, {target_y})"
            )
            pyautogui.moveTo(target_x, target_y, duration=0.25)
            pyautogui.click(x=target_x, y=target_y, clicks=3, interval=0.12)
            print("已根据验证码输入框位置点击三下用户名输入框")
            page.wait_for_timeout(1200)
            return True

        page.wait_for_timeout(interval)
        deadline -= interval

    if save_not_found_screenshot:
        print("未能通过验证码输入框位置反推出用户名输入框")
        save_pyautogui_screenshot("account_input_anchor_not_found")
    return False



def click_dx_candidate_by_image(page):
    if pyautogui is None:
        return False
    if not DX_CANDIDATE_IMAGE_PATH.exists():
        print("缺少 dx 候选图片模板:", DX_CANDIDATE_IMAGE_PATH.name)
        return False

    print(f"开始定位 dx 候选账号 [{DX_CANDIDATE_IMAGE_PATH.name}]")
    deadline = 10000
    interval = 300
    next_progress_ms = deadline
    while deadline > 0:
        match = locate_image_on_screen(DX_CANDIDATE_IMAGE_PATH)

        if match:
            center = pyautogui.center(match)
            print(f"已定位 dx 候选账号，点击坐标: ({center.x}, {center.y})")
            pyautogui.moveTo(center.x, center.y, duration=0.2)
            pyautogui.click(x=center.x, y=center.y)
            page.wait_for_timeout(800)
            return True

        if deadline <= next_progress_ms:
            print(f"等待 dx 候选图片中，剩余约 {format_remaining_seconds(deadline)} 秒")
            next_progress_ms -= 2000
        page.wait_for_timeout(interval)
        deadline -= interval

    print("等待 dx 候选账号超时")
    save_pyautogui_screenshot("dx_candidate_not_found")
    return False



def fill_remote_captcha_and_login(page, captcha_code, cleanup_paths=None):
    if pyautogui is None:
        raise RuntimeError("未安装 PyAutoGUI，无法填入远程验证码")

    print(f"准备填入验证码: {captcha_code}")
    bring_remote_page_to_front(page)
    captcha_match = wait_for_captcha_input_image(page, timeout_ms=20000)
    center = pyautogui.center(captcha_match)
    pyautogui.moveTo(center.x, center.y, duration=0.2)
    pyautogui.click(x=center.x, y=center.y)
    page.wait_for_timeout(300)

    pyautogui.hotkey("ctrl", "a")
    pyautogui.write(captcha_code, interval=0.04)
    page.wait_for_timeout(300)
    print("已填入验证码，回车提交")
    pyautogui.press("enter")
    if not wait_for_remote_login_transition(page):
        print("检测到页面仍停留在验证码登录界面，准备重新选择 dx 账号后再输入验证码")
        retry_remote_login_after_captcha_failure(page)
        if not wait_for_remote_login_transition(page):
            raise RuntimeError("重新输入验证码后页面仍未跳转，请检查验证码是否正确")
    if not wait_for_verified_remote_login(page, timeout_ms=15000):
        print("未确认真正进入认证计费管理系统，请手动完成登录，程序会持续等待确认")
        wait_for_manual_verified_remote_login(page)
    cleanup_captcha_images(cleanup_paths)
    navigate_remote_profile_menu(page)


def wait_for_remote_login_transition(page, timeout_ms=8000):
    print("检查验证码提交结果")
    deadline = timeout_ms
    interval = 800

    while deadline > 0:
        if is_remote_captcha_still_visible():
            page.wait_for_timeout(interval)
            deadline -= interval
            continue

        if is_remote_profile_entry_visible():
            print("验证码页已消失，检测到小人头像")
            return True
        if is_remote_billing_system_entry_visible(page):
            print("验证码页已消失，检测到认证计费管理系统入口")
            return True
        print("验证码页已消失，继续做真实登录验证")
        return True

    return False


def is_remote_profile_entry_visible():
    if not REMOTE_AVATAR_IMAGE_PATH.exists():
        return False
    return locate_image_on_screen(REMOTE_AVATAR_IMAGE_PATH, min_confidence=0.55) is not None


def is_remote_billing_system_entry_visible(page):
    if has_visible_text(page, ["认证计费管理系统"], prefer_exact_match=True):
        return True
    if not REMOTE_SYSTEM_MENU_IMAGE_PATH.exists():
        return False
    return locate_image_on_screen(REMOTE_SYSTEM_MENU_IMAGE_PATH, min_confidence=0.48) is not None


def wait_for_verified_remote_login(page, timeout_ms=15000):
    print("验证是否真正登录到认证计费管理系统")
    deadline = timeout_ms
    interval = 1000
    next_progress_ms = timeout_ms

    while deadline > 0:
        if verify_remote_login_ready(page):
            print("已确认登录成功，可以进入认证计费管理系统")
            return True
        if deadline <= next_progress_ms:
            print(f"等待认证计费管理系统入口中，剩余约 {format_remaining_seconds(deadline)} 秒")
            next_progress_ms -= 3000
        page.wait_for_timeout(interval)
        deadline -= interval

    save_pyautogui_screenshot("remote_login_not_verified")
    return False


def wait_for_manual_verified_remote_login(page, timeout_ms=300000):
    print("请在浏览器里手动登录远程系统，直到能看到小人头像和认证计费管理系统入口")
    deadline = timeout_ms
    interval = 2000

    while deadline > 0:
        if verify_remote_login_ready(page):
            print("已检测到手动登录成功")
            return
        print(f"继续等待手动登录完成，剩余约 {format_remaining_seconds(deadline)} 秒")
        page.wait_for_timeout(interval)
        deadline -= interval

    save_pyautogui_screenshot("manual_remote_login_timeout")
    raise RuntimeError("等待手动登录超时，未确认认证计费管理系统入口")


def verify_remote_login_ready(page):
    if is_remote_billing_system_entry_visible(page):
        return True
    if not is_remote_profile_entry_visible():
        return False

    print("检测到小人头像，尝试展开菜单验证认证计费管理系统入口")
    if click_image_center(REMOTE_AVATAR_IMAGE_PATH, "小人头像", timeout_ms=1500, min_confidence=0.55):
        page.wait_for_timeout(800)
        return is_remote_billing_system_entry_visible(page)

    return False


def is_remote_captcha_still_visible():
    if not CAPTCHA_INPUT_IMAGE_PATH.exists():
        return False
    return locate_image_on_screen(CAPTCHA_INPUT_IMAGE_PATH, min_confidence=0.55) is not None


def refill_remote_captcha_and_resubmit(page, captcha_code):
    print(f"准备重新填写验证码: {captcha_code}")
    captcha_match = wait_for_captcha_input_image(page, timeout_ms=10000)
    center = pyautogui.center(captcha_match)
    pyautogui.moveTo(center.x, center.y, duration=0.2)
    pyautogui.click(x=center.x, y=center.y)
    page.wait_for_timeout(200)
    pyautogui.hotkey("ctrl", "a")
    page.wait_for_timeout(120)
    pyautogui.press("backspace")
    page.wait_for_timeout(120)
    pyautogui.write(captcha_code, interval=0.04)
    page.wait_for_timeout(300)
    print("已重新填入验证码，回车提交")
    pyautogui.press("enter")


def retry_remote_login_after_captcha_failure(page):
    print("重新激活用户名输入框")
    if not wait_and_double_click_account_input_by_image(page, timeout_ms=15000):
        raise RuntimeError("验证码失败后未能重新激活用户名输入框")

    print("重新选择保存的 dx 账号")
    if not click_dx_candidate_by_image(page):
        if not wait_for_saved_dx_candidate(page, timeout_ms=5000, raise_on_timeout=False):
            raise RuntimeError("验证码失败后未检测到 dx 候选账号")
        if not click_saved_dx_candidate(page):
            raise RuntimeError("验证码失败后未能重新点击 dx 候选账号")

    page.wait_for_timeout(1200)
    manual_captcha_code = input("请重新手动输入验证码: ").strip()
    if not manual_captcha_code:
        raise RuntimeError("验证码不能为空")
    refill_remote_captcha_and_resubmit(page, manual_captcha_code)
    return manual_captcha_code


def wait_for_remote_avatar_visible(page, timeout_ms=12000):
    print("等待小人头像出现")
    deadline = timeout_ms
    interval = 800

    while deadline > 0:
        if is_remote_profile_entry_visible():
            print("已检测到小人头像")
            return
        page.wait_for_timeout(interval)
        deadline -= interval

    raise RuntimeError("验证码提交后未检测到小人头像，请检查页面是否真正登录成功")


def bring_remote_page_to_front(page):
    try:
        page.bring_to_front()
        page.wait_for_timeout(300)
    except Exception as error:
        print("切换远程页面到前台失败，继续执行:", error)



def click_remote_login_button(page, captcha_match):
    print("准备点击远程登录按钮")

    if click_remote_login_button_by_dom(page):
        print("已通过 DOM 点击远程登录按钮")
        return True

    bring_remote_page_to_front(page)
    login_region = get_remote_login_button_search_region(captcha_match)
    if click_image_center(REMOTE_LOGIN_BUTTON_IMAGE_PATH, "远程登录按钮", timeout_ms=6000, region=login_region):
        print("已通过图片点击远程登录按钮")
        return True

    print("图片点击远程登录按钮失败，尝试使用回车提交表单")
    bring_remote_page_to_front(page)
    try:
        pyautogui.press("enter")
        page.wait_for_timeout(1000)
        return True
    except Exception as error:
        print("回车提交失败:", error)

    return False



def click_remote_login_button_by_dom(page):
    print("开始通过 DOM 查找远程登录按钮")

    for frame_index, frame in enumerate(page.frames):
        try:
            clicked = frame.evaluate(
                """() => {
                    const candidates = Array.from(document.querySelectorAll(
                        'button,a,input[type="button"],input[type="submit"],.btn,span,div,[role="button"]'
                    )).filter(el => {
                        const style = window.getComputedStyle(el);
                        const rect = el.getBoundingClientRect();
                        const value = (
                            el.innerText
                            || el.value
                            || el.textContent
                            || el.getAttribute('title')
                            || el.getAttribute('aria-label')
                            || ''
                        ).replace(/\\s+/g, '');
                        return style.display !== 'none'
                            && style.visibility !== 'hidden'
                            && rect.width > 0
                            && rect.height > 0
                            && (value.includes('登录') || value.toLowerCase().includes('login'));
                    });

                    candidates.sort((left, right) => {
                        const leftRect = left.getBoundingClientRect();
                        const rightRect = right.getBoundingClientRect();
                        return rightRect.width * rightRect.height - leftRect.width * leftRect.height;
                    });

                    const target = candidates[0];
                    if (!target) return false;
                    const clickable = target.closest('button,a,input[type="button"],input[type="submit"],.btn,[role="button"]') || target;
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
                    if (typeof clickable.click === 'function') {
                        clickable.click();
                    }
                    return true;
                }"""
            )
        except Exception:
            clicked = False

        if clicked:
            print(f"已在 Frame {frame_index} 通过 DOM 触发远程登录按钮")
            return True

    print("未通过 DOM 找到远程登录按钮")
    return False



def navigate_remote_profile_menu(page):
    print("开始进入修改资料页面")
    steps = [
        {
            "image_path": REMOTE_AVATAR_IMAGE_PATH,
            "label": "小人头像",
            "timeout_ms": 45000,
            "dom_texts": None,
        },
        {
            "image_path": REMOTE_SYSTEM_MENU_IMAGE_PATH,
            "label": "认证计费管理系统",
            "timeout_ms": 20000,
            "dom_texts": None,
            "min_confidence": 0.48,
        },
        {
            "image_path": REMOTE_BUSINESS_MENU_IMAGE_PATH,
            "label": "业务管理",
            "timeout_ms": 45000,
            "dom_texts": None,
            "min_confidence": 0.45,
        },
        {
            "image_path": REMOTE_EDIT_PROFILE_IMAGE_PATH,
            "label": "修改资料",
            "timeout_ms": 45000,
            "dom_texts": None,
            "min_confidence": 0.45,
        },
    ]

    for step in steps:
        label = step["label"]
        image_path = step["image_path"]
        timeout_ms = step["timeout_ms"]
        dom_texts = step["dom_texts"]
        min_confidence = step.get("min_confidence", 0.62)

        if label == "小人头像" and is_remote_billing_system_entry_visible(page):
            print("认证计费管理系统入口已可见，跳过再次点击小人头像")
            continue

        if dom_texts:
            print(f"尝试通过 DOM 点击{label}")
            if click_visible_text_by_dom(
                page,
                dom_texts,
                prefer_clickable_ancestor=True,
                raise_on_missing=False,
                prefer_exact_match=True,
            ):
                print(f"已通过 DOM 点击{label}")
                page.wait_for_timeout(1200)
                continue
            print(f"DOM 未找到{label}，改用图片点击")

        if not click_image_center(image_path, label, timeout_ms=timeout_ms, min_confidence=min_confidence):
            raise RuntimeError(f"未找到或未能点击{label}: {image_path}")
        page.wait_for_timeout(1200)

    wait_for_edit_profile_form_ready(page)
    print("已进入修改资料页面")
    complete_profile_update_form(page)


def complete_profile_update_form(page):
    print("开始进入工单处理循环")
    idle_started_at = time.monotonic()

    while True:
        ticket = get_next_ticket_for_processing()
        if ticket is None:
            refresh_edit_profile_page(page)
            print_idle_duration(idle_started_at)
            page.wait_for_timeout(1000)
            continue

        idle_started_at = time.monotonic()
        print(
            f"开始处理工单: id={ticket.id}, studentId={ticket.student_id}, "
            f"broadbandAccount={ticket.broadband_account}, status={ticket.status}"
        )
        try:
            refresh_edit_profile_page(page)
            result_message = process_profile_update_ticket(page, ticket)
            mark_ticket_completed(ticket, result_message)
            save_ticket_cursor(ticket)
            print(f"工单处理完成: id={ticket.id}, result={result_message}")
        except Exception as error:
            save_pyautogui_screenshot(f"ticket_{ticket.id}_failed")
            print(f"工单处理失败，未写入完成状态: id={ticket.id}, error={error}")
            raise


def process_profile_update_ticket(page, ticket: PendingTicket):
    if not ticket.student_id:
        raise RuntimeError(f"工单 {ticket.id} 的 student_id 为空")
    if not ticket.broadband_account:
        raise RuntimeError(f"工单 {ticket.id} 的 broadband_account 为空")
    if not ticket.new_password:
        raise RuntimeError(f"工单 {ticket.id} 的 new_password 为空")

    fill_input_by_image(page, STUDENT_ID_INPUT_IMAGE_PATH, "学号输入框", ticket.student_id, click_x_ratio=0.78)
    if not click_image_center(QUERY_BUTTON_IMAGE_PATH, "查询按钮", timeout_ms=15000, min_confidence=0.45):
        raise RuntimeError("未能点击查询按钮，停止处理以避免假完成")
    print("已点击查询按钮，等待页面加载")
    query_result = wait_for_student_query_result(page)
    if query_result == RESULT_STUDENT_NOT_FOUND:
        print("查询结果为学号未查询到")
        return RESULT_STUDENT_NOT_FOUND

    scroll_profile_form_to_bottom(page)
    wait_for_broadband_fields_ready(page)
    fill_input_by_image(
        page,
        BROADBAND_ACCOUNT_INPUT_IMAGE_PATH,
        "宽带账号输入框",
        ticket.broadband_account,
        click_x_ratio=1.08,
    )
    fill_input_by_image(
        page,
        BROADBAND_PASSWORD_INPUT_IMAGE_PATH,
        "宽带密码输入框",
        ticket.new_password,
        click_x_ratio=1.08,
    )

    print("宽带信息填写完成，按回车提交")
    pyautogui.press("enter")
    page.wait_for_timeout(1500)
    return confirm_profile_submission(page)


def refresh_edit_profile_page(page):
    if REMOTE_EDIT_PROFILE_IMAGE_PATH.exists():
        if click_image_center(REMOTE_EDIT_PROFILE_IMAGE_PATH, "修改资料", timeout_ms=1500, min_confidence=0.45):
            print("已点击修改资料")
            page.wait_for_timeout(800)
            wait_for_edit_profile_form_ready(page)
            return

    if is_edit_profile_form_ready():
        print("修改资料图片未匹配到，但当前仍在修改资料表单")
        return

    save_pyautogui_screenshot("edit_profile_refresh_failed")
    raise RuntimeError("未能重新进入修改资料页面，停止处理以避免假完成")


def wait_for_edit_profile_form_ready(page, timeout_ms=15000):
    print("确认修改资料表单是否可用")
    deadline = timeout_ms
    interval = 500

    while deadline > 0:
        if is_edit_profile_form_ready():
            print("已确认修改资料表单可用")
            return
        page.wait_for_timeout(interval)
        deadline -= interval

    save_pyautogui_screenshot("edit_profile_form_not_ready")
    raise RuntimeError("未确认进入修改资料表单")


def is_edit_profile_form_ready():
    if STUDENT_ID_INPUT_IMAGE_PATH.exists():
        if locate_image_on_screen(STUDENT_ID_INPUT_IMAGE_PATH, min_confidence=0.45):
            return True
    if QUERY_BUTTON_IMAGE_PATH.exists():
        if locate_image_on_screen(QUERY_BUTTON_IMAGE_PATH, min_confidence=0.45):
            return True
    return False


def wait_for_student_query_result(page, timeout_ms=10000):
    print("等待学号查询结果")
    deadline = timeout_ms
    interval = 500
    elapsed = 0
    scrolled_to_fields = False
    user_list_hits = 0

    while deadline > 0:
        if elapsed >= 2500 and not scrolled_to_fields:
            scroll_profile_form_to_bottom(page)
            scrolled_to_fields = True

        if is_broadband_field_visible():
            print("已检测到宽带信息输入区域，继续填写")
            return None

        if is_user_list_visible(page):
            user_list_hits += 1
            print(f"检测到用户列表候选，第 {user_list_hits} 次，继续等待成功证据")
        else:
            user_list_hits = 0

        if elapsed >= 3500 and scrolled_to_fields and user_list_hits >= 3:
            print("连续检测到用户列表，且未检测到宽带输入框，判定学号未查询到")
            return RESULT_STUDENT_NOT_FOUND

        page.wait_for_timeout(interval)
        deadline -= interval
        elapsed += interval

    save_pyautogui_screenshot("student_query_result_unknown")
    raise RuntimeError("学号查询后页面状态不明，未看到用户列表或宽带输入框")


def is_user_list_visible(page):
    if has_visible_text(page, ["用户列表"], prefer_exact_match=True):
        return True
    if USER_LIST_IMAGE_PATH.exists():
        return locate_image_on_screen(USER_LIST_IMAGE_PATH, min_confidence=0.55) is not None
    return False


def wait_for_broadband_fields_ready(page, timeout_ms=8000):
    print("确认宽带账号/密码输入框是否可用")
    deadline = timeout_ms
    interval = 500

    while deadline > 0:
        if is_broadband_field_visible():
            print("已确认宽带输入框可用")
            return
        page.wait_for_timeout(interval)
        deadline -= interval

    save_pyautogui_screenshot("broadband_fields_not_ready")
    raise RuntimeError("未确认宽带输入框可用")


def is_broadband_field_visible():
    account_visible = (
        BROADBAND_ACCOUNT_INPUT_IMAGE_PATH.exists()
        and locate_image_on_screen(BROADBAND_ACCOUNT_INPUT_IMAGE_PATH, min_confidence=0.45) is not None
    )
    password_visible = (
        BROADBAND_PASSWORD_INPUT_IMAGE_PATH.exists()
        and locate_image_on_screen(BROADBAND_PASSWORD_INPUT_IMAGE_PATH, min_confidence=0.45) is not None
    )
    return account_visible or password_visible


def get_next_ticket_for_processing() -> PendingTicket | None:
    cursor = load_ticket_cursor()
    return fetch_next_ticket_from_db(
        after_created_at=cursor.get("created_at"),
        after_id=cursor.get("id"),
    )


def load_ticket_cursor() -> dict:
    if not TICKET_CURSOR_PATH.exists():
        return {}
    try:
        return json.loads(TICKET_CURSOR_PATH.read_text(encoding="utf-8"))
    except Exception as error:
        print("读取工单游标失败，改为从头开始:", error)
        return {}


def save_ticket_cursor(ticket: PendingTicket) -> None:
    TICKET_CURSOR_PATH.parent.mkdir(parents=True, exist_ok=True)
    payload = {
        "id": ticket.id,
        "created_at": ticket.created_at,
    }
    TICKET_CURSOR_PATH.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")


def mark_ticket_completed(ticket: PendingTicket, result_message: str) -> None:
    update_ticket_status_in_db(ticket.id, 3, result_message)


def print_idle_duration(idle_started_at: float) -> None:
    elapsed_seconds = int(time.monotonic() - idle_started_at)
    hours = elapsed_seconds // 3600
    minutes = (elapsed_seconds % 3600) // 60
    seconds = elapsed_seconds % 60
    print(f"已 {hours} 小时 {minutes} 分 {seconds} 秒没有工单")


def fill_input_by_image(page, image_path, label, value, timeout_ms=20000, min_confidence=0.45, click_x_ratio=0.75):
    if pyautogui is None:
        raise RuntimeError("未安装 PyAutoGUI，无法通过图片填写输入框")
    if not image_path.exists():
        raise RuntimeError(f"缺少{label}图片模板: {image_path}")

    print(f"开始定位{label} [{image_path.name}]")
    deadline = timeout_ms
    interval = 400

    while deadline > 0:
        match = locate_image_on_screen(image_path, min_confidence=min_confidence)
        if match:
            target_x = int(round(match.left + match.width * click_x_ratio))
            target_y = int(round(match.top + match.height / 2))
            screen_size = pyautogui.size()
            target_x = max(1, min(screen_size.width - 1, target_x))
            target_y = max(1, min(screen_size.height - 1, target_y))
            print(f"已定位{label}，输入坐标: ({target_x}, {target_y})")
            pyautogui.moveTo(target_x, target_y, duration=0.2)
            pyautogui.click(x=target_x, y=target_y)
            page.wait_for_timeout(200)
            pyautogui.hotkey("ctrl", "a")
            page.wait_for_timeout(120)
            pyautogui.press("backspace")
            page.wait_for_timeout(120)
            pyautogui.write(value, interval=0.03)
            page.wait_for_timeout(300)
            print(f"已填写{label}")
            return

        page.wait_for_timeout(interval)
        deadline -= interval

    save_pyautogui_screenshot(f"{label}_not_found")
    raise RuntimeError(f"未找到{label}: {image_path}")


def scroll_profile_form_to_bottom(page):
    if pyautogui is None:
        raise RuntimeError("未安装 PyAutoGUI，无法滚动页面")

    print("开始下滑到宽带信息区域")
    try:
        page.mouse.wheel(0, 1200)
        page.wait_for_timeout(600)
        page.mouse.wheel(0, 1200)
        page.wait_for_timeout(600)
    except Exception:
        pyautogui.scroll(-1200)
        page.wait_for_timeout(600)
        pyautogui.scroll(-1200)
        page.wait_for_timeout(600)
    print("已下滑页面")


def confirm_profile_submission(page):
    print("等待提交结果弹窗加载")
    deadline = 15000
    interval = 500

    while deadline > 0:
        bound_match = locate_result_template(ACCOUNT_BOUND_WARNING_IMAGE_PATH, "账号已绑定提示", min_confidence=0.55)
        if bound_match or has_visible_text(page, ["该账号已被其他用户绑定", "请填写其他未绑定账号"]):
            print("检测到账号已被其他用户绑定提示")
            click_result_dialog_confirm(page, bound_match)
            return RESULT_CONTACT_STAFF

        success_match = locate_result_template(OPERATION_SUCCESS_IMAGE_PATH, "操作成功提示", min_confidence=0.55)
        if success_match or has_visible_text(page, ["操作成功"]):
            print("检测到操作成功提示")
            click_result_dialog_confirm(page, success_match)
            return RESULT_SUCCESS

        page.wait_for_timeout(interval)
        deadline -= interval

    save_pyautogui_screenshot("profile_submission_result_unknown")
    raise RuntimeError("提交后未识别到操作成功或账号已绑定提示，未写入完成状态")


def locate_result_template(image_path, label, min_confidence=0.55):
    if not image_path.exists():
        print(f"缺少{label}模板: {image_path}")
        return None
    return locate_image_on_screen(image_path, min_confidence=min_confidence)


def click_result_dialog_confirm(page, dialog_match=None):
    if dialog_match and pyautogui is not None:
        target_x = int(round(dialog_match.left + dialog_match.width * 0.82))
        target_y = int(round(dialog_match.top + dialog_match.height * 0.83))
        print(f"点击结果弹窗确认按钮坐标: ({target_x}, {target_y})")
        pyautogui.moveTo(target_x, target_y, duration=0.2)
        pyautogui.click(x=target_x, y=target_y)
        page.wait_for_timeout(800)
        return

    if CONFIRM_BUTTON_IMAGE_PATH.exists():
        print("开始查找确认按钮图片")
        if click_image_center(CONFIRM_BUTTON_IMAGE_PATH, "确认按钮", timeout_ms=4000, min_confidence=0.45):
            print("已通过图片点击确认按钮")
            return

    save_pyautogui_screenshot("result_dialog_confirm_not_found")
    raise RuntimeError("识别到结果弹窗，但未能通过图片点击确认按钮")



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
