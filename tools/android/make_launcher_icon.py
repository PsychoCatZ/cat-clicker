"""Generates the launcher icons from the first cat sprite. Run from the repository root:  python tools/android/make_launcher_icon.py

Writes
  android/app/src/main/res/mipmap-<density>/ic_launcher.png      legacy square icons (Android 7 and older)
  android/app/src/main/res/drawable-nodpi/ic_launcher_foreground.png   foreground layer of the adaptive icon
The adaptive icon itself (mipmap-anydpi-v26/ic_launcher.xml) is a small XML that combines this foreground with a
plain cream background colour. Only the middle two thirds of an adaptive icon are guaranteed to stay visible,
so the cat is scaled to fit there.
"""
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
CAT = ROOT / "public" / "assets" / "cats" / "basic" / "01.png"
RES = ROOT / "android" / "app" / "src" / "main" / "res"
SIZES = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
CREAM = (247, 239, 227, 255)

cat = Image.open(CAT).convert("RGBA")

# Legacy square icons.
for density, size in SIZES.items():
    icon = Image.new("RGBA", (size, size), CREAM)
    inner = int(size * 0.82)
    scaled = cat.copy()
    scaled.thumbnail((inner, inner), Image.LANCZOS)
    icon.alpha_composite(scaled, ((size - scaled.width) // 2, size - scaled.height - int(size * 0.06)))
    out = RES / f"mipmap-{density}"
    out.mkdir(parents=True, exist_ok=True)
    icon.save(out / "ic_launcher.png")

# Adaptive foreground: 108 dp canvas drawn at xxxhdpi (432 px); the cat stays inside the safe zone.
canvas = Image.new("RGBA", (432, 432), (0, 0, 0, 0))
safe = int(432 * 0.62)
scaled = cat.copy()
scaled.thumbnail((safe, safe), Image.LANCZOS)
canvas.alpha_composite(scaled, ((432 - scaled.width) // 2, (432 - scaled.height) // 2))
target = RES / "drawable-nodpi"
target.mkdir(parents=True, exist_ok=True)
canvas.save(target / "ic_launcher_foreground.png")
print("ok")
