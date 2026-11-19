#!/usr/bin/env bash
# Live-test the Thirds extension on a real (headless) GNOME Shell without
# touching the user's session: starts a private gnome-shell, unlocks it,
# captures the stage via the extension's THIRDS_DEBUG_SHOT hook, saves a
# PNG (default /tmp/thirds-gnome-live.png).
#
# Requires: gnome-shell, dbus-run-session (same machine as dev target).
set -u
SHOT="${1:-/tmp/thirds-gnome-live.png}"
UUID="thirds@ngale.dev"
SRC="$(cd "$(dirname "$0")" && pwd)/src"

rm -f "$SHOT" /tmp/thirds-gs.log
mkdir -p "$HOME/.local/share/gnome-shell/extensions/$UUID"
cp -r "$SRC/." "$HOME/.local/share/gnome-shell/extensions/$UUID/"

# ensure the extension is enabled for the headless shell's lookup
python3 - "$UUID" <<'PY'
import json, subprocess, sys
uuid = sys.argv[1]
out = subprocess.run(['gsettings', 'get', 'org.gnome.shell', 'enabled-extensions'],
                     capture_output=True, text=True).stdout.strip()
try:
    cur = json.loads(out.replace("'", '"'))
except Exception:
    cur = []
if uuid not in cur:
    cur.append(uuid)
    subprocess.run(['gsettings', 'set', 'org.gnome.shell', 'enabled-extensions',
                    json.dumps(cur).replace('"', "'")], check=True)
PY

export XDG_CURRENT_DESKTOP=ubuntu:GNOME XDG_SESSION_TYPE=wayland WAYLAND_DISPLAY= THIRDS_DEBUG_SHOT="$SHOT"
setsid dbus-run-session -- bash -c '
  export XDG_RUNTIME_DIR=/tmp/thirds-runtime HOME='"$HOME"'
  rm -rf "$XDG_RUNTIME_DIR"; mkdir -p "$XDG_RUNTIME_DIR"; chmod 700 "$XDG_RUNTIME_DIR"
  gnome-shell --wayland --headless --virtual-monitor 1920x1080 > /tmp/thirds-gs.log 2>&1 &
  gs=$!
  sleep 12
  kill $gs 2>/dev/null; sleep 1; kill -9 $gs 2>/dev/null
  exit 0
' > /dev/null 2>&1 &
launcher=$!
sleep 20
kill -- -"$launcher" 2>/dev/null
sleep 1

if [ -f "$SHOT" ]; then
  echo "LIVE TEST OK: $SHOT"
else
  echo "LIVE TEST FAILED — shell log:"
  grep -iE "thirds|JS ERROR|CRITICAL" /tmp/thirds-gs.log | grep -viE "ding|rastersoft|SessionManager" | head -10
  exit 1
fi
grep -i "thirds" /tmp/thirds-gs.log | grep -v "shot written" | head -5 || true
