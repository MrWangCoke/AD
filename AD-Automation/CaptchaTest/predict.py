import os
from pathlib import Path

import numpy as np
import torch
import torch.nn as nn
from PIL import Image
from torchvision import transforms


FOREGROUND_MIN_VALUE = 225
FOREGROUND_MIN_DELTA = 15


class CaptchaCNN(nn.Module):
    def __init__(self, feature_width: int):
        super().__init__()
        self.features = nn.Sequential(
            nn.Conv2d(1, 32, kernel_size=3, padding=1),
            nn.BatchNorm2d(32),
            nn.ReLU(),
            nn.MaxPool2d(2),
            nn.Conv2d(32, 64, kernel_size=3, padding=1),
            nn.BatchNorm2d(64),
            nn.ReLU(),
            nn.MaxPool2d(2),
            nn.Conv2d(64, 128, kernel_size=3, padding=1),
            nn.BatchNorm2d(128),
            nn.ReLU(),
            nn.MaxPool2d(2),
        )
        self.classifier = nn.Sequential(
            nn.Flatten(),
            nn.Linear(128 * 5 * feature_width, 512),
            nn.ReLU(),
            nn.Dropout(0.3),
            nn.Linear(512, 4 * 10),
        )

    def forward(self, x):
        x = self.features(x)
        x = self.classifier(x)
        return x.view(-1, 4, 10)


CURRENT_DIR = Path(__file__).resolve().parent
MODEL_PATH = CURRENT_DIR / "captcha_cnn.pth"

device = torch.device("cpu")
print("正在加载模型...")
state_dict = torch.load(MODEL_PATH, map_location=device, weights_only=True)

classifier_input_dim = state_dict["classifier.1.weight"].shape[1]
if classifier_input_dim == 128 * 5 * 16:
    TARGET_WIDTH = 128
    TARGET_HEIGHT = 44
    FEATURE_WIDTH = 16
elif classifier_input_dim == 128 * 5 * 15:
    TARGET_WIDTH = 120
    TARGET_HEIGHT = 40
    FEATURE_WIDTH = 15
else:
    raise RuntimeError(
        f"不支持的模型结构，classifier.1.weight 维度是 {classifier_input_dim}，"
        "当前只识别 128*5*16 和 128*5*15 两种结构。"
    )

model = CaptchaCNN(feature_width=FEATURE_WIDTH).to(device)
model.load_state_dict(state_dict)
model.eval()
print(f"✅ 模型加载成功！当前输入尺寸: {TARGET_WIDTH}x{TARGET_HEIGHT}")

transform = transforms.Compose([
    transforms.Grayscale(num_output_channels=1),
    transforms.Resize((TARGET_HEIGHT, TARGET_WIDTH)),
    transforms.ToTensor(),
    transforms.Normalize(mean=[0.5], std=[0.5]),
])


def tighten_captcha_image(image: Image.Image, padding: int = 2) -> Image.Image:
    rgb = image.convert("RGB")
    image_array = np.array(rgb)
    pixel_min = image_array.min(axis=2)
    pixel_delta = image_array.max(axis=2) - pixel_min
    foreground_mask = (pixel_min < FOREGROUND_MIN_VALUE) & (pixel_delta > FOREGROUND_MIN_DELTA)
    foreground_points = np.argwhere(foreground_mask)

    if foreground_points.size == 0:
        return rgb

    top, left = foreground_points.min(axis=0)
    bottom, right = foreground_points.max(axis=0)

    left = max(0, int(left) - padding)
    top = max(0, int(top) - padding)
    right = min(rgb.width - 1, int(right) + padding)
    bottom = min(rgb.height - 1, int(bottom) + padding)
    return rgb.crop((left, top, right + 1, bottom + 1))


def preprocess_image(image: Image.Image) -> Image.Image:
    rgb = image.convert("RGB")
    if rgb.size != (TARGET_WIDTH, TARGET_HEIGHT):
        rgb = tighten_captcha_image(rgb, padding=2)
    return rgb


def predict(image_path: str | os.PathLike) -> str | None:
    image_path = str(image_path)
    print(f"正在识别图片: {image_path} ...")
    try:
        image = Image.open(image_path).convert("RGB")
        processed = preprocess_image(image)
        img_tensor = transform(processed).unsqueeze(0).to(device)

        with torch.no_grad():
            output = model(img_tensor)
            pred = output.argmax(dim=2).squeeze(0)

        return "".join(str(int(i)) for i in pred)
    except Exception as error:
        print(f"❌ 识别图片时出错: {error}")
        return None


if __name__ == "__main__":
    test_image_path = CURRENT_DIR / "test.png"
    if not test_image_path.exists():
        print(f"❌ 找不到测试图片！请确保 {test_image_path} 存在。")
    else:
        answer = predict(test_image_path)
        if answer:
            print("\n" + "=" * 30)
            print(f"🎉 最终识别结果是: 【 {answer} 】")
            print("=" * 30 + "\n")
