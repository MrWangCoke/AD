import os
from pathlib import Path
from dotenv import load_dotenv

BASE_DIR = Path(__file__).resolve().parent
load_dotenv(BASE_DIR / ".env")

BACKEND_URL = os.getenv("BACKEND_URL", "http://localhost:8080")
RUN_MODE = os.getenv("RUN_MODE", "tickets").strip().lower()
TICKET_SOURCE = os.getenv("TICKET_SOURCE", "api").strip().lower()
DATABASE_URL = os.getenv("DATABASE_URL", "")
DATABASE_USERNAME = os.getenv("DATABASE_USERNAME", "")
DATABASE_PASSWORD = os.getenv("DATABASE_PASSWORD", "")
TARGET_URL = os.getenv("TARGET_URL", "https://blj.ahut.edu.cn/bhost/")
ACCOUNT_INPUT_IMAGE_PATH = BASE_DIR / os.getenv("ACCOUNT_INPUT_IMAGE_PATH", "assets/account_input.png")
DX_CANDIDATE_IMAGE_PATH = BASE_DIR / os.getenv("DX_CANDIDATE_IMAGE_PATH", "assets/dx_candidate.png")
CAPTCHA_INPUT_IMAGE_PATH = BASE_DIR / os.getenv("CAPTCHA_INPUT_IMAGE_PATH", "assets/captcha_input.png")
REMOTE_LOGIN_BUTTON_IMAGE_PATH = BASE_DIR / os.getenv("REMOTE_LOGIN_BUTTON_IMAGE_PATH", "assets/remote_login_button.png")
REMOTE_AVATAR_IMAGE_PATH = BASE_DIR / os.getenv("REMOTE_AVATAR_IMAGE_PATH", "assets/remote_user_avatar.png")
REMOTE_SYSTEM_MENU_IMAGE_PATH = BASE_DIR / os.getenv("REMOTE_SYSTEM_MENU_IMAGE_PATH", "assets/billing_system_entry.png")
REMOTE_BUSINESS_MENU_IMAGE_PATH = BASE_DIR / os.getenv("REMOTE_BUSINESS_MENU_IMAGE_PATH", "assets/business_management_menu.png")
REMOTE_EDIT_PROFILE_IMAGE_PATH = BASE_DIR / os.getenv("REMOTE_EDIT_PROFILE_IMAGE_PATH", "assets/edit_profile_menu.png")
STUDENT_ID_INPUT_IMAGE_PATH = BASE_DIR / os.getenv("STUDENT_ID_INPUT_IMAGE_PATH", "assets/student_id_input.png")
QUERY_BUTTON_IMAGE_PATH = BASE_DIR / os.getenv("QUERY_BUTTON_IMAGE_PATH", "assets/query_button.png")
BROADBAND_ACCOUNT_INPUT_IMAGE_PATH = BASE_DIR / os.getenv("BROADBAND_ACCOUNT_INPUT_IMAGE_PATH", "assets/broadband_account_input.png")
BROADBAND_PASSWORD_INPUT_IMAGE_PATH = BASE_DIR / os.getenv("BROADBAND_PASSWORD_INPUT_IMAGE_PATH", "assets/broadband_password_input.png")
CONFIRM_BUTTON_IMAGE_PATH = BASE_DIR / os.getenv("CONFIRM_BUTTON_IMAGE_PATH", "assets/confirm_button.png")
HEADLESS = os.getenv("HEADLESS", "false").lower() == "true"
BROWSER_CHANNEL = os.getenv("BROWSER_CHANNEL", "chrome")
CHROME_CDP_URL = os.getenv("CHROME_CDP_URL", "")
AUTH_STATE_PATH = BASE_DIR / os.getenv("AUTH_STATE_PATH", ".auth/storage_state.json")
USE_AUTH_STATE = os.getenv("USE_AUTH_STATE", "false").lower() == "true"
PORTAL_USERNAME = os.getenv("PORTAL_USERNAME", "")
PORTAL_PASSWORD = os.getenv("PORTAL_PASSWORD", "")
