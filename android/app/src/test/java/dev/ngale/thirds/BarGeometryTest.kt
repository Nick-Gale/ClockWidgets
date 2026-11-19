package dev.ngale.thirds

import org.junit.Assert.assertEquals
import org.junit.Test

class BarGeometryTest {
    @Test
    fun barScalesUniformlyAndCentres() {
        // 330dp wide × 40dp tall at density 1: height-limited fit of the
        // natural bar (192.6×18 dp) → scale ≈ 1.67.
        val g = BarRenderer.computeGeometry(330, 40, 1f)
        assertEquals(4, g.pad)
        assertEquals(30, g.pillH)
        assertEquals(38, g.pillW)
        assertEquals(3, g.gap)
        assertEquals(9, g.radius)   // cornerRatio 0.30 × pillH
        assertEquals(0, g.inset)    // fillInsetRatio 0: fill = box shape
        assertEquals(9, g.fillRadius) // shares the box squareness
        val barW = 8 * g.pillW + 7 * g.gap
        assertEquals(325, barW)
        assertEquals(2, g.offX)     // centred: 325 + 2*2 = 329 ≤ 330
    }

    @Test
    fun pillAspectFollowsShapeToken() {
        val sh = Palette.SHAPE
        for (w in listOf(258, 330, 500, 834)) {
            val g = BarRenderer.computeGeometry(w, 40, 2.75f)
            val expectedW = g.pillH * sh.aspect
            assertEquals(true, kotlin.math.abs(g.pillW - expectedW) <= 1.5f)
        }
    }

    @Test
    fun barNeverOverflowsWidth() {
        for (w in listOf(120, 258, 330, 834)) {
            val g = BarRenderer.computeGeometry(w, 40, 1f)
            val used = 8 * g.pillW + 7 * g.gap + 2 * g.offX
            assertEquals(true, used in (w - 2)..w)
        }
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
