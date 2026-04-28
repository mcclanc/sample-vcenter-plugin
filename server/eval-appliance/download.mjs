import { createWriteStream } from "node:fs";
import { stat } from "node:fs/promises";
import { Readable } from "node:stream";
import { pipeline } from "node:stream/promises";

/**
 * Stream download to disk. Uses global fetch (Node 18+).
 * For TLS issues with Broadcom endpoints in lab, you may set NODE_TLS_REJECT_UNAUTHORIZED=0
 * on the Node process (insecure — lab only).
 * @param {string} sourceUrl
 * @param {string} destPath
 * @param {{ headers?: Record<string, string> }} [opts]
 */
export async function downloadOvaToFile(sourceUrl, destPath, opts = {}) {
  const headers = {
    "User-Agent": "sample-vcenter-plugin/eval-appliance",
    ...(opts.headers || {}),
  };
  const res = await fetch(sourceUrl, {
    method: "GET",
    headers,
    redirect: "follow",
  });
  if (!res.ok) {
    const snippet = (await res.text()).slice(0, 800);
    throw new Error(`Download failed HTTP ${res.status}: ${snippet}`);
  }
  if (!res.body) {
    throw new Error("Download response has no body.");
  }
  const nodeReadable = Readable.fromWeb(/** @type {import('stream/web').ReadableStream} */ (res.body));
  await pipeline(nodeReadable, createWriteStream(destPath));
  const st = await stat(destPath);
  return { bytes: st.size };
}

/**
 * Try Bearer registry token, then unauthenticated (e.g. time-limited signed URL).
 */
export async function downloadWithRegistryTokenFallback(ovaDownloadUrl, registryToken, destPath) {
  const attempts = [];
  if (registryToken) {
    attempts.push({ Authorization: `Bearer ${registryToken}` });
  }
  attempts.push({});
  let lastErr;
  for (const h of attempts) {
    try {
      const { bytes } = await downloadOvaToFile(ovaDownloadUrl, destPath, { headers: h });
      return { usedBearerAuth: Boolean(h.Authorization), bytes };
    } catch (e) {
      lastErr = e;
    }
  }
  throw lastErr;
}
