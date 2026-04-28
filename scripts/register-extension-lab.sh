#!/usr/bin/env bash
# Lab helper: register this repo's remote plug-in with vCenter using Broadcom's extension-registration tool.
# Run from a host that resolves your vCenter FQDN and can reach the plug-in HTTPS URL.
#
# On Windows / Windows Server, use:  powershell -File scripts/register-extension-lab.ps1
#   (or:  npm run register:lab  from the repo root)
#
# Interactive (default on a terminal): prompts for values; press Enter to keep [bracketed] defaults.
# Non-interactive: set REGISTER_NON_INTERACTIVE=1 and export VC_PASSWORD, PLUGIN_SERVER_TP (and overrides as needed).
# Re-register / refresh an existing extension: REGISTER_ACTION=updatePlugin (same PLUGIN_KEY as the first registration).
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

# vCenter SDK URL (env or default; VCENTER_IP forces https://IP/sdk)
VC_SDK_URL="${VC_SDK_URL:-https://vcenter.cmaclabs.com/sdk}"
if [[ -n "${VCENTER_IP:-}" ]]; then
  VC_SDK_URL="https://${VCENTER_IP}/sdk"
fi

# Defaults (overridden by env or prompts)
DEFAULT_VC_USER="${VC_USER:-administrator@vsphere.local}"
# Reverse-DNS, VMware-style "com.<org>.remote.<product>" (see SDK sample com.vmware.sample.remote).
# Avoid com.example.* and stale PLUGIN_KEY in your shell from older runs.
DEFAULT_PLUGIN_KEY="${PLUGIN_KEY:-com.cmaclabs.remote.paasforvcf}"
DEFAULT_PLUGIN_VERSION="${PLUGIN_VERSION:-1.0.0.0}"
DEFAULT_PLUGIN_URL="${PLUGIN_URL:-https://192.168.70.27:8443/tanzu-hub-poc-ui/plugin.json}"
DEFAULT_PLUGIN_COMPANY="${PLUGIN_COMPANY:-Lab}"
DEFAULT_PLUGIN_NAME="${PLUGIN_NAME:-App Platform as a Service}"
DEFAULT_PLUGIN_SUMMARY="${PLUGIN_SUMMARY:-Tanzu Hub install from vSphere Client}"

read_with_default() {
  local prompt="$1"
  local def="$2"
  local var_name="$3"
  local input=""
  # shellcheck disable=SC2162
  read -r -p "$prompt [${def}]: " input || true
  if [[ -z "${input}" ]]; then
    printf -v "$var_name" '%s' "$def"
  else
    printf -v "$var_name" '%s' "$input"
  fi
}

read_password() {
  local prompt="$1"
  local var_name="$2"
  local input=""
  if [[ -n "${!var_name:-}" ]]; then
    return
  fi
  # shellcheck disable=SC2162
  read -rs -p "$prompt: " input || true
  echo ""
  if [[ -z "${input}" ]]; then
    echo "ERROR: Password cannot be empty." >&2
    exit 1
  fi
  printf -v "$var_name" '%s' "$input"
}

read_required() {
  local prompt="$1"
  local var_name="$2"
  local input=""
  if [[ -n "${!var_name:-}" ]]; then
    return
  fi
  # shellcheck disable=SC2162
  read -r -p "$prompt: " input || true
  if [[ -z "${input}" ]]; then
    echo "ERROR: This value is required (or set ${var_name} in the environment)." >&2
    exit 1
  fi
  printf -v "$var_name" '%s' "$input"
}

if [[ -t 0 ]] && [[ "${REGISTER_NON_INTERACTIVE:-}" != "1" ]]; then
  echo "=== Register remote plug-in with vCenter ===" >&2
  echo "Press Enter at each prompt to accept the default in [brackets]." >&2
  echo "" >&2

  read_with_default "1) vCenter SDK URL (https://vcenter/sdk or https://IP/sdk)" "$VC_SDK_URL" VC_SDK_URL

  if [[ -z "${VCENTER_IP:-}" ]]; then
    read_with_default "   Optional: vCenter IP only if DNS fails (overrides URL host; leave empty to skip)" "" _VC_IP_RAW
    if [[ -n "${_VC_IP_RAW}" ]]; then
      VC_SDK_URL="https://${_VC_IP_RAW}/sdk"
      echo "   Using ${VC_SDK_URL}" >&2
    fi
  else
    VC_SDK_URL="https://${VCENTER_IP}/sdk"
    echo "   (VCENTER_IP is set → using ${VC_SDK_URL})" >&2
  fi

  VC_USER="$DEFAULT_VC_USER"
  read_with_default "2) vCenter SSO username" "$VC_USER" VC_USER

  if [[ -z "${VC_PASSWORD:-}" ]]; then
    read_password "3) vCenter SSO password" VC_PASSWORD
  else
    echo "3) vCenter SSO password: (using existing \$VC_PASSWORD)" >&2
  fi

  PLUGIN_URL="$DEFAULT_PLUGIN_URL"
  read_with_default "4) Plug-in manifest URL (must end with .../plugin.json)" "$PLUGIN_URL" PLUGIN_URL

  if [[ -z "${PLUGIN_SERVER_TP:-}" ]]; then
    echo "" >&2
    echo "5) Plug-in server HTTPS cert SHA-1 thumbprint (with colons), e.g. AA:BB:…" >&2
    echo "   Tip: openssl s_client -connect HOST:8443 </dev/null 2>/dev/null | openssl x509 -fingerprint -sha1 -noout" >&2
    read_required "   Thumbprint (-st)" PLUGIN_SERVER_TP
  else
    echo "5) Plug-in server thumbprint: (using existing \$PLUGIN_SERVER_TP)" >&2
  fi

  PLUGIN_KEY="$DEFAULT_PLUGIN_KEY"
  read_with_default "6) Extension key (-k); unique reverse-DNS; avoid com.example.*" "$PLUGIN_KEY" PLUGIN_KEY

  PLUGIN_VERSION="$DEFAULT_PLUGIN_VERSION"
  read_with_default "7) Plug-in version (-v); bump for re-register / refresh" "$PLUGIN_VERSION" PLUGIN_VERSION

  PLUGIN_NAME="$DEFAULT_PLUGIN_NAME"
  read_with_default "8) Display name in vSphere Client" "$PLUGIN_NAME" PLUGIN_NAME

  PLUGIN_COMPANY="$DEFAULT_PLUGIN_COMPANY"
  read_with_default "9) Company / publisher (-c)" "$PLUGIN_COMPANY" PLUGIN_COMPANY

  PLUGIN_SUMMARY="$DEFAULT_PLUGIN_SUMMARY"
  read_with_default "10) Short summary (-s)" "$PLUGIN_SUMMARY" PLUGIN_SUMMARY

  echo "" >&2
else
  VC_USER="${VC_USER:-administrator@vsphere.local}"
  VC_PASSWORD="${VC_PASSWORD:?Set VC_PASSWORD or run this script on a terminal for prompts.}"
  PLUGIN_KEY="${PLUGIN_KEY:-com.cmaclabs.remote.paasforvcf}"
  PLUGIN_VERSION="${PLUGIN_VERSION:-1.0.0.0}"
  PLUGIN_URL="${PLUGIN_URL:-https://192.168.70.27:8443/tanzu-hub-poc-ui/plugin.json}"
  PLUGIN_SERVER_TP="${PLUGIN_SERVER_TP:?Set PLUGIN_SERVER_TP or run this script on a terminal for prompts.}"
  PLUGIN_COMPANY="${PLUGIN_COMPANY:-Lab}"
  PLUGIN_NAME="${PLUGIN_NAME:-App Platform as a Service}"
  PLUGIN_SUMMARY="${PLUGIN_SUMMARY:-Tanzu Hub install from vSphere Client}"
fi

# Strip accidental whitespace from CLI-derived values (common cause of invalid key).
PLUGIN_KEY="${PLUGIN_KEY//[[:space:]]/}"
PLUGIN_VERSION="${PLUGIN_VERSION//[[:space:]]/}"
PLUGIN_SERVER_TP="${PLUGIN_SERVER_TP//[[:space:]]/}"

# vCenter typically expects an all-lowercase reverse-DNS key (mixed case often → extension.key fault).
PLUGIN_KEY="$(printf '%s' "$PLUGIN_KEY" | tr '[:upper:]' '[:lower:]')"

validate_plugin_key() {
  local k="$1"
  if [[ -z "$k" ]]; then
    echo "ERROR: PLUGIN_KEY is empty after trimming." >&2
    exit 1
  fi
  if ((${#k} > 127)); then
    echo "ERROR: PLUGIN_KEY is longer than 127 characters (${#k}). Use a shorter reverse-DNS key." >&2
    exit 1
  fi
  if [[ "$k" == *..* ]]; then
    echo "ERROR: PLUGIN_KEY must not contain consecutive dots. Got: $k" >&2
    exit 1
  fi
  if ! [[ "$k" =~ ^[a-z][a-z0-9]*(\.[a-z0-9]+)+$ ]]; then
    echo "ERROR: PLUGIN_KEY must look like reverse-DNS: only a-z, 0-9, dots; start with a letter;" >&2
    echo "       each segment non-empty; no leading/trailing/double dots. Got: $k" >&2
    echo "       Example: com.yourcompany.remote.paasforvcf" >&2
    exit 1
  fi
  if [[ "$k" == com.example* ]]; then
    echo "ERROR: com.example.* is rejected by many vCenter builds. Pick a key you own, e.g. com.yourorg.remote.paasforvcf" >&2
    exit 1
  fi
}

validate_plugin_key "$PLUGIN_KEY"

VC_HOST="$(printf '%s\n' "$VC_SDK_URL" | sed -E 's#^https?://([^/:]+).*#\1#')"

# Java often inherits HTTP/SOCKS proxies (shell env or macOS system config). Proxies usually cannot
# resolve private vCenter DNS → UnknownHostException. Clear common env vars and bypass proxies for this JVM.
unset http_proxy https_proxy HTTP_PROXY HTTPS_PROXY ALL_PROXY all_proxy \
  socks_proxy SOCKS_PROXY socksProxyHost socksProxyPort JAVA_TOOL_OPTIONS || true

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
      echo "  Or re-run and enter the vCenter IP when prompted (or VC_SDK_URL=https://<IP>/sdk)." >&2
      echo "  To skip this check: SKIP_DNS_CHECK=1 $0" >&2
      exit 1
    fi
  fi
fi

REGISTER_ACTION="${REGISTER_ACTION:-registerPlugin}"
case "$REGISTER_ACTION" in
  registerPlugin | updatePlugin) ;;
  *)
    echo "ERROR: REGISTER_ACTION must be registerPlugin or updatePlugin (got: $REGISTER_ACTION)." >&2
    exit 1
    ;;
esac

if [[ -t 0 ]] && [[ "${REGISTER_NON_INTERACTIVE:-}" != "1" ]]; then
  echo "Registering with:" >&2
  echo "  VC_SDK_URL=$VC_SDK_URL" >&2
  echo "  VC_USER=$VC_USER" >&2
  echo "  PLUGIN_URL=$PLUGIN_URL" >&2
  echo "  PLUGIN_KEY=$PLUGIN_KEY PLUGIN_VERSION=$PLUGIN_VERSION" >&2
  echo "  PLUGIN_NAME=$PLUGIN_NAME" >&2
  echo "  -action $REGISTER_ACTION" >&2
  echo "" >&2
fi

# Use -username / -password / -pluginUrl / … (Broadcom sample style). Avoid -p immediately before -pu:
# some Commons CLI builds treat "-pu" as "-p" with a bogus value, which can corrupt -k and yield
# SOAP Fault "extension.key" from vCenter.
if [[ "${REGISTER_DEBUG:-0}" == "1" ]]; then
  printf 'DEBUG argv to Java tool:\n' >&2
  printf '  -action %q\n' "$REGISTER_ACTION" >&2
  printf '  -url %q\n' "$VC_SDK_URL" >&2
  printf '  -username %q\n' "$VC_USER" >&2
  printf '  -password %q\n' "(hidden ${#VC_PASSWORD} chars)" >&2
  printf '  -key %q\n' "$PLUGIN_KEY" >&2
  printf '  -version %q\n' "$PLUGIN_VERSION" >&2
  printf '  -pluginUrl %q\n' "$PLUGIN_URL" >&2
  printf '  -serverThumbprint %q\n' "$PLUGIN_SERVER_TP" >&2
  printf '  -company %q\n' "$PLUGIN_COMPANY" >&2
  printf '  -name %q\n' "$PLUGIN_NAME" >&2
  printf '  -summary %q\n' "$PLUGIN_SUMMARY" >&2
  echo "" >&2
fi

exec "$TOOL" \
  -action "$REGISTER_ACTION" \
  -url "$VC_SDK_URL" \
  -username "$VC_USER" \
  -password "$VC_PASSWORD" \
  -key "$PLUGIN_KEY" \
  -version "$PLUGIN_VERSION" \
  -pluginUrl "$PLUGIN_URL" \
  -serverThumbprint "$PLUGIN_SERVER_TP" \
  -company "$PLUGIN_COMPANY" \
  -name "$PLUGIN_NAME" \
  -summary "$PLUGIN_SUMMARY"
