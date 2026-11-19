# Thirds

A clock, as a progress bar. The day is divided into three equal thirds;
Thirds shows your progress through the current third as 8 glass sections
that fill left to right. No numbers.

- 09:00–17:00 — green
- 17:00–01:00 — blue
- 01:00–09:00 — purple

Each section is one hour of the current third and fills in quarter-steps
every 15 minutes, on wall-clock :00/:15/:30/:45. At each boundary the bar
resets and adopts the next scheme.

## Modules

| Directory  | What it is                                                         |
|------------|--------------------------------------------------------------------|
| `docs/`    | The product specification every module implements.                 |
| `core/`    | Canonical time model: spec + golden test vectors + JS reference.   |
| `palette/` | Colour tokens (single source of truth) + rendered preview images.  |
| `gnome/`   | GNOME Shell extension (top bar, GNOME 50).                         |
| `android/` | Home-screen widget (Jetpack Glance).                               |
| `macos/`   | Menu bar status item (Swift, LSUIElement agent).                   |

`core/` and `palette/` are the shared contracts: each platform
reimplements the ~40-line time model in its own language and its tests
run against the same golden vectors in `core/fixtures/`.

See `docs/spec.md` for the full specification.
