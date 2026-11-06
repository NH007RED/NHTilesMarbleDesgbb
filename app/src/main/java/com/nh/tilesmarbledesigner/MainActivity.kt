package com.nh.tilesmarbledesigner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.nh.tilesmarbledesigner.ui.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val vm: AppViewModel = viewModel()
            val settings by vm.settings.collectAsState()
            NHTheme(Palettes.byId(settings.themeId)) { App(vm) }
        }
    }
}

private data class Tab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val isCenterFeature: Boolean = false
)

@Composable
private fun LuxuryBottomDock(
    tabs: List<Tab>,
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    val pal = LocalPalette.current
    val dockShape = RoundedCornerShape(28.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Transparent)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .softShadow(18.dp, dockShape, glow = true),
            shape = dockShape,
            color = Color.Transparent
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(pal.cardTop, pal.card, pal.chip.copy(alpha = 0.90f))
                        ),
                        dockShape
                    )
                    .border(
                        width = 1.4.dp,
                        brush = Brush.horizontalGradient(
                            listOf(
                                pal.primary.copy(alpha = 0.45f),
                                pal.vivid.copy(alpha = 0.55f),
                                pal.subAccent.copy(alpha = 0.45f)
                            )
                        ),
                        shape = dockShape
                    )
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEach { tab ->
                    val selected = currentRoute.substringBefore('/') == tab.route.substringBefore('/')
                    LuxuryDockItem(
                        tab = tab,
                        selected = selected,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(tab.route) }
                    )
                }
            }
        }
    }
}

@Composable
private fun LuxuryDockItem(
    tab: Tab,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val pal = LocalPalette.current
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.05f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "navScale"
    )
    val dotWidth by animateDpAsState(
        targetValue = if (selected) 20.dp else 0.dp,
        animationSpec = tween(220),
        label = "dotWidth"
    )
    val labelColor by animateColorAsState(
        targetValue = when {
            selected -> Color.White
            tab.isCenterFeature -> pal.subAccent
            else -> pal.label
        },
        animationSpec = tween(200),
        label = "navLabelColor"
    )
    val pillShape = RoundedCornerShape(20.dp)

    Box(
        modifier = modifier
            .scale(scale)
            .padding(horizontal = 3.dp)
            .then(
                if (selected) {
                    Modifier
                        .softShadow(9.dp, pillShape, glow = true)
                        .background(
                            Brush.linearGradient(
                                listOf(pal.dark, pal.primary, pal.subAccent)
                            ),
                            pillShape
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.32f), pillShape)
                } else if (tab.isCenterFeature) {
                    Modifier
                        .background(pal.chip.copy(alpha = 0.65f), pillShape)
                        .border(1.dp, pal.primary.copy(alpha = 0.25f), pillShape)
                } else {
                    Modifier
                }
            )
            .clip(pillShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 7.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            // Glowing top spark indicator when selected
            if (selected) {
                Box(
                    Modifier
                        .width(dotWidth)
                        .height(3.dp)
                        .background(pal.vivid, CircleShape)
                )
            } else {
                Spacer(Modifier.height(3.dp))
            }

            Icon(
                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                contentDescription = tab.label,
                tint = when {
                    selected -> Color.White
                    tab.isCenterFeature -> pal.subAccent
                    else -> pal.accent.copy(alpha = 0.80f)
                },
                modifier = Modifier.size(if (selected) 22.dp else 20.dp)
            )

            Text(
                text = tab.label,
                color = labelColor,
                fontSize = 11.sp,
                fontWeight = if (selected || tab.isCenterFeature) FontWeight.ExtraBold else FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun App(vm: AppViewModel) {
    val pal = LocalPalette.current
    val nav = rememberNavController()
    val snack = remember { SnackbarHostState() }
    LaunchedEffect(vm.toast) { vm.toast?.let { snack.showSnackbar(it); vm.toast = null } }
    val route = nav.currentBackStackEntryAsState().value?.destination?.route
    val tabs = listOf(
        Tab("home", "Home", Icons.Filled.Home, Icons.Outlined.Home),
        Tab("measure", "Measure", Icons.Filled.Straighten, Icons.Outlined.Straighten),
        Tab("studio/wall", "Studio", Icons.Filled.Palette, Icons.Outlined.Palette, isCenterFeature = true),
        Tab("projects", "Projects", Icons.Filled.Folder, Icons.Outlined.Folder),
        Tab("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
    )
    val tabRoutes = tabs.map { it.route }.toSet()
    val showBar = route != null && (route in setOf("home", "measure", "projects", "settings") || route.startsWith("studio"))
    val go: (String) -> Unit = { r ->
        if (r in tabRoutes) nav.navigate(r) { popUpTo("home"); launchSingleTop = true } else nav.navigate(r)
    }
    val back: () -> Unit = { nav.popBackStack() }

    Scaffold(
        containerColor = pal.bg,
        snackbarHost = { SnackbarHost(snack) },
        bottomBar = {
            if (showBar && route != null) {
                LuxuryBottomDock(
                    tabs = tabs,
                    currentRoute = route,
                    onNavigate = go
                )
            }
        }
    ) { pad ->
        NavHost(
            nav, "splash", Modifier.padding(pad).background(pal.bg),
            enterTransition = { fadeIn(tween(220)) + slideInHorizontally(tween(220)) { it / 14 } },
            exitTransition = { fadeOut(tween(160)) },
            popEnterTransition = { fadeIn(tween(220)) },
            popExitTransition = { fadeOut(tween(160)) + slideOutHorizontally(tween(160)) { it / 14 } }
        ) {
            composable("splash") { SplashScreen { nav.navigate("home") { popUpTo("splash") { inclusive = true } } } }
            composable("home") { HomeScreen(vm, go) }
            composable("create") { CreateRoomScreen(vm, go, back) }
            composable("measure") { MeasurementScreen(vm, go) }
            composable("wall") { WallScreen(vm, back) }
            composable("floor") { FloorScreen(vm, back) }
            composable("calc") { CalculatorScreen(vm, go, back) }
            composable("results") { ResultsScreen(vm, go, back) }
            composable("library") { LibraryScreen(vm, go, back) }
            composable("studio/{mode}", listOf(navArgument("mode") { type = NavType.StringType })) {
                StudioScreen(vm, it.arguments?.getString("mode") ?: "wall", go)
            }
            composable("crop") { CropScreen(vm, go, back) }
            composable("texture") { TextureScreen(vm, go, back) }
            composable("preview") { PreviewScreen(vm, go, back) }
            composable("projects") { ProjectsScreen(vm, go) }
            composable("details/{id}", listOf(navArgument("id") { type = NavType.LongType })) {
                DetailsScreen(vm, it.arguments?.getLong("id") ?: 0L, go, back)
            }
            composable("templates") { TemplatesScreen(vm, go, back) }
            composable("furniture") { FurnitureScreen(vm, go, back) }
            composable("fullview") { FullViewScreen(vm, back) }
            composable("export") { ExportScreen(vm, back) }
            composable("settings") { SettingsScreen(vm) }
        }
    }
}
