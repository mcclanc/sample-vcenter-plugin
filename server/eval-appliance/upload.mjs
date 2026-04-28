import crypto from "node:crypto";
import os from "node:os";
import path from "node:path";
import multer from "multer";

const GIB = 1024 * 1024 * 1024;

/**
 * Resolves the upload cap (multer `limits.fileSize`). Evaluation OVAs are often 30–50+ GiB.
 * - Prefer `OVA_UPLOAD_MAX_GB` (integer GiB) for readability, e.g. `OVA_UPLOAD_MAX_GB=64`.
 * - Or set `OVA_UPLOAD_MAX_BYTES` to an explicit byte count.
 * Defaults to 100 GiB when neither is set.
 */
function parseOvaUploadMaxBytes() {
  const g = process.env.OVA_UPLOAD_MAX_GB;
  if (g != null && String(g).trim() !== "") {
    const n = Number(String(g).trim());
    if (Number.isFinite(n) && n > 0) {
      return Math.min(Math.floor(n * GIB), Number.MAX_SAFE_INTEGER);
    }
  }
  const b = process.env.OVA_UPLOAD_MAX_BYTES;
  if (b != null && String(b).trim() !== "") {
    const n = Number(String(b).trim());
    if (Number.isFinite(n) && n > 0) {
      return Math.min(Math.floor(n), Number.MAX_SAFE_INTEGER);
    }
  }
  return 100 * GIB;
}

export const ovaUploadMaxBytes = parseOvaUploadMaxBytes();

const storage = multer.diskStorage({
  destination: (_req, _file, cb) => {
    cb(null, os.tmpdir());
  },
  filename: (_req, file, cb) => {
    const ext = path.extname(file.originalname) || ".ova";
    cb(null, `ova-upload-${Date.now()}-${crypto.randomBytes(8).toString("hex")}${ext}`);
  },
});

export const ovaUpload = multer({
  storage,
  limits: { fileSize: ovaUploadMaxBytes },
});
