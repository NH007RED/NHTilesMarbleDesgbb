package com.nh.tilesmarbledesigner.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items as rowItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nh.tilesmarbledesigner.data.*
import com.nh.tilesmarbledesigner.engine.*

@Composable
fun CalculatorScreen(vm: AppViewModel, go: (String) -> Unit, back: () -> Unit) {
    val p = vm.draft; val u = p.unit
    val pal = LocalPalette.current
    val errors = remember(p) { Calc.validate(p) }
    val r = remember(p) { Calc.report(p) }
    ScreenFrame("Tile Calculator", back) {
        ChipRow(LengthUnit.values().toList(), u, { it.label }) { nu -> vm.update { it.copy(unit = nu) } }
        NHCard {
            SectionTitle("Room Size")
            StatRow("Floor area", "${fmt2(u.areaFromM2(r.floorArea))} ${u.areaLabel}", true)
            DimField("Room length", p.roomL, u) { v -> vm.update { q -> q.withRoom(v, if (q.shape == RoomShape.SQUARE) v else q.roomW, q.height) } }
            if (p.shape != RoomShape.SQUARE) DimField("Room width", p.roomW, u) { v -> vm.update { q -> q.withRoom(q.roomL, v, q.height) } }
        }
        NHCard {
            SectionTitle("Floor Tile Spec")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DimField("Tile length", p.floorTile.l, u, Modifier.weight(1f)) { v -> vm.update { it.copy(floorTile = it.floorTile.copy(l = v)) } }
                DimField("Tile width", p.floorTile.w, u, Modifier.weight(1f)) { v -> vm.update { it.copy(floorTile = it.floorTile.copy(w = v)) } }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IntField("Tiles per box", p.floorTile.perBox, 1, Modifier.weight(1f)) { v -> vm.update { it.copy(floorTile = it.floorTile.copy(perBox = v)) } }
                IntField("Spare tiles", p.floorTile.spare, 0, Modifier.weight(1f)) { v -> vm.update { it.copy(floorTile = it.floorTile.copy(spare = v)) } }
            }
            NumField("Wastage", p.floorTile.wastagePct, "w", "%", allowZero = true) { v -> vm.update { it.copy(floorTile = it.floorTile.copy(wastagePct = v)) } }
        }
        NHCard {
            SectionTitle("Wall Tile Spec")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DimField("Tile length", p.wallTile.l, u, Modifier.weight(1f)) { v -> vm.update { it.copy(wallTile = it.wallTile.copy(l = v)) } }
                DimField("Tile width", p.wallTile.w, u, Modifier.weight(1f)) { v -> vm.update { it.copy(wallTile = it.wallTile.copy(w = v)) } }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IntField("Tiles per box", p.wallTile.perBox, 1, Modifier.weight(1f)) { v -> vm.update { it.copy(wallTile = it.wallTile.copy(perBox = v)) } }
                IntField("Spare tiles", p.wallTile.spare, 0, Modifier.weight(1f)) { v -> vm.update { it.copy(wallTile = it.wallTile.copy(spare = v)) } }
            }
            NumField("Wastage", p.wallTile.wastagePct, "w", "%", allowZero = true) { v -> vm.update { it.copy(wallTile = it.wallTile.copy(wastagePct = v)) } }
            Text(
                "Net wall area: ${fmt2(u.areaFromM2(r.wallNet))} ${u.areaLabel} (after openings). Edit walls in Wall Measurement.",
                fontSize = 12.sp,
                color = pal.subAccent,
                fontWeight = FontWeight.SemiBold
            )
        }
        NHCard {
            SectionTitle("Marble / Granite Slab")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DimField("Slab length", p.slabL, u, Modifier.weight(1f)) { v -> vm.update { it.copy(slabL = v) } }
                DimField("Slab width", p.slabW, u, Modifier.weight(1f)) { v -> vm.update { it.copy(slabW = v) } }
            }
        }
        errors.forEach { Text("• $it", color = MaterialTheme.colorScheme.error, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
        if (errors.isEmpty()) NHCard {
            SectionTitle("Quick Estimate")
            StatRow("Floor boxes", "${r.floor.boxes}")
            StatRow("Total boxes (floor + wall)", "${r.totalBoxes}", true)
        }
        PrimaryButton("Calculate Full Report", enabled = errors.isEmpty()) { go("results") }
    }
}

@Composable
fun ResultsScreen(vm: AppViewModel, go: (String) -> Unit, back: () -> Unit) {
    val p = vm.draft; val u = p.unit
    val pal = LocalPalette.current
    val errors = remember(p) { Calc.validate(p) }
    ScreenFrame("Calculation Results", back) {
        if (errors.isNotEmpty()) {
            errors.forEach { Text("• $it", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) }
            PrimaryButton("Fix inputs") { back() }
            return@ScreenFrame
        }
        val r = remember(p) { Calc.report(p) }
        fun d(m: Double) = "${fmt(u.fromM(m))} ${u.label}"
        fun a(m2: Double) = "${fmt2(u.areaFromM2(m2))} ${u.areaLabel}"

        // Executive Highlight Banner
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NHCard(Modifier.weight(1f)) {
                Text("FLOOR BOXES", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = pal.subAccent)
                Text("${r.floor.boxes}", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = pal.metric)
                Text("${r.floor.purchased} tiles total", fontSize = 11.sp, color = pal.label, fontWeight = FontWeight.SemiBold)
            }
            NHCard(Modifier.weight(1f)) {
                Text("TOTAL BOXES", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = pal.accent)
                Text("${r.totalBoxes}", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = pal.metric)
                Text("${r.totalTiles} tiles (F+W)", fontSize = 11.sp, color = pal.label, fontWeight = FontWeight.SemiBold)
            }
        }

        NHCard {
            SectionTitle("Summary Overview")
            StatRow("Room size", if (p.sections.size == 1) "${d(p.roomL)} × ${d(p.roomW)}" else "${p.sections.size} sections")
            StatRow("Floor tile size", "${d(p.floorTile.l)} × ${d(p.floorTile.w)}")
            StatRow("Tiles per box", "${p.floorTile.perBox}")
            StatRow("Floor boxes", "${r.floor.boxes}", true)
            StatRow("Floor tiles purchased", "${r.floor.purchased}")
        }
        NHCard {
            SectionTitle("Floor Tiles Breakdown")
            StatRow("Floor area", a(r.floorArea))
            StatRow("Tiles for exact area", "${r.floor.baseTiles}")
            StatRow("Wastage", "${fmt(p.floorTile.wastagePct)} %   +${p.floorTile.spare} spare")
            StatRow("Tiles required", "${r.floor.requiredTiles}")
            StatRow("Boxes required", "${r.floor.boxes}", true)
            StatRow("Tiles purchased", "${r.floor.purchased}")
            StatRow("Extra tiles", "${r.floor.extra}")
        }
        NHCard {
            SectionTitle("Wall Tiles Breakdown")
            r.walls.forEach { StatRow("Wall ${it.index} (${d(it.length)} × ${d(it.height)})", a(it.net)) }
            StatRow("Gross wall area", a(r.wallGross))
            StatRow("Doors & windows", "- ${a(r.openingsArea)}")
            StatRow("Net wall area", a(r.wallNet))
            StatRow("Tiles required", "${r.wall.requiredTiles}")
            StatRow("Boxes required", "${r.wall.boxes}", true)
            StatRow("Tiles purchased", "${r.wall.purchased}  (extra ${r.wall.extra})")
        }
        NHCard {
            SectionTitle("Marble / Granite Slabs (${d(p.slabL)} × ${d(p.slabW)})")
            StatRow("Floor slabs", "${r.floorSlabs}", true)
            StatRow("Wall slabs", "${r.wallSlabs}")
        }
        NHCard {
            SectionTitle("Total Estimated Material")
            StatRow("Boxes (floor + wall)", "${r.totalBoxes}", true)
            StatRow("Tiles purchased", "${r.totalTiles}")
            StatRow("Purchased tile area", a(r.totalPurchasedArea))
        }
        NHCard {
            SectionTitle("Calculation Notes")
            r.assumptions.forEach { Text("• $it", fontSize = 12.sp, color = pal.label) }
        }
        PrimaryButton("Save Project") { vm.save() }
        GoldButton("Export Report (PDF & 3D)") { go("export") }
        GoldButton("Open 3D Design Studio") { go("studio/floor") }
    }
}

@Composable
fun LibraryScreen(vm: AppViewModel, go: (String) -> Unit, back: () -> Unit) {
    val custom by vm.customMaterials.collectAsState()
    val p = vm.draft
    val pal = LocalPalette.current
    val sel = vm.selectedSurface
    val current = p.design(sel).materialId
    val all = Materials.builtin.map { Triple(it.id, it.name, null as CustomMaterialEntity?) } +
        custom.map { Triple("custom:${it.path}", it.name, it as CustomMaterialEntity?) }
    ScreenFrame("Material Library", back, scroll = false) {
        NHCard {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Target Surface", color = pal.accent, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                Text("${all.size} Materials", color = pal.metric, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
            }
            ChipRow(Surf.values().toList(), sel, { it.label }) { vm.selectedSurface = it }
        }
        GoldButton("＋ Import Photo Texture (Atomic Crop)") { go("crop") }
        LazyVerticalGrid(
            GridCells.Fixed(3),
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(all) { (id, name, ent) ->
                Box(contentAlignment = Alignment.TopCenter) {
                    MaterialSwatch(id, name, id == current) {
                        vm.update { q -> q.copy(designs = q.designs + (sel to q.design(sel).copy(materialId = id))) }
                        vm.toast = "$name applied to ${sel.label}"
                    }
                    if (ent != null) IconButton({ vm.deleteMaterial(ent) }, Modifier.align(Alignment.TopEnd).size(28.dp)) {
                        Icon(Icons.Filled.Delete, "Delete", tint = Color(0xFFFF6B6B), modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
        PrimaryButton("Open ${sel.label} Studio") { go(if (sel == Surf.FLOOR) "studio/floor" else "studio/wall") }
    }
}

@Composable
fun StudioScreen(vm: AppViewModel, mode: String, go: (String) -> Unit) {
    val p = vm.draft; val u = p.unit
    val pal = LocalPalette.current
    val custom by vm.customMaterials.collectAsState()
    val surfaces = if (mode == "floor") listOf(Surf.FLOOR) else listOf(Surf.LEFT, Surf.RIGHT, Surf.FRONT, Surf.BACK, Surf.CEILING)
    val sel = if (vm.selectedSurface in surfaces) vm.selectedSurface else surfaces.first()
    val committed = p.design(sel)
    var pending by remember(sel, committed) { mutableStateOf(committed) }
    val previewDesigns = p.designs + (sel to pending)
    val bmps by rememberSceneBitmaps(p, previewDesigns)
    var yaw by remember { mutableFloatStateOf(0f) }
    var pitch by remember { mutableFloatStateOf(24f) }
    var zoom by remember { mutableFloatStateOf(1f) }
    val boxes = remember(p.furniture, p.showFurniture, p.sections) { FurnitureLib.layout(p) }
    val mats = Materials.builtin.map { it.id to it.name } + custom.map { "custom:${it.path}" to it.name }
    ScreenFrame(if (mode == "floor") "Floor Design Studio" else "Wall Design Studio") {
        // Quick Studio Mode Switcher Pill Bar
        Row(
            Modifier
                .fillMaxWidth()
                .softShadow(6.dp, RoundedCornerShape(18.dp))
                .background(pal.card, RoundedCornerShape(18.dp))
                .border(1.dp, pal.primary.copy(alpha = 0.25f), RoundedCornerShape(18.dp))
                .padding(5.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("floor" to "Floor Studio", "wall" to "Wall & Ceiling Studio").forEach { (m, lbl) ->
                val active = mode == m
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (active) Brush.horizontalGradient(listOf(pal.dark, pal.primary))
                            else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                        )
                        .clickable {
                            if (!active) {
                                vm.selectedSurface = if (m == "floor") Surf.FLOOR else Surf.BACK
                                go("studio/$m")
                            }
                        }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        lbl,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (active) Color.White else pal.subAccent
                    )
                }
            }
        }

        Box(Modifier.fillMaxWidth().height(265.dp)) {
            RoomCanvas(bmps, null, p, yaw, pitch, zoom, modifier = Modifier.fillMaxSize(), boxes = boxes) { pan, z ->
                yaw += pan.x * 0.3f; pitch = (pitch + pan.y * 0.3f).coerceIn(-80f, 80f); zoom = (zoom * z).coerceIn(0.5f, 3f)
            }
            Tag(
                "${sel.label}  •  ${Materials.nameOf(pending.materialId)}",
                Modifier.align(Alignment.TopStart).padding(12.dp)
            )
        }

        if (surfaces.size > 1) ChipRow(surfaces, sel, { it.label }) { vm.selectedSurface = it }

        NHCard {
            SectionTitle("Select Material")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                rowItems((0 until mats.size).toList()) { i ->
                    val (id, name) = mats[i]
                    MaterialSwatch(id, name, pending.materialId == id) { pending = pending.copy(materialId = id) }
                }
            }
        }

        NHCard {
            SectionTitle("Tile Size & Pattern")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DimField("Length", pending.tileL, u, Modifier.weight(1f)) { v -> pending = pending.copy(tileL = v) }
                DimField("Width", pending.tileW, u, Modifier.weight(1f)) { v -> pending = pending.copy(tileW = v) }
            }
            TextButton({
                val t = if (sel == Surf.FLOOR) p.floorTile else p.wallTile
                pending = pending.copy(tileL = t.l, tileW = t.w)
            }) { Text("Sync with Calculator Tile Size", color = pal.subAccent, fontWeight = FontWeight.ExtraBold) }
            ChipRow(TilePattern.values().toList(), pending.pattern, { it.label }) { pt ->
                pending = if (pt == TilePattern.SLAB) pending.copy(pattern = pt, tileL = p.slabL.coerceAtMost(2.4), tileW = p.slabW.coerceAtMost(1.2))
                else pending.copy(pattern = pt)
            }
        }

        NHCard {
            SectionTitle("Grout Width & Colour")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Grout joint width", fontSize = 13.sp, color = pal.label, fontWeight = FontWeight.SemiBold)
                Text("${fmt(pending.groutMm)} mm", fontSize = 14.sp, color = pal.metric, fontWeight = FontWeight.ExtraBold)
            }
            Slider(pending.groutMm.toFloat(), { pending = pending.copy(groutMm = it.toDouble()) }, valueRange = 0f..20f)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(0xFFB8B8B8, 0xFFFFFFFF, 0xFF2B2B2B, 0xFFD4AF37, 0xFF8B7355, 0xFFE8DCC8).forEach {
                    val c = it.toInt(); ColorDot(c, pending.groutColor == c) { pending = pending.copy(groutColor = c) }
                }
            }
        }

        PrimaryButton("Apply Design to ${sel.label}") {
            vm.selectedSurface = sel
            vm.update { it.copy(designs = it.designs + (sel to pending)) }
            vm.toast = "Design applied to ${sel.label}"
        }
        if (mode != "floor") GoldButton("Apply to All Four Walls") {
            vm.update { q -> q.copy(designs = q.designs + listOf(Surf.LEFT, Surf.RIGHT, Surf.FRONT, Surf.BACK).associateWith { pending }) }
            vm.toast = "Applied to all walls"
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton({ pending = committed }, Modifier.weight(1f)) { Text("Reset Surface", fontWeight = FontWeight.Bold) }
            OutlinedButton({
                val d = ProjectData.default(u).designs
                vm.update { it.copy(designs = d) }; vm.toast = "All surfaces reset"
            }, Modifier.weight(1f)) { Text("Reset All", fontWeight = FontWeight.Bold) }
        }
        val wi = Scene.wallIndex(sel)
        if (mode != "floor" && wi >= 0) NHCard {
            SectionTitle("Doors & Windows on ${sel.label}")
            val addOpening = { door: Boolean ->
                vm.update { q ->
                    val n = q.openings.count { it.wall == wi }
                    val pos = listOf(0.5, 0.22, 0.78)[n % 3]
                    q.copy(openings = q.openings + Opening(wi, door, if (door) 0.9 else 1.2, if (door) 2.1 else 1.2, 1, pos, if (door) 0.0 else 0.9))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton({ addOpening(true) }, Modifier.weight(1f)) { Icon(Icons.Filled.Add, null); Text(" Door", fontWeight = FontWeight.Bold) }
                OutlinedButton({ addOpening(false) }, Modifier.weight(1f)) { Icon(Icons.Filled.Add, null); Text(" Window", fontWeight = FontWeight.Bold) }
            }
            p.openings.withIndex().filter { it.value.wall == wi }.forEach { (idx, o) ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("${if (o.door) "Door" else "Window"} ${fmt(u.fromM(o.w))}×${fmt(u.fromM(o.h))} ${u.label}", Modifier.weight(1f), fontSize = 13.sp, color = pal.subAccent, fontWeight = FontWeight.SemiBold)
                    IconButton({ vm.update { q -> q.copy(openings = q.openings.filterIndexed { i, _ -> i != idx }) } }) { Icon(Icons.Filled.Delete, "Delete", tint = Color(0xFFFF6B6B)) }
                }
            }
            Text("Fine-tune size and position in Wall Measurement.", fontSize = 11.sp, color = pal.label)
        }
        GoldButton("Material Library") { vm.selectedSurface = sel; go("library") }
        GoldButton("Furniture & Room Templates") { go("furniture") }
        GoldButton("Room 3D Preview & Before/After") { go("preview") }
        GoldButton("Atomic Crop: Create Custom Texture") { vm.selectedSurface = sel; go("crop") }
    }
}
