import https from "node:https";

/** Dedicated agent so plug-in → vCenter TLS skip is reliable (default globalAgent can ignore per-request rejectUnauthorized in some setups). */
const insecureVcHttpsAgent = new https.Agent({
  rejectUnauthorized: false,
  keepAlive: true,
});

/**
 * @param {string} host
 */
export function normalizeVcHostname(host) {
  return String(host || "")
    .trim()
    .replace(/^https?:\/\//i, "")
    .split("/")[0]
    .split(":")[0];
}

/**
 * @param {unknown} v — request flag or env-style string
 * @returns {boolean}
 */
export function isSkipTlsVerifyFlag(v) {
  if (v === true || v === 1) return true;
  if (typeof v === "string") {
    const s = v.trim().toLowerCase();
    return s === "true" || s === "1" || s === "yes" || s === "on";
  }
  return false;
}

/**
 * @param {unknown} requestInsecure — from UI allowInsecureVcTls; when truthy, skip cert verification for this vCenter HTTPS call.
 */
function tlsRejectUnauthorized(requestInsecure) {
  if (isSkipTlsVerifyFlag(requestInsecure)) return false;
  const v = process.env.VCENTER_REST_TLS_INSECURE;
  return !isSkipTlsVerifyFlag(v);
}

/**
 * @param {{ hostname: string, port?: number, path: string, method?: string, headers?: Record<string, string>, body?: string, insecureTls?: boolean }} opts
 */
export function httpsRequestRaw(opts) {
  const { hostname, port = 443, path, method = "GET", headers = {}, body, insecureTls } = opts;
  const rejectUnauthorized = tlsRejectUnauthorized(insecureTls);
  return new Promise((resolve, reject) => {
    /** @type {import("node:https").RequestOptions} */
    const requestOpts = {
      hostname,
      port,
      path,
      method,
      headers: { Accept: "application/json", ...headers },
      rejectUnauthorized,
    };
    if (rejectUnauthorized === false) {
      requestOpts.agent = insecureVcHttpsAgent;
    }
    const req = https.request(
      requestOpts,
      (res) => {
        const chunks = [];
        res.on("data", (d) => chunks.push(d));
        res.on("end", () => {
          const text = Buffer.concat(chunks).toString("utf8");
          resolve({
            status: res.statusCode || 0,
            headers: res.headers,
            text,
          });
        });
      },
    );
    req.on("error", reject);
    if (body != null && body !== "") req.write(body);
    req.end();
  });
}

/**
 * @param {{ hostname: string, path: string, method?: string, headers?: Record<string, string>, body?: string, insecureTls?: boolean }} opts
 */
async function restJson(opts) {
  const { text, status, headers } = await httpsRequestRaw(opts);
  let json = null;
  try {
    json = text ? JSON.parse(text) : null;
  } catch {
    json = { _parseError: true, _raw: text.slice(0, 2000) };
  }
  return { status, headers, json, text };
}

/**
 * @param {{ vcHost: string, vcUsername: string, vcPassword: string, insecureTls?: boolean }} p
 * @returns {Promise<string>} session id
 */
export async function createVcenterRestSession(p) {
  const hostname = normalizeVcHostname(p.vcHost);
  if (!hostname) throw new Error("vCenter hostname is empty.");
  const auth = Buffer.from(`${p.vcUsername}:${p.vcPassword}`, "utf8").toString("base64");
  const { status, json } = await restJson({
    hostname,
    path: "/rest/com/vmware/cis/session",
    method: "POST",
    headers: {
      Authorization: `Basic ${auth}`,
      "Content-Type": "application/json",
    },
    body: "{}",
    insecureTls: p.insecureTls,
  });
  if (status === 401 || status === 403) {
    throw new Error("vCenter rejected credentials (HTTP 401/403). Check username and password.");
  }
  if (status < 200 || status >= 300) {
    const msg =
      json && typeof json === "object" && json.value && json.value.messages
        ? JSON.stringify(json.value.messages)
        : `HTTP ${status}`;
    throw new Error(`vCenter session failed: ${msg}`);
  }
  const sid = json && typeof json.value === "string" ? json.value : "";
  if (!sid) throw new Error("vCenter session response did not include a session id.");
  return sid;
}

/**
 * @param {{ vcHost: string, sessionId: string, insecureTls?: boolean }} p
 */
export async function deleteVcenterRestSession(p) {
  const hostname = normalizeVcHostname(p.vcHost);
  try {
    await restJson({
      hostname,
      path: "/rest/com/vmware/cis/session",
      method: "DELETE",
      headers: { "vmware-api-session-id": p.sessionId },
      insecureTls: p.insecureTls,
    });
  } catch {
    /* best-effort */
  }
}

/**
 * @param {{ vcHost: string, sessionId: string, insecureTls?: boolean }} p
 * @returns {Promise<Array<{ name: string, id: string }>>}
 */
export async function listDatacentersRest(p) {
  const hostname = normalizeVcHostname(p.vcHost);
  const { status, json } = await restJson({
    hostname,
    path: "/rest/vcenter/datacenter",
    headers: { "vmware-api-session-id": p.sessionId },
    insecureTls: p.insecureTls,
  });
  if (status < 200 || status >= 300) {
    throw new Error(`List datacenters failed (HTTP ${status}).`);
  }
  const rows = Array.isArray(json?.value) ? json.value : [];
  return rows
    .map((r) => ({
      name: typeof r?.name === "string" ? r.name : "",
      id: typeof r?.datacenter === "string" ? r.datacenter : "",
    }))
    .filter((r) => r.name && r.id);
}

/**
 * @param {{ hostname: string, sessionId: string, segment: string, datacenterId: string, insecureTls?: boolean }} p
 */
async function fetchVcenterListFiltered(p) {
  const queries = [
    `datacenters=${encodeURIComponent(p.datacenterId)}`,
    `filter.datacenters=${encodeURIComponent(p.datacenterId)}`,
  ];
  let lastStatus = 0;
  for (const q of queries) {
    const { status, json } = await restJson({
      hostname: p.hostname,
      path: `/rest/vcenter/${p.segment}?${q}`,
      headers: { "vmware-api-session-id": p.sessionId },
      insecureTls: p.insecureTls,
    });
    lastStatus = status;
    if (status >= 200 && status < 300) return json;
  }
  throw new Error(`List ${p.segment} failed (HTTP ${lastStatus}).`);
}

/**
 * @param {{ vcHost: string, sessionId: string, datacenterId: string, insecureTls?: boolean }} p
 */
export async function listClustersRest(p) {
  const json = await fetchVcenterListFiltered({
    hostname: normalizeVcHostname(p.vcHost),
    sessionId: p.sessionId,
    segment: "cluster",
    datacenterId: p.datacenterId,
    insecureTls: p.insecureTls,
  });
  const rows = Array.isArray(json?.value) ? json.value : [];
  return rows
    .map((r) => ({
      name: typeof r?.name === "string" ? r.name : "",
      id: typeof r?.cluster === "string" ? r.cluster : "",
    }))
    .filter((r) => r.name);
}

/**
 * @param {{ vcHost: string, sessionId: string, datacenterId: string, insecureTls?: boolean }} p
 */
export async function listHostsRest(p) {
  const json = await fetchVcenterListFiltered({
    hostname: normalizeVcHostname(p.vcHost),
    sessionId: p.sessionId,
    segment: "host",
    datacenterId: p.datacenterId,
    insecureTls: p.insecureTls,
  });
  const rows = Array.isArray(json?.value) ? json.value : [];
  return rows
    .map((r) => ({
      name: typeof r?.name === "string" ? r.name : "",
      cluster: r?.cluster != null ? String(r.cluster) : "",
    }))
    .filter((r) => r.name);
}

/**
 * @param {{ vcHost: string, sessionId: string, datacenterId: string, insecureTls?: boolean }} p
 */
export async function listDatastoresRest(p) {
  const json = await fetchVcenterListFiltered({
    hostname: normalizeVcHostname(p.vcHost),
    sessionId: p.sessionId,
    segment: "datastore",
    datacenterId: p.datacenterId,
    insecureTls: p.insecureTls,
  });
  const rows = Array.isArray(json?.value) ? json.value : [];
  return rows
    .map((r) => ({
      name: typeof r?.name === "string" ? r.name : "",
      type: typeof r?.type === "string" ? r.type : "",
    }))
    .filter((r) => r.name);
}

const NETWORK_TYPES_SKIP = new Set(["NETWORK_RESOURCE_POOL"]);

/**
 * @param {{ vcHost: string, sessionId: string, datacenterId: string, insecureTls?: boolean }} p
 */
export async function listNetworksRest(p) {
  const json = await fetchVcenterListFiltered({
    hostname: normalizeVcHostname(p.vcHost),
    sessionId: p.sessionId,
    segment: "network",
    datacenterId: p.datacenterId,
    insecureTls: p.insecureTls,
  });
  const rows = Array.isArray(json?.value) ? json.value : [];
  /** @type {{ name: string, type: string }[]} */
  const out = [];
  for (const r of rows) {
    const name = typeof r?.name === "string" ? r.name : "";
    const type = typeof r?.type === "string" ? r.type : "";
    if (!name) continue;
    if (type && NETWORK_TYPES_SKIP.has(type)) continue;
    out.push({ name, type });
  }
  /* de-dupe by name */
  const seen = new Set();
  return out.filter((n) => {
    if (seen.has(n.name)) return false;
    seen.add(n.name);
    return true;
  });
}

/**
 * Build ovftool-style compute paths: host/<clusterOrHostName>
 * @param {{ clusters: { name: string }[], hosts: { name: string, cluster: string }[] }} p
 */
export function buildComputeOptions(p) {
  const clusterNames = new Set(p.clusters.map((c) => c.name));
  /** @type {{ label: string, computePath: string }[]} */
  const opts = [];
  for (const c of p.clusters) {
    opts.push({
      label: `Cluster: ${c.name}`,
      computePath: `host/${c.name}`,
    });
  }
  for (const h of p.hosts) {
    if (h.cluster) continue;
    if (clusterNames.has(h.name)) continue;
    opts.push({
      label: `Host: ${h.name}`,
      computePath: `host/${h.name}`,
    });
  }
  opts.sort((a, b) => a.label.localeCompare(b.label));
  return opts;
}

/**
 * @param {{ vcHost: string, vcUsername: string, vcPassword: string, datacenterName: string, insecureTls?: boolean }} p
 */
export async function fetchPlacementForDatacenter(p) {
  const sessionId = await createVcenterRestSession(p);
  try {
    const dcs = await listDatacentersRest({ vcHost: p.vcHost, sessionId, insecureTls: p.insecureTls });
    const dc = dcs.find((d) => d.name === p.datacenterName);
    if (!dc) {
      throw new Error(`Datacenter "${p.datacenterName}" was not found in vCenter inventory.`);
    }
    const tls = p.insecureTls;
    const [clusters, hosts, datastores, networks] = await Promise.all([
      listClustersRest({ vcHost: p.vcHost, sessionId, datacenterId: dc.id, insecureTls: tls }),
      listHostsRest({ vcHost: p.vcHost, sessionId, datacenterId: dc.id, insecureTls: tls }),
      listDatastoresRest({ vcHost: p.vcHost, sessionId, datacenterId: dc.id, insecureTls: tls }),
      listNetworksRest({ vcHost: p.vcHost, sessionId, datacenterId: dc.id, insecureTls: tls }),
    ]);
    const compute = buildComputeOptions({ clusters, hosts });
    return {
      datacenter: p.datacenterName,
      compute,
      datastores: datastores.map((d) => d.name).sort((a, b) => a.localeCompare(b)),
      networks: networks.map((n) => n.name).sort((a, b) => a.localeCompare(b)),
    };
  } finally {
    await deleteVcenterRestSession({ vcHost: p.vcHost, sessionId, insecureTls: p.insecureTls });
  }
}

/**
 * @param {{ vcHost: string, vcUsername: string, vcPassword: string, insecureTls?: boolean }} p
 */
export async function fetchDatacentersOnly(p) {
  const sessionId = await createVcenterRestSession(p);
  try {
    const dcs = await listDatacentersRest({ vcHost: p.vcHost, sessionId, insecureTls: p.insecureTls });
    return dcs.map((d) => ({ name: d.name, id: d.id })).sort((a, b) => a.name.localeCompare(b.name));
  } finally {
    await deleteVcenterRestSession({ vcHost: p.vcHost, sessionId, insecureTls: p.insecureTls });
  }
}
