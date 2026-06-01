/**
 * Fetches the TLS certificate from a vCenter host and returns it as PEM.
 *
 * POST /api/vcenter/cert
 * Body: { vcHost, port? }
 *
 * Uses rejectUnauthorized=false so it works with self-signed certs.
 * The raw DER cert is converted to PEM and returned — the client can
 * paste it straight into the Tanzu Hub "vCenter certificate" field.
 */
import tls from "node:tls";

export function vcenterCertHandler(req, res) {
  const { vcHost, port: portRaw } = req.body || {};

  if (!vcHost || typeof vcHost !== "string" || !vcHost.trim()) {
    return res.status(400).json({ error: "vcHost is required." });
  }

  const host = vcHost.trim();
  const port = Number(portRaw) || 443;

  let settled = false;

  const socket = tls.connect({
    host,
    port,
    rejectUnauthorized: false,
    timeout: 10_000,
    servername: host,
  });

  socket.setTimeout(10_000);

  socket.once("secureConnect", () => {
    if (settled) return;
    settled = true;

    try {
      const cert = socket.getPeerCertificate(false);
      socket.destroy();

      if (!cert || !cert.raw) {
        return res.status(502).json({ error: "Connected but received no certificate." });
      }

      const b64 = cert.raw.toString("base64");
      const lines = b64.match(/.{1,64}/g) || [];
      const pem = `-----BEGIN CERTIFICATE-----\n${lines.join("\n")}\n-----END CERTIFICATE-----`;

      const { subject, issuer, valid_from, valid_to, fingerprint } = cert;

      res.json({
        pem,
        meta: { subject, issuer, valid_from, valid_to, fingerprint },
      });
    } catch (err) {
      res.status(500).json({ error: `Certificate parse error: ${err.message}` });
    }
  });

  socket.once("error", (err) => {
    if (settled) return;
    settled = true;
    socket.destroy();
    res.status(502).json({
      error: `Could not connect to ${host}:${port} — ${err.message}`,
      hint: "Check the hostname and that port 443 is reachable from the plugin server.",
    });
  });

  socket.once("timeout", () => {
    if (settled) return;
    settled = true;
    socket.destroy();
    res.status(504).json({ error: `Connection to ${host}:${port} timed out after 10 s.` });
  });
}
