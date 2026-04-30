const log = (msg) => {
  const el = document.getElementById("log");
  if (!el) return;
  el.textContent = `${new Date().toISOString()} ${msg}\n${el.textContent}`;
};

function whenSdkReady() {
  const sdk = globalThis.htmlClientSdk;
  if (!sdk) {
    log("htmlClientSdk missing — expected script /api/ui/htmlClientSdk.js");
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
const continueBtn = document.getElementById("ova-file-continue");
const deployBtn = document.getElementById("ova-deploy-btn");
const deployStep = document.getElementById("ova-modal-step-deploy");
const statusEl = document.getElementById("ova-modal-status");

const ovaModalOpenIds = [
  "assess-card-open",
  "data-intel-card-open",
  "secure-enterprise-card-open",
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

/** Resolve POST URL for the dev Express handler (must match server/index.mjs routes). */
function candidateDeployUrls() {
  const urls = [];
  const sdk = globalThis.htmlClientSdk;
  const plug =
    sdk && typeof sdk.getProxiedPluginServerOrigin === "function"
      ? sdk.getProxiedPluginServerOrigin()
      : "";
  const o = typeof plug === "string" ? plug.trim().replace(/\/+$/, "") : "";
  /* Prefer the same base URL as this page first (avoids a wrong getProxiedPluginServerOrigin). */
  try {
    urls.push(new URL("api/eval-appliance/deploy", document.baseURI).href);
  } catch {
    /* ignore */
  }
  if (o) {
    const withUi = o.endsWith("/tanzu-hub-poc-ui") ? o : `${o}/tanzu-hub-poc-ui`;
    urls.push(`${withUi}/api/eval-appliance/deploy`);
  }
  if (typeof window !== "undefined" && window.location?.origin) {
    urls.push(`${window.location.origin}/tanzu-hub-poc-ui/api/eval-appliance/deploy`);
    urls.push(`${window.location.origin}/api/eval-appliance/deploy`);
  }
  const unique = [...new Set(urls)];
  if (unique.length === 0) {
    unique.push("/tanzu-hub-poc-ui/api/eval-appliance/deploy");
  }
  return unique;
}

function setStatus(message, isError = false) {
  if (!(statusEl instanceof HTMLElement)) return;
  statusEl.textContent = message || "";
  statusEl.classList.toggle("ova-modal__status--error", Boolean(isError && message));
}

function resetOvaModal() {
  setStatus("");
  const form = document.getElementById("ova-install-form");
  if (form instanceof HTMLFormElement) form.reset();
  if (deployStep instanceof HTMLElement) deployStep.hidden = true;
  if (deployBtn instanceof HTMLButtonElement) deployBtn.disabled = true;
}

function openOvaModal() {
  if (!(modal instanceof HTMLDialogElement)) return;
  resetOvaModal();
  const logEl = document.getElementById("log");
  if (logEl) logEl.textContent = "";
  modal.showModal();
  document.getElementById("ova-file-input")?.focus();
}

function closeOvaModal() {
  if (!(modal instanceof HTMLDialogElement)) return;
  modal.close();
  resetOvaModal();
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

continueBtn?.addEventListener("click", () => {
  const fileInput = document.getElementById("ova-file-input");
  const files = fileInput instanceof HTMLInputElement ? fileInput.files : null;
  if (!files || files.length === 0) {
    setStatus("Choose an .ova or .ovf file from your computer to continue.", true);
    fileInput?.focus();
    return;
  }
  const name = files[0].name.toLowerCase();
  if (!name.endsWith(".ova") && !name.endsWith(".ovf")) {
    setStatus("File must be an .ova or .ovf package.", true);
    fileInput?.focus();
    return;
  }
  setStatus("Package selected. Enter vCenter details, then click Deploy.");
  if (deployStep instanceof HTMLElement) deployStep.hidden = false;
  if (deployBtn instanceof HTMLButtonElement) deployBtn.disabled = false;
  document.getElementById("vc-host-input")?.focus();
});

deployBtn?.addEventListener("click", async () => {
  const fileInput = document.getElementById("ova-file-input");
  const files = fileInput instanceof HTMLInputElement ? fileInput.files : null;
  if (!files || files.length === 0) {
    setStatus("Choose an .ova or .ovf file first (step 1).", true);
    fileInput?.focus();
    return;
  }
  const file = files[0];
  const name = file.name.toLowerCase();
  if (!name.endsWith(".ova") && !name.endsWith(".ovf")) {
    setStatus("File must be an .ova or .ovf package.", true);
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
  const datacenter = (document.getElementById("datacenter-input") instanceof HTMLInputElement
    ? document.getElementById("datacenter-input").value
    : ""
  ).trim();
  const computePath = (document.getElementById("compute-path-input") instanceof HTMLInputElement
    ? document.getElementById("compute-path-input").value
    : ""
  ).trim();
  const datastore = (document.getElementById("datastore-input") instanceof HTMLInputElement
    ? document.getElementById("datastore-input").value
    : ""
  ).trim();
  const network = (document.getElementById("network-input") instanceof HTMLInputElement
    ? document.getElementById("network-input").value
    : ""
  ).trim();
  const ovfNetworkLabel = (
    document.getElementById("ovf-network-label-input") instanceof HTMLInputElement
      ? document.getElementById("ovf-network-label-input").value
      : ""
  ).trim();

  const ovfEl = document.getElementById("ovf-properties-json");
  const ovfRaw = ovfEl instanceof HTMLTextAreaElement ? ovfEl.value.trim() : "";
  let ovfPropertiesJson = "";
  if (ovfRaw) {
    try {
      const parsed = JSON.parse(ovfRaw);
      if (typeof parsed !== "object" || parsed === null || Array.isArray(parsed)) {
        setStatus("OVF properties must be a JSON object (not an array).", true);
        return;
      }
      ovfPropertiesJson = JSON.stringify(parsed);
    } catch (err) {
      setStatus(`Invalid OVF properties JSON: ${err instanceof Error ? err.message : String(err)}`, true);
      return;
    }
  }

  if (!vcHost || !vcUsername || !vcPassword) {
    setStatus("vCenter hostname, username, and password are required.", true);
    return;
  }
  if (!datacenter || !computePath) {
    setStatus("Datacenter name and compute path (e.g. host/ClusterName) are required.", true);
    return;
  }
  if (!vmName || !datastore || !network) {
    setStatus("VM name, datastore, and destination port group are required.", true);
    return;
  }

  const fd = new FormData();
  fd.append("ovaFile", file, file.name);
  fd.append("vcHost", vcHost);
  fd.append("vcUsername", vcUsername);
  fd.append("vcPassword", vcPassword);
  fd.append("vmName", vmName);
  fd.append("datacenter", datacenter);
  fd.append("computePath", computePath);
  fd.append("datastore", datastore);
  fd.append("network", network);
  if (ovfNetworkLabel) fd.append("ovfNetworkLabel", ovfNetworkLabel);
  if (ovfPropertiesJson) fd.append("ovfProperties", ovfPropertiesJson);

  const urls = candidateDeployUrls();
  if (deployBtn instanceof HTMLButtonElement) deployBtn.disabled = true;
  setStatus("Deploying: uploading package, then running ovftool…");
  try {
    let res = null;
    let text = "";
    let usedUrl = "";
    for (const url of urls) {
      usedUrl = url;
      res = await fetch(url, {
        method: "POST",
        body: fd,
      });
      text = await res.text();
      const looksLikeExpress404 =
        !res.ok && res.status === 404 && /Cannot POST/i.test(text) && /<!DOCTYPE/i.test(text);
      if (looksLikeExpress404 && urls.indexOf(url) < urls.length - 1) {
        log(`POST ${url} → ${res.status} (no route) — retrying alternate URL…`);
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
    if (!res.ok) {
      const errMsg = typeof data.error === "string" ? data.error : text;
      log(`HTTP ${res.status} (${usedUrl}): ${errMsg.slice(0, 500)}`);
      if (/Cannot POST/i.test(text) && /<!DOCTYPE/i.test(text)) {
        setStatus(
          "The plug-in UI server does not expose POST /…/api/eval-appliance/deploy (404). Restart `npm start` after `npm install` so multipart deploy is available.",
          true,
        );
      } else {
        setStatus(typeof data.error === "string" ? data.error : "Request failed.", true);
      }
      return;
    }
    log(`POST ${usedUrl} → OK`);
    log(JSON.stringify(data, null, 2));
    setStatus(
      typeof data.message === "string"
        ? data.message
        : "Deploy request finished — see activity log for server response.",
    );
  } catch (err) {
    const msg = err instanceof Error ? err.message : String(err);
    log(`Fetch error: ${msg}`);
    setStatus(
      "Could not reach the plug-in server. For local dev, run `npm start` on the same host that serves /tanzu-hub-poc-ui/.",
      true,
    );
  } finally {
    if (deployBtn instanceof HTMLButtonElement) deployBtn.disabled = false;
  }
});
