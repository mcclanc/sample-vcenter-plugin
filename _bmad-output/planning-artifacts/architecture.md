---
stepsCompleted:
  - 1
  - 2
  - 3
  - 4
  - 5
  - 6
  - 7
  - 8
lastStep: 8
status: complete
completedAt: '2026-04-30'
inputDocuments:
  - _bmad-output/planning-artifacts/prd.md
  - _bmad-output/project-context.md
  - docs/ARCHITECTURE.md
  - docs/DEV_SETUP.md
  - docs/REGISTRATION.md
  - docs/SDK_LAYOUT.md
workflowType: architecture
project_name: sample-vcenter-plugin
user_name: Chris
date: 2026-04-30
---

# Architecture Decision Document

_This document builds collaboratively through step-by-step discovery. Sections are appended as we work through each architectural decision together._

## Project Context Analysis

### Requirements Overview

**Functional requirements**

The PRD defines **24 FRs** grouped as: **entry & intent** (FR1–3); **POC registry** (FR4–5); **POC upload** (FR6–7); **POC deploy planning & execution** (FR8–12); **Tanzu Hub connection** (FR13–14); **production path MVP boundary** (FR15); **supportability & flow control** (FR16–18); **cross-cutting UX/product** (FR19–23); **optional automation/API parity** (FR24). Architecturally, this implies clear **bounded contexts**: *client presentation & wizard orchestration*; *artifact acquisition* (registry vs upload); *deploy execution* (vCenter-facing); *Hub integration*; *platform ops* (health, logging, errors).

**Non-functional requirements**

**Performance** (NFR-P1–P4): cold-load budget, wizard step responsiveness, **bounded polling** for long jobs, **pilot concurrency** and resource caps (notably upload temp storage). **Security** (NFR-S1–S5): TLS, **secret hygiene**, upload constraints, **least-privilege** service identities, safe diagnostics. **Accessibility** (NFR-A1): WCAG 2.1 AA within iframe constraints. **Integration** (NFR-I1–I3): timeouts/retries/idempotency, **classifiable** failures aligned to FR17, **contract verification** per MVP integration.

**Scale & complexity**

- **Primary domain:** Full-stack **remote vSphere plug-in** (static UI + plug-in host + optional **deploy orchestrator**).
- **Complexity level:** **Medium–high** driven by **three external integration classes** (Broadcom portal/registry, vCenter OVF/deploy, Tanzu Hub) and **dual POC acquisition** paths including **large binary upload**.
- **Estimated architectural components (logical):** (1) plug-in **UI shell & step flows**; (2) **acquisition** services (registry client, multipart upload); (3) **deploy** adapter (in-process vs external orchestrator); (4) **Hub** client + validation; (5) **shared** cross-cutting (auth context propagation, error taxonomy, correlation, config); (6) **static manifest & asset** serving; (7) **health/readiness** surface.

### Technical constraints & dependencies

- **Brownfield stack:** Node **ESM** host, static **`ui/`** at `/tanzu-hub-poc-ui/`, **htmlClientSdk** stub in dev and injected client in prod; **plugin.json** and registration per `docs/REGISTRATION.md` / `docs/SDK_LAYOUT.md`.
- **PRD / project context:** Same-origin expectations for UI APIs; **bounded JSON** bodies; **OVA upload limits** via env; **no shipping proprietary OVAs** in-repo—artifacts via approved portal or customer upload only.
- **vSphere:** Deploy likely via **REST/OVF** or dedicated orchestrator using **service account**—choice affects trust boundary and where long-running work runs (`docs/ARCHITECTURE.md`).
- **Dependencies:** vCenter **availability** and API version surface; **portal/registry** contract TBD but must be versioned; **Hub** API and auth scheme TBD for MVP depth.

### Cross-cutting concerns identified

- **Session & authorization model:** Mapping **vSphere user** actions to server-side and optional **orchestrator** credentials (FR11–12, NFR-S4).
- **Secrets & logging:** Registry keys, Hub credentials, SSO—**masking**, no plaintext logs, safe correlation payloads (FR16–17, NFR-S2, S5).
- **Error taxonomy & support:** Stable **error classes** and **correlation IDs** across UI and API (FR16–17, NFR-I2).
- **Long-running work:** Download, upload, deploy—**progress**, **cancellation** semantics, **idempotency** where supported (FR5–6, NFR-P3, I1).
- **Accessibility & i18n:** Keyboard primary path and localized strings (FR19–20, NFR-A1, project-type section in PRD).
- **Operations:** TLS/thumbprint/registration, **`/health`**, deployment of plug-in host (Riley journey, FR21, NFR-P4).

## Starter Template Evaluation

### Primary technology domain

**Brownfield Node.js (ESM) + Express** plug-in host with **static HTML/CSS/JS** under `/tanzu-hub-poc-ui/`, aligned with the PRD **web_app** classification and `docs/ARCHITECTURE.md`.

### Starter options considered

Greenfield starters (**Next.js**, **Vite SPA**, **Remix**, etc.) and the **full Angular/Spring** HTML Client SDK sample were **set aside**: they do not match the remote plug-in delivery model or the project rule that product logic lives in **`ui/`** + **`server/`**, with **`html-client-sdk/`** as read-only reference.

### Selected starter: existing repository scaffold

**Rationale:** The repo already implements manifest hosting, SDK load order, static UI prefix, eval/upload/deploy routes, and registration documentation. Extending this scaffold preserves **same-origin**, **health**, and **TLS** assumptions validated in `docs/`.

**Initialization (clone / CI / new machine):**

```bash
cd sample-vcenter-plugin
npm install
npm run certs
npm run start:https
```

**Architectural decisions already provided by the scaffold**

| Area | Decision embodied |
|------|-------------------|
| **Language & runtime** | **ESM** (`.mjs`, `"type": "module"`). `package.json` engines **`node >= 20`** — align with org **Active LTS** policy (Node **20** EOL **2026-04-30** per current schedule; **22/24** LTS available as of 2026). |
| **HTTP & uploads** | **Express ^4.21.0**, **multer ^2.1.1**; bounded **`express.json`** payload (256kb pattern in project context). **Express 5** is the current default line on npm; migration is **optional** and out of starter scope. |
| **UI delivery** | Static **`ui/`**; **`htmlClientSdk`** stub route; **`<base>`** from proxied origin. |
| **Ops surfaces** | **`GET /health`**; HTTPS dev entry via **`npm run start:https`**. |
| **Quality gates** | No root ESLint harness; **manual + integration** verification until a runner is adopted. |

**Note:** First implementation stories extend **`server/**` and `ui/`**; they do not re-run a greenfield CLI generator.

## Core Architectural Decisions

### Decision priority analysis

**Critical (block MVP if undefined)**

1. **Deploy execution boundary** — Where OVF/OVA deploy runs (plug-in host process vs separate **orchestrator** service) and which identity calls vCenter.
2. **Artifact acquisition** — Concrete contract for **registry key → download** vs **upload → temp storage → deploy** (streaming, disk caps, cleanup).
3. **Error & correlation contract** — Stable **JSON error shape** (code, message, correlationId) for UI, logs, and optional FR24 API consumers.

**Important (shape quality and ops)**

4. **Hub integration depth** — MVP: **validate/test** only vs partial APIs; auth scheme (token basic OAuth TBD with product).
5. **Runtime LTS** — Move **`engines.node`** toward org **Active LTS** (e.g. **22+**) on a planned cadence; keep CI matrix in sync.

**Deferred (post-MVP)**

- Application **database** for multi-tenant analytics, full production automation engine, **Express 5** migration, **chunked/resumable** upload unless required earlier.

### Data architecture

- **Decision:** **No primary application database** for MVP POC flows.
- **Rationale:** FRs focus on **wizard + integrations**; persistence is **vCenter inventory** and **artifact files**. Avoid premature DB ops burden.
- **Implications:** In-flight job/deploy state is **in-process and/or filesystem** (upload temp dirs) with **documented cleanup** on success/failure/timeout. If **audit evidence** is required before DB work, use **structured application logs** (no secrets) and/or export to customer SIEM—align with domain "audit-friendly logs."
- **Future hook:** Optional **append-only audit store** or external DB if program mandates retention beyond log rotation.

### Authentication & security

- **Decision:** **Two-tier trust model:** (a) **vSphere Client user** for in-iframe actions the SDK can perform; (b) **service account / API token** (or workflow token) for **server-side** registry download and **vCenter Deploy OVF** when the browser cannot hold long-lived secrets or APIs are server-only.
- **Rationale:** Matches PRD / `docs/ARCHITECTURE.md` and NFR-S4 least-privilege.
- **Secrets:** **Environment + secret store** (customer-provided); never log registry keys or Hub passwords; **mask** in UI (FR22, NFR-S2).
- **Transport:** **TLS** everywhere for plug-in host in production (NFR-S1); dev **mkcert** path per `docs/DEV_SETUP.md`.
- **Upload safety:** **Multer 2.x** with **size/type limits** and temp path isolation; **multer does not scan malware**—document customer responsibility (domain risk table).

### API & communication patterns

- **Decision:** **REST over HTTPS**, **JSON** for control plane; **multipart** for OVA upload. **Dual route** convention preserved: UI-facing **`/tanzu-hub-poc-ui/...`** and compatibility **`/api/...`** where already wired (project context).
- **Errors:** **HTTP status** + body `{ code, message, correlationId?, details? }` with codes mapped to **FR17** categories.
- **Resilience:** **Timeouts** and **bounded retries** on outbound HTTP to portal, Hub, and vCenter client; **idempotency** or **dedupe keys** for deploy submission where vCenter supports safe replay (NFR-I1).
- **Documentation:** OpenAPI or static **`docs/`** route table for FR24 consumers—pick one in first API story.

### Frontend architecture

- **Decision:** **Static modular JavaScript** (or small ES modules) in **`ui/`** without introducing a SPA framework for MVP unless complexity forces it.
- **State:** **Wizard state** in memory; persist only what server needs for deploy jobs; **keyboard-first** paths for FR19.
- **i18n:** Continue **`plugin.json` definitions** as source of keys; avoid hard-coded strings for new surfaces (FR20).

### Infrastructure & deployment

- **Decision:** **Single Node process** (or small fixed pool) behind customer **reverse proxy / TLS termination** in production; **12-factor-style** env config (`VC_*`, `OVA_*`, Hub URLs, etc.).
- **Observability:** **Structured JSON logs** to stdout; **correlationId** propagation middleware; keep **`GET /health`** stable.
- **CI/CD:** Not prescribed here—customer pipeline; architecture requires **contract tests** for vCenter/registry/Hub mocks per NFR-I3.

### Decision impact analysis

**Implementation sequence (suggested)**

1. Finalize **deploy boundary** (in-process vs orchestrator) and **identity** story per environment.
2. Implement **error envelope** + **correlation middleware** across new routes.
3. **Registry client** + **upload pipeline** with caps and cleanup.
4. **Hub validate** path behind feature flag if API TBD.
5. Harden **logging/secrets** review gate before pilot.

**Cross-component dependencies**

- Deploy boundary choice **drives** where multer files are consumed and how progress events are surfaced to UI.
- Error contract **must** be shared by UI fetch layer and FR24 APIs.
- LTS / engine bump **touches** CI, Docker (if any), and registration tooling Java version only indirectly (separate concern).

## Implementation Patterns & Consistency Rules

### Pattern categories defined

**Critical conflict points (addressed below):** HTTP route layout vs **same-origin**; **JSON field casing**; **error body shape**; **server module layout**; **logging vs user errors**; **where new UI and server code live**.

### Naming patterns

**Database naming (MVP):** **N/A** — no application database; do not introduce tables/columns for MVP POC state without an ADR.

**API naming conventions**

- **Paths:** **Lowercase** segments; **kebab-case** multi-word resources (match existing: `/api/eval-appliance/deploy`, `/tanzu-hub-poc-ui/api/...`). New deploy/registry/Hub routes **mirror** both **prefixed** and **unprefixed** variants when the UI requires same-origin parity (see `server/index.mjs` pattern).
- **JSON bodies:** **camelCase** keys for new fields (align with JavaScript clients and existing Express handlers).
- **Query params:** **camelCase** for new APIs unless integrating an upstream that requires snake_case (then isolate at adapter boundary).

**Code naming conventions**

- **Server files:** **`*.mjs`** in **`server/`** (and **`server/<feature>/`** subfolders for new domains); **camelCase** exported functions; use **`node:`** imports for builtins (project context).
- **Client scripts in `ui/`:** **kebab-case** file names for pages/helpers if split (`wizard-state.js`); **camelCase** functions inside. Do not add **TypeScript** unless an ADR introduces a build step.

### Structure patterns

**Project organization**

- **Server:** New behavior under **`server/`** as small modules; wire in **`server/index.mjs`** or a single **`server/routes/*.mjs`** if route count grows—**one obvious registration point** per route.
- **UI:** New views/assets under **`ui/`** only; manifest and i18n remain in **`ui/plugin.json`** per SDK rules.
- **Docs:** User-visible or operator flows that change → update **`docs/`** in the same change (project context).
- **Tests:** Until a runner exists, add **`*.test.mjs`** only if `package.json` gains a test script; prefer **`tests/`** or co-located tests—**pick one** when the first test lands and document it in `docs/DEV_SETUP.md`.

**Reference-only tree:** **`html-client-sdk/`** — no product feature code here.

### Format patterns

**API response formats**

- **Success (JSON):** Return **resource-shaped JSON** directly (no `{ data: { ... } }` wrapper) unless versioning forces a wrapper—**stay consistent** within the plug-in API surface.
- **Errors:** Response body **`{ "code": string, "message": string, "correlationId"?: string, "details"?: object }`** with HTTP **4xx/5xx** appropriate to fault class. **`code`** values must map to the **FR17** taxonomy document maintained with the PRD.
- **Dates:** **ISO-8601** strings in UTC for any timestamps returned to UI or logs.

**Uploads:** **`multipart/form-data`** field names **documented** per route; max size from **`OVA_UPLOAD_MAX_*`** env vars only.

### Communication patterns

**Browser events (if used):** **`domain.action`** in lowercase with dot (e.g. `deploy.progress`). Payloads are **plain objects**, camelCase.

**Server logging:** One **JSON object per line** to stdout: minimum **`{ "level","msg","correlationId" }`**; **never** attach raw **Authorization**, registry keys, or upload bytes.

**In-process progress:** Prefer **polling** endpoints with **stable job IDs** over ad-hoc sockets unless NFRs change—keeps firewall story simple.

### Process patterns

**Error handling**

- **Controllers:** Never `throw` uncaught to Express without an error middleware; map upstream faults to **`code` + HTTP status**.
- **UI:** Map **`code`** to user-visible strings via **`plugin.json`** keys where possible; show **correlationId** on failure panes for support (FR16).

**Loading / long jobs**

- Use explicit **`status: queued|running|succeeded|failed`** (or equivalent) in job responses; **bounded poll interval** on client (NFR-P3).

### Enforcement guidelines

**All AI agents MUST**

- Preserve **`GET /api/ui/htmlClientSdk.js`** stub and **`GET /health`** contracts unless an ADR changes them.
- Add **JSON** routes with a **bounded body limit** (same order of magnitude as existing **256kb** unless justified).
- Keep **secrets out of logs and API responses**; use **masking** for any echoed configuration previews.
- Register **both** UI-prefixed and root API paths when the feature is reachable from the static UI origin.

**Pattern enforcement:** PR / human review checks routes and error shape against this section; update **`_bmad-output/project-context.md`** when a new **global** rule is agreed.

### Pattern examples

**Good**

- `POST /tanzu-hub-poc-ui/api/registry/resolve` + `POST /api/registry/resolve` with shared handler.
- Log line: `{"level":"error","msg":"registry.download_failed","correlationId":"abc-123","code":"REGISTRY_AUTH"}`

**Anti-patterns**

- `require("fs")` in new server code; **`import fs from "node:fs"`** style instead.
- Returning `{ error: "invalid" }` without **`code`** and **HTTP status** alignment.
- Storing **registry keys** in a new **SQLite** file without an ADR and security review.

## Project Structure & Boundaries

### Complete project directory structure

```text
sample-vcenter-plugin/
├── package.json              # scripts, engines, express + multer deps
├── .npmrc                    # npm registry pin (preserve)
├── README.md                 # (if present) high-level clone/run
├── server/
│   ├── index.mjs             # Express app: middleware, /health, SDK stub, static mount, route registration
│   ├── htmlClientSdk.stub.js # dev-only stub served at GET /api/ui/htmlClientSdk.js
│   └── eval-appliance/       # POC OVA: upload, download, deploy, vi URL helpers
│       ├── deployHandler.mjs
│       ├── download.mjs
│       ├── upload.mjs
│       ├── viUrl.mjs
│       └── ovftool.mjs
├── scripts/                   # HTTPS dev, certs, registration helpers, icon tooling
├── ui/                        # shipped plug-in UI + manifest (served under /tanzu-hub-poc-ui/)
│   ├── index.html
│   ├── plugin.json
│   ├── styles.css
│   ├── main.js
│   ├── landing.js
│   ├── usecase.html
│   ├── deploy-production.html
│   ├── connect-vcenter.html
│   └── images/
├── docs/                      # project_knowledge — architecture, setup, registration
├── certs/                     # local TLS (gitignored; generated)
├── html-client-sdk/           # BOUNDARY: Broadcom reference only — no product logic
└── _bmad-output/              # planning artifacts (PRD, architecture); not runtime
```

### Architectural boundaries

**API boundaries**

- **External (northbound):** vSphere Client loads **`ui/`** and calls **`/tanzu-hub-poc-ui/api/**` (and **`/api/**` aliases** where registered). vSphere / Hub / portal are **southbound** HTTP/SDK calls from **`server/`** (or future orchestrator).
- **Internal:** **`server/index.mjs`** is the composition root; feature logic stays in **`server/<feature>/`** modules callable from thin route handlers.

**Component boundaries**

- **`ui/*`:** Presentation + wizard orchestration in JS; **no** secrets for server-only operations unless using a deliberate, short-lived token pattern approved in security review.
- **`server/*`:** Trust boundary for **multer** disk, **registry download**, and **vCenter** operations; emits **structured errors** to UI.

**Service boundaries**

- **Eval appliance subsystem:** `server/eval-appliance/*` owns upload/download/deploy orchestration **until** an external deploy service is introduced—then this package becomes a **client** of that service with the same route surface or a documented redirect.

**Data boundaries**

- **No app DB:** State in **memory + temp files** under controlled dirs; **vCenter** is system of record for VMs; **logs** for audit trail (see Step 4 data decision).

### Requirements to structure mapping

| FR group | Primary location |
|----------|------------------|
| FR1–3 entry & intent | `ui/landing.js`, `ui/index.html`, flow HTML (`usecase.html`, `connect-vcenter.html`, …) |
| FR4–7 POC acquisition | `server/eval-appliance/download.mjs`, `upload.mjs`; new `registry/*.mjs` when split |
| FR8–12 deploy | `server/eval-appliance/deployHandler.mjs`, `ovftool.mjs`, `viUrl.mjs` (+ vCenter adapter when extracted) |
| FR13–14 Hub | new `server/hub/` (recommended) + thin routes in `index.mjs` |
| FR16–18 supportability | shared `server/middleware/` (recommended) for correlation + error envelope |
| FR19–23 cross-cutting | `ui/` + `plugin.json`; shared fetch helper in `ui/` |
| FR24 automation | same handlers as UI; document paths under `docs/` |

### Integration points

- **Internal:** `ui` → `fetch` → same-origin **`/tanzu-hub-poc-ui/api/*`** → `server/index.mjs` → feature modules.
- **External:** `server` → HTTPS → **Broadcom portal**, **vCenter**, **Tanzu Hub** (timeouts per NFR-I1).
- **Data flow (POC upload):** Browser multipart → **multer** temp file → deploy path (ovftool / API) → vCenter inventory → response + cleanup.

### File organization patterns

- **Configuration:** **Environment variables** documented in `docs/`; no committed secrets.
- **Source:** **`server/**/*.mjs`**, **`ui/`** static assets only unless a bundler ADR exists.
- **Tests:** TBD under `tests/` **or** co-located—first test PR picks one and updates `docs/DEV_SETUP.md`.
- **Assets:** `ui/images/`; sprite rules per `docs/SDK_LAYOUT.md`.

### Development workflow integration

- **Dev server:** `npm run start:https` → `scripts/start-https.mjs` → **`server/index.mjs`** with TLS.
- **Build:** No compile step for MVP UI; optional future `ui` build outputs still served from same prefix.
- **Deploy:** Customer packages **Node host + `ui/` + manifest URL**; vCenter registration per `docs/REGISTRATION.md`.

## Architecture Validation Results

### Coherence validation

**Decision compatibility**

Express **4.x** + **multer 2.x** + **ESM** + static **`ui/`** are consistent. **Dual API prefix** (`/tanzu-hub-poc-ui` + `/api`) matches same-origin constraints. **No MVP DB** aligns with **filesystem + vCenter** persistence. **Two-tier trust** (Client user vs service identity) matches PRD / `docs/ARCHITECTURE.md` without contradicting the starter scaffold.

**Pattern consistency**

Naming (**kebab** routes, **camelCase** JSON), error envelope, and logging rules **support** the core decisions on security and supportability.

**Structure alignment**

The directory tree and **FR → folder** mapping match how **`server/index.mjs`** composes today and where new **`server/hub/`** / **`server/middleware/`** slots fit.

### Requirements coverage validation

**Epic/feature coverage**

No epics in inputs; coverage traced via **PRD FR1–24** and **NFR-P/S/A/I** blocks—all have a **home** in `ui/`, `server/`, or docs. **FR24** depends on explicitly exposing the same handlers documented for automation—tracked as implementation + `docs/` work.

**Functional requirements**

All FR groups mapped in **Project Structure & Boundaries**; no orphan FR category.

**Non-functional requirements**

Performance (**polling**, caps), security (**TLS**, secrets, multer limits), accessibility (**WCAG** target), integration (**timeouts**, contract tests) are all **addressed** in decisions or patterns; **numeric budgets** remain measurement tasks, not architecture gaps.

### Implementation readiness validation

**Decision completeness**

Critical trio (**deploy boundary**, **acquisition contract**, **error envelope**) is **named**; **deploy boundary** still needs a **binary choice + ADR** in the first implementation increment. **Hub** auth and endpoints remain **product TBD** with a clear **MVP validate-only** slot.

**Structure completeness**

Product tree is **concrete**; tooling trees (`.agents`, large `_bmad`) correctly excluded from runtime structure.

**Pattern completeness**

Conflict-prone areas (routes, JSON case, errors, logs) are covered; **test folder convention** is explicitly first-PR decision.

### Gap analysis results

**Critical gaps:** **None** for starting implementation—provided the first story sequence follows **Decision impact analysis** and locks **deploy boundary**.

**Important gaps**

1. **Published FR17 code list** (markdown or OpenAPI enum) referenced by UI and API.
2. **Registry portal contract** (URLs, auth headers, error mapping) in `docs/` or spec file.
3. **`server/middleware/`** creation for correlation + error wrapper to avoid drift across routes.

**Nice-to-have**

- OpenAPI export for FR24 consumers.
- Express 5 migration ADR when ready.

### Validation issues addressed

No blocking contradictions found during pass; TBDs listed above are **expected** for brownfield extension and are **scoped** to first stories.

### Architecture completeness checklist

**Requirements analysis**

- [x] Project context thoroughly analyzed
- [x] Scale and complexity assessed
- [x] Technical constraints identified
- [x] Cross-cutting concerns mapped

**Architectural decisions**

- [x] Critical decisions documented with versions (runtime deps pinned in `package.json`; **deploy topology choice** pending ADR)
- [x] Technology stack fully specified for MVP
- [x] Integration patterns defined
- [x] Performance considerations addressed

**Implementation patterns**

- [x] Naming conventions established
- [x] Structure patterns defined
- [x] Communication patterns specified
- [x] Process patterns documented

**Project structure**

- [x] Complete directory structure defined (product scope)
- [x] Component boundaries established
- [x] Integration points mapped
- [x] Requirements to structure mapping complete

### Architecture readiness assessment

**Overall status:** **READY WITH MINOR GAPS** — **deploy execution boundary** must be committed in the first implementation ADR; **FR17** taxonomy should be published alongside route work.

**Confidence level:** **High** for UI/static + Express patterns; **medium** until portal/Hub contracts are captured.

**Key strengths**

- Brownfield-aligned; **no** risky greenfield starter pivot.
- **Dual acquisition** and **error contract** are first-class in the doc set.

**Areas for future enhancement**

- Optional orchestrator split; **Express 5**; **DB** for audit if customers demand retention beyond logs.

### Implementation handoff

**AI agent guidelines**

- Follow **`architecture.md`** + **`prd.md`** + **`_bmad-output/project-context.md`** together.
- Obey **Implementation Patterns** for every new route and log line.

**First implementation priority**

1. ADR: **in-process deploy vs external orchestrator** + identity model.
2. Add **correlation + error middleware** and **FR17** code registry.
3. **Registry download** path + **upload** path hardening per NFRs.
