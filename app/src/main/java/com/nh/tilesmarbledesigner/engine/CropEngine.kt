package com.nh.tilesmarbledesigner.engine

import android.content.Context
import android.graphics.*
import android.media.ExifInterface
import android.net.Uri
import kotlin.math.*

/** Image import + crop + perspective warp. Quad = [TLx,TLy, TRx,TRy, BRx,BRy, BLx,BLy] in image pixels. */
object CropEngine {

    fun decode(ctx: Context, uri: Uri, maxDim: Int = 2048): Bitmap? {
        return try {
            val cr = ctx.contentResolver
            val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, o) }
            if (o.outWidth <= 0) return null
            var s = 1
            while (max(o.outWidth, o.outHeight) / s > maxDim) s *= 2
            val o2 = BitmapFactory.Options().apply { inSampleSize = s }
            val b = cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, o2) } ?: return null
            val orient = cr.openInputStream(uri)?.use {
                ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            } ?: ExifInterface.ORIENTATION_NORMAL
            val deg = when (orient) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
            if (deg == 0f) b else rotate(b, deg)
        } catch (e: Throwable) { null }
    }

    fun rotate(b: Bitmap, deg: Float): Bitmap {
        val m = Matrix().apply { postRotate(deg) }
        return Bitmap.createBitmap(b, 0, 0, b.width, b.height, m, true)
    }

    fun defaultQuad(b: Bitmap): FloatArray {
        val w = b.width.toFloat(); val h = b.height.toFloat()
        return floatArrayOf(0.1f * w, 0.1f * h, 0.9f * w, 0.1f * h, 0.9f * w, 0.9f * h, 0.1f * w, 0.9f * h)
    }

    /** Detects the main subject by trimming near-uniform borders; falls back to a 10 % inset. */
    fun autoQuad(b: Bitmap): FloatArray {
        return try {
            val sw = 128; val sh = max(16, (128f * b.height / b.width).toInt())
            val sm = Bitmap.createScaledBitmap(b, sw, sh, true)
            val px = IntArray(sw * sh); sm.getPixels(px, 0, sw, 0, 0, sw, sh)
            var r = 0; var g = 0; var bl = 0; var n = 0
            for (cx in listOf(0, sw - 5)) for (cy in listOf(0, sh - 5)) for (y in cy until cy + 5) for (x in cx until cx + 5) {
                val p = px[y * sw + x]; r += Color.red(p); g += Color.green(p); bl += Color.blue(p); n++
            }
            r /= n; g /= n; bl /= n
            fun diff(p: Int) = abs(Color.red(p) - r) + abs(Color.green(p) - g) + abs(Color.blue(p) - bl)
            fun rowBusy(y: Int) = (0 until sw).count { diff(px[y * sw + it]) > 60 } > sw * 0.04
            fun colBusy(x: Int) = (0 until sh).count { diff(px[it * sw + x]) > 60 } > sh * 0.04
            var top = 0; while (top < sh - 1 && !rowBusy(top)) top++
            var bottom = sh - 1; while (bottom > top && !rowBusy(bottom)) bottom--
            var left = 0; while (left < sw - 1 && !colBusy(left)) left++
            var right = sw - 1; while (right > left && !colBusy(right)) right--
            if ((right - left) < sw * 0.25f || (bottom - top) < sh * 0.25f) return defaultQuad(b)
            val kx = b.width / sw.toFloat(); val ky = b.height / sh.toFloat()
            val l = left * kx; val t = top * ky; val rr = (right + 1) * kx; val bb = (bottom + 1) * ky
            floatArrayOf(l, t, rr, t, rr, bb, l, bb)
        } catch (e: Throwable) { defaultQuad(b) }
    }

    fun rotateQuad(q: FloatArray, deg: Float, w: Int, h: Int): FloatArray {
        val cx = (q[0] + q[2] + q[4] + q[6]) / 4f; val cy = (q[1] + q[3] + q[5] + q[7]) / 4f
        val a = Math.toRadians(deg.toDouble()); val ca = cos(a).toFloat(); val sa = sin(a).toFloat()
        val o = FloatArray(8)
        for (i in 0 until 4) {
            val dx = q[i * 2] - cx; val dy = q[i * 2 + 1] - cy
            o[i * 2] = (cx + dx * ca - dy * sa).coerceIn(0f, w.toFloat())
            o[i * 2 + 1] = (cy + dx * sa + dy * ca).coerceIn(0f, h.toFloat())
        }
        return o
    }

    /** Crops the quad and maps it to an upright rectangle (perspective correction). Never stretches beyond 1024 px. */
    fun warp(src: Bitmap, quad: FloatArray, maxSide: Int = 768): Bitmap {
        fun dist(a: Int, b: Int) = hypot(quad[a * 2] - quad[b * 2], quad[a * 2 + 1] - quad[b * 2 + 1])
        val wq = (dist(0, 1) + dist(3, 2)) / 2f; val hq = (dist(0, 3) + dist(1, 2)) / 2f
        val k = min(1f, maxSide / max(max(wq, hq), 1f))
        val ow = (wq * k).roundToInt().coerceIn(16, 1024); val oh = (hq * k).roundToInt().coerceIn(16, 1024)
        val m = Matrix()
        val ok = m.setPolyToPoly(quad, 0, floatArrayOf(0f, 0f, ow.toFloat(), 0f, ow.toFloat(), oh.toFloat(), 0f, oh.toFloat()), 0, 4)
        val out = Bitmap.createBitmap(ow, oh, Bitmap.Config.ARGB_8888)
        if (ok) Canvas(out).drawBitmap(src, m, Paint(Paint.FILTER_BITMAP_FLAG))
        return out
    }
}
