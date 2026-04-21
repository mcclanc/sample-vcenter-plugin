# sample-vcenter-plugin

Remote **vSphere Client** plug-in scaffold for a **Tanzu Hub / Tanzu Platform evaluation POC** style workflow: extend the HTML5 vSphere Client with a global view, then connect your own deploy logic (SDK in the browser, or a trusted backend for OVF).

## References

- **vSphere 9 — remote plug-ins:** [Developing Remote Plug-ins with the vSphere Client SDK](https://techdocs.broadcom.com/us/en/vmware-cis/vsphere/vsphere-sdks-tools/9-0/developing-remote-plug-ins-with-the-vsphere-client-sdk-8-0.html)
- **Manifest shape:** [Sample manifest file for a remote plug-in](https://techdocs.broadcom.com/us/en/vmware-cis/vsphere/vsphere-sdks-tools/8-0/developing-remote-plug-ins-with-the-vsphere-client-sdk-8-0/creating-a-remote-plug-in-for-the-vsphere-client/sample-manifest-file-for-a-remote-plug-in.html)
- **Tanzu evaluation / POC appliances on vSphere:** [Deploying Tanzu Platform evaluation appliances on vSphere](https://techdocs.broadcom.com/us/en/vmware-tanzu/platform/tanzu-platform-evaluation-appliances/10-3/tp-evaluation-appliances/deploy.html)

## Layout

```
ui/plugin.json          # Remote plug-in manifest
ui/index.html           # Global view entry (extend with vSphere Client SDK JS)
server/index.mjs        # Static host + /health
docs/ARCHITECTURE.md     # How this maps to vCenter + Tanzu POC
docs/REGISTRATION.md     # HTTPS + manifest URL for registration
```

## Machine setup

See **[docs/DEV_SETUP.md](docs/DEV_SETUP.md)** (Homebrew **Node** + **mkcert**, `PATH`, HTTPS dev server, SDK download notes).

## Quick start

```bash
cd sample-vcenter-plugin
npm install
npm run certs          # first time: localhost TLS files under certs/
npm run start:https    # HTTPS on port 8443
```

- Health: `https://localhost:8443/health`
- Manifest: `https://localhost:8443/tanzu-hub-poc-ui/plugin.json`  
  Plain HTTP (`npm start`) is only for quick checks; vCenter registration usually needs **HTTPS** — [docs/REGISTRATION.md](docs/REGISTRATION.md).

## Next implementation steps

1. Add the **vSphere Client Remote Plug-in** JavaScript SDK (from the Broadcom SDK bundle) to `ui/` and replace the stub UI with real inventory pickers and task monitoring.
2. Implement deploy either via **SDK-supported APIs** or a **backend** that performs OVF deploy to vCenter using your approved automation (REST, govc, pyVmomi).
3. Register the plug-in with vCenter using the SDK registration flow; iterate on **privileges** and **extension points** (global vs object workspace) per the same documentation set.
