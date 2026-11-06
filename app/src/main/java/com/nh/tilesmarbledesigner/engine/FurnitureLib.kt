package com.nh.tilesmarbledesigner.engine

import com.nh.tilesmarbledesigner.data.FKind
import com.nh.tilesmarbledesigner.data.ProjectData

/** Axis-aligned box in room coordinates (metres): centre x/z, bottom y0, size w (x) d (z) h (y). */
data class Box3(val cx: Float, val cz: Float, val y0: Float, val w: Float, val d: Float, val h: Float, val color: Int)

/** Furniture is modelled from simple 3D boxes and drawn by RoomRenderer. */
object FurnitureLib {
    private const val WOOD = 0xFF8B5A33.toInt()
    private const val DARK = 0xFF2B2B30.toInt()
    private const val FABRIC = 0xFF6B7A99.toInt()
    private const val FABRIC2 = 0xFF8895B3.toInt()
    private const val WHITE = 0xFFF2F2F2.toInt()
    private const val CREAM = 0xFFEFE4CF.toInt()
    private const val STEEL = 0xFFB9BEC6.toInt()
    private const val GOLD = 0xFFD4AF37.toInt()
    private const val GREEN = 0xFF3E7D3A.toInt()

    private fun b(cx: Float, cz: Float, y0: Float, w: Float, d: Float, h: Float, c: Int) = Box3(cx, cz, y0, w, d, h, c)

    private fun chair(cx: Float, cz: Float, backX: Float, backZ: Float): List<Box3> {
        val out = mutableListOf(b(cx, cz, 0.45f, 0.44f, 0.44f, 0.05f, WOOD))
        for (sx in listOf(-0.18f, 0.18f)) for (sz in listOf(-0.18f, 0.18f)) out += b(cx + sx, cz + sz, 0f, 0.04f, 0.04f, 0.45f, WOOD)
        out += if (backZ != 0f) b(cx, cz + backZ * 0.2f, 0.5f, 0.44f, 0.04f, 0.42f, WOOD)
        else b(cx + backX * 0.2f, cz, 0.5f, 0.04f, 0.44f, 0.42f, WOOD)
        return out
    }

    /** Parts centred on (0,0), back of the piece at +z, front (user side) at -z. Listed bottom to top. */
    fun parts(k: FKind): List<Box3> = when (k) {
        FKind.SOFA -> listOf(
            b(0f, 0f, 0.1f, 2.1f, 0.9f, 0.3f, FABRIC), b(0f, 0.34f, 0.4f, 2.1f, 0.22f, 0.45f, FABRIC),
            b(-0.95f, 0f, 0.1f, 0.2f, 0.9f, 0.55f, FABRIC), b(0.95f, 0f, 0.1f, 0.2f, 0.9f, 0.55f, FABRIC),
            b(-0.45f, -0.05f, 0.4f, 0.8f, 0.62f, 0.12f, FABRIC2), b(0.45f, -0.05f, 0.4f, 0.8f, 0.62f, 0.12f, FABRIC2))
        FKind.ARMCHAIR -> listOf(
            b(0f, 0f, 0.1f, 0.9f, 0.9f, 0.3f, FABRIC), b(0f, 0.34f, 0.4f, 0.9f, 0.22f, 0.45f, FABRIC),
            b(-0.34f, 0f, 0.1f, 0.2f, 0.9f, 0.5f, FABRIC), b(0.34f, 0f, 0.1f, 0.2f, 0.9f, 0.5f, FABRIC),
            b(0f, -0.05f, 0.4f, 0.5f, 0.62f, 0.12f, FABRIC2))
        FKind.COFFEE_TABLE -> listOf(
            b(-0.5f, -0.24f, 0f, 0.06f, 0.06f, 0.38f, DARK), b(0.5f, -0.24f, 0f, 0.06f, 0.06f, 0.38f, DARK),
            b(-0.5f, 0.24f, 0f, 0.06f, 0.06f, 0.38f, DARK), b(0.5f, 0.24f, 0f, 0.06f, 0.06f, 0.38f, DARK),
            b(0f, 0f, 0.38f, 1.1f, 0.6f, 0.05f, WOOD))
        FKind.TV_UNIT -> listOf(
            b(0f, 0f, 0f, 1.8f, 0.4f, 0.45f, DARK), b(0f, 0.1f, 0.45f, 0.25f, 0.1f, 0.1f, DARK),
            b(0f, 0.12f, 0.55f, 1.2f, 0.05f, 0.7f, 0xFF0B0B0D.toInt()))
        FKind.SIDEBOARD -> listOf(
            b(0f, 0f, 0.1f, 1.6f, 0.45f, 0.7f, WOOD), b(-0.7f, 0f, 0f, 0.05f, 0.4f, 0.1f, DARK), b(0.7f, 0f, 0f, 0.05f, 0.4f, 0.1f, DARK),
            b(-0.5f, 0f, 0.8f, 0.14f, 0.14f, 0.3f, 0xFF4A7FB5.toInt()), b(0.45f, 0f, 0.8f, 0.2f, 0.12f, 0.05f, GOLD))
        FKind.RUG -> listOf(b(0f, 0f, 0f, 2.4f, 1.7f, 0.015f, 0xFFC9B79C.toInt()), b(0f, 0f, 0.015f, 2.1f, 1.4f, 0.005f, 0xFFB09A7A.toInt()))
        FKind.PLANT -> listOf(
            b(0f, 0f, 0f, 0.3f, 0.3f, 0.3f, 0xFFB5653A.toInt()), b(0f, 0f, 0.3f, 0.5f, 0.5f, 0.6f, GREEN),
            b(0f, 0f, 0.9f, 0.3f, 0.3f, 0.3f, 0xFF4F9A4A.toInt()))
        FKind.LAMP -> listOf(b(0f, 0f, 0f, 0.06f, 0.06f, 1.4f, STEEL), b(0f, 0f, 1.4f, 0.35f, 0.35f, 0.25f, CREAM))
        FKind.BED -> listOf(
            b(0f, 0f, 0.1f, 1.6f, 2.1f, 0.25f, WOOD), b(0f, -0.03f, 0.35f, 1.5f, 1.95f, 0.2f, WHITE),
            b(0f, 1.0f, 0.1f, 1.6f, 0.1f, 0.9f, WOOD), b(0f, -0.35f, 0.55f, 1.52f, 1.2f, 0.04f, 0xFF5E7391.toInt()),
            b(-0.4f, 0.78f, 0.55f, 0.6f, 0.35f, 0.12f, WHITE), b(0.4f, 0.78f, 0.55f, 0.6f, 0.35f, 0.12f, WHITE))
        FKind.NIGHTSTAND -> listOf(b(0f, 0f, 0f, 0.45f, 0.4f, 0.5f, WOOD), b(0f, 0f, 0.5f, 0.18f, 0.18f, 0.25f, CREAM))
        FKind.WARDROBE -> listOf(
            b(0f, 0f, 0f, 1.8f, 0.6f, 2.1f, 0xFF9C7048.toInt()), b(-0.1f, -0.315f, 1.0f, 0.03f, 0.03f, 0.35f, STEEL),
            b(0.1f, -0.315f, 1.0f, 0.03f, 0.03f, 0.35f, STEEL), b(0f, -0.305f, 0.02f, 0.01f, 0.01f, 2.05f, 0xFF6B4A2A.toInt()))
        FKind.DESK -> listOf(
            b(-0.6f, 0f, 0f, 0.05f, 0.6f, 0.72f, WOOD), b(0.6f, 0f, 0f, 0.05f, 0.6f, 0.72f, WOOD),
            b(0f, 0f, 0.72f, 1.3f, 0.65f, 0.04f, WOOD), b(0f, 0.2f, 0.76f, 0.15f, 0.1f, 0.1f, DARK),
            b(0f, 0.2f, 0.86f, 0.55f, 0.03f, 0.32f, 0xFF0B0B0D.toInt()))
        FKind.CHAIR -> chair(0f, 0f, 0f, 1f)
        FKind.BOOKSHELF -> listOf(
            b(0f, 0f, 0f, 1.0f, 0.35f, 1.9f, WOOD), b(0f, -0.04f, 0.35f, 0.9f, 0.22f, 0.3f, 0xFF4A7FB5.toInt()),
            b(0f, -0.04f, 0.9f, 0.9f, 0.22f, 0.3f, 0xFFB5533A.toInt()), b(0f, -0.04f, 1.4f, 0.9f, 0.22f, 0.3f, 0xFF3E7D3A.toInt()))
        FKind.DINING_SET -> {
            val out = mutableListOf<Box3>()
            for (sx in listOf(-0.82f, 0.82f)) for (sz in listOf(-0.38f, 0.38f)) out += b(sx, sz, 0f, 0.07f, 0.07f, 0.74f, WOOD)
            out += b(0f, 0f, 0.74f, 1.8f, 0.95f, 0.05f, WOOD)
            out += b(0f, 0f, 0.79f, 0.3f, 0.3f, 0.12f, WHITE)
            for (x in listOf(-0.5f, 0.5f)) { out += chair(x, 0.82f, 0f, 1f); out += chair(x, -0.82f, 0f, -1f) }
            out += chair(1.15f, 0f, 1f, 0f); out += chair(-1.15f, 0f, -1f, 0f)
            out
        }
        FKind.COUNTER -> listOf(
            b(0f, 0f, 0f, 2.4f, 0.6f, 0.85f, WHITE), b(0f, 0f, 0.85f, 2.4f, 0.62f, 0.04f, DARK),
            b(0f, -0.31f, 0.55f, 0.6f, 0.01f, 0.03f, STEEL), b(0f, 0.15f, 1.5f, 2.4f, 0.3f, 0.7f, CREAM),
            b(0.4f, -0.02f, 0.89f, 0.5f, 0.35f, 0.02f, STEEL))
        FKind.FRIDGE -> listOf(
            b(0f, 0f, 0f, 0.7f, 0.7f, 1.8f, STEEL), b(0f, -0.355f, 1.15f, 0.7f, 0.01f, 0.01f, DARK),
            b(0.25f, -0.37f, 1.2f, 0.03f, 0.03f, 0.4f, DARK), b(0.25f, -0.37f, 0.6f, 0.03f, 0.03f, 0.3f, DARK))
        FKind.TUB -> listOf(b(0f, 0f, 0f, 1.7f, 0.75f, 0.55f, WHITE), b(0f, 0f, 0.55f, 1.5f, 0.55f, 0.02f, 0xFF9CCCEB.toInt()),
            b(0.7f, 0.3f, 0.55f, 0.05f, 0.05f, 0.25f, STEEL))
        FKind.TOILET -> listOf(
            b(0f, 0.25f, 0f, 0.4f, 0.2f, 0.75f, WHITE), b(0f, -0.05f, 0f, 0.38f, 0.45f, 0.4f, WHITE),
            b(0f, -0.05f, 0.4f, 0.4f, 0.45f, 0.04f, CREAM))
        FKind.VANITY -> listOf(
            b(0f, 0f, 0f, 0.9f, 0.5f, 0.8f, WOOD), b(0f, 0f, 0.8f, 0.92f, 0.52f, 0.04f, WHITE),
            b(0f, 0f, 0.84f, 0.4f, 0.3f, 0.02f, 0xFFCFE6F5.toInt()), b(0f, 0.23f, 1.1f, 0.7f, 0.02f, 0.8f, 0xFFB5D8F0.toInt()))
    }

    private fun rotate(x: Float, z: Float, w: Float, d: Float, rot: Int): FloatArray = when (rot) {
        90 -> floatArrayOf(z, -x, d, w)
        180 -> floatArrayOf(-x, -z, w, d)
        270 -> floatArrayOf(-z, x, d, w)
        else -> floatArrayOf(x, z, w, d)
    }

    private fun clamp(v: Float, half: Float, limit: Float): Float =
        if (half * 2f >= limit) 0f else v.coerceIn(-limit / 2f + half, limit / 2f - half)

    /** World-space boxes for every furniture item of the project (empty when furniture is hidden). */
    fun layout(p: ProjectData): List<Box3> {
        if (!p.showFurniture || p.furniture.isEmpty()) return emptyList()
        val L = p.roomL.toFloat(); val W = p.roomW.toFloat()
        val out = ArrayList<Box3>()
        for (f in p.furniture) {
            val rot = ((f.rot % 360) + 360) % 360
            val swap = rot == 90 || rot == 270
            val fw = if (swap) f.kind.d else f.kind.w
            val fd = if (swap) f.kind.w else f.kind.d
            val cx = clamp((-L / 2f + f.fx.toFloat() * L), fw / 2f, L)
            val cz = clamp((-W / 2f + f.fz.toFloat() * W), fd / 2f, W)
            for (pt in parts(f.kind)) {
                val r = rotate(pt.cx, pt.cz, pt.w, pt.d, rot)
                out += Box3(cx + r[0], cz + r[1], pt.y0, r[2], r[3], pt.h, pt.color)
            }
        }
        return out
    }
}
