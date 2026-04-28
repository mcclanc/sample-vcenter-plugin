import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const root = path.join(__dirname, "..");
process.env.SSL_KEY_PATH = path.join(root, "certs", "dev-key.pem");
process.env.SSL_CERT_PATH = path.join(root, "certs", "dev-cert.pem");
await import("../server/index.mjs");
