/**
 * Renders the Data Intelligence icon SVG to ui-data-intel/images/sprites.png.
 * Uses @resvg/resvg-js (pure WASM, no native deps).
 *
 * Usage:
 *   node scripts/set_dataintel_icon.mjs
 */
import { Resvg } from "@resvg/resvg-js";
import { writeFileSync, mkdirSync } from "fs";
import { join } from "path";
import struct from "node:buffer";
import zlib from "node:zlib";

const SVG = `<svg width="34" height="34" viewBox="0 0 34 34" fill="none" xmlns="http://www.w3.org/2000/svg">
<path d="M30.2222 17.0005C31.2654 17.0005 32.1108 17.846 32.1108 18.8892V30.2222C32.1108 31.2654 31.2654 32.1108 30.2222 32.1108H17.0005C15.9573 32.1108 15.1108 31.2654 15.1108 30.2222V18.8892C15.1108 17.846 15.9573 17.0005 17.0005 17.0005H30.2222ZM16.0552 3.89111C20.6166 3.89111 28.3328 4.65604 28.3335 7.66846V15.1206H24.7349C25.3525 14.9553 25.9333 14.6732 26.4448 14.2896V9.50146C24.2632 10.6726 20.0033 11.2202 15.9517 11.2202C13.1244 11.2597 10.303 10.9425 7.55518 10.2759V8.91553C10.2994 9.60134 13.1237 9.91538 15.9517 9.8501C22.1189 9.8501 25.6704 8.66066 26.4448 7.91455V7.63135C26.1332 7.01746 22.3735 5.74268 16.0552 5.74268C9.73721 5.74273 5.98771 7.0171 5.6665 7.73486V26.4448C5.93156 27.0304 8.62339 28.0124 13.2222 28.3335V30.2222C8.86853 29.92 3.77835 28.9284 3.77783 26.4448V7.66846C3.77858 4.69391 11.4937 3.89116 16.0552 3.89111ZM17.0005 30.2222H30.2222V18.8892H17.0005V30.2222ZM23.6108 20.6079C23.976 20.6079 24.272 20.9039 24.272 21.269V28.3335C24.2719 28.6985 23.9759 28.9946 23.6108 28.9946C23.2459 28.9945 22.9498 28.6984 22.9497 28.3335V21.269C22.9497 20.904 23.2459 20.6081 23.6108 20.6079ZM19.8335 20.4946C20.1986 20.4947 20.4946 20.7907 20.4946 21.1558V28.229C20.4946 28.5941 20.1986 28.8901 19.8335 28.8901C19.4684 28.8901 19.1724 28.5941 19.1724 28.229V21.1558C19.1724 20.7906 19.4684 20.4946 19.8335 20.4946ZM27.3892 20.4946C27.7542 20.4947 28.0503 20.7907 28.0503 21.1558V28.229C28.0503 28.5941 27.7542 28.89 27.3892 28.8901C27.024 28.8901 26.728 28.5941 26.728 28.229V21.1558C26.728 20.7906 27.024 20.4946 27.3892 20.4946ZM7.55518 21.5708C9.408 22.0615 11.3077 22.3565 13.2222 22.4497V23.772C11.3106 23.6759 9.41237 23.3941 7.55518 22.9312V21.5708ZM7.55518 15.2339C9.78299 15.8163 12.0723 16.1332 14.3745 16.1782C14.0008 16.5467 13.705 16.9868 13.5054 17.4722C11.4984 17.3789 9.50504 17.0876 7.55518 16.603V15.2339Z" fill="#2D4048"/>
</svg>`;

const ICON_SIZE  = 32;
const ROWS       = 6;
const ICON_X     = 0;
const ICON_Y     = 96;   // matches plugin.json { "x": 0, "y": 96 }
const OUT_DIR    = "ui-data-intel/images";
const OUT_FILE   = join(OUT_DIR, "sprites.png");

// ── render SVG to 32×32 RGBA ─────────────────────────────────────────────────
const resvg = new Resvg(SVG, {
  fitTo: { mode: "width", value: ICON_SIZE },
  background: "transparent",
});
const rendered = resvg.render();
const iconPng  = rendered.asPng();   // PNG bytes of the 32×32 icon

// ── decode the icon PNG to raw RGBA pixels ───────────────────────────────────
function decodePng(buf) {
  const data = Buffer.from(buf);
  let pos = 8;
  let w, h, idat = Buffer.alloc(0);
  while (pos < data.length) {
    const len  = data.readUInt32BE(pos);
    const tag  = data.slice(pos + 4, pos + 8).toString("ascii");
    const body = data.slice(pos + 8, pos + 8 + len);
    pos += 12 + len;
    if (tag === "IHDR") { w = body.readUInt32BE(0); h = body.readUInt32BE(4); }
    if (tag === "IDAT") { idat = Buffer.concat([idat, body]); }
    if (tag === "IEND") break;
  }
  const bpp    = 4;   // RGBA
  const stride = w * bpp;
  const raw    = zlib.inflateSync(idat);
  const pixels = Buffer.alloc(w * h * bpp);
  const prev   = Buffer.alloc(stride, 0);

  function paeth(a, b, c) {
    const p = a + b - c, pa = Math.abs(p - a), pb = Math.abs(p - b), pc = Math.abs(p - c);
    return (pa <= pb && pa <= pc) ? a : pb <= pc ? b : c;
  }

  for (let y = 0; y < h; y++) {
    const rowIn  = y * (1 + stride);
    const filt   = raw[rowIn];
    const rowOut = y * stride;
    for (let x = 0; x < stride; x++) {
      const byte = raw[rowIn + 1 + x];
      const left = x >= bpp ? pixels[rowOut + x - bpp] : 0;
      const up   = y > 0    ? prev[x]                  : 0;
      const ul   = (x >= bpp && y > 0) ? prev[x - bpp] : 0;
      let val;
      switch (filt) {
        case 0: val = byte; break;
        case 1: val = byte + left; break;
        case 2: val = byte + up; break;
        case 3: val = byte + Math.floor((left + up) / 2); break;
        case 4: val = byte + paeth(left, up, ul); break;
        default: throw new Error(`Unknown PNG filter ${filt}`);
      }
      pixels[rowOut + x] = val & 0xFF;
    }
    pixels.copy(prev, 0, rowOut, rowOut + stride);
  }
  return { w, h, pixels };
}

const { w: iw, h: ih, pixels: iconRgba } = decodePng(iconPng);
if (iw !== ICON_SIZE || ih !== ICON_SIZE) {
  throw new Error(`Unexpected icon size ${iw}×${ih}`);
}

// ── build 32×192 sprite sheet (transparent) ──────────────────────────────────
const totalH = ROWS * ICON_SIZE;
const sheet  = Buffer.alloc(ICON_SIZE * totalH * 4, 0);   // transparent

for (let row = 0; row < ICON_SIZE; row++) {
  const srcOff = row * ICON_SIZE * 4;
  const dstOff = ((ICON_Y + row) * ICON_SIZE + ICON_X) * 4;
  iconRgba.copy(sheet, dstOff, srcOff, srcOff + ICON_SIZE * 4);
}

// ── write PNG ─────────────────────────────────────────────────────────────────
function writePng(path, width, height, rgba) {
  function chunk(tag, data) {
    const tagBuf = Buffer.from(tag, "ascii");
    const crc    = zlib.crc32(Buffer.concat([tagBuf, data]));
    const out    = Buffer.alloc(12 + data.length);
    out.writeUInt32BE(data.length, 0);
    tagBuf.copy(out, 4);
    data.copy(out, 8);
    out.writeUInt32BE(crc >>> 0, 8 + data.length);
    return out;
  }
  const raw    = [];
  const stride = width * 4;
  for (let y = 0; y < height; y++) {
    raw.push(Buffer.from([0]));          // filter: None
    raw.push(rgba.slice(y * stride, (y + 1) * stride));
  }
  const idat = zlib.deflateSync(Buffer.concat(raw), { level: 9 });
  const ihdr = Buffer.alloc(13);
  ihdr.writeUInt32BE(width, 0); ihdr.writeUInt32BE(height, 4);
  ihdr[8] = 8; ihdr[9] = 6;  // bit depth 8, RGBA

  const png = Buffer.concat([
    Buffer.from("\x89PNG\r\n\x1a\n", "binary"),
    chunk("IHDR", ihdr),
    chunk("IDAT", idat),
    chunk("IEND", Buffer.alloc(0)),
  ]);
  mkdirSync(OUT_DIR, { recursive: true });
  writeFileSync(path, png);
}

writePng(OUT_FILE, ICON_SIZE, totalH, sheet);
console.log(`Written ${OUT_FILE}  (${ICON_SIZE}×${totalH} px, icon at x=${ICON_X} y=${ICON_Y})`);
