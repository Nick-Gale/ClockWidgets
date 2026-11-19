import AppKit
import ThirdsCore

// Geometry in points, derived from the shared shape tokens. Menu bar
// content height is ~14–22 pt, so the base pill stays small and the
// ratios (aspect, gap, corner, inset) come from Palette.shape.
enum Geo {
    static let pillH: CGFloat = 12
    static let pillW: CGFloat = (pillH * Palette.shape.aspect).rounded()
    static let gap: CGFloat = max(1, (pillW * Palette.shape.gapRatio).rounded())
    static let inset: CGFloat = (pillH * Palette.shape.fillInsetRatio).rounded()
    static let radius: CGFloat = pillH * Palette.shape.cornerRatio
    // Fill shares the box's squareness: same cornerRatio on its own height.
    static let fillRadius: CGFloat = (pillH - 2 * inset) * Palette.shape.cornerRatio
    static let barW: CGFloat = 8 * pillW + 7 * gap
    static let barH: CGFloat = 14
}

func periodColors(_ period: String, _ scheme: Palette) -> Palette.Period {
    switch period {
    case "day": return scheme.day
    case "evening": return scheme.evening
    default: return scheme.night
    }
}

func renderBar(state: ThirdsState, scheme: Palette) -> NSImage {
    let size = NSSize(width: Geo.barW, height: Geo.barH)
    let image = NSImage(size: size)
    image.lockFocus()

    let period = periodColors(state.period, scheme)
    let top = (Geo.barH - Geo.pillH) / 2
    for i in 0..<8 {
        let x = CGFloat(i) * (Geo.pillW + Geo.gap)
        let pill = NSRect(x: x, y: top, width: Geo.pillW, height: Geo.pillH)

        NSGraphicsContext.current?.shouldAntialias = true
        let body = NSBezierPath(roundedRect: pill, xRadius: Geo.radius, yRadius: Geo.radius)
        scheme.glass.body.setFill()
        body.fill()

        // The fill is the box shape itself, growing left→right.
        let frac = fillFraction(i, state)
        if frac > 0 {
            var inner = pill
            inner.size.width *= frac
            let r = min(Geo.fillRadius, inner.width / 2)
            let fillPath = NSBezierPath(roundedRect: inner, xRadius: r, yRadius: r)
            // Vertical sheen→fill gradient gives the fill its glass depth.
            NSGradient(starting: period.sheen, ending: period.fill)?
                .draw(in: fillPath, angle: -90)
        }

        // Specular streak across the top inside every pill (glass cue).
        let sp = Geo.radius / 2
        let specH = (Geo.pillH - 2 * sp) / 3
        let spec = NSBezierPath(
            roundedRect: NSRect(x: pill.minX + sp, y: pill.maxY - sp - specH,
                                width: pill.width - 2 * sp, height: specH),
            xRadius: specH / 2, yRadius: specH / 2)
        NSColor(calibratedWhite: 1, alpha: 0.09).setFill()
        spec.fill()

        // Hairline rim last: the glass edge stays crisp over any fill.
        scheme.glass.hairline.setStroke()
        body.lineWidth = 1
        body.stroke()
    }

    image.unlockFocus()
    image.isTemplate = false
    return image
}

final class AppDelegate: NSObject, NSApplicationDelegate {
    private var statusItem: NSStatusItem!
    private var timer: Timer?

    func applicationDidFinishLaunching(_ note: Notification) {
        statusItem = NSStatusBar.system.statusItem(withLength: Geo.barW)
        update()

        // Theme switches need an immediate redraw.
        DistributedNotificationCenter.default().addObserver(
            self, selector: #selector(update),
            name: NSNotification.Name("AppleInterfaceThemeChangedNotification"),
            object: nil, suspensionBehavior: .deliverImmediately)

        scheduleNextQuarter()
    }

    @objc private func update() {
        let now = Calendar.current.dateComponents([.hour, .minute], from: Date())
        let state = thirdState(hour: now.hour!, minute: now.minute!)
        let dark = (statusItem.button?.effectiveAppearance
            ?? NSApp.effectiveAppearance)
            .bestMatch(from: [.aqua, .darkAqua]) == .darkAqua
        statusItem.button?.image = renderBar(state: state, scheme: dark ? Palette.dark : Palette.light)
    }

    /// One-shot timer to the next :00/:15/:30/:45 boundary, then re-arm —
    /// same quarter-aligned pattern as the GNOME ticker.
    private func scheduleNextQuarter() {
        let now = Date()
        let c = Calendar.current.dateComponents([.hour, .minute, .second], from: now)
        let secsOfDay = c.hour! * 3600 + c.minute! * 60 + c.second!
        let delta = 900 - (secsOfDay % 900) + 1 // fire 1s after the boundary
        timer = Timer(timeInterval: TimeInterval(delta), repeats: false) { [weak self] _ in
            self?.update()
            self?.scheduleNextQuarter()
        }
        RunLoop.main.add(timer!, forMode: .common)
    }
}

let app = NSApplication.shared
let delegate = AppDelegate()
app.delegate = delegate
app.setActivationPolicy(.accessory) // LSUIElement also set in Info.plist
app.run()
