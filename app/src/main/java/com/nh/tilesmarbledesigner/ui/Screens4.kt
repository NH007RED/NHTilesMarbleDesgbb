package com.nh.tilesmarbledesigner.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nh.tilesmarbledesigner.data.*
import com.nh.tilesmarbledesigner.engine.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
private fun Thumb(path: String?, modifier: Modifier) {
    val pal = LocalPalette.current
    val b by produceState<Bitmap?>(null, path) { value = path?.let { withContext(Dispatchers.IO) { BitmapFactory.decodeFile(it) } } }
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier
            .softShadow(6.dp, shape)
            .clip(shape)
            .background(pal.chip)
            .border(1.dp, pal.primary.copy(alpha = 0.25f), shape),
        contentAlignment = Alignment.Center
    ) {
        if (b != null) Image(b!!.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Icon(Icons.Filled.Image, null, tint = pal.label)
    }
}

private fun dateStr(ms: Long) = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(ms))

@Composable
fun ProjectsScreen(vm: AppViewModel, go: (String) -> Unit) {
    val list by vm.projects.collectAsState()
    val pal = LocalPalette.current
    var confirm by remember { mutableStateOf<ProjectEntity?>(null) }
    ScreenFrame("My Projects", scroll = false) {
        if (list.isEmpty()) {
            NHCard {
                SectionTitle("No Saved Projects Yet")
                Text(
                    "Create a room, customize floor & wall materials in the 3D Studio, then tap Save Project.",
                    color = pal.label,
                    fontSize = 13.sp
                )
                PrimaryButton("＋ Create New Room") { go("create") }
            }
        }
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(list, key = { it.id }) { e ->
                val pd = remember(e.json) { runCatching { ProjectData.fromJson(e.json) }.getOrNull() }
                var menu by remember { mutableStateOf(false) }
                val shape = RoundedCornerShape(22.dp)
                Card(
                    onClick = { go("details/${e.id}") },
                    modifier = Modifier.softShadow(10.dp, shape),
                    shape = shape,
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    border = BorderStroke(
                        1.2.dp,
                        Brush.linearGradient(listOf(pal.borderHighlight, pal.subAccent.copy(alpha = 0.25f)))
                    )
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(Brush.verticalGradient(listOf(pal.cardTop, pal.card)))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(13.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Thumb(e.thumbPath, Modifier.size(96.dp, 74.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(e.name, fontWeight = FontWeight.ExtraBold, maxLines = 2, fontSize = 15.sp, color = pal.ink)
                            if (pd != null) {
                                Text(
                                    "${fmt2(pd.unit.areaFromM2(Calc.floorArea(pd)))} ${pd.unit.areaLabel}",
                                    color = pal.metric,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp
                                )
                            }
                            Text(dateStr(e.updatedAt), fontSize = 11.sp, color = pal.subAccent, fontWeight = FontWeight.SemiBold)
                        }
                        Box {
                            IconButton({ menu = true }) { Icon(Icons.Filled.MoreVert, "Menu", tint = pal.accent) }
                            DropdownMenu(menu, { menu = false }) {
                                DropdownMenuItem({ Text("Edit", fontWeight = FontWeight.Bold) }, { menu = false; vm.open(e.id) { go("measure") } })
                                DropdownMenuItem({ Text("Duplicate", fontWeight = FontWeight.Bold) }, { menu = false; vm.duplicate(e.id) })
                                DropdownMenuItem({ Text("Delete", color = Danger, fontWeight = FontWeight.Bold) }, { menu = false; confirm = e })
                            }
                        }
                    }
                }
            }
        }
    }
    confirm?.let { e ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text("Delete project?", fontWeight = FontWeight.ExtraBold, color = Gold) },
            text = { Text("\"${e.name}\" will be permanently deleted.") },
            confirmButton = { TextButton({ vm.delete(e.id); confirm = null }) { Text("Delete", color = Color(0xFFFF6B6B), fontWeight = FontWeight.ExtraBold) } },
            dismissButton = { TextButton({ confirm = null }) { Text("Cancel") } }
        )
    }
}

@Composable
fun DetailsScreen(vm: AppViewModel, id: Long, go: (String) -> Unit, back: () -> Unit) {
    val entity by produceState<ProjectEntity?>(null, id, vm.projects.collectAsState().value) { value = vm.load(id) }
    val pal = LocalPalette.current
    var confirm by remember { mutableStateOf(false) }
    ScreenFrame("Project Details", back) {
        val e = entity
        if (e == null) { Text("Loading…"); return@ScreenFrame }
        val p = remember(e.json) { ProjectData.fromJson(e.json) }
        val u = p.unit
        val r = remember(e.json) { Calc.report(p) }
        Thumb(e.thumbPath, Modifier.fillMaxWidth().height(210.dp))
        NHCard {
            Text(e.name, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = pal.accent)
            StatRow("Created", dateStr(e.createdAt))
            StatRow("Updated", dateStr(e.updatedAt))
            StatRow("Room Size", "${fmt(u.fromM(p.roomL))} × ${fmt(u.fromM(p.roomW))} × ${fmt(u.fromM(p.height))} ${u.label}")
            StatRow("Floor Area", "${fmt2(u.areaFromM2(r.floorArea))} ${u.areaLabel}", true)
            StatRow("Net Wall Area", "${fmt2(u.areaFromM2(r.wallNet))} ${u.areaLabel}")
            StatRow("Floor Boxes", "${r.floor.boxes}  (${r.floor.purchased} tiles)")
            StatRow("Wall Boxes", "${r.wall.boxes}  (${r.wall.purchased} tiles)")
            StatRow("Total Boxes", "${r.totalBoxes}", true)
            StatRow("Floor Material", Materials.nameOf(p.design(Surf.FLOOR).materialId))
        }
        PrimaryButton("Edit Project") { vm.open(id) { go("measure") } }
        GoldButton("Open 3D Preview") { vm.open(id) { go("preview") } }
        GoldButton("Export (PNG / PDF / 3D OBJ)") { vm.open(id) { go("export") } }
        GoldButton("Duplicate Project") { vm.duplicate(id) }
        TextButton({ confirm = true }, Modifier.fillMaxWidth()) {
            Text("Delete Project", color = Color(0xFFFF6B6B), fontWeight = FontWeight.ExtraBold)
        }
    }
    if (confirm) AlertDialog(
        onDismissRequest = { confirm = false },
        title = { Text("Delete project?", fontWeight = FontWeight.ExtraBold) },
        text = { Text("This cannot be undone.") },
        confirmButton = { TextButton({ vm.delete(id); confirm = false; back() }) { Text("Delete", color = Color(0xFFFF6B6B), fontWeight = FontWeight.ExtraBold) } },
        dismissButton = { TextButton({ confirm = false }) { Text("Cancel") } }
    )
}

@Composable
fun ExportScreen(vm: AppViewModel, back: () -> Unit) {
    val ctx = LocalContext.current
    val pal = LocalPalette.current
    val scope = rememberCoroutineScope()
    val p = vm.draft
    val views = listOf("Front" to (0f to 24f), "Left corner" to (-35f to 24f), "Right corner" to (35f to 24f), "Top view" to (0f to 80f), "Back" to (180f to 24f))
    var viewIdx by remember { mutableIntStateOf(0) }
    val angle = views[viewIdx].second
    val preview by produceState<Bitmap?>(null, p, viewIdx) { value = withContext(Dispatchers.Default) { Scene.render(p, p.designs, 1280, 960, angle.first, angle.second, 1f, 1024) } }
    var pendingFile by remember { mutableStateOf<File?>(null) }
    var busy by remember { mutableStateOf(false) }
    val saver = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri ->
        val f = pendingFile
        if (uri != null && f != null) scope.launch {
            withContext(Dispatchers.IO) { Exporter.copyTo(ctx, f, uri) }; vm.toast = "Saved"
        }
    }
    val errors = remember(p) { Calc.validate(p) }
    val caption = "${p.name} - designed with NH Tiles & Marble Room Designer v3.5"
    suspend fun png(): File? = withContext(Dispatchers.IO) {
        val b = Scene.render(p, p.designs, 1920, 1440, angle.first, angle.second, 1f, 1280)
        try { Exporter.savePng(ctx, b, p.name + "_" + views[viewIdx].first + "_3d") } finally { b.recycle() }
    }
    suspend fun pdf(): File? = if (errors.isNotEmpty()) null else withContext(Dispatchers.IO) { Exporter.createPdf(ctx, p, Calc.report(p), preview) }
    suspend fun model(): File? = try { withContext(Dispatchers.IO) { ModelExporter.exportZip(ctx, p) } } catch (e: Throwable) { vm.toast = "3D export failed"; null }
    fun whatsApp(f: File, mime: String) {
        if (!Exporter.sendWhatsApp(ctx, f, mime, caption)) { vm.toast = "WhatsApp not found - choose another app"; Exporter.share(ctx, f, mime) }
    }
    fun work(block: suspend () -> Unit) { scope.launch { busy = true; try { block() } finally { busy = false } } }
    val green = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366), contentColor = Color.Black)

    ScreenFrame("Export Design", back) {
        val previewShape = RoundedCornerShape(22.dp)
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 3f)
                .softShadow(14.dp, previewShape, glow = true)
                .clip(previewShape)
                .background(NavyCard)
                .border(1.5.dp, Brush.linearGradient(listOf(pal.vivid, pal.primary, pal.subAccent)), previewShape),
            contentAlignment = Alignment.Center
        ) {
            val b = preview
            if (b != null) Image(b.asImageBitmap(), null, Modifier.fillMaxSize()) else CircularProgressIndicator(color = Gold)
            Tag("${p.name}  •  ${views[viewIdx].first}", Modifier.align(Alignment.TopStart).padding(12.dp))
        }
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth(), color = pal.metric)

        NHCard {
            SectionTitle("3D Camera Angle Gallery")
            ChipRow((0 until views.size).toList(), viewIdx, { views[it].first }) { viewIdx = it }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton({
                    work {
                        val b = withContext(Dispatchers.Default) { Scene.render(p, p.designs, 1920, 1440, angle.first, angle.second, 1f, 1280) }
                        val path = withContext(Dispatchers.IO) { Exporter.saveToGallery(ctx, b, p.name + "_" + views[viewIdx].first) }
                        b.recycle()
                        vm.toast = if (path != null) "Saved to $path" else "Could not save image"
                    }
                }, Modifier.weight(1f), enabled = !busy) { Icon(Icons.Filled.Download, null); Text(" Save Angle", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                OutlinedButton({
                    work {
                        var ok = 0
                        withContext(Dispatchers.Default) {
                            views.forEach { (label, a) ->
                                val b = Scene.render(p, p.designs, 1920, 1440, a.first, a.second, 1f, 1280)
                                if (Exporter.saveToGallery(ctx, b, p.name + "_" + label) != null) ok++
                                b.recycle()
                            }
                        }
                        vm.toast = "$ok 3D images saved to gallery"
                    }
                }, Modifier.weight(1f), enabled = !busy) { Icon(Icons.Filled.PhotoLibrary, null); Text(" Save All 5", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            }
        }

        NHCard {
            SectionTitle("Direct WhatsApp Share")
            Button({ work { png()?.let { whatsApp(it, "image/png") } } }, Modifier.fillMaxWidth().height(48.dp).softShadow(6.dp, RoundedCornerShape(16.dp)), enabled = !busy, colors = green, shape = RoundedCornerShape(16.dp)) {
                Icon(Icons.Filled.Share, null); Text("  Send 3D Design Image", fontWeight = FontWeight.ExtraBold)
            }
            Button({ work { pdf()?.let { whatsApp(it, "application/pdf") } } }, Modifier.fillMaxWidth().height(48.dp).softShadow(6.dp, RoundedCornerShape(16.dp)), enabled = errors.isEmpty() && !busy, colors = green, shape = RoundedCornerShape(16.dp)) {
                Icon(Icons.Filled.PictureAsPdf, null); Text("  Send PDF Calculation Report", fontWeight = FontWeight.ExtraBold)
            }
            Button({ work { model()?.let { whatsApp(it, "application/zip") } } }, Modifier.fillMaxWidth().height(48.dp).softShadow(6.dp, RoundedCornerShape(16.dp)), enabled = !busy, colors = green, shape = RoundedCornerShape(16.dp)) {
                Icon(Icons.Filled.ViewInAr, null); Text("  Send 3D Model (OBJ .zip)", fontWeight = FontWeight.ExtraBold)
            }
        }

        NHCard {
            SectionTitle("3D Model Package (OBJ + MTL)")
            Text("Opens in Blender, Windows 3D Viewer, MeshLab, Sketchfab & CAD tools with full textures and 3D furniture.", fontSize = 12.sp, color = pal.subAccent, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton({ work { model()?.let { pendingFile = it; saver.launch(it.name) } } }, Modifier.weight(1f), enabled = !busy) { Icon(Icons.Filled.Download, null); Text(" Save ZIP", fontWeight = FontWeight.Bold) }
                OutlinedButton({ work { model()?.let { Exporter.share(ctx, it, "application/zip") } } }, Modifier.weight(1f), enabled = !busy) { Icon(Icons.Filled.Share, null); Text(" Share ZIP", fontWeight = FontWeight.Bold) }
            }
        }

        NHCard {
            SectionTitle("High-Res PNG & PDF Report")
            if (errors.isNotEmpty()) errors.forEach { Text("• $it", color = MaterialTheme.colorScheme.error, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton({ work { png()?.let { pendingFile = it; saver.launch(it.name) } } }, Modifier.weight(1f), enabled = preview != null && !busy) { Icon(Icons.Filled.Download, null); Text(" Save PNG", fontWeight = FontWeight.Bold) }
                OutlinedButton({ work { png()?.let { Exporter.share(ctx, it, "image/png") } } }, Modifier.weight(1f), enabled = preview != null && !busy) { Icon(Icons.Filled.Share, null); Text(" Share PNG", fontWeight = FontWeight.Bold) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton({ work { pdf()?.let { pendingFile = it; saver.launch(it.name) } } }, Modifier.weight(1f), enabled = errors.isEmpty() && !busy) { Icon(Icons.Filled.PictureAsPdf, null); Text(" Save PDF", fontWeight = FontWeight.Bold) }
                OutlinedButton({ work { pdf()?.let { Exporter.share(ctx, it, "application/pdf") } } }, Modifier.weight(1f), enabled = errors.isEmpty() && !busy) { Icon(Icons.Filled.Share, null); Text(" Share PDF", fontWeight = FontWeight.Bold) }
            }
        }
        PrimaryButton("Save Project") { vm.save() }
    }
}

@Composable
fun SettingsScreen(vm: AppViewModel) {
    val s by vm.settings.collectAsState()
    ScreenFrame("Settings") {
        val pal = LocalPalette.current
        NHCard {
            SectionTitle("Luxury Colour Themes (${pal.name})")
            Text(
                "Tap any palette below to transform background, shadows, navigation dock & multi-color text:",
                fontSize = 12.sp,
                color = pal.subAccent,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Palettes.all.forEach { pp -> ThemeSwatch(pp, pp.id == pal.id) { vm.saveSettings(s.copy(themeId = pp.id)) } }
            }
        }
        NHCard {
            SectionTitle("Defaults for New Rooms")
            Text("Default Measurement Unit", color = pal.subAccent, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
            ChipRow(LengthUnit.values().toList(), s.unit, { it.label }) { vm.saveSettings(s.copy(unit = it)) }
            NumField("Default wastage", s.wastage, "w", "%", allowZero = true) { v -> if (v <= 100) vm.saveSettings(s.copy(wastage = v)) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Default grout width", color = pal.label, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text("${fmt(s.groutMm)} mm", color = pal.metric, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
            }
            Slider(s.groutMm.toFloat(), { vm.saveSettings(s.copy(groutMm = it.toDouble())) }, valueRange = 0f..20f)
            Text("Default grout colour", color = pal.subAccent, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(0xFFB8B8B8, 0xFFFFFFFF, 0xFF2B2B2B, 0xFFD4AF37, 0xFF8B7355, 0xFFE8DCC8).forEach {
                    val c = it.toInt(); ColorDot(c, s.groutColor == c) { vm.saveSettings(s.copy(groutColor = c)) }
                }
            }
            Text("Defaults apply to rooms created from now on.", fontSize = 12.sp, color = pal.label)
        }
        NHCard {
            SectionTitle("About NH Suite v3.5")
            StatRow("Application", "NH Tiles & Marble Designer")
            StatRow("Version", "3.5.0 Luxury Edition", true)
            StatRow("Package", "com.nh.tilesmarbledesigner")
            StatRow("Branding", "NH Developer")
            Text(
                "8 Luxury Palettes: Emerald & Amber, Royal Amethyst, Sapphire Lagoon, Saffron & Ruby, Velvet Rose, Platinum Indigo, Midnight Gold & Obsidian Neon.",
                fontSize = 12.sp,
                color = pal.subAccent,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "100% offline architecture. Declares zero INTERNET permissions; all projects, textures, and 3D exports remain private on your device.",
                fontSize = 12.sp,
                color = pal.label
            )
        }
    }
}
