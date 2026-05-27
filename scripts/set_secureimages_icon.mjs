/**
 * Renders the Secure Images icon SVG to ui-secure-images/images/sprites.png.
 * Uses @resvg/resvg-js (pure WASM, no native deps).
 *
 * Usage:
 *   node scripts/set_secureimages_icon.mjs
 */
import { Resvg } from "@resvg/resvg-js";
import { writeFileSync, mkdirSync } from "fs";
import { join } from "path";
import zlib from "node:zlib";

const SVG = `<svg width="37" height="37" viewBox="0 0 37 37" fill="none" xmlns="http://www.w3.org/2000/svg">
<path d="M19.0652 1.89136C21.061 3.14882 23.1502 4.25143 25.3142 5.19019C27.5138 6.17616 29.789 6.98364 32.1179 7.60522L32.8884 7.80054V16.3728C32.8883 30.1449 18.9933 35.3347 18.8494 35.3347L18.4998 35.4587L18.1501 35.3347C17.9884 35.2729 4.11123 30.1204 4.11108 16.3728V7.80054L4.88159 7.60522C7.21372 6.98751 9.49283 6.18391 11.696 5.20093C13.8601 4.26217 15.9493 3.15859 17.9451 1.90112L18.4998 1.54175L19.0652 1.89136ZM18.4998 3.9978C16.578 5.15989 14.5797 6.1907 12.5183 7.08179C10.4571 7.9983 8.33477 8.77102 6.16675 9.39429V16.3728C6.16688 27.6673 16.4434 32.4154 18.4998 33.2585C20.5553 32.4158 30.8336 27.6782 30.8337 16.3728V9.39429C28.6657 8.77101 26.5425 7.99832 24.4812 7.08179C22.4199 6.19072 20.4214 5.15985 18.4998 3.9978ZM25.6941 12.6726C26.1028 12.2753 26.7567 12.2844 27.1541 12.6931C27.5512 13.1017 27.5418 13.7548 27.1335 14.1521L15.9001 24.9548L9.73315 18.7878C9.38394 18.3801 9.40724 17.772 9.78687 17.3923C10.1665 17.0128 10.7746 16.9894 11.1824 17.3386L15.9617 22.1179L25.6941 12.6726Z" fill="#2D4048"/>
</svg>`;

const ICON_SIZE = 32;
const ROWS      = 6;
const ICON_X    = 0;
const ICON_Y    = 96;   // matches plugin.json { "x": 0, "y": 96 }
const OUT_DIR   = "ui-secure-images/images";
const OUT_FILE  = join(OUT_DIR, "sprites.png");

// ── render SVG to 32×32 PNG via resvg ────────────────────────────────────────
const resvg    = new Resvg(SVG, { fitTo: { mode: "width", value: ICON_SIZE }, background: "transparent" });
const iconPng  = resvg.render().asPng();

// ── decode PNG to raw RGBA (handles all filter types) ────────────────────────
function decodePng(buf) {
  const data = Buffer.from(buf);
  let pos = 8, w, h, idat = Buffer.alloc(0);
  while (pos < data.length) {
    const len  = data.readUInt32BE(pos);
    const tag  = data.slice(pos + 4, pos + 8).toString("ascii");
    const body = data.slice(pos + 8, pos + 8 + len);
    pos += 12 + len;
    if (tag === "IHDR") { w = body.readUInt32BE(0); h = body.readUInt32BE(4); }
    if (tag === "IDAT") { idat = Buffer.concat([idat, body]); }
    if (tag === "IEND") break;
  }
  const bpp = 4, stride = w * bpp;
  const raw = zlib.inflateSync(idat);
  const pixels = Buffer.alloc(w * h * bpp);
  const prev   = Buffer.alloc(stride, 0);
  function paeth(a, b, c) {
    const p = a + b - c, pa = Math.abs(p-a), pb = Math.abs(p-b), pc = Math.abs(p-c);
    return (pa <= pb && pa <= pc) ? a : pb <= pc ? b : c;
  }
  for (let y = 0; y < h; y++) {
    const ri = y * (1 + stride), filt = raw[ri], ro = y * stride;
    for (let x = 0; x < stride; x++) {
      const byte = raw[ri + 1 + x];
      const left = x >= bpp ? pixels[ro + x - bpp] : 0;
      const up   = y > 0   ? prev[x]               : 0;
      const ul   = (x >= bpp && y > 0) ? prev[x - bpp] : 0;
      let v;
      switch (filt) {
        case 0: v = byte; break;
        case 1: v = byte + left; break;
        case 2: v = byte + up; break;
        case 3: v = byte + Math.floor((left + up) / 2); break;
        case 4: v = byte + paeth(left, up, ul); break;
        default: throw new Error(`Unknown PNG filter ${filt}`);
      }
      pixels[ro + x] = v & 0xFF;
    }
    pixels.copy(prev, 0, ro, ro + stride);
  }
  return { w, h, pixels };
}

// ── build transparent sprite sheet and paste icon ────────────────────────────
const { w: iw, h: ih, pixels: iconRgba } = decodePng(iconPng);
if (iw !== ICON_SIZE || ih !== ICON_SIZE) throw new Error(`Unexpected icon size ${iw}×${ih}`);

const totalH = ROWS * ICON_SIZE;
const sheet  = Buffer.alloc(ICON_SIZE * totalH * 4, 0);
for (let row = 0; row < ICON_SIZE; row++) {
  iconRgba.copy(sheet, ((ICON_Y + row) * ICON_SIZE + ICON_X) * 4, row * ICON_SIZE * 4, (row + 1) * ICON_SIZE * 4);
}

// ── write PNG ─────────────────────────────────────────────────────────────────
function writePng(path, width, height, rgba) {
  function chunk(tag, data) {
    const t = Buffer.from(tag, "ascii");
    const crc = zlib.crc32(Buffer.concat([t, data]));
    const out = Buffer.alloc(12 + data.length);
    out.writeUInt32BE(data.length, 0); t.copy(out, 4); data.copy(out, 8);
    out.writeUInt32BE(crc >>> 0, 8 + data.length);
    return out;
  }
  const rows = [];
  for (let y = 0; y < height; y++) {
    rows.push(Buffer.from([0]));
    rows.push(rgba.slice(y * width * 4, (y + 1) * width * 4));
  }
  const ihdr = Buffer.alloc(13);
  ihdr.writeUInt32BE(width, 0); ihdr.writeUInt32BE(height, 4); ihdr[8] = 8; ihdr[9] = 6;
  mkdirSync(OUT_DIR, { recursive: true });
  writeFileSync(path, Buffer.concat([
    Buffer.from("\x89PNG\r\n\x1a\n", "binary"),
    chunk("IHDR", ihdr),
    chunk("IDAT", zlib.deflateSync(Buffer.concat(rows), { level: 9 })),
    chunk("IEND", Buffer.alloc(0)),
  ]));
}

writePng(OUT_FILE, ICON_SIZE, totalH, sheet);
console.log(`Written ${OUT_FILE}  (${ICON_SIZE}×${totalH} px, icon at x=${ICON_X} y=${ICON_Y})`);
