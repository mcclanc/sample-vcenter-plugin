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

const ovaModalOpenIds = [
  "assess-card-open",
  "data-intel-card-open",
  "secure-enterprise-card-open",
];

function openOvaModal() {
  if (!(modal instanceof HTMLDialogElement)) return;
  modal.showModal();
  const first = modal.querySelector('input[name="ovaName"]');
  if (first instanceof HTMLInputElement) first.focus();
}

function closeOvaModal() {
  if (!(modal instanceof HTMLDialogElement)) return;
  modal.close();
}

for (const id of ovaModalOpenIds) {
  document.getElementById(id)?.addEventListener("click", openOvaModal);
}
closeBtn?.addEventListener("click", closeOvaModal);
cancelBtn?.addEventListener("click", closeOvaModal);

modal?.addEventListener("click", (e) => {
  if (e.target === modal) closeOvaModal();
});

document.getElementById("stub-action")?.addEventListener("click", () => {
  const form = document.getElementById("ova-install-form");
  if (!(form instanceof HTMLFormElement)) return;
  const data = new FormData(form);
  const ovaName = data.get("ovaName");
  const ovaUrl = data.get("ovaUrl");
  const vmName = data.get("vmName");

  if (!ovaName || String(ovaName).trim() === "") {
    log("OVA name is required.");
    return;
  }
  if (!ovaUrl || String(ovaUrl).trim() === "") {
    log("OVA URL or Content Library path is required for install.");
    return;
  }
  log(
    `Stub OK: install OVA "${ovaName}" from ${ovaUrl} as VM "${vmName || "unnamed"}"`,
  );
  log("Next: call vSphere APIs via htmlClientSdk.app (in client) or your /rest backend.");
});
