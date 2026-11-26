package com.example.ui.components

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlin.math.roundToInt
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.map.GeocodePlace
import com.example.data.map.NominatimGeocoder
import com.example.ui.map.HeatmapDisplayMode
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
import com.example.ui.theme.NeonRose
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.PlusJakartaSans
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import java.util.Locale

@Composable
fun FirstStrikeLocationPickerDialog(
    initialLat: Double?,
    initialLng: Double?,
    initialPlaceName: String,
    currentLang: String,
    isDarkTheme: Boolean = LocalAppDarkTheme.current,
    titleOverride: String? = null,
    confirmLabelOverride: String? = null,
    allowCategorySelection: Boolean = false,
    initialIsFirstStrike: Boolean = true,
    personName: String? = null,
    existingPersonLocations: List<MappedStrikePoint> = emptyList(),
    onConfirmWithCategory: ((lat: Double, lng: Double, placeName: String, isFirstStrike: Boolean) -> Unit)? = null,
    onConfirm: (lat: Double, lng: Double, placeName: String) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val isAr = currentLang == "ar"
    val isFr = currentLang == "fr"

    val firstSavedFallback = remember(existingPersonLocations) { existingPersonLocations.firstOrNull() }
    val defaultLat = initialLat?.takeIf { it.isFinite() } ?: firstSavedFallback?.lat ?: 36.7538
    val defaultLng = initialLng?.takeIf { it.isFinite() } ?: firstSavedFallback?.lng ?: 3.0588
    var isFirstStrikeSelected by remember(initialIsFirstStrike) { mutableStateOf(initialIsFirstStrike) }

    var selectedLat by remember { mutableDoubleStateOf(defaultLat) }
    var selectedLng by remember { mutableDoubleStateOf(defaultLng) }
    val latestPersonLocations by rememberUpdatedState(existingPersonLocations)
    var selectedPlaceName by remember {
        mutableStateOf(
            NominatimGeocoder.stripTifinagh(initialPlaceName)
                .ifBlank { String.format(Locale.US, "%.4f, %.4f", defaultLat, defaultLng) }
        )
    }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<GeocodePlace>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var isReverseGeocoding by remember { mutableStateOf(false) }
    var isCameraDragging by remember { mutableStateOf(false) }
    var hasUserMovedCamera by remember { mutableStateOf(false) }
    var hasAutoFittedBounds by remember { mutableStateOf(false) }
    var cameraProjectionTick by remember { mutableIntStateOf(0) }
    var mapFrameSizePx by remember { mutableStateOf(IntSize.Zero) }
    var mapLibreRef by remember { mutableStateOf<MapLibreMap?>(null) }
    var searchJob by remember { mutableStateOf<Job?>(null) }
    var reverseJob by remember { mutableStateOf<Job?>(null) }

    val pinTranslationY by animateDpAsState(
        targetValue = if (isCameraDragging) (-18).dp else 0.dp,
        animationSpec = if (isCameraDragging) {
            tween(durationMillis = 120)
        } else {
            spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium)
        },
        label = "picker_pin_lift"
    )
    val pinScale by animateFloatAsState(
        targetValue = if (isCameraDragging) 1.1f else 1.0f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium),
        label = "picker_pin_scale"
    )
    val shadowAlpha by animateFloatAsState(
        targetValue = if (isCameraDragging) 0.55f else 0.25f,
        animationSpec = tween(durationMillis = 150),
        label = "picker_shadow_alpha"
    )

    val mapTheme = if (isDarkTheme) MapVisualTheme.OBSIDIAN_DARK else MapVisualTheme.CLASSIC_LIGHT

    fun triggerReverseGeocode(lat: Double, lng: Double) {
        reverseJob?.cancel()
        reverseJob = coroutineScope.launch {
            isReverseGeocoding = true
            val resolved = NominatimGeocoder.reverseGeocode(lat, lng, currentLang)
            if (resolved.isNotBlank()) {
                selectedPlaceName = NominatimGeocoder.stripTifinagh(resolved)
            }
            isReverseGeocoding = false
        }
    }

    fun fetchCurrentGps() {
        resolveBestLocation(context) { lat, lng ->
            selectedLat = lat
            selectedLng = lng
            mapLibreRef?.let { map ->
                MapStyleHelper.animateCameraTo(map, lat, lng, zoom = 15.2, durationMs = 800)
            }
            triggerReverseGeocode(lat, lng)
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            fetchCurrentGps()
        }
    }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0

    LaunchedEffect(Unit) {
        focusManager.clearFocus()
        keyboardController?.hide()
    }

    BackHandler { onDismiss() }

    val bgColor = if (isDarkTheme) Color(0xFF060A13) else Color(0xFFF8FAFC)
    val panelBg = if (isDarkTheme) Color(0xEC0B1324) else Color(0xF5FFFFFF)
    val accentCyan = if (isDarkTheme) NeonCyan else DayCyan
    val routineMagenta = Color(0xFFD946EF)
    val activeAccent = if (allowCategorySelection && !isFirstStrikeSelected) routineMagenta else accentCyan
    val borderColor = activeAccent.copy(alpha = 0.48f)
    val textPrimary = if (isDarkTheme) Color(0xFFF8FAFC) else DayTextPrimary
    val textSecondary = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF475569)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xB803060E))
            .clipToBounds()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        val compactMapHeight = if (imeVisible) {
            140.dp
        } else if (existingPersonLocations.isNotEmpty()) {
            minOf(210.dp, (maxHeight * 0.30f).coerceAtLeast(155.dp))
        } else {
            minOf(235.dp, (maxHeight * 0.34f).coerceAtLeast(165.dp))
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 580.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(bgColor)
                .border(1.2.dp, borderColor, RoundedCornerShape(24.dp))
                .padding(12.dp)
                .testTag("first_strike_picker_dialog"),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Top Search Bar & Action Row (strictly inside safe frame)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(panelBg)
                            .border(1.dp, borderColor, CircleShape)
                            .testTag("picker_close_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = if (isAr) "إغلاق" else "Close",
                            tint = accentCyan
                        )
                    }

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { q ->
                            searchQuery = q
                            searchJob?.cancel()
                            if (q.trim().length >= 2) {
                                searchJob = coroutineScope.launch {
                                    delay(350)
                                    isSearching = true
                                    searchResults = NominatimGeocoder.searchPlaces(q, currentLang)
                                    isSearching = false
                                }
                            } else {
                                searchResults = emptyList()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .widthIn(min = 0.dp)
                            .testTag("picker_search_input"),
                        placeholder = {
                            Text(
                                text = when {
                                    isAr -> "ابحث عن مدينة أو حي أو معلم..."
                                    isFr -> "Rechercher une ville ou un lieu..."
                                    else -> "Search city, district, or landmark..."
                                },
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = {
                            if (isSearching) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = accentCyan
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = accentCyan
                                )
                            }
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        searchQuery = ""
                                        searchResults = emptyList()
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = textSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(18.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentCyan,
                            unfocusedBorderColor = borderColor,
                            focusedContainerColor = panelBg,
                            unfocusedContainerColor = panelBg
                        )
                    )

                    IconButton(
                        onClick = {
                            if (hasLocationPermission(context)) {
                                fetchCurrentGps()
                            } else {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_COARSE_LOCATION,
                                        Manifest.permission.ACCESS_FINE_LOCATION
                                    )
                                )
                            }
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(panelBg)
                            .border(1.dp, borderColor, CircleShape)
                            .testTag("picker_gps_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = when {
                                isAr -> "موقعي الحالي"
                                isFr -> "Ma position"
                                else -> "My Location"
                            },
                            tint = NeonEmerald
                        )
                    }
                }

                AnimatedVisibility(visible = searchResults.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = if (imeVisible) 120.dp else 180.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(panelBg)
                            .border(1.dp, borderColor, RoundedCornerShape(18.dp))
                            .testTag("picker_search_results"),
                        contentPadding = PaddingValues(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(searchResults) { place ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        selectedLat = place.lat
                                        selectedLng = place.lng
                                        selectedPlaceName = place.shortLabel.ifBlank { place.displayName }
                                        searchResults = emptyList()
                                        focusManager.clearFocus()
                                        keyboardController?.hide()
                                        mapLibreRef?.let { map ->
                                            MapStyleHelper.animateCameraTo(map, place.lat, place.lng, zoom = 14.5, durationMs = 800)
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = accentCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = place.shortLabel,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = PlusJakartaSans,
                                        color = textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = place.displayName,
                                        fontSize = 11.sp,
                                        color = textSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Sync existing person strike pins whenever data/theme changes on live MapLibre style
            LaunchedEffect(mapLibreRef, existingPersonLocations, mapTheme) {
                val map = mapLibreRef ?: return@LaunchedEffect
                map.getStyle { style ->
                    MapStyleHelper.applyPickerPersonLocations(style, mapTheme, existingPersonLocations)
                    cameraProjectionTick++
                }
                if (!hasAutoFittedBounds && existingPersonLocations.isNotEmpty()) {
                    hasAutoFittedBounds = true
                    MapStyleHelper.fitCameraToPoints(
                        mapLibreMap = map,
                        points = existingPersonLocations,
                        paddingPx = 86,
                        singlePointZoom = if (initialLat != null && initialLng != null) 13.8 else 13.0,
                        durationMs = 420
                    )
                }
            }

            // 2. Framed Compact Map Viewport (proportionally sized height so dialog stays crisp & uncluttered)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(compactMapHeight)
                    .onSizeChanged { mapFrameSizePx = it }
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.1.dp, borderColor, RoundedCornerShape(20.dp))
                    .clipToBounds()
                    .testTag("picker_map_frame")
            ) {
                if (!isRobolectricRuntime()) {
                    val mapView = remember {
                        MapStyleHelper.createHighPrecisionMapView(context).apply {
                            onCreate(null)
                            getMapAsync { map ->
                                mapLibreRef = map
                                MapStyleHelper.configureHighPrecisionMap(map, context)
                                val hasAnchor = (initialLat != null && initialLng != null) || firstSavedFallback != null
                                val startZoom = when {
                                    latestPersonLocations.size >= 2 -> 11.8
                                    hasAnchor -> 13.4
                                    else -> 5.5
                                }
                                map.cameraPosition = CameraPosition.Builder()
                                    .target(LatLng(defaultLat, defaultLng))
                                    .zoom(startZoom)
                                    .build()

                                val syncPickerStyle: (Style) -> Unit = { style ->
                                    MapStyleHelper.applyGoogleGradeStyling(
                                        style = style,
                                        theme = mapTheme,
                                        show3dBuildings = false,
                                        displayMode = HeatmapDisplayMode.CLUSTER_PINS,
                                        individuals = emptyList()
                                    )
                                    MapStyleHelper.applyPickerPersonLocations(
                                        style = style,
                                        theme = mapTheme,
                                        personLocations = latestPersonLocations
                                    )
                                    cameraProjectionTick++
                                }

                                this@apply.addOnDidFinishLoadingStyleListener {
                                    map.getStyle { style ->
                                        syncPickerStyle(style)
                                    }
                                }

                                map.setStyle(Style.Builder().fromUri(MapStyleHelper.OPEN_FREE_MAP_LIBERTY_STYLE_URI)) { style ->
                                    syncPickerStyle(style)
                                    if (!hasAutoFittedBounds && latestPersonLocations.isNotEmpty()) {
                                        this@apply.post {
                                            hasAutoFittedBounds = true
                                            MapStyleHelper.fitCameraToPoints(
                                                mapLibreMap = map,
                                                points = latestPersonLocations,
                                                paddingPx = 86,
                                                singlePointZoom = 13.4,
                                                durationMs = 450
                                            )
                                        }
                                    }
                                }
                                map.addOnMapClickListener { tapPoint ->
                                    val candidate = MapStyleHelper.findNearestStrikePoint(
                                        tapLat = tapPoint.latitude,
                                        tapLng = tapPoint.longitude,
                                        points = latestPersonLocations,
                                        zoomLevel = map.cameraPosition.zoom
                                    )
                                    if (candidate != null) {
                                        hasUserMovedCamera = true
                                        selectedLat = candidate.lat
                                        selectedLng = candidate.lng
                                        selectedPlaceName = candidate.placeName.ifBlank {
                                            String.format(Locale.US, "%.4f, %.4f", candidate.lat, candidate.lng)
                                        }
                                        MapStyleHelper.animateCameraTo(map, candidate.lat, candidate.lng, zoom = 15.0, durationMs = 550)
                                        true
                                    } else {
                                        false
                                    }
                                }
                                map.addOnCameraMoveStartedListener { reason ->
                                    if (reason == MapLibreMap.OnCameraMoveStartedListener.REASON_API_GESTURE) {
                                        isCameraDragging = true
                                        hasUserMovedCamera = true
                                    }
                                }
                                map.addOnCameraMoveListener {
                                    cameraProjectionTick++
                                }
                                map.addOnCameraIdleListener {
                                    isCameraDragging = false
                                    cameraProjectionTick++
                                    if (hasUserMovedCamera) {
                                        val target = map.cameraPosition.target ?: return@addOnCameraIdleListener
                                        selectedLat = target.latitude
                                        selectedLng = target.longitude
                                        triggerReverseGeocode(target.latitude, target.longitude)
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

                    // Live 60-90 FPS Screen-Projected Strike Dots Overlay (guarantees all person locations appear live on map)
                    val projTick = cameraProjectionTick
                    val liveMap = mapLibreRef
                    if (liveMap != null && existingPersonLocations.isNotEmpty() && mapFrameSizePx.width > 0 && mapFrameSizePx.height > 0 && projTick >= 0) {
                        val density = LocalDensity.current
                        val halfDotPx = with(density) { 14.dp.toPx() }
                        existingPersonLocations.forEach { pt ->
                            val screenPt = try {
                                liveMap.projection.toScreenLocation(LatLng(pt.lat, pt.lng))
                            } catch (_: Throwable) {
                                null
                            }
                            if (screenPt != null &&
                                screenPt.x.isFinite() &&
                                screenPt.y.isFinite() &&
                                screenPt.x >= -24f &&
                                screenPt.x <= mapFrameSizePx.width + 24f &&
                                screenPt.y >= -24f &&
                                screenPt.y <= mapFrameSizePx.height + 24f
                            ) {
                                val dotAccent = if (pt.isFirstStrike) NeonAmber else routineMagenta
                                val coreBg = if (pt.isFirstStrike) Color(0xFF0B1221) else routineMagenta
                                val rimColor = if (pt.isFirstStrike) NeonAmber else Color.White
                                Box(
                                    modifier = Modifier
                                        .offset {
                                            IntOffset(
                                                (screenPt.x - halfDotPx).roundToInt(),
                                                (screenPt.y - halfDotPx).roundToInt()
                                            )
                                        }
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .clickable {
                                            hasUserMovedCamera = true
                                            selectedLat = pt.lat
                                            selectedLng = pt.lng
                                            selectedPlaceName = pt.placeName.ifBlank {
                                                String.format(Locale.US, "%.4f, %.4f", pt.lat, pt.lng)
                                            }
                                            MapStyleHelper.animateCameraTo(liveMap, pt.lat, pt.lng, zoom = 15.0, durationMs = 550)
                                        }
                                        .testTag("picker_live_dot_${pt.key}"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Soft glowing halo
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(dotAccent.copy(alpha = 0.28f))
                                    )
                                    // Crisp jewel dot with high-contrast rim & white center spark
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clip(CircleShape)
                                            .background(coreBg)
                                            .border(2.2.dp, rimColor, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(4.5.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFFFFBEB))
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Lightweight tactical grid canvas for Robolectric JVM environment
                    Box(modifier = Modifier.fillMaxSize()) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawRect(if (isDarkTheme) Color(0xFF0F172A) else Color(0xFFF8F9FA))
                            val step = 44.dp.toPx()
                            val gridColor = if (isDarkTheme) Color(0xFF1E293B) else Color(0xFFE2E8F0)
                            var x = 0f
                            while (x < size.width) {
                                drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
                                x += step
                            }
                            var y = 0f
                            while (y < size.height) {
                                drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                                y += step
                            }
                        }

                        // Preview all registered strikes for this person on the picker map as live dots + labels
                        existingPersonLocations.forEachIndexed { idx, pt ->
                            val pinColor = if (pt.isFirstStrike) NeonAmber else routineMagenta
                            val coreBg = if (pt.isFirstStrike) Color(0xFF0B1221) else routineMagenta
                            val rimColor = if (pt.isFirstStrike) NeonAmber else Color.White
                            Row(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(
                                        start = (14 + ((idx * 58) % 210)).dp,
                                        top = (36 + ((idx * 34) % 105)).dp
                                    )
                                    .clickable {
                                        selectedLat = pt.lat
                                        selectedLng = pt.lng
                                        selectedPlaceName = pt.placeName.ifBlank {
                                            String.format(Locale.US, "%.4f, %.4f", pt.lat, pt.lng)
                                        }
                                    }
                                    .testTag("picker_existing_pin_${pt.key}"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(pinColor.copy(alpha = 0.28f))
                                        .testTag("picker_live_dot_${pt.key}"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(11.dp)
                                            .clip(CircleShape)
                                            .background(coreBg)
                                            .border(1.8.dp, rimColor, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(3.5.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFFFFBEB))
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(panelBg.copy(alpha = 0.88f))
                                        .border(0.8.dp, pinColor.copy(alpha = 0.65f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = pt.placeName.ifBlank {
                                            String.format(Locale.US, "%.2f, %.2f", pt.lat, pt.lng)
                                        },
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                if (existingPersonLocations.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(panelBg)
                            .border(1.dp, borderColor, RoundedCornerShape(50))
                            .clickable {
                                hasAutoFittedBounds = true
                                mapLibreRef?.let { map ->
                                    MapStyleHelper.fitCameraToPoints(
                                        mapLibreMap = map,
                                        points = existingPersonLocations,
                                        paddingPx = 86,
                                        singlePointZoom = 13.4,
                                        durationMs = 500
                                    )
                                }
                            }
                            .padding(horizontal = 9.dp, vertical = 5.dp)
                            .testTag("picker_fit_all_pins_btn"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(NeonAmber)
                        )
                        Text(
                            text = when {
                                isAr -> "${existingPersonLocations.size} نقاط • عرض الكل"
                                isFr -> "${existingPersonLocations.size} pts • Voir tout"
                                else -> "${existingPersonLocations.size} Dots • Fit All"
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = textPrimary,
                            maxLines = 1
                        )
                    }
                }

                // Center Pinpoint Overlay (mathematically centered in visible map frame)
                Box(
                    modifier = Modifier.align(Alignment.Center),
                    contentAlignment = Alignment.Center
                ) {
                    if (allowCategorySelection && !isFirstStrikeSelected) {
                        // Subtle magenta/violet aura ring around center pin when Routine Strike is selected
                        Box(
                            modifier = Modifier
                                .offset(y = 10.dp)
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(routineMagenta.copy(alpha = 0.22f))
                                .border(1.3.dp, routineMagenta.copy(alpha = 0.72f), CircleShape)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .offset(y = 14.dp)
                            .size(if (isCameraDragging) 12.dp else 8.dp)
                            .alpha(shadowAlpha)
                            .clip(CircleShape)
                            .background(Color.Black)
                            .testTag("location_picker_pin_shadow")
                    )
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = if (isAr) "موقع الضربة" else "Strike Pin",
                        tint = if (allowCategorySelection && !isFirstStrikeSelected) routineMagenta else NeonAmber,
                        modifier = Modifier
                            .offset(y = (-12).dp + pinTranslationY)
                            .scale(pinScale)
                            .size(44.dp)
                            .testTag("location_picker_pin")
                    )
                }
            }

            // 2b. Person's Existing Registered Map Strikes Strip (quick jump & inspect inside picker)
            if (!imeVisible && existingPersonLocations.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(panelBg)
                        .border(0.9.dp, borderColor.copy(alpha = 0.65f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 10.dp, vertical = 7.dp)
                        .testTag("picker_person_locations_strip"),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    val ownerLabel = personName?.takeIf { it.isNotBlank() }
                    Text(
                        text = when {
                            isAr && ownerLabel != null -> "سترايكات $ownerLabel المسجلة على الخريطة (${existingPersonLocations.size})"
                            isAr -> "السترايكات المسجلة على الخريطة (${existingPersonLocations.size})"
                            isFr && ownerLabel != null -> "Strikes enregistrés de $ownerLabel (${existingPersonLocations.size})"
                            ownerLabel != null -> "$ownerLabel's Map Strikes (${existingPersonLocations.size})"
                            else -> "Registered Map Strikes (${existingPersonLocations.size})"
                        },
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentCyan,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(existingPersonLocations, key = { it.key }) { pt ->
                            val chipColor = if (pt.isFirstStrike) NeonAmber else routineMagenta
                            val placeText = pt.placeName.ifBlank {
                                String.format(Locale.US, "%.3f, %.3f", pt.lat, pt.lng)
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(chipColor.copy(alpha = 0.16f))
                                    .border(0.9.dp, chipColor.copy(alpha = 0.58f), RoundedCornerShape(10.dp))
                                    .clickable {
                                        hasUserMovedCamera = true
                                        selectedLat = pt.lat
                                        selectedLng = pt.lng
                                        selectedPlaceName = placeText
                                        mapLibreRef?.let { map ->
                                            MapStyleHelper.animateCameraTo(map, pt.lat, pt.lng, zoom = 15.0, durationMs = 550)
                                        }
                                    }
                                    .padding(horizontal = 8.dp, vertical = 5.dp)
                                    .testTag("picker_saved_loc_chip_${pt.key}")
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(chipColor)
                                )
                                Text(
                                    text = if (pt.date.isNotBlank()) "$placeText • ${pt.date}" else placeText,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // 3. Bottom Address Preview & Confirm Card (always strictly within screen frame)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(panelBg)
                    .border(1.1.dp, borderColor, RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!imeVisible) {
                    if (allowCategorySelection) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val firstSelected = isFirstStrikeSelected
                            val firstBg = if (firstSelected) NeonAmber.copy(alpha = 0.22f) else Color.Transparent
                            val firstBorder = if (firstSelected) NeonAmber else borderColor.copy(alpha = 0.4f)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(firstBg)
                                    .border(1.2.dp, firstBorder, RoundedCornerShape(12.dp))
                                    .clickable { isFirstStrikeSelected = true }
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                                    .testTag("picker_category_first_strike"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = when {
                                        isAr -> "ضربة أولى (First)"
                                        isFr -> "Premier Strike"
                                        else -> "First Strike"
                                    },
                                    fontSize = 11.5.sp,
                                    fontWeight = if (firstSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                    color = if (firstSelected) NeonAmber else textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            val routineSelected = !isFirstStrikeSelected
                            val routineBg = if (routineSelected) routineMagenta.copy(alpha = 0.22f) else Color.Transparent
                            val routineBorder = if (routineSelected) routineMagenta else borderColor.copy(alpha = 0.4f)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(routineBg)
                                    .border(1.2.dp, routineBorder, RoundedCornerShape(12.dp))
                                    .clickable { isFirstStrikeSelected = false }
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                                    .testTag("picker_category_routine_strike"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = when {
                                        isAr -> "سترايك روتيني"
                                        isFr -> "Strike routinier"
                                        else -> "Routine Strike"
                                    },
                                    fontSize = 11.5.sp,
                                    fontWeight = if (routineSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                    color = if (routineSelected) routineMagenta else textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = titleOverride ?: when {
                                    allowCategorySelection && !isFirstStrikeSelected -> when {
                                        isAr -> "موقع السترايك الروتيني المحدد"
                                        isFr -> "Lieu du strike routinier"
                                        else -> "Selected Routine Strike Location"
                                    }
                                    isAr -> "موقع الضربة الأولى المحدد"
                                    isFr -> "Lieu du premier strike sélectionné"
                                    else -> "Selected First-Strike Location"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = activeAccent,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = String.format(Locale.US, "%.4f, %.4f", selectedLat, selectedLng),
                                fontSize = 10.5.sp,
                                fontFamily = PlusJakartaSans,
                                color = textSecondary,
                                maxLines = 1
                            )
                        }
                        Text(
                            text = if (isReverseGeocoding) {
                                when {
                                    isAr -> "جاري تحديد اسم الموقع..."
                                    isFr -> "Résolution de l'adresse..."
                                    else -> "Resolving address..."
                                }
                            } else {
                                selectedPlaceName
                            },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = PlusJakartaSans,
                            color = textPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("picker_resolved_place_name")
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (initialLat != null && initialLng != null) {
                        OutlinedButton(
                            onClick = onClear,
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .heightIn(min = 48.dp)
                                .testTag("picker_clear_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = if (isAr) "مسح" else "Clear",
                                tint = NeonRose,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Button(
                        onClick = {
                            val cleanPlace = NominatimGeocoder.stripTifinagh(selectedPlaceName)
                            onConfirmWithCategory?.invoke(selectedLat, selectedLng, cleanPlace, isFirstStrikeSelected)
                            onConfirm(selectedLat, selectedLng, cleanPlace)
                        },
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = activeAccent,
                            contentColor = if (allowCategorySelection && !isFirstStrikeSelected) Color.White else Color(0xFF031521)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .heightIn(min = 48.dp)
                            .testTag("picker_confirm_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = confirmLabelOverride ?: when {
                                allowCategorySelection && !isFirstStrikeSelected -> when {
                                    isAr -> "تأكيد موقع السترايك"
                                    isFr -> "Confirmer le lieu du strike"
                                    else -> "Confirm Strike Location"
                                }
                                isAr -> "تأكيد موقع الضربة الأولى"
                                isFr -> "Confirmer le lieu"
                                else -> "Confirm First-Strike Location"
                            },
                            fontWeight = FontWeight.Black,
                            fontFamily = PlusJakartaSans,
                            fontSize = 13.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

internal fun hasLocationPermission(context: Context): Boolean {
    val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED
    val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED
    return fine || coarse
}

@SuppressLint("MissingPermission")
internal fun resolveBestLocation(
    context: Context,
    onResolved: (Double, Double) -> Unit
) {
    if (isRobolectricRuntime() || !hasLocationPermission(context)) return
    try {
        val fused = LocationServices.getFusedLocationProviderClient(context)
        fused.lastLocation
            .addOnSuccessListener { loc ->
                if (loc != null && loc.latitude.isFinite() && loc.longitude.isFinite()) {
                    onResolved(loc.latitude, loc.longitude)
                } else {
                    val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                    val gps = runCatching { lm?.getLastKnownLocation(LocationManager.GPS_PROVIDER) }.getOrNull()
                    val net = runCatching { lm?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) }.getOrNull()
                    val best = gps ?: net
                    if (best != null) {
                        onResolved(best.latitude, best.longitude)
                    }
                }
            }
    } catch (_: Throwable) {
        // Fallback ignored cleanly
    }
}
