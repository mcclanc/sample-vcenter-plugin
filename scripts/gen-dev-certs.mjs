#!/usr/bin/env node
/**
 * Generates localhost TLS files under certs/ using mkcert (same as gen-dev-certs.sh).
 */
import { spawnSync } from "node:child_process";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const root = path.join(__dirname, "..");
const certsDir = path.join(root, "certs");

fs.mkdirSync(certsDir, { recursive: true });
const keyFile = path.join(certsDir, "dev-key.pem");
const certFile = path.join(certsDir, "dev-cert.pem");

const run = spawnSync(
  "mkcert",
  ["-key-file", keyFile, "-cert-file", certFile, "localhost", "127.0.0.1", "::1"],
  { cwd: root, stdio: "inherit", shell: false },
);
if (run.error?.code === "ENOENT") {
  console.error(
    "mkcert not found. Install: https://github.com/FiloSottile/mkcert#installation",
  );
  console.error("  Windows: winget install FiloSottile.mkcert");
  console.error("  macOS:   brew install mkcert");
  process.exit(1);
}
if (run.status !== 0) process.exit(run.status ?? 1);

console.log("");
console.log("Wrote certs/dev-key.pem and certs/dev-cert.pem");
console.log("HTTPS dev server:");
console.log("  npm run start:https");
console.log("");
console.log("If browsers show untrusted CA, run once:");
console.log("  mkcert -install");
