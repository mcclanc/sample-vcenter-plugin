# sample-vcenter-plugin

Remote **vSphere Client** plug-in scaffold for a **Tanzu Hub / Tanzu Platform evaluation POC** style workflow: extend the HTML5 vSphere Client with a global view, then connect your own deploy logic (SDK in the browser, or a trusted backend for OVF).

## References

- **vSphere 9 — remote plug-ins:** [Developing Remote Plug-ins with the vSphere Client SDK](https://techdocs.broadcom.com/us/en/vmware-cis/vsphere/vsphere-sdks-tools/9-0/developing-remote-plug-ins-with-the-vsphere-client-sdk-8-0.html)
- **Manifest shape:** [Sample manifest file for a remote plug-in](https://techdocs.broadcom.com/us/en/vmware-cis/vsphere/vsphere-sdks-tools/8-0/developing-remote-plug-ins-with-the-vsphere-client-sdk-8-0/creating-a-remote-plug-in-for-the-vsphere-client/sample-manifest-file-for-a-remote-plug-in.html)
- **Tanzu evaluation / POC appliances on vSphere:** [Deploying Tanzu Platform evaluation appliances on vSphere](https://techdocs.broadcom.com/us/en/vmware-tanzu/platform/tanzu-platform-evaluation-appliances/10-3/tp-evaluation-appliances/deploy.html)

## Three vCenter plugin entries

This server registers **three separate plugins** in vCenter — each appears as its own entry in the vSphere Client Plugins sidebar:

| # | Plugin name | vCenter key | Manifest URL path | Description |
|---|---|---|---|---|
| 1 | **Secure Images** | `com.cmaclabs.remote.secureimages` | `/secure-images-ui/plugin.json` | VMware Secure Images landing with "Get started" |
| 2 | **Data Intelligence** | `com.cmaclabs.remote.dataintelligence` | `/data-intel-ui/plugin.json` | Tanzu Data Intelligence landing with "Get started" |
| 3 | **App Platform as a Service** | `com.cmaclabs.remote.paasforvcf` | `/tanzu-hub-poc-ui/plugin.json` | Full use-case grid + OVA deploy flow |

All three are served by the **same Node.js server process**. Each has its own `plugin.json` manifest pointing to its own `index.html` entry view.

## Layout

```
html-client-sdk/              # Unpacked Broadcom vSphere HTML Client SDK
ui/                           # App Platform as a Service plugin
  plugin.json                 #   manifest (navigationId: tanzuHubPoc)
  index.html                  #   landing — use-case cards + connect flow
  usecase.html                #   use-cases view + OVA deploy modal
  images/sprites.png          #   icon sprite
ui-secure-images/             # Secure Images plugin
  plugin.json                 #   manifest (navigationId: vmwareSecureImages)
  index.html                  #   product landing page
  images/sprites.png          #   icon sprite
ui-data-intel/                # Data Intelligence plugin
  plugin.json                 #   manifest (navigationId: tanzuDataIntelligence)
  index.html                  #   product landing page
  images/sprites.png          #   icon sprite
server/index.mjs              # Express server — serves all three plugin paths
scripts/register-all-plugins.ps1   # Register all three plugins at once (Windows)
scripts/register-extension-lab.ps1 # Register a single plugin (existing, Windows)
scripts/register-extension-lab.sh  # Register a single plugin (existing, macOS/Linux)
docs/REGISTRATION.md
```

## Machine setup

See **[docs/DEV_SETUP.md](docs/DEV_SETUP.md)** (Node **20+**, **mkcert**, HTTPS dev server; **Windows** and **macOS**).

## Quick start

```bash
cd sample-vcenter-plugin
npm install
npm run certs          # first time: localhost TLS files under certs/
npm run start:https    # HTTPS on port 8443
```

- Health (lists all three plugins): `https://localhost:8443/health`
- App Platform manifest: `https://localhost:8443/tanzu-hub-poc-ui/plugin.json`
- Secure Images manifest: `https://localhost:8443/secure-images-ui/plugin.json`
- Data Intelligence manifest: `https://localhost:8443/data-intel-ui/plugin.json`

Plain HTTP (`npm start`) is only for quick checks; vCenter registration requires **HTTPS** — [docs/REGISTRATION.md](docs/REGISTRATION.md).

## Register all three plugins

Run the combined registration script to register all three entries in one pass:

```powershell
# Windows — interactive (prompts for credentials once, then registers all three)
.\scripts\register-all-plugins.ps1

# Non-interactive
$env:REGISTER_NON_INTERACTIVE = "1"
$env:VC_SDK_URL               = "https://your-vcenter/sdk"
$env:VC_USER                  = "administrator@vsphere.local"
$env:VC_PASSWORD              = "your-password"
$env:PLUGIN_SERVER_TP         = "AA:BB:CC:..."   # cert SHA-1 thumbprint
$env:PLUGIN_SERVER_HOST       = "192.168.68.5:8443"
.\scripts\register-all-plugins.ps1

# To update already-registered plugins
$env:REGISTER_ACTION = "updatePlugin"
.\scripts\register-all-plugins.ps1
```

To register a single plugin (or choose a custom key), use the existing script:

```powershell
.\scripts\register-extension-lab.ps1
```

## Credentials (safe for GitHub)

- **Do not commit** real vCenter or SSO passwords, API tokens, or `.env` files. This repo’s `.gitignore` excludes `.env*`, `*.pem`, and `certs/`.
- **Registration:** use environment variables (for example `VC_PASSWORD` for [scripts/register-extension-lab.sh](scripts/register-extension-lab.sh)), not literals in tracked files.
- **HTML Client SDK Java samples** under `html-client-sdk/samples/` use `server.ssl.key-store-password=${KEYSTORE_PASSWORD}`. Export `KEYSTORE_PASSWORD` before running Spring Boot. For the **bundled sample `keystore.jks`**, use the passphrase from Broadcom’s upstream remote plug-in sample `application.properties` in the official SDK bundle (or replace the keystore with your own and set this variable accordingly).

## Next implementation steps

1. Add the **vSphere Client Remote Plug-in** JavaScript SDK (from the Broadcom SDK bundle) to `ui/` and replace the stub UI with real inventory pickers and task monitoring.
2. Implement deploy either via **SDK-supported APIs** or a **backend** that performs OVF deploy to vCenter using your approved automation (REST, govc, pyVmomi).
3. Register the plug-in with vCenter using the SDK registration flow; iterate on **privileges** and **extension points** (global vs object workspace) per the same documentation set.
