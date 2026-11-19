# Thirds — behaviour specification

This is the canonical product specification. Every platform module
implements exactly this document; where a platform cannot, the deviation
is recorded in that module's README.

## 1. Concept

Thirds renders a 24-hour day as three equal 8-hour **periods**, and shows
the user's progress through the *current* period as a horizontal bar of
**8 sections** filling left to right. The widget contains **no numbers,
ever**.

## 2. Periods

Periods are defined in **local wall-clock time** — no configuration, no
timezone UI, no DST special-casing. A DST-shifted hour simply does not
appear on the wall clock; periods are keyed to wall-clock hour:minute.

| Period id | Wall-clock range | Colour scheme |
|-----------|------------------|---------------|
| `day`     | 09:00 – 16:59    | green         |
| `evening` | 17:00 – 00:59    | blue          |
| `night`   | 01:00 – 08:59    | purple        |

The `evening` period spans midnight: 17:00 Sunday belongs to the same
period as 00:30 Monday. Period membership is purely by wall clock.

## 3. Sections and progress

Each period has 8 sections; each section is **one hour** of that period.
Sections are indexed 0–7 left to right (section 0 of `day` is 09:00–10:00,
section 0 of `evening` is 17:00–18:00, section 0 of `night` is 01:00–02:00,
and so on).

Progress advances in **15-minute quarter-steps**, and all platforms tick
on the wall-clock quarter hours **:00, :15, :30, :45**. Within a period:

- `elapsed` = minutes since the period started (0–479).
- `quarters` = floor(elapsed / 15)  (0–31 — at 480 the period has ended).
- fully filled sections = floor(quarters / 4)
- current section = floor(quarters / 4) (clamped to 7)
- current section fill = (quarters mod 4) × 0.25

At 09:00 the bar is empty. At 16:45 it is 7 full sections and ¾ of the
last. At each period boundary the bar resets to empty and switches to the
next period's colour scheme.

The formal input/output contract and shared test vectors live in
[`core/spec.md`](../core/spec.md).

## 4. Visual design

### 4.1 Empty sections ("glass")

All 8 sections are always present. An empty section is a **colourless
glass pane**: a rounded pill with a low-alpha neutral body and a 1px
hairline highlight along its top edge, so it reads as glass on any
background:

- on dark bars/backgrounds: ~10% white body, ~25% white top hairline
- on light bars/backgrounds: ~7% black body, ~20% black top hairline

Empty glass carries **no period colour** — the scheme is revealed only by
filled sections.

### 4.2 Filled sections

A filled section is the same pill rendered in the period's colour from
`palette/tokens.json`, with a soft glassy sheen (subtle lighter top
third). Partial fills (¼, ½, ¾) extend from the **left edge** of the
current pill; the remainder of that pill stays glass.

### 4.3 Colour

Colours come only from `palette/tokens.json` (deep-pastel / dusty
palettes: sage green, slate blue, lilac). Tokens define per-scheme
colours for dark and light appearance; each platform maps appearance to
its own light/dark context.

### 4.4 Geometry

- 8 equal pills, small gaps between them (~1/8 of pill width).
- Bar aspect: long and low (≈ 8–10× pill height overall).
- Bar corners are not rounded beyond the pills themselves.

## 5. Platform requirements

### 5.1 Android (`android/`)
- Jetpack Glance App Widget; minSdk 26; default size 5×1 cells, width
  resizable 4–12, height locked to 1 row.
- Redraw scheduled with exact alarms aligned to :00/:15/:30/:45 local.
- Follows system dark/light mode via palette tokens.

### 5.2 GNOME (`gnome/`)
- GNOME Shell extension (gjs), targets GNOME 50.
- Status item inserted at the left edge of the right panel box (before
  the system indicator aggregate, i.e. closest to the centre clock).
- Timer aligned to the next quarter hour; re-renders only on change.
- Follows shell light/dark theme.

### 5.3 macOS (`macos/`)
- Swift (SPM, Command Line Tools only), LSUIElement agent app, no Dock
  icon. Full-colour custom-drawn NSStatusItem (not a template image).
- Left click: no action. Right click: menu with Quit.
- Distributed as a `.app` bundle (ad-hoc signed) built by a script, plus
  a LaunchAgent plist for login start. Deployment target macOS 14.
