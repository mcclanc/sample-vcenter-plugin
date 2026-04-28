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

### Plug-in icon (menu / title bar)

vSphere Client shows **`configuration.icon`** from **`definitions.iconSpriteSheet`**: it loads **`uri`** (e.g. `images/sprites.png` relative to the plug-in base URL) and crops a **32×32 px** tile at **`x` / `y`** (pixels from the top-left of the PNG). The starter sheet is **32×192** (six rows). This repo’s default **`main`** tile was at **`y: 96`**, which is the **SDK’s puzzle-piece** artwork—not a “missing icon” bug.

To use a **Tanzu** mark: obtain an approved asset from **Broadcom / Tanzu brand guidelines**, scale it to **32×32**, and either (a) paste it into the correct row of `sprites.png` with an image editor, or (b) replace the sheet and set **`y`** to `row_index * 32`. Keep **`configuration.icon.name`** (`main`) aligned with the matching key under **`definitions.iconSpriteSheet.definitions`**.

For a quick **solid-color placeholder** strip (e.g. while designing), from the repo root run: `python3 scripts/gen_plugin_icon_sprite.py` (then set **`main`** `y` to match `--main-row`, default `0`).

To **drop in your own logo** (e.g. Tanzu mark) as the menu icon: save a PNG, then from repo root `pip install -r scripts/requirements-icons.txt` and run `python3 scripts/compose_icon_into_sprite.py path/to/logo.png` (defaults to pasting at **`(0, 96)`** to match **`configuration.icon` → `main`** in `plugin.json`). Use **`--y 0`** if your `main` coordinates use the first row. Dark UI may need **`themeOverrides.dark`** (see `html-client-sdk/samples/.../plugin-70.json` and requirement **`icon.sprite.sheet.theming`**) if the icon must differ on dark theme.

## Local dev: `htmlClientSdk.js`

Inside vSphere Client, the host provides the real **`htmlClientSdk`**. On this dev server, **`GET /api/ui/htmlClientSdk.js`** returns **`server/htmlClientSdk.stub.js`** so `index.html` loads without 404. Replace nothing when the same `index.html` runs in vCenter—the client injects the production script for that URL on the real infrastructure.

## Optional: build the VMware Angular sample

To compile the stock Angular UI (not required for the slim `ui/` in this repo):

1. Install **JDK** and **Maven** per Broadcom’s sample instructions.
2. From `html-client-sdk/samples/remote-plugin-sample/`, run the Maven build that produces `target/classes/ui/`.
3. Run the Spring Boot app and point vCenter registration at that server’s `plugin.json` URL.

For the **Tanzu Hub POC** workstream, it is usually faster to extend the lightweight **`ui/`** app here and add a small backend later than to fork the entire Angular sample wholesale.
