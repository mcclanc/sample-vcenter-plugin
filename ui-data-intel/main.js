import { inspectOvaOrOvfFile } from "./ova-inspect.js";

/** Max characters kept in #log to avoid huge DOM. */
const MODAL_LOG_MAX_CHARS = 56_000;

// ── OVA deploy modal: persist non-sensitive fields across refresh / navigation ──
const OVA_FORM_STORAGE_KEY = "di-ova-deploy-form-v1";
const OVA_PERSIST_FIELDS = [
  { id: "vc-host-input",           type: "text"     },
  { id: "vc-user-input",           type: "text"     },
  { id: "vc-insecure-tls",         type: "checkbox" },
  { id: "vm-name-input",           type: "text"     },
  { id: "ovf-network-label-input", type: "text"     },
  { id: "ovf-properties-json",     type: "text"     },
  { id: "datacenter-input",        type: "select"   },
  { id: "compute-path-input",      type: "select"   },
  { id: "datastore-input",         type: "select"   },
  { id: "network-input",           type: "select"   },
];
let ovaFormResetting = false;
function saveOvaFormToStorage() {
  if (ovaFormResetting) return;
  try {
    const data = {};
    for (const f of OVA_PERSIST_FIELDS) {
      const el = document.getElementById(f.id);
      if (!el) continue;
      data[f.id] = f.type === "checkbox" ? el.checked : el.value;
    }
    localStorage.setItem(OVA_FORM_STORAGE_KEY, JSON.stringify(data));
  } catch (_) {}
}
function loadOvaFormStorage() {
  try {
    const raw = localStorage.getItem(OVA_FORM_STORAGE_KEY);
    return raw ? JSON.parse(raw) : null;
  } catch (_) { return null; }
}
function restoreOvaFormStaticFields() {
  const data = loadOvaFormStorage();
  if (!data) return;
  for (const f of OVA_PERSIST_FIELDS) {
    if (f.type === "select") continue;
    const el = document.getElementById(f.id);
    if (!el) continue;
    if (f.type === "checkbox") { el.checked = Boolean(data[f.id]); }
    else if (data[f.id] != null && data[f.id] !== "") { el.value = String(data[f.id]); }
  }
}
function tryRestoreSelectValue(sel) {
  const data = loadOvaFormStorage();
  if (!data) return;
  const saved = data[sel.id];
  if (typeof saved === "string" && saved && [...sel.options].some((o) => o.value === saved)) {
    sel.value = saved;
  }
}
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Append a line to the modal activity log (#log). Newest lines first.
 * @param {string} msg
 * @param {{ error?: boolean, scroll?: boolean }} [opts]
 */
function log(msg, opts = {}) {
  const el = document.getElementById("log");
  if (!el) return;
  const level = opts.error ? "ERROR" : "INFO";
  const line = `${new Date().toISOString()} [${level}] ${msg}`;
  let next = `${line}\n${el.textContent || ""}`;
  if (next.length > MODAL_LOG_MAX_CHARS) {
    next = `${next.slice(0, MODAL_LOG_MAX_CHARS)}\nâ€¦(log truncated)`;
  }
  el.textContent = next;
  if (opts.error || opts.scroll) {
    requestAnimationFrame(() => {
      try {
        el.scrollIntoView({ block: "nearest", behavior: "smooth" });
      } catch {
        el.scrollIntoView(false);
      }
    });
  }
}

function clearModalLog() {
  const el = document.getElementById("log");
  if (el) el.textContent = "";
}

/**
 * vSphere Client: vCenter the user is connected to (from plugin API host URL).
 * @returns {string} hostname or IP, no port
 */
function getRegisteredVcenterHostname() {
  try {
    const sdk = globalThis.htmlClientSdk;
    if (!sdk?.app?.getApiEndpoints) return "";
    const eps = sdk.app.getApiEndpoints();
    const origin = eps?.uiApiEndpoint?.origin;
    if (typeof origin !== "string" || !origin.trim()) return "";
    const u = new URL(origin);
    return u.hostname || "";
  } catch {
    return "";
  }
}

function prefillVcHostIfEmpty() {
  const el = document.getElementById("vc-host-input");
  if (!(el instanceof HTMLInputElement)) return;
  if (el.value.trim()) return;
  const host = getRegisteredVcenterHostname();
  if (host) el.value = host;
}

function whenSdkReady() {
  const sdk = globalThis.htmlClientSdk;
  if (!sdk) {
    log("htmlClientSdk missing â€” expected script /api/ui/htmlClientSdk.js");
    return;
  }
  sdk.initialize(() => {
    log(
      `htmlClientSdk initialized. locale=${sdk.app.getClientLocale()} theme=${JSON.stringify(sdk.app.getTheme())}`,
    );
    log(`getProxiedPluginServerOrigin()=${sdk.getProxiedPluginServerOrigin()}`);
  });
}

whenSdkReady();

const modal = document.getElementById("ova-modal");
const closeBtn = document.getElementById("ova-modal-close");
const cancelBtn = document.getElementById("ova-modal-cancel");
const deployBtn = document.getElementById("ova-deploy-btn");
const deployStep = document.getElementById("ova-modal-step-deploy");
const statusEl = document.getElementById("ova-modal-status");
const fileInput = document.getElementById("ova-file-input");
const networkRowsWrap = document.getElementById("ova-network-rows-wrap");
const networkRows = document.getElementById("ova-network-rows");
const networkStatic = document.getElementById("ova-network-static");
const ovfDynWrap = document.getElementById("ovf-dynamic-properties-wrap");
const ovfDyn = document.getElementById("ovf-dynamic-properties");

/** Parsed OVF descriptor from the selected package (null until read succeeds). */
let lastDescriptor = null;

const ovaModalOpenIds = [
  "di-get-started-btn",
];

const pathModal = document.getElementById("usecase-path-modal");
const pathModalClose = document.getElementById("usecase-path-modal-close");
const pathPocBtn = document.getElementById("usecase-path-poc");
const pathProductionBtn = document.getElementById("usecase-path-production");
const pathCancelBtn = document.getElementById("usecase-path-cancel");

function openUsecasePathModal() {
  if (!(pathModal instanceof HTMLDialogElement)) return;
  pathModal.showModal();
  pathPocBtn?.focus();
}

function closeUsecasePathModal() {
  if (!(pathModal instanceof HTMLDialogElement)) return;
  pathModal.close();
}

const deployHelpModal = document.getElementById("deploy-help-modal");

function openDeployHelpModal() {
  if (!(deployHelpModal instanceof HTMLDialogElement)) return;
  deployHelpModal.showModal();
}

function closeDeployHelpModal() {
  if (!(deployHelpModal instanceof HTMLDialogElement)) return;
  deployHelpModal.close();
}

document.getElementById("deploy-help-open")?.addEventListener("click", openDeployHelpModal);
document.getElementById("deploy-help-close")?.addEventListener("click", closeDeployHelpModal);
deployHelpModal?.addEventListener("click", (e) => {
  if (e.target === deployHelpModal) closeDeployHelpModal();
});
document.getElementById("deploy-help-choose-poc")?.addEventListener("click", () => {
  closeDeployHelpModal();
  closeUsecasePathModal();
  openOvaModal();
});
document.getElementById("deploy-help-choose-prod")?.addEventListener("click", () => {
  closeDeployHelpModal();
  closeUsecasePathModal();
  globalThis.location.assign("deploy-production.html?mode=production-ha");
});

/** vSphere Client: real backend base from SDK (often correct when getProxiedPluginServerOrigin matches UI host). */
let pluginBackendBaseCache = "";
/**
 * True after getPluginBackendBaseCached finished once (URL or empty). Cleared on modal reset so each open can probe again.
 * Avoids hanging forever when getPluginBackendInfo never invokes its callback.
 */
let pluginBackendSdkProbeDone = false;

const PLUGIN_BACKEND_SDK_TIMEOUT_MS = 4000;

function getPluginBackendBaseFromSdk() {
  return new Promise((resolve) => {
    const sdk = globalThis.htmlClientSdk;
    if (!sdk?.app?.getPluginBackendInfo) {
      resolve("");
      return;
    }
    try {
      sdk.app.getPluginBackendInfo((info) => {
        try {
          const list = info?.allPluginBackendServers;
          if (!Array.isArray(list)) {
            resolve("");
            return;
          }
          const u = list.map((s) => s?.proxiedBaseUrl).find((x) => typeof x === "string" && x.trim());
          resolve(typeof u === "string" ? u.trim().replace(/\/+$/, "") : "");
        } catch {
          resolve("");
        }
      });
    } catch {
      resolve("");
    }
  });
}

async function getPluginBackendBaseCached() {
  if (pluginBackendBaseCache) return pluginBackendBaseCache;
  if (pluginBackendSdkProbeDone) return "";
  const u = await new Promise((resolve) => {
    let settled = false;
    const finish = (/** @type {string} */ value) => {
      if (settled) return;
      settled = true;
      clearTimeout(tid);
      resolve(typeof value === "string" ? value : "");
    };
    const tid = setTimeout(() => finish(""), PLUGIN_BACKEND_SDK_TIMEOUT_MS);
    getPluginBackendBaseFromSdk().then((v) => finish(v)).catch(() => finish(""));
  });
  pluginBackendSdkProbeDone = true;
  if (u) pluginBackendBaseCache = u;
  return u || "";
}

/**
 * @param {string[]} urls
 */
function uniqOrdered(urls) {
  const seen = new Set();
  const out = [];
  for (const u of urls) {
    if (typeof u !== "string" || !u) continue;
    if (seen.has(u)) continue;
    seen.add(u);
    out.push(u);
  }
  return out;
}

/**
 * Prefer same-origin URLs (typically vCenter â†’ plug-in reverse proxy) before direct
 * https://&lt;plug-in-host&gt;:â€¦ URLs. Browsers cannot ignore self-signed certs for fetch();
 * direct plug-in TLS failures show as "Failed to fetch".
 * @param {string[]} urls
 */
function sortFetchUrlsSameOriginFirst(urls) {
  const pageOrigin = typeof window !== "undefined" && window.location?.origin ? window.location.origin : "";
  /** @param {string} url */
  function crossOriginTier(url) {
    if (!pageOrigin || typeof url !== "string") return 0;
    try {
      const abs = new URL(url, typeof document !== "undefined" ? document.baseURI : pageOrigin);
      return abs.origin === pageOrigin ? 0 : 1;
    } catch {
      return 0;
    }
  }
  return uniqOrdered(urls).sort((a, b) => crossOriginTier(a) - crossOriginTier(b));
}

/**
 * Parse the direct plugin server origin from a vCenter proxy path.
 * The proxy segment looks like "192.168.68.5-8443" or "hostname.local-8443".
 * e.g. "/plugins/com.example~1.0/192.168.1.1-8443"  â†’ "https://192.168.1.1:8443"
 * @param {string} proxiedBaseUrl
 * @returns {string}
 */
function parseDirectPluginOriginFromProxiedUrl(proxiedBaseUrl) {
  if (!proxiedBaseUrl || typeof proxiedBaseUrl !== "string") return "";
  try {
    const segment = proxiedBaseUrl.replace(/\/+$/, "").split("/").pop() || "";
    const lastDash = segment.lastIndexOf("-");
    if (lastDash < 1) return "";
    const host = segment.slice(0, lastDash);
    const port = segment.slice(lastDash + 1);
    if (!host || !/^\d+$/.test(port)) return "";
    return `https://${host}:${port}`;
  } catch {
    return "";
  }
}

/**
 * @param {string} root
 * @param {"deploy" | "inventory"} kind
 */
function urlsFromPluginRoot(root, kind) {
  const urls = [];
  const r = String(root || "").trim().replace(/\/+$/, "");
  if (!r) return urls;
  const withUi = r.endsWith("/data-intel-ui") ? r : `${r}/data-intel-ui`;
  const tail = kind === "deploy" ? "api/eval-appliance/deploy" : "api/vcenter/inventory";
  urls.push(`${withUi}/${tail}`);
  urls.push(`${r}/${tail}`);
  return urls;
}

/**
 * @param {string[]} extraRoots â€” from getPluginBackendInfo
 * @returns {{ urls: string[], directOrigin: string }}
 */
function candidateDeployUrls(extraRoots = []) {
  const urls = [];
  try {
    urls.push(new URL("api/eval-appliance/deploy", document.baseURI).href);
  } catch {
    /* ignore */
  }
  for (const root of extraRoots) {
    urls.push(...urlsFromPluginRoot(root, "deploy"));
  }
  const sdk = globalThis.htmlClientSdk;
  const plug =
    sdk && typeof sdk.getProxiedPluginServerOrigin === "function"
      ? sdk.getProxiedPluginServerOrigin()
      : "";
  const o = typeof plug === "string" ? plug.trim().replace(/\/+$/, "") : "";
  if (o) urls.push(...urlsFromPluginRoot(o, "deploy"));
  if (!extraRoots.length && !o && typeof window !== "undefined" && window.location?.origin) {
    urls.push(...urlsFromPluginRoot(window.location.origin, "deploy"));
  }
  /* Parse the direct plugin server address (e.g. https://192.168.68.5:8443) from the proxied
   * base URL ("â€¦/192.168.68.5-8443"). Added last so the vCenter proxy is tried first, but if
   * the proxy times out (408) the browser can reach the plugin directly once its cert is trusted. */
  const proxyBase = extraRoots[0] || o || "";
  const directOrigin = parseDirectPluginOriginFromProxiedUrl(proxyBase);
  if (directOrigin) urls.push(...urlsFromPluginRoot(directOrigin, "deploy"));
  const out = sortFetchUrlsSameOriginFirst(urls);
  if (out.length === 0) out.push(new URL("api/eval-appliance/deploy", document.baseURI).href);
  return { urls: out, directOrigin };
}

/** @param {string[]} extraRoots */
function candidateVcInventoryUrls(extraRoots = []) {
  const urls = [];
  try {
    urls.push(new URL("api/vcenter/inventory", document.baseURI).href);
  } catch {
    /* ignore */
  }
  for (const root of extraRoots) {
    urls.push(...urlsFromPluginRoot(root, "inventory"));
  }
  const sdk = globalThis.htmlClientSdk;
  const plug =
    sdk && typeof sdk.getProxiedPluginServerOrigin === "function"
      ? sdk.getProxiedPluginServerOrigin()
      : "";
  const o = typeof plug === "string" ? plug.trim().replace(/\/+$/, "") : "";
  if (o) urls.push(...urlsFromPluginRoot(o, "inventory"));
  if (!extraRoots.length && !o && typeof window !== "undefined" && window.location?.origin) {
    urls.push(...urlsFromPluginRoot(window.location.origin, "inventory"));
  }
  const proxyBase = extraRoots[0] || o || "";
  const directOrigin = parseDirectPluginOriginFromProxiedUrl(proxyBase);
  if (directOrigin) urls.push(...urlsFromPluginRoot(directOrigin, "inventory"));
  const out = sortFetchUrlsSameOriginFirst(urls);
  if (out.length === 0) out.push(new URL("api/vcenter/inventory", document.baseURI).href);
  return out;
}

/**
 * @param {string} id
 */
function readControlValue(id) {
  const el = document.getElementById(id);
  if (el instanceof HTMLSelectElement || el instanceof HTMLInputElement) return el.value.trim();
  return "";
}

function vcInventoryTlsFlag() {
  const el =
    document.querySelector("#ova-modal #vc-insecure-tls") || document.getElementById("vc-insecure-tls");
  return el instanceof HTMLInputElement && el.type === "checkbox" && el.checked;
}

/**
 * @param {Record<string, unknown>} body
 */
function inventoryBodyWantsInsecureTls(body) {
  const v = body?.allowInsecureVcTls;
  if (v === true || v === 1) return true;
  if (typeof v === "string") {
    const s = v.trim().toLowerCase();
    return s === "true" || s === "1" || s === "yes" || s === "on";
  }
  return false;
}

/**
 * @param {string} url
 * @param {boolean} insecure
 */
function withInventoryInsecureQuery(url, insecure) {
  if (!insecure) return url;
  try {
    const u = new URL(url, typeof document !== "undefined" ? document.baseURI : "http://localhost/");
    u.searchParams.set("allowInsecureVcTls", "1");
    return u.pathname + u.search + u.hash;
  } catch {
    return url.includes("?") ? `${url}&allowInsecureVcTls=1` : `${url}?allowInsecureVcTls=1`;
  }
}

/** Port group names from last vCenter placement load (for OVF multi-network rows). */
let vcNetworkChoices = [];

/**
 * @param {HTMLSelectElement} sel
 * @param {string} placeholder
 * @param {string[]} values
 */
function fillStringSelect(sel, placeholder, values) {
  const prev = sel.value;
  sel.replaceChildren();
  const ph = document.createElement("option");
  ph.value = "";
  ph.textContent = placeholder;
  sel.appendChild(ph);
  for (const v of values) {
    const o = document.createElement("option");
    o.value = v;
    o.textContent = v;
    sel.appendChild(o);
  }
  if (prev && [...sel.options].some((opt) => opt.value === prev)) { sel.value = prev; }
  else { tryRestoreSelectValue(sel); }
}

/**
 * @param {HTMLSelectElement} sel
 * @param {{ label: string, computePath: string }[]} compute
 */
function fillComputeSelect(sel, compute) {
  const prev = sel.value;
  sel.replaceChildren();
  const ph = document.createElement("option");
  ph.value = "";
  ph.textContent = "Select cluster or hostâ€¦";
  sel.appendChild(ph);
  for (const c of compute) {
    const o = document.createElement("option");
    o.value = c.computePath;
    o.textContent = c.label;
    sel.appendChild(o);
  }
  if (prev && [...sel.options].some((opt) => opt.value === prev)) { sel.value = prev; }
  else { tryRestoreSelectValue(sel); }
}

/**
 * @param {HTMLSelectElement} sel
 * @param {string} placeholder
 * @param {{ name: string }[]} datacenters
 */
function fillDatacenterSelect(sel, placeholder, datacenters) {
  const prev = sel.value;
  sel.replaceChildren();
  const ph = document.createElement("option");
  ph.value = "";
  ph.textContent = placeholder;
  sel.appendChild(ph);
  for (const d of datacenters) {
    const o = document.createElement("option");
    o.value = d.name;
    o.textContent = d.name;
    sel.appendChild(o);
  }
  if (prev && [...sel.options].some((opt) => opt.value === prev)) { sel.value = prev; }
  else { tryRestoreSelectValue(sel); }
}

/**
 * @param {Record<string, unknown>} body
 */
async function postVcenterInventory(body) {
  const bb = await getPluginBackendBaseCached();
  const urls = candidateVcInventoryUrls(bb ? [bb] : []);
  let res = null;
  let text = "";
  let usedUrl = "";
  const insecure = inventoryBodyWantsInsecureTls(body);
  for (const url of urls) {
    const urlToUse = withInventoryInsecureQuery(url, insecure);
    usedUrl = urlToUse;
    /** @type {Record<string, string>} */
    const headers = { "Content-Type": "application/json", Accept: "application/json" };
    if (insecure) headers["X-Allow-Insecure-Vc-Tls"] = "1";
    res = await fetch(urlToUse, {
      method: "POST",
      headers,
      body: JSON.stringify(body),
    });
    text = await res.text();
    const idx = urls.indexOf(url);
    const canRetry = idx < urls.length - 1;
    const looksLikeExpress404 =
      !res.ok && res.status === 404 && /Cannot POST/i.test(text) && /<!DOCTYPE/i.test(text);
    /* vCenter UI often returns plain 404 HTML without "Cannot POST" â€” still wrong host; try next URL. */
    if (!res.ok && canRetry && (res.status === 404 || res.status === 405) && (looksLikeExpress404 || !text.trim().startsWith("{"))) {
      log(`POST ${url} â†’ ${res.status} â€” trying next inventory URLâ€¦`);
      continue;
    }
    break;
  }
  let data;
  try {
    data = JSON.parse(text);
  } catch {
    data = { raw: text };
  }
  return { res, data, usedUrl, text };
}

/**
 * @param {string} datacenterName
 */
async function loadVcPlacement(datacenterName) {
  const dc = datacenterName.trim();
  if (!dc) return;
  const vcHost = readControlValue("vc-host-input");
  const vcUsername = readControlValue("vc-user-input");
  const vcPassword =
    document.getElementById("vc-pass-input") instanceof HTMLInputElement
      ? document.getElementById("vc-pass-input").value
      : "";
  const computeSel = document.getElementById("compute-path-input");
  const dsSel = document.getElementById("datastore-input");
  const netSel = document.getElementById("network-input");
  if (!(computeSel instanceof HTMLSelectElement && dsSel instanceof HTMLSelectElement && netSel instanceof HTMLSelectElement)) {
    return;
  }
  const { res, data } = await postVcenterInventory({
    vcHost,
    vcUsername,
    vcPassword,
    datacenter: dc,
    allowInsecureVcTls: vcInventoryTlsFlag(),
  });
  if (!res.ok) {
    const err = typeof data.error === "string" ? data.error : `HTTP ${res.status}`;
    setStatus(err, true);
    vcNetworkChoices = [];
    return;
  }
  const compute = Array.isArray(data.compute) ? data.compute : [];
  const datastores = Array.isArray(data.datastores) ? data.datastores : [];
  const networks = Array.isArray(data.networks) ? data.networks : [];
  fillComputeSelect(computeSel, compute);
  fillStringSelect(dsSel, "Select datastoreâ€¦", datastores);
  fillStringSelect(netSel, "Select port groupâ€¦", networks);
  vcNetworkChoices = networks;
  if (lastDescriptor?.networks?.length) {
    renderNetworkRows(lastDescriptor.networks);
  }
  setStatus(`Loaded placement for datacenter "${dc}".`, false);
}

function setStatus(message, isError = false) {
  const text = message || "";
  if (statusEl instanceof HTMLElement) {
    statusEl.textContent = text;
    statusEl.classList.toggle("ova-modal__status--error", Boolean(isError && text));
  } else {
    log(`[status${isError ? " ERROR" : ""}] ${text}`, { error: isError });
  }
}

/** Deploy / OVF validation: status line + modal #log (errors scroll log into view). */
function deployModalError(message) {
  setStatus(message, true);
  log(message, { error: true });
}

function deployModalInfo(message) {
  setStatus(message, false);
  log(message, { scroll: true });
}

function clearDescriptorUi() {
  lastDescriptor = null;
  if (networkRows) networkRows.replaceChildren();
  if (networkRowsWrap instanceof HTMLElement) networkRowsWrap.hidden = true;
  if (networkStatic instanceof HTMLElement) networkStatic.hidden = false;
  if (ovfDyn) ovfDyn.replaceChildren();
  if (ovfDynWrap instanceof HTMLElement) ovfDynWrap.hidden = true;
}

function resetOvaModal() {
  setStatus("");
  pluginBackendBaseCache = "";
  pluginBackendSdkProbeDone = false;
  vcNetworkChoices = [];
  const form = document.getElementById("ova-install-form");
  ovaFormResetting = true;
  if (form instanceof HTMLFormElement) form.reset();
  ovaFormResetting = false;
  clearDescriptorUi();
  if (deployBtn instanceof HTMLButtonElement) deployBtn.disabled = true;
}

function openOvaModal() {
  if (!(modal instanceof HTMLDialogElement)) return;
  resetOvaModal();
  restoreOvaFormStaticFields();
  if (deployStep instanceof HTMLElement) deployStep.hidden = false;
  prefillVcHostIfEmpty();
  setStatus(
    "Choose an .ova or .ovf file below. vCenter and placement fields stay visibleâ€”you can fill them before or after the descriptor loads.",
  );
  modal.showModal();
  fileInput?.focus();
}

function closeOvaModal() {
  if (!(modal instanceof HTMLDialogElement)) return;
  modal.close();
  resetOvaModal();
  if (deployStep instanceof HTMLElement) deployStep.hidden = true;
}

for (const id of ovaModalOpenIds) {
  document.getElementById(id)?.addEventListener("click", openUsecasePathModal);
}
pathModalClose?.addEventListener("click", closeUsecasePathModal);
pathCancelBtn?.addEventListener("click", closeUsecasePathModal);
pathPocBtn?.addEventListener("click", () => {
  closeUsecasePathModal();
  openOvaModal();
});
pathProductionBtn?.addEventListener("click", () => {
  closeUsecasePathModal();
  globalThis.location.assign("deploy-production.html?mode=production-ha");
});

pathModal?.addEventListener("click", (e) => {
  if (e.target === pathModal) closeUsecasePathModal();
});

closeBtn?.addEventListener("click", closeOvaModal);
cancelBtn?.addEventListener("click", closeOvaModal);

modal?.addEventListener("click", (e) => {
  if (e.target === modal) closeOvaModal();
});

async function onVcConnect() {
  const btn = document.getElementById("vc-connect-btn");
  const vcHost = readControlValue("vc-host-input");
  const vcUsername = readControlValue("vc-user-input");
  const vcPassword =
    document.getElementById("vc-pass-input") instanceof HTMLInputElement
      ? document.getElementById("vc-pass-input").value
      : "";
  if (!vcHost || !vcUsername || !vcPassword) {
    setStatus("Enter vCenter hostname, username, and password before Connect.", true);
    return;
  }
  const dcSel = document.getElementById("datacenter-input");
  if (!(dcSel instanceof HTMLSelectElement)) return;
  if (btn instanceof HTMLButtonElement) btn.disabled = true;
  setStatus("Connecting to vCenterâ€¦");
  try {
    const { res, data, usedUrl, text } = await postVcenterInventory({
      vcHost,
      vcUsername,
      vcPassword,
      allowInsecureVcTls: vcInventoryTlsFlag(),
    });
    if (!res.ok) {
      const err = typeof data.error === "string" ? data.error : `HTTP ${res.status}`;
      const hint = typeof data.hint === "string" ? ` ${data.hint}` : "";
      log(`Inventory POST ${usedUrl} â†’ ${res.status}: ${String(text).slice(0, 500)}`);
      let probe = "";
      try {
        probe = new URL("api/vcenter/inventory", document.baseURI).href;
      } catch {
        probe = "";
      }
      setStatus(
        `${err}${hint} Confirm the plug-in server exposes POST /api/vcenter/inventory. Probe in browser: GET ${probe || "â€¦/api/vcenter/inventory"} (expect JSON, not 404).`,
        true,
      );
      return;
    }
    const dcs = Array.isArray(data.datacenters) ? data.datacenters : [];
    if (!dcs.length) {
      setStatus("vCenter returned no datacenters (check account permissions).", true);
      return;
    }
    fillDatacenterSelect(dcSel, "Select datacenterâ€¦", dcs);
    const first = typeof dcs[0]?.name === "string" ? dcs[0].name : "";
    if (first) {
      dcSel.value = first;
      await loadVcPlacement(first);
    } else {
      setStatus(`Connected. Found ${dcs.length} datacenter(s). Select one to load placement.`, false);
    }
  } catch (e) {
    setStatus(e instanceof Error ? e.message : String(e), true);
  } finally {
    if (btn instanceof HTMLButtonElement) btn.disabled = false;
  }
}

document.getElementById("vc-connect-btn")?.addEventListener("click", () => void onVcConnect());

document.getElementById("datacenter-input")?.addEventListener("change", () => {
  const v = readControlValue("datacenter-input");
  const computeSel = document.getElementById("compute-path-input");
  const dsSel = document.getElementById("datastore-input");
  const netSel = document.getElementById("network-input");
  if (!v) {
    vcNetworkChoices = [];
    if (computeSel instanceof HTMLSelectElement) fillComputeSelect(computeSel, []);
    if (dsSel instanceof HTMLSelectElement) fillStringSelect(dsSel, "Select datastoreâ€¦", []);
    if (netSel instanceof HTMLSelectElement) fillStringSelect(netSel, "Select port groupâ€¦", []);
    if (lastDescriptor?.networks?.length) renderNetworkRows(lastDescriptor.networks);
    return;
  }
  void loadVcPlacement(v);
});

function looksLikePasswordKey(key) {
  return /password|passwd|secret|credential|root_pw|admin_pw|token/i.test(key);
}

/**
 * @param {{ name: string }[]} networks
 */
function renderNetworkRows(networks) {
  if (!networkRows || !networkRowsWrap || !networkStatic) return;
  networkRows.replaceChildren();
  if (!networks.length) {
    networkRowsWrap.hidden = true;
    networkStatic.hidden = false;
    return;
  }
  networkRowsWrap.hidden = false;
  networkStatic.hidden = true;
  networks.forEach((net, i) => {
    const row = document.createElement("div");
    row.className = "ova-network-row";
    const lab = document.createElement("label");
    const intro = document.createElement("span");
    intro.className = "ova-network-row__intro";
    intro.append("vSphere port group for OVF network ");
    const strong = document.createElement("strong");
    strong.textContent = net.name;
    intro.appendChild(strong);
    lab.appendChild(intro);
    /** @type {HTMLInputElement | HTMLSelectElement} */
    let pgControl;
    if (vcNetworkChoices.length > 0) {
      const sel = document.createElement("select");
      sel.id = `ova-net-pg-${i}`;
      sel.autocomplete = "off";
      sel.dataset.ovfNetwork = net.name;
      const ph = document.createElement("option");
      ph.value = "";
      ph.textContent = "Select port groupâ€¦";
      sel.appendChild(ph);
      for (const n of vcNetworkChoices) {
        const o = document.createElement("option");
        o.value = n;
        o.textContent = n;
        sel.appendChild(o);
      }
      pgControl = sel;
    } else {
      const inp = document.createElement("input");
      inp.type = "text";
      inp.id = `ova-net-pg-${i}`;
      inp.autocomplete = "off";
      inp.dataset.ovfNetwork = net.name;
      pgControl = inp;
    }
    lab.appendChild(pgControl);
    row.appendChild(lab);
    networkRows.appendChild(row);
  });
}

/** @param {Array<{ key: string, type: string, defaultValue: string, userConfigurable: boolean, label: string, description: string }>} properties */
function renderOvfProperties(properties) {
  if (!ovfDyn || !ovfDynWrap) return;
  ovfDyn.replaceChildren();
  const configurable = properties.filter((p) => p.userConfigurable);
  if (!configurable.length) {
    ovfDynWrap.hidden = true;
    return;
  }
  ovfDynWrap.hidden = false;
  configurable.forEach((p, i) => {
    const wrap = document.createElement("div");
    wrap.className = "ovf-dynamic-prop";
    const lab = document.createElement("label");
    const title = document.createElement("span");
    title.className = "ovf-dynamic-prop__title";
    title.textContent = p.label;
    lab.appendChild(title);
    if (p.description) {
      const desc = document.createElement("span");
      desc.className = "ovf-dynamic-prop__desc";
      desc.textContent = p.description;
      lab.appendChild(desc);
    }
    const typeLower = (p.type || "").toLowerCase();
    /** @type {HTMLInputElement} */
    let control;
    if (typeLower === "boolean" || typeLower === "bool") {
      control = document.createElement("input");
      control.type = "checkbox";
      const d = String(p.defaultValue || "").toLowerCase();
      control.checked = d === "true" || d === "1" || d === "yes";
    } else if (
      typeLower.includes("int") ||
      typeLower === "real" ||
      typeLower.includes("uint") ||
      typeLower === "float"
    ) {
      control = document.createElement("input");
      control.type = "number";
      control.step = typeLower.includes("int") || typeLower.includes("uint") ? "1" : "any";
      if (p.defaultValue !== undefined && p.defaultValue !== "") control.value = String(p.defaultValue);
    } else {
      control = document.createElement("input");
      control.type = looksLikePasswordKey(p.key) ? "password" : "text";
      control.value = p.defaultValue ? String(p.defaultValue) : "";
      control.autocomplete = "off";
    }
    control.id = `ovf-prop-field-${i}`;
    control.dataset.ovfKey = p.key;
    lab.appendChild(control);
    wrap.appendChild(lab);
    ovfDyn.appendChild(wrap);
  });
  wrapConfigurableCountForCollect(configurable.length);
}

let configurablePropCount = 0;

function wrapConfigurableCountForCollect(n) {
  configurablePropCount = n;
}

/**
 * @returns {Record<string, string | number | boolean>}
 */
function collectOvfPropertiesFromForm() {
  /** @type {Record<string, string | number | boolean>} */
  const props = {};
  for (let i = 0; i < configurablePropCount; i += 1) {
    const el = document.getElementById(`ovf-prop-field-${i}`);
    if (!(el instanceof HTMLInputElement)) continue;
    const key = el.dataset.ovfKey;
    if (!key) continue;
    if (el.type === "checkbox") {
      props[key] = el.checked;
      continue;
    }
    const raw = el.value.trim();
    if (raw === "") continue;
    if (el.type === "number") {
      const n = Number(raw);
      props[key] = Number.isFinite(n) ? n : raw;
    } else {
      props[key] = raw;
    }
  }
  return props;
}

async function onOvaFileSelected() {
  clearDescriptorUi();
  const files = fileInput instanceof HTMLInputElement ? fileInput.files : null;
  if (!files || files.length === 0) {
    if (deployBtn instanceof HTMLButtonElement) deployBtn.disabled = true;
    setStatus(
      "Choose an .ova or .ovf file. vCenter and placement fields above stay available while you browse.",
    );
    return;
  }
  const file = files[0];
  const name = file.name.toLowerCase();
  if (!name.endsWith(".ova") && !name.endsWith(".ovf")) {
    setStatus("File must be an .ova or .ovf package.", true);
    if (deployBtn instanceof HTMLButtonElement) deployBtn.disabled = true;
    return;
  }

  setStatus("Reading OVF descriptor from packageâ€¦");
  if (deployBtn instanceof HTMLButtonElement) deployBtn.disabled = true;
  if (deployStep instanceof HTMLElement) deployStep.hidden = false;

  try {
    lastDescriptor = await inspectOvaOrOvfFile(file);
    renderNetworkRows(lastDescriptor.networks);
    renderOvfProperties(lastDescriptor.properties);

    const vmInput = document.getElementById("vm-name-input");
    if (vmInput instanceof HTMLInputElement && lastDescriptor.vmNameSuggestion && !vmInput.value.trim()) {
      vmInput.value = lastDescriptor.vmNameSuggestion;
    }
    if (!lastDescriptor.networks.length) {
      /* Network label field: leave blank so the server auto-detects it from the OVF.
         The placeholder text already shows "VM Network" as a hint. */
    }

    setStatus("Descriptor loaded. Enter vCenter details, map networks, then click Deploy.");
    if (deployBtn instanceof HTMLButtonElement) deployBtn.disabled = false;
  } catch (err) {
    lastDescriptor = null;
    renderNetworkRows([]);
    renderOvfProperties([]);
    configurablePropCount = 0;
    if (deployBtn instanceof HTMLButtonElement) deployBtn.disabled = false;
    setStatus(
      `${err instanceof Error ? err.message : String(err)} You can still deploy: set port group and OVF network label below, add any OVF properties under Advanced JSON, then click Deploy.`,
      true,
    );
  }
}

fileInput?.addEventListener("change", () => {
  void onOvaFileSelected();
});

// ── Wire up storage listeners ─────────────────────────────────────────────────
for (const f of OVA_PERSIST_FIELDS) {
  const el = document.getElementById(f.id);
  if (!el) continue;
  el.addEventListener("input",  saveOvaFormToStorage);
  el.addEventListener("change", saveOvaFormToStorage);
}
// ─────────────────────────────────────────────────────────────────────────────

deployBtn?.addEventListener("click", async () => {
  const files = fileInput instanceof HTMLInputElement ? fileInput.files : null;
  if (!files || files.length === 0) {
    deployModalError("Choose an .ova or .ovf file first.");
    fileInput?.focus();
    return;
  }
  const file = files[0];
  const name = file.name.toLowerCase();
  if (!name.endsWith(".ova") && !name.endsWith(".ovf")) {
    deployModalError("File must be an .ova or .ovf package.");
    fileInput?.focus();
    return;
  }
  const vcHost = (document.getElementById("vc-host-input") instanceof HTMLInputElement
    ? document.getElementById("vc-host-input").value
    : ""
  ).trim();
  const vcUsername = (document.getElementById("vc-user-input") instanceof HTMLInputElement
    ? document.getElementById("vc-user-input").value
    : ""
  ).trim();
  const vcPassword =
    document.getElementById("vc-pass-input") instanceof HTMLInputElement
      ? document.getElementById("vc-pass-input").value
      : "";
  const vmName = (document.getElementById("vm-name-input") instanceof HTMLInputElement
    ? document.getElementById("vm-name-input").value
    : ""
  ).trim();
  const datacenter = readControlValue("datacenter-input");
  const computePath = readControlValue("compute-path-input");
  const datastore = readControlValue("datastore-input");

  const ovfEl = document.getElementById("ovf-properties-json");
  const ovfRaw = ovfEl instanceof HTMLTextAreaElement ? ovfEl.value.trim() : "";
  let advancedProps = null;
  if (ovfRaw) {
    try {
      advancedProps = JSON.parse(ovfRaw);
      if (typeof advancedProps !== "object" || advancedProps === null || Array.isArray(advancedProps)) {
        deployModalError("Advanced OVF properties must be a JSON object (not an array).");
        return;
      }
    } catch (err) {
      deployModalError(`Invalid advanced OVF properties JSON: ${err instanceof Error ? err.message : String(err)}`);
      return;
    }
  }

  const fromForm = collectOvfPropertiesFromForm();
  const ovfPropertiesObj =
    advancedProps && typeof advancedProps === "object"
      ? { ...fromForm, ...advancedProps }
      : { ...fromForm };
  const ovfPropertiesJson = Object.keys(ovfPropertiesObj).length ? JSON.stringify(ovfPropertiesObj) : "";

  let network = "";
  let ovfNetworkLabel = "";
  let ovfNetworkMappingsJson = "";

  if (lastDescriptor && lastDescriptor.networks.length > 0) {
    /** @type {Record<string, string>} */
    const map = {};
    for (let i = 0; i < lastDescriptor.networks.length; i += 1) {
      const el = document.getElementById(`ova-net-pg-${i}`);
      const ovfName = lastDescriptor.networks[i]?.name || "";
      const pg =
        el instanceof HTMLSelectElement || el instanceof HTMLInputElement ? el.value.trim() : "";
      if (!ovfName || !pg) {
        deployModalError(`Enter a vSphere port group for every OVF network (missing mapping for "${ovfName || "?"}").`);
        el?.focus();
        return;
      }
      map[ovfName] = pg;
    }
    ovfNetworkMappingsJson = JSON.stringify(map);
  } else {
    network = readControlValue("network-input");
    ovfNetworkLabel = (
      document.getElementById("ovf-network-label-input") instanceof HTMLInputElement
        ? document.getElementById("ovf-network-label-input").value
        : ""
    ).trim();
  }

  if (!vcHost || !vcUsername || !vcPassword) {
    deployModalError("vCenter hostname, username, and password are required.");
    return;
  }
  if (!datacenter || !computePath) {
    deployModalError("Datacenter and compute (cluster or host) are required. Use Connect to load values from vCenter.");
    return;
  }
  if (!vmName || !datastore) {
    deployModalError("VM name and datastore are required.");
    return;
  }
  if (!ovfNetworkMappingsJson && !network) {
    deployModalError("Destination port group is required.");
    return;
  }

  clearModalLog();
  const sizeMiB = typeof file.size === "number" && file.size >= 0 ? (file.size / (1024 * 1024)).toFixed(2) : "?";
  log(`Deploy: start â€” file=${file.name} sizeâ‰ˆ${sizeMiB} MiB`, { scroll: true });

  /** Fresh FormData each attempt â€” a consumed body cannot be retried on another URL. */
  function buildDeployFormData() {
    const f = new FormData();
    f.append("ovaFile", file, file.name);
    f.append("vcHost", vcHost);
    f.append("vcUsername", vcUsername);
    f.append("vcPassword", vcPassword);
    f.append("vmName", vmName);
    f.append("datacenter", datacenter);
    f.append("computePath", computePath);
    f.append("datastore", datastore);
    if (ovfNetworkMappingsJson) {
      f.append("ovfNetworkMappings", ovfNetworkMappingsJson);
    } else {
      f.append("network", network);
      if (ovfNetworkLabel) f.append("ovfNetworkLabel", ovfNetworkLabel);
    }
    if (ovfPropertiesJson) f.append("ovfProperties", ovfPropertiesJson);
    return f;
  }

  deployModalInfo("Resolving plug-in server for uploadâ€¦");
  if (deployBtn instanceof HTMLButtonElement) deployBtn.disabled = true;
  try {
    const bb = await getPluginBackendBaseCached();
    const { urls, directOrigin: directPluginOrigin } = candidateDeployUrls(bb ? [bb] : []);
    log(`Deploy: SDK/backend base=${bb || "(empty)"}; direct plugin=${directPluginOrigin || "(none)"}; trying ${urls.length} URL(s)`, { scroll: true });
    for (let i = 0; i < urls.length; i += 1) log(`Deploy: URL[${i}] ${urls[i]}`, {});

    /* â”€â”€ Progress bar helpers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ */
    const progressWrap   = document.getElementById("deploy-progress-wrap");
    const progressBar    = document.getElementById("deploy-progress-bar");
    const progressPct    = document.getElementById("deploy-progress-pct");
    const progressDetail = document.getElementById("deploy-progress-detail");
    const phaseUpload    = document.getElementById("deploy-phase-upload");
    const phaseOvftool   = document.getElementById("deploy-phase-ovftool");

    function fmtBytes(b) {
      if (b >= 1073741824) return `${(b / 1073741824).toFixed(1)} GiB`;
      if (b >= 1048576)    return `${(b / 1048576).toFixed(1)} MiB`;
      return `${(b / 1024).toFixed(0)} KiB`;
    }

    /** phase: "upload" | "deploy" | "done" | "error" */
    function setProgress(phase, pct, detail) {
      if (progressWrap instanceof HTMLElement)    progressWrap.hidden = false;
      if (progressBar  instanceof HTMLProgressElement) {
        progressBar.max = 100;
        if (pct != null) {
          progressBar.value = pct;
        } else {
          progressBar.removeAttribute("value"); // indeterminate â€” shows pulsing animation
        }
      }
      if (progressPct  instanceof HTMLElement)    progressPct.textContent  = pct != null ? `${Math.round(pct)}%` : "";
      if (progressDetail instanceof HTMLElement && detail != null) progressDetail.textContent = detail;
      if (phaseUpload  instanceof HTMLElement) {
        phaseUpload.classList.toggle("deploy-phase--active", phase === "upload");
        phaseUpload.classList.toggle("deploy-phase--done",   phase !== "upload");
      }
      if (phaseOvftool instanceof HTMLElement) {
        phaseOvftool.classList.toggle("deploy-phase--active", phase === "deploy");
        phaseOvftool.classList.toggle("deploy-phase--done",   phase === "done" || phase === "error");
      }
    }

    setProgress("upload", 0, "Preparingâ€¦");

    /* â”€â”€ XHR deploy attempt: upload + SSE streaming response â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ */
    /**
     * Try one URL. Returns a Promise that resolves with the 'done' SSE event
     * data object on success, or rejects.
     * err.uploadStarted=true means bytes were already sent â€” skip remaining URLs.
     */
    function attemptDeploy(url) {
      return new Promise((resolve, reject) => {
        const xhr = new XMLHttpRequest();
        xhr.open("POST", url);

        let uploadStarted = false;
        let processedLen  = 0;
        let lineBuffer    = "";
        let sseStarted    = false;
        let doneEvent     = null;
        let lastUploadPct = 0;

        function parseSSEChunk() {
          const fullText  = xhr.responseText;
          const newText   = fullText.slice(processedLen);
          processedLen    = fullText.length;
          if (!newText) return;
          lineBuffer += newText;
          const blocks = lineBuffer.split("\n\n");
          lineBuffer   = blocks.pop() ?? "";
          for (const block of blocks) {
            for (const line of block.split("\n")) {
              if (!line.startsWith("data: ")) continue;
              let ev;
              try { ev = JSON.parse(line.slice(6)); } catch { continue; }
              sseStarted = true;
              if (ev.type === "line") {
                const pfx = ev.stream === "stderr" ? "[ovftool stderr] " : "[ovftool] ";
                log(`${pfx}${ev.text}`, {});
                /* Mirror latest ovftool line into the progress detail so the user can
                   see the current step without reading the full activity log. */
                if (progressDetail instanceof HTMLElement) {
                  progressDetail.textContent = ev.text.slice(0, 120);
                }
              } else if (ev.type === "status") {
                deployModalInfo(ev.message || "Deployingâ€¦");
                setProgress("deploy", null, ev.message || "ovftool runningâ€¦");
              } else if (ev.type === "done") {
                doneEvent = ev;
              }
            }
          }
        }

        xhr.upload.onprogress = (e) => {
          uploadStarted = true;
          if (e.lengthComputable && e.total > 0) {
            lastUploadPct = (e.loaded / e.total) * 100;
            const detail = `${fmtBytes(e.loaded)} of ${fmtBytes(e.total)}`;
            setProgress("upload", lastUploadPct, detail);
            if (Math.round(lastUploadPct) % 5 === 0) {
              log(`Deploy: upload ${Math.round(lastUploadPct)}% â€” ${detail}`, {});
            }
          }
        };

        xhr.upload.onloadend = () => {
          /* Upload phase finished (success or not); switch phase indicator */
          if (lastUploadPct >= 99) setProgress("deploy", null, "Upload complete â€” ovftool deploying to vCenterâ€¦");
          log("Deploy: upload phase complete, waiting for ovftoolâ€¦", { scroll: true });
        };

        xhr.onprogress = () => parseSSEChunk();

        xhr.onload = () => {
          parseSSEChunk(); // flush any final bytes
          if (doneEvent) {
            resolve(doneEvent);
          } else if (!sseStarted) {
            /* Plain JSON response before SSE started (e.g. 400 validation / 501 no ovftool) */
            let data;
            try { data = JSON.parse(xhr.responseText); } catch { data = { raw: xhr.responseText }; }
            if (xhr.status >= 200 && xhr.status < 300) {
              resolve({ ok: true, ...data });
            } else {
              const e = Object.assign(
                new Error(data?.error || `HTTP ${xhr.status}`),
                { status: xhr.status, data, uploadStarted },
              );
              reject(e);
            }
          } else {
            reject(Object.assign(new Error("SSE stream ended without a done event"), { uploadStarted }));
          }
        };

        xhr.onerror   = () => reject(Object.assign(new Error("Network error"),      { uploadStarted }));
        xhr.ontimeout = () => reject(Object.assign(new Error("Request timed out"), { uploadStarted }));

        xhr.send(buildDeployFormData());
      });
    }

    /* â”€â”€ Try each URL â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ */
    let doneData   = null;
    let deployErr  = null;

    for (let idx = 0; idx < urls.length; idx += 1) {
      const url = urls[idx];
      log(`Deploy: POST ${url}`, { scroll: true });
      try {
        doneData = await attemptDeploy(url);
        deployErr = null;
        break; // success
      } catch (err) {
        deployErr = err;
        const canRetry = !err.uploadStarted && idx < urls.length - 1;
        if (!canRetry) break;
        const looksLikeWrongServer =
          err.status === 400 || err.status === 404 || err.status === 405 ||
          err.status === 408 || err.status === 502 || err.status === 503 || err.status === 504;
        if (looksLikeWrongServer) {
          log(`Deploy: ${url} â†’ ${err.message} â€” retrying next URLâ€¦`, { error: true });
          continue;
        }
        break;
      }
    }

    /* â”€â”€ Handle final result â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ */
    if (deployErr && !doneData) throw deployErr;

    const done = doneData ?? {};
    if (!done.ok) {
      const errMsg  = done.error  || (deployErr ? (deployErr.message || String(deployErr)) : "Deploy failed");
      const hint    = done.hint   || "";
      if (hint) log(`Deploy: hint: ${hint}`, { error: true });
      if (done.ovftoolStderr) log(`Deploy: ovftool stderr:\n${done.ovftoolStderr}`, { error: true, scroll: true });
      if (done.ovftoolStdout) log(`Deploy: ovftool stdout:\n${done.ovftoolStdout}`, { error: true, scroll: true });
      setProgress("error", 100, "Deploy failed");
      deployModalError(errMsg);
      return;
    }

    setProgress("done", 100, "");
    log(`Deploy: success â€” ${done.message || "complete"}`, { scroll: true });
    deployModalInfo(done.message || "Deploy finished â€” confirm the new VM in vCenter inventory.");

  } catch (err) {
    const msg = err instanceof Error ? err.message : String(err);
    log(`Deploy: exception ${msg}`, { error: true });
    if (err instanceof Error && err.stack) log(`Deploy: stack\n${err.stack}`, { error: true });
    const data = err?.data ?? {};
    if (data.hint)          log(`Deploy: hint: ${data.hint}`, { error: true });
    if (data.ovftoolStderr) log(`Deploy: ovftool stderr:\n${data.ovftoolStderr}`, { error: true, scroll: true });
    if (/Network error|Failed to fetch|NetworkError|load failed|ECONNREFUSED|aborted/i.test(msg)) {
      deployModalError(
        "Could not reach the plug-in server from your browser. Prefer the vCenter-proxied URL (same origin as this UI) â€” the client tries it first. If you still see this: open the plug-in server URL in a new tab, accept the certificate warning, then retry. Also confirm CORS is enabled and firewalls allow the path.",
      );
    } else {
      deployModalError(msg || "Deploy failed unexpectedly.");
    }
  } finally {
    if (deployBtn instanceof HTMLButtonElement) deployBtn.disabled = false;
  }
});

