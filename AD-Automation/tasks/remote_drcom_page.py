# Auto-split from campus_portal_task.py. Keep behavior changes focused.

import importlib.util
import json
import time
from datetime import datetime

from config import (
    ACCOUNT_INPUT_IMAGE_PATH,
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
    STUDENT_ID_INPUT_IMAGE_PATH,
    TICKET_CURSOR_PATH,
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
        manual_captcha_code = retry_remote_login_after_captcha_failure(page)
        if not wait_for_remote_login_transition(page):
            raise RuntimeError("重新输入验证码后页面仍未跳转，请检查验证码是否正确")
    wait_for_remote_avatar_visible(page)
    cleanup_captcha_images(cleanup_paths)
    navigate_remote_profile_menu(page)


def wait_for_remote_login_transition(page, timeout_ms=8000):
    print("检查验证码提交结果")
    deadline = timeout_ms
    interval = 800

    while deadline > 0:
        if is_remote_profile_entry_visible():
            print("验证码提交成功，页面已继续")
            return True
        if not is_remote_captcha_still_visible():
            print("验证码页已消失，继续后续流程")
            return True
        page.wait_for_timeout(interval)
        deadline -= interval

    return False


def is_remote_profile_entry_visible():
    if not REMOTE_AVATAR_IMAGE_PATH.exists():
        return False
    return locate_image_on_screen(REMOTE_AVATAR_IMAGE_PATH, min_confidence=0.55) is not None


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
            "dom_texts": ["认证计费管理系统"],
            "min_confidence": 0.48,
        },
        {
            "image_path": REMOTE_BUSINESS_MENU_IMAGE_PATH,
            "label": "业务管理",
            "timeout_ms": 45000,
            "dom_texts": ["业务管理"],
            "min_confidence": 0.45,
        },
        {
            "image_path": REMOTE_EDIT_PROFILE_IMAGE_PATH,
            "label": "修改资料",
            "timeout_ms": 45000,
            "dom_texts": ["修改资料"],
            "min_confidence": 0.45,
        },
    ]

    for step in steps:
        label = step["label"]
        image_path = step["image_path"]
        timeout_ms = step["timeout_ms"]
        dom_texts = step["dom_texts"]
        min_confidence = step.get("min_confidence", 0.62)

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
        refresh_edit_profile_page(page)
        process_profile_update_ticket(page, ticket)
        mark_ticket_completed(ticket)
        save_ticket_cursor(ticket)
        print(f"工单处理完成: id={ticket.id}")


def process_profile_update_ticket(page, ticket: PendingTicket):
    if not ticket.student_id:
        raise RuntimeError(f"工单 {ticket.id} 的 student_id 为空")
    if not ticket.broadband_account:
        raise RuntimeError(f"工单 {ticket.id} 的 broadband_account 为空")
    if not ticket.new_password:
        raise RuntimeError(f"工单 {ticket.id} 的 new_password 为空")

    fill_input_by_image(page, STUDENT_ID_INPUT_IMAGE_PATH, "学号输入框", ticket.student_id, click_x_ratio=0.78)
    click_image_center(QUERY_BUTTON_IMAGE_PATH, "查询按钮", timeout_ms=15000, min_confidence=0.45)
    print("已点击查询按钮，等待页面加载")
    page.wait_for_timeout(2000)

    scroll_profile_form_to_bottom(page)
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
    confirm_profile_submission(page)


def refresh_edit_profile_page(page):
    if click_visible_text_by_dom(
        page,
        ["修改资料"],
        prefer_clickable_ancestor=True,
        raise_on_missing=False,
        prefer_exact_match=True,
    ):
        print("已点击修改资料")
        page.wait_for_timeout(800)
        return

    if REMOTE_EDIT_PROFILE_IMAGE_PATH.exists():
        if click_image_center(REMOTE_EDIT_PROFILE_IMAGE_PATH, "修改资料", timeout_ms=1500, min_confidence=0.45):
            print("已点击修改资料")
            page.wait_for_timeout(800)
            return

    print("当前未重新点击修改资料，继续使用现有页面")


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


def mark_ticket_completed(ticket: PendingTicket) -> None:
    update_ticket_status_in_db(ticket.id, 3, "自动化处理完成")


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
    print("等待确认弹窗加载")
    page.wait_for_timeout(2500)

    if CONFIRM_BUTTON_IMAGE_PATH.exists():
        print("开始查找确认按钮图片")
        if click_image_center(CONFIRM_BUTTON_IMAGE_PATH, "确认按钮", timeout_ms=12000, min_confidence=0.45):
            print("已通过图片点击确认按钮")
            return

    print("图片未找到确认按钮，尝试通过文字点击")
    if click_visible_text_by_dom(
        page,
        ["确认", "确定"],
        prefer_clickable_ancestor=True,
        raise_on_missing=False,
        prefer_exact_match=True,
    ):
        print("已通过 DOM 点击确认按钮")
        return

    raise RuntimeError("未找到弹出的确认按钮，请补充确认按钮图片或检查弹窗是否出现")



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
