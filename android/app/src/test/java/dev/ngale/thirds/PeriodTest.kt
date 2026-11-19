package dev.ngale.thirds

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

/** Runs the canonical shared vectors (core/fixtures/vectors.json). */
class PeriodTest {
    data class Vector(
        val t: String,
        val period: String,
        val quarters: Int,
        val fullSections: Int,
        val section: Int,
        val fill: Double,
    )

    @Test
    fun sharedVectorsAllMatch() {
        val path = File("../../core/fixtures/vectors.json")
        check(path.exists()) { "expected $path — run gradle from android/" }
        val root = Gson().fromJson(path.readText(), Map::class.java)
        val vectors: List<Vector> =
            Gson().fromJson(
                Gson().toJson(root["vectors"]),
                object : TypeToken<List<Vector>>() {}.type,
            )
        assertEquals("vectors found", 23, vectors.size)
        for (v in vectors) {
            val (h, m) = v.t.split(":").map { it.toInt() }
            val s = thirdState(h, m)
            assertEquals("period@${v.t}", v.period, s.period)
            assertEquals("quarters@${v.t}", v.quarters, s.quarters)
            assertEquals("full@${v.t}", v.fullSections, s.fullSections)
            assertEquals("section@${v.t}", v.section, s.section)
            assertEquals("fill@${v.t}", v.fill, s.fill, 1e-9)
        }
    }
}
