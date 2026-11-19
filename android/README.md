# Thirds — Android home-screen widget

Eight glass pills showing the current third of the day, filling left to
right — see [`../docs/spec.md`](../docs/spec.md).

Default **5×1** cells; drag to resize horizontally between 4 and 12
cells (height stays one row). Min Android **8.0 (API 26)**.

## Build

    ./gradlew :app:assembleDebug      # JDK 17+; SDK via local.properties or ANDROID_HOME
    ./gradlew :app:testDebugUnitTest  # model + geometry + palette-drift tests

APK: `app/build/outputs/apk/debug/app-debug.apk`.

## Install (sideload)

Copy the APK to the phone (any way you like — USB, network share) and
open it; allow "install unknown apps" for that source once. Then add
the **Thirds** widget from your launcher's widget picker. There is no
app icon — the widget is the whole product.

## Refresh behaviour

The host's own update minimum is 30 min, so the app schedules its own
wall-clock alarm on :00/:15/:30/:45 and redraws itself. Under Doze,
alarms can lag; the bar also redraws whenever the widget is resized or
the host refreshes it.

## Colours

Generated from [`../palette/tokens.json`](../palette/tokens.json) by
[`../scripts/sync_palette.py`](../scripts/sync_palette.py); the widget
follows system light/dark. Never edit `src/main/gen/**` or the generated
`res/drawable/fill_*.xml`, `glass_pill.xml` by hand — run the script.
