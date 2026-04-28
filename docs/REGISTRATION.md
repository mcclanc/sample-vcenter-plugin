# Registering the remote plug-in with vCenter

Follow the **vSphere Client SDK** installation and registration chapters in Broadcom’s guide: [Developing Remote Plug-ins with the vSphere Client SDK](https://techdocs.broadcom.com/us/en/vmware-cis/vsphere/vsphere-sdks-tools/9-0/developing-remote-plug-ins-with-the-vsphere-client-sdk-8-0.html).

## Manifest URL

With the dev server in this repo, the manifest is:

`https://<your-host>:8443/tanzu-hub-poc-ui/plugin.json`

Use a hostname and certificate that vCenter trusts (or add your CA to trust). Self-signed dev certs require registering the server thumbprint per the SDK registration tool / API.

## TLS for local development

From the repo root, generate **localhost** certs and run with HTTPS:

```bash
npm run certs
npm run start:https
```

Or set paths yourself:

```bash
export SSL_KEY_PATH=/path/to/key.pem
export SSL_CERT_PATH=/path/to/cert.pem
npm start
```

## Extension registration tool (HTML Client SDK)

The Broadcom JAR and launcher live under:

`html-client-sdk/tools/vCenter plugin registration/prebuilt/`

Example (run on a machine that **DNS-resolves your vCenter** and can reach the plug-in URL):

```bash
chmod +x html-client-sdk/tools/vCenter\ plugin\ registration/prebuilt/extension-registration.sh
```

### Lab: trust vCenter TLS without `-vct`

With the bundled Commons CLI, **`-insecure` is not always applied** when combined with all other flags. This repo’s **patched** `extension-registration.jar` also treats either of the following as “insecure to vCenter” (equivalent intent):

- `EXTENSION_REGISTRATION_INSECURE=true`
- `JAVA_OPTS=-Dextension.registration.insecure=true` (the `extension-registration.sh` wrapper passes `JAVA_OPTS` through to `java`)

Convenience script (password via env, never committed):

```bash
export VC_PASSWORD='your-sso-password'
export PLUGIN_SERVER_TP='3A:6B:67:…'   # SHA-1 of the plug-in host cert (colons)
./scripts/register-extension-lab.sh
```

**Interactive prompts:** from a normal terminal, run `./scripts/register-extension-lab.sh` with no `VC_PASSWORD` / `PLUGIN_SERVER_TP` (or leave them unset) and it will ask for vCenter URL, SSO user/password, manifest URL, thumbprint, extension key, version, and display name—press Enter to keep each `[default]`. Pre-set any variable in the environment to skip that prompt. For CI or scripts, set `REGISTER_NON_INTERACTIVE=1` and export `VC_PASSWORD` and `PLUGIN_SERVER_TP` at minimum.

Override defaults with `VC_SDK_URL`, `PLUGIN_URL`, `PLUGIN_KEY`, etc. See `scripts/register-extension-lab.sh`.

If the vCenter **FQDN does not resolve** from the machine that runs registration (common on laptops without split‑DNS), use **`VCENTER_IP=<vCenter-IP>`** so the script uses `https://<IP>/sdk` instead of the hostname (patched JAR “insecure” still applies to vCenter TLS). Example: `VCENTER_IP=172.16.30.14 ./scripts/register-extension-lab.sh`.

### If registration fails with a TLS / FQDN message

Check the **root cause** in the nested exception (this environment showed `UnknownHostException` when vCenter’s DNS name was not resolvable). Use your vCenter **FQDN** in `-url` and run the tool from a network that can resolve and reach it.

### Common errors (what to look for in the stack trace)

| Symptom / “Caused by” | What to do |
|------------------------|------------|
| **`UnknownHostException`** for your vCenter name | (1) Confirm DNS: `host vcenter.cmaclabs.com` (or your FQDN). (2) Connect VPN if the name is **internal-only**. (3) Add **`/etc/hosts`** if needed: `172.16.x.x vcenter.cmaclabs.com`. (4) **Bypass DNS:** **`VCENTER_IP=<IP>`** (or **`VC_SDK_URL=https://<IP>/sdk`**) when only the IP works from that host. (5) **Proxies:** stack frames mentioning **`SocksSocketImpl`** often mean a SOCKS path — the lab script clears proxy env vars, **`JAVA_TOOL_OPTIONS`**, and sets **`-Djava.net.useSystemProxies=false`**, **`http.nonProxyHosts`**, and empty **`http(s).proxyHost` / `socksProxyHost`** for this JVM. |
| **`InvalidLoginFault`** / login error | Check `VC_USER` / `VC_PASSWORD` and SSO lockout; use the same account you use in the vSphere Client. |
| **Thumbprint / SSL** when talking to **192.168.x** (plug-in host) | Re-copy **`-st` / `PLUGIN_SERVER_TP`** from the cert actually served on that IP: `openssl s_client -connect IP:8443 … \| openssl x509 -fingerprint -sha1 -noout`. |
| **Download / manifest** errors for `plugin.json` | From the **vCenter appliance** (or same network path), `curl -vk https://<plugin-ip>:8443/tanzu-hub-poc-ui/plugin.json` must succeed; keep `npm run start:https` running on the plug-in host. |
| **SOAP Fault: `extension.key` / “parameter was not correct”** | (1) Use **lowercase** reverse-DNS only (`a`–`z`, `0`–`9`, `.`), e.g. `com.yourorg.remote.paasforvcf` — **mixed case often fails**. (2) Run **`unset PLUGIN_KEY`** if your shell still has an old value (`com.example.*` is commonly rejected). (3) Match the Broadcom sample shape if unsure: **`com.<yourorg>.remote.<name>`**. (4) If the extension **already exists** and you only change manifest/URL/version, use **`REGISTER_ACTION=updatePlugin`** with the **same** key instead of a second `registerPlugin`. (5) **`register-extension-lab.sh`** validates the key and calls the JAR with **`-username` / `-password` / `-pluginUrl`** (sample style), **not** `-p` then `-pu`, so Commons CLI cannot mis-parse `-pu` as `-p` and send a bad key to vCenter. (6) **`REGISTER_DEBUG=1`** prints the resolved `-key` and other args (password length only) before Java runs. (7) If it still fails, check **vpxd** logs on the vCenter appliance for the precise invalid field. |
| **Extension already exists** | Unregister the same **`-k` / `PLUGIN_KEY`** or bump **`-v` / `PLUGIN_VERSION`** per Broadcom’s update flow. |

## Related doc links

- [Register the vSphere Client Remote Plug-in Sample](https://techdocs.broadcom.com/us/en/vmware-cis/vsphere/vsphere-sdks-tools/8-0/developing-remote-plug-ins-with-the-vsphere-client-sdk-8-0/using-the-vsphere-client-remote-plug-in-sample/register-the-vsphere-client-remote-plug-in-sample.html) (same family of steps for your own manifest URL).
