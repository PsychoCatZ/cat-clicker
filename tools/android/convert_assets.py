"""Converts the web version's PNG assets (public/assets) to WebP drawables for the Android app.

Usage (from the repository root):
    python tools/android/convert_assets.py

Requires Pillow (already listed in tools/requirements.txt).
Output: android/app/src/main/res/drawable-nodpi/*.webp
Naming (all lowercase, [a-z0-9_] only, as Android resources require):
    cats/<group>/<nn>.png            -> cat_<group>_<nn>.webp
    cats/<group>/sleep/<nn>.png      -> cat_<group>_<nn>_sleep.webp
    backgrounds/room-<n>-day.png     -> room_<n>_day.webp      (and _night)
    backgrounds/match3-room-<n>.png  -> match3_room_<n>.webp
    upgrades/room-<n>/<tier>/<nn>    -> upgrade_r<n>_<tier>_<nn>.webp
    resources/<nn>, food/<nn>, ui/<nn> -> resource_<nn>, food_<nn>, ui_<nn>
    ui/door/01.png -> ui_door.webp,  ui/final.png -> ui_final.webp
"""
from pathlib import Path
import sys
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
SRC = ROOT / "public" / "assets"
OUT = ROOT / "android" / "app" / "src" / "main" / "res" / "drawable-nodpi"

# Opaque scenery is lossy (much smaller); sprites with transparency stay lossless so edges are not haloed.
LOSSY_QUALITY = 88


def resource_name(rel: Path) -> str:
    parts = list(rel.with_suffix("").parts)
    head = parts[0]
    if head == "cats":
        group = parts[1]
        if parts[2] == "sleep":
            return f"cat_{group}_{parts[3]}_sleep"
        return f"cat_{group}_{parts[2]}"
    if head == "backgrounds":
        name = parts[1].replace("-", "_")
        return name  # room_1_day, room_1_night, match3_room_1
    if head == "upgrades":
        room = parts[1].split("-")[1]
        return f"upgrade_r{room}_{parts[2]}_{parts[3]}"
    if head in ("resources", "food"):
        return f"{head[:-1] if head == 'resources' else head}_{parts[1]}"
    if head == "ui":
        if parts[1] == "door":
            return "ui_door"
        if parts[1] == "final":
            return "ui_final"
        return f"ui_{parts[1]}"
    raise ValueError(f"Unmapped asset: {rel}")


def has_transparency(image: Image.Image) -> bool:
    if image.mode != "RGBA":
        return False
    return image.getchannel("A").getextrema()[0] < 255


def main() -> int:
    OUT.mkdir(parents=True, exist_ok=True)
    total_in = total_out = 0
    seen: dict[str, Path] = {}
    for png in sorted(SRC.rglob("*.png")):
        rel = png.relative_to(SRC)
        name = resource_name(rel)
        if name in seen:
            print(f"Name clash: {name} <- {rel} and {seen[name]}", file=sys.stderr)
            return 1
        seen[name] = rel
        image = Image.open(png)
        target = OUT / f"{name}.webp"
        if has_transparency(image):
            image.save(target, "WEBP", lossless=True, quality=100, method=6)
        else:
            image.convert("RGB").save(target, "WEBP", quality=LOSSY_QUALITY, method=6)
        total_in += png.stat().st_size
        total_out += target.stat().st_size
    print(f"{len(seen)} files: {total_in / 1e6:.1f} MB -> {total_out / 1e6:.1f} MB")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
