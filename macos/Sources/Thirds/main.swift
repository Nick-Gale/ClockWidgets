import AppKit
import ThirdsCore

// Geometry in points; menu bar content height is ~22, keep the bar compact.
enum Geo {
    static let pillW: CGFloat = 16
    static let pillH: CGFloat = 12
    static let gap: CGFloat = 2
    static let inset: CGFloat = 1
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
        let body = NSBezierPath(roundedRect: pill, xRadius: Geo.pillH / 2, yRadius: Geo.pillH / 2)
        scheme.glass.body.setFill()
        body.fill()
        scheme.glass.hairline.setStroke()
        body.lineWidth = 1
        body.stroke()

        let frac = fillFraction(i, state)
        if frac > 0 {
            var inner = pill.insetBy(dx: Geo.inset, dy: Geo.inset)
            inner.size.width *= frac
            let r = min(Geo.pillH / 2 - Geo.inset, inner.width / 2)
            let fill = NSBezierPath(roundedRect: inner, xRadius: r, yRadius: r)
            period.fill.setFill()
            fill.fill()
        }
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
