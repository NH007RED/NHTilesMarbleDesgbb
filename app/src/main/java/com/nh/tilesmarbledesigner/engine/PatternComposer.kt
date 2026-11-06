package com.nh.tilesmarbledesigner.engine

import android.graphics.*
import com.nh.tilesmarbledesigner.data.SurfaceDesign
import com.nh.tilesmarbledesigner.data.TilePattern
import kotlin.math.*

/** Builds the flat (unwarped) texture of one surface: tile pattern + grout + edge shading. */
object PatternComposer {

    fun compose(tex: Bitmap, custom: Boolean, d: SurfaceDesign, wM: Double, hM: Double, maxPx: Int = 768): Bitmap {
        val scale = (maxPx / max(wM, hM)).toFloat()
        val bw = (wM * scale).roundToInt().coerceAtLeast(32)
        val bh = (hM * scale).roundToInt().coerceAtLeast(32)
        val out = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        c.drawColor(d.groutColor)
        val g = if (d.groutMm <= 0.0) 0f else max(1f, (d.groutMm / 1000.0 * scale).toFloat())
        val tl = max(8f, (d.tileL * scale).toFloat())
        val tw = max(8f, (d.tileW * scale).toFloat())
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)
        val shade = Paint().apply { color = Color.BLACK }
        val src = Rect(); val dst = RectF()
        val tw0 = tex.width.toFloat(); val th0 = tex.height.toFloat()

        fun tile(l: Float, t: Float, r: Float, b: Float, idx: Int, flip: Boolean = false, dark: Int = 0) {
            val inset = g / 2f
            dst.set(l + inset, t + inset, r - inset, b - inset)
            if (dst.width() < 0.5f || dst.height() < 0.5f) return
            val ar = dst.width() / dst.height()
            var sw: Float; var sh: Float
            if (tw0 / th0 > ar) { sh = th0; sw = sh * ar } else { sw = tw0; sh = sw / ar }
            var left = (tw0 - sw) / 2f; var top = (th0 - sh) / 2f
            if (!custom) { // built-in textures: each tile shows a different piece
                val rnd = java.util.Random(idx * 7919L xor 0x5DEECE66DL)
                val f = 0.45f + 0.5f * rnd.nextFloat()
                sw *= f; sh *= f
                left = rnd.nextFloat() * (tw0 - sw); top = rnd.nextFloat() * (th0 - sh)
            }
            src.set(left.toInt(), top.toInt(), (left + sw).toInt().coerceAtLeast(left.toInt() + 1), (top + sh).toInt().coerceAtLeast(top.toInt() + 1))
            if (flip) { c.save(); c.scale(-1f, 1f, dst.centerX(), dst.centerY()); c.drawBitmap(tex, src, dst, paint); c.restore() }
            else c.drawBitmap(tex, src, dst, paint)
            if (dark > 0) { shade.alpha = dark; c.drawRect(dst, shade) }
        }

        when (d.pattern) {
            TilePattern.STRAIGHT, TilePattern.GRID, TilePattern.SLAB -> {
                val cols = ceil(bw / tl).toInt(); val rows = ceil(bh / tw).toInt()
                for (r in 0 until rows) for (cc in 0 until cols) {
                    val chk = (r + cc) % 2 == 1
                    tile(cc * tl, r * tw, cc * tl + tl, r * tw + tw, r * 1000 + cc,
                        flip = (d.pattern == TilePattern.SLAB && cc % 2 == 1) || (d.pattern == TilePattern.GRID && chk),
                        dark = if (d.pattern == TilePattern.GRID && chk) 28 else 0)
                }
            }
            TilePattern.BRICK -> {
                val cols = ceil(bw / tl).toInt() + 1; val rows = ceil(bh / tw).toInt()
                for (r in 0 until rows) {
                    val off = if (r % 2 == 1) tl / 2f else 0f
                    for (cc in -1 until cols) tile(cc * tl + off, r * tw, cc * tl + off + tl, r * tw + tw, r * 1000 + cc + 5)
                }
            }
            TilePattern.DIAGONAL -> {
                c.save(); c.translate(bw / 2f, bh / 2f); c.rotate(45f)
                val rad = hypot(bw.toFloat(), bh.toFloat()) / 2f
                val n = ceil(rad / tl).toInt() + 1; val m = ceil(rad / tw).toInt() + 1
                for (r in -m..m) for (cc in -n..n) tile(cc * tl - tl / 2f, r * tw - tw / 2f, cc * tl + tl / 2f, r * tw + tw / 2f, r * 1000 + cc + 9)
                c.restore()
            }
            TilePattern.HERRINGBONE -> {
                // tile length is snapped to a whole multiple (n) of the tile width
                c.save(); c.translate(bw / 2f, bh / 2f); c.rotate(45f)
                val cell = tw
                val n = max(2, (tl / tw).roundToInt())
                val rad = hypot(bw.toFloat(), bh.toFloat()) / 2f
                val big = ceil(rad / cell).toInt() + n + 2
                for (y in -big..big) {
                    val mLo = Math.floorDiv(-big - y, 2 * n) - 1
                    val mHi = Math.floorDiv(big - y, 2 * n) + 1
                    for (m in mLo..mHi) {
                        val hx = (y + 2 * n * m) * cell
                        tile(hx, y * cell, hx + n * cell, (y + 1) * cell, y * 977 + m * 13)
                        val vx = (y + n + 2 * n * m) * cell
                        val vy = (y - n + 1) * cell
                        tile(vx, vy, vx + cell, vy + n * cell, y * 977 + m * 13 + 1)
                    }
                }
                c.restore()
            }
        }

        // soft edge shadow (ambient occlusion where surfaces meet)
        val e = min(bw, bh) * 0.08f
        val ep = Paint()
        fun edge(x0: Float, y0: Float, x1: Float, y1: Float, l: Float, t: Float, r: Float, b: Float) {
            ep.shader = LinearGradient(x0, y0, x1, y1, 0x50000000, 0x00000000, Shader.TileMode.CLAMP)
            c.drawRect(l, t, r, b, ep)
        }
        edge(0f, 0f, 0f, e, 0f, 0f, bw.toFloat(), e)
        edge(0f, bh.toFloat(), 0f, bh - e, 0f, bh - e, bw.toFloat(), bh.toFloat())
        edge(0f, 0f, e, 0f, 0f, 0f, e, bh.toFloat())
        edge(bw.toFloat(), 0f, bw - e, 0f, bw - e, 0f, bw.toFloat(), bh.toFloat())
        if (d.materialId == "glossy" || d.materialId.contains("marble")) {
            ep.shader = LinearGradient(0f, 0f, bw.toFloat(), bh.toFloat(),
                intArrayOf(0x00FFFFFF, 0x26FFFFFF, 0x00FFFFFF), floatArrayOf(0.2f, 0.5f, 0.8f), Shader.TileMode.CLAMP)
            c.drawRect(0f, 0f, bw.toFloat(), bh.toFloat(), ep)
        }
        return out
    }
}
