package com.nh.tilesmarbledesigner.engine

import com.nh.tilesmarbledesigner.data.*
import kotlin.math.ceil

/** Tile / marble calculation engine. All inputs are metres / square metres. */
object Calc {
    fun ceilI(x: Double): Int = ceil(x - 1e-9).toInt().coerceAtLeast(0)

    data class TileResult(
        val netArea: Double, val tileArea: Double, val baseTiles: Int, val requiredTiles: Int,
        val boxes: Int, val purchased: Int, val extra: Int, val spare: Int, val wastagePct: Double
    )

    data class WallLine(val index: Int, val length: Double, val height: Double, val gross: Double, val openings: Double, val net: Double)

    data class Report(
        val unit: LengthUnit, val floorArea: Double, val floor: TileResult,
        val wallGross: Double, val openingsArea: Double, val wallNet: Double, val walls: List<WallLine>, val wall: TileResult,
        val floorSlabs: Int, val wallSlabs: Int, val totalBoxes: Int, val totalTiles: Int, val totalPurchasedArea: Double,
        val assumptions: List<String>
    )

    /** Floor Area / Tile Area, wastage applied before rounding, boxes always rounded up. */
    fun tiles(area: Double, t: TileSpec): TileResult {
        val ta = t.l * t.w
        if (area <= 0.0 || ta <= 0.0 || t.perBox < 1) return TileResult(area, ta, 0, 0, 0, 0, 0, t.spare, t.wastagePct)
        val base = ceilI(area / ta)
        val req = ceilI(area / ta * (1 + t.wastagePct / 100.0)) + t.spare
        val boxes = ceilI(req.toDouble() / t.perBox)
        val purchased = boxes * t.perBox
        return TileResult(area, ta, base, req, boxes, purchased, purchased - req, t.spare, t.wastagePct)
    }

    fun openingArea(p: ProjectData, wall: Int) =
        p.openings.filter { it.wall == wall }.sumOf { it.w * it.h * it.count }

    fun floorArea(p: ProjectData) = p.sections.sumOf { it.length * it.width }

    fun validate(p: ProjectData): List<String> {
        val e = mutableListOf<String>()
        p.sections.forEachIndexed { i, s ->
            if (s.length <= 0 || s.width <= 0) e += "Floor section ${i + 1}: length and width must be greater than 0."
            else if (s.length > 500 || s.width > 500) e += "Floor section ${i + 1}: dimensions look unrealistic (over 500 m)."
        }
        if (p.height <= 0 || p.height > 50) e += "Wall height must be greater than 0 and at most 50 m."
        p.walls.forEachIndexed { i, w ->
            if (w.length <= 0 || w.height <= 0) e += "Wall ${i + 1}: length and height must be greater than 0."
            else if (openingArea(p, i) > w.length * w.height + 1e-9) e += "Wall ${i + 1}: doors/windows are larger than the wall."
        }
        p.openings.forEachIndexed { i, o ->
            if (o.wall !in p.walls.indices) e += "Opening ${i + 1} refers to a wall that no longer exists."
            if (o.w <= 0 || o.h <= 0 || o.count < 1) e += "Opening ${i + 1}: size and count must be greater than 0."
        }
        listOf("Floor tile" to p.floorTile, "Wall tile" to p.wallTile).forEach { (n, t) ->
            if (t.l <= 0 || t.w <= 0) e += "$n: length and width must be greater than 0."
            if (t.perBox < 1) e += "$n: tiles per box must be at least 1."
            if (t.wastagePct < 0 || t.wastagePct > 100) e += "$n: wastage must be between 0 and 100 %."
            if (t.spare < 0) e += "$n: spare tiles cannot be negative."
        }
        if (p.slabL <= 0 || p.slabW <= 0) e += "Marble slab size must be greater than 0."
        return e
    }

    fun report(p: ProjectData): Report {
        val u = p.unit
        val fa = floorArea(p)
        val floor = tiles(fa, p.floorTile)
        val lines = p.walls.mapIndexed { i, w ->
            val g = w.length * w.height
            val o = openingArea(p, i).coerceAtMost(g)
            WallLine(i + 1, w.length, w.height, g, o, g - o)
        }
        val gross = lines.sumOf { it.gross }
        val op = lines.sumOf { it.openings }
        val net = gross - op
        val wall = tiles(net, p.wallTile)
        val slabArea = p.slabL * p.slabW
        val floorSlabs = ceilI(fa * (1 + p.floorTile.wastagePct / 100.0) / slabArea)
        val wallSlabs = ceilI(net * (1 + p.wallTile.wastagePct / 100.0) / slabArea)
        val assumptions = listOf(
            "All dimensions are converted to metres internally and shown in ${u.label}.",
            "Floor area = sum of ${p.sections.size} rectangular section(s); irregular rooms are split into sections.",
            "Wall area = sum of (length x height) of ${p.walls.size} wall(s), minus doors and windows. Ceiling is not included.",
            "Wastage % is added to the exact tile count before rounding up; spare tiles are added after.",
            "Boxes are always rounded up to the next whole box; extra tiles = tiles purchased - tiles required.",
            "Floor: ${p.floorTile.perBox} tiles per box. Wall: ${p.wallTile.perBox} tiles per box.",
            "Marble slabs are estimated by area with the same wastage; slab joints and veining match are not modelled."
        )
        return Report(
            u, fa, floor, gross, op, net, lines, wall, floorSlabs, wallSlabs,
            floor.boxes + wall.boxes, floor.purchased + wall.purchased,
            floor.purchased * floor.tileArea + wall.purchased * wall.tileArea, assumptions
        )
    }
}
