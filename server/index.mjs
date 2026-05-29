import express from "express";
import fs from "node:fs";
import http from "node:http";
import https from "node:https";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const root = path.join(__dirname, "..");
const ui = path.join(root, "ui");
const uiSecureImages = path.join(root, "ui-secure-images");
const uiDataIntel = path.join(root, "ui-data-intel");

import { evalApplianceDeployHandler } from "./eval-appliance/deployHandler.mjs";
import { vcenterInventoryHandler } from "./eval-appliance/vcenterInventoryHandler.mjs";
import { ovaUpload, ovaUploadMaxBytes } from "./eval-appliance/upload.mjs";

const GIB = 1024 * 1024 * 1024;
const MIB = 1024 * 1024;

function formatOvaSizeLimit(bytes) {
  if (bytes >= GIB) return `${(bytes / GIB).toFixed(2)} GiB`;
  if (bytes >= MIB) return `${(bytes / MIB).toFixed(0)} MiB`;
  return `${bytes} B`;
}

const app = express();

/**
 * vSphere Client loads the UI from the vCenter origin while deploy/inventory POSTs target the
 * remote plug-in host — browsers require CORS. multipart/form-data triggers a preflight OPTIONS.
 */
app.use((req, res, next) => {
  const p = req.path || "";
  const isPluginApi =
    p.startsWith("/api/eval-appliance/") ||
    p.startsWith("/api/vcenter/") ||
    p.startsWith("/tanzu-hub-poc-ui/api/eval-appliance/") ||
    p.startsWith("/tanzu-hub-poc-ui/api/vcenter/") ||
    p.startsWith("/secure-images-ui/api/") ||
    p.startsWith("/data-intel-ui/api/");
  if (!isPluginApi) return next();

  const origin = req.get("Origin");
  if (origin) {
    res.setHeader("Access-Control-Allow-Origin", origin);
    res.setHeader("Access-Control-Allow-Credentials", "true");
    res.setHeader("Vary", "Origin");
  } else {
    res.setHeader("Access-Control-Allow-Origin", "*");
  }
  res.setHeader("Access-Control-Allow-Methods", "GET, HEAD, POST, OPTIONS");
  const reqHdrs = req.get("Access-Control-Request-Headers");
  res.setHeader(
    "Access-Control-Allow-Headers",
    reqHdrs || "Content-Type, Accept, Authorization, X-Allow-Insecure-Vc-Tls",
  );
  res.setHeader("Access-Control-Max-Age", "7200");

  if (req.method === "OPTIONS") {
    return res.sendStatus(204);
  }
  next();
});

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
    plugins: [
      { name: "App Platform as a Service", manifest: "/tanzu-hub-poc-ui/plugin.json" },
      { name: "Secure Images",             manifest: "/secure-images-ui/plugin.json" },
      { name: "Data Intelligence",         manifest: "/data-intel-ui/plugin.json" },
    ],
    evalApplianceDeploy: {
      mode: "multipart-ova",
      formField: "ovaFile",
      requiresSupportPortalRegistryToken: false,
      maxUploadBytes: ovaUploadMaxBytes,
    },
    vcenterInventory: {
      path: "POST /api/vcenter/inventory (and /tanzu-hub-poc-ui/...)",
      body: "{ vcHost, vcUsername, vcPassword, datacenter?, allowInsecureVcTls? }",
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

function vcenterInventoryPing(_req, res) {
  res.json({
    ok: true,
    method: "GET",
    message: "vCenter inventory API is mounted here; use POST with JSON body { vcHost, vcUsername, vcPassword, datacenter? }.",
  });
}

app.get("/tanzu-hub-poc-ui/api/vcenter/inventory", vcenterInventoryPing);
app.get("/api/vcenter/inventory", vcenterInventoryPing);

app.post("/tanzu-hub-poc-ui/api/vcenter/inventory", (req, res) => void vcenterInventoryHandler(req, res));
app.post("/api/vcenter/inventory", (req, res) => void vcenterInventoryHandler(req, res));

app.use("/tanzu-hub-poc-ui",  express.static(ui,              { index: false }));
app.use("/secure-images-ui", express.static(uiSecureImages, { index: false }));
app.use("/data-intel-ui",    express.static(uiDataIntel,    { index: false }));

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

/** Node 18+ defaults `requestTimeout` to 5m — large OVA multipart + ovftool needs much longer. */
const PLUGIN_HTTP_LONG_MS = Number(process.env.PLUGIN_HTTP_SERVER_TIMEOUT_MS) || 4 * 60 * 60 * 1000;
if ("requestTimeout" in server) server.requestTimeout = PLUGIN_HTTP_LONG_MS;
if ("headersTimeout" in server) server.headersTimeout = PLUGIN_HTTP_LONG_MS + 120_000;
server.timeout = PLUGIN_HTTP_LONG_MS;

server.listen(port, () => {
  const scheme = useTls ? "https" : "http";
  console.log(
    `${scheme}://localhost:${port} | manifests: /tanzu-hub-poc-ui/plugin.json  /secure-images-ui/plugin.json  /data-intel-ui/plugin.json | /health | deploy timeout≈${Math.round(PLUGIN_HTTP_LONG_MS / 60000)}min`,
  );
  if (!useTls) {
    console.warn(
      "Running without TLS. vCenter remote plug-in registration typically requires HTTPS; set SSL_KEY_PATH and SSL_CERT_PATH.",
    );
  }
});
