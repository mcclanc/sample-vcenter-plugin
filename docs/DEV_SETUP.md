# Development machine setup (macOS)

This project’s plug-in host is **Node.js** + static `ui/`. vCenter registration expects **HTTPS** for the manifest URL in most setups.

## Installed via Homebrew (this repo)

| Tool | Purpose |
|------|--------|
| **Node.js** (`node`, `npm`) | Run `server/index.mjs`, install UI/build tooling later. |
| **mkcert** | Issue **localhost** TLS certificates for `npm run start:https`. |

Ensure Homebrew’s binaries are on your `PATH` (Apple Silicon default):

```bash
echo 'eval "$(/opt/homebrew/bin/brew shellenv)"' >> ~/.zprofile
eval "$(/opt/homebrew/bin/brew shellenv)"
```

Verify:

```bash
node -v
npm -v
mkcert -version
```

## One-time: trust mkcert’s local CA

So **Safari / Chrome** trust dev certificates without warnings, run **once** in Terminal (sudo password):

```bash
mkcert -install
```

If you skip this, `curl -k` and some tools still work; vSphere Client may be stricter until the CA or cert is trusted.

## Project setup

```bash
cd sample-vcenter-plugin
npm install
npm run certs    # creates certs/ (gitignored); skip if files already exist
npm run start:https
```

- Health: `https://localhost:8443/health`
- Manifest: `https://localhost:8443/tanzu-hub-poc-ui/plugin.json`

`npm run start:https` sets `SSL_KEY_PATH` and `SSL_CERT_PATH` to the files under `certs/`.

## Broadcom vSphere Client SDK

Unpack the distribution into **`html-client-sdk/`** at the repo root (already expected by this project). See **[SDK_LAYOUT.md](SDK_LAYOUT.md)** for how that tree maps to **`ui/`** and the dev **`htmlClientSdk`** stub.

- Doc hub: [Developing Remote Plug-ins with the vSphere Client SDK (vSphere 9)](https://techdocs.broadcom.com/us/en/vmware-cis/vsphere/vsphere-sdks-tools/9-0/developing-remote-plug-ins-with-the-vsphere-client-sdk-8-0.html)

The real **`htmlClientSdk`** is provided by vSphere Client in production; locally, **`server/htmlClientSdk.stub.js`** is served at **`/api/ui/htmlClientSdk.js`** so `index.html` matches the official load order.

## Optional tooling

| Tool | When you need it |
|------|-------------------|
| **OpenJDK + Maven** | If you use the SDK’s **Java** samples or registration utilities that ship as Maven projects. `brew install openjdk maven` |
| **Docker Desktop** | Only if you switch to Broadcom’s **Docker**-hosted [GitHub MCP server](https://github.com/github/github-mcp-server) or other containerized tooling—not required for this Node scaffold. |
| **govc** / **Python + pyVmomi** | If you add a **backend** that deploys OVAs to vCenter outside the browser. |

## Cursor

Use a current **Cursor** build with Streamable HTTP MCP if you use GitHub MCP; for the plug-in itself, any editor is fine.
