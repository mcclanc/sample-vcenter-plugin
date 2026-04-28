import express from "express";
import fs from "node:fs";
import http from "node:http";
import https from "node:https";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const root = path.join(__dirname, "..");
const ui = path.join(root, "ui");

import { evalApplianceDeployHandler } from "./eval-appliance/deployHandler.mjs";
import { ovaUpload, ovaUploadMaxBytes } from "./eval-appliance/upload.mjs";

const GIB = 1024 * 1024 * 1024;
const MIB = 1024 * 1024;

function formatOvaSizeLimit(bytes) {
  if (bytes >= GIB) return `${(bytes / GIB).toFixed(2)} GiB`;
  if (bytes >= MIB) return `${(bytes / MIB).toFixed(0)} MiB`;
  return `${bytes} B`;
}

const app = express();

app.use(express.json({ limit: "256kb" }));

function handleOvaDeployPost(req, res) {
  ovaUpload.single("ovaFile")(req, res, (err) => {
    if (err) {
      if (err.code === "LIMIT_FILE_SIZE") {
        return res.status(413).json({
          error: `Uploaded OVA exceeds server size limit (current: ${formatOvaSizeLimit(ovaUploadMaxBytes)} = ${ovaUploadMaxBytes} bytes). Set OVA_UPLOAD_MAX_GB (e.g. 80) or OVA_UPLOAD_MAX_BYTES, then restart the server.`,
          limitBytes: ovaUploadMaxBytes,
        });
      }
      return res.status(400).json({ error: String(err.message || err) });
    }
    void evalApplianceDeployHandler(req, res);
  });
}

app.get("/health", (_req, res) => {
  res.json({
    ok: true,
    service: "sample-vcenter-plugin",
    evalApplianceDeploy: {
      mode: "multipart-ova",
      formField: "ovaFile",
      requiresSupportPortalRegistryToken: false,
      maxUploadBytes: ovaUploadMaxBytes,
    },
  });
});

/** Same path the vSphere Client uses; real client injects the production script. */
app.get("/api/ui/htmlClientSdk.js", (_req, res) => {
  res.type("application/javascript");
  res.sendFile(path.join(__dirname, "htmlClientSdk.stub.js"));
});

app.post("/tanzu-hub-poc-ui/api/eval-appliance/deploy", handleOvaDeployPost);
app.post("/api/eval-appliance/deploy", handleOvaDeployPost);

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
