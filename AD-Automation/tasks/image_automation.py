# Auto-split from campus_portal_task.py. Keep behavior changes focused.

from pathlib import Path
import time

try:
    import pyautogui
except Exception:
    pyautogui = None



def click_image_center(image_path, label, timeout_ms=10000):
    if pyautogui is None:
        return False
    if not image_path.exists():
        print(f"未找到{label}图片模板，请保存到:", image_path)
        return False

    print(f"开始根据图片查找{label}:", image_path)
    deadline = timeout_ms
    interval = 300
    while deadline > 0:
        match = locate_image_on_screen(image_path)
        if match:
            center = pyautogui.center(match)
            print(f"已根据图片找到{label}，准备点击: ({center.x}, {center.y})")
            pyautogui.moveTo(center.x, center.y, duration=0.2)
            pyautogui.click(x=center.x, y=center.y)
            page_wait_sleep()
            return True

        page_sleep(interval)
        deadline -= interval

    save_pyautogui_screenshot(f"{label}_not_found")
    return False



def page_sleep(milliseconds):
    time.sleep(milliseconds / 1000)



def page_wait_sleep():
    page_sleep(300)



def locate_image_on_screen(image_path):
    for confidence in (0.82, 0.72, 0.62, 0.55):
        try:
            match = pyautogui.locateOnScreen(str(image_path), confidence=confidence, grayscale=True)
        except TypeError:
            match = pyautogui.locateOnScreen(str(image_path))
        except Exception as error:
            print(f"图片识别失败 {image_path}: {error}")
            return None

        if match:
            print(f"图片匹配成功，confidence={confidence}")
            return match

    return None



def save_pyautogui_screenshot(name):
    if pyautogui is None:
        return
    try:
        output_dir = Path(__file__).resolve().parents[1] / "output"
        output_dir.mkdir(parents=True, exist_ok=True)
        output_path = output_dir / f"{name}.png"
        pyautogui.screenshot(str(output_path))
        print("已保存当前屏幕截图:", output_path)
    except Exception as error:
        print("保存屏幕截图失败:", error)
