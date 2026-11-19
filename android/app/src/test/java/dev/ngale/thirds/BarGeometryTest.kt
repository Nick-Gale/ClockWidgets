package dev.ngale.thirds

import org.junit.Assert.assertEquals
import org.junit.Test

class BarGeometryTest {
    @Test
    fun eightEqualPillsFitInWidth() {
        // 330dp at density 1: pad 4, gap 3 → 2*4 + 8*pillW + 7*3 = 330 → pillW ≈ 38
        val g = BarRenderer.computeGeometry(330, 40, 1f)
        assertEquals(4, g.pad)
        assertEquals(3, g.gap)
        assertEquals(18, g.pillH)
        assertEquals(37, g.pillW) // (330 - 8 - 21)/8 = 37.6 → 37
        val used = 2 * g.pad + 8 * g.pillW + 7 * g.gap
        assertEquals(true, used <= 330)
        assertEquals(true, used >= 330 - 8)
    }

    @Test
    fun fillFractionFollowsState() {
        // 12:45 → day, 15 quarters → 3 full, section 3 at 0.75
        val s = thirdState(12, 45)
        assertEquals(3, s.fullSections)
        assertEquals(listOf(1.0, 1.0, 1.0, 0.75, 0.0, 0.0, 0.0, 0.0),
            (0 until 8).map { BarRenderer.fillFraction(it, s) })
    }

    @Test
    fun fillFractionAtBoundary() {
        // exactly 17:00 → evening, 0 elapsed → all glass
        val s = thirdState(17, 0)
        assertEquals(0.0, BarRenderer.fillFraction(0, s), 1e-9)
        assertEquals(0.0, BarRenderer.fillFraction(7, s), 1e-9)
    }
}
