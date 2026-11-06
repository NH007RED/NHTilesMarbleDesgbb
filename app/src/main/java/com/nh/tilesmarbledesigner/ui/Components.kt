package com.nh.tilesmarbledesigner.ui

import android.graphics.Bitmap
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nh.tilesmarbledesigner.data.*
import com.nh.tilesmarbledesigner.engine.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Locale

fun fmt(d: Double): String {
    val s = String.format(Locale.US, "%.3f", d).trimEnd('0').trimEnd('.')
    return if (s.isEmpty() || s == "-0") "0" else s
}

fun fmt2(d: Double): String = String.format(Locale.US, "%.2f", d)

fun parseNum(t: String): Double? = t.trim().replace(',', '.').toDoubleOrNull()

/**
 * Next-level multi-depth shadow system:
 * Combines strong chromatic spot & ambient shadows for deep floating 3D card elevation.
 */
@Composable
fun Modifier.softShadow(
    elevation: Dp = 10.dp,
    shape: Shape = RoundedCornerShape(22.dp),
    glow: Boolean = false
): Modifier {
    val pal = LocalPalette.current
    val amb = when {
        glow && pal.isDark -> pal.primary.copy(alpha = 0.50f)
        pal.isDark -> Color.Black.copy(alpha = 0.80f)
        glow -> pal.primary.copy(alpha = 0.42f)
        else -> pal.dark.copy(alpha = 0.34f)
    }
    val spot = when {
        glow && pal.isDark -> pal.vivid.copy(alpha = 0.65f)
        pal.isDark -> Color.Black.copy(alpha = 0.92f)
        glow -> pal.dark.copy(alpha = 0.62f)
        else -> pal.dark.copy(alpha = 0.56f)
    }
    return this.shadow(elevation, shape, clip = false, ambientColor = amb, spotColor = spot)
}

/**
 * Sculpted Luxury Top Header Bar + Dimensional Gradient Page Backdrop.
 */
@Composable
fun ScreenFrame(
    title: String, onBack: (() -> Unit)? = null, scroll: Boolean = true,
    actions: @Composable RowScope.() -> Unit = {}, content: @Composable ColumnScope.() -> Unit
) {
    val pal = LocalPalette.current
    val headerShape = RoundedCornerShape(bottomStart = 26.dp, bottomEnd = 26.dp)
    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(pal.bg, pal.bgBottom)))
    ) {
        // Elevated Luxury Top Header Bar with deep shadow & gradient border
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .softShadow(12.dp, headerShape),
            shape = headerShape,
            color = Color.Transparent
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(pal.cardTop, pal.card, pal.chip.copy(alpha = 0.85f))
                        ),
                        headerShape
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.horizontalGradient(
                            listOf(pal.borderHighlight, pal.subAccent.copy(alpha = 0.25f), pal.borderHighlight)
                        ),
                        shape = headerShape
                    )
                    .padding(
                        start = if (onBack != null) 12.dp else 18.dp,
                        end = 12.dp,
                        top = 10.dp,
                        bottom = 11.dp
                    )
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onBack != null) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(40.dp)
                                .softShadow(6.dp, CircleShape, glow = true)
                                .background(
                                    Brush.linearGradient(listOf(pal.dark, pal.primary)),
                                    CircleShape
                                )
                                .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                    } else {
                        // Subtle luxury jewel bar before top-level screen titles
                        Box(
                            Modifier
                                .width(5.dp)
                                .height(26.dp)
                                .softShadow(4.dp, RoundedCornerShape(3.dp), glow = true)
                                .background(
                                    Brush.verticalGradient(listOf(pal.vivid, pal.primary, pal.subAccent)),
                                    RoundedCornerShape(3.dp)
                                )
                        )
                        Spacer(Modifier.width(10.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        // Multi-color eye-catching title: first word in primary/accent, rest in subAccent/ink
                        val words = title.trim().split(" ", limit = 2)
                        Text(
                            text = buildAnnotatedString {
                                if (words.size == 2) {
                                    withStyle(SpanStyle(color = pal.accent, fontWeight = FontWeight.ExtraBold)) {
                                        append(words[0] + " ")
                                    }
                                    withStyle(SpanStyle(color = pal.subAccent, fontWeight = FontWeight.ExtraBold)) {
                                        append(words[1])
                                    }
                                } else {
                                    withStyle(SpanStyle(color = pal.accent, fontWeight = FontWeight.ExtraBold)) {
                                        append(title)
                                    }
                                }
                            },
                            fontSize = 21.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, content = actions)
                }
            }
        }

        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .then(if (scroll) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 26.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content
            )
        }
    }
}

/**
 * Luxury Elevated Card with gradient surface, specular border highlight, and deep 11.dp shadow.
 */
@Composable
fun NHCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val pal = LocalPalette.current
    val shape = RoundedCornerShape(22.dp)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .softShadow(11.dp, shape),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(
            1.2.dp,
            Brush.linearGradient(
                listOf(
                    pal.borderHighlight,
                    pal.subAccent.copy(alpha = 0.22f),
                    pal.ink.copy(alpha = 0.08f)
                )
            )
        )
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(pal.cardTop, pal.card)))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )
    }
}

/**
 * Eye-catching dual-tone Section Title with glowing gradient pill bar and subtle accent tag.
 */
@Composable
fun SectionTitle(t: String) {
    val pal = LocalPalette.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 2.dp, bottom = 2.dp)
    ) {
        Box(
            Modifier
                .width(5.dp)
                .height(20.dp)
                .softShadow(4.dp, RoundedCornerShape(3.dp), glow = true)
                .background(
                    Brush.verticalGradient(listOf(pal.vivid, pal.primary, pal.subAccent)),
                    RoundedCornerShape(3.dp)
                )
        )
        Spacer(Modifier.width(9.dp))
        val parts = t.trim().split(" ", limit = 2)
        Text(
            text = buildAnnotatedString {
                if (parts.size == 2) {
                    withStyle(SpanStyle(color = pal.accent, fontWeight = FontWeight.ExtraBold)) {
                        append(parts[0] + " ")
                    }
                    withStyle(SpanStyle(color = pal.subAccent, fontWeight = FontWeight.ExtraBold)) {
                        append(parts[1])
                    }
                } else {
                    withStyle(SpanStyle(color = pal.accent, fontWeight = FontWeight.ExtraBold)) {
                        append(t)
                    }
                }
            },
            fontSize = 16.sp
        )
    }
}

/**
 * Gradient Luxury Primary Button with glowing shadow and specular rim border.
 */
@Composable
fun PrimaryButton(text: String, enabled: Boolean = true, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val pal = LocalPalette.current
    val shape = RoundedCornerShape(18.dp)
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp)
            .softShadow(if (enabled) 10.dp else 2.dp, shape, glow = enabled),
        enabled = enabled,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = Color.White,
            disabledContainerColor = pal.chip.copy(alpha = 0.5f),
            disabledContentColor = pal.ink.copy(alpha = 0.4f)
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 50.dp)
                .background(
                    if (enabled) Brush.horizontalGradient(listOf(pal.dark, pal.primary, pal.dark))
                    else Brush.horizontalGradient(listOf(pal.chip, pal.chip)),
                    shape
                )
                .border(
                    1.dp,
                    if (enabled) Color.White.copy(alpha = 0.28f) else Color.Transparent,
                    shape
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp,
                color = if (enabled) Color.White else pal.ink.copy(alpha = 0.4f),
                textAlign = TextAlign.Center,
                letterSpacing = 0.3.sp
            )
        }
    }
}

/**
 * Luxury Tonal / Accent Button with subtle gradient fill, border highlight, and eye-catching text.
 */
@Composable
fun GoldButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    val pal = LocalPalette.current
    val shape = RoundedCornerShape(18.dp)
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .softShadow(6.dp, shape),
        enabled = enabled,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = pal.accent
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
                .background(
                    Brush.horizontalGradient(listOf(pal.cardTop, pal.chip, pal.cardTop)),
                    shape
                )
                .border(
                    1.2.dp,
                    Brush.horizontalGradient(listOf(pal.primary.copy(alpha = 0.45f), pal.subAccent.copy(alpha = 0.45f))),
                    shape
                )
                .padding(horizontal = 14.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp,
                color = pal.subAccent,
                textAlign = TextAlign.Center
            )
        }
    }
}

/** Same-package replacement for material3 OutlinedButton: luxury tonal pill with shadow & border. */
@Composable
fun OutlinedButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, content: @Composable RowScope.() -> Unit) {
    val pal = LocalPalette.current
    val shape = RoundedCornerShape(15.dp)
    Button(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = 44.dp)
            .softShadow(if (enabled) 5.dp else 1.dp, shape),
        enabled = enabled,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = pal.chip,
            contentColor = pal.accent,
            disabledContainerColor = pal.chip.copy(alpha = 0.5f),
            disabledContentColor = pal.ink.copy(alpha = 0.4f)
        ),
        border = BorderStroke(1.dp, pal.primary.copy(alpha = 0.30f)),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    ) { content() }
}

@Composable
fun fieldColors(): TextFieldColors {
    val pal = LocalPalette.current
    return OutlinedTextFieldDefaults.colors(
        focusedContainerColor = pal.cardTop,
        unfocusedContainerColor = pal.chip.copy(alpha = 0.75f),
        errorContainerColor = pal.chip,
        focusedBorderColor = pal.primary,
        unfocusedBorderColor = pal.primary.copy(alpha = 0.25f),
        focusedLabelColor = pal.subAccent,
        unfocusedLabelColor = pal.label,
        focusedTextColor = pal.ink,
        unfocusedTextColor = pal.ink,
        focusedSuffixColor = pal.metric,
        unfocusedSuffixColor = pal.subAccent,
        cursorColor = pal.primary
    )
}

@Composable
fun NumField(
    label: String, value: Double, key: Any?, suffix: String, modifier: Modifier = Modifier,
    allowZero: Boolean = false, onValue: (Double) -> Unit
) {
    var text by remember(key) { mutableStateOf(fmt(value)) }
    val parsed = parseNum(text)
    val bad = parsed == null || parsed < 0 || (!allowZero && parsed <= 0)
    OutlinedTextField(
        value = text,
        onValueChange = { t -> text = t; parseNum(t)?.let { if (it >= 0) onValue(it) } },
        label = { Text(label, fontWeight = FontWeight.SemiBold) },
        suffix = { Text(suffix, fontWeight = FontWeight.Bold, color = LocalPalette.current.metric) },
        singleLine = true,
        isError = bad,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier.fillMaxWidth().softShadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = fieldColors(),
        supportingText = if (bad) { { Text(if (allowZero) "Enter 0 or more" else "Enter a number above 0", fontSize = 11.sp) } } else null
    )
}

/** Field showing a metre value in the project's unit. */
@Composable
fun DimField(label: String, meters: Double, unit: LengthUnit, modifier: Modifier = Modifier, onMeters: (Double) -> Unit) =
    NumField(label, unit.fromM(meters), unit, unit.label, modifier) { onMeters(unit.toM(it)) }

@Composable
fun IntField(label: String, value: Int, min: Int, modifier: Modifier = Modifier, onValue: (Int) -> Unit) {
    var text by remember { mutableStateOf(value.toString()) }
    val parsed = text.toIntOrNull()
    OutlinedTextField(
        value = text,
        onValueChange = { t -> text = t.filter { it.isDigit() }.take(6); text.toIntOrNull()?.let { if (it >= min) onValue(it) } },
        label = { Text(label, fontWeight = FontWeight.SemiBold) },
        singleLine = true,
        isError = parsed == null || parsed < min,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier.fillMaxWidth().softShadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = fieldColors(),
        supportingText = if (parsed == null || parsed < min) { { Text("Minimum $min", fontSize = 11.sp) } } else null
    )
}

@Composable
fun <T> ChipRow(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    val pal = LocalPalette.current
    Row(
        Modifier
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        options.forEach { o ->
            val isSel = o == selected
            val shape = RoundedCornerShape(18.dp)
            FilterChip(
                selected = isSel,
                onClick = { onSelect(o) },
                label = {
                    Text(
                        label(o),
                        fontWeight = if (isSel) FontWeight.ExtraBold else FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                },
                modifier = Modifier.softShadow(if (isSel) 7.dp else 3.dp, shape, glow = isSel),
                shape = shape,
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSel,
                    borderColor = pal.primary.copy(alpha = 0.28f),
                    selectedBorderColor = pal.vivid.copy(alpha = 0.7f),
                    borderWidth = 1.dp,
                    selectedBorderWidth = 1.5.dp
                ),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = pal.card,
                    labelColor = pal.subAccent,
                    selectedContainerColor = pal.dark,
                    selectedLabelColor = Color.White
                )
            )
        }
    }
}

@Composable
fun StatRow(label: String, value: String, big: Boolean = false) {
    val pal = LocalPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .then(
                if (big) Modifier
                    .background(pal.chip.copy(alpha = 0.65f), RoundedCornerShape(12.dp))
                    .border(1.dp, pal.metric.copy(alpha = 0.30f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                else Modifier.padding(vertical = 2.dp)
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            Modifier.weight(1f, fill = false),
            color = if (big) pal.accent else pal.label,
            fontWeight = if (big) FontWeight.Bold else FontWeight.Medium,
            fontSize = if (big) 14.sp else 13.sp
        )
        Spacer(Modifier.width(10.dp))
        Text(
            value,
            fontWeight = FontWeight.ExtraBold,
            color = if (big) pal.metric else pal.subAccent,
            fontSize = if (big) 19.sp else 14.sp,
            textAlign = TextAlign.End
        )
    }
}

@Composable
fun ColorDot(c: Int, selected: Boolean, onClick: () -> Unit) {
    val pal = LocalPalette.current
    Box(
        Modifier
            .size(38.dp)
            .softShadow(if (selected) 8.dp else 3.dp, CircleShape, glow = selected)
            .clip(CircleShape)
            .background(Color(c))
            .border(
                if (selected) 3.dp else 1.2.dp,
                if (selected) pal.vivid else pal.ink.copy(alpha = 0.3f),
                CircleShape
            )
            .clickable(onClick = onClick)
    )
}

/** Small rounded label used on top of 3D views. */
@Composable
fun Tag(text: String, modifier: Modifier = Modifier) {
    val pal = LocalPalette.current
    val shape = RoundedCornerShape(14.dp)
    Text(
        text,
        modifier
            .softShadow(6.dp, shape)
            .background(pal.card.copy(alpha = 0.92f), shape)
            .border(1.dp, pal.primary.copy(alpha = 0.35f), shape)
            .padding(horizontal = 13.dp, vertical = 6.dp),
        color = pal.subAccent,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 11.sp
    )
}

/** Circular luxury icon badge with gradient fill, subtle glow shadow, and crisp border. */
@Composable
fun IconBadge(icon: androidx.compose.ui.graphics.vector.ImageVector, size: Int = 40) {
    val pal = LocalPalette.current
    Box(
        Modifier
            .size(size.dp)
            .softShadow(6.dp, CircleShape, glow = true)
            .background(
                Brush.linearGradient(listOf(pal.dark, pal.primary)),
                CircleShape
            )
            .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size((size * 0.5f).dp))
    }
}

/** Theme preview tile: luxury card with multi-color dots and label; selected one gets glowing ring and check. */
@Composable
fun ThemeSwatch(p: NHPalette, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            Modifier
                .size(86.dp)
                .softShadow(if (selected) 12.dp else 5.dp, shape, glow = selected)
                .clip(shape)
                .background(Brush.verticalGradient(listOf(p.bg, p.chip)))
                .border(
                    if (selected) 3.dp else 1.dp,
                    if (selected) p.vivid else p.primary.copy(alpha = 0.35f),
                    shape
                )
        ) {
            Box(Modifier.align(Alignment.TopStart).padding(10.dp).size(15.dp).background(p.primary, RoundedCornerShape(5.dp)))
            Box(Modifier.align(Alignment.TopEnd).padding(10.dp).size(15.dp).background(p.subAccent, RoundedCornerShape(5.dp)))
            Box(Modifier.align(Alignment.BottomStart).padding(10.dp).size(12.dp).background(p.metric, CircleShape))
            Box(Modifier.align(Alignment.BottomEnd).padding(10.dp).size(15.dp).background(p.dark, RoundedCornerShape(5.dp)))
            if (selected) {
                Box(
                    Modifier
                        .align(Alignment.Center)
                        .size(32.dp)
                        .softShadow(6.dp, CircleShape)
                        .background(p.dark, CircleShape)
                        .border(1.5.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }
        Text(
            p.name.substringBefore(' '),
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
            color = if (selected) LocalPalette.current.subAccent else LocalPalette.current.label
        )
    }
}

@Composable
fun MaterialSwatch(id: String, name: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val pal = LocalPalette.current
    val bmp by produceState<Bitmap?>(null, id) { value = withContext(Dispatchers.Default) { Materials.bitmap(id) } }
    val shape = RoundedCornerShape(18.dp)
    Column(modifier.clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(72.dp)
                .softShadow(if (selected) 9.dp else 4.dp, shape, glow = selected)
                .clip(shape)
                .border(
                    if (selected) 3.dp else 1.2.dp,
                    if (selected) pal.vivid else pal.primary.copy(alpha = 0.25f),
                    shape
                )
        ) {
            bmp?.let { Image(it.asImageBitmap(), name, Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop) }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            name,
            Modifier.width(76.dp),
            fontSize = 11.sp,
            maxLines = 2,
            textAlign = TextAlign.Center,
            lineHeight = 13.sp,
            color = if (selected) pal.subAccent else pal.ink,
            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium
        )
    }
}

/** Computes (debounced, cached) surface textures off the UI thread. Keeps the previous result while recomputing. */
@Composable
fun rememberSceneBitmaps(p: ProjectData, designs: Map<Surf, SurfaceDesign>, enabled: Boolean = true): State<Map<Surf, Bitmap>?> =
    produceState<Map<Surf, Bitmap>?>(null, p.sections, p.height, p.openings, designs, enabled) {
        if (!enabled) { value = null; return@produceState }
        delay(60)
        value = withContext(Dispatchers.Default) { Scene.surfaceBitmaps(p, designs) }
    }

/** Interactive perspective room view. If [before] is set, draws a split before/after comparison at [split]. */
@Composable
fun RoomCanvas(
    after: Map<Surf, Bitmap>?, before: Map<Surf, Bitmap>?, p: ProjectData,
    yaw: Float, pitch: Float, zoom: Float, split: Float = 0.5f, modifier: Modifier = Modifier,
    boxes: List<Box3> = emptyList(), rounded: Boolean = true,
    onGesture: ((Offset, Float) -> Unit)? = null
) {
    val pal = LocalPalette.current
    val stageTop = pal.soft; val stageBottom = pal.bg; val divider = pal.vivid
    val shape = RoundedCornerShape(24.dp)
    val m = if (onGesture != null) modifier.pointerInput(Unit) { detectTransformGestures { _, pan, z, _ -> onGesture(pan, z) } } else modifier
    Canvas(
        if (rounded) m
            .softShadow(14.dp, shape, glow = true)
            .clip(shape)
            .border(1.5.dp, Brush.linearGradient(listOf(pal.borderHighlight, pal.subAccent.copy(alpha = 0.35f))), shape)
        else m
    ) {
        drawRect(Brush.verticalGradient(listOf(stageTop, stageBottom)))
        if (after == null) return@Canvas
        drawIntoCanvas { cv ->
            val n = cv.nativeCanvas
            val L = p.roomL.toFloat(); val W = p.roomW.toFloat(); val H = p.height.toFloat()
            if (before != null) {
                n.save(); n.clipRect(0f, 0f, size.width * split, size.height)
                RoomRenderer.render(n, size.width, size.height, L, W, H, after, yaw, pitch, zoom, boxes); n.restore()
                n.save(); n.clipRect(size.width * split, 0f, size.width, size.height)
                RoomRenderer.render(n, size.width, size.height, L, W, H, before, yaw, pitch, zoom); n.restore()
            } else RoomRenderer.render(n, size.width, size.height, L, W, H, after, yaw, pitch, zoom, boxes)
        }
        if (before != null) drawLine(divider, Offset(size.width * split, 0f), Offset(size.width * split, size.height), 5f)
    }
}
