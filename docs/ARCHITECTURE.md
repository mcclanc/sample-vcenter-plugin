# Architecture

## Goal

A **remote vSphere Client plug-in** that adds UI in vSphere Client to plan and trigger deployment of a **Tanzu Platform / Hub POC** style environment (typically an evaluation OVA on vSphere 8+), aligned with Broadcom’s vSphere 9 remote plug-in documentation.

Primary SDK reference: [Developing Remote Plug-ins with the vSphere Client SDK (vSphere 9)](https://techdocs.broadcom.com/us/en/vmware-cis/vsphere/vsphere-sdks-tools/9-0/developing-remote-plug-ins-with-the-vsphere-client-sdk-8-0.html).

## Components

| Piece | Role |
|--------|------|
| `ui/plugin.json` | Remote plug-in manifest (extension points, i18n, global view). See also Broadcom’s [sample manifest](https://techdocs.broadcom.com/us/en/vmware-cis/vsphere/vsphere-sdks-tools/8-0/developing-remote-plug-ins-with-the-vsphere-client-sdk-8-0/creating-a-remote-plug-in-for-the-vsphere-client/sample-manifest-file-for-a-remote-plug-in.html). |
| `ui/*` | Static HTML/CSS/JS served to the vSphere Client iframe. Integrate **vSphere Client Remote Plug-in SDK** (JavaScript module) here for session-aware API calls where the host supports it. |
| `server/index.mjs` | Development web server: serves the UI under `/tanzu-hub-poc-ui` so the manifest URL is stable for registration tooling. Production should use a hardened reverse proxy (TLS 1.2+, trusted certs). |

## Tanzu POC / evaluation context

Evaluation appliances and vSphere deploy flow are documented under Tanzu Platform evaluation appliances (Broadcom Tech Docs). Use that as the source of truth for sizing, DNS, and OVA handling—for example [Deploying Tanzu Platform evaluation appliances on vSphere](https://techdocs.broadcom.com/us/en/vmware-tanzu/platform/tanzu-platform-evaluation-appliances/10-3/tp-evaluation-appliances/deploy.html).

This repo does **not** bundle proprietary OVAs; the plug-in should accept an OVA URL or Content Library reference your org approves.

## Deployment execution options

1. **In-browser (SDK)**  
   Use official remote plug-in client APIs to perform supported operations with the logged-in user’s session (subject to extension privileges and API surface).

2. **Backend orchestrator (common for OVF)**  
   A small service (same host or separate) uses a **service account** or **workflow token** to call vSphere REST / `govc` / pyVmomi for `Deploy OVF` and property injection. The plug-in UI collects inputs and calls your API. Harden authn/z between vSphere Client → plug-in server → orchestrator.

Choose based on your security model and whether the SDK exposes the operations you need for your vCenter version.
