#!/usr/bin/env bash
# Install VMware OVF Tool for macOS into tools/vendor/ovftool/ovftool (repo-local).
#
# Alternatively: copy the `ovftool` binary to tools/ovftool at the repo root — the plug-in
# server resolves that path automatically (no unzip needed).
#
# Broadcom does not publish a public unauthenticated URL for the Mac zip; downloads
# require a Broadcom account. Use either:
#
#   1) OVFTOOL_MAC_ZIP=/path/to/zip-you-downloaded.zip  bash tools/download-ovftool-mac.sh
#   2) OVFTOOL_MAC_URL='https://…signed-or-authenticated-url…'  bash tools/download-ovftool-mac.sh
#
# Get the zip from: https://developer.broadcom.com/tools/open-virtualization-format-ovf-tool/4.6.3
# (or 4.6.0) → "Zip of OVF Tool MacOS 64-bit" — Login Required.
#
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DEST="${REPO_ROOT}/tools/vendor/ovftool"
STAGING="$(mktemp -d "${TMPDIR:-/tmp}/ovftool-staging.XXXXXX")"
cleanup() { rm -rf "${STAGING}"; }
trap cleanup EXIT

ZIP_LOCAL="${OVFTOOL_MAC_ZIP:-}"
ZIP_URL="${OVFTOOL_MAC_URL:-}"

if [[ -n "${ZIP_LOCAL}" ]]; then
  if [[ ! -f "${ZIP_LOCAL}" ]]; then
    echo "error: OVFTOOL_MAC_ZIP is not a file: ${ZIP_LOCAL}" >&2
    exit 1
  fi
  echo "Using local zip: ${ZIP_LOCAL}"
  cp "${ZIP_LOCAL}" "${STAGING}/ovftool.zip"
elif [[ -n "${ZIP_URL}" ]]; then
  echo "Downloading from OVFTOOL_MAC_URL …"
  if ! curl -fL --progress-bar -o "${STAGING}/ovftool.zip" "${ZIP_URL}"; then
    echo "error: download failed (HTTP or curl). For Broadcom you usually need a session or signed URL." >&2
    exit 1
  fi
else
  echo "Broadcom OVF Tool for Mac is not redistributable from this script without your own download."
  echo ""
  echo "  1. Log in at https://developer.broadcom.com/tools/open-virtualization-format-ovf-tool/4.6.3"
  echo "  2. Download: \"Zip of OVF Tool MacOS 64-bit\" (recommended for Apple Silicon + Intel via Rosetta)."
  echo "  3. Run:"
  echo "       OVFTOOL_MAC_ZIP=~/Downloads/<that-zip>.zip bash tools/download-ovftool-mac.sh"
  echo ""
  echo "Or set OVFTOOL_MAC_URL to a direct https link (e.g. time-limited) and re-run this script."
  if [[ "$(uname -s)" == "Darwin" ]]; then
    open "https://developer.broadcom.com/tools/open-virtualization-format-ovf-tool/4.6.3" 2>/dev/null || true
  fi
  exit 1
fi

if ! unzip -l "${STAGING}/ovftool.zip" >/dev/null 2>&1; then
  echo "error: ${STAGING}/ovftool.zip is not a valid zip (wrong URL or HTML login page saved as file)." >&2
  exit 1
fi

unzip -q -o "${STAGING}/ovftool.zip" -d "${STAGING}/extracted"

# Locate the macOS ovftool binary inside the archive (layout varies by release).
BIN="$(find "${STAGING}/extracted" -type f \( -name ovftool -o -name ovftool.bin \) 2>/dev/null | head -n1 || true)"
if [[ -z "${BIN}" ]]; then
  echo "error: could not find ovftool inside the zip. List archive contents and adjust this script if needed." >&2
  find "${STAGING}/extracted" -maxdepth 4 -type f 2>/dev/null | head -40 >&2 || true
  exit 1
fi

chmod +x "${BIN}"
rm -rf "${DEST}"
mkdir -p "${DEST}"
cp "${BIN}" "${DEST}/ovftool"
chmod +x "${DEST}/ovftool"

echo ""
echo "Installed OVF Tool → ${DEST}/ovftool"
"${DEST}/ovftool" --version | head -5 || true
echo ""
echo "The dev server (server/eval-appliance/ovftool.mjs) checks this path automatically after install."
