package dev.ngale.thirds

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Draws the eight-pill bar for one exact widget-instance size. Geometry is
 * pure ints (unit-testable); rendering is the thin part.
 */
object BarRenderer {

    data class Geometry(
        val pad: Int,
        val gap: Int,
        val pillW: Int,
        val pillH: Int,
        val radius: Int,
        val inset: Int,
    )

    /** Even width: 2*pad + 8*pillW + 7*gap == widthPx (± rounding). */
    fun computeGeometry(widthPx: Int, heightPx: Int, density: Float): Geometry {
        fun dp(v: Int) = (v * density).roundToInt()
        val pad = dp(4)
        val gap = dp(3)
        val pillH = dp(18)
        val inset = dp(2)
        val pillW = maxOf(dp(8), (widthPx - 2 * pad - 7 * gap) / 8)
        return Geometry(pad, gap, pillW, min(pillH, heightPx), pillH / 2, inset)
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
        val cy = heightPx / 2f
        val top = cy - g.pillH / 2f
        val bottom = cy + g.pillH / 2f

        for (i in 0 until 8) {
            val left = g.pad + i * (g.pillW + g.gap).toFloat()
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
                paint.color = period.fill
                canvas.drawRoundRect(inner, r, r, paint)
            }
        }
        return bmp
    }
}
