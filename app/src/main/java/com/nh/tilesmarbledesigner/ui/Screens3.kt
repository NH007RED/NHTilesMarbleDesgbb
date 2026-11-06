package com.nh.tilesmarbledesigner.ui

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items as rowItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nh.tilesmarbledesigner.data.*
import com.nh.tilesmarbledesigner.engine.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.hypot
import kotlin.math.min

@Composable
fun CropScreen(vm: AppViewModel, go: (String) -> Unit, back: () -> Unit) {
    val ctx = LocalContext.current
    val pal = LocalPalette.current
    val scope = rememberCoroutineScope()
    val src = vm.cropSource
    var quad by remember(src) { mutableStateOf(src?.let { CropEngine.defaultQuad(it) } ?: FloatArray(8)) }
    var zoom by remember(src) { mutableFloatStateOf(1f) }
    var pan by remember(src) { mutableStateOf(Offset.Zero) }
    var angle by remember(src) { mutableFloatStateOf(0f) }
    var busy by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            busy = true
            val b = withContext(Dispatchers.IO) { CropEngine.decode(ctx, uri) }
            if (b == null) vm.toast = "Could not open this image" else vm.cropSource = b
            busy = false
        }
    }
    val pick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }

    ScreenFrame("Atomic Crop Editor", back) {
        if (src == null) {
            NHCard {
                SectionTitle("Import Surface Photo")
                Text(
                    "Import a photo of marble, ceramic tile, granite, or wood, then drag the four corners to isolate the surface. Perspective distortion is corrected automatically.",
                    fontSize = 13.sp,
                    color = pal.label
                )
                PrimaryButton(if (busy) "Opening…" else "Import Image from Gallery", enabled = !busy) { pick() }
            }
            return@ScreenFrame
        }
        val iw = src.width; val ih = src.height
        val cropShape = RoundedCornerShape(24.dp)
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(360.dp)
                .softShadow(14.dp, cropShape, glow = true)
                .clip(cropShape)
                .background(Color(0xFF070B14))
                .border(1.5.dp, Brush.linearGradient(listOf(pal.vivid, pal.primary, pal.subAccent)), cropShape)
                .pointerInput(src) {
                    var drag = -1
                    detectDragGestures(
                        onDragStart = { o ->
                            val fit = min(size.width / iw.toFloat(), size.height / ih.toFloat()) * zoom
                            val ox = size.width / 2f + pan.x - iw * fit / 2f; val oy = size.height / 2f + pan.y - ih * fit / 2f
                            var best = -1; var bd = 70f
                            for (i in 0 until 4) {
                                val d = hypot(quad[i * 2] * fit + ox - o.x, quad[i * 2 + 1] * fit + oy - o.y)
                                if (d < bd) { bd = d; best = i }
                            }
                            drag = best
                        },
                        onDrag = { ch, amt ->
                            ch.consume()
                            val fit = min(size.width / iw.toFloat(), size.height / ih.toFloat()) * zoom
                            if (drag >= 0) {
                                val q = quad.copyOf()
                                q[drag * 2] = (q[drag * 2] + amt.x / fit).coerceIn(0f, iw.toFloat())
                                q[drag * 2 + 1] = (q[drag * 2 + 1] + amt.y / fit).coerceIn(0f, ih.toFloat())
                                quad = q
                            } else pan += amt
                        }
                    )
                }
        ) {
            val fit = min(size.width / iw, size.height / ih) * zoom
            val ox = size.width / 2f + pan.x - iw * fit / 2f; val oy = size.height / 2f + pan.y - ih * fit / 2f
            drawImage(src.asImageBitmap(), IntOffset.Zero, IntSize(iw, ih), IntOffset(ox.toInt(), oy.toInt()), IntSize((iw * fit).toInt(), (ih * fit).toInt()))
            val pts = (0 until 4).map { Offset(quad[it * 2] * fit + ox, quad[it * 2 + 1] * fit + oy) }
            val path = Path().apply { moveTo(pts[0].x, pts[0].y); pts.drop(1).forEach { lineTo(it.x, it.y) }; close() }
            drawPath(path, Color(0x2800E5FF))
            drawPath(path, Color(0xFF00E5FF), style = Stroke(4.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 10f))))
            pts.forEach {
                drawCircle(Color.White, 17f, it)
                drawCircle(Color(0xFF1F6FFF), 17f, it, style = Stroke(5.5f))
            }
        }
        Text(
            "Drag any corner handle to warp/select the tile. Drag inside to pan.",
            fontSize = 12.sp,
            color = pal.subAccent,
            fontWeight = FontWeight.SemiBold
        )
        NHCard {
            SectionTitle("Transform Controls")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Zoom", Modifier.width(56.dp), fontWeight = FontWeight.Bold, color = pal.accent)
                Slider(zoom, { zoom = it }, Modifier.weight(1f), valueRange = 0.5f..4f)
                Text("${fmt2(zoom.toDouble())}×", color = pal.metric, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Rotate", Modifier.width(56.dp), fontWeight = FontWeight.Bold, color = pal.accent)
                Slider(angle, { v -> quad = CropEngine.rotateQuad(quad, v - angle, iw, ih); angle = v }, Modifier.weight(1f), valueRange = -45f..45f)
                Text("${angle.toInt()}°", color = pal.metric, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton({ quad = CropEngine.autoQuad(src); angle = 0f }, Modifier.weight(1f)) { Icon(Icons.Filled.AutoFixHigh, null); Text(" Auto", fontWeight = FontWeight.Bold) }
                OutlinedButton({ vm.cropSource = CropEngine.rotate(src, 90f) }, Modifier.weight(1f)) { Icon(Icons.Filled.RotateRight, null); Text(" 90°", fontWeight = FontWeight.Bold) }
                OutlinedButton({ quad = CropEngine.defaultQuad(src); zoom = 1f; pan = Offset.Zero; angle = 0f }, Modifier.weight(1f)) { Icon(Icons.Filled.Refresh, null); Text(" Reset", fontWeight = FontWeight.Bold) }
            }
        }
        GoldButton("Choose Another Image") { pick() }
        PrimaryButton("Crop & Preview Texture", enabled = !busy) {
            scope.launch {
                busy = true
                try { vm.cropResult = withContext(Dispatchers.Default) { CropEngine.warp(src, quad) }; go("texture") }
                catch (e: Throwable) { vm.toast = "Crop failed: try a smaller selection" }
                busy = false
            }
        }
    }
}

@Composable
fun TextureScreen(vm: AppViewModel, go: (String) -> Unit, back: () -> Unit) {
    val bmp = vm.cropResult
    val pal = LocalPalette.current
    var name by remember { mutableStateOf("My texture") }
    var grout by remember { mutableDoubleStateOf(vm.draft.design(vm.selectedSurface).groutMm) }
    val sel = vm.selectedSurface
    ScreenFrame("Texture Preview", back) {
        if (bmp == null) { Text("No cropped texture yet. Use the Atomic Crop Editor first."); PrimaryButton("Open crop editor") { back() }; return@ScreenFrame }
        NHCard {
            SectionTitle("Extracted Single Tile")
            Image(
                bmp.asImageBitmap(),
                null,
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 210.dp)
                    .softShadow(8.dp, RoundedCornerShape(18.dp))
                    .clip(RoundedCornerShape(18.dp)),
                contentScale = ContentScale.Fit
            )
        }
        NHCard {
            SectionTitle("Seamless Repeating Grid")
            Text(
                "Each tile preserves its natural aspect ratio without stretching.",
                fontSize = 12.sp,
                color = pal.subAccent,
                fontWeight = FontWeight.SemiBold
            )
            val tiled by produceState<Bitmap?>(null, bmp, grout) {
                value = withContext(Dispatchers.Default) {
                    PatternComposer.compose(bmp, true, SurfaceDesign("custom:preview", 0.34, 0.34, TilePattern.STRAIGHT, grout, vm.draft.design(sel).groutColor), 1.0, 1.0, 512)
                }
            }
            tiled?.let {
                Image(
                    it.asImageBitmap(),
                    null,
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .softShadow(10.dp, RoundedCornerShape(18.dp))
                        .clip(RoundedCornerShape(18.dp))
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Grout preview width", fontSize = 13.sp, color = pal.label, fontWeight = FontWeight.SemiBold)
                Text("${fmt(grout)} mm", fontSize = 14.sp, color = pal.metric, fontWeight = FontWeight.ExtraBold)
            }
            Slider(grout.toFloat(), { grout = it.toDouble() }, valueRange = 0f..20f)
        }
        NHCard {
            SectionTitle("Save & Apply")
            OutlinedTextField(
                name,
                { name = it },
                label = { Text("Material name", fontWeight = FontWeight.SemiBold) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().softShadow(4.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = fieldColors()
            )
            Text("Apply to surface", color = pal.subAccent, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
            ChipRow(Surf.values().toList(), sel, { it.label }) { vm.selectedSurface = it }
        }
        PrimaryButton("Save & Apply to ${sel.label}") {
            vm.saveCustomMaterial(name, bmp, sel, grout); go(if (sel == Surf.FLOOR) "studio/floor" else "studio/wall")
        }
        GoldButton("Save to Library Only") { vm.saveCustomMaterial(name, bmp, null) }
    }
}

@Composable
private fun KeyedDimField(
    label: String,
    meters: Double,
    unit: LengthUnit,
    key: Any?,
    modifier: Modifier = Modifier,
    onMeters: (Double) -> Unit
) = NumField(label, unit.fromM(meters), listOf(unit, key), unit.label, modifier) { onMeters(unit.toM(it)) }

@Composable
fun PreviewScreen(vm: AppViewModel, go: (String) -> Unit, back: () -> Unit) {
    val p = vm.draft
    val u = p.unit
    val pal = LocalPalette.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val custom by vm.customMaterials.collectAsState()

    var yaw by remember { mutableFloatStateOf(0f) }
    var pitch by remember { mutableFloatStateOf(24f) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var compare by remember { mutableStateOf(false) }
    var split by remember { mutableFloatStateOf(0.5f) }

    // 0 = Wall/Floor Tiles, 1 = Door/Window, 2 = Wall/Floor Size
    var quickTab by remember { mutableIntStateOf(0) }
    var targetWall by remember { mutableIntStateOf(0) }
    var showAddOpeningDialog by remember { mutableStateOf(false) }

    val selSurf = vm.selectedSurface
    val curDesign = p.design(selSurf)
    val mats = remember(custom) {
        Materials.builtin.map { it.id to it.name } + custom.map { "custom:${it.path}" to it.name }
    }

    val after by rememberSceneBitmaps(p, p.designs)
    val before by rememberSceneBitmaps(p, Scene.before(), compare)
    val boxes = remember(p.furniture, p.showFurniture, p.sections) { FurnitureLib.layout(p) }

    ScreenFrame("Room 3D Preview", back, actions = {
        IconButton({ go("fullview") }) { Icon(Icons.Filled.Fullscreen, "Full screen", tint = pal.subAccent) }
    }) {
        Box(Modifier.fillMaxWidth().height(325.dp)) {
            RoomCanvas(after, if (compare) before else null, p, yaw, pitch, zoom, split, Modifier.fillMaxSize(), boxes) { pan, z ->
                yaw += pan.x * 0.3f; pitch = (pitch + pan.y * 0.3f).coerceIn(-80f, 80f); zoom = (zoom * z).coerceIn(0.5f, 3f)
            }
            if (compare) {
                Tag("AFTER", Modifier.align(Alignment.TopStart).padding(12.dp))
                Tag("BEFORE", Modifier.align(Alignment.TopEnd).padding(12.dp))
            } else {
                Tag(
                    "Angle ${yaw.toInt()}°  •  Zoom ${fmt2(zoom.toDouble())}×",
                    Modifier.align(Alignment.TopStart).padding(12.dp)
                )
            }
        }
        Text(
            "Drag to rotate 360°  •  Pinch to zoom  •  Tap Full Screen for walkthrough",
            fontSize = 12.sp,
            color = pal.subAccent,
            fontWeight = FontWeight.SemiBold
        )
        PrimaryButton("Launch Full Screen 3D Walkthrough") { go("fullview") }

        NHCard {
            SectionTitle("Camera & Comparison Controls")
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Before / After Split", Modifier.weight(1f), fontWeight = FontWeight.Bold, color = pal.ink)
                Switch(compare, { compare = it })
            }
            if (compare) Slider(split, { split = it }, Modifier.fillMaxWidth(), valueRange = 0.05f..0.95f)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Show 3D Furniture", Modifier.weight(1f), fontWeight = FontWeight.Bold, color = pal.ink)
                Switch(p.showFurniture, { v -> vm.update { it.copy(showFurniture = v) } })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton({ yaw = 0f; pitch = 24f; zoom = 1f }, Modifier.weight(1f)) { Text("Reset Camera", fontWeight = FontWeight.Bold) }
                OutlinedButton({ yaw = 90f * ((yaw / 90f).toInt() + 1) }, Modifier.weight(1f)) { Text("Rotate 90°", fontWeight = FontWeight.Bold) }
            }
        }

        NHCard {
            SectionTitle("Capture Current 3D Angle")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton({
                    val y = yaw; val pi = pitch; val z = zoom
                    scope.launch {
                        val path = withContext(Dispatchers.Default) {
                            val b = Scene.render(p, p.designs, 1920, 1440, y, pi, z, 1280)
                            try { Exporter.saveToGallery(ctx, b, p.name + "_3dview") } finally { b.recycle() }
                        }
                        vm.toast = if (path != null) "3D image saved to $path" else "Could not save image"
                    }
                }, Modifier.weight(1f)) { Icon(Icons.Filled.Download, null); Text(" Save Gallery", fontWeight = FontWeight.Bold) }
                OutlinedButton({
                    val y = yaw; val pi = pitch; val z = zoom
                    scope.launch {
                        val f = withContext(Dispatchers.Default) {
                            val b = Scene.render(p, p.designs, 1920, 1440, y, pi, z, 1280)
                            try { Exporter.savePng(ctx, b, p.name + "_3dview") } finally { b.recycle() }
                        }
                        Exporter.share(ctx, f, "image/png")
                    }
                }, Modifier.weight(1f)) { Icon(Icons.Filled.Share, null); Text(" Share HD", fontWeight = FontWeight.Bold) }
            }
        }

        // =========================================================================
        // UPGRADED QUICK MATERIAL, DOOR/WINDOW & WALL/FLOOR SIZE LIVE EDITOR CARD
        // =========================================================================
        NHCard {
            SectionTitle("Quick Material & Room Editor")
            Text(
                "Edit Wall/Floor Tiles, Doors/Windows, or Wall/Floor Sizes live while watching the 3D preview above:",
                fontSize = 12.sp,
                color = pal.subAccent,
                fontWeight = FontWeight.SemiBold
            )

            // 3-Tab Pill Switcher
            val tabItems = listOf(
                Triple(0, "Wall / Floor Tiles", Icons.Filled.GridView),
                Triple(1, "Door / Window", Icons.Filled.MeetingRoom),
                Triple(2, "Wall / Floor Size", Icons.Filled.Straighten)
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .softShadow(6.dp, RoundedCornerShape(18.dp))
                    .background(pal.chip.copy(alpha = 0.75f), RoundedCornerShape(18.dp))
                    .border(1.dp, pal.primary.copy(alpha = 0.28f), RoundedCornerShape(18.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                tabItems.forEach { (idx, title, ic) ->
                    val active = quickTab == idx
                    val pillShape = RoundedCornerShape(14.dp)
                    Box(
                        Modifier
                            .weight(1f)
                            .then(
                                if (active) Modifier
                                    .softShadow(6.dp, pillShape, glow = true)
                                    .background(Brush.horizontalGradient(listOf(pal.dark, pal.primary)), pillShape)
                                else Modifier
                            )
                            .clip(pillShape)
                            .clickable { quickTab = idx }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Icon(
                                ic,
                                contentDescription = title,
                                tint = if (active) Color.White else pal.accent,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                title,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (active) Color.White else pal.subAccent,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            when (quickTab) {
                // -----------------------------------------------------------------
                // TAB 0: WALL / FLOOR TILES LIVE EDITOR
                // -----------------------------------------------------------------
                0 -> {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Select Surface", fontWeight = FontWeight.ExtraBold, color = pal.accent, fontSize = 13.sp)
                        Text(Materials.nameOf(curDesign.materialId), fontWeight = FontWeight.ExtraBold, color = pal.metric, fontSize = 12.sp)
                    }
                    ChipRow(Surf.values().toList(), selSurf, { it.label }) { vm.selectedSurface = it }

                    Text("Tap Material to Apply Live", fontWeight = FontWeight.Bold, color = pal.subAccent, fontSize = 12.sp)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        rowItems((0 until mats.size).toList()) { i ->
                            val (id, name) = mats[i]
                            MaterialSwatch(id, name, curDesign.materialId == id) {
                                vm.update { q -> q.copy(designs = q.designs + (selSurf to q.design(selSurf).copy(materialId = id))) }
                            }
                        }
                    }

                    Text("Tile Size (${u.label}) & Pattern", fontWeight = FontWeight.ExtraBold, color = pal.accent, fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        KeyedDimField("Tile length", curDesign.tileL, u, selSurf, Modifier.weight(1f)) { v ->
                            vm.update { q -> q.copy(designs = q.designs + (selSurf to q.design(selSurf).copy(tileL = v))) }
                        }
                        KeyedDimField("Tile width", curDesign.tileW, u, selSurf, Modifier.weight(1f)) { v ->
                            vm.update { q -> q.copy(designs = q.designs + (selSurf to q.design(selSurf).copy(tileW = v))) }
                        }
                    }
                    ChipRow(TilePattern.values().toList(), curDesign.pattern, { it.label }) { pt ->
                        vm.update { q ->
                            val old = q.design(selSurf)
                            val next = if (pt == TilePattern.SLAB) old.copy(pattern = pt, tileL = q.slabL.coerceAtMost(2.4), tileW = q.slabW.coerceAtMost(1.2))
                            else old.copy(pattern = pt)
                            q.copy(designs = q.designs + (selSurf to next))
                        }
                    }

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Grout Width & Colour", fontWeight = FontWeight.Bold, color = pal.label, fontSize = 12.sp)
                        Text("${fmt(curDesign.groutMm)} mm", fontWeight = FontWeight.ExtraBold, color = pal.metric, fontSize = 13.sp)
                    }
                    Slider(
                        value = curDesign.groutMm.toFloat(),
                        onValueChange = { g -> vm.update { q -> q.copy(designs = q.designs + (selSurf to q.design(selSurf).copy(groutMm = g.toDouble()))) } },
                        valueRange = 0f..20f
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf(0xFFB8B8B8, 0xFFFFFFFF, 0xFF2B2B2B, 0xFFD4AF37, 0xFF8B7355, 0xFFE8DCC8).forEach {
                            val c = it.toInt()
                            ColorDot(c, curDesign.groutColor == c) {
                                vm.update { q -> q.copy(designs = q.designs + (selSurf to q.design(selSurf).copy(groutColor = c))) }
                            }
                        }
                    }

                    if (selSurf != Surf.FLOOR) {
                        GoldButton("Apply ${selSurf.label} Tile to All 4 Walls") {
                            val d = p.design(selSurf)
                            vm.update { q -> q.copy(designs = q.designs + listOf(Surf.LEFT, Surf.RIGHT, Surf.FRONT, Surf.BACK).associateWith { d }) }
                            vm.toast = "Applied to all 4 walls"
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton({ vm.selectedSurface = Surf.FLOOR; go("studio/floor") }, Modifier.weight(1f)) {
                            Icon(Icons.Filled.GridView, null, Modifier.size(16.dp))
                            Text(" Floor Studio", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton({ vm.selectedSurface = Surf.BACK; go("studio/wall") }, Modifier.weight(1f)) {
                            Icon(Icons.Filled.Layers, null, Modifier.size(16.dp))
                            Text(" Wall Studio", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton({ go("library") }, Modifier.weight(1f)) {
                            Icon(Icons.Filled.Image, null, Modifier.size(16.dp))
                            Text(" Material Library", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton({ go("crop") }, Modifier.weight(1f)) {
                            Icon(Icons.Filled.Crop, null, Modifier.size(16.dp))
                            Text(" Atomic Crop", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // -----------------------------------------------------------------
                // TAB 1: DOOR / WINDOW LIVE ADD & EDIT
                // -----------------------------------------------------------------
                1 -> {
                    val safeWall = targetWall.coerceIn(0, (p.walls.size - 1).coerceAtLeast(0))
                    Text("Target Wall for New Door / Window", fontWeight = FontWeight.ExtraBold, color = pal.accent, fontSize = 13.sp)
                    ChipRow(p.walls.indices.toList(), safeWall, { "Wall ${it + 1} (${Scene.wallName(it)})" }) { targetWall = it }

                    val quickAddOpening = { isDoor: Boolean ->
                        vm.update { q ->
                            val wIdx = safeWall.coerceIn(0, (q.walls.size - 1).coerceAtLeast(0))
                            val n = q.openings.count { it.wall == wIdx }
                            val pos = listOf(0.5, 0.22, 0.78)[n % 3]
                            q.copy(
                                openings = q.openings + Opening(
                                    wall = wIdx,
                                    door = isDoor,
                                    w = if (isDoor) 0.9 else 1.2,
                                    h = if (isDoor) 2.1 else 1.2,
                                    count = 1,
                                    pos = pos,
                                    sill = if (isDoor) 0.0 else 0.9
                                )
                            )
                        }
                        vm.toast = "${if (isDoor) "Door" else "Window"} added on Wall ${safeWall + 1}"
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton({ quickAddOpening(true) }, Modifier.weight(1f)) {
                            Icon(Icons.Filled.Add, null, Modifier.size(16.dp))
                            Text(" Add Door", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        OutlinedButton({ quickAddOpening(false) }, Modifier.weight(1f)) {
                            Icon(Icons.Filled.Add, null, Modifier.size(16.dp))
                            Text(" Add Window", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                    GoldButton("＋ Custom Door / Window (Full Options)") { showAddOpeningDialog = true }

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Doors & Windows in Room (${p.openings.size})", fontWeight = FontWeight.ExtraBold, color = pal.accent, fontSize = 13.sp)
                        if (p.openings.isNotEmpty()) {
                            TextButton({ vm.update { it.copy(openings = emptyList()) } }) {
                                Text("Clear All", color = Danger, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                            }
                        }
                    }

                    if (p.openings.isEmpty()) {
                        Text(
                            "No doors or windows yet. Tap '+ Add Door' or '+ Add Window' above to place one on the 3D walls.",
                            fontSize = 12.sp,
                            color = pal.label
                        )
                    }

                    p.openings.forEachIndexed { idx, o ->
                        val itemShape = RoundedCornerShape(16.dp)
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .softShadow(5.dp, itemShape)
                                .background(pal.chip.copy(alpha = 0.65f), itemShape)
                                .border(1.dp, pal.primary.copy(alpha = 0.25f), itemShape)
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "${if (o.door) "Door" else "Window"} #${idx + 1}  •  Wall ${o.wall + 1} (${Scene.wallName(o.wall)})",
                                    fontWeight = FontWeight.ExtraBold,
                                    color = pal.accent,
                                    fontSize = 13.5.sp
                                )
                                IconButton(
                                    onClick = { vm.update { q -> q.copy(openings = q.openings.filterIndexed { i, _ -> i != idx }) } },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(Icons.Filled.Delete, "Delete", tint = Color(0xFFFF6B6B), modifier = Modifier.size(18.dp))
                                }
                            }

                            ChipRow(listOf(true, false), o.door, { if (it) "Door" else "Window" }) { isDoor ->
                                vm.update { q ->
                                    val list = q.openings.toMutableList()
                                    list[idx] = o.copy(door = isDoor, sill = if (isDoor) 0.0 else if (o.sill <= 0.0) 0.9 else o.sill)
                                    q.copy(openings = list)
                                }
                            }

                            ChipRow(p.walls.indices.toList(), o.wall.coerceIn(0, (p.walls.size - 1).coerceAtLeast(0)), { "Wall ${it + 1} (${Scene.wallName(it)})" }) { newW ->
                                vm.update { q ->
                                    val list = q.openings.toMutableList()
                                    list[idx] = o.copy(wall = newW)
                                    q.copy(openings = list)
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                KeyedDimField("Width", o.w, u, "ow_${idx}_${o.door}", Modifier.weight(1f)) { v ->
                                    vm.update { q ->
                                        val list = q.openings.toMutableList()
                                        list[idx] = list[idx].copy(w = v)
                                        q.copy(openings = list)
                                    }
                                }
                                KeyedDimField("Height", o.h, u, "oh_${idx}_${o.door}", Modifier.weight(1f)) { v ->
                                    vm.update { q ->
                                        val list = q.openings.toMutableList()
                                        list[idx] = list[idx].copy(h = v)
                                        q.copy(openings = list)
                                    }
                                }
                            }

                            if (!o.door) {
                                NumField(
                                    label = "Sill height (above floor)",
                                    value = u.fromM(o.sill),
                                    key = listOf(u, idx, "sill"),
                                    suffix = u.label,
                                    allowZero = true
                                ) { v ->
                                    vm.update { q ->
                                        val list = q.openings.toMutableList()
                                        list[idx] = list[idx].copy(sill = u.toM(v))
                                        q.copy(openings = list)
                                    }
                                }
                            }

                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Position along wall", fontSize = 12.sp, color = pal.subAccent, fontWeight = FontWeight.SemiBold)
                                Text("${(o.pos * 100).toInt()}%", fontSize = 13.sp, color = pal.metric, fontWeight = FontWeight.ExtraBold)
                            }
                            Slider(
                                value = o.pos.toFloat(),
                                onValueChange = { pos ->
                                    vm.update { q ->
                                        val list = q.openings.toMutableList()
                                        list[idx] = list[idx].copy(pos = pos.toDouble())
                                        q.copy(openings = list)
                                    }
                                },
                                valueRange = 0.1f..0.9f
                            )
                        }
                    }

                    OutlinedButton({ go("wall") }, Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.Straighten, null, Modifier.size(16.dp))
                        Text(" Open Full Wall Measurement Screen", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // -----------------------------------------------------------------
                // TAB 2: WALL / FLOOR SIZE EDIT & ADD
                // -----------------------------------------------------------------
                2 -> {
                    val r = remember(p) { Calc.report(p) }
                    Text("Measurement Unit & Room Shape", fontWeight = FontWeight.ExtraBold, color = pal.accent, fontSize = 13.sp)
                    ChipRow(LengthUnit.values().toList(), u, { it.label }) { nu -> vm.update { it.copy(unit = nu) } }
                    ChipRow(RoomShape.values().toList(), p.shape, { it.label }) { sh ->
                        vm.update { it.copy(shape = sh).let { q -> if (sh == RoomShape.SQUARE) q.withRoom(q.roomL, q.roomL, q.height) else q } }
                    }

                    Text("Main Room Size (Live 3D Resize)", fontWeight = FontWeight.ExtraBold, color = pal.accent, fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        KeyedDimField("Room length", p.roomL, u, "mainL", Modifier.weight(1f)) { v ->
                            vm.update { q -> q.withRoom(v, if (q.shape == RoomShape.SQUARE) v else q.roomW, q.height) }
                        }
                        if (p.shape != RoomShape.SQUARE) {
                            KeyedDimField("Room width", p.roomW, u, "mainW", Modifier.weight(1f)) { v ->
                                vm.update { q -> q.withRoom(q.roomL, v, q.height) }
                            }
                        }
                    }
                    KeyedDimField("Wall height", p.height, u, "mainH") { v ->
                        vm.update { q -> q.withRoom(q.roomL, q.roomW, v) }
                    }

                    StatRow("Total Floor Area", "${fmt2(u.areaFromM2(r.floorArea))} ${u.areaLabel}", true)
                    StatRow("Net Wall Area (after openings)", "${fmt2(u.areaFromM2(r.wallNet))} ${u.areaLabel}")

                    // Floor Sections Edit & Add
                    Spacer(Modifier.height(4.dp))
                    Text("Floor Sections (${p.sections.size})", fontWeight = FontWeight.ExtraBold, color = pal.accent, fontSize = 13.5.sp)
                    p.sections.forEachIndexed { i, sec ->
                        val secShape = RoundedCornerShape(15.dp)
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .background(pal.chip.copy(alpha = 0.60f), secShape)
                                .border(1.dp, pal.primary.copy(alpha = 0.22f), secShape)
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    if (i == 0) "Section 1 (Main Room)" else "Floor Section ${i + 1}",
                                    fontWeight = FontWeight.ExtraBold,
                                    color = pal.subAccent,
                                    fontSize = 13.sp
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "${fmt2(u.areaFromM2(sec.length * sec.width))} ${u.areaLabel}",
                                        fontWeight = FontWeight.ExtraBold,
                                        color = pal.metric,
                                        fontSize = 12.sp
                                    )
                                    if (i > 0) {
                                        IconButton(
                                            onClick = { vm.update { q -> q.copy(sections = q.sections.filterIndexed { idx, _ -> idx != i }) } },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Filled.Delete, "Delete section", tint = Color(0xFFFF6B6B), modifier = Modifier.size(17.dp))
                                        }
                                    }
                                }
                            }
                            if (i > 0) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    KeyedDimField("Length", sec.length, u, "secL_$i", Modifier.weight(1f)) { v ->
                                        vm.update { q -> q.copy(sections = q.sections.toMutableList().also { it[i] = it[i].copy(length = v) }) }
                                    }
                                    KeyedDimField("Width", sec.width, u, "secW_$i", Modifier.weight(1f)) { v ->
                                        vm.update { q -> q.copy(sections = q.sections.toMutableList().also { it[i] = it[i].copy(width = v) }) }
                                    }
                                }
                            }
                        }
                    }
                    GoldButton("＋ Add Floor Section") {
                        vm.update { q -> q.copy(sections = q.sections + Section(q.roomL / 2, q.roomW / 2)) }
                        vm.toast = "Extra floor section added"
                    }

                    // Individual Walls Size Edit & Add
                    Spacer(Modifier.height(4.dp))
                    Text("Walls Size Edit & Add (${p.walls.size})", fontWeight = FontWeight.ExtraBold, color = pal.accent, fontSize = 13.5.sp)
                    p.walls.forEachIndexed { i, w ->
                        val wallShape = RoundedCornerShape(15.dp)
                        val netW = (w.length * w.height) - Calc.openingArea(p, i)
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .background(pal.chip.copy(alpha = 0.60f), wallShape)
                                .border(1.dp, pal.primary.copy(alpha = 0.22f), wallShape)
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Wall ${i + 1} (${Scene.wallName(i)})",
                                    fontWeight = FontWeight.ExtraBold,
                                    color = pal.subAccent,
                                    fontSize = 13.sp
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "Net: ${fmt2(u.areaFromM2(netW))} ${u.areaLabel}",
                                        fontWeight = FontWeight.ExtraBold,
                                        color = pal.metric,
                                        fontSize = 12.sp
                                    )
                                    if (i >= 4) {
                                        IconButton(
                                            onClick = {
                                                vm.update { q ->
                                                    q.copy(
                                                        walls = q.walls.filterIndexed { idx, _ -> idx != i },
                                                        openings = q.openings.filter { it.wall != i }.map { if (it.wall > i) it.copy(wall = it.wall - 1) else it }
                                                    )
                                                }
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Filled.Delete, "Delete wall", tint = Color(0xFFFF6B6B), modifier = Modifier.size(17.dp))
                                        }
                                    }
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                KeyedDimField("Length", w.length, u, "wallL_${i}_${p.roomL}_${p.roomW}", Modifier.weight(1f)) { v ->
                                    vm.update { q -> q.copy(walls = q.walls.toMutableList().also { it[i] = it[i].copy(length = v) }) }
                                }
                                KeyedDimField("Height", w.height, u, "wallH_${i}_${p.height}", Modifier.weight(1f)) { v ->
                                    vm.update { q -> q.copy(walls = q.walls.toMutableList().also { it[i] = it[i].copy(height = v) }) }
                                }
                            }
                        }
                    }
                    GoldButton("＋ Add Extra Wall") {
                        vm.update { q -> q.copy(walls = q.walls + Wall(q.roomL, q.height)) }
                        vm.toast = "Extra wall added"
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton({ go("wall") }, Modifier.weight(1f)) { Text("Wall Measure", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                        OutlinedButton({ go("floor") }, Modifier.weight(1f)) { Text("Floor Measure", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                        OutlinedButton({ go("calc") }, Modifier.weight(1f)) { Text("Calculator", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }

        GoldButton("Furniture & Room Templates") { go("furniture") }
        GoldButton("Export Design (PNG / PDF / 3D OBJ)") { go("export") }
    }

    if (showAddOpeningDialog) {
        var wall by remember { mutableIntStateOf(targetWall.coerceIn(0, (p.walls.size - 1).coerceAtLeast(0))) }
        var door by remember { mutableStateOf(true) }
        var ow by remember { mutableDoubleStateOf(0.9) }
        var oh by remember { mutableDoubleStateOf(2.1) }
        var cnt by remember { mutableIntStateOf(1) }
        var pos by remember { mutableFloatStateOf(0.5f) }
        var sill by remember { mutableDoubleStateOf(0.0) }
        AlertDialog(
            onDismissRequest = { showAddOpeningDialog = false },
            title = { Text("Add Door / Window", fontWeight = FontWeight.ExtraBold, color = Gold) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChipRow(listOf(true, false), door, { if (it) "Door" else "Window" }) {
                        door = it; ow = if (it) 0.9 else 1.2; oh = if (it) 2.1 else 1.2; sill = if (it) 0.0 else 0.9
                    }
                    ChipRow(p.walls.indices.toList(), wall, { "Wall ${it + 1} (${Scene.wallName(it)})" }) { wall = it }
                    KeyedDimField("Width", ow, u, door) { ow = it }
                    KeyedDimField("Height", oh, u, door) { oh = it }
                    if (!door) NumField("Sill height (above floor)", u.fromM(sill), listOf(u, door), u.label, allowZero = true) { sill = u.toM(it) }
                    IntField("Count", cnt, 1) { cnt = it }
                    Text("Position along wall: ${(pos * 100).toInt()} %", fontSize = 13.sp, color = pal.subAccent, fontWeight = FontWeight.Bold)
                    Slider(pos, { pos = it }, valueRange = 0.1f..0.9f)
                }
            },
            confirmButton = {
                TextButton({
                    vm.update { q -> q.copy(openings = q.openings + Opening(wall, door, ow, oh, cnt, pos.toDouble(), if (door) 0.0 else sill)) }
                    showAddOpeningDialog = false
                }) { Text("Add", color = Gold, fontWeight = FontWeight.ExtraBold) }
            },
            dismissButton = { TextButton({ showAddOpeningDialog = false }) { Text("Cancel") } }
        )
    }
}
