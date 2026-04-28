#!/usr/bin/env python3
"""
Paste a source image into ui/images/sprites.png at (x, y), resized to 32x32 (LANCZOS).

Requires: pip install -r scripts/requirements-icons.txt

Examples (from repo root):
  pip install -r scripts/requirements-icons.txt
  python3 scripts/compose_icon_into_sprite.py ui/images/my-logo.png
  python3 scripts/compose_icon_into_sprite.py ~/Downloads/tanzu.png --y 96
"""
from __future__ import annotations

import argparse
from pathlib import Path

try:
    from PIL import Image, ImageOps
except ImportError as e:
    raise SystemExit(
        "Pillow is required: pip install -r scripts/requirements-icons.txt"
    ) from e


def main() -> None:
    p = argparse.ArgumentParser(description="Composite a 32x32 icon into the plugin sprite sheet.")
    p.add_argument("source", type=Path, help="PNG/SVG-not-supported — use PNG or JPEG path")
    p.add_argument(
        "--sprite",
        type=Path,
        default=Path("ui/images/sprites.png"),
        help="Target sprite sheet (default: ui/images/sprites.png)",
    )
    p.add_argument("--x", type=int, default=0, help="Left pixel in sprite (default 0)")
    p.add_argument("--y", type=int, default=96, help="Top pixel in sprite (default 96 = main row)")
    p.add_argument("--size", type=int, default=32, help="Icon size (default 32)")
    args = p.parse_args()

    if not args.source.is_file():
        raise SystemExit(f"Source not found: {args.source}")

    src = Image.open(args.source).convert("RGBA")
    icon = ImageOps.fit(src, (args.size, args.size), method=Image.Resampling.LANCZOS)

    if not args.sprite.is_file():
        raise SystemExit(f"Sprite sheet not found: {args.sprite}")

    sheet = Image.open(args.sprite).convert("RGBA")
    w, h = sheet.size
    if args.x + args.size > w or args.y + args.size > h:
        raise SystemExit(
            f"Paste region ({args.x},{args.y})+{args.size} exceeds sheet size {w}x{h}"
        )

    sheet.paste(icon, (args.x, args.y), icon)
    args.sprite.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(args.sprite, format="PNG")
    print(f"Updated {args.sprite} — pasted {args.source.name} at ({args.x}, {args.y}), {args.size}x{args.size}.")


if __name__ == "__main__":
    main()
