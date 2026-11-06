package com.nh.tilesmarbledesigner.engine

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import kotlin.math.*

/** Built-in materials are generated procedurally on-device (no network, no bundled photos needed). */
object Materials {
    data class Mat(val id: String, val name: String, val category: String)

    val builtin = listOf(
        Mat("white_marble", "White Marble", "Marble"), Mat("black_marble", "Black Marble", "Marble"),
        Mat("italian_marble", "Italian Marble", "Marble"), Mat("granite", "Granite", "Granite"),
        Mat("ceramic", "Ceramic Tile", "Tiles"), Mat("wood", "Wooden Flooring", "Wood"),
        Mat("glossy", "Glossy Tile", "Tiles"), Mat("matte", "Matte Tile", "Tiles"),
        Mat("stone", "Stone", "Stone"), Mat("modern_wall", "Modern Wall", "Wall")
    )

    private val cache = LruCache<String, Bitmap>(24)

    fun isCustom(id: String) = id.startsWith("custom:")
    fun evict(id: String) { cache.remove(id) }
    fun nameOf(id: String) = builtin.firstOrNull { it.id == id }?.name ?: "Custom texture"

    fun bitmap(id: String): Bitmap {
        cache.get(id)?.let { return it }
        val b = if (isCustom(id)) loadCustom(id.removePrefix("custom:")) ?: gen("ceramic") else gen(id)
        cache.put(id, b)
        return b
    }

    private fun loadCustom(path: String): Bitmap? = try {
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, o)
        var s = 1
        while (max(o.outWidth, o.outHeight) / s > 1024) s *= 2
        BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = s })
    } catch (e: Throwable) { null }

    // ---------- procedural noise ----------
    private fun h(x: Int, y: Int, s: Int): Float {
        var n = x * 374761393 + y * 668265263 + s * 982451653
        n = (n xor (n ushr 13)) * 1274126177
        n = n xor (n ushr 16)
        return (n and 0xFFFFFF) / 16777215f
    }

    private fun vnoise(x: Float, y: Float, s: Int): Float {
        val xi = floor(x).toInt(); val yi = floor(y).toInt()
        val fx = x - xi; val fy = y - yi
        val u = fx * fx * (3 - 2 * fx); val v = fy * fy * (3 - 2 * fy)
        val a = h(xi, yi, s); val b = h(xi + 1, yi, s); val c = h(xi, yi + 1, s); val d = h(xi + 1, yi + 1, s)
        return a + (b - a) * u + (c - a) * v + (a - b - c + d) * u * v
    }

    private fun fbm(x: Float, y: Float, s: Int, oct: Int = 4): Float {
        var amp = 0.5f; var f = 1f; var sum = 0f; var norm = 0f
        for (i in 0 until oct) { sum += amp * vnoise(x * f, y * f, s + i); norm += amp; amp *= 0.5f; f *= 2f }
        return sum / norm
    }

    private fun mix(a: Int, b: Int, t: Float): Int {
        val k = t.coerceIn(0f, 1f)
        fun ch(sh: Int) = (((a shr sh) and 255) * (1 - k) + ((b shr sh) and 255) * k).toInt()
        return (0xFF shl 24) or (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
    }

    private fun marble(x: Float, y: Float, base: Int, cloud: Int, vein: Int, freq: Float, seed: Int): Int {
        val pi = Math.PI.toFloat()
        val warp = fbm(x * 3f, y * 3f, seed) * 4f
        val v = abs(sin((x * freq + y * freq * 0.6f + warp) * pi))
        val line = (1f - v).pow(10f)
        val fine = (1f - abs(sin((x * freq * 2.3f - y * freq * 1.4f + warp * 1.7f) * pi))).pow(18f) * 0.5f
        val c = mix(base, cloud, fbm(x * 5f, y * 5f, seed + 7))
        return mix(c, vein, line * 0.85f + fine)
    }

    private fun gen(id: String): Bitmap {
        val s = 256
        val px = IntArray(s * s)
        for (y in 0 until s) for (x in 0 until s) {
            val fx = x / s.toFloat(); val fy = y / s.toFloat()
            px[y * s + x] = when (id) {
                "white_marble" -> marble(fx, fy, 0xFFF4F4F2.toInt(), 0xFFD9DCE0.toInt(), 0xFF6F7680.toInt(), 3f, 1)
                "black_marble" -> marble(fx, fy, 0xFF16161A.toInt(), 0xFF2C2C33.toInt(), 0xFFE0E0E0.toInt(), 3f, 6)
                "italian_marble" -> marble(fx, fy, 0xFFEDE3D2.toInt(), 0xFFD8C9AE.toInt(), 0xFFA97C3F.toInt(), 2.5f, 4)
                "granite" -> {
                    val base = mix(0xFF5E5A58.toInt(), 0xFF8A8480.toInt(), fbm(fx * 20f, fy * 20f, 3))
                    val r = h(x, y, 17)
                    when { r > 0.93f -> 0xFF1A1A1A.toInt(); r < 0.04f -> 0xFFE8E0D8.toInt(); r in 0.5f..0.52f -> 0xFF9C6B5E.toInt(); else -> base }
                }
                "wood" -> {
                    val plank = y / 64
                    val g = fbm(fx * 2f, fy * 40f + plank * 7f, 3)
                    var c = mix(0xFF8A5A33.toInt(), 0xFFB98254.toInt(), g)
                    c = mix(c, 0xFF5A3A20.toInt(), h(plank, 0, 9) * 0.35f)
                    if (y % 64 < 2) 0xFF4A2E18.toInt() else c
                }
                "glossy" -> {
                    val c = mix(0xFF1E3A56.toInt(), 0xFF3C6B93.toInt(), fbm(fx * 2f, fy * 2f, 11))
                    val t = (fx + fy) * 0.5f
                    mix(c, 0xFFFFFFFF.toInt(), exp(-((t - 0.4f) * 8f).pow(2)) * 0.35f)
                }
                "matte" -> mix(0xFFD6D0C6.toInt(), 0xFFC4BDB1.toInt(), fbm(fx * 30f, fy * 30f, 5))
                "stone" -> {
                    val n = fbm(fx * 6f, fy * 6f, 12)
                    val c = mix(0xFF9E9684.toInt(), 0xFF776F60.toInt(), n)
                    mix(c, 0xFF4E483C.toInt(), (sin(fy * 30f + n * 5f) * 0.5f + 0.5f) * 0.18f)
                }
                "modern_wall" -> mix(0xFFBDBDBB.toInt(), 0xFFA5A6A6.toInt(), fbm(fx * 4f, fy * 4f, 8) * 0.8f + h(x, y, 2) * 0.2f)
                else -> { // ceramic
                    val c = mix(0xFFEFEDE8.toInt(), 0xFFE2DFD8.toInt(), fbm(fx * 8f, fy * 8f, 2))
                    if (h(x, y, 21) > 0.985f) 0xFFC9C5BC.toInt() else c
                }
            }
        }
        return Bitmap.createBitmap(px, s, s, Bitmap.Config.ARGB_8888)
    }
}
