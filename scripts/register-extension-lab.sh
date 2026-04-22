#!/usr/bin/env bash
# Lab helper: register this repo's remote plug-in with vCenter using Broadcom's extension-registration tool.
# Run from a host that resolves your vCenter FQDN and can reach the plug-in HTTPS URL.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TOOL="$ROOT/html-client-sdk/tools/vCenter plugin registration/prebuilt/extension-registration.sh"

if [[ ! -x "$TOOL" ]]; then
  echo "Registration launcher not found or not executable:" >&2
  echo "  $TOOL" >&2
  echo "Fix: chmod +x \"$TOOL\"" >&2
  echo "Also ensure html-client-sdk is unpacked under: $ROOT/html-client-sdk/" >&2
  exit 1
fi

VC_SDK_URL="${VC_SDK_URL:-https://vcenter.cmaclabs.com/sdk}"
# When internal DNS does not work from this host, point at the vCenter IP (same as browser / curl).
# Example: VCENTER_IP=172.16.30.14 ./scripts/register-extension-lab.sh
if [[ -n "${VCENTER_IP:-}" ]]; then
  VC_SDK_URL="https://${VCENTER_IP}/sdk"
fi
VC_HOST="$(printf '%s\n' "$VC_SDK_URL" | sed -E 's#^https?://([^/:]+).*#\1#')"

# Java often inherits HTTP/SOCKS proxies (shell env or macOS system config). Proxies usually cannot
# resolve private vCenter DNS → UnknownHostException. Clear common env vars and bypass proxies for this JVM.
unset http_proxy https_proxy HTTP_PROXY HTTPS_PROXY ALL_PROXY all_proxy \
  socks_proxy SOCKS_PROXY socksProxyHost socksProxyPort JAVA_TOOL_OPTIONS || true

VC_USER="${VC_USER:-administrator@vsphere.local}"
VC_PASSWORD="${VC_PASSWORD:?Set VC_PASSWORD}"
PLUGIN_KEY="${PLUGIN_KEY:-com.example.tanzu.platform.integration}"
PLUGIN_VERSION="${PLUGIN_VERSION:-1.0.0.0}"
PLUGIN_URL="${PLUGIN_URL:-https://192.168.70.27:8443/tanzu-hub-poc-ui/plugin.json}"
PLUGIN_SERVER_TP="${PLUGIN_SERVER_TP:?Set PLUGIN_SERVER_TP (SHA-1 thumbprint of plug-in HTTPS cert, colons)}"
PLUGIN_COMPANY="${PLUGIN_COMPANY:-Lab}"
PLUGIN_NAME="${PLUGIN_NAME:-Tanzu Platform Integration}"
PLUGIN_SUMMARY="${PLUGIN_SUMMARY:-Tanzu Hub install from vSphere Client}"

# Broadcom tool: "-insecure" is often not picked up by Commons CLI with the full option set.
# Patched ConnectionService.jar also honors:
#   EXTENSION_REGISTRATION_INSECURE=true
#   JAVA_OPTS=-Dextension.registration.insecure=true
export EXTENSION_REGISTRATION_INSECURE="${EXTENSION_REGISTRATION_INSECURE:-true}"
# Prefer IPv4; disable system proxy picker; send vCenter + lab suffixes direct (not via corporate proxy).
HTTP_NON_PROXY="${VC_NO_PROXY_HOSTS:-${VC_HOST}|*.cmaclabs.com|localhost|127.*|[::1]}"
# Empty proxyHost / socksProxyHost clears inherited JVM defaults; JAVA_TOOL_OPTIONS is unset above.
export JAVA_OPTS="${JAVA_OPTS:--Dextension.registration.insecure=true} -Djava.net.preferIPv4Stack=true -Djava.net.useSystemProxies=false -Dhttp.nonProxyHosts=${HTTP_NON_PROXY} -Dhttps.nonProxyHosts=${HTTP_NON_PROXY} -Dhttp.proxyHost= -Dhttp.proxyPort=0 -Dhttps.proxyHost= -Dhttps.proxyPort=0 -DsocksProxyHost= -DsocksProxyPort=0"

# vCenter host from SDK URL (optional DNS sanity check; skipped for literal IPv4 in URL)
if [[ "${SKIP_DNS_CHECK:-0}" != "1" ]]; then
  if command -v host >/dev/null 2>&1; then
    if [[ "$VC_HOST" =~ ^[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
      :
    elif ! host "$VC_HOST" >/dev/null 2>&1; then
      echo "ERROR: DNS did not resolve vCenter host \"$VC_HOST\" (from VC_SDK_URL)." >&2
      echo "  Registration will fail with UnknownHostException until this host resolves (VPN, /etc/hosts, or correct FQDN)." >&2
      echo "  Or use the IP: VCENTER_IP=<vCenter-IP> $0  (or VC_SDK_URL=https://<IP>/sdk)." >&2
      echo "  To skip this check: SKIP_DNS_CHECK=1 $0" >&2
      exit 1
    fi
  fi
fi

exec "$TOOL" \
  -action registerPlugin \
  -url "$VC_SDK_URL" \
  -u "$VC_USER" \
  -p "$VC_PASSWORD" \
  -k "$PLUGIN_KEY" \
  -v "$PLUGIN_VERSION" \
  -pu "$PLUGIN_URL" \
  -st "$PLUGIN_SERVER_TP" \
  -c "$PLUGIN_COMPANY" \
  -n "$PLUGIN_NAME" \
  -s "$PLUGIN_SUMMARY"
