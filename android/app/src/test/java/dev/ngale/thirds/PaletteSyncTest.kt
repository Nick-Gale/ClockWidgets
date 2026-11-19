package dev.ngale.thirds

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

/**
 * Guards against drift: the generated Palette.kt must equal what
 * palette/tokens.json says. If this fails, run scripts/sync_palette.py.
 */
class PaletteSyncTest {
    @Test
    fun generatedPaletteMatchesTokens() {
        val tokensFile = File("../../palette/tokens.json")
        check(tokensFile.exists()) { "expected $tokensFile — run gradle from android/" }
        val tokens = Gson().fromJson(tokensFile.readText(), Map::class.java)

        val generated = File("src/main/gen/dev/ngale/thirds/Palette.kt").readText()

        @Suppress("UNCHECKED_CAST")
        val periods = tokens["periods"] as Map<String, Map<String, Map<String, String>>>
        for (p in listOf("day", "evening", "night")) {
            for (ap in listOf("dark", "light")) {
                val fill = periods.getValue(p).getValue(ap).getValue("fill")
                assertEquals("$p/$ap fill in Palette.kt", true,
                    generated.contains(fill.removePrefix("#").uppercase()))
            }
        }
    }
}
