"""Cut the supplied transparent sprite sheets into lossless, padded PNG files.

Run from the project root: python tools/slice_assets.py
Requires Pillow and NumPy. Room backgrounds and the finale are copied unchanged.
"""

from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
from shutil import copyfile

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "public" / "assets"


@dataclass
class Component:
    x0: int
    y0: int
    x1: int
    y1: int
    pixels: int

    def absorb(self, other: "Component") -> None:
        self.x0 = min(self.x0, other.x0)
        self.y0 = min(self.y0, other.y0)
        self.x1 = max(self.x1, other.x1)
        self.y1 = max(self.y1, other.y1)
        self.pixels += other.pixels


def connected_components(mask: np.ndarray) -> tuple[list[tuple[int, Component]], np.ndarray]:
    """Label 8-connected alpha runs without looping through every pixel in Python."""
    parent: list[int] = []
    bounds: list[Component] = []
    previous: list[tuple[int, int, int]] = []
    runs: list[tuple[int, int, int, int]] = []

    def find(index: int) -> int:
        while parent[index] != index:
            parent[index] = parent[parent[index]]
            index = parent[index]
        return index

    def union(a: int, b: int) -> None:
        a, b = find(a), find(b)
        if a != b:
            parent[b] = a
            bounds[a].absorb(bounds[b])

    for y, row in enumerate(mask):
        padded = np.pad(row.astype(np.int8), (1, 1))
        starts = np.flatnonzero(np.diff(padded) == 1)
        ends = np.flatnonzero(np.diff(padded) == -1)
        current: list[tuple[int, int, int]] = []
        prev_index = 0
        for x0, x1 in zip(starts.tolist(), ends.tolist()):
            index = len(parent)
            parent.append(index)
            bounds.append(Component(x0, y, x1, y + 1, x1 - x0))
            while prev_index < len(previous) and previous[prev_index][1] < x0 - 1:
                prev_index += 1
            cursor = prev_index
            while cursor < len(previous) and previous[cursor][0] <= x1:
                union(index, previous[cursor][2])
                cursor += 1
            current.append((x0, x1, index))
            runs.append((y, x0, x1, index))
        previous = current

    labels = np.zeros(mask.shape, dtype=np.int32)
    for y, x0, x1, index in runs:
        labels[y, x0:x1] = find(index) + 1
    return [(i + 1, bounds[i]) for i in range(len(parent)) if find(i) == i], labels


# Row shapes describe visual placement in each supplied sheet. Disconnected
# sparkles, holograms and wisps are joined to the nearest main illustration.
SHEETS: list[tuple[str, str, tuple[int, ...]]] = [
    ("Базовые коты.png", "cats/basic", (3, 2)),
    ("Базовые коты спят.png", "cats/basic/sleep", (3, 2)),
    ("Редкие коты.png", "cats/rare", (3, 2)),
    ("Редкие коты спят.png", "cats/rare/sleep", (3, 2)),
    ("Особые коты.png", "cats/special", (3, 2)),
    ("Особые коты спят.png", "cats/special/sleep", (3, 2)),
    ("Супер коты спят.png", "cats/superhero/sleep", (3, 2)),
    ("ИИ коты.png", "cats/ai", (3, 2)),
    ("ИИ коты спят.png", "cats/ai/sleep", (3, 2)),
    ("Корм.png", "food", (2, 2)),
    ("Ресурсы и награды.png", "resources", (3, 2)),
    ("Интерфейсные игровые значки.png", "ui", (3, 2)),
    ("Дверь.png", "ui/door", (1,)),
]

UPGRADE_ROWS = [
    ((3, 2), (3, 2)),
    ((2, 3), (2, 2, 1)),
    ((2, 3), (2, 3)),
    ((3, 2), (2, 3)),
    ((3, 2), (2, 3)),
]
for room_number, (basic_rows, advanced_rows) in enumerate(UPGRADE_ROWS, 1):
    SHEETS.extend([
        (f"Базовые улучшения {room_number}.png", f"upgrades/room-{room_number}/basic", basic_rows),
        (f"Продвинутые улучшения {room_number}.png", f"upgrades/room-{room_number}/advanced", advanced_rows),
    ])


def ordered_main(found: list[tuple[int, Component]], rows: tuple[int, ...]) -> list[tuple[int, Component]]:
    expected = sum(rows)
    main = sorted(found, key=lambda item: item[1].pixels, reverse=True)[:expected]
    if len(main) != expected or main[-1][1].pixels < 5000:
        raise ValueError(f"Expected {expected} sizeable objects, found {len(main)}")
    main.sort(key=lambda item: (item[1].y0 + item[1].y1) / 2)
    ordered: list[tuple[int, Component]] = []
    cursor = 0
    for count in rows:
        ordered.extend(sorted(main[cursor:cursor + count], key=lambda item: item[1].x0))
        cursor += count
    return ordered


def distance_to_box(piece: Component, box: Component) -> int:
    dx = max(box.x0 - piece.x1, piece.x0 - box.x1, 0)
    dy = max(box.y0 - piece.y1, piece.y0 - box.y1, 0)
    return dx * dx + dy * dy


def save_object(image: Image.Image, labels: np.ndarray, ids: list[int], box: Component, target: Path, number: int, padding: int) -> None:
    left = max(0, box.x0 - padding)
    top = max(0, box.y0 - padding)
    right = min(image.width, box.x1 + padding)
    bottom = min(image.height, box.y1 + padding)
    pixels = np.array(image.crop((left, top, right, bottom)))
    object_mask = np.isin(labels[top:bottom, left:right], ids)
    # Retain source antialiasing just outside the alpha threshold.
    for _ in range(8):
        grown = object_mask.copy()
        grown[1:] |= object_mask[:-1]
        grown[:-1] |= object_mask[1:]
        grown[:, 1:] |= object_mask[:, :-1]
        grown[:, :-1] |= object_mask[:, 1:]
        object_mask = grown
    pixels[~object_mask] = 0
    result = Image.new("RGBA", (right - left + padding * 2, bottom - top + padding * 2))
    result.paste(Image.fromarray(pixels, "RGBA"), (padding, padding))
    result.save(target / f"{number:02d}.png", optimize=True)


def clear_numbered(target: Path) -> None:
    target.mkdir(parents=True, exist_ok=True)
    for old in target.glob("*.png"):
        if old.stem.isdecimal():
            old.unlink()


def slice_sheet(source: Path, target: Path, rows: tuple[int, ...], padding: int = 12) -> None:
    image = Image.open(source).convert("RGBA")
    found, labels = connected_components(np.asarray(image.getchannel("A")) >= 8)
    ordered = ordered_main(found, rows)
    owners = {label: [label] for label, _ in ordered}
    boxes = {label: Component(c.x0, c.y0, c.x1, c.y1, c.pixels) for label, c in ordered}
    selected = set(owners)
    for label, piece in found:
        if label in selected or piece.pixels < 100:
            continue
        owner = min(ordered, key=lambda item: (distance_to_box(piece, item[1]), -item[1].pixels))[0]
        owners[owner].append(label)
        boxes[owner].absorb(piece)
    clear_numbered(target)
    for number, (label, _) in enumerate(ordered, 1):
        save_object(image, labels, owners[label], boxes[label], target, number, padding)
    print(f"{source.name}: {len(ordered)} objects -> {target.relative_to(ASSETS)}")


def slice_super_cats(source: Path, target: Path, padding: int = 12) -> None:
    """The first three hero cats touch; split their known sheet cells."""
    image = Image.open(source).convert("RGBA")
    found, labels = connected_components(np.asarray(image.getchannel("A")) >= 8)
    major = sorted((item for item in found if item[1].pixels > 5000), key=lambda item: item[1].y0)
    top = sorted((item for item in major if item[1].y0 < 100), key=lambda item: item[1].x0)
    bottom = sorted((item for item in major if item[1].y0 >= 100), key=lambda item: item[1].x0)
    if len(top) != 2 or len(bottom) != 2:
        raise ValueError("Unexpected superhero sheet layout")
    x = np.arange(image.width)[None, :]
    masks = [
        labels == top[0][0],
        (labels == top[1][0]) & (x < 975),
        (labels == top[1][0]) & (x >= 990),
        labels == bottom[0][0],
        labels == bottom[1][0],
    ]
    clear_numbered(target)
    for number, raw_mask in enumerate(masks, 1):
        mask = raw_mask.copy()
        for _ in range(8):
            grown = mask.copy()
            grown[1:] |= mask[:-1]
            grown[:-1] |= mask[1:]
            grown[:, 1:] |= mask[:, :-1]
            grown[:, :-1] |= mask[:, 1:]
            mask = grown
        pixels = np.array(image)
        pixels[~mask] = 0
        cropped = Image.fromarray(pixels, "RGBA")
        bbox = cropped.getchannel("A").getbbox()
        if bbox is None:
            raise ValueError(f"Empty superhero cell {number}")
        result = Image.new("RGBA", (bbox[2] - bbox[0] + padding * 2, bbox[3] - bbox[1] + padding * 2))
        result.paste(cropped.crop(bbox), (padding, padding))
        result.save(target / f"{number:02d}.png", optimize=True)
    print(f"{source.name}: 5 objects -> {target.relative_to(ASSETS)}")


def main() -> None:
    for filename, folder, rows in SHEETS:
        slice_sheet(ROOT / filename, ASSETS / folder, rows)
    slice_super_cats(ROOT / "Супер коты.png", ASSETS / "cats/superhero")
    backgrounds = ASSETS / "backgrounds"
    backgrounds.mkdir(parents=True, exist_ok=True)
    for number in range(1, 6):
        copyfile(ROOT / f"Фон {number}.png", backgrounds / f"room-{number}-day.png")
        copyfile(ROOT / f"Фон {number} ночь.png", backgrounds / f"room-{number}-night.png")
        copyfile(ROOT / f"Фон match-3 {number}.png", backgrounds / f"match3-room-{number}.png")
    copyfile(ROOT / "Финал.png", ASSETS / "ui/final.png")
    # Remove only obsolete outputs from the first version of the slicer.
    for folder_name in ("upgrades/basic", "upgrades/advanced"):
        folder = ASSETS / folder_name
        for old in folder.glob("[0-9][0-9].png"):
            old.unlink()
        if folder.exists() and not any(folder.iterdir()):
            folder.rmdir()
    for number in range(1, 4):
        (backgrounds / f"room-{number}.png").unlink(missing_ok=True)
    print("Room and match-3 backgrounds plus finale copied unchanged.")


if __name__ == "__main__":
    main()
