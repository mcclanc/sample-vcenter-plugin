#!/usr/bin/env python3
"""
Build ui/images/sprites.png as a 32-wide strip of 32x32 RGBA tiles (stdlib only).
Default: one solid color tile repeated 6 rows (matches common vSphere Client strip height).
Replace tiles or edit this script once you have an approved Tanzu mark from Broadcom brand guidelines.

Usage:
  python3 scripts/gen_plugin_icon_sprite.py
  python3 scripts/gen_plugin_icon_sprite.py --rows 6 --main-row 0
"""
from __future__ import annotations

import argparse
import struct
import zlib
from pathlib import Path


def _chunk(tag: bytes, data: bytes) -> bytes:
    return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)


def write_png_rgba(path: Path, width: int, height: int, rgba_pixels: bytes) -> None:
    if len(rgba_pixels) != width * height * 4:
        raise ValueError("pixel buffer size mismatch")
    raw = bytearray()
    stride = width * 4
    for y in range(height):
        raw.append(0)  # filter type: None
        row_start = y * stride
        raw.extend(rgba_pixels[row_start : row_start + stride])
    compressed = zlib.compress(bytes(raw), 9)
    ihdr = struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0)
    png = b"\x89PNG\r\n\x1a\n" + _chunk(b"IHDR", ihdr) + _chunk(b"IDAT", compressed) + _chunk(b"IEND", b"")
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(png)


def solid_tile(r: int, g: int, b: int, a: int = 255) -> bytes:
    return bytes([r, g, b, a]) * (32 * 32)


def main() -> None:
    p = argparse.ArgumentParser(description="Generate 32x(32*N) plugin icon sprite for vSphere Client.")
    p.add_argument("--rows", type=int, default=6, help="Number of 32px-tall rows (default 6 = 192px)")
    p.add_argument(
        "--main-row",
        type=int,
        default=0,
        help="0-based row index used as configuration.icon main in plugin.json (y = row * 32)",
    )
    p.add_argument("--out", type=Path, default=Path("ui/images/sprites.png"))
    p.add_argument(
        "--color",
        default="0096D6",
        help="RRGGBB hex for placeholder tiles (no #); replace artwork for production",
    )
    args = p.parse_args()

    hx = args.color.strip().lstrip("#")
    r, g, b = (int(hx[i : i + 2], 16) for i in (0, 2, 4))
    tile = solid_tile(r, g, b)
    strip = tile * args.rows
    write_png_rgba(args.out, 32, 32 * args.rows, strip)

    y = args.main_row * 32
    print(f"Wrote {args.out} ({32}x{32 * args.rows}). Set plugin.json main icon to {{ \"x\": 0, \"y\": {y} }}.")


if __name__ == "__main__":
    main()
