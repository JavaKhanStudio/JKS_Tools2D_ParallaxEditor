#!/usr/bin/env python3
"""r67: puts each panel's BEFORE shot over its AFTER shot, the left panel only, into one image per panel.

    python3 tools/r67-before-after.py editor/build/r67/before1300 editor/build/r67/after editor/build/r67/compare

The shots come from tools/r67-every-panel.txt run twice (before and after the change), same window size.
"""
import sys
from pathlib import Path

from PIL import Image, ImageDraw

PANEL = 400  # px of the left edge kept: the panel and a bit of the preview


def main(before, after, out):
    before, after, out = Path(before), Path(after), Path(out)
    out.mkdir(parents=True, exist_ok=True)
    for shot in sorted(after.glob("*.png")):
        old = before / shot.name
        if not old.exists():
            continue
        a, b = Image.open(old).convert("RGB"), Image.open(shot).convert("RGB")
        h = max(a.height, b.height)
        sheet = Image.new("RGB", (PANEL * 2 + 10, h + 24), (40, 40, 40))
        sheet.paste(a.crop((0, 0, PANEL, a.height)), (0, 24))
        sheet.paste(b.crop((0, 0, PANEL, b.height)), (PANEL + 10, 24))
        draw = ImageDraw.Draw(sheet)
        draw.text((4, 4), "BEFORE " + shot.stem, fill=(255, 255, 255))
        draw.text((PANEL + 14, 4), "AFTER " + shot.stem, fill=(255, 255, 255))
        sheet.save(out / shot.name)
        print(out / shot.name)


if __name__ == "__main__":
    main(*sys.argv[1:4])
