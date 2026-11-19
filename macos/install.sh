#!/usr/bin/env bash
# Install Thirds.app into ~/Applications and autostart via LaunchAgent.
set -euo pipefail
cd "$(dirname "$0")"
[ -d dist/Thirds.app ] || ./build-app.sh

mkdir -p ~/Applications
rm -rf ~/Applications/Thirds.app
cp -R dist/Thirds.app ~/Applications/Thirds.app

LA="$HOME/Library/LaunchAgents/dev.ngale.thirds.plist"
mkdir -p "$HOME/Library/LaunchAgents"
cat > "$LA" <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>Label</key><string>dev.ngale.thirds</string>
    <key>ProgramArguments</key>
    <array><string>$HOME/Applications/Thirds.app/Contents/MacOS/Thirds</string></array>
    <key>RunAtLoad</key><true/>
    <key>KeepAlive</key><true/>
</dict>
</plist>
EOF

launchctl remove dev.ngale.thirds 2>/dev/null || true
launchctl load "$LA"
echo "Thirds should now appear in the menu bar (top right)."
