package com.nh.tilesmarbledesigner.data

import org.json.JSONArray
import org.json.JSONObject

enum class LengthUnit(val label: String, val meters: Double) {
    FT("ft", 0.3048), IN("in", 0.0254), M("m", 1.0), CM("cm", 0.01);

    fun toM(v: Double) = v * meters
    fun fromM(m: Double) = m / meters
    fun areaFromM2(m2: Double) = m2 / (meters * meters)
    val areaLabel: String get() = "sq $label"

    companion object {
        fun of(s: String?) = values().firstOrNull { it.name == s } ?: FT
    }
}

enum class Surf(val label: String) {
    FLOOR("Floor"), LEFT("Left wall"), RIGHT("Right wall"), FRONT("Front wall"), BACK("Back wall"), CEILING("Ceiling")
}

enum class TilePattern(val label: String) {
    STRAIGHT("Straight"), DIAGONAL("Diagonal"), BRICK("Brick"), HERRINGBONE("Herringbone"), GRID("Grid"), SLAB("Large slab")
}

enum class RoomShape(val label: String) { SQUARE("Square"), RECTANGLE("Rectangle"), CUSTOM("Custom") }

/** All lengths in metres. */
data class SurfaceDesign(
    val materialId: String = "ceramic",
    val tileL: Double = 0.6,
    val tileW: Double = 0.6,
    val pattern: TilePattern = TilePattern.STRAIGHT,
    val groutMm: Double = 3.0,
    val groutColor: Int = 0xFFB8B8B8.toInt()
)

/** wall: 0=Back 1=Right 2=Front 3=Left. pos = centre position along the wall (0..1). sill = height above floor (m). */
data class Opening(
    val wall: Int, val door: Boolean, val w: Double, val h: Double, val count: Int = 1,
    val pos: Double = 0.5, val sill: Double = if (door) 0.0 else 0.9
)
data class Wall(val length: Double, val height: Double)
data class Section(val length: Double, val width: Double)
data class TileSpec(val l: Double, val w: Double, val perBox: Int, val wastagePct: Double, val spare: Int)

data class ProjectData(
    val name: String,
    val shape: RoomShape,
    val unit: LengthUnit,
    val sections: List<Section>,
    val height: Double,
    val walls: List<Wall>,
    val openings: List<Opening>,
    val floorTile: TileSpec,
    val wallTile: TileSpec,
    val slabL: Double,
    val slabW: Double,
    val designs: Map<Surf, SurfaceDesign>,
    val furniture: List<Furniture> = emptyList(),
    val showFurniture: Boolean = true
) {
    val roomL get() = sections.first().length
    val roomW get() = sections.first().width

    fun design(s: Surf) = designs[s] ?: SurfaceDesign()

    /** Sets main room size and re-syncs the first four walls (extra walls are kept). */
    fun withRoom(l: Double, w: Double, h: Double): ProjectData {
        val secs = listOf(Section(l, w)) + sections.drop(1)
        val ws = listOf(Wall(l, h), Wall(w, h), Wall(l, h), Wall(w, h)) + walls.drop(4)
        return copy(sections = secs, height = h, walls = ws)
    }

    fun toJson(): String = JSONObject().apply {
        put("name", name); put("shape", shape.name); put("unit", unit.name); put("height", height)
        put("sections", JSONArray().also { a -> sections.forEach { a.put(JSONObject().put("l", it.length).put("w", it.width)) } })
        put("walls", JSONArray().also { a -> walls.forEach { a.put(JSONObject().put("l", it.length).put("h", it.height)) } })
        put("openings", JSONArray().also { a ->
            openings.forEach { a.put(JSONObject().put("wall", it.wall).put("door", it.door).put("w", it.w).put("h", it.h).put("n", it.count).put("p", it.pos).put("s", it.sill)) }
        })
        put("floorTile", floorTile.js()); put("wallTile", wallTile.js())
        put("slabL", slabL); put("slabW", slabW)
        put("designs", JSONObject().also { o -> designs.forEach { (k, v) -> o.put(k.name, v.js()) } })
        put("furniture", JSONArray().also { a -> furniture.forEach { a.put(JSONObject().put("k", it.kind.name).put("x", it.fx).put("z", it.fz).put("r", it.rot)) } })
        put("showFurniture", showFurniture)
    }.toString()

    companion object {
        fun default(
            unit: LengthUnit = LengthUnit.FT, wastage: Double = 0.0,
            groutMm: Double = 3.0, groutColor: Int = 0xFFB8B8B8.toInt()
        ): ProjectData {
            val imperial = unit == LengthUnit.FT || unit == LengthUnit.IN
            val side = if (imperial) 3.048 else 3.0
            val tile = if (imperial) 0.3048 else 0.3
            fun d(m: String, l: Double, w: Double, g: Double = groutMm) =
                SurfaceDesign(m, l, w, TilePattern.STRAIGHT, g, groutColor)
            return ProjectData(
                "New Room", RoomShape.RECTANGLE, unit, listOf(Section(side, side)), side,
                List(4) { Wall(side, side) }, emptyList(),
                TileSpec(tile, tile, 8, wastage, 0), TileSpec(tile, tile * 2, 6, wastage, 0),
                if (imperial) 2.4384 else 2.4, if (imperial) 1.2192 else 1.2,
                mapOf(
                    Surf.FLOOR to d("white_marble", 0.6, 0.6),
                    Surf.LEFT to d("matte", 0.6, 1.2, 0.0), Surf.RIGHT to d("matte", 0.6, 1.2, 0.0),
                    Surf.FRONT to d("matte", 0.6, 1.2, 0.0), Surf.BACK to d("modern_wall", 0.6, 1.2, 0.0),
                    Surf.CEILING to d("matte", 3.0, 3.0, 0.0)
                )
            )
        }

        fun fromJson(s: String): ProjectData {
            val o = JSONObject(s)
            val def = default(LengthUnit.of(o.optString("unit")))
            fun <T> JSONArray?.mapObj(f: (JSONObject) -> T): List<T> =
                if (this == null) emptyList() else (0 until length()).map { f(getJSONObject(it)) }
            val designs = Surf.values().associateWith { k ->
                o.optJSONObject("designs")?.optJSONObject(k.name)?.toDesign() ?: def.design(k)
            }
            val sections = o.optJSONArray("sections").mapObj { Section(it.getDouble("l"), it.getDouble("w")) }
                .ifEmpty { def.sections }
            return ProjectData(
                o.optString("name", "Room"),
                runCatching { RoomShape.valueOf(o.optString("shape")) }.getOrDefault(RoomShape.RECTANGLE),
                LengthUnit.of(o.optString("unit")), sections, o.optDouble("height", def.height),
                o.optJSONArray("walls").mapObj { Wall(it.getDouble("l"), it.getDouble("h")) }.ifEmpty { def.walls },
                o.optJSONArray("openings").mapObj {
                    Opening(it.getInt("wall"), it.getBoolean("door"), it.getDouble("w"), it.getDouble("h"), it.optInt("n", 1),
                        it.optDouble("p", 0.5), it.optDouble("s", if (it.getBoolean("door")) 0.0 else 0.9))
                },
                o.optJSONObject("floorTile")?.toTile() ?: def.floorTile,
                o.optJSONObject("wallTile")?.toTile() ?: def.wallTile,
                o.optDouble("slabL", def.slabL), o.optDouble("slabW", def.slabW), designs,
                furniture = o.optJSONArray("furniture").mapObj { j ->
                    val k = runCatching { FKind.valueOf(j.getString("k")) }.getOrNull()
                    if (k == null) null else Furniture(k, j.optDouble("x", 0.5), j.optDouble("z", 0.5), j.optInt("r", 0))
                }.filterNotNull(),
                showFurniture = o.optBoolean("showFurniture", true)
            )
        }
    }
}

private fun TileSpec.js() = JSONObject().put("l", l).put("w", w).put("box", perBox).put("wp", wastagePct).put("sp", spare)
private fun JSONObject.toTile() = TileSpec(getDouble("l"), getDouble("w"), optInt("box", 1), optDouble("wp", 0.0), optInt("sp", 0))
private fun SurfaceDesign.js() = JSONObject().put("m", materialId).put("l", tileL).put("w", tileW)
    .put("p", pattern.name).put("g", groutMm).put("c", groutColor)

private fun JSONObject.toDesign() = SurfaceDesign(
    optString("m", "ceramic"), optDouble("l", 0.6), optDouble("w", 0.6),
    runCatching { TilePattern.valueOf(optString("p")) }.getOrDefault(TilePattern.STRAIGHT),
    optDouble("g", 3.0), optInt("c", 0xFFB8B8B8.toInt())
)
