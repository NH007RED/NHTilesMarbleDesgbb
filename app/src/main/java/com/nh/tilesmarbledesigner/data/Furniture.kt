package com.nh.tilesmarbledesigner.data

/** Furniture catalogue. w/d are the real-world footprint in metres (w along x, d along z; back of the piece is +z). */
enum class FKind(val label: String, val w: Float, val d: Float) {
    SOFA("Sofa", 2.1f, 0.9f), ARMCHAIR("Armchair", 0.9f, 0.9f), COFFEE_TABLE("Coffee table", 1.1f, 0.6f),
    TV_UNIT("TV unit", 1.8f, 0.4f), SIDEBOARD("Sideboard", 1.6f, 0.45f), RUG("Rug", 2.4f, 1.7f),
    PLANT("Plant", 0.5f, 0.5f), LAMP("Floor lamp", 0.35f, 0.35f),
    BED("Bed", 1.6f, 2.1f), NIGHTSTAND("Nightstand", 0.45f, 0.4f), WARDROBE("Wardrobe", 1.8f, 0.6f),
    DESK("Desk", 1.3f, 0.65f), CHAIR("Chair", 0.45f, 0.45f), BOOKSHELF("Bookshelf", 1.0f, 0.35f),
    DINING_SET("Dining set", 2.8f, 2.1f), COUNTER("Kitchen counter", 2.4f, 0.6f), FRIDGE("Fridge", 0.7f, 0.7f),
    TUB("Bathtub", 1.7f, 0.75f), TOILET("Toilet", 0.4f, 0.7f), VANITY("Vanity", 0.9f, 0.5f)
}

/**
 * fx / fz = position as a fraction of the room (fx 0 = left wall, fz 1 = back wall). The piece is clamped
 * so it always stays inside the room, so 0 or 1 means "against that wall".
 * rot: 0 = back against back wall, 90 = back to right wall, 180 = back to front wall, 270 = back to left wall.
 */
data class Furniture(val kind: FKind, val fx: Double, val fz: Double, val rot: Int = 0)
