package dev.ngale.thirds

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Draws the eight-pill bar for one exact widget-instance size. Geometry is
 * pure ints (unit-testable); rendering is the thin part.
 *
 * Shape comes from Palette.SHAPE (tokens.json): pills keep the shared
 * aspect, gap and corner ratios, and the whole bar scales uniformly to fit
 * the instance, centred horizontally. Wide widgets get a bigger bar, not
 * stretched pills.
 */
object BarRenderer {

    data class Geometry(
        val pad: Int,
        val gap: Int,
        val pillW: Int,
        val pillH: Int,
        val radius: Int,
        val inset: Int,
        val offX: Int,
    )

    /** Uniform fit: 8*pillW + 7*gap == widthPx - 2*offX (± rounding). */
    fun computeGeometry(widthPx: Int, heightPx: Int, density: Float): Geometry {
        val sh = Palette.SHAPE
        val aspect = sh.aspect.toFloat()
        val gapRatio = sh.gapRatio.toFloat()
        val cornerRatio = sh.cornerRatio.toFloat()
        val fillInsetRatio = sh.fillInsetRatio.toFloat()
        fun dp(v: Float) = (v * density).roundToInt()
        val pad = dp(4f)
        val natH = 18f * density                       // base pill height (v1 scale)
        val natPillW = natH * aspect
        val natGap = natPillW * gapRatio
        val natBarW = 8 * natPillW + 7 * natGap
        val availW = (widthPx - 2 * pad).coerceAtLeast(1).toFloat()
        val availH = (heightPx - 2 * pad).coerceAtLeast(1).toFloat()
        val scale = min(availW / natBarW, availH / natH)
        // nat* values are already in px (natH carries density), so the
        // scaled results round straight to px — no dp() here.
        val pillW = (natPillW * scale).roundToInt().coerceAtLeast(4)
        val pillH = (natH * scale).roundToInt().coerceAtLeast(4)
        val gap = (natGap * scale).roundToInt().coerceAtLeast(1)
        val radius = (cornerRatio * pillH).roundToInt()   // ratios of px pillH
        val inset = (fillInsetRatio * pillH).roundToInt()
        val barW = 8 * pillW + 7 * gap
        return Geometry(pad, gap, pillW, min(pillH, heightPx), radius, inset,
            (widthPx - barW) / 2)
    }

    /** Fill fraction 0..1 for pill i given the time state. */
    fun fillFraction(i: Int, state: ThirdsState): Double = when {
        i < state.fullSections -> 1.0
        i == state.section -> state.fill
        else -> 0.0
    }

    fun render(context: Context, state: ThirdsState, widthPx: Int, heightPx: Int): Bitmap {
        val density = context.resources.displayMetrics.density
        val dark = context.resources.configuration
            .uiMode.and(Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val scheme = if (dark) Palette.DARK else Palette.LIGHT
        val period = when (state.period) {
            "day" -> scheme.day
            "evening" -> scheme.evening
            else -> scheme.night
        }
        val g = computeGeometry(widthPx, heightPx, density)

        val bmp = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val top = (heightPx - g.pillH) / 2f
        val bottom = top + g.pillH

        for (i in 0 until 8) {
            val left = g.offX + i * (g.pillW + g.gap).toFloat()
            val right = left + g.pillW
            val pill = RectF(left, top, right, bottom)

            paint.color = scheme.glass.body
            canvas.drawRoundRect(pill, g.radius.toFloat(), g.radius.toFloat(), paint)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = (density).coerceAtLeast(1f)
            paint.color = scheme.glass.hairline
            canvas.drawRoundRect(pill, g.radius.toFloat(), g.radius.toFloat(), paint)
            paint.style = Paint.Style.FILL

            val frac = fillFraction(i, state)
            if (frac > 0.0) {
                val inner = RectF(
                    left + g.inset, top + g.inset,
                    left + g.inset + (g.pillW - 2 * g.inset) * frac.toFloat(),
                    bottom - g.inset,
                )
                val r = min((g.radius - g.inset).toFloat(), inner.width() / 2f)
                // Vertical sheen→fill gradient gives the capsule its glass depth.
                paint.shader = LinearGradient(
                    inner.left, inner.top, inner.left, inner.bottom,
                    intArrayOf(period.sheen, period.fill), null, Shader.TileMode.CLAMP,
                )
                canvas.drawRoundRect(inner, r, r, paint)
                paint.shader = null
            }

            // Specular streak across the top inside every pill (glass cue).
            paint.color = 0x16FFFFFF
            val spec = RectF(left + g.inset, top + g.inset, right - g.inset,
                top + g.inset + (g.pillH - 2 * g.inset) / 3f)
            val sr = spec.height() / 2f
            canvas.drawRoundRect(spec, sr, sr, paint)
        }
        return bmp
    }
}
