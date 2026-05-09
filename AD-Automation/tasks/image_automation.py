# Auto-split from campus_portal_task.py. Keep behavior changes focused.

from pathlib import Path
import time

import numpy as np

try:
    import cv2
except Exception:
    cv2 = None

try:
    import pyautogui
except Exception:
    pyautogui = None

try:
    import pyscreeze
except Exception:
    pyscreeze = None


def click_image_center(image_path, label, timeout_ms=10000, region=None, min_confidence=0.62):
    if pyautogui is None:
        return False
    if not image_path.exists():
        print(f"缺少{label}图片模板: {image_path.name}")
        return False

    print(f"开始定位{label} [{image_path.name}]")
    deadline = timeout_ms
    total_timeout_ms = timeout_ms
    interval = 300
    next_progress_ms = total_timeout_ms
    while deadline > 0:
        match = locate_image_on_screen(image_path, region=region, min_confidence=min_confidence)
        if match:
            center = pyautogui.center(match)
            print(f"已定位{label}，点击坐标: ({center.x}, {center.y})")
            pyautogui.moveTo(center.x, center.y, duration=0.2)
            pyautogui.mouseDown(x=center.x, y=center.y)
            page_sleep(60)
            pyautogui.mouseUp(x=center.x, y=center.y)
            page_wait_sleep()
            return True

        if deadline <= next_progress_ms:
            print(f"等待{label}中，剩余约 {format_remaining_seconds(deadline)} 秒")
            next_progress_ms -= 2000
        page_sleep(interval)
        deadline -= interval

    print(f"等待{label}超时，未匹配到模板")
    save_pyautogui_screenshot(f"{label}_not_found")
    return False


def page_sleep(milliseconds):
    time.sleep(milliseconds / 1000)


def page_wait_sleep():
    page_sleep(300)


def locate_image_on_screen(image_path, region=None, min_confidence=0.55):
    match = locate_image_on_screen_with_opencv(image_path, region=region, min_confidence=min_confidence)
    if match:
        return match

    for confidence in (0.82, 0.72, 0.62, 0.55):
        if confidence < min_confidence:
            continue
        try:
            match = pyautogui.locateOnScreen(str(image_path), confidence=confidence, grayscale=True, region=region)
        except TypeError:
            try:
                match = pyautogui.locateOnScreen(str(image_path), region=region)
            except Exception:
                match = None
        except Exception:
            match = None

        if match:
            print(f"PyAutoGUI 匹配成功，score={confidence:.2f}")
            return match

    return None


def locate_image_on_screen_with_opencv(image_path, region=None, min_confidence=0.55):
    if cv2 is None or pyautogui is None or pyscreeze is None:
        return None

    try:
        screenshot = pyautogui.screenshot(region=region)
        screen_bgr = cv2.cvtColor(np.array(screenshot), cv2.COLOR_RGB2BGR)
        screen_gray = cv2.cvtColor(screen_bgr, cv2.COLOR_BGR2GRAY)
        template_bgr = cv2.imread(str(image_path), cv2.IMREAD_COLOR)
        if template_bgr is None:
            print("OpenCV 无法读取模板图片:", image_path)
            return None
        template_gray = cv2.cvtColor(template_bgr, cv2.COLOR_BGR2GRAY)
    except Exception as error:
        print(f"OpenCV 截图或读取模板失败 {image_path}: {error}")
        return None

    best_score = -1.0
    best_box = None
    scales = (0.85, 0.92, 1.0, 1.08, 1.15)

    for scale in scales:
        if scale == 1.0:
            scaled_template = template_gray
        else:
            width = max(1, int(round(template_gray.shape[1] * scale)))
            height = max(1, int(round(template_gray.shape[0] * scale)))
            scaled_template = cv2.resize(template_gray, (width, height), interpolation=cv2.INTER_LINEAR)

        template_height, template_width = scaled_template.shape[:2]
        screen_height, screen_width = screen_gray.shape[:2]
        if template_width > screen_width or template_height > screen_height:
            continue

        result = cv2.matchTemplate(screen_gray, scaled_template, cv2.TM_CCOEFF_NORMED)
        _, score, _, location = cv2.minMaxLoc(result)
        if score > best_score:
            left = location[0] + (region[0] if region else 0)
            top = location[1] + (region[1] if region else 0)
            best_score = float(score)
            best_box = pyscreeze.Box(left, top, template_width, template_height)

    if best_box and best_score >= min_confidence:
        print(f"OpenCV 模板匹配成功，score={best_score:.3f}")
        return best_box

    return None


def save_pyautogui_screenshot(name):
    if pyautogui is None:
        return
    try:
        output_dir = Path(__file__).resolve().parents[1] / "output"
        output_dir.mkdir(parents=True, exist_ok=True)
        output_path = output_dir / f"{name}.png"
        pyautogui.screenshot(str(output_path))
        print("已保存调试截图:", output_path)
    except Exception as error:
        print("保存屏幕截图失败:", error)


def format_remaining_seconds(milliseconds):
    return max(0, (milliseconds + 999) // 1000)
