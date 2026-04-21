const log = (msg) => {
  const el = document.getElementById("log");
  if (!el) return;
  el.textContent = `${new Date().toISOString()} ${msg}\n${el.textContent}`;
};

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
  log("Next: integrate vSphere Client Remote Plug-in SDK (HtmlApiClient) + deploy library or backend.");
});
