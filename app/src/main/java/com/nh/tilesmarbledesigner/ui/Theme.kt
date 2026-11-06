package com.nh.tilesmarbledesigner.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

/**
 * v3.5 Next-Level Luxury Palette System:
 * Combines rich surface depth with a multi-color eye-catching text hierarchy:
 * - [primary]: vibrant brand/section hue
 * - [dark]: deep rich button/active capsule color
 * - [ink]: ultra-crisp primary body & card headline color
 * - [subAccent]: contrasting eye-catching secondary text color (warm amber, cyan, coral, magenta, gold)
 * - [metric]: vivid high-impact color for numbers, areas, box counts & key stats
 * - [vivid]: luminous highlight / active indicator spark color
 */
data class NHPalette(
    val id: String,
    val name: String,
    val bg: Color,
    val card: Color,
    val chip: Color,
    val primary: Color,
    val dark: Color,
    val soft: Color,
    val ink: Color,
    val subAccent: Color,
    val metric: Color,
    val vivid: Color,
    val isDark: Boolean = false
)

object Palettes {
    val all = listOf(
        NHPalette(
            id = "forest", name = "Emerald & Amber",
            bg = Color(0xFFE6F2E4), card = Color(0xFFF7FCF5), chip = Color(0xFFD6EBD2),
            primary = Color(0xFF1E824C), dark = Color(0xFF0B4D2C), soft = Color(0xFFBBE2B5),
            ink = Color(0xFF0D2E1B), subAccent = Color(0xFFC85A17), metric = Color(0xFFB8336A), vivid = Color(0xFFF59E0B)
        ),
        NHPalette(
            id = "lavender", name = "Royal Amethyst",
            bg = Color(0xFFEDE2F8), card = Color(0xFFFAF5FF), chip = Color(0xFFE0CEF5),
            primary = Color(0xFF8B3DFF), dark = Color(0xFF4C1D95), soft = Color(0xFFD4B8F8),
            ink = Color(0xFF28104E), subAccent = Color(0xFF0E7490), metric = Color(0xFFD9386A), vivid = Color(0xFF06B6D4)
        ),
        NHPalette(
            id = "ocean", name = "Sapphire Lagoon",
            bg = Color(0xFFDDF3F8), card = Color(0xFFF2FCFF), chip = Color(0xFFC5E8F2),
            primary = Color(0xFF0284C7), dark = Color(0xFF1E3A8A), soft = Color(0xFFA5DEF1),
            ink = Color(0xFF0A2742), subAccent = Color(0xFFD9531E), metric = Color(0xFF7C3AED), vivid = Color(0xFF00B4D8)
        ),
        NHPalette(
            id = "sunset", name = "Saffron & Ruby",
            bg = Color(0xFFFCE8D5), card = Color(0xFFFFF8F2), chip = Color(0xFFF8D6B5),
            primary = Color(0xFFEA580C), dark = Color(0xFF9A1B1B), soft = Color(0xFFF7C99B),
            ink = Color(0xFF431407), subAccent = Color(0xFF7E22CE), metric = Color(0xFF0F766E), vivid = Color(0xFFF59E0B)
        ),
        NHPalette(
            id = "rose", name = "Velvet Rose & Gold",
            bg = Color(0xFFFCE2EC), card = Color(0xFFFFF5F8), chip = Color(0xFFF9C8DB),
            primary = Color(0xFFE11D48), dark = Color(0xFF881337), soft = Color(0xFFF7B2CD),
            ink = Color(0xFF4C0519), subAccent = Color(0xFFB45309), metric = Color(0xFF6D28D9), vivid = Color(0xFFFB7185)
        ),
        NHPalette(
            id = "slate", name = "Platinum Indigo",
            bg = Color(0xFFE9ECF5), card = Color(0xFFF9FAFE), chip = Color(0xFFD7DEF0),
            primary = Color(0xFF4F46E5), dark = Color(0xFF1E1B4B), soft = Color(0xFFC2CCEB),
            ink = Color(0xFF111827), subAccent = Color(0xFF0D9488), metric = Color(0xFFE11D48), vivid = Color(0xFF6366F1)
        ),
        NHPalette(
            id = "midnight", name = "Midnight Gold",
            bg = Color(0xFF080D1A), card = Color(0xFF121D35), chip = Color(0xFF1C2D52),
            primary = Color(0xFFF5C451), dark = Color(0xFF2563EB), soft = Color(0xFF2B4270),
            ink = Color(0xFFF8FAFC), subAccent = Color(0xFF38BDF8), metric = Color(0xFFFBBF24), vivid = Color(0xFFFFD166),
            isDark = true
        ),
        NHPalette(
            id = "royal", name = "Obsidian Neon",
            bg = Color(0xFF0B0914), card = Color(0xFF171329), chip = Color(0xFF251E42),
            primary = Color(0xFF00E5FF), dark = Color(0xFF7C3AED), soft = Color(0xFF372C63),
            ink = Color(0xFFF5F3FF), subAccent = Color(0xFFFF7597), metric = Color(0xFF00F5D4), vivid = Color(0xFFFFBE0B),
            isDark = true
        )
    )

    fun byId(id: String?): NHPalette = all.firstOrNull { it.id == id } ?: all[0]
}

val LocalPalette = staticCompositionLocalOf { Palettes.all[0] }

/** High-contrast primary title & icon accent color. */
val NHPalette.accent: Color get() = if (isDark) primary else dark

/** Distinct eye-catching secondary text color for subtitles, tags & category captions. */
val NHPalette.eyeCatchingSub: Color get() = subAccent

/** Vivid high-impact color for numbers, measurements, areas, and key counts. */
val NHPalette.highlight: Color get() = metric

/** Readable chromatic label color for form labels and descriptive text. */
val NHPalette.label: Color get() = if (isDark) lerp(ink, subAccent, 0.38f) else lerp(ink, primary, 0.42f)

/** Subtle top surface sheen for gradient cards. */
val NHPalette.cardTop: Color get() = if (isDark) lerp(card, chip, 0.45f) else Color.White

/** Subtle bottom page gradient tint. */
val NHPalette.bgBottom: Color get() = if (isDark) lerp(bg, chip, 0.35f) else lerp(bg, soft, 0.42f)

/** Luminous border highlight color. */
val NHPalette.borderHighlight: Color get() = if (isDark) primary.copy(alpha = 0.42f) else dark.copy(alpha = 0.22f)

// Palette-aware colour accessors (names kept so every screen follows the active theme).
val Gold: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.accent      // readable accent: titles / icons
val Blue: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.dark        // buttons / selection
val Navy: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.bg          // page background
val NavySurface: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.card // cards
val NavyCard: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.chip    // fields / chips
val Ink: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.ink          // body text
val Danger = Color(0xFFE5484D)

private tailrec fun Context.findAct(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findAct()
    else -> null
}

@Composable
private fun SystemBars(p: NHPalette) {
    val view = LocalView.current
    if (!view.isInEditMode) SideEffect {
        val w = view.context.findAct()?.window ?: return@SideEffect
        w.statusBarColor = p.bg.toArgb()
        w.navigationBarColor = p.bgBottom.toArgb()
        val c = WindowCompat.getInsetsController(w, view)
        c.isAppearanceLightStatusBars = !p.isDark
        c.isAppearanceLightNavigationBars = !p.isDark
    }
}

private val base = Typography()
private val NHType = Typography(
    bodyLarge = base.bodyLarge.copy(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
    bodyMedium = base.bodyMedium.copy(fontSize = 13.sp, lineHeight = 18.sp),
    bodySmall = base.bodySmall.copy(fontSize = 11.sp, lineHeight = 15.sp),
    labelLarge = base.labelLarge.copy(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = base.labelMedium.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium),
    titleMedium = base.titleMedium.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
    titleLarge = base.titleLarge.copy(fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
)

@Composable
fun NHTheme(palette: NHPalette, content: @Composable () -> Unit) {
    val scheme = if (palette.isDark) darkColorScheme(
        primary = palette.dark, onPrimary = Color.White, primaryContainer = palette.chip, onPrimaryContainer = palette.ink,
        secondary = palette.primary, onSecondary = Color.Black, secondaryContainer = palette.chip, onSecondaryContainer = palette.subAccent,
        tertiary = palette.metric, onTertiary = Color.Black,
        background = palette.bg, onBackground = palette.ink, surface = palette.card, onSurface = palette.ink,
        surfaceVariant = palette.chip, onSurfaceVariant = palette.label,
        outline = palette.primary.copy(alpha = 0.45f), outlineVariant = palette.ink.copy(alpha = 0.15f), error = Danger
    ) else lightColorScheme(
        primary = palette.dark, onPrimary = Color.White, primaryContainer = palette.chip, onPrimaryContainer = palette.ink,
        secondary = palette.primary, onSecondary = Color.White, secondaryContainer = palette.chip, onSecondaryContainer = palette.subAccent,
        tertiary = palette.metric, onTertiary = Color.White,
        background = palette.bg, onBackground = palette.ink, surface = palette.card, onSurface = palette.ink,
        surfaceVariant = palette.chip, onSurfaceVariant = palette.label,
        outline = palette.dark.copy(alpha = 0.40f), outlineVariant = palette.ink.copy(alpha = 0.14f), error = Danger
    )
    CompositionLocalProvider(LocalPalette provides palette) {
        MaterialTheme(
            colorScheme = scheme,
            typography = NHType,
            shapes = Shapes(
                small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(16.dp),
                large = RoundedCornerShape(22.dp), extraLarge = RoundedCornerShape(28.dp)
            )
        ) {
            SystemBars(palette)
            content()
        }
    }
}
