#!/usr/bin/env python3
"""
Generate ui/images/sprites.png with the custom plugin icon.
Pure stdlib — no native dependencies required.

The SVG is 36×36 with 9 rectilinear L-bracket shapes (fill #2D4048).
We rasterise it directly, scale 36→32, then embed in a 32×192 sprite sheet
at y=96 (matching plugin.json { "x": 0, "y": 96 }).
"""
from __future__ import annotations
import struct, zlib
from pathlib import Path

# ── icon geometry ─────────────────────────────────────────────────────────────
# Each bracket: two rectangles whose union forms a bottom-right corner L-shape.
# Values are (x0, y0, x1, y1) half-open rectangles in the 36×36 SVG viewport.
BRACKETS = [
    # top row
    ((8,4,10,10), (4,8,8,10)),   # top-left:     right strip + bottom strip
    ((19,4,21,10), (15,8,19,10)),# top-center
    ((30,4,32,10), (26,8,30,10)),# top-right
    # middle row
    ((8,15,10,21), (4,19,8,21)),
    ((19,15,21,21), (15,19,19,21)),
    ((30,15,32,21), (26,19,30,21)),
    # bottom row
    ((8,26,10,32), (4,30,8,32)),
    ((19,26,21,32), (15,30,19,32)),
    ((30,26,32,32), (26,30,30,32)),
]
FILL = (0x2D, 0x40, 0x48, 0xFF)
SVG_SIZE  = 36
ICON_SIZE = 32   # output tile size

SPRITE_OUT = Path("ui/images/sprites.png")
ROWS       = 6          # 6 × 32 = 192 px tall
ICON_X     = 0
ICON_Y     = 96         # matches plugin.json  "y": 96


# ── PNG writer (stdlib only) ───────────────────────────────────────────────────

def _chunk(tag: bytes, data: bytes) -> bytes:
    crc = zlib.crc32(tag + data) & 0xFFFFFFFF
    return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", crc)


def write_png(path: Path, width: int, height: int, rgba: bytearray) -> None:
    raw = bytearray()
    stride = width * 4
    for y in range(height):
        raw.append(0)                               # filter: None
        raw.extend(rgba[y * stride:(y + 1) * stride])
    ihdr = struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0)
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(
        b"\x89PNG\r\n\x1a\n"
        + _chunk(b"IHDR", ihdr)
        + _chunk(b"IDAT", zlib.compress(bytes(raw), 9))
        + _chunk(b"IEND", b"")
    )


# ── rasterise SVG at SVG_SIZE × SVG_SIZE ─────────────────────────────────────

def rasterise_icon() -> bytearray:
    """Return RGBA bytearray (SVG_SIZE × SVG_SIZE) for the icon."""
    pixels = bytearray(SVG_SIZE * SVG_SIZE * 4)   # transparent

    def fill_rect(x0: int, y0: int, x1: int, y1: int) -> None:
        r, g, b, a = FILL
        for y in range(max(0, y0), min(SVG_SIZE, y1)):
            for x in range(max(0, x0), min(SVG_SIZE, x1)):
                off = (y * SVG_SIZE + x) * 4
                pixels[off:off + 4] = bytes([r, g, b, a])

    for rect1, rect2 in BRACKETS:
        fill_rect(*rect1)
        fill_rect(*rect2)

    return pixels


# ── nearest-neighbour scale SVG_SIZE → ICON_SIZE ─────────────────────────────

def scale_nn(src: bytearray, src_size: int, dst_size: int) -> bytearray:
    dst = bytearray(dst_size * dst_size * 4)
    for dy in range(dst_size):
        sy = int(dy * src_size / dst_size)
        for dx in range(dst_size):
            sx  = int(dx * src_size / dst_size)
            src_off = (sy * src_size + sx) * 4
            dst_off = (dy * dst_size + dx) * 4
            dst[dst_off:dst_off + 4] = src[src_off:src_off + 4]
    return dst


# ── main ──────────────────────────────────────────────────────────────────────

def main() -> None:
    total_h = ROWS * ICON_SIZE

    # 1. Rasterise at SVG_SIZE, then scale to ICON_SIZE
    raw_pixels  = rasterise_icon()
    icon_pixels = scale_nn(raw_pixels, SVG_SIZE, ICON_SIZE)

    # 2. Build blank (transparent) sprite sheet
    sheet = bytearray(ICON_SIZE * total_h * 4)

    # 3. Paste icon into sprite sheet at (ICON_X, ICON_Y)
    for row in range(ICON_SIZE):
        src_off = row * ICON_SIZE * 4
        dst_off = ((ICON_Y + row) * ICON_SIZE + ICON_X) * 4
        sheet[dst_off:dst_off + ICON_SIZE * 4] = icon_pixels[src_off:src_off + ICON_SIZE * 4]

    write_png(SPRITE_OUT, ICON_SIZE, total_h, sheet)
    print(f"Written {SPRITE_OUT}  ({ICON_SIZE}×{total_h} px, icon at x={ICON_X} y={ICON_Y})")


if __name__ == "__main__":
    main()
