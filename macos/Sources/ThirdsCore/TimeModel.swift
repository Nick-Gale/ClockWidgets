import Foundation

/// Thirds time model — macOS reimplementation.
/// Pure local wall-clock arithmetic; must match core/spec.md and pass
/// core/fixtures/vectors.json (see TimeModelTests).
public struct ThirdsState: Equatable {
    public let period: String
    public let quarters: Int
    public let fullSections: Int
    public let section: Int
    public let fill: Double
}

public func thirdState(hour: Int, minute: Int) -> ThirdsState {
    let period: String
    let start: Int
    if (9...16).contains(hour) { period = "day"; start = 9 }
    else if hour >= 17 || hour < 1 { period = "evening"; start = 17 }
    else { period = "night"; start = 1 }
    let elapsed = ((hour - start + 24) % 24) * 60 + minute // 0..479
    let quarters = elapsed / 15
    let fullSections = quarters / 4
    return ThirdsState(
        period: period,
        quarters: quarters,
        fullSections: fullSections,
        section: min(fullSections, 7),
        fill: Double(quarters % 4) * 0.25
    )
}

/// Fill fraction 0…1 for section i given a state (shared across platforms).
public func fillFraction(_ i: Int, _ s: ThirdsState) -> Double {
    if i < s.fullSections { return 1 }
    if i == s.section { return s.fill }
    return 0
}
