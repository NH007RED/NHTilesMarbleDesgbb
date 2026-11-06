package com.nh.tilesmarbledesigner.engine

import android.graphics.*
import com.nh.tilesmarbledesigner.data.Surf
import kotlin.math.*

/**
 * Offline perspective renderer. The room is a box seen from outside with the nearest walls cut away
 * ("dolls-house" view). Every visible surface is a texture warped through a perspective mesh.
 *
 * World: x = left/right, y = up, z = depth. Floor y=0, ceiling y=H, back wall z=+W/2, front wall z=-W/2.
 */
object RoomRenderer {
    private class Face(val s: Surf, val n: FloatArray, val pt: (Float, Float) -> FloatArray)

    private val light = floatArrayOf(-0.35f, 0.8f, -0.5f).let { v ->
        val l = sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]); floatArrayOf(v[0] / l, v[1] / l, v[2] / l)
    }

    fun render(
        c: Canvas, w: Float, h: Float, L: Float, W: Float, H: Float,
        tex: Map<Surf, Bitmap>, yawDeg: Float, pitchDeg: Float, zoom: Float, boxes: List<Box3> = emptyList()
    ) {
        val ry = Math.toRadians(yawDeg.toDouble()); val rx = Math.toRadians(pitchDeg.toDouble())
        val cY = cos(ry).toFloat(); val sY = sin(ry).toFloat(); val cX = cos(rx).toFloat(); val sX = sin(rx).toFloat()
        val radius = 0.5f * sqrt(L * L + W * W + H * H)
        val d = radius * 2.4f
        val f = (min(w, h) / 2f) * (d / radius) * 0.95f * zoom.coerceIn(0.4f, 4f)

        fun rot(x: Float, y: Float, z: Float): FloatArray {
            val x1 = x * cY + z * sY; val z1 = -x * sY + z * cY
            return floatArrayOf(x1, y * cX + z1 * sX, -y * sX + z1 * cX)
        }
        fun view(p: FloatArray): FloatArray { val r = rot(p[0], p[1] - H / 2f, p[2]); r[2] += d; return r }

        val faces = listOf(
            Face(Surf.FLOOR, floatArrayOf(0f, 1f, 0f)) { u, v -> floatArrayOf(-L / 2 + u * L, 0f, W / 2 - v * W) },
            Face(Surf.CEILING, floatArrayOf(0f, -1f, 0f)) { u, v -> floatArrayOf(-L / 2 + u * L, H, W / 2 - v * W) },
            Face(Surf.BACK, floatArrayOf(0f, 0f, -1f)) { u, v -> floatArrayOf(-L / 2 + u * L, H - v * H, W / 2) },
            Face(Surf.FRONT, floatArrayOf(0f, 0f, 1f)) { u, v -> floatArrayOf(L / 2 - u * L, H - v * H, -W / 2) },
            Face(Surf.LEFT, floatArrayOf(1f, 0f, 0f)) { u, v -> floatArrayOf(-L / 2, H - v * H, W / 2 - u * W) },
            Face(Surf.RIGHT, floatArrayOf(-1f, 0f, 0f)) { u, v -> floatArrayOf(L / 2, H - v * H, -W / 2 + u * W) }
        )

        val visible = faces.mapNotNull { fc ->
            val cv = view(fc.pt(0.5f, 0.5f)); val nv = rot(fc.n[0], fc.n[1], fc.n[2])
            val facing = -(nv[0] * cv[0] + nv[1] * cv[1] + nv[2] * cv[2])
            if (facing > 0f && tex[fc.s] != null) Pair(fc, cv[2]) else null
        }.sortedByDescending { it.second }

        val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 2f; color = 0x66000000 }
        val n = 12
        for ((fc, _) in visible) {
            val verts = FloatArray((n + 1) * (n + 1) * 2)
            var k = 0
            for (j in 0..n) for (i in 0..n) {
                val v = view(fc.pt(i / n.toFloat(), j / n.toFloat()))
                verts[k++] = w / 2f + f * v[0] / v[2]; verts[k++] = h / 2f - f * v[1] / v[2]
            }
            val dot = fc.n[0] * light[0] + fc.n[1] * light[1] + fc.n[2] * light[2]
            val b = (0.82f + 0.18f * dot).coerceIn(0.6f, 1f)
            val p = Paint(Paint.FILTER_BITMAP_FLAG).apply {
                colorFilter = ColorMatrixColorFilter(ColorMatrix(floatArrayOf(
                    b, 0f, 0f, 0f, 0f, 0f, b, 0f, 0f, 0f, 0f, 0f, b, 0f, 0f, 0f, 0f, 0f, 1f, 0f)))
            }
            c.drawBitmapMesh(tex[fc.s]!!, n, n, verts, 0, null, 0, p)
            val path = Path()
            val corners = listOf(0, n, (n + 1) * (n + 1) - 1, n * (n + 1))
            corners.forEachIndexed { idx, ci ->
                val x = verts[ci * 2]; val y = verts[ci * 2 + 1]
                if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            c.drawPath(path, line)
        }

        // ---------- furniture ----------
        if (boxes.isNotEmpty()) {
            val fill = Paint(Paint.ANTI_ALIAS_FLAG)
            val edge = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 1.5f; color = 0x55000000 }
            fun shadeC(col: Int, k: Float): Int = Color.rgb(
                ((col shr 16 and 255) * k).toInt().coerceIn(0, 255),
                ((col shr 8 and 255) * k).toInt().coerceIn(0, 255),
                ((col and 255) * k).toInt().coerceIn(0, 255))
            fun poly(pts: List<FloatArray>, color: Int, outline: Boolean) {
                val path = Path()
                pts.forEachIndexed { i, q ->
                    val v = view(q)
                    val x = w / 2f + f * v[0] / v[2]; val y = h / 2f - f * v[1] / v[2]
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
                fill.color = color; fill.style = Paint.Style.FILL
                c.drawPath(path, fill)
                if (outline) c.drawPath(path, edge)
            }
            fun drawBox(bx: Box3) {
                val x0 = bx.cx - bx.w / 2f; val x1 = bx.cx + bx.w / 2f
                val z0 = bx.cz - bx.d / 2f; val z1 = bx.cz + bx.d / 2f
                val y0 = bx.y0; val y1 = bx.y0 + bx.h
                fun p3(x: Float, y: Float, z: Float) = floatArrayOf(x, y, z)
                val sides = listOf(
                    floatArrayOf(0f, 1f, 0f) to listOf(p3(x0, y1, z0), p3(x1, y1, z0), p3(x1, y1, z1), p3(x0, y1, z1)),
                    floatArrayOf(0f, 0f, -1f) to listOf(p3(x0, y0, z0), p3(x1, y0, z0), p3(x1, y1, z0), p3(x0, y1, z0)),
                    floatArrayOf(0f, 0f, 1f) to listOf(p3(x0, y0, z1), p3(x1, y0, z1), p3(x1, y1, z1), p3(x0, y1, z1)),
                    floatArrayOf(-1f, 0f, 0f) to listOf(p3(x0, y0, z0), p3(x0, y0, z1), p3(x0, y1, z1), p3(x0, y1, z0)),
                    floatArrayOf(1f, 0f, 0f) to listOf(p3(x1, y0, z0), p3(x1, y0, z1), p3(x1, y1, z1), p3(x1, y1, z0))
                )
                for ((n, corners) in sides) {
                    val cxm = corners.sumOf { it[0].toDouble() }.toFloat() / 4f
                    val cym = corners.sumOf { it[1].toDouble() }.toFloat() / 4f
                    val czm = corners.sumOf { it[2].toDouble() }.toFloat() / 4f
                    val cv = view(floatArrayOf(cxm, cym, czm)); val nv = rot(n[0], n[1], n[2])
                    if (-(nv[0] * cv[0] + nv[1] * cv[1] + nv[2] * cv[2]) <= 0f) continue
                    val dot = n[0] * light[0] + n[1] * light[1] + n[2] * light[2]
                    poly(corners, shadeC(bx.color, (0.78f + 0.3f * dot).coerceIn(0.5f, 1.12f)), true)
                }
            }
            val flats = boxes.filter { it.h < 0.03f }
            val solids = boxes.filter { it.h >= 0.03f }.sortedByDescending { view(floatArrayOf(it.cx, it.y0 + it.h / 2f, it.cz))[2] }
            flats.forEach { drawBox(it) }
            if (pitchDeg > 2f) solids.filter { it.y0 < 0.2f && it.w * it.d > 0.15f }.forEach { bx ->
                val m = 0.06f
                poly(listOf(
                    floatArrayOf(bx.cx - bx.w / 2 - m, 0.004f, bx.cz - bx.d / 2 - m), floatArrayOf(bx.cx + bx.w / 2 + m, 0.004f, bx.cz - bx.d / 2 - m),
                    floatArrayOf(bx.cx + bx.w / 2 + m, 0.004f, bx.cz + bx.d / 2 + m), floatArrayOf(bx.cx - bx.w / 2 - m, 0.004f, bx.cz + bx.d / 2 + m)
                ), 0x30000000, false)
            }
            solids.forEach { drawBox(it) }
        }
    }
}
