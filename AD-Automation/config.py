import os
from pathlib import Path
from dotenv import load_dotenv

BASE_DIR = Path(__file__).resolve().parent
load_dotenv(BASE_DIR / ".env")

BACKEND_URL = os.getenv("BACKEND_URL", "http://localhost:8080")
TARGET_URL = os.getenv("TARGET_URL", "https://blj.ahut.edu.cn/bhost/")
ACCOUNT_INPUT_IMAGE_PATH = BASE_DIR / os.getenv("ACCOUNT_INPUT_IMAGE_PATH", "assets/account_input.png")
DX_CANDIDATE_IMAGE_PATH = BASE_DIR / os.getenv("DX_CANDIDATE_IMAGE_PATH", "assets/dx_candidate.png")
CAPTCHA_INPUT_IMAGE_PATH = BASE_DIR / os.getenv("CAPTCHA_INPUT_IMAGE_PATH", "assets/captcha_input.png")
REMOTE_LOGIN_BUTTON_IMAGE_PATH = BASE_DIR / os.getenv("REMOTE_LOGIN_BUTTON_IMAGE_PATH", "assets/remote_login_button.png")
REMOTE_AVATAR_IMAGE_PATH = BASE_DIR / os.getenv("REMOTE_AVATAR_IMAGE_PATH", "assets/remote_user_avatar.png")
REMOTE_SYSTEM_MENU_IMAGE_PATH = BASE_DIR / os.getenv("REMOTE_SYSTEM_MENU_IMAGE_PATH", "assets/billing_system_entry.png")
REMOTE_BUSINESS_MENU_IMAGE_PATH = BASE_DIR / os.getenv("REMOTE_BUSINESS_MENU_IMAGE_PATH", "assets/business_management_menu.png")
REMOTE_EDIT_PROFILE_IMAGE_PATH = BASE_DIR / os.getenv("REMOTE_EDIT_PROFILE_IMAGE_PATH", "assets/edit_profile_menu.png")
HEADLESS = os.getenv("HEADLESS", "false").lower() == "true"
BROWSER_CHANNEL = os.getenv("BROWSER_CHANNEL", "chrome")
CHROME_CDP_URL = os.getenv("CHROME_CDP_URL", "")
AUTH_STATE_PATH = BASE_DIR / os.getenv("AUTH_STATE_PATH", ".auth/storage_state.json")
USE_AUTH_STATE = os.getenv("USE_AUTH_STATE", "false").lower() == "true"
PORTAL_USERNAME = os.getenv("PORTAL_USERNAME", "")
PORTAL_PASSWORD = os.getenv("PORTAL_PASSWORD", "")
