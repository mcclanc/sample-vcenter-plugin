# Where the HTML Client SDK lives in this repo

You unpacked Broadcom’s distribution into **`html-client-sdk/`**. That tree stays **read-only reference**: samples, JavaScript API HTML docs, registration tools, and the full **Angular + Spring Boot** remote plug-in sample.

## What to use where

| Path | Use |
|------|-----|
| **`html-client-sdk/docs/`** | API references (e.g. `Javascript-API-Remote.html` for `htmlClientSdk` objects). |
| **`html-client-sdk/samples/remote-plugin-sample-starter/`** | Minimal static plug-in (Java server + `plugin.json` + `index.html`). Good mental model for manifests and icons. |
| **`html-client-sdk/samples/remote-plugin-sample/`** | Full product-style sample: Angular UI under `src/main/ui/`, built into `target/classes/ui/`, plus Spring `rest/` APIs and vCenter session gateway. Clone patterns from here when you add real backend calls. |
| **`html-client-sdk/tools/vCenter plugin registration/`** | Scripts / JAR flow to register an extension with vCenter (thumbprint, URL to `plugin.json`, etc.). |

## Your deployable UI (this project)

Files **served to vCenter** for this plug-in live under **`ui/`** at URL prefix **`/tanzu-hub-poc-ui/`** (see `server/index.mjs`).

- **`ui/plugin.json`** — manifest (aligned with starter + your Tanzu strings).
- **`ui/index.html`** — same **load order** as the official samples: `/api/ui/htmlClientSdk.js`, then `base` from `htmlClientSdk.getProxiedPluginServerOrigin()`, then assets under the base path.
- **`ui/images/sprites.png`** — copied from the starter sample for the manifest `iconSpriteSheet`.

## Local dev: `htmlClientSdk.js`

Inside vSphere Client, the host provides the real **`htmlClientSdk`**. On this dev server, **`GET /api/ui/htmlClientSdk.js`** returns **`server/htmlClientSdk.stub.js`** so `index.html` loads without 404. Replace nothing when the same `index.html` runs in vCenter—the client injects the production script for that URL on the real infrastructure.

## Optional: build the VMware Angular sample

To compile the stock Angular UI (not required for the slim `ui/` in this repo):

1. Install **JDK** and **Maven** per Broadcom’s sample instructions.
2. From `html-client-sdk/samples/remote-plugin-sample/`, run the Maven build that produces `target/classes/ui/`.
3. Run the Spring Boot app and point vCenter registration at that server’s `plugin.json` URL.

For the **Tanzu Hub POC** workstream, it is usually faster to extend the lightweight **`ui/`** app here and add a small backend later than to fork the entire Angular sample wholesale.
