import express from "express";
import fs from "node:fs";
import http from "node:http";
import https from "node:https";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const root = path.join(__dirname, "..");
const ui = path.join(root, "ui");

const app = express();

app.get("/health", (_req, res) => {
  res.json({ ok: true, service: "sample-vcenter-plugin" });
});

/** Same path the vSphere Client uses; real client injects the production script. */
app.get("/api/ui/htmlClientSdk.js", (_req, res) => {
  res.type("application/javascript");
  res.sendFile(path.join(__dirname, "htmlClientSdk.stub.js"));
});

app.use("/tanzu-hub-poc-ui", express.static(ui, { index: false }));

const port = Number(process.env.PORT || 8443);
const keyPath = process.env.SSL_KEY_PATH;
const certPath = process.env.SSL_CERT_PATH;

const useTls = keyPath && certPath && fs.existsSync(keyPath) && fs.existsSync(certPath);

const server = useTls
  ? https.createServer(
      {
        key: fs.readFileSync(keyPath),
        cert: fs.readFileSync(certPath),
      },
      app,
    )
  : http.createServer(app);

server.listen(port, () => {
  const scheme = useTls ? "https" : "http";
  console.log(
    `${scheme}://localhost:${port}/tanzu-hub-poc-ui/plugin.json (manifest) | /health`,
  );
  if (!useTls) {
    console.warn(
      "Running without TLS. vCenter remote plug-in registration typically requires HTTPS; set SSL_KEY_PATH and SSL_CERT_PATH.",
    );
  }
});
