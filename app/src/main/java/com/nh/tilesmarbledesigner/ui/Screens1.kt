package com.nh.tilesmarbledesigner.ui

import android.graphics.Bitmap
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nh.tilesmarbledesigner.data.*
import com.nh.tilesmarbledesigner.engine.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun LogoMark(modifier: Modifier = Modifier) {
    val pal = LocalPalette.current
    val roofC = pal.accent
    val cols = listOf(pal.dark, pal.subAccent, pal.metric, pal.primary)
    Canvas(modifier) {
        val w = size.width; val h = size.height
        val roof = Path().apply {
            moveTo(w * 0.08f, h * 0.45f); lineTo(w * 0.5f, h * 0.08f); lineTo(w * 0.92f, h * 0.45f)
            lineTo(w * 0.82f, h * 0.53f); lineTo(w * 0.5f, h * 0.25f); lineTo(w * 0.18f, h * 0.53f); close()
        }
        drawPath(roof, roofC)
        for (i in 0 until 4) {
            val x = w * (0.28f + (i % 2) * 0.24f); val y = h * (0.52f + (i / 2) * 0.24f)
            drawRoundRect(cols[i], Offset(x, y), Size(w * 0.21f, h * 0.21f), CornerRadius(12f))
        }
    }
}

@Composable
fun SplashScreen(onDone: () -> Unit) {
    val pal = LocalPalette.current
    val a = remember { Animatable(0f) }
    LaunchedEffect(Unit) { a.animateTo(1f, tween(900)); delay(700); onDone() }
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(pal.bg, pal.chip, pal.bgBottom))),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier
                .alpha(a.value)
                .scale(0.9f + 0.1f * a.value)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                Modifier
                    .size(138.dp)
                    .softShadow(20.dp, RoundedCornerShape(40.dp), glow = true)
                    .background(Brush.verticalGradient(listOf(pal.cardTop, pal.card)), RoundedCornerShape(40.dp))
                    .border(2.dp, Brush.linearGradient(listOf(pal.vivid, pal.primary, pal.subAccent)), RoundedCornerShape(40.dp)),
                contentAlignment = Alignment.Center
            ) {
                LogoMark(Modifier.size(94.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = pal.ink, fontWeight = FontWeight.ExtraBold)) { append("Tiles & ") }
                    withStyle(SpanStyle(color = pal.subAccent, fontWeight = FontWeight.ExtraBold)) { append("Marble") }
                },
                fontSize = 30.sp
            )
            Text("Room Designer", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = pal.accent)
            Text(
                "Design  •  Measure  •  Calculate  •  Decorate",
                color = pal.metric,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier
                    .softShadow(8.dp, RoundedCornerShape(20.dp), glow = true)
                    .background(Brush.horizontalGradient(listOf(pal.dark, pal.primary)), RoundedCornerShape(20.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 18.dp, vertical = 7.dp)
            ) {
                Text("NH Developer  ·  v3.5 Luxury Offline Suite", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
private fun FeatureTile(icon: ImageVector, title: String, sub: String, modifier: Modifier, onClick: () -> Unit) {
    val pal = LocalPalette.current
    val shape = RoundedCornerShape(22.dp)
    Card(
        onClick = onClick,
        modifier = modifier
            .height(118.dp)
            .softShadow(10.dp, shape),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(
            1.2.dp,
            Brush.linearGradient(
                listOf(pal.borderHighlight, pal.subAccent.copy(alpha = 0.28f), pal.ink.copy(alpha = 0.08f))
            )
        )
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(pal.cardTop, pal.card)))
                .padding(13.dp)
        ) {
            // Subtle top-right jewel indicator dot
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .size(22.dp)
                    .background(pal.chip, CircleShape)
                    .border(1.dp, pal.primary.copy(alpha = 0.30f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.NorthEast,
                    contentDescription = null,
                    tint = pal.subAccent,
                    modifier = Modifier.size(12.dp)
                )
            }
            Column(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                IconBadge(icon, size = 40)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        title,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.5.sp,
                        lineHeight = 16.sp,
                        color = pal.ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        sub,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = pal.subAccent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun StatPill(label: String, value: String, accentColor: Color, modifier: Modifier) {
    val pal = LocalPalette.current
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier
            .softShadow(8.dp, shape)
            .background(Brush.verticalGradient(listOf(pal.cardTop, pal.card)), shape)
            .border(1.2.dp, accentColor.copy(alpha = 0.35f), shape)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Box(Modifier.size(7.dp).background(accentColor, CircleShape))
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = pal.label, maxLines = 1)
        }
        Text(
            value,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 15.sp,
            color = accentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun HomeScreen(vm: AppViewModel, go: (String) -> Unit) {
    val pal = LocalPalette.current
    val p = vm.draft
    val projects by vm.projects.collectAsState()
    val settings by vm.settings.collectAsState()
    val hero by produceState<Bitmap?>(null, p) {
        value = withContext(Dispatchers.Default) { Scene.render(p, p.designs, 800, 450, 18f, 26f, 1f) }
    }
    ScreenFrame("Tiles & Marble", actions = {
        Surface(
            onClick = { go("settings") },
            shape = RoundedCornerShape(16.dp),
            color = pal.chip,
            border = BorderStroke(1.dp, pal.primary.copy(alpha = 0.35f)),
            modifier = Modifier.softShadow(5.dp, RoundedCornerShape(16.dp))
        ) {
            Row(
                Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(Icons.Filled.Palette, "Theme", tint = pal.subAccent, modifier = Modifier.size(17.dp))
                Text(pal.name.substringBefore(' '), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = pal.accent)
            }
        }
    }) {
        // Eye-catching multi-color tagline pill
        Row(
            Modifier
                .fillMaxWidth()
                .softShadow(5.dp, RoundedCornerShape(14.dp))
                .background(pal.card, RoundedCornerShape(14.dp))
                .border(1.dp, pal.primary.copy(alpha = 0.22f), RoundedCornerShape(14.dp))
                .padding(horizontal = 12.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = pal.accent, fontWeight = FontWeight.ExtraBold)) { append("DESIGN") }
                    withStyle(SpanStyle(color = pal.label)) { append("  •  ") }
                    withStyle(SpanStyle(color = pal.subAccent, fontWeight = FontWeight.ExtraBold)) { append("MEASURE") }
                    withStyle(SpanStyle(color = pal.label)) { append("  •  ") }
                    withStyle(SpanStyle(color = pal.metric, fontWeight = FontWeight.ExtraBold)) { append("CALCULATE") }
                    withStyle(SpanStyle(color = pal.label)) { append("  •  ") }
                    withStyle(SpanStyle(color = pal.primary, fontWeight = FontWeight.ExtraBold)) { append("3D") }
                },
                fontSize = 11.sp
            )
            Text("v3.5 PRO", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = pal.metric)
        }

        // Next-Level 3D Hero Showcase Card
        val heroShape = RoundedCornerShape(24.dp)
        Box(
            Modifier
                .fillMaxWidth()
                .height(200.dp)
                .softShadow(16.dp, heroShape, glow = true)
                .clip(heroShape)
                .background(pal.chip)
                .border(1.5.dp, Brush.linearGradient(listOf(pal.vivid, pal.primary, pal.subAccent)), heroShape)
                .clickable { go("preview") }
        ) {
            hero?.let { Image(it.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0x22000000), Color(0x55000000), Color(0xD9070B16))
                        )
                    )
            )
            // Top floating badges on Hero
            Row(
                Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .background(Color(0xAA0B1020), RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0x66FFFFFF), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("LIVE 3D STAGE", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFFFD166))
                }
                Box(
                    Modifier
                        .background(pal.dark.copy(alpha = 0.90f), RoundedCornerShape(12.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        fmt2(p.unit.areaFromM2(Calc.floorArea(p))) + " " + p.unit.areaLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            }
            // Bottom info + quick action inside Hero
            Row(
                Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Build Your Dream Room",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        color = Color.White
                    )
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = Color(0xFFFFD166), fontWeight = FontWeight.Bold)) {
                                append(p.name)
                            }
                            withStyle(SpanStyle(color = Color(0xDDFFFFFF))) {
                                append("  ·  Tap to explore in 3D")
                            }
                        },
                        fontSize = 12.sp
                    )
                }
                Box(
                    Modifier
                        .background(Color.White.copy(alpha = 0.20f), RoundedCornerShape(14.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 11.dp, vertical = 6.dp)
                ) {
                    Text("3D View →", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                }
            }
        }

        // 3 Multi-color Stat Pills
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatPill("Saved Rooms", "${projects.size}", pal.metric, Modifier.weight(1f))
            StatPill("Active Unit", settings.unit.label.uppercase(), pal.subAccent, Modifier.weight(1f))
            StatPill("Palette", pal.name.substringBefore(' '), pal.accent, Modifier.weight(1f))
        }

        PrimaryButton("＋  Create New Room Project") { go("create") }

        SectionTitle("Measure & Calculate")
        val group1 = listOf(
            Triple(Icons.Filled.Straighten, "Room Measurement", "Size, walls & doors") to "measure",
            Triple(Icons.Filled.Calculate, "Tile Calculator", "Boxes, slabs & waste") to "calc"
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            group1.forEach { (t, route) -> FeatureTile(t.first, t.second, t.third, Modifier.weight(1f)) { go(route) } }
        }

        SectionTitle("Surface & Material Studio")
        val group2 = listOf(
            Triple(Icons.Filled.Layers, "Wall Designer", "Patterns & grout") to "studio/wall",
            Triple(Icons.Filled.GridView, "Floor Designer", "Marble, tile & wood") to "studio/floor",
            Triple(Icons.Filled.Crop, "Atomic Crop Tool", "Photo to texture") to "crop",
            Triple(Icons.Filled.Image, "Material Library", "Built-in & custom") to "library"
        )
        group2.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { (t, route) -> FeatureTile(t.first, t.second, t.third, Modifier.weight(1f)) { go(route) } }
            }
        }

        SectionTitle("3D Interior, Templates & Export")
        val group3 = listOf(
            Triple(Icons.Filled.Chair, "Room Templates", "5 furnished layouts") to "templates",
            Triple(Icons.Filled.Weekend, "Furniture Studio", "Add, move & rotate") to "furniture",
            Triple(Icons.Filled.ViewInAr, "Room 3D Preview", "Rotate & compare") to "preview",
            Triple(Icons.Filled.Fullscreen, "Full Screen 3D", "Immersive walkthrough") to "fullview",
            Triple(Icons.Filled.Folder, "My Designs", "Saved room projects") to "projects",
            Triple(Icons.Filled.Share, "Export & Share", "PNG, PDF & 3D OBJ") to "export"
        )
        group3.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { (t, route) -> FeatureTile(t.first, t.second, t.third, Modifier.weight(1f)) { go(route) } }
            }
        }

        NHCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IconBadge(Icons.Filled.Verified, size = 38)
                Column(Modifier.weight(1f)) {
                    Text("NH Architectural Luxury Suite v3.5", color = pal.accent, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                    Text(
                        "Smart 3D perspective • Precision tile & slab maths • 100% offline privacy.",
                        fontSize = 12.sp,
                        color = pal.subAccent,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun CreateRoomScreen(vm: AppViewModel, go: (String) -> Unit, back: () -> Unit) {
    val pal = LocalPalette.current
    val s = vm.settings.collectAsState().value
    var name by remember { mutableStateOf("") }
    var shape by remember { mutableStateOf(RoomShape.RECTANGLE) }
    var unit by remember { mutableStateOf(s.unit) }
    var l by remember { mutableDoubleStateOf(if (s.unit == LengthUnit.FT || s.unit == LengthUnit.IN) 3.048 else 3.0) }
    var w by remember { mutableDoubleStateOf(l) }
    var h by remember { mutableDoubleStateOf(l) }
    val ww = if (shape == RoomShape.SQUARE) l else w
    ScreenFrame("Create Room", back) {
        NHCard {
            SectionTitle("Project Identity")
            OutlinedTextField(
                name,
                { name = it },
                label = { Text("Room / project name", fontWeight = FontWeight.SemiBold) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().softShadow(4.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = fieldColors()
            )
            Text("Room Geometry", color = pal.subAccent, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
            ChipRow(RoomShape.values().toList(), shape, { it.label }) { shape = it }
            Text("Measurement Unit", color = pal.subAccent, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
            ChipRow(LengthUnit.values().toList(), unit, { it.label }) { unit = it }
        }
        NHCard {
            SectionTitle("Room Dimensions")
            DimField("Room length", l, unit) { l = it }
            if (shape != RoomShape.SQUARE) DimField("Room width", w, unit) { w = it }
            DimField("Wall height", h, unit) { h = it }
        }
        NHCard {
            StatRow("Estimated Floor Area", "${fmt2(unit.areaFromM2(l * ww))} ${unit.areaLabel}", true)
        }
        PrimaryButton("Create Room & Continue", enabled = l > 0 && ww > 0 && h > 0) {
            vm.newDraft(name, shape, unit, l, ww, h); go("measure")
        }
    }
}

private fun DrawScope.label(s: String, x: Float, y: Float, ink: Int, size: Float = 30f) {
    drawContext.canvas.nativeCanvas.drawText(s, x, y, android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size; color = ink; isFakeBoldText = true; textAlign = android.graphics.Paint.Align.CENTER
    })
}

@Composable
fun MeasurementScreen(vm: AppViewModel, go: (String) -> Unit) {
    val p = vm.draft; val u = p.unit
    val pal = LocalPalette.current
    ScreenFrame("Room Measurement") {
        NHCard {
            SectionTitle("Project & Shape")
            OutlinedTextField(
                p.name,
                { n -> vm.update { it.copy(name = n) } },
                label = { Text("Project name", fontWeight = FontWeight.SemiBold) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().softShadow(4.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = fieldColors()
            )
            ChipRow(RoomShape.values().toList(), p.shape, { it.label }) { sh ->
                vm.update { it.copy(shape = sh).let { q -> if (sh == RoomShape.SQUARE) q.withRoom(q.roomL, q.roomL, q.height) else q } }
            }
            ChipRow(LengthUnit.values().toList(), u, { it.label }) { nu -> vm.update { it.copy(unit = nu) } }
        }

        val roomFill = pal.soft
        val roomLine = pal.dark
        val dimColor = pal.subAccent.toArgb()
        val areaColor = pal.metric.toArgb()
        val canvasShape = RoundedCornerShape(22.dp)
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(215.dp)
                .softShadow(12.dp, canvasShape, glow = true)
                .clip(canvasShape)
                .background(Brush.verticalGradient(listOf(pal.cardTop, pal.chip)))
                .border(1.5.dp, Brush.linearGradient(listOf(pal.borderHighlight, pal.subAccent.copy(alpha = 0.35f))), canvasShape)
        ) {
            val l = p.roomL.toFloat(); val w = p.roomW.toFloat()
            val k = minOf((size.width - 140f) / l, (size.height - 90f) / w)
            val tl = Offset((size.width - l * k) / 2f, (size.height - w * k) / 2f + 10f)
            drawRect(roomFill, tl, Size(l * k, w * k))
            drawRect(roomLine, tl, Size(l * k, w * k), style = Stroke(6f))
            label("${fmt(u.fromM(p.roomL))} ${u.label}", size.width / 2f, tl.y - 14f, dimColor, 31f)
            label("${fmt(u.fromM(p.roomW))} ${u.label}", tl.x + l * k + 64f, tl.y + w * k / 2f, dimColor, 31f)
            label("Floor ${fmt2(u.areaFromM2(p.roomL * p.roomW))} ${u.areaLabel}", size.width / 2f, tl.y + w * k / 2f + 10f, areaColor, 30f)
        }

        NHCard {
            SectionTitle("Dimensions (${u.label})")
            DimField("Room length", p.roomL, u) { v -> vm.update { q -> q.withRoom(v, if (q.shape == RoomShape.SQUARE) v else q.roomW, q.height) } }
            if (p.shape == RoomShape.SQUARE) Text("Square room: width automatically matches length.", color = pal.subAccent, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            else DimField("Room width", p.roomW, u) { v -> vm.update { q -> q.withRoom(q.roomL, v, q.height) } }
            DimField("Wall height", p.height, u) { v -> vm.update { q -> q.withRoom(q.roomL, q.roomW, v) } }
        }

        val r = remember(p) { Calc.report(p) }
        NHCard {
            SectionTitle("Live Area Summary")
            StatRow("Total floor area", "${fmt2(u.areaFromM2(r.floorArea))} ${u.areaLabel}", true)
            StatRow("Net wall area", "${fmt2(u.areaFromM2(r.wallNet))} ${u.areaLabel}")
        }
        GoldButton("Wall Measurement (Walls, Doors & Windows)") { go("wall") }
        GoldButton("Floor Measurement (Irregular Sections)") { go("floor") }
        PrimaryButton("Open Tile Calculator") { go("calc") }
    }
}

@Composable
fun WallScreen(vm: AppViewModel, back: () -> Unit) {
    val p = vm.draft; val u = p.unit
    val pal = LocalPalette.current
    var showAdd by remember { mutableStateOf(false) }
    ScreenFrame("Wall Measurement", back) {
        Text(
            "Each wall is length × height. Editing room size in Measurement re-syncs walls 1–4.",
            color = pal.subAccent,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp
        )
        p.walls.forEachIndexed { i, w ->
            NHCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    SectionTitle("Wall ${i + 1} (${Scene.wallName(i)})")
                    if (i >= 4) IconButton({
                        vm.update { q ->
                            q.copy(walls = q.walls.filterIndexed { idx, _ -> idx != i },
                                openings = q.openings.filter { it.wall != i }.map { if (it.wall > i) it.copy(wall = it.wall - 1) else it })
                        }
                    }) { Icon(Icons.Filled.Delete, "Delete wall", tint = Color(0xFFFF6B6B)) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DimField("Length", w.length, u, Modifier.weight(1f)) { v -> vm.update { q -> q.copy(walls = q.walls.toMutableList().also { it[i] = it[i].copy(length = v) }) } }
                    DimField("Height", w.height, u, Modifier.weight(1f)) { v -> vm.update { q -> q.copy(walls = q.walls.toMutableList().also { it[i] = it[i].copy(height = v) }) } }
                }
                val g = w.length * w.height; val o = Calc.openingArea(p, i)
                StatRow("Net area", "${fmt2(u.areaFromM2(g - o))} ${u.areaLabel}", true)
            }
        }
        GoldButton("＋ Add Extra Wall") { vm.update { q -> q.copy(walls = q.walls + Wall(q.roomL, q.height)) } }
        SectionTitle("Doors & Windows")
        if (p.openings.isEmpty()) Text("No openings yet. Add doors or windows to deduct them from wall tile area.", color = pal.label)
        p.openings.forEachIndexed { i, o ->
            NHCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "${if (o.door) "Door" else "Window"}  •  Wall ${o.wall + 1} (${Scene.wallName(o.wall)})",
                            fontWeight = FontWeight.ExtraBold,
                            color = pal.accent,
                            fontSize = 14.sp
                        )
                        Text(
                            "${fmt(u.fromM(o.w))} × ${fmt(u.fromM(o.h))} ${u.label}  × ${o.count}   (${fmt(o.pos * 100)}% along wall)",
                            color = pal.subAccent,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    }
                    IconButton({ vm.update { q -> q.copy(openings = q.openings.filterIndexed { idx, _ -> idx != i }) } }) {
                        Icon(Icons.Filled.Delete, "Delete", tint = Color(0xFFFF6B6B))
                    }
                }
            }
        }
        PrimaryButton("＋ Add Door / Window") { showAdd = true }
        val errs = Calc.validate(p).filter { it.startsWith("Wall") || it.startsWith("Opening") }
        errs.forEach { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
    }
    if (showAdd) {
        var wall by remember { mutableIntStateOf(0) }
        var door by remember { mutableStateOf(true) }
        var ow by remember { mutableDoubleStateOf(0.9) }
        var oh by remember { mutableDoubleStateOf(2.1) }
        var cnt by remember { mutableIntStateOf(1) }
        var pos by remember { mutableFloatStateOf(0.5f) }
        var sill by remember { mutableDoubleStateOf(0.0) }
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("Add Opening", fontWeight = FontWeight.ExtraBold, color = Gold) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChipRow(listOf(true, false), door, { if (it) "Door" else "Window" }) { door = it; ow = if (it) 0.9 else 1.2; oh = if (it) 2.1 else 1.2; sill = if (it) 0.0 else 0.9 }
                    ChipRow(p.walls.indices.toList(), wall, { "Wall ${it + 1} (${Scene.wallName(it)})" }) { wall = it }
                    DimField("Width", ow, u, key = door) { ow = it }
                    DimField("Height", oh, u, key = door) { oh = it }
                    if (!door) NumField("Sill height (above floor)", u.fromM(sill), listOf(u, door), u.label, allowZero = true) { sill = u.toM(it) }
                    IntField("Count", cnt, 1) { cnt = it }
                    Text("Position along wall: ${(pos * 100).toInt()} %", fontSize = 13.sp, color = pal.subAccent, fontWeight = FontWeight.Bold)
                    Slider(pos, { pos = it }, valueRange = 0.1f..0.9f)
                }
            },
            confirmButton = {
                TextButton({
                    vm.update { q -> q.copy(openings = q.openings + Opening(wall, door, ow, oh, cnt, pos.toDouble(), if (door) 0.0 else sill)) }
                    showAdd = false
                }) { Text("Add", color = Gold, fontWeight = FontWeight.ExtraBold) }
            },
            dismissButton = { TextButton({ showAdd = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun DimField(label: String, meters: Double, unit: LengthUnit, modifier: Modifier = Modifier, key: Any?, onMeters: (Double) -> Unit) =
    NumField(label, unit.fromM(meters), listOf(unit, key), unit.label, modifier) { onMeters(unit.toM(it)) }

@Composable
fun FloorScreen(vm: AppViewModel, back: () -> Unit) {
    val p = vm.draft; val u = p.unit
    val pal = LocalPalette.current
    ScreenFrame("Floor Measurement", back) {
        Text(
            "For irregular rooms (L-shape, alcoves…) split the floor into rectangles. Section 1 is the main room.",
            color = pal.subAccent,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp
        )
        p.sections.forEachIndexed { i, s ->
            NHCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    SectionTitle(if (i == 0) "Section 1 (Main Room)" else "Section ${i + 1}")
                    if (i > 0) IconButton({ vm.update { q -> q.copy(sections = q.sections.filterIndexed { idx, _ -> idx != i }) } }) { Icon(Icons.Filled.Delete, "Delete", tint = Color(0xFFFF6B6B)) }
                }
                if (i == 0) {
                    StatRow("Length × width", "${fmt(u.fromM(s.length))} × ${fmt(u.fromM(s.width))} ${u.label}")
                    Text("Edit the main room in Room Measurement.", fontSize = 12.sp, color = pal.label)
                } else Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DimField("Length", s.length, u, Modifier.weight(1f)) { v -> vm.update { q -> q.copy(sections = q.sections.toMutableList().also { it[i] = it[i].copy(length = v) }) } }
                    DimField("Width", s.width, u, Modifier.weight(1f)) { v -> vm.update { q -> q.copy(sections = q.sections.toMutableList().also { it[i] = it[i].copy(width = v) }) } }
                }
                StatRow("Section Area", "${fmt2(u.areaFromM2(s.length * s.width))} ${u.areaLabel}", true)
            }
        }
        GoldButton("＋ Add Floor Section") { vm.update { q -> q.copy(sections = q.sections + Section(q.roomL / 2, q.roomW / 2)) } }
        NHCard { StatRow("Total Floor Area", "${fmt2(u.areaFromM2(Calc.floorArea(p)))} ${u.areaLabel}", true) }
        Text("The 3D preview shows the main rectangle; extra sections are included in all calculations.", fontSize = 12.sp, color = pal.label)
    }
}
