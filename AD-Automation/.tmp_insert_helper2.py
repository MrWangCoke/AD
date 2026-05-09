from pathlib import Path

path = Path(r'e:/MrWang/Desktop/AD-project/AD-Automation/tasks/remote_drcom_page.py')
text = path.read_text(encoding='utf-8')
needle = '''    print("已保存本次验证码裁剪图:", run_crop_path)
    return {
        "crop_path": run_crop_path,
        "cleanup_paths": [run_crop_path],
    }




def wait_for_captcha_input_image(page, timeout_ms=20000):'''
insert = '''    print("已保存本次验证码裁剪图:", run_crop_path)
    return {
        "crop_path": run_crop_path,
        "cleanup_paths": [run_crop_path],
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


def wait_for_captcha_input_image(page, timeout_ms=20000):'''
if needle not in text:
    raise SystemExit('Needle not found')
text = text.replace(needle, insert)
path.write_text(text, encoding='utf-8')
print('inserted helper successfully')
