---
stepsCompleted:
  - step-01-init
  - step-02-discovery
  - step-02b-vision
  - step-02c-executive-summary
  - step-03-success
  - step-04-journeys
  - step-05-domain
  - step-06-innovation
  - step-07-project-type
  - step-08-scoping
  - step-09-functional
  - step-10-nonfunctional
  - step-11-polish
inputDocuments:
  - _bmad-output/project-context.md
  - docs/ARCHITECTURE.md
  - docs/DEV_SETUP.md
  - docs/REGISTRATION.md
  - docs/SDK_LAYOUT.md
briefCount: 0
researchCount: 0
brainstormingCount: 0
projectDocsCount: 4
workflowType: 'prd'
releaseMode: phased
classification:
  projectType: web_app
  domain: general
  complexity: medium
  projectContext: brownfield
---

# Product Requirements Document - sample-vcenter-plugin

**Author:** Chris
**Date:** 2026-04-30

## Executive Summary

This product is a **remote vSphere Client plug-in** (static web UI plus a small Node host) that lets **vSphere administrators** start from **outcome-oriented choices** inside the client: run one of **three supported use cases**, or **connect this vCenter to an existing Tanzu Hub**. After a use case is selected, the experience branches to **POC appliance deployment** versus a **full production implementation** of that use case. For **POC**, the plug-in supports two **first-class acquisition paths**: enter a **Broadcom support portal registry key** so the host can **download and deploy** the POC appliance into vCenter, or **upload an already-downloaded POC OVA** and supply the **information required to deploy** it into vCenter. The problem being solved is **fragmented evaluation and rollout**—admins otherwise juggle docs, portals, content libraries, and OVF wizards with inconsistent policy fit (connected vs restricted environments).

### What Makes This Special

The differentiator is a **guided decision tree** aligned to **how enterprises actually obtain software**: not a single “paste OVA URL” path. **Registry-backed download** serves customers who can use portal credentials; **upload + structured deploy inputs** serves **air-gapped or policy-constrained** estates. Pairing that with an explicit **Tanzu Hub connection** path and **POC vs production** keeps intent visible and reduces wrong-size deployments. Compared to **manual OVF deploy** or **external-only documentation**, the plug-in concentrates **licensing/delivery choice** and **vCenter placement** in one in-client flow.

## Project Classification

| Attribute | Value |
|-----------|--------|
| **Project type** | `web_app` — browser-hosted remote plug-in UI (`ui/`) with HTTPS manifest and optional backend APIs for deploy/orchestration patterns described in project docs |
| **Domain** | `general` (workflow taxonomy); **enterprise IT / virtualization / Tanzu** in practice |
| **Complexity** | **Medium** — vCenter registration/TLS, session-aware client SDK usage, OVA lifecycle, registry vs upload, optional Hub integration |
| **Project context** | **Brownfield** — extends `sample-vcenter-plugin` with existing architecture and deploy documentation |

## Success Criteria

### User Success

- An authenticated vSphere user can reach the **first decision** (use case path vs **connect to existing Tanzu Hub**) without leaving the plug-in global view, with copy and controls understandable without external runbooks.
- On the **use-case path**, the user can **select one of three use cases**, then **POC vs full production**, and complete the POC path via **either** registry-key-driven download+deploy **or** upload+guided deploy inputs, ending in a **clear completion state** (success with next steps, or actionable failure with reason).
- **Relief moment:** POC deploy completes in vCenter without the admin manually hunting OVA in a separate portal **when** they chose the registry path; for upload path, **no surprise missing fields** after upload (required placement/networking/storage captured up front or progressively with validation).
- **Hub path:** User selecting "connect to existing Tanzu Hub" can complete whatever **connection / registration / URL + credential** flow the PRD specifies, with verification feedback (connected vs not).

### Business Success

- **Adoption (90 days):** Plug-in registered on **N** pilot vCenters (target set by program); **≥1 successful POC deploy** or **≥1 successful Hub connection** per pilot where applicable.
- **Efficiency:** Median time from **open plug-in → deploy job submitted** for POC (registry path) **below** current manual baseline (measure in pilot interviews or telemetry if available).
- **Support load:** Reduction in **Tier-1 "where is the OVA / which wizard"** questions relative to pre-plug-in baseline (qualitative in MVP; optional ticket tagging later).

### Technical Success

- **Manifest and HTTPS:** `plugin.json` served over TLS patterns compatible with vCenter registration; health endpoint stable for ops checks (`GET /health` per project context).
- **Deploy APIs:** Documented routes for eval/OVA deploy remain coherent (including **`/tanzu-hub-poc-ui`** prefix where UI expects same-origin); upload path respects **`OVA_UPLOAD_MAX_*`** caps and fails safely on oversize or corrupt OVA.
- **Security:** No plaintext storage of **registry keys** or **SSO passwords** in logs or UI beyond masked display; secrets handling aligned with `_bmad-output/project-context.md` and `docs/REGISTRATION.md` norms.
- **Resilience:** Deploy failures return **structured errors** (auth to registry, download timeout, vCenter API fault, validation) suitable for admin remediation.

### Measurable Outcomes

| Outcome | Signal |
|--------|--------|
| POC via registry | Successful download using portal key + OVF deploy initiated to chosen inventory path |
| POC via upload | Successful deploy after upload + completed required property wizard |
| Hub connect | Connection test passes or Hub acknowledges linkage per defined integration |
| Quality | **0** P1 defects open on "cannot complete happy path" for each MVP branch before pilot exit |

## Product Scope

### MVP - Minimum Viable Product

- **Entry:** Two top-level options — **use cases** vs **connect to existing Tanzu Hub** (Hub path may be minimal viable "collect connection info + validate" if full integration is phased).
- **Use cases:** Present **three** existing use cases; after selection, **POC vs production** choice.
- **POC:** **Both** acquisition modes — **registry key → download → deploy** and **upload OVA → collect deploy fields → deploy** — with input validation and clear completion/failure.
- **Production (use case):** At minimum **definition of scope placeholder** or **guided checklist** if full automation is post-MVP; must not block shipping POC+Hub MVP if product decision is POC-first.

### Growth Features (Post-MVP)

- **Full production implementation** automation per use case (templates, sizing, multi-VM, day-2 config).
- Deeper **Tanzu Hub** lifecycle (drift, inventory sync, policy bundles) beyond initial connect.
- **Telemetry** for funnel metrics (drop-off at each branch) and **role-based** restrictions.

### Vision (Future)

- **End-to-end platform lifecycle** from evaluation through production with **governance** (approvals, change windows, integration with ITSM).
- **Multi-tenant / MSP** patterns and **content library** integration as optional first-class paths alongside registry and upload.

## User Journeys

### Dana — Platform admin (primary, happy path: registry POC)

**Opening:** Dana is on-call to stand up a **Tanzu-style POC** for a line-of-business demo Monday. She lives in **vSphere Client** and dreads another night of tab-hopping between the support portal, downloads, and **Deploy OVF**.

**Rising action:** She opens the plug-in, chooses **use cases**, picks the relevant use case, selects **POC**, then **registry key**. She pastes the **Broadcom support portal** key, picks cluster/datastore/network, and starts deploy.

**Climax:** Progress shows **download → validate → deploy to inventory** without leaving the client; vCenter shows the new VM.

**Resolution:** She hands off the **console URL / next-step checklist** to the app team. **Relief:** no manual OVA hunt when the key path works.

**Failure / recovery:** If the key is invalid, she sees a **specific error** and fixes entitlement or typo without opening a sev-1 blind.

### Marco — Platform admin (primary, edge: air-gapped upload)

**Opening:** Marco's site **cannot** pull from external registries. Security shipped him an OVA on **sneakernet**.

**Rising action:** Same use case → **POC** → **upload OVA**. He selects the file; the wizard asks for **placement, storage, networking, and OVF properties** before submit—no "upload then discover missing fields."

**Climax:** Deploy succeeds from **local bytes** only.

**Resolution:** Compliance is satisfied; POC timeline holds.

**Edge:** Corrupt or oversize OVA → **clear limit message** (`OVA_UPLOAD_MAX_*`) and retry path.

### Riley — Plug-in / infrastructure ops (secondary)

**Opening:** Riley owns **HTTPS**, **manifest URL**, and **vCenter registration**. The plug-in worked in lab; production vCenter is strict about **thumbprint and CA**.

**Rising action:** She follows `docs/REGISTRATION.md` patterns: trusted cert or documented insecure path for lab only, `plugin.json` reachable from vCenter, extension key/version discipline.

**Climax:** **Global view loads** in production Client without mixed-content or 404 on **`htmlClientSdk`**.

**Resolution:** Platform admins can use the feature set; **GET /health** is green for monitoring.

### Sam — Support / L2 (troubleshooting)

**Opening:** A ticket says "**Deploy stuck at 40%**" with no actionable detail.

**Rising action:** Sam collects **correlation ID**, timestamp, branch (registry vs upload), and **structured error payload** from UI or API logs.

**Climax:** Error maps to a **known class** (registry auth, download timeout, vCenter API fault, validation).

**Resolution:** Sam sends **precise remediation** (renew key, fix DNS, datastore permissions) instead of generic "try again."

### Jordan — Automation / integration (API consumer)

**Opening:** Jordan wants **CI lab** spins of the same POC from a pipeline.

**Rising action:** They call the **documented deploy endpoints** (same contract as UI) with **service identity** or approved token model aligned to `docs/ARCHITECTURE.md` orchestrator pattern.

**Climax:** Headless run reaches the same **completion states** as Dana's path.

**Resolution:** Repeatable labs without manual clicking.

### Journey Requirements Summary

| Capability area | Driven by |
|-----------------|-----------|
| **Top-level navigation** | Dana, Marco — use case vs Hub; three use cases; POC vs production |
| **Registry acquisition + progress** | Dana — key validation, download, deploy status, errors |
| **Upload + property capture** | Marco — limits, progressive validation, pre-submit completeness |
| **Hub connect flow** | Dana/Marco when choosing Hub path — URL/credentials/test result |
| **TLS, manifest, SDK load order** | Riley — production readiness |
| **Structured errors + correlation** | Sam — supportability |
| **Deploy API parity / automation** | Jordan — backend contract, auth model |

## Domain-Specific Requirements

### Compliance & Regulatory

- **Broadcom / VMware usage:** POC OVAs and **registry keys** are used only in ways consistent with **support portal and license terms**; the product does not redistribute proprietary OVAs from the repo (`docs/ARCHITECTURE.md`, project context).
- **Customer overlays:** Some tenants will map this to **SOC 2–style** expectations (change control, evidence of who deployed what). MVP should support **audit-friendly logs** (who/when/what outcome) where the platform identity model allows it.
- **Data residency / export:** Deploy artifacts and **download traffic** may need to stay in **approved regions**; registry path must be **configurable or documentable** for proxy and egress policies.

### Technical Constraints

- **Identity:** Operations run under **vSphere user context** for in-client actions; **backend/orchestrator** paths (if used) require a **documented service account or token** model, least privilege on **Deploy OVF** and inventory placement, no broad admin shares (`docs/ARCHITECTURE.md`).
- **Secrets:** **Registry keys**, **Hub credentials**, and **SSO passwords** must not appear in plaintext logs; masking in UI; secure handling at rest/in memory per project security norms.
- **Network:** vCenter and plug-in host **TLS** trust (thumbprint/CA), optional **air-gap** (upload-only path), timeouts and retries for **external registry** calls with safe backoff.
- **Availability:** Plug-in host **health** checks; deploy operations should be **idempotent or clearly resumable** where vCenter APIs allow; user-visible state during long OVF deploys.

### Integration Requirements

- **vSphere:** vCenter **REST / OVF deploy** (or supported SDK path) consistent with chosen architecture (browser vs orchestrator); correct handling of **datacenter, cluster, folder, network, datastore**.
- **Broadcom support / download surfaces:** Contract for **registry key → artifact resolution** must be explicit (APIs, headers, auth) and versioned.
- **Tanzu Hub (existing):** **Base URL, auth scheme, test handshake**, and failure modes when Hub is unreachable or returns **4xx/5xx**.

### Risk Mitigations

| Risk | Mitigation |
|------|------------|
| **Credential leakage** | Structured logging without secrets; short-lived tokens where possible; docs for ops hardening |
| **Malicious or tampered OVA (upload path)** | Checksum or signing policy where product allows; size/type validation; clear "enterprise acceptance" boundary |
| **Failed partial deploy** | Clear failure state in UI; support correlation IDs; documented cleanup for orphaned tasks if any |
| **Wrong environment (prod vs lab)** | **POC vs production** branching and copy that states blast radius; optional guardrails (e.g. naming prefix) in later releases |

## Innovation & Novel Patterns

### Detected Innovation Areas

- **In-client "delivery topology" branching:** First-class **registry-key download** and **admin-upload OVA** paths for the same POC outcome—treating **how bits enter the estate** as a primary design axis, not an afterthought.
- **Intent-first navigation:** **Use case vs existing Tanzu Hub**, then **POC vs production**, reducing wrong-size installs compared to a single deploy wizard.
- **Operational narrative alignment:** UX mirrors **enterprise reality** (connected vs air-gapped) without forcing unsupported shortcuts (e.g. bundling proprietary OVAs in-repo).

### Market Context & Competitive Landscape

- Typical alternatives: **manual OVF** from portal/downloads, **CLI/docs**, or **separate portals**—often weak on **consistent in-vCenter guidance** and **dual acquisition**. Incumbent advantage is **distribution and trust** of official channels; this plug-in's angle is **guided execution** and **parity between acquisition modes** where competitors often optimize one path only.

### Validation Approach

- **Pilot A/B:** Measure completion rate and time for **registry vs upload** cohorts on representative networks (with/without egress).
- **Usability:** Task-based tests for **Dana** and **Marco** journeys; error-message comprehension tests with **Sam**.
- **Technical:** Contract tests for **registry resolution** and **vCenter deploy** APIs; resilience tests (timeouts, partial failures).

### Risk Mitigation

- **MVP shape:** Dual POC acquisition (registry + upload) preserves a fallback if either integration slips; see **Project Scoping → Risk Mitigation Strategy** for scope gates.
- **Impact guard:** Instrument failed deploys and support volume; avoid UX churn that does not move those metrics.

## Web App Specific Requirements

### Project-Type Overview

The deliverable is a **remote vSphere Client plug-in**: a **single-page, static web UI** served under **`/tanzu-hub-poc-ui/`** with **`htmlClientSdk`** loaded first and **`<base>`** derived from `getProxiedPluginServerOrigin()` (`docs/SDK_LAYOUT.md`, `docs/ARCHITECTURE.md`). This is **MPA-like hosting with SPA-style navigation** inside one global view (wizard-style steps). **Native mobile apps and general-purpose CLIs** are out of scope for this step (`skip_sections`).

### Technical Architecture Considerations

- **Same-origin expectations:** UI calls APIs on the plug-in host; keep **`/tanzu-hub-poc-ui`** and root API aliases coherent for deploy/upload routes (`project-context.md`).
- **Session context:** Rely on **vSphere Client–injected** `htmlClientSdk` in production; dev **stub** for local HTTPS.
- **Security surface:** Bounded JSON body size; large binary via **multipart** with documented max size env vars.

### Browser Matrix

- **Primary:** Browsers supported by the **vSphere Client versions** you target (document explicit **Client version ↔ browser** matrix in implementation docs).
- **Verification:** Smoke and regression on **latest N** supported combinations; no requirement to support arbitrary consumer browsers outside Client support.

### Responsive Design

- **Desktop admin UI** optimized for **Client iframe** width; layouts must not assume full 4K canvas—support **horizontal scroll only as last resort**.
- **Wizard steps:** Clear step chrome, sticky primary actions where helpful.

### Performance Targets

- **Cold load:** Initial plug-in shell interactive within a **documented budget** (e.g. **< 3 s** on reference LAN after auth—tune with measurements).
- **Long operations:** **Deploy/download** show **determinate or stepped progress** with bounded **poll interval** and cancel/timeout behavior where APIs allow.
- **Uploads:** Progress indicator for large OVA; **chunked/resumable** upload is post-MVP unless required.

### SEO Strategy

- **Not applicable** for an authenticated **vSphere Client** extension: no public indexing, **no** SEO investment; `plugin.json` strings are for **Client UI**, not search engines.

### Accessibility Level

- **Target WCAG 2.1 AA** for new UI where feasible within the iframe: **keyboard operability**, visible **focus**, **labels** for form controls, **error text** associated with fields.
- **Theme:** Respect Client **light/dark** where possible; icon sprite theming per manifest patterns when needed (`docs/SDK_LAYOUT.md`).

### Implementation Considerations

- **i18n:** Keep **`plugin.json` `definitions`** keys aligned with UI copy; avoid hard-coded English-only strings for user-visible errors where product ships multiple locales.
- **Errors:** User-facing messages **actionable** and mapped to support playbooks (ties to **Sam** journey).
- **Testing:** Manual matrix per release until an automated UI harness exists (`project-context.md`).

## Project Scoping & Phased Development

### MVP Strategy & Philosophy

**MVP approach:** **Problem-solving / pilot MVP**—smallest shippable slice where admins complete **(a)** use-case–guided **POC** via **registry or upload**, and/or **(b)** a **minimal viable "connect to existing Tanzu Hub"** validation, inside vSphere Client, with **structured errors** suitable for support.

**Resource requirements (indicative):** **Full-stack** (remote plug-in **UI** + **Node** host), **vSphere integration** knowledge (OVF/deploy or orchestrator), **security-aware** handling of keys and uploads; optional **Hub** integration owner for that branch.

### MVP Feature Set (Phase 1)

**Core user journeys supported:**

- **Dana** — registry-key POC path end-to-end where portal egress exists.
- **Marco** — upload POC path with pre-submit validation and size limits.
- **Riley** — production-style **HTTPS + registration** path documented and reproducible.
- **Sam** (MVP-lite) — **structured error classes** and correlation-friendly logging for deploy failures.
- **Jordan** (optional MVP) — only if program commits: **API parity** with UI for deploy; otherwise defer to Phase 2.

**MVP capability boundary:** Phase 1 must satisfy **Product Scope → MVP** and the signals in **Success Criteria → Measurable Outcomes**. **FR1–FR24** state the capability contract; nothing from discovery was removed from that baseline without an explicit scope-change note.

### Post-MVP Features

**Phase 2 (growth):**

- **Full production implementation** automation per use case (beyond checklist/placeholder if MVP shipped lighter).
- **Deeper Hub** lifecycle (sync, inventory, policy) per Product Scope **Growth**.
- **Telemetry** funnel metrics; **role-based** restrictions; **chunked/resumable** upload if Phase 1 used simple multipart.

**Phase 3 (expansion / vision):**

- **Governance** (approvals, change windows, ITSM), **MSP/multi-tenant**, **content library** as additional acquisition path—per **Vision** in Product Scope.

### Risk Mitigation Strategy

- **Technical:** Parallel **registry** and **upload** MVP reduces single-point failure on portal integration; document **vCenter/orchestrator** choice from `docs/ARCHITECTURE.md`.
- **Market / adoption:** Pilot success = **completed POC or verified Hub handshake** per success criteria; collect qualitative **"relief moment"** feedback from Dana/Marco journeys.
- **Resource:** If Hub or registry slips, **do not** remove upload/use-case MVP; narrow Hub to **documented stub** only with explicit program sign-off (scope-change gate—not assumed here).

## Functional Requirements

### Entry & guided intent

- **FR1:** An authenticated vSphere user can start from a choice between **pursuing a supported use-case flow** and **connecting this vCenter to an existing Tanzu Hub**.
- **FR2:** An authenticated vSphere user can select **one of three** defined use cases when on the use-case path.
- **FR3:** An authenticated vSphere user can indicate **POC appliance deployment** versus **full production implementation** for the selected use case.

### POC — Registry acquisition

- **FR4:** An authenticated vSphere user can provide a **Broadcom support portal registry key** (or equivalent program-defined credential) to authorize **retrieval** of the POC appliance artifact for deployment.
- **FR5:** An authenticated vSphere user can observe **status from artifact retrieval through deployment submission** on the registry acquisition path.

### POC — Upload acquisition

- **FR6:** An authenticated vSphere user can **upload** a POC appliance file obtained outside the plug-in when not using the registry path.
- **FR7:** An authenticated vSphere user can be informed when an upload is **rejected** for policy or size reasons using **non-sensitive** messaging.

### POC — Deployment planning & submission

- **FR8:** An authenticated vSphere user can supply **placement and connectivity inputs** required to deploy the POC appliance into **vCenter-managed inventory** (e.g. compute, storage, network targets as defined by the product).
- **FR9:** An authenticated vSphere user can supply **OVF or appliance properties** required for a valid deployment before submission.
- **FR10:** An authenticated vSphere user can receive **validation feedback** when required inputs are missing or invalid **before** deployment submission.
- **FR11:** An authenticated vSphere user can **initiate** deployment of an accepted POC appliance from within the plug-in flow.
- **FR12:** An authenticated vSphere user can receive a **terminal outcome** for a deployment attempt (success with next actions, or failure with a **remediation-oriented** reason).

### Tanzu Hub (existing) connection

- **FR13:** An authenticated vSphere user can provide **connection details** needed to associate this environment with an **existing** Tanzu Hub instance.
- **FR14:** An authenticated vSphere user can run a **connection validation** whose result distinguishes **success** from **failure** at the level the product defines.

### Full production implementation (MVP boundary)

- **FR15:** An authenticated vSphere user selecting **full production implementation** can access **product-defined guidance** (e.g. checklist, initiation steps, or scoped automation) for that use case consistent with phased scope.

### Transparency, supportability, and continuity

- **FR16:** A user or support-facing consumer can obtain a **correlation reference** for a deployment attempt when the product surfaces one.
- **FR17:** User-visible deployment failures can be shown under a **documented set of error categories** without exposing **secrets** or full credential values.
- **FR18:** An authenticated vSphere user can **withdraw from** an in-progress multi-step flow according to product-defined **cancel or abandon** rules.

### Cross-cutting product behavior

- **FR19:** An authenticated vSphere user can complete **essential wizard steps** without **pointer-only** interaction for those steps (keyboard-capable primary path).
- **FR20:** An authenticated vSphere user can experience **localized or alternate-language** user-visible strings for primary flows when the product ships more than one locale.
- **FR21:** Operations staff can use a **documented readiness signal** for the plug-in host service.
- **FR22:** The product can enforce **documented limits** on sensitive input retention in the **UI** after dismissal or completion rules appropriate to the flow.
- **FR23:** An authenticated vSphere user can access **program-offered pointers** to external documentation or support channels where the product provides them.

### Optional program commitment (automation consumer)

- **FR24:** When explicitly in scope for the release, an **automation consumer** can perform **deployment-equivalent actions** through **documented programmatic interfaces** that mirror the user-visible deploy contract at the capability level.

## Non-Functional Requirements

### Performance

- **NFR-P1:** Initial plug-in shell (first meaningful interaction after load) meets a **documented cold-load budget** on a reference LAN environment (baseline to be measured; starting target aligned with Web App Specific Requirements, e.g. **under 3 seconds** after vSphere auth stabilizes).
- **NFR-P2:** Standard wizard transitions (step-to-step) complete within a **documented P95 interaction time** under reference load (define measurement method in test plan).
- **NFR-P3:** Long-running operations (**registry download**, **OVA upload**, **vCenter deploy**) expose **bounded-frequency** status updates so the UI remains responsive and does not flood the browser or backend.
- **NFR-P4:** The plug-in host supports **concurrent pilot usage** (documented upper bound of simultaneous sessions and deploy jobs) without **unbounded** resource growth (memory, temp disk for uploads).

### Security

- **NFR-S1:** All **browser-to-plug-in-host** traffic for the shipped configuration uses **TLS** with certificates and trust models appropriate for vCenter registration (see registration docs).
- **NFR-S2:** **Registry keys**, **Hub credentials**, and **SSO-related** inputs are **never written to logs** in recoverable plaintext; UI follows **masking** rules for sensitive fields.
- **NFR-S3:** Uploaded artifacts are subject to **documented size and type constraints**; failures are explicit and non-leaky.
- **NFR-S4:** **Least privilege** for any service identity used to reach vCenter or external systems—permissions limited to **deploy and inventory placement** needs (no blanket admin).
- **NFR-S5:** Correlation identifiers and diagnostics **must not** embed secrets or raw credential material.

### Accessibility

- **NFR-A1:** New or materially changed primary flows meet **WCAG 2.1 Level AA** for the scope the Client iframe allows (keyboard, focus, labels, errors associated to fields)—exceptions documented with rationale if any.

### Integration

- **NFR-I1:** Calls to **vCenter**, **Tanzu Hub**, and **Broadcom portal/registry** surfaces use **documented timeouts**, retries where safe, and **idempotent** client behavior where APIs support it.
- **NFR-I2:** Integration failures return **classifiable** outcomes that map to the **FR17** error categories and support playbooks.
- **NFR-I3:** Contract tests or equivalent verification exist for **each external integration** the MVP depends on (vCenter deploy path minimum; Hub and registry per program scope).
