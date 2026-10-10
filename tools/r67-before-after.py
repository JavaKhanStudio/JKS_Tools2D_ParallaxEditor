#!/usr/bin/env python3
"""r67: puts each panel's BEFORE shot beside its AFTER shot, the left panel only, into one image per panel.

    python3 tools/r67-before-after.py editor/build/r67/before1300 editor/build/r67/after editor/build/r67/compare

The shots come from tools/r67-every-panel.txt run twice (before and after the change), same window size. The left
panel is the left 31% of a shot (400 px at 1300); the start screen and the preview bar ("start*", "*preview") are
whole shots, halved.
"""
import sys
from pathlib import Path

from PIL import Image, ImageDraw

PANEL = 0.31  # of the shot's width kept: the panel and a bit of the preview


def part(image, stem):
    if stem.endswith("preview") or "start" in stem:
        return image.resize((image.width // 2, image.height // 2))
    return image.crop((0, 0, int(image.width * PANEL), image.height))


def main(before, after, out):
    before, after, out = Path(before), Path(after), Path(out)
    out.mkdir(parents=True, exist_ok=True)
    for shot in sorted(after.glob("*.png")):
        old = before / shot.name
        if not old.exists():
            continue
        a = part(Image.open(old).convert("RGB"), shot.stem)
        b = part(Image.open(shot).convert("RGB"), shot.stem)
        sheet = Image.new("RGB", (a.width + b.width + 10, max(a.height, b.height) + 24), (40, 40, 40))
        sheet.paste(a, (0, 24))
        sheet.paste(b, (a.width + 10, 24))
        draw = ImageDraw.Draw(sheet)
        draw.text((4, 4), "BEFORE " + shot.stem, fill=(255, 255, 255))
        draw.text((a.width + 14, 4), "AFTER " + shot.stem, fill=(255, 255, 255))
        sheet.save(out / shot.name)
        print(out / shot.name)


if __name__ == "__main__":
    main(*sys.argv[1:4])
