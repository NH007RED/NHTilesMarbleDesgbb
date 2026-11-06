package com.nh.tilesmarbledesigner.engine

import android.graphics.*
import android.util.LruCache
import com.nh.tilesmarbledesigner.data.*

/** Combines project dimensions + designs + openings into surface textures and renders the room. */
object Scene {
    private val cache = LruCache<String, Bitmap>(14)

    fun dims(p: ProjectData, s: Surf): Pair<Double, Double> = when (s) {
        Surf.FLOOR, Surf.CEILING -> p.roomL to p.roomW
        Surf.LEFT, Surf.RIGHT -> p.roomW to p.height
        Surf.FRONT, Surf.BACK -> p.roomL to p.height
    }

    /** Wall index used by openings: 0 Back, 1 Right, 2 Front, 3 Left. */
    fun wallIndex(s: Surf): Int = when (s) { Surf.BACK -> 0; Surf.RIGHT -> 1; Surf.FRONT -> 2; Surf.LEFT -> 3; else -> -1 }
    fun wallName(i: Int): String = listOf("Back", "Right", "Front", "Left").getOrNull(i) ?: "Extra"

    fun surfaceBitmaps(p: ProjectData, designs: Map<Surf, SurfaceDesign>, maxPx: Int = 768): Map<Surf, Bitmap> =
        Surf.values().associateWith { s ->
            val d = designs[s] ?: SurfaceDesign()
            val (w, h) = dims(p, s)
            val wi = wallIndex(s)
            val ops = if (wi >= 0) p.openings.filter { it.wall == wi } else emptyList()
            val key = "$s|$d|$w|$h|$maxPx|$ops"
            cache.get(key) ?: PatternComposer.compose(Materials.bitmap(d.materialId), Materials.isCustom(d.materialId), d, w, h, maxPx)
                .also { if (ops.isNotEmpty()) drawOpenings(it, ops, w, p.height); cache.put(key, it) }
        }

    private fun drawOpenings(b: Bitmap, ops: List<Opening>, wallLen: Double, height: Double) {
        val c = Canvas(b)
        val kx = (b.width / wallLen).toFloat(); val ky = (b.height / height).toFloat()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        for (o in ops) for (i in 0 until o.count) {
            val centreM = o.pos * wallLen + (i - (o.count - 1) / 2.0) * (o.w * 1.35)
            val cx = (centreM * kx).toFloat()
            val wpx = (o.w * kx).toFloat().coerceAtLeast(6f); val hpx = (o.h * ky).toFloat().coerceAtLeast(6f)
            val bottom = b.height - (o.sill * ky).toFloat(); val top = bottom - hpx
            val l = cx - wpx / 2f; val r = cx + wpx / 2f
            if (o.door) drawDoor(c, paint, l, top, r, bottom) else drawWindow(c, paint, l, top, r, bottom)
        }
    }

    private fun drawDoor(c: Canvas, p: Paint, l: Float, t: Float, r: Float, b: Float) {
        val f = maxOf(3f, (r - l) * 0.06f)
        p.style = Paint.Style.FILL; p.shader = null
        p.color = 0xFF3E2610.toInt(); c.drawRect(l, t, r, b, p)
        p.color = 0xFFA0693A.toInt(); c.drawRect(l + f, t + f, r - f, b, p)
        p.color = 0xFF8A5830.toInt()
        val mw = (r - l - f * 2) * 0.62f; val mx = (l + r) / 2f - mw / 2f
        val h = b - t
        c.drawRect(mx, t + h * 0.08f, mx + mw, t + h * 0.42f, p)
        c.drawRect(mx, t + h * 0.50f, mx + mw, t + h * 0.90f, p)
        p.color = 0xFFD4AF37.toInt(); c.drawCircle(r - f * 2.6f, t + h * 0.52f, maxOf(2.5f, (r - l) * 0.03f), p)
        p.style = Paint.Style.STROKE; p.strokeWidth = 1.5f; p.color = 0x66000000; c.drawRect(l, t, r, b, p)
    }

    private fun drawWindow(c: Canvas, p: Paint, l: Float, t: Float, r: Float, b: Float) {
        val f = maxOf(3f, (r - l) * 0.05f)
        p.style = Paint.Style.FILL
        p.shader = null; p.color = 0xFFF2F2F2.toInt(); c.drawRect(l, t, r, b, p)
        p.shader = LinearGradient(0f, t, 0f, b, 0xFFD6EEFF.toInt(), 0xFF7FB3E3.toInt(), Shader.TileMode.CLAMP)
        c.drawRect(l + f, t + f, r - f, b - f, p)
        p.shader = null; p.color = 0xFFF2F2F2.toInt()
        c.drawRect((l + r) / 2f - f * 0.4f, t, (l + r) / 2f + f * 0.4f, b, p)
        c.drawRect(l, (t + b) / 2f - f * 0.4f, r, (t + b) / 2f + f * 0.4f, p)
        p.color = 0xFFD0D0D0.toInt(); c.drawRect(l - f, b, r + f, b + f * 1.6f, p)
        p.style = Paint.Style.STROKE; p.strokeWidth = 1.5f; p.color = 0x66000000; c.drawRect(l, t, r, b, p)
    }

    /** "Before" look: plain bare walls and floor. */
    fun before(): Map<Surf, SurfaceDesign> =
        Surf.values().associateWith { SurfaceDesign("matte", 6.0, 6.0, TilePattern.STRAIGHT, 0.0, 0xFFC8C2B8.toInt()) }

    fun render(
        p: ProjectData, designs: Map<Surf, SurfaceDesign>, w: Int, h: Int,
        yaw: Float, pitch: Float, zoom: Float, maxPx: Int = 768
    ): Bitmap {
        val bm = surfaceBitmaps(p, designs, maxPx)
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), Paint().apply {
            shader = LinearGradient(0f, 0f, 0f, h.toFloat(), 0xFFEEF1F6.toInt(), 0xFFCDD5E3.toInt(), Shader.TileMode.CLAMP)
        })
        RoomRenderer.render(c, w.toFloat(), h.toFloat(), p.roomL.toFloat(), p.roomW.toFloat(), p.height.toFloat(), bm, yaw, pitch, zoom, FurnitureLib.layout(p))
        return out
    }
}
