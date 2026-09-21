"""Generates legacy launcher icons (mipmap-*/ic_launcher.png) from the first cat sprite. Run from the repo root."""
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
CAT = ROOT / "public" / "assets" / "cats" / "basic" / "01.png"
RES = ROOT / "android" / "app" / "src" / "main" / "res"
SIZES = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
CREAM = (247, 239, 227, 255)

cat = Image.open(CAT).convert("RGBA")
for density, size in SIZES.items():
    icon = Image.new("RGBA", (size, size), CREAM)
    inner = int(size * 0.82)
    scaled = cat.copy()
    scaled.thumbnail((inner, inner), Image.LANCZOS)
    icon.alpha_composite(scaled, ((size - scaled.width) // 2, size - scaled.height - int(size * 0.06)))
    out = RES / f"mipmap-{density}"
    out.mkdir(parents=True, exist_ok=True)
    icon.save(out / "ic_launcher.png")
print("ok")
