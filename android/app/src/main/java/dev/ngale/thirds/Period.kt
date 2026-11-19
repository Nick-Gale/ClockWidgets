package dev.ngale.thirds

/**
 * Thirds time model — Android reimplementation.
 * Pure local wall-clock arithmetic; must match core/spec.md and pass
 * core/fixtures/vectors.json (see PeriodTest).
 */
data class ThirdsState(
    val period: String,
    val quarters: Int,
    val fullSections: Int,
    val section: Int,
    val fill: Double,
)

fun thirdState(hour: Int, minute: Int): ThirdsState {
    val period = when {
        hour in 9..16 -> "day"
        hour >= 17 || hour < 1 -> "evening"
        else -> "night"
    }
    val start = when (period) {
        "day" -> 9
        "evening" -> 17
        else -> 1
    }
    val elapsed = ((hour - start + 24) % 24) * 60 + minute // 0..479
    val quarters = elapsed / 15
    val fullSections = quarters / 4
    return ThirdsState(
        period = period,
        quarters = quarters,
        fullSections = fullSections,
        section = minOf(fullSections, 7),
        fill = (quarters % 4) * 0.25,
    )
}
