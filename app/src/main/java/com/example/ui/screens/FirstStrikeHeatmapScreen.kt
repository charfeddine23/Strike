package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Individual
import com.example.ui.components.GlassTypeBadge
import com.example.ui.components.hasLocationPermission
import com.example.ui.components.resolveBestLocation
import com.example.ui.map.FirstStrikeHotspot
import com.example.ui.map.HeatmapDisplayMode
import com.example.ui.map.MapStyleHelper
import com.example.ui.map.MapVisualTheme
import com.example.ui.map.isRobolectricRuntime
import com.example.ui.theme.DayCyan
import com.example.ui.theme.DayTextPrimary
import com.example.ui.theme.LocalAppDarkTheme
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.PlusJakartaSans
import com.example.ui.viewmodel.IndividualViewModel
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FirstStrikeHeatmapScreen(
    viewModel: IndividualViewModel,
    currentLang: String,
    isAppDarkTheme: Boolean = LocalAppDarkTheme.current,
    onEnterApp: () -> Unit,
    onNavigateToPersonProfile: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val isAr = currentLang == "ar"
    val isFr = currentLang == "fr"

    val allIndividuals by viewModel.allIndividuals.collectAsStateWithLifecycle()
    val hotspots = remember(allIndividuals) { MapStyleHelper.groupHotspots(allIndividuals) }
    val geoCount = remember(allIndividuals) { allIndividuals.count { it.hasFirstStrikeLocation } }

    // Default to Obsidian Dark Mode on MapLibre
    var mapVisualTheme by remember { mutableStateOf(MapVisualTheme.OBSIDIAN_DARK) }
    var show3dBuildings by remember { mutableStateOf(true) }
    var displayMode by remember { mutableStateOf(HeatmapDisplayMode.HYBRID) }
    var showLayersMenu by remember { mutableStateOf(false) }

    var userLat by remember { mutableStateOf<Double?>(null) }
    var userLng by remember { mutableStateOf<Double?>(null) }
    var selectedHotspot by remember { mutableStateOf<FirstStrikeHotspot?>(null) }

    var mapLibreRef by remember { mutableStateOf<MapLibreMap?>(null) }
    var loadedStyleRef by remember { mutableStateOf<Style?>(null) }
    var hasAutoFramed by remember { mutableStateOf(false) }

    val requestLocationAndAnimate = {
        if (hasLocationPermission(context)) {
            resolveBestLocation(context) { lat, lng ->
                userLat = lat
                userLng = lng
                mapLibreRef?.let { map ->
                    MapStyleHelper.animateCameraTo(map, lat, lng, zoom = 14.5, durationMs = 800)
                }
            }
        } else {
            val msg = when {
                isAr -> "يرجى السماح بالوصول إلى الموقع لعرض موقعك على الخريطة"
                isFr -> "Veuillez autoriser l'accès à la position"
                else -> "Location permission is needed to center on your position"
            }
            viewModel.notifyMessage(msg)
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            requestLocationAndAnimate()
        } else {
            val deniedMsg = when {
                isAr -> "تم رفض إذن الموقع — يمكنك استخدام الخريطة يدوياً"
                isFr -> "Permission de localisation refusée"
                else -> "Location permission denied — pan the map manually"
            }
            viewModel.notifyMessage(deniedMsg)
        }
    }

    // Keep MapLibre style synced with layer toggles & Room geospatial data
    LaunchedEffect(loadedStyleRef, mapVisualTheme, show3dBuildings, displayMode, allIndividuals, userLat, userLng) {
        val style = loadedStyleRef ?: return@LaunchedEffect
        MapStyleHelper.applyGoogleGradeStyling(
            style = style,
            theme = mapVisualTheme,
            show3dBuildings = show3dBuildings,
            displayMode = displayMode,
            individuals = allIndividuals,
            userLat = userLat,
            userLng = userLng
        )
    }

    // Frame existing first-strike locations smoothly once on launch if present
    LaunchedEffect(mapLibreRef, hotspots) {
        val map = mapLibreRef ?: return@LaunchedEffect
        if (!hasAutoFramed && hotspots.isNotEmpty()) {
            hasAutoFramed = true
            if (hotspots.size == 1) {
                MapStyleHelper.animateCameraTo(
                    map,
                    hotspots.first().lat,
                    hotspots.first().lng,
                    zoom = 11.5,
                    durationMs = 800
                )
            } else {
                val builder = LatLngBounds.Builder()
                hotspots.forEach {
                    builder.include(org.maplibre.android.geometry.LatLng(it.lat, it.lng))
                }
                try {
                    map.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 140), 800)
                } catch (_: Throwable) {
                    // Bounds degenerate if all points identical
                }
            }
        }
    }

    val isMapDark = mapVisualTheme == MapVisualTheme.OBSIDIAN_DARK
    val glassBg = if (isMapDark) Color(0xE80A1120) else Color(0xEBFFFFFF)
    val glassBorder = if (isMapDark) Color(0x4538BDF8) else Color(0x660284C7)
    val accentCyan = if (isMapDark || isAppDarkTheme) NeonCyan else DayCyan
    val textPrimary = if (isMapDark) Color(0xFFF8FAFC) else DayTextPrimary
    val textSecondary = if (isMapDark) Color(0xFF94A3B8) else Color(0xFF475569)

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("first_strike_heatmap_screen")
    ) {
        // 1. Full-Viewport MapLibre Native Canvas (Robolectric-safe fallback on JVM tests)
        if (!isRobolectricRuntime()) {
            val mapView = remember {
                MapStyleHelper.createHighPrecisionMapView(context).apply {
                    onCreate(null)
                    getMapAsync { map ->
                        mapLibreRef = map
                        MapStyleHelper.configureHighPrecisionMap(map, context)
                        map.cameraPosition = CameraPosition.Builder()
                            .target(MapStyleHelper.DEFAULT_CENTER)
                            .zoom(MapStyleHelper.DEFAULT_ZOOM)
                            .build()
                        map.setStyle(Style.Builder().fromUri(MapStyleHelper.OPEN_FREE_MAP_LIBERTY_STYLE_URI)) { style ->
                            loadedStyleRef = style
                            MapStyleHelper.applyGoogleGradeStyling(
                                style = style,
                                theme = mapVisualTheme,
                                show3dBuildings = show3dBuildings,
                                displayMode = displayMode,
                                individuals = allIndividuals,
                                userLat = userLat,
                                userLng = userLng
                            )
                        }
                        map.addOnMapClickListener { point ->
                            val screenPt = map.projection.toScreenLocation(point)
                            val rendered = map.queryRenderedFeatures(
                                screenPt,
                                MapStyleHelper.CIRCLE_LAYER_ID,
                                MapStyleHelper.HEATMAP_LAYER_ID
                            )
                            val clickedId = rendered.firstOrNull()?.getNumberProperty("id")?.toLong()
                            val hitHotspot = if (clickedId != null) {
                                hotspots.firstOrNull { hs -> hs.individuals.any { it.id == clickedId } }
                            } else {
                                MapStyleHelper.findNearestHotspot(
                                    tapLat = point.latitude,
                                    tapLng = point.longitude,
                                    hotspots = hotspots,
                                    zoomLevel = map.cameraPosition.zoom
                                )
                            }
                            if (hitHotspot != null) {
                                selectedHotspot = hitHotspot
                                true
                            } else {
                                false
                            }
                        }
                    }
                }
            }

            DisposableEffect(lifecycleOwner, mapView) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_START -> mapView.onStart()
                        Lifecycle.Event.ON_RESUME -> mapView.onResume()
                        Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                        Lifecycle.Event.ON_STOP -> mapView.onStop()
                        else -> Unit
                    }
                }
                if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                    mapView.onStart()
                }
                if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                    mapView.onResume()
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                    mapView.onPause()
                    mapView.onStop()
                    mapView.onDestroy()
                }
            }

            AndroidView(
                factory = { mapView },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // High-craft dark/light tactical canvas preview for Robolectric JVM environment
            Box(modifier = Modifier.fillMaxSize()) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(if (isMapDark) Color(0xFF0F172A) else Color(0xFFF8F9FA))
                    // Simulated water & grid contours
                    drawRect(
                        color = if (isMapDark) Color(0xFF0E1626) else Color(0xFFAADAFF),
                        topLeft = Offset(0f, 0f),
                        size = androidx.compose.ui.geometry.Size(size.width, size.height * 0.25f)
                    )
                    // Draw glowing heatmap blobs for recorded hotspots
                    hotspots.forEachIndexed { idx, _ ->
                        val cx = size.width * (0.28f + ((idx * 0.21f) % 0.48f))
                        val cy = size.height * (0.38f + ((idx * 0.14f) % 0.28f))
                        drawCircle(
                            brush = Brush.radialGradient(
                                listOf(
                                    Color(0xFFEF4444),
                                    Color(0xFFF59E0B).copy(alpha = 0.72f),
                                    Color(0xFF22D3EE).copy(alpha = 0.35f),
                                    Color.Transparent
                                ),
                                center = Offset(cx, cy),
                                radius = 72.dp.toPx()
                            ),
                            radius = 72.dp.toPx(),
                            center = Offset(cx, cy)
                        )
                    }
                }

                // Tappable hotspot indicators in Robolectric preview
                hotspots.forEachIndexed { idx, hotspot ->
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(
                                start = ((idx % 3) * 32).dp,
                                top = ((idx / 3) * 32).dp
                            )
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(0x4422D3EE))
                            .border(1.5.dp, NeonCyan, CircleShape)
                            .clickable { selectedHotspot = hotspot }
                            .testTag("heatmap_hotspot_marker_${hotspot.id}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Whatshot,
                            contentDescription = hotspot.placeName,
                            tint = NeonAmber,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        // 2. Floating Top Overlay Pills (NOT a TopAppBar — Top Bar is completely hidden on launch)
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Sleek First-Strike Telemetry Pill (tapping opens first hotspot if available)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(glassBg)
                    .border(1.dp, glassBorder, RoundedCornerShape(50))
                    .clickable(enabled = hotspots.isNotEmpty()) {
                        selectedHotspot = hotspots.firstOrNull()
                    }
                    .padding(horizontal = 14.dp, vertical = 9.dp)
                    .testTag("heatmap_stats_pill"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Whatshot,
                    contentDescription = null,
                    tint = NeonAmber,
                    modifier = Modifier.size(18.dp)
                )
                Column {
                    Text(
                        text = when {
                            isAr -> "خريطة الضربة الأولى"
                            isFr -> "Carte Premier Strike"
                            else -> "First-Strike Heatmap"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = PlusJakartaSans,
                        color = textPrimary,
                        maxLines = 1
                    )
                    Text(
                        text = when {
                            isAr -> "$geoCount موقع مسجل • ${hotspots.size} بؤرة"
                            isFr -> "$geoCount lieux • ${hotspots.size} foyers"
                            else -> "$geoCount pinned • ${hotspots.size} hotspot(s)"
                        },
                        fontSize = 10.sp,
                        color = accentCyan,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }

            // Right: Floating Layers Switcher Trigger
            IconButton(
                onClick = { showLayersMenu = !showLayersMenu },
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(glassBg)
                    .border(1.dp, glassBorder, CircleShape)
                    .testTag("heatmap_layers_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Layers,
                    contentDescription = when {
                        isAr -> "طبقات الخريطة"
                        isFr -> "Couches de carte"
                        else -> "Map Layers"
                    },
                    tint = accentCyan,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // 3. Floating Liquid Glass Layers Switcher Popover (Dark / Light / 3D Buildings / Heatmap vs Pins)
        AnimatedVisibility(
            visible = showLayersMenu,
            enter = fadeIn() + scaleIn(initialScale = 0.9f),
            exit = fadeOut() + scaleOut(targetScale = 0.9f),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(top = 68.dp, end = 14.dp)
        ) {
            Column(
                modifier = Modifier
                    .width(260.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(glassBg)
                    .border(1.1.dp, glassBorder, RoundedCornerShape(22.dp))
                    .padding(14.dp)
                    .testTag("heatmap_layers_panel"),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = when {
                            isAr -> "طبقات ومظهر الخريطة"
                            isFr -> "Style & Couches"
                            else -> "Map Style & Layers"
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = PlusJakartaSans,
                        color = textPrimary
                    )
                    IconButton(
                        onClick = { showLayersMenu = false },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close layers",
                            tint = textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Theme Switcher: Obsidian Dark vs Classic Light
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LayerChipButton(
                        label = when {
                            isAr -> "داكن فاخر"
                            isFr -> "Sombre"
                            else -> "Obsidian Dark"
                        },
                        selected = mapVisualTheme == MapVisualTheme.OBSIDIAN_DARK,
                        accent = accentCyan,
                        isDark = isMapDark,
                        onClick = { mapVisualTheme = MapVisualTheme.OBSIDIAN_DARK },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("heatmap_theme_dark_btn")
                    )
                    LayerChipButton(
                        label = when {
                            isAr -> "نهاري كلاسيك"
                            isFr -> "Clair"
                            else -> "Classic Light"
                        },
                        selected = mapVisualTheme == MapVisualTheme.CLASSIC_LIGHT,
                        accent = NeonAmber,
                        isDark = isMapDark,
                        onClick = { mapVisualTheme = MapVisualTheme.CLASSIC_LIGHT },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("heatmap_theme_light_btn")
                    )
                }

                // 3D Extruded Buildings Toggle (minZoom = 15f)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isMapDark) Color(0x1E1E293B) else Color(0xFFF1F5F9))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ViewInAr,
                            contentDescription = null,
                            tint = accentCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = when {
                                isAr -> "مباني ثلاثية الأبعاد"
                                isFr -> "Bâtiments 3D"
                                else -> "3D Buildings"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                    }
                    Switch(
                        checked = show3dBuildings,
                        onCheckedChange = { show3dBuildings = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = accentCyan,
                            checkedTrackColor = accentCyan.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.testTag("heatmap_3d_buildings_toggle")
                    )
                }

                // Display Mode: Hybrid / Heatmap Glow / Cluster Pins
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = when {
                            isAr -> "نمط العرض الحراري"
                            isFr -> "Mode d'affichage"
                            else -> "Visualization Mode"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = textSecondary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        LayerChipButton(
                            label = if (isAr) "مختلط" else if (isFr) "Hybride" else "Hybrid",
                            selected = displayMode == HeatmapDisplayMode.HYBRID,
                            accent = accentCyan,
                            isDark = isMapDark,
                            onClick = { displayMode = HeatmapDisplayMode.HYBRID },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("heatmap_mode_hybrid_btn")
                        )
                        LayerChipButton(
                            label = if (isAr) "حراري" else if (isFr) "Chaleur" else "Heat",
                            selected = displayMode == HeatmapDisplayMode.HEATMAP_GLOW,
                            accent = NeonAmber,
                            isDark = isMapDark,
                            onClick = { displayMode = HeatmapDisplayMode.HEATMAP_GLOW },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("heatmap_mode_heat_btn")
                        )
                        LayerChipButton(
                            label = if (isAr) "دبابيس" else if (isFr) "Points" else "Pins",
                            selected = displayMode == HeatmapDisplayMode.CLUSTER_PINS,
                            accent = NeonEmerald,
                            isDark = isMapDark,
                            onClick = { displayMode = HeatmapDisplayMode.CLUSTER_PINS },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("heatmap_mode_pins_btn")
                        )
                    }
                }
            }
        }

        // 4. Bottom Dual-Action Liquid Glass Bar + Fade-in Black Scrim ("Know My Location" + "Enter")
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            Color(0x9902050B),
                            Color(0xF5020408)
                        )
                    )
                )
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .testTag("heatmap_bottom_bar")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .background(
                        Brush.verticalGradient(
                            if (isMapDark) {
                                listOf(Color(0xD80E1729), Color(0xEC060B15))
                            } else {
                                listOf(Color(0xEBFFFFFF), Color(0xDCF1F5F9))
                            }
                        )
                    )
                    .border(
                        1.dp,
                        Brush.verticalGradient(
                            listOf(
                                Color(0x55FFFFFF),
                                accentCyan.copy(alpha = 0.42f)
                            )
                        ),
                        RoundedCornerShape(26.dp)
                    )
                    .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Know My Location Button
                OutlinedButton(
                    onClick = {
                        if (hasLocationPermission(context)) {
                            requestLocationAndAnimate()
                        } else {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_COARSE_LOCATION,
                                    Manifest.permission.ACCESS_FINE_LOCATION
                                )
                            )
                        }
                    },
                    shape = RoundedCornerShape(18.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.1.dp,
                        accentCyan.copy(alpha = 0.65f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .minimumInteractiveComponentSize()
                        .testTag("heatmap_know_location_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = null,
                        tint = accentCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when {
                            isAr -> "موقعي الآن"
                            isFr -> "Ma position"
                            else -> "Know My Location"
                        },
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = PlusJakartaSans,
                        color = textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Enter Main App Button
                Button(
                    onClick = onEnterApp,
                    shape = RoundedCornerShape(18.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentCyan,
                        contentColor = Color(0xFF021622)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .minimumInteractiveComponentSize()
                        .testTag("heatmap_enter_btn")
                ) {
                    Text(
                        text = when {
                            isAr -> "دخول"
                            isFr -> "Entrer"
                            else -> "Enter"
                        },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = PlusJakartaSans,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }

    // 5. Interactive Hotspot Inspection Liquid Glass Bottom Sheet
    val inspected = selectedHotspot
    if (inspected != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { selectedHotspot = null },
            sheetState = sheetState,
            containerColor = if (isMapDark) Color(0xF50B1324) else Color(0xFAFFFFFF),
            contentColor = textPrimary,
            shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
            modifier = Modifier.testTag("heatmap_hotspot_sheet")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(NeonAmber.copy(alpha = 0.18f))
                                .border(1.dp, NeonAmber.copy(alpha = 0.65f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = NeonAmber,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = inspected.placeName,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = PlusJakartaSans,
                                color = textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = when {
                                    isAr -> "${inspected.individuals.size} شخص مسجل في هذا الموقع"
                                    isFr -> "${inspected.individuals.size} personne(s) à cet endroit"
                                    else -> "${inspected.individuals.size} person(s) with first strike here"
                                },
                                fontSize = 11.5.sp,
                                color = accentCyan,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 18.dp)
                ) {
                    items(inspected.individuals, key = { it.id }) { person ->
                        HotspotPersonCard(
                            person = person,
                            isDark = isMapDark,
                            accentCyan = accentCyan,
                            isAr = isAr,
                            onClick = {
                                selectedHotspot = null
                                onNavigateToPersonProfile(person.id)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HotspotPersonCard(
    person: Individual,
    isDark: Boolean,
    accentCyan: Color,
    isAr: Boolean,
    onClick: () -> Unit
) {
    val cardBg = if (isDark) Color(0xFF131F36) else Color(0xFFF1F5F9)
    val textPrimary = if (isDark) Color(0xFFF8FAFC) else DayTextPrimary
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(cardBg)
            .border(1.dp, accentCyan.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag("hotspot_person_item_${person.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(accentCyan.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = accentCyan,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = person.fullName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = PlusJakartaSans,
                        color = textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    GlassTypeBadge(type = person.type, darkTheme = isDark)
                }
                Text(
                    text = if (isAr) {
                        "التعزيز ${person.boost} • الضربات ${person.strikeCount}"
                    } else {
                        "BOOST ${person.boost} • Strikes ${person.strikeCount}"
                    },
                    fontSize = 11.sp,
                    color = textSecondary
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Bolt,
                contentDescription = null,
                tint = NeonAmber,
                modifier = Modifier.size(15.dp)
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = if (isAr) "فتح الملف" else "Open Profile",
                tint = accentCyan,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun LayerChipButton(
    label: String,
    selected: Boolean,
    accent: Color,
    isDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = when {
        selected -> accent.copy(alpha = if (isDark) 0.24f else 0.18f)
        isDark -> Color(0x1E1E293B)
        else -> Color(0xFFF1F5F9)
    }
    val border = if (selected) accent else if (isDark) Color(0x33FFFFFF) else Color(0xFFCBD5E1)
    val textColor = when {
        selected -> accent
        isDark -> Color(0xFFE2E8F0)
        else -> Color(0xFF334155)
    }

    Box(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
