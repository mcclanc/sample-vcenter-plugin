/**
 * Server-side proxy for Tanzu Hub GraphQL requests.
 * The browser cannot call Tanzu Hub directly (CORS), so we forward from Node.
 * Uses Node's https module with rejectUnauthorized=false to support on-prem
 * Tanzu Hub deployments that use self-signed certificates.
 *
 * POST /api/hub/graphql-proxy
 * Body: { hubUrl, hubToken, query, variables? }
 */
import https from "node:https";
import http from "node:http";
import { URL } from "node:url";

function httpsRequest(urlStr, options, body) {
  return new Promise((resolve, reject) => {
    const parsed = new URL(urlStr);
    const isHttps = parsed.protocol === "https:";
    const lib = isHttps ? https : http;

    const reqOptions = {
      hostname: parsed.hostname,
      port: parsed.port || (isHttps ? 443 : 80),
      path: parsed.pathname + parsed.search,
      method: options.method || "POST",
      headers: options.headers || {},
      rejectUnauthorized: false, // allow self-signed certs on on-prem Tanzu Hub
    };

    const req = lib.request(reqOptions, (upstreamRes) => {
      const chunks = [];
      upstreamRes.on("data", (c) => chunks.push(c));
      upstreamRes.on("end", () => {
        resolve({ status: upstreamRes.statusCode, body: Buffer.concat(chunks).toString("utf8") });
      });
    });

    req.on("error", reject);
    req.setTimeout(30_000, () => {
      req.destroy(new Error("Request to Tanzu Hub timed out after 30 s"));
    });

    if (body) req.write(body);
    req.end();
  });
}

export async function hubGraphqlProxyHandler(req, res) {
  const { hubUrl, hubToken, query, variables } = req.body || {};

  if (!hubUrl || typeof hubUrl !== "string" || !hubUrl.trim()) {
    return res.status(400).json({ error: "hubUrl is required." });
  }
  if (!hubToken || typeof hubToken !== "string" || !hubToken.trim()) {
    return res.status(400).json({ error: "hubToken is required." });
  }
  if (!query || typeof query !== "string" || !query.trim()) {
    return res.status(400).json({ error: "query is required." });
  }

  const bodyStr = JSON.stringify({ query, variables: variables ?? {} });

  let upstream;
  try {
    upstream = await httpsRequest(
      hubUrl.trim(),
      {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "Content-Length": Buffer.byteLength(bodyStr),
          "Authorization": `Bearer ${hubToken.trim()}`,
        },
      },
      bodyStr,
    );
  } catch (err) {
    return res.status(502).json({
      error: `Proxy could not reach Tanzu Hub: ${err.message}`,
      hint: "Confirm the Tanzu Hub GraphQL URL is correct and reachable from the plugin server host.",
    });
  }

  let data;
  try { data = JSON.parse(upstream.body); } catch { data = { raw: upstream.body }; }

  res.status(upstream.status).json(data);
}
