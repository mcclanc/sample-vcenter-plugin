---
project_name: 'sample-vcenter-plugin'
user_name: 'Chris'
date: '2026-04-30'
sections_completed:
  - technology_stack
  - language_rules
  - framework_rules
  - testing_rules
  - quality_rules
  - workflow_rules
  - anti_patterns
status: complete
rule_count: 32
optimized_for_llm: true
---

# Project Context for AI Agents

_This file contains critical rules and patterns that AI agents must follow when implementing code in this project. Focus on unobvious details that agents might otherwise miss._

---

## Technology Stack & Versions

| Area | Detail |
|------|--------|
| Runtime | **Node.js ≥ 20** (`engines` in root `package.json`). |
| Module system | **ESM** — root `"type": "module"`; server entry is **`.mjs`**; use `import` / `export`, not `require`. |
| HTTP | **express** `^4.21.0`, **multer** `^2.1.1`. |
| Deliverable UI | Static files under **`ui/`**, served with URL prefix **`/tanzu-hub-poc-ui/`** (see `server/index.mjs`). |
| vSphere Client SDK | **`html-client-sdk/`** is Broadcom’s **read-only reference** (docs, samples, registration JAR). Do not treat it as the app’s source of truth for this repo’s shipped UI. |
| Optional (SDK tree only) | **Java 17**, **Maven**, **Spring Boot 2.7.x**, **Angular** in `html-client-sdk/samples/remote-plugin-sample/` — use only if explicitly building that sample, not for the slim `ui/` plug-in. |
| TLS (dev) | **`npm run certs`** then **`npm run start:https`**; keys default `certs/dev-key.pem`, `certs/dev-cert.pem`, or **`SSL_KEY_PATH`** / **`SSL_CERT_PATH`**. |
| Python | **Python 3** for `scripts/gen_plugin_icon_sprite.py`, `scripts/compose_icon_into_sprite.py` (icon pipeline); deps in `scripts/requirements-icons.txt` where applicable. |
| Registry | Root **`.npmrc`** pins **`registry=https://registry.npmjs.org/`** — preserve so corporate mirrors do not break installs. |

---

## Critical Implementation Rules

### Language-Specific Rules

- Prefer **Node built-in imports** with the `node:` prefix (`node:fs`, `node:path`, `node:url`, etc.) in new server code, matching `server/index.mjs`.
- **`__dirname` in ESM**: derive via `path.dirname(fileURLToPath(import.meta.url))` when needed.
- Use **`void` or explicit `.catch()`** on floating promises where the codebase already does (e.g. async handlers invoked without await).

### Framework-Specific Rules (Express + remote plug-in UI)

- Register **`express.json`** with a bounded payload (repo uses **`limit: "256kb"`**); keep new JSON routes within similar limits unless product needs justify a higher cap with abuse review.
- **Static UI**: `app.use("/tanzu-hub-poc-ui", express.static(ui, { index: false }))` — new static assets belong under **`ui/`** and are referenced relative to the plug-in base (after `<base>`).
- **`GET /api/ui/htmlClientSdk.js`**: always serve the **stub** from `server/htmlClientSdk.stub.js` in dev; **vSphere Client** injects the real script in production — **do not** remove or rename this route; **`index.html`** must load it **first**, before setting `<base>`.
- **`<base href>`**: set from **`htmlClientSdk.getProxiedPluginServerOrigin() + "/tanzu-hub-poc-ui/"`** so asset URLs resolve inside the client iframe (see `ui/index.html`).
- **Deploy / OVA API**: both **`POST /tanzu-hub-poc-ui/api/eval-appliance/deploy`** and **`POST /api/eval-appliance/deploy`** are wired — if you add a new API, consider whether it must exist under **`/tanzu-hub-poc-ui`** for same-origin expectations from the UI.
- **vCenter browse API**: **`POST /tanzu-hub-poc-ui/api/vcenter/inventory`** and **`POST /api/vcenter/inventory`** — JSON body `vcHost`, `vcUsername`, `vcPassword`, optional `datacenter`, optional **`allowInsecureVcTls`** (skip TLS verification plug-in→vCenter, same as **`VCENTER_REST_TLS_INSECURE=true`** on the server). **UI must POST to the remote plug-in host** (`htmlClientSdk.getProxiedPluginServerOrigin()`), not to the vCenter UI origin, or the browser will see HTTP 404. For trusted private CAs without skipping verification, use **`NODE_EXTRA_CA_CERTS`** (PEM bundle) or start Node with **`--use-system-ca`** (see **`npm run start:system-ca`**).
- **Multer / uploads**: max size from **`OVA_UPLOAD_MAX_GB`** or **`OVA_UPLOAD_MAX_BYTES`** (see `server/eval-appliance/upload.mjs`); default cap is large — document env vars when adding ops docs.
- **`plugin.json`**: keep **`manifestVersion`**, **`requirements.plugin.api.version`**, **`configuration`**, **`definitions`** (i18n, **iconSpriteSheet**), and **`global.view`** aligned with Broadcom remote plug-in docs; **`configuration.icon.name`** must match a key under **`definitions.iconSpriteSheet.definitions`** (e.g. `main`).

### Testing Rules

- The **root scaffold has no Jest/Vitest harness**; the **HTML Client SDK sample** may carry its own (e.g. ESLint/TestNG) — **do not assume** root `npm test` exists.
- When adding behavior, **add or extend tests** if you introduce a test runner; otherwise document manual verification steps in PR description for significant server or UI changes.

### Code Quality & Style Rules

- **No root ESLint/Prettier config** — follow **existing file style** in `server/` (double quotes in `.mjs`, functional layout, small focused modules under `server/**`).
- Keep **`html-client-sdk/`** unchanged except for **documented local patches** (e.g. registration JAR) called out in `docs/` — product logic belongs in **`ui/`**, **`server/`**, **`scripts/`**.
- **`docs/`** is **`project_knowledge`** — update architecture/setup docs when behavior or registration flow changes materially.

### Development Workflow Rules

- **Secrets**: never commit **`VC_PASSWORD`**, thumbprints, or real URLs; lab scripts use **env vars** (see `docs/REGISTRATION.md`).
- **Windows**: `npm run register:lab` → PowerShell `scripts/register-extension-lab.ps1`; ensure **`JAVA_HOME` / `java`** on PATH when using registration JAR.
- **Extension key**: **lowercase reverse-DNS only** (`a`–`z`, `0`–`9`, `.`); mixed-case keys often fail registration (see `docs/REGISTRATION.md` troubleshooting table).
- **Icon sprite**: vSphere crops **32×32** tiles from **`images/sprites.png`**; **`x` / `y`** are pixel offsets; changing **`configuration.icon`** requires matching **`definitions.iconSpriteSheet.definitions`**.

### Critical Don't-Miss Rules

- **Do not** ship or “vendor” **proprietary OVAs** in-repo; UI/server should accept **approved OVA URL or library reference** only (`docs/ARCHITECTURE.md`).
- **HTTPS** for manifest URL in real vCenter setups; trust **mkcert** CA locally so browsers and vSphere Client accept dev certs.
- **Registration TLS to vCenter**: if **`-insecure` behavior** is flaky, use **`EXTENSION_REGISTRATION_INSECURE=true`** or **`-Dextension.registration.insecure=true`** with the **patched** JAR as documented; **`VCENTER_IP`** when FQDN does not resolve from the registration host.
- **`UnknownHostException` / proxy issues**: lab scripts clear proxy env for the JVM — follow `docs/REGISTRATION.md` before blaming plug-in code.
- **Duplicate extension**: use **`REGISTER_ACTION=updatePlugin`** and same **key** when updating URL/version; bump **`PLUGIN_VERSION`** as needed.
- **Health check**: **`GET /health`** — keep JSON shape stable if external monitors depend on it.

---

## Usage Guidelines

**For AI Agents:**

- Read this file before implementing any code in this repository.
- Follow all rules above; when in doubt, prefer the more restrictive or better-documented option (`docs/`).
- If you change manifest paths, registration, TLS, or upload limits, update **`docs/`** in the same change when behavior is user-visible.

**For Humans:**

- Keep this file **lean**; add rules only when agents repeatedly miss the same detail.
- Update **Technology Stack** when `package.json` engines or major server deps change.
- Remove rules that become obvious or obsolete as the codebase matures.

Last Updated: 2026-04-30
