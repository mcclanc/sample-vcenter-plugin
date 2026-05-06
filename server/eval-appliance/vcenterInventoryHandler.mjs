import { fetchDatacentersOnly, fetchPlacementForDatacenter, isSkipTlsVerifyFlag } from "./vcenterRest.mjs";

/**
 * POST JSON body: { vcHost, vcUsername, vcPassword, datacenter?, allowInsecureVcTls? }
 * - Without datacenter: { ok, datacenters: [{ name, id }] }
 * - With datacenter: { ok, datacenter, compute, datastores, networks }
 * - allowInsecureVcTls: when true, plug-in → vCenter HTTPS skips TLS cert verification (same as VCENTER_REST_TLS_INSECURE=true).
 */
export async function vcenterInventoryHandler(req, res) {
  const body = req.body && typeof req.body === "object" ? req.body : {};
  const vcHost = typeof body.vcHost === "string" ? body.vcHost.trim() : "";
  const vcUsername = typeof body.vcUsername === "string" ? body.vcUsername.trim() : "";
  const vcPassword = typeof body.vcPassword === "string" ? body.vcPassword : "";
  const datacenter = typeof body.datacenter === "string" ? body.datacenter.trim() : "";
  /* Body, query, or header — some proxies strip uncommon JSON fields; duplicates are safe. */
  const insecureTls =
    isSkipTlsVerifyFlag(body.allowInsecureVcTls) ||
    isSkipTlsVerifyFlag(req.query?.allowInsecureVcTls) ||
    isSkipTlsVerifyFlag(typeof req.get === "function" ? req.get("x-allow-insecure-vc-tls") : "");

  if (!vcHost || !vcUsername || !vcPassword) {
    return res.status(400).json({
      error: "vcHost, vcUsername, and vcPassword are required to query vCenter inventory.",
    });
  }

  try {
    if (!datacenter) {
      const datacenters = await fetchDatacentersOnly({ vcHost, vcUsername, vcPassword, insecureTls });
      return res.json({ ok: true, datacenters });
    }
    const placement = await fetchPlacementForDatacenter({
      vcHost,
      vcUsername,
      vcPassword,
      datacenterName: datacenter,
      insecureTls,
    });
    return res.json({ ok: true, ...placement });
  } catch (err) {
    const msg = err instanceof Error ? err.message : String(err);
    console.error("[vcenter-inventory]", msg);
    const certProblem = /cert|certificate|ssl|tls|UNABLE_TO_VERIFY|unknown ca|self signed/i.test(msg);
    const hintParts = [
      "The plug-in server must reach vCenter over HTTPS (DNS/firewall from this host to vCenter:443).",
      certProblem
        ? "Certificate trust: (1) Enable **Allow Insecure Connection** in the modal and Connect again, or (2) set environment variable VCENTER_REST_TLS_INSECURE=true and restart Node, or (3) add your root CA via NODE_EXTRA_CA_CERTS=/path/to/ca.pem , or (4) start Node with --use-system-ca (Node 22+) so the OS trust store is used: npm run start:system-ca"
        : "If the certificate is not trusted, enable Allow Insecure Connection in the UI or set VCENTER_REST_TLS_INSECURE=true on the server.",
    ];
    return res.status(502).json({
      error: msg,
      hint: hintParts.join(" "),
    });
  }
}
