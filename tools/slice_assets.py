"""Split transparent sprite sheets into individual, lossless PNG files.

Run from the project root with: python tools/slice_assets.py
Requires Pillow and NumPy. Backgrounds are copied without cropping.
"""

from __future__ import annotations

import argparse
from dataclasses import dataclass
from pathlib import Path
from shutil import copyfile

import numpy as np
from PIL import Image


ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "public" / "assets"
SHEETS = {
    "Базовые коты.png": "cats/basic",
    "Редкие коты.png": "cats/rare",
    "Особые коты.png": "cats/special",
    "Базовые улучшения.png": "upgrades/basic",
    "Продвинутые улучшения.png": "upgrades/advanced",
    "Интерфейсные игровые значки.png": "ui",
    "Ресурсы и награды.png": "resources",
}


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
    """Label 8-connected foreground runs, avoiding a Python loop per pixel."""
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


def sort_in_rows(components: list[Component]) -> list[Component]:
    if not components:
        return []
    median_height = float(np.median([c.y1 - c.y0 for c in components]))
    row_tolerance = median_height * 0.4
    rows: list[list[Component]] = []
    for component in sorted(components, key=lambda c: (c.y0, c.x0)):
        center = (component.y0 + component.y1) / 2
        for row in rows:
            row_center = sum((item.y0 + item.y1) / 2 for item in row) / len(row)
            if abs(center - row_center) <= row_tolerance:
                row.append(component)
                break
        else:
            rows.append([component])
    return [component for row in rows for component in sorted(row, key=lambda c: c.x0)]


def slice_sheet(source: Path, target: Path, alpha_threshold: int, min_pixels: int, padding: int) -> None:
    image = Image.open(source).convert("RGBA")
    alpha = np.asarray(image.getchannel("A"))
    found, labels = connected_components(alpha >= alpha_threshold)
    major = [(label, component) for label, component in found if component.pixels >= min_pixels]
    # Tiny decorative pieces can be disconnected from the main illustration.
    # Attach those enclosed by a larger object's box; keep their real alpha.
    for small_label, small in list(major):
        containing = [
            (label, large) for label, large in major
            if label != small_label and large.pixels > small.pixels * 5
            and large.x0 <= small.x0 and large.y0 <= small.y0
            and large.x1 >= small.x1 and large.y1 >= small.y1
        ]
        if containing:
            owner_label, owner = min(containing, key=lambda item: item[1].pixels)
            labels[labels == small_label] = owner_label
            owner.pixels += small.pixels
            major.remove((small_label, small))
    ordered = sort_in_rows([component for _, component in major])
    components = [(next(label for label, item in major if item is component), component) for component in ordered]
    target.mkdir(parents=True, exist_ok=True)
    for existing in target.glob("*.png"):
        if existing.stem.isdecimal():
            existing.unlink()
    print(f"{source.name}: {len(components)} objects")
    for number, (label, component) in enumerate(components, 1):
        box = (
            max(0, component.x0 - padding),
            max(0, component.y0 - padding),
            min(image.width, component.x1 + padding),
            min(image.height, component.y1 + padding),
        )
        pixels = np.array(image.crop(box))
        mask_for_object = labels[box[1]:box[3], box[0]:box[2]] == label
        # Keep all original edge alpha around the detected foreground.
        # An 8 px dilation admits antialiasing but excludes neighboring sprites.
        for _ in range(8):
            grown = mask_for_object.copy()
            grown[1:] |= mask_for_object[:-1]
            grown[:-1] |= mask_for_object[1:]
            grown[:, 1:] |= mask_for_object[:, :-1]
            grown[:, :-1] |= mask_for_object[:, 1:]
            mask_for_object = grown
        pixels[~mask_for_object] = 0
        result = Image.new("RGBA", (box[2] - box[0] + 2 * padding, box[3] - box[1] + 2 * padding))
        result.paste(Image.fromarray(pixels, "RGBA"), (padding, padding))
        name = f"{number:02d}.png"
        result.save(target / name, optimize=True)
        print(f"  {name}: {box}, {component.pixels} foreground pixels")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--alpha-threshold", type=int, default=8)
    parser.add_argument("--min-pixels", type=int, default=250)
    parser.add_argument("--padding", type=int, default=12)
    args = parser.parse_args()
    for filename, folder in SHEETS.items():
        slice_sheet(ROOT / filename, ASSETS / folder, args.alpha_threshold, args.min_pixels, args.padding)
    backgrounds = ASSETS / "backgrounds"
    backgrounds.mkdir(parents=True, exist_ok=True)
    for number in range(1, 4):
        copyfile(ROOT / f"Фон {number}.png", backgrounds / f"room-{number}.png")
    print("Backgrounds copied unchanged.")


if __name__ == "__main__":
    main()
