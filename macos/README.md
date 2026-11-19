# Thirds — macOS menu bar item

A menu-bar (status bar) item showing the current third of the day as
eight glass pills filling left to right — see [`../docs/spec.md`](../docs/spec.md).

Requires **macOS 14+** and the Xcode Command Line Tools (`xcode-select
--install`). No full Xcode needed.

## Test the model (shared golden vectors)

    swift test

## Build the .app and install with autostart

    ./build-app.sh      # produces dist/Thirds.app (ad-hoc signed)
    ./install.sh        # copies to ~/Applications, adds a LaunchAgent

The item appears top-right of the menu bar immediately and after every
login. Colours follow the system light/dark appearance.

## Uninstall

    launchctl remove dev.ngale.thirds
    rm ~/Library/LaunchAgents/dev.ngale.thirds.plist
    rm -rf ~/Applications/Thirds.app

## Notes

- Palette is generated from [`../palette/tokens.json`](../palette/tokens.json)
  by [`../scripts/sync_palette.py`](../scripts/sync_palette.py) — never edit
  `Sources/ThirdsCore/Palette.swift` by hand.
- The status item is ~142pt wide (8 pills). It does not resize; if that
  is too wide for a crowded menu bar, tune `Geo` in
  [`Sources/Thirds/main.swift`](Sources/Thirds/main.swift).
- Cannot be built on Linux (AppKit); build on the Mac.
