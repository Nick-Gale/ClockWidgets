#!/usr/bin/env bash
# Install/update Thirds into the running GNOME Shell and enable it.
set -euo pipefail
cd "$(dirname "$0")"

UUID="thirds@ngale.dev"
DEST="$HOME/.local/share/gnome-shell/extensions/$UUID"

./sync_palette.sh
mkdir -p "$DEST"
cp -r src/. "$DEST/"

gnome-extensions disable "$UUID" 2>/dev/null || true
gnome-extensions enable "$UUID"
echo "installed and enabled $UUID"
