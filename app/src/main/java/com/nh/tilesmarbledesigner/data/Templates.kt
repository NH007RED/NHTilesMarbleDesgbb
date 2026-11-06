package com.nh.tilesmarbledesigner.data

data class RoomTemplate(val id: String, val name: String, val desc: String, val build: () -> ProjectData)

object Templates {
    private fun d(m: String, l: Double, w: Double, p: TilePattern = TilePattern.STRAIGHT, g: Double = 2.0, c: Int = 0xFFB8B8B8.toInt()) =
        SurfaceDesign(m, l, w, p, g, c)

    private fun wallsOf(vararg x: Pair<Surf, SurfaceDesign>, ceiling: SurfaceDesign = d("matte", 3.0, 3.0, g = 0.0)): Map<Surf, SurfaceDesign> =
        mapOf(Surf.CEILING to ceiling) + x.toMap()

    private fun room(name: String, l: Double, w: Double, h: Double, designs: Map<Surf, SurfaceDesign>, openings: List<Opening>, furniture: List<Furniture>) =
        ProjectData.default(LengthUnit.M).withRoom(l, w, h).copy(
            name = name, shape = RoomShape.RECTANGLE, designs = designs, openings = openings, furniture = furniture
        )

    private val plain = d("matte", 0.6, 1.2, g = 0.0)

    val all: List<RoomTemplate> = listOf(
        RoomTemplate("living", "Living Room", "Italian marble floor, marble feature wall, sofa, TV unit and rug.") {
            room("Living Room", 5.2, 4.4, 2.9,
                wallsOf(
                    Surf.FLOOR to d("italian_marble", 0.8, 0.8, c = 0xFFE8DCC8.toInt()),
                    Surf.BACK to d("white_marble", 1.2, 0.6, TilePattern.SLAB, 1.0, 0xFFFFFFFF.toInt()),
                    Surf.LEFT to plain, Surf.RIGHT to plain, Surf.FRONT to plain),
                listOf(Opening(1, false, 1.8, 1.4, 1, 0.5, 0.9), Opening(2, true, 1.0, 2.1, 1, 0.3), Opening(3, false, 1.2, 1.3, 1, 0.5, 0.9)),
                listOf(
                    Furniture(FKind.RUG, 0.5, 0.58), Furniture(FKind.SOFA, 0.5, 1.0, 0),
                    Furniture(FKind.COFFEE_TABLE, 0.5, 0.6), Furniture(FKind.TV_UNIT, 0.0, 0.5, 270),
                    Furniture(FKind.ARMCHAIR, 1.0, 0.45, 90), Furniture(FKind.PLANT, 1.0, 1.0),
                    Furniture(FKind.LAMP, 0.0, 1.0)))
        },
        RoomTemplate("bedroom", "Bedroom", "Wooden plank floor, accent wall, bed with nightstands, wardrobe and desk.") {
            room("Bedroom", 4.4, 3.8, 2.8,
                wallsOf(
                    Surf.FLOOR to d("wood", 1.2, 0.2, TilePattern.BRICK, 0.5, 0xFF3A2A1A.toInt()),
                    Surf.BACK to d("modern_wall", 0.6, 1.2, g = 0.0),
                    Surf.LEFT to plain, Surf.RIGHT to plain, Surf.FRONT to plain),
                listOf(Opening(1, false, 1.4, 1.3, 1, 0.65, 0.9), Opening(2, true, 0.95, 2.1, 1, 0.8)),
                listOf(
                    Furniture(FKind.RUG, 0.5, 0.55), Furniture(FKind.BED, 0.5, 1.0, 0),
                    Furniture(FKind.NIGHTSTAND, 0.17, 1.0, 0), Furniture(FKind.NIGHTSTAND, 0.83, 1.0, 0),
                    Furniture(FKind.WARDROBE, 0.0, 0.45, 270), Furniture(FKind.DESK, 1.0, 0.25, 90),
                    Furniture(FKind.CHAIR, 0.84, 0.25, 270), Furniture(FKind.PLANT, 0.0, 1.0)))
        },
        RoomTemplate("kitchen", "Kitchen", "Diagonal ceramic floor, glossy brick wall tiles, counters, fridge and dining set.") {
            room("Kitchen", 4.2, 3.4, 2.8,
                wallsOf(
                    Surf.FLOOR to d("ceramic", 0.6, 0.6, TilePattern.DIAGONAL, 3.0, 0xFF9A9A9A.toInt()),
                    Surf.BACK to d("glossy", 0.3, 0.6, TilePattern.BRICK, 2.0, 0xFFFFFFFF.toInt()),
                    Surf.LEFT to d("ceramic", 0.3, 0.6, TilePattern.BRICK, 2.0, 0xFFFFFFFF.toInt()),
                    Surf.RIGHT to d("ceramic", 0.3, 0.6, TilePattern.BRICK, 2.0, 0xFFFFFFFF.toInt()),
                    Surf.FRONT to plain),
                listOf(Opening(1, false, 1.2, 1.1, 1, 0.5, 1.0), Opening(2, true, 0.95, 2.1, 1, 0.22)),
                listOf(
                    Furniture(FKind.COUNTER, 0.45, 1.0, 0), Furniture(FKind.COUNTER, 0.0, 0.4, 270),
                    Furniture(FKind.FRIDGE, 1.0, 1.0, 0), Furniture(FKind.DINING_SET, 0.62, 0.3)))
        },
        RoomTemplate("bathroom", "Bathroom", "White marble slab walls, black glossy grid floor, tub, toilet and vanity.") {
            room("Bathroom", 2.8, 2.4, 2.6,
                wallsOf(
                    Surf.FLOOR to d("glossy", 0.3, 0.3, TilePattern.GRID, 2.0, 0xFF444444.toInt()),
                    Surf.BACK to d("white_marble", 1.2, 0.6, TilePattern.SLAB, 1.0, 0xFFFFFFFF.toInt()),
                    Surf.LEFT to d("white_marble", 1.2, 0.6, TilePattern.SLAB, 1.0, 0xFFFFFFFF.toInt()),
                    Surf.RIGHT to d("white_marble", 1.2, 0.6, TilePattern.SLAB, 1.0, 0xFFFFFFFF.toInt()),
                    Surf.FRONT to d("white_marble", 1.2, 0.6, TilePattern.SLAB, 1.0, 0xFFFFFFFF.toInt())),
                listOf(Opening(0, false, 0.8, 0.5, 1, 0.5, 1.6), Opening(2, true, 0.85, 2.1, 1, 0.3)),
                listOf(
                    Furniture(FKind.TUB, 0.5, 1.0, 0), Furniture(FKind.TOILET, 1.0, 0.3, 90),
                    Furniture(FKind.VANITY, 0.0, 0.4, 270), Furniture(FKind.PLANT, 1.0, 1.0)))
        },
        RoomTemplate("dining", "Dining Room", "Granite floor, Italian marble feature wall, dining set, sideboard and plants.") {
            room("Dining Room", 4.8, 4.0, 2.8,
                wallsOf(
                    Surf.FLOOR to d("granite", 0.6, 0.6, g = 3.0, c = 0xFF555555.toInt()),
                    Surf.BACK to d("italian_marble", 1.2, 0.6, TilePattern.SLAB, 1.0, 0xFFE8DCC8.toInt()),
                    Surf.LEFT to plain, Surf.RIGHT to plain, Surf.FRONT to plain),
                listOf(Opening(3, false, 1.6, 1.4, 1, 0.5, 0.9), Opening(1, false, 1.6, 1.4, 1, 0.5, 0.9), Opening(2, true, 1.0, 2.1, 1, 0.5)),
                listOf(
                    Furniture(FKind.RUG, 0.5, 0.5), Furniture(FKind.DINING_SET, 0.5, 0.5),
                    Furniture(FKind.SIDEBOARD, 0.5, 1.0, 0), Furniture(FKind.PLANT, 0.0, 1.0), Furniture(FKind.PLANT, 1.0, 1.0),
                    Furniture(FKind.LAMP, 0.0, 0.0)))
        }
    )
}
