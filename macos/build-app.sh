#!/usr/bin/env bash
# Build dist/Thirds.app from the SwiftPM executable (run on macOS).
set -euo pipefail
cd "$(dirname "$0")"

swift build -c release

APP=dist/Thirds.app
rm -rf "$APP"
mkdir -p "$APP/Contents/MacOS"
cp .build/release/Thirds "$APP/Contents/MacOS/Thirds"

cat > "$APP/Contents/Info.plist" <<'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>CFBundleIdentifier</key><string>dev.ngale.thirds</string>
    <key>CFBundleName</key><string>Thirds</string>
    <key>CFBundleExecutable</key><string>Thirds</string>
    <key>CFBundlePackageType</key><string>APPL</string>
    <key>CFBundleShortVersionString</key><string>0.1.0</string>
    <key>CFBundleVersion</key><string>1</string>
    <key>LSMinimumSystemVersion</key><string>14.0</string>
    <key>LSUIElement</key><true/>
</dict>
</plist>
EOF

# Ad-hoc signature: enough for Gatekeeper on a personally-built binary you
# copy to your own machine (no notarization).
codesign --force --sign - "$APP"
echo "Built $APP"
