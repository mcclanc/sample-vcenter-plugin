#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

if ! command -v mkcert >/dev/null 2>&1; then
  echo "mkcert not found. Install with: brew install mkcert" >&2
  exit 1
fi

mkdir -p certs
mkcert -key-file certs/dev-key.pem -cert-file certs/dev-cert.pem localhost 127.0.0.1 ::1

echo ""
echo "Wrote certs/dev-key.pem and certs/dev-cert.pem"
echo "HTTPS dev server:"
echo "  npm run start:https"
echo ""
echo "If browsers show untrusted CA, run once in Terminal (password prompt):"
echo "  mkcert -install"
