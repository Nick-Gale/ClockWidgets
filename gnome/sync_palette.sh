#!/usr/bin/env bash
# Copies the canonical palette tokens into the extension source dir so the
# extension ships self-contained. Run after editing palette/tokens.json;
# gnome/test/palette-sync.test.mjs guards against drift.
set -euo pipefail
cd "$(dirname "$0")"
cp ../palette/tokens.json src/tokens.json
echo "synced palette/tokens.json -> gnome/src/tokens.json"
