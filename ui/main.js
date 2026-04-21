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

document.getElementById("stub-action")?.addEventListener("click", () => {
  const form = document.getElementById("deploy-form");
  if (!(form instanceof HTMLFormElement)) return;
  const data = new FormData(form);
  const ova = data.get("ova");
  const vmName = data.get("vmName");
  if (!ova || String(ova).trim() === "") {
    log("OVA URL is required for a real deploy flow.");
    return;
  }
  log(`Stub OK: would deploy "${vmName || "unnamed"}" from ${ova}`);
  log("Next: call vSphere APIs via htmlClientSdk.app (in client) or your /rest backend.");
});
