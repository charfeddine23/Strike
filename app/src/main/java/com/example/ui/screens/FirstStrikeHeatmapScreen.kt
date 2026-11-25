package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
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
import com.example.data.model.StrikeEvent
import com.example.ui.components.ElevatedSwipeUpBeaconPill
import com.example.ui.components.GlassTypeBadge
import com.example.ui.components.HeatmapDualActionBar
import com.example.ui.components.HeatmapSummaryAndToggleBar
import com.example.ui.components.hasLocationPermission
import com.example.ui.components.resolveBestLocation
import com.example.ui.map.FirstStrikeHotspot
import com.example.ui.map.HeatmapDisplayMode
import com.example.ui.map.HottestRegionTarget
import com.example.ui.map.MapStyleHelper
import com.example.ui.map.MapVisualTheme
import com.example.ui.map.MappedStrikePoint
import com.example.ui.map.isRobolectricRuntime
import com.example.ui.theme.DayCyan
import com.example.ui.theme.DayTextPrimary
import com.example.ui.theme.LocalAppDarkTheme
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.PlusJakartaSans
import com.example.ui.viewmodel.IndividualViewModel
import kotlin.math.abs
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style

@Stable
class HeatmapMapController(context: Context) {
    private val appContext: Context = context.applicationContext

    var mapVisualTheme by mutableStateOf(MapVisualTheme.OBSIDIAN_DARK)
    var show3dBuildings by mutableStateOf(true)
    var displayMode by mutableStateOf(HeatmapDisplayMode.HYBRID)
    var userLat by mutableStateOf<Double?>(null)
    var userLng by mutableStateOf<Double?>(null)
    var focusedStrikePerson by mutableStateOf<Individual?>(null)
    var focusedStrikePoint by mutableStateOf<MappedStrikePoint?>(null)

    var mapLibreMap by mutableStateOf<MapLibreMap?>(null)
    var loadedStyle by mutableStateOf<Style?>(null)
    var hasAutoFramed by mutableStateOf(false)

    var latestIndividuals: List<Individual> = emptyList()
    var latestStrikeEvents: List<StrikeEvent> = emptyList()
    var latestHotspots: List<FirstStrikeHotspot> = emptyList()
    var latestHottestRegion: HottestRegionTarget? = null
    var onHotspotTapped: ((FirstStrikeHotspot) -> Unit)? = null

    private var cachedMapView: MapView? = null
    private var isStarted = false
    private var isResumed = false

    fun getOrCreateMapView(): MapView {
        cachedMapView?.let { existing ->
            (existing.parent as? ViewGroup)?.removeView(existing)
            existing.getMapAsync { map ->
                mapLibreMap = map
                map.getStyle { style ->
                    loadedStyle = style
                    MapStyleHelper.applyGoogleGradeStyling(
                        style = style,
                        theme = mapVisualTheme,
                        show3dBuildings = show3dBuildings,
                        displayMode = displayMode,
                        individuals = latestIndividuals,
                        userLat = userLat,
                        userLng = userLng,
                        strikeEvents = latestStrikeEvents
                    )
                }
            }
            return existing
        }

        val created = MapStyleHelper.createHighPrecisionMapView(appContext).apply {
            onCreate(null)
            getMapAsync { map ->
                mapLibreMap = map
                MapStyleHelper.configureHighPrecisionMap(map, appContext)
                val initialHot = latestHottestRegion
                val startLatLng = if (initialHot != null) {
                    hasAutoFramed = true
                    LatLng(initialHot.lat, initialHot.lng)
                } else {
                    MapStyleHelper.DEFAULT_CENTER
                }
                val startZoom = initialHot?.recommendedZoom ?: MapStyleHelper.DEFAULT_ZOOM
                map.cameraPosition = CameraPosition.Builder()
                    .target(startLatLng)
                    .zoom(startZoom)
                    .build()

                val applyCurrentStyle: (Style) -> Unit = { style ->
                    loadedStyle = style
                    MapStyleHelper.applyGoogleGradeStyling(
                        style = style,
                        theme = mapVisualTheme,
                        show3dBuildings = show3dBuildings,
                        displayMode = displayMode,
                        individuals = latestIndividuals,
                        userLat = userLat,
                        userLng = userLng,
                        strikeEvents = latestStrikeEvents
                    )
                }

                this@apply.addOnDidFinishLoadingStyleListener {
                    map.getStyle { style ->
                        applyCurrentStyle(style)
                    }
                }

                map.setStyle(Style.Builder().fromUri(MapStyleHelper.OPEN_FREE_MAP_LIBERTY_STYLE_URI)) { style ->
                    applyCurrentStyle(style)
                }

                map.addOnMapClickListener { point ->
                    val currentHotspots = latestHotspots
                    val screenPt = map.projection.toScreenLocation(point)
                    val rendered = map.queryRenderedFeatures(
                        screenPt,
                        MapStyleHelper.FIRST_STRIKE_CORE_SPARK_LAYER_ID,
                        MapStyleHelper.CIRCLE_LAYER_ID,
                        MapStyleHelper.FIRST_STRIKE_AURA_LAYER_ID,
                        MapStyleHelper.ROUTINE_CIRCLE_LAYER_ID,
                        MapStyleHelper.ROUTINE_INNER_HALO_LAYER_ID,
                        MapStyleHelper.ROUTINE_HALO_LAYER_ID,
                        MapStyleHelper.ROUTINE_HEAT_RED_LAYER_ID,
                        MapStyleHelper.ROUTINE_HEAT_YELLOW_LAYER_ID,
                        MapStyleHelper.ROUTINE_HEAT_GREEN_LAYER_ID,
                        MapStyleHelper.ROUTINE_HEATMAP_LAYER_ID
                    )
                    val clickedId = rendered.firstOrNull()?.getNumberProperty("id")?.toLong()
                    val hitHotspot = if (clickedId != null) {
                        MapStyleHelper.findNearestHotspot(
                            tapLat = point.latitude,
                            tapLng = point.longitude,
                            hotspots = currentHotspots.filter { hs ->
                                hs.individuals.any { it.id == clickedId } ||
                                    hs.points.any { it.individualId == clickedId }
                            }.ifEmpty { currentHotspots },
                            zoomLevel = map.cameraPosition.zoom
                        ) ?: currentHotspots.firstOrNull { hs -> hs.individuals.any { it.id == clickedId } }
                    } else {
                        MapStyleHelper.findNearestHotspot(
                            tapLat = point.latitude,
                            tapLng = point.longitude,
                            hotspots = currentHotspots,
                            zoomLevel = map.cameraPosition.zoom
                        )
                    }
                    if (hitHotspot != null) {
                        onHotspotTapped?.invoke(hitHotspot)
                        true
                    } else {
                        false
                    }
                }
            }
        }
        cachedMapView = created
        return created
    }

    fun onStart() {
        val view = cachedMapView ?: return
        if (!isStarted) {
            isStarted = true
            view.onStart()
        }
    }

    fun onResume() {
        val view = cachedMapView ?: return
        onStart()
        if (!isResumed) {
            isResumed = true
            view.onResume()
        }
        mapLibreMap?.getStyle { style ->
            loadedStyle = style
            MapStyleHelper.applyGoogleGradeStyling(
                style = style,
                theme = mapVisualTheme,
                show3dBuildings = show3dBuildings,
                displayMode = displayMode,
                individuals = latestIndividuals,
                userLat = userLat,
                userLng = userLng,
                strikeEvents = latestStrikeEvents
            )
        }
    }

    fun onPause() {
        val view = cachedMapView ?: return
        if (isResumed) {
            isResumed = false
            view.onPause()
        }
    }

    fun onStop() {
        val view = cachedMapView ?: return
        onPause()
        if (isStarted) {
            isStarted = false
            view.onStop()
        }
    }

    fun destroy() {
        val view = cachedMapView ?: return
        onStop()
        view.onDestroy()
        cachedMapView = null
        mapLibreMap = null
        loadedStyle = null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FirstStrikeHeatmapScreen(
    viewModel: IndividualViewModel,
    currentLang: String,
    isAppDarkTheme: Boolean = LocalAppDarkTheme.current,
    onEnterApp: () -> Unit,
    onNavigateToPersonProfile: (Long) -> Unit,
    onCurtainDragDelta: ((Float) -> Unit)? = null,
    onCurtainDragEnd: ((Float) -> Unit)? = null,
    mapController: HeatmapMapController? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val isAr = currentLang == "ar"
    val isFr = currentLang == "fr"

    val activeController = mapController ?: remember { HeatmapMapController(context) }
    val ownsController = mapController == null

    val allIndividuals by viewModel.allIndividuals.collectAsStateWithLifecycle()
    val allStrikeEvents by viewModel.allStrikeEvents.collectAsStateWithLifecycle()
    val hotspots = remember(allIndividuals, allStrikeEvents) {
        MapStyleHelper.groupHotspots(allIndividuals, allStrikeEvents)
    }
    val hottestRegion = remember(allIndividuals, allStrikeEvents) {
        MapStyleHelper.findHottestRegion(allIndividuals, allStrikeEvents)
    }
    val allMappedPoints = remember(allIndividuals, allStrikeEvents) {
        MapStyleHelper.collectAllMappedPoints(allIndividuals, allStrikeEvents)
            .sortedWith(
                compareByDescending<MappedStrikePoint> { it.isFirstStrike }
                    .thenByDescending { it.weight }
                    .thenByDescending { it.boost }
            )
    }
    val geoCount = allMappedPoints.size
    val registeredPeopleCount = remember(allMappedPoints) {
        allMappedPoints.map { it.individualId }.distinct().size
    }

    activeController.latestIndividuals = allIndividuals
    activeController.latestStrikeEvents = allStrikeEvents
    activeController.latestHotspots = hotspots
    activeController.latestHottestRegion = hottestRegion

    val mapVisualTheme = activeController.mapVisualTheme
    val show3dBuildings = activeController.show3dBuildings
    val displayMode = activeController.displayMode
    val userLat = activeController.userLat
    val userLng = activeController.userLng
    val focusedStrikePerson = activeController.focusedStrikePerson
    val focusedStrikePoint = activeController.focusedStrikePoint

    var showLayersMenu by remember { mutableStateOf(false) }
    var showAllStrikesMenu by remember { mutableStateOf(false) }
    var strikeSearchQuery by remember { mutableStateOf("") }

    val filteredMappedPoints = remember(allMappedPoints, strikeSearchQuery) {
        val q = strikeSearchQuery.trim()
        if (q.isEmpty()) {
            allMappedPoints
        } else {
            allMappedPoints.filter { pt ->
                pt.individualName.contains(q, ignoreCase = true) ||
                    pt.placeName.contains(q, ignoreCase = true) ||
                    pt.individualType.contains(q, ignoreCase = true)
            }
        }
    }

    var selectedHotspot by remember { mutableStateOf<FirstStrikeHotspot?>(null) }

    val mapLibreRef = activeController.mapLibreMap
    val loadedStyleRef = activeController.loadedStyle

    val requestLocationAndAnimate = {
        if (hasLocationPermission(context)) {
            resolveBestLocation(context) { lat, lng ->
                activeController.userLat = lat
                activeController.userLng = lng
                activeController.mapLibreMap?.let { map ->
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

    val triggerKnowMyLocation: () -> Unit = {
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
    }

    // Keep MapLibre style synced with layer toggles & Room geospatial data (including on every re-mount)
    LaunchedEffect(
        mapLibreRef,
        loadedStyleRef,
        mapVisualTheme,
        show3dBuildings,
        displayMode,
        allIndividuals,
        allStrikeEvents,
        userLat,
        userLng
    ) {
        val map = mapLibreRef
        if (map != null) {
            map.getStyle { style ->
                activeController.loadedStyle = style
                MapStyleHelper.applyGoogleGradeStyling(
                    style = style,
                    theme = mapVisualTheme,
                    show3dBuildings = show3dBuildings,
                    displayMode = displayMode,
                    individuals = allIndividuals,
                    userLat = userLat,
                    userLng = userLng,
                    strikeEvents = allStrikeEvents
                )
            }
        } else if (loadedStyleRef != null) {
            MapStyleHelper.applyGoogleGradeStyling(
                style = loadedStyleRef,
                theme = mapVisualTheme,
                show3dBuildings = show3dBuildings,
                displayMode = displayMode,
                individuals = allIndividuals,
                userLat = userLat,
                userLng = userLng,
                strikeEvents = allStrikeEvents
            )
        }
    }

    // Center the camera on the hottest region once on initial launch (preserved across page switches)
    LaunchedEffect(mapLibreRef, hottestRegion) {
        val map = mapLibreRef ?: return@LaunchedEffect
        val target = hottestRegion ?: return@LaunchedEffect
        if (!activeController.hasAutoFramed) {
            activeController.hasAutoFramed = true
            MapStyleHelper.animateCameraTo(
                mapLibreMap = map,
                lat = target.lat,
                lng = target.lng,
                zoom = target.recommendedZoom,
                durationMs = 800
            )
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
            val mapView = remember(activeController) { activeController.getOrCreateMapView() }

            DisposableEffect(lifecycleOwner, activeController, mapView) {
                activeController.onHotspotTapped = { hotspot -> selectedHotspot = hotspot }
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_START -> activeController.onStart()
                        Lifecycle.Event.ON_RESUME -> activeController.onResume()
                        Lifecycle.Event.ON_PAUSE -> activeController.onPause()
                        Lifecycle.Event.ON_STOP -> activeController.onStop()
                        else -> Unit
                    }
                }
                if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                    activeController.onStart()
                }
                if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                    activeController.onResume()
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                    activeController.onHotspotTapped = null
                    if (ownsController) {
                        activeController.destroy()
                    } else {
                        // Keep GL context, loaded OpenFreeMap style, camera & hotspots intact when entering pages
                        activeController.onPause()
                    }
                }
            }

            AndroidView(
                factory = {
                    (mapView.parent as? ViewGroup)?.removeView(mapView)
                    mapView
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // High-craft dark/light tactical canvas preview for Robolectric JVM environment
            val routinePulseTransition = rememberInfiniteTransition(label = "routine_halo_pulse")
            val haloPulse by routinePulseTransition.animateFloat(
                initialValue = 0.86f,
                targetValue = 1.14f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1900, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "halo_pulse_scale"
            )

            Box(modifier = Modifier.fillMaxSize()) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(if (isMapDark) Color(0xFF0F172A) else Color(0xFFF8F9FA))
                    // Simulated water & grid contours
                    drawRect(
                        color = if (isMapDark) Color(0xFF0E1626) else Color(0xFFAADAFF),
                        topLeft = Offset(0f, 0f),
                        size = androidx.compose.ui.geometry.Size(size.width, size.height * 0.25f)
                    )
                    // Draw image-matched translucent Hot Zones (Blue -> Cyan -> Lime Green -> thin Yellow/Orange -> broad Coral-Red plateau)
                    // and First Strikes strictly as a tight luxurious Obsidian-Gold point glow (no First-Strike heatmap blob)
                    hotspots.forEachIndexed { idx, hotspot ->
                        val cx = size.width * (0.28f + ((idx * 0.21f) % 0.48f))
                        val cy = size.height * (0.38f + ((idx * 0.14f) % 0.28f))
                        if (hotspot.hasRoutineStrikes && displayMode != HeatmapDisplayMode.CLUSTER_PINS) {
                            val outerRadius = 78.dp.toPx() * haloPulse
                            drawCircle(
                                brush = Brush.radialGradient(
                                    0.00f to Color(0xFFEF3E38).copy(alpha = 0.78f),
                                    0.42f to Color(0xFFF43A34).copy(alpha = 0.76f),
                                    0.50f to Color(0xFFFF8A1E).copy(alpha = 0.76f),
                                    0.56f to Color(0xFFFBE922).copy(alpha = 0.74f),
                                    0.68f to Color(0xFF42F252).copy(alpha = 0.66f),
                                    0.82f to Color(0xFF38DEF0).copy(alpha = 0.48f),
                                    0.92f to Color(0xFF4080FF).copy(alpha = 0.26f),
                                    1.00f to Color.Transparent,
                                    center = Offset(cx, cy),
                                    radius = outerRadius
                                ),
                                radius = outerRadius,
                                center = Offset(cx, cy)
                            )
                        }
                        if (hotspot.hasFirstStrikes) {
                            // Subtle, tight ambient gold shimmer around the luxurious First Strike point only
                            val pointAuraRadius = 18.dp.toPx()
                            drawCircle(
                                brush = Brush.radialGradient(
                                    0.00f to Color(0xFFF59E0B).copy(alpha = 0.34f),
                                    0.65f to Color(0xFFF59E0B).copy(alpha = 0.12f),
                                    1.00f to Color.Transparent,
                                    center = Offset(cx, cy),
                                    radius = pointAuraRadius
                                ),
                                radius = pointAuraRadius,
                                center = Offset(cx, cy)
                            )
                        }
                    }
                }

                // Tappable hotspot indicators in Robolectric preview
                hotspots.forEachIndexed { idx, hotspot ->
                    val isRoutineOnly = hotspot.hasRoutineStrikes && !hotspot.hasFirstStrikes
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(
                                start = ((idx % 3) * 32).dp,
                                top = ((idx / 3) * 32).dp
                            )
                            .size(48.dp)
                            .clip(CircleShape)
                            .semantics { contentDescription = hotspot.placeName }
                            .clickable { selectedHotspot = hotspot }
                            .testTag("heatmap_hotspot_marker_${hotspot.id}"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!isRoutineOnly) {
                            // Luxurious multi-layered Obsidian-Gold jewel dot for First Strike
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF0B1221))
                                    .border(2.4.dp, Color(0xFFFBBF24), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFFFBEB))
                                )
                            }
                        } else {
                            // Routine pin dot inside thermal cloud
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEF4444))
                                    .border(1.8.dp, Color.White, CircleShape)
                            )
                        }
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
            // Left: Interactive First-Strike Telemetry Pill (tapping expands full list of all mapped strikes)
            Row(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .padding(end = 10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(glassBg)
                    .border(
                        width = if (showAllStrikesMenu) 1.3.dp else 1.dp,
                        color = if (showAllStrikesMenu) accentCyan else glassBorder,
                        shape = RoundedCornerShape(50)
                    )
                    .clickable {
                        showAllStrikesMenu = !showAllStrikesMenu
                        if (showAllStrikesMenu) showLayersMenu = false
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
                Column(modifier = Modifier.weight(1f, fill = false)) {
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
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val focusedPt = focusedStrikePoint
                    val focused = focusedStrikePerson
                    Text(
                        text = when {
                            focusedPt != null -> {
                                val place = focusedPt.placeName.ifBlank {
                                    "${String.format(java.util.Locale.US, "%.2f", focusedPt.lat)}, ${String.format(java.util.Locale.US, "%.2f", focusedPt.lng)}"
                                }
                                "${focusedPt.individualName} • $place"
                            }
                            focused != null -> {
                                val place = focused.firstStrikePlaceName.ifBlank {
                                    "${String.format(java.util.Locale.US, "%.2f", focused.firstStrikeLat ?: 0.0)}, ${String.format(java.util.Locale.US, "%.2f", focused.firstStrikeLng ?: 0.0)}"
                                }
                                "${focused.fullName} • $place"
                            }
                            hottestRegion != null -> when {
                                isAr -> "الأكثر سخونة: ${hottestRegion.placeName}"
                                isFr -> "Zone max: ${hottestRegion.placeName}"
                                else -> "Hottest: ${hottestRegion.placeName}"
                            }
                            else -> when {
                                isAr -> "$geoCount موقع مسجل • ${hotspots.size} بؤرة"
                                isFr -> "$geoCount lieux • ${hotspots.size} foyers"
                                else -> "$geoCount pinned • ${hotspots.size} hotspot(s)"
                            }
                        },
                        fontSize = 10.sp,
                        color = accentCyan,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    imageVector = if (showAllStrikesMenu) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = accentCyan,
                    modifier = Modifier.size(17.dp)
                )
            }

            // Right: Floating Layers Switcher Trigger
            IconButton(
                onClick = {
                    showLayersMenu = !showLayersMenu
                    if (showLayersMenu) showAllStrikesMenu = false
                },
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

        // 2b. Floating Liquid Glass Dropdown List of ALL Strikes on the Map (tapping any name flies camera to that strike)
        AnimatedVisibility(
            visible = showAllStrikesMenu,
            enter = fadeIn(tween(180)) + expandVertically(expandFrom = Alignment.Top) + scaleIn(initialScale = 0.94f),
            exit = fadeOut(tween(140)) + shrinkVertically(shrinkTowards = Alignment.Top) + scaleOut(targetScale = 0.94f),
            modifier = Modifier
                .align(Alignment.TopStart)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(top = 68.dp, start = 14.dp, end = 14.dp)
        ) {
            Column(
                modifier = Modifier
                    .width(305.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(glassBg)
                    .border(1.1.dp, glassBorder, RoundedCornerShape(22.dp))
                    .padding(12.dp)
                    .testTag("heatmap_all_strikes_panel"),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Whatshot,
                            contentDescription = null,
                            tint = NeonAmber,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = when {
                                isAr -> "كل الضربات على الخريطة (${allMappedPoints.size})"
                                isFr -> "Tous les strikes (${allMappedPoints.size})"
                                else -> "All Map Strikes (${allMappedPoints.size})"
                            },
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = PlusJakartaSans,
                            color = textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(
                        onClick = { showAllStrikesMenu = false },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close strikes list",
                            tint = textSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                if (allMappedPoints.isNotEmpty()) {
                    // Compact luxury search bar to jump to any name cleanly
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isMapDark) Color(0x281E293B) else Color(0xFFF1F5F9))
                            .border(
                                0.8.dp,
                                if (isMapDark) Color(0x3338BDF8) else Color(0xFFCBD5E1),
                                RoundedCornerShape(14.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = accentCyan,
                            modifier = Modifier.size(15.dp)
                        )
                        Box(modifier = Modifier.weight(1f)) {
                            if (strikeSearchQuery.isEmpty()) {
                                Text(
                                    text = when {
                                        isAr -> "ابحث بالاسم أو المنطقة..."
                                        isFr -> "Rechercher nom ou lieu..."
                                        else -> "Search name or place..."
                                    },
                                    fontSize = 11.sp,
                                    color = textSecondary,
                                    maxLines = 1
                                )
                            }
                            BasicTextField(
                                value = strikeSearchQuery,
                                onValueChange = { strikeSearchQuery = it },
                                singleLine = true,
                                textStyle = TextStyle(
                                    color = textPrimary,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                cursorBrush = SolidColor(accentCyan),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("heatmap_strikes_search_input")
                            )
                        }
                        if (strikeSearchQuery.isNotEmpty()) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search",
                                tint = textSecondary,
                                modifier = Modifier
                                    .size(14.dp)
                                    .clickable { strikeSearchQuery = "" }
                            )
                        }
                    }
                }

                if (filteredMappedPoints.isEmpty()) {
                    Text(
                        text = when {
                            allMappedPoints.isEmpty() -> when {
                                isAr -> "لا توجد ضربات بموقع مسجل على الخريطة بعد"
                                isFr -> "Aucun strike localisé sur la carte"
                                else -> "No strikes pinned on the map yet"
                            }
                            else -> when {
                                isAr -> "لا توجد نتائج مطابقة"
                                isFr -> "Aucun résultat correspondant"
                                else -> "No matching strikes"
                            }
                        },
                        fontSize = 11.5.sp,
                        color = textSecondary,
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp)
                    )
                } else {
                    val primaryKeysByPersonId = remember(filteredMappedPoints) {
                        val map = mutableMapOf<Long, String>()
                        for (pt in filteredMappedPoints) {
                            if (!map.containsKey(pt.individualId)) {
                                map[pt.individualId] = pt.key
                            }
                        }
                        map
                    }
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 260.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filteredMappedPoints, key = { it.key }) { pt ->
                            val isFocused = focusedStrikePoint?.key == pt.key ||
                                (focusedStrikePoint == null && focusedStrikePerson?.id == pt.individualId)
                            val catAccent = if (pt.isFirstStrike) NeonAmber else Color(0xFF22C55E)
                            val itemBg = when {
                                isFocused -> accentCyan.copy(alpha = if (isMapDark) 0.18f else 0.14f)
                                isMapDark -> Color(0xFF131F36)
                                else -> Color(0xFFF1F5F9)
                            }
                            val itemBorder = if (isFocused) accentCyan else catAccent.copy(alpha = 0.34f)
                            val placeLabel = pt.placeName.ifBlank {
                                String.format(java.util.Locale.US, "%.4f, %.4f", pt.lat, pt.lng)
                            }
                            val itemTag = if (primaryKeysByPersonId[pt.individualId] == pt.key) {
                                "heatmap_strike_list_item_${pt.individualId}"
                            } else {
                                "heatmap_strike_list_item_${pt.key}"
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(itemBg)
                                    .border(1.dp, itemBorder, RoundedCornerShape(14.dp))
                                    .clickable {
                                        activeController.hasAutoFramed = true
                                        activeController.focusedStrikePoint = pt
                                        activeController.focusedStrikePerson = allIndividuals.firstOrNull { it.id == pt.individualId }
                                        showAllStrikesMenu = false
                                        mapLibreRef?.let { map ->
                                            MapStyleHelper.animateCameraTo(
                                                mapLibreMap = map,
                                                lat = pt.lat,
                                                lng = pt.lng,
                                                zoom = 16.0,
                                                durationMs = 850
                                            )
                                        }
                                    }
                                    .minimumInteractiveComponentSize()
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                                    .testTag(itemTag),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(CircleShape)
                                            .background(catAccent.copy(alpha = 0.18f))
                                            .border(
                                                width = if (pt.isFirstStrike) 0.8.dp else 1.2.dp,
                                                color = catAccent.copy(alpha = 0.7f),
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (pt.isFirstStrike) Icons.Default.LocationOn else Icons.Default.Bolt,
                                            contentDescription = null,
                                            tint = catAccent,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            Text(
                                                text = pt.individualName,
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = PlusJakartaSans,
                                                color = textPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            GlassTypeBadge(type = pt.individualType, darkTheme = isMapDark)
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(catAccent.copy(alpha = 0.16f))
                                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                                            ) {
                                                Text(
                                                    text = when {
                                                        pt.isFirstStrike && isAr -> "أول"
                                                        pt.isFirstStrike -> "First"
                                                        isAr -> "روتيني"
                                                        else -> "Routine"
                                                    },
                                                    fontSize = 8.5.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = catAccent
                                                )
                                            }
                                        }
                                        Text(
                                            text = if (isAr) {
                                                "$placeLabel • ${pt.strikeCount} ضربة"
                                            } else {
                                                "$placeLabel • ${pt.strikeCount}x"
                                            },
                                            fontSize = 10.5.sp,
                                            color = if (isFocused) accentCyan else textSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.Default.NearMe,
                                    contentDescription = when {
                                        isAr -> "الانتقال إلى الموقع"
                                        isFr -> "Aller à l'emplacement"
                                        else -> "Go to strike location"
                                    },
                                    tint = accentCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Top-Right Floating Stack: Map Style & Layers Popover + Know My Location Button directly below it
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(top = 68.dp, end = 14.dp)
                .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AnimatedVisibility(
                visible = showLayersMenu,
                enter = fadeIn(tween(180)) + expandVertically(expandFrom = Alignment.Top) + scaleIn(initialScale = 0.94f),
                exit = fadeOut(tween(140)) + shrinkVertically(shrinkTowards = Alignment.Top) + scaleOut(targetScale = 0.94f)
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
                            onClick = { activeController.mapVisualTheme = MapVisualTheme.OBSIDIAN_DARK },
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
                            onClick = { activeController.mapVisualTheme = MapVisualTheme.CLASSIC_LIGHT },
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
                            onCheckedChange = { activeController.show3dBuildings = it },
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
                                onClick = { activeController.displayMode = HeatmapDisplayMode.HYBRID },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("heatmap_mode_hybrid_btn")
                            )
                            LayerChipButton(
                                label = if (isAr) "حراري" else if (isFr) "Chaleur" else "Heat",
                                selected = displayMode == HeatmapDisplayMode.HEATMAP_GLOW,
                                accent = NeonAmber,
                                isDark = isMapDark,
                                onClick = { activeController.displayMode = HeatmapDisplayMode.HEATMAP_GLOW },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("heatmap_mode_heat_btn")
                            )
                            LayerChipButton(
                                label = if (isAr) "دبابيس" else if (isFr) "Points" else "Pins",
                                selected = displayMode == HeatmapDisplayMode.CLUSTER_PINS,
                                accent = NeonEmerald,
                                isDark = isMapDark,
                                onClick = { activeController.displayMode = HeatmapDisplayMode.CLUSTER_PINS },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("heatmap_mode_pins_btn")
                            )
                        }
                    }

                    // Compact Halo Color Legend: First Strike (Luxury Obsidian-Gold Point) vs Hot Zones (Translucent Spectral Heatmap)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isMapDark) Color(0x1E1E293B) else Color(0xFFF1F5F9))
                            .padding(horizontal = 10.dp, vertical = 7.dp)
                            .testTag("heatmap_halo_legend"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(13.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF0B1221))
                                    .border(1.8.dp, Color(0xFFFBBF24), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(3.5.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFFFBEB))
                                )
                            }
                            Text(
                                text = "First Strike",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(15.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            0.00f to Color(0xFFEF3E38),
                                            0.44f to Color(0xFFF43A34),
                                            0.56f to Color(0xFFFBE922),
                                            0.72f to Color(0xFF42F252),
                                            0.88f to Color(0xFF38DEF0),
                                            1.00f to Color(0xFF4080FF)
                                        )
                                    )
                            )
                            Text(
                                text = if (isAr) "المناطق الساخنة" else if (isFr) "Zones chaudes" else "Hot Zones",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4ADE80)
                            )
                        }
                    }
                }
            }
        }

        // 4. Lower HUD & Entry Controls over a fade-in black Liquid Glass scrim:
        //    - Elevated lower-center "Swipe up to hide map" pill
        //    - Non-redundant Summary (Total Mapped People) + Instant Visualization Switcher (Heatmap <-> Pins)
        //    - Bottom Dual-Action Bar (Know My Location & Enter)
        var bottomDockCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
        val latestOnDragDelta by rememberUpdatedState(onCurtainDragDelta)
        val latestOnDragEnd by rememberUpdatedState(onCurtainDragEnd)
        val latestOnEnterApp by rememberUpdatedState(onEnterApp)

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .onGloballyPositioned { bottomDockCoords = it }
                .pointerInput(Unit) {
                    val slop = viewConfiguration.touchSlop
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        val rootDown = bottomDockCoords?.localToRoot(down.position) ?: down.position
                        val tracker = VelocityTracker()
                        tracker.addPosition(down.uptimeMillis, rootDown)
                        var draggingUp = false
                        var lastRootY = rootDown.y
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) {
                                if (draggingUp) {
                                    val velY = tracker.calculateVelocity().y
                                    val endCb = latestOnDragEnd
                                    if (endCb != null) {
                                        endCb(velY)
                                    } else {
                                        latestOnEnterApp()
                                    }
                                }
                                break
                            }
                            val rootPos = bottomDockCoords?.localToRoot(change.position) ?: change.position
                            tracker.addPosition(change.uptimeMillis, rootPos)
                            val totalDy = rootPos.y - rootDown.y
                            val totalDx = rootPos.x - rootDown.x
                            if (!draggingUp) {
                                if (totalDy < -slop && abs(totalDy) > abs(totalDx) * 1.05f) {
                                    draggingUp = true
                                    val deltaY = rootPos.y - lastRootY
                                    if (deltaY != 0f) {
                                        latestOnDragDelta?.invoke(deltaY)
                                    }
                                    change.consume()
                                } else if (abs(totalDx) > slop && abs(totalDx) > abs(totalDy)) {
                                    break
                                }
                            } else {
                                val deltaY = rootPos.y - lastRootY
                                if (deltaY != 0f) {
                                    latestOnDragDelta?.invoke(deltaY)
                                }
                                change.consume()
                            }
                            lastRootY = rootPos.y
                        }
                    }
                }
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Transparent,
                            0.24f to Color(0x6602050B),
                            0.62f to Color(0xCC020409),
                            1.0f to Color(0xF5020408)
                        )
                    )
                )
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .testTag("heatmap_lower_hud_dock"),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1) Elevated "Swipe up to hide map" Pill in the lower-center above the HUD stack
                ElevatedSwipeUpBeaconPill(
                    label = when {
                        isAr -> "اسحب لأعلى لإخفاء الخريطة"
                        isFr -> "Glisser vers le haut"
                        else -> "Swipe up to hide map"
                    },
                    accentCyan = accentCyan,
                    textPrimary = textPrimary,
                    isMapDark = isMapDark,
                    onClick = onEnterApp
                )

                // 2) Non-Redundant Summary (Total Mapped) + Instant Visualization Switcher (Heatmap <-> Pins)
                HeatmapSummaryAndToggleBar(
                    registeredPeopleCount = registeredPeopleCount,
                    totalPinnedStrikesCount = geoCount,
                    displayMode = displayMode,
                    accentCyan = accentCyan,
                    isMapDark = isMapDark,
                    currentLang = currentLang,
                    onSelectDisplayMode = { nextMode -> activeController.displayMode = nextMode },
                    onCounterClick = {
                        showAllStrikesMenu = !showAllStrikesMenu
                        if (showAllStrikesMenu) showLayersMenu = false
                    }
                )

                // 3) Bottom Dual-Action Bar: Know My Location & Enter
                HeatmapDualActionBar(
                    accentCyan = accentCyan,
                    isMapDark = isMapDark,
                    currentLang = currentLang,
                    onKnowLocationClick = triggerKnowMyLocation,
                    onEnterClick = onEnterApp
                )
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
                                    isAr -> "${inspected.individuals.size} شخص • ${inspected.firstStrikeCount} أول • ${inspected.routineStrikeCount} روتيني"
                                    isFr -> "${inspected.individuals.size} pers. • ${inspected.firstStrikeCount} 1er • ${inspected.routineStrikeCount} routine"
                                    else -> "${inspected.individuals.size} person(s) • ${inspected.firstStrikeCount} First • ${inspected.routineStrikeCount} Routine"
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
