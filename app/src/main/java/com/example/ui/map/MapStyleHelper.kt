package com.example.ui.map

import android.content.Context
import android.graphics.Color as AndroidColor
import android.os.Build
import com.example.data.model.Individual
import org.json.JSONArray
import org.json.JSONObject
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.offline.OfflineManager
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.BackgroundLayer
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.FillExtrusionLayer
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.HeatmapLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.android.style.sources.VectorSource
import kotlin.math.abs

enum class MapVisualTheme {
    OBSIDIAN_DARK,
    CLASSIC_LIGHT
}

enum class HeatmapDisplayMode {
    HYBRID,
    HEATMAP_GLOW,
    CLUSTER_PINS
}

data class FirstStrikeHotspot(
    val id: String,
    val lat: Double,
    val lng: Double,
    val placeName: String,
    val individuals: List<Individual>,
    val heatScore: Double = 0.0
)

data class HottestRegionTarget(
    val lat: Double,
    val lng: Double,
    val placeName: String,
    val heatScore: Double,
    val recommendedZoom: Double,
    val peopleCount: Int
)

fun isRobolectricRuntime(): Boolean {
    val fingerprint = Build.FINGERPRINT ?: ""
    val hardware = Build.HARDWARE ?: ""
    val product = Build.PRODUCT ?: ""
    return fingerprint.contains("robolectric", ignoreCase = true) ||
        hardware.contains("robolectric", ignoreCase = true) ||
        product.contains("robolectric", ignoreCase = true)
}

object MapStyleHelper {

    const val OPEN_FREE_MAP_LIBERTY_STYLE_URI = "https://tiles.openfreemap.org/styles/liberty"

    const val FIRST_STRIKE_SOURCE_ID = "obsidian-first-strike-source"
    const val HEATMAP_LAYER_ID = "obsidian-first-strike-heatmap"
    const val CIRCLE_LAYER_ID = "obsidian-first-strike-circles"
    const val SYMBOL_LAYER_ID = "obsidian-first-strike-labels"

    const val USER_LOCATION_SOURCE_ID = "obsidian-user-location-source"
    const val USER_LOCATION_HALO_LAYER_ID = "obsidian-user-location-halo"
    const val USER_LOCATION_CORE_LAYER_ID = "obsidian-user-location-core"

    const val BUILDINGS_3D_LAYER_ID = "obsidian-3d-buildings"

    // Default camera overview (used only when no first-strike locations exist yet)
    val DEFAULT_CENTER = LatLng(32.5, 4.0)
    const val DEFAULT_ZOOM = 4.2

    // Obsidian Dark Mode Palette
    private val DARK_LAND_PRIMARY = AndroidColor.parseColor("#0F172A")
    private val DARK_LAND_SECONDARY = AndroidColor.parseColor("#17263C")
    private val DARK_WATER = AndroidColor.parseColor("#0E1626")
    private val DARK_PARK = AndroidColor.parseColor("#18332E")
    private val DARK_HIGHWAY_FILL = AndroidColor.parseColor("#38414E")
    private val DARK_HIGHWAY_CASING_MAJOR = AndroidColor.parseColor("#D97706")
    private val DARK_HIGHWAY_CASING_SECONDARY = AndroidColor.parseColor("#64748B")
    private val DARK_LOCAL_ROAD = AndroidColor.parseColor("#2C3646")
    private val DARK_BUILDING_FLAT = AndroidColor.parseColor("#162238")
    private val DARK_BUILDING_3D = AndroidColor.parseColor("#1E293B")
    private val DARK_LABEL_TEXT = AndroidColor.parseColor("#E2E8F0")
    private val DARK_LABEL_HALO = AndroidColor.parseColor("#0F172A")

    // Classic Light Mode Palette
    private val LIGHT_LAND = AndroidColor.parseColor("#F8F9FA")
    private val LIGHT_WATER = AndroidColor.parseColor("#AADAFF")
    private val LIGHT_PARK = AndroidColor.parseColor("#CEEAD6")
    private val LIGHT_HIGHWAY_FILL = AndroidColor.parseColor("#FCD34D")
    private val LIGHT_HIGHWAY_CASING = AndroidColor.parseColor("#F59E0B")
    private val LIGHT_LOCAL_ROAD = AndroidColor.parseColor("#FFFFFF")
    private val LIGHT_BUILDING_FLAT = AndroidColor.parseColor("#E9ECEF")
    private val LIGHT_BUILDING_3D = AndroidColor.parseColor("#CBD5E1")
    private val LIGHT_LABEL_TEXT = AndroidColor.parseColor("#1E293B")
    private val LIGHT_LABEL_HALO = AndroidColor.parseColor("#FFFFFF")

    fun pointHeatWeight(person: Individual): Double {
        return (1.0 + (person.strikeCount.coerceAtLeast(1) * 0.15)).coerceAtMost(4.0)
    }

    fun ensureMapLibreInitialized(context: Context): Boolean {
        if (isRobolectricRuntime()) return false
        return try {
            MapLibre.getInstance(context.applicationContext)
            true
        } catch (_: Throwable) {
            false
        }
    }

    fun createHighPrecisionMapView(context: Context): MapView {
        ensureMapLibreInitialized(context)
        val density = context.resources.displayMetrics.density.coerceAtLeast(1f)
        val options = MapLibreMapOptions.createFromAttributes(context)
            .pixelRatio(density)
        return MapView(context, options)
    }

    fun configureHighPrecisionMap(mapLibreMap: MapLibreMap, context: Context) {
        mapLibreMap.setMaxZoomPreference(21.0)
        mapLibreMap.setMinZoomPreference(1.4)
        mapLibreMap.uiSettings.apply {
            isCompassEnabled = false
            isLogoEnabled = false
            isAttributionEnabled = false
            isRotateGesturesEnabled = true
            isTiltGesturesEnabled = true
            isZoomGesturesEnabled = true
            isScrollGesturesEnabled = true
            isDoubleTapGesturesEnabled = true
            isQuickZoomGesturesEnabled = true
        }
        try {
            OfflineManager.getInstance(context.applicationContext)
                .setMaximumAmbientCacheSize(64L * 1024L * 1024L, object : OfflineManager.FileSourceCallback {
                    override fun onSuccess() = Unit
                    override fun onError(message: String) = Unit
                })
        } catch (_: Throwable) {
            // Best-effort ambient cache sizing
        }
    }

    fun buildFirstStrikeGeoJson(individuals: List<Individual>): String {
        val features = JSONArray()
        for (person in individuals) {
            val lat = person.firstStrikeLat ?: continue
            val lng = person.firstStrikeLng ?: continue
            if (!lat.isFinite() || !lng.isFinite()) continue
            if (lat !in -90.0..90.0 || lng !in -180.0..180.0) continue

            val geometry = JSONObject().apply {
                put("type", "Point")
                put("coordinates", JSONArray().apply {
                    put(lng)
                    put(lat)
                })
            }
            val weight = pointHeatWeight(person)
            val properties = JSONObject().apply {
                put("id", person.id)
                put("name", person.fullName)
                put("type", person.type)
                put("boost", person.boost)
                put("strikeCount", person.strikeCount)
                put("placeName", person.firstStrikePlaceName)
                put("weight", weight)
            }
            val feature = JSONObject().apply {
                put("type", "Feature")
                put("geometry", geometry)
                put("properties", properties)
            }
            features.put(feature)
        }
        return JSONObject().apply {
            put("type", "FeatureCollection")
            put("features", features)
        }.toString()
    }

    fun buildUserLocationGeoJson(lat: Double?, lng: Double?): String {
        val features = JSONArray()
        if (lat != null && lng != null && lat.isFinite() && lng.isFinite()) {
            val feature = JSONObject().apply {
                put("type", "Feature")
                put("geometry", JSONObject().apply {
                    put("type", "Point")
                    put("coordinates", JSONArray().apply {
                        put(lng)
                        put(lat)
                    })
                })
                put("properties", JSONObject())
            }
            features.put(feature)
        }
        return JSONObject().apply {
            put("type", "FeatureCollection")
            put("features", features)
        }.toString()
    }

    fun groupHotspots(
        individuals: List<Individual>,
        thresholdDegrees: Double = 0.018
    ): List<FirstStrikeHotspot> {
        val geoPeople = individuals.filter { it.hasFirstStrikeLocation }
        if (geoPeople.isEmpty()) return emptyList()

        val clusters = mutableListOf<MutableList<Individual>>()
        for (person in geoPeople) {
            val lat = person.firstStrikeLat ?: continue
            val lng = person.firstStrikeLng ?: continue
            val existing = clusters.firstOrNull { cluster ->
                val anchor = cluster.first()
                val aLat = anchor.firstStrikeLat ?: 0.0
                val aLng = anchor.firstStrikeLng ?: 0.0
                abs(lat - aLat) <= thresholdDegrees && abs(lng - aLng) <= thresholdDegrees
            }
            if (existing != null) {
                existing.add(person)
            } else {
                clusters.add(mutableListOf(person))
            }
        }

        return clusters.mapIndexed { idx, group ->
            val avgLat = group.mapNotNull { it.firstStrikeLat }.average()
            val avgLng = group.mapNotNull { it.firstStrikeLng }.average()
            val place = group.firstOrNull { it.firstStrikePlaceName.isNotBlank() }?.firstStrikePlaceName
                ?: String.format(java.util.Locale.US, "%.4f, %.4f", avgLat, avgLng)
            val totalHeat = group.sumOf { pointHeatWeight(it) }
            FirstStrikeHotspot(
                id = "hotspot_${idx}_${group.first().id}",
                lat = avgLat,
                lng = avgLng,
                placeName = place,
                individuals = group.sortedByDescending { it.boost },
                heatScore = totalHeat
            )
        }.sortedWith(
            compareByDescending<FirstStrikeHotspot> { it.heatScore }
                .thenByDescending { it.individuals.size }
                .thenByDescending { it.individuals.sumOf { person -> person.strikeCount } }
                .thenByDescending { it.individuals.sumOf { person -> person.boost } }
        )
    }

    /**
     * Finds the hottest heatmap region (highest cumulative thermal weight within a city-scale
     * neighborhood radius) so launching the map centers directly on the hottest area.
     */
    fun findHottestRegion(
        individuals: List<Individual>,
        regionRadiusDegrees: Double = 0.08
    ): HottestRegionTarget? {
        val geoPeople = individuals.filter {
            val lat = it.firstStrikeLat
            val lng = it.firstStrikeLng
            lat != null && lng != null &&
                lat.isFinite() && lng.isFinite() &&
                lat in -90.0..90.0 && lng in -180.0..180.0
        }
        if (geoPeople.isEmpty()) return null

        val radiusSq = regionRadiusDegrees * regionRadiusDegrees
        var bestCluster: List<Individual> = emptyList()
        var bestHeat = -1.0
        var bestStrikes = -1
        var bestBoost = -1.0

        for (anchor in geoPeople) {
            val aLat = anchor.firstStrikeLat ?: continue
            val aLng = anchor.firstStrikeLng ?: continue
            val neighbors = geoPeople.filter { candidate ->
                val cLat = candidate.firstStrikeLat ?: return@filter false
                val cLng = candidate.firstStrikeLng ?: return@filter false
                val dLat = cLat - aLat
                val dLng = cLng - aLng
                (dLat * dLat + dLng * dLng) <= radiusSq
            }
            val heat = neighbors.sumOf { pointHeatWeight(it) }
            val strikes = neighbors.sumOf { it.strikeCount }
            val boost = neighbors.sumOf { it.boost }
            val isBetter = heat > bestHeat + 1e-6 ||
                (abs(heat - bestHeat) <= 1e-6 && neighbors.size > bestCluster.size) ||
                (abs(heat - bestHeat) <= 1e-6 && neighbors.size == bestCluster.size && strikes > bestStrikes) ||
                (abs(heat - bestHeat) <= 1e-6 && neighbors.size == bestCluster.size && strikes == bestStrikes && boost > bestBoost)
            if (isBetter) {
                bestCluster = neighbors
                bestHeat = heat
                bestStrikes = strikes
                bestBoost = boost
            }
        }

        if (bestCluster.isEmpty()) return null

        var weightedLatSum = 0.0
        var weightedLngSum = 0.0
        var weightTotal = 0.0
        for (p in bestCluster) {
            val lat = p.firstStrikeLat ?: continue
            val lng = p.firstStrikeLng ?: continue
            val w = pointHeatWeight(p)
            weightedLatSum += lat * w
            weightedLngSum += lng * w
            weightTotal += w
        }
        val centerLat = weightedLatSum / weightTotal.coerceAtLeast(1e-6)
        val centerLng = weightedLngSum / weightTotal.coerceAtLeast(1e-6)

        val maxSpreadDeg = bestCluster.maxOf { p ->
            val dLat = (p.firstStrikeLat ?: centerLat) - centerLat
            val dLng = (p.firstStrikeLng ?: centerLng) - centerLng
            kotlin.math.hypot(dLat, dLng)
        }
        val recommendedZoom = when {
            maxSpreadDeg <= 0.015 -> 13.0
            maxSpreadDeg <= 0.045 -> 12.2
            else -> 11.4
        }

        val namedLeader = bestCluster
            .filter { it.firstStrikePlaceName.isNotBlank() }
            .maxByOrNull { pointHeatWeight(it) }
        val placeLabel = namedLeader?.firstStrikePlaceName
            ?: String.format(java.util.Locale.US, "%.4f, %.4f", centerLat, centerLng)

        return HottestRegionTarget(
            lat = centerLat,
            lng = centerLng,
            placeName = placeLabel,
            heatScore = bestHeat,
            recommendedZoom = recommendedZoom,
            peopleCount = bestCluster.size
        )
    }

    fun findNearestHotspot(
        tapLat: Double,
        tapLng: Double,
        hotspots: List<FirstStrikeHotspot>,
        zoomLevel: Double
    ): FirstStrikeHotspot? {
        if (hotspots.isEmpty()) return null
        val hitRadiusDeg = when {
            zoomLevel >= 15.0 -> 0.008
            zoomLevel >= 12.0 -> 0.025
            zoomLevel >= 9.0 -> 0.12
            zoomLevel >= 6.0 -> 0.55
            else -> 1.6
        }
        return hotspots
            .map { hotspot ->
                val dLat = hotspot.lat - tapLat
                val dLng = hotspot.lng - tapLng
                hotspot to (dLat * dLat + dLng * dLng)
            }
            .filter { (_, distSq) -> distSq <= hitRadiusDeg * hitRadiusDeg }
            .minByOrNull { it.second }
            ?.first
    }

    fun animateCameraTo(
        mapLibreMap: MapLibreMap,
        lat: Double,
        lng: Double,
        zoom: Double = 14.5,
        durationMs: Int = 800
    ) {
        if (!lat.isFinite() || !lng.isFinite()) return
        mapLibreMap.animateCamera(
            CameraUpdateFactory.newLatLngZoom(LatLng(lat, lng), zoom),
            durationMs
        )
    }

    fun applyGoogleGradeStyling(
        style: Style,
        theme: MapVisualTheme,
        show3dBuildings: Boolean,
        displayMode: HeatmapDisplayMode,
        individuals: List<Individual>,
        userLat: Double? = null,
        userLng: Double? = null
    ) {
        restyleBaseLayers(style, theme)
        ensureOrUpdate3dBuildings(style, theme, show3dBuildings)
        ensureOrUpdateFirstStrikeLayers(style, theme, displayMode, individuals)
        ensureOrUpdateUserLocationLayers(style, userLat, userLng)
    }

    fun updateFirstStrikeDataOnly(
        style: Style,
        theme: MapVisualTheme,
        displayMode: HeatmapDisplayMode,
        individuals: List<Individual>,
        userLat: Double?,
        userLng: Double?
    ) {
        ensureOrUpdateFirstStrikeLayers(style, theme, displayMode, individuals)
        ensureOrUpdateUserLocationLayers(style, userLat, userLng)
    }

    private fun restyleBaseLayers(style: Style, theme: MapVisualTheme) {
        val isDark = theme == MapVisualTheme.OBSIDIAN_DARK
        val landPrimary = if (isDark) DARK_LAND_PRIMARY else LIGHT_LAND
        val landSecondary = if (isDark) DARK_LAND_SECONDARY else LIGHT_LAND
        val waterColor = if (isDark) DARK_WATER else LIGHT_WATER
        val parkColor = if (isDark) DARK_PARK else LIGHT_PARK
        val highwayFill = if (isDark) DARK_HIGHWAY_FILL else LIGHT_HIGHWAY_FILL
        val highwayCasingMajor = if (isDark) DARK_HIGHWAY_CASING_MAJOR else LIGHT_HIGHWAY_CASING
        val highwayCasingMinor = if (isDark) DARK_HIGHWAY_CASING_SECONDARY else LIGHT_HIGHWAY_CASING
        val localRoad = if (isDark) DARK_LOCAL_ROAD else LIGHT_LOCAL_ROAD
        val buildingFlat = if (isDark) DARK_BUILDING_FLAT else LIGHT_BUILDING_FLAT
        val labelColor = if (isDark) DARK_LABEL_TEXT else LIGHT_LABEL_TEXT
        val labelHalo = if (isDark) DARK_LABEL_HALO else LIGHT_LABEL_HALO

        for (layer in style.layers) {
            val id = layer.id.lowercase()
            if (id.startsWith("obsidian-")) continue

            when (layer) {
                is BackgroundLayer -> {
                    layer.setProperties(PropertyFactory.backgroundColor(landPrimary))
                }
                is FillLayer -> {
                    when {
                        id.contains("water") || id.contains("ocean") || id.contains("lake") || id.contains("river") -> {
                            layer.setProperties(PropertyFactory.fillColor(waterColor))
                        }
                        id.contains("park") || id.contains("grass") || id.contains("wood") ||
                            id.contains("forest") || id.contains("green") || id.contains("garden") ||
                            id.contains("cemetery") || id.contains("pitch") -> {
                            layer.setProperties(PropertyFactory.fillColor(parkColor))
                        }
                        id.contains("building") -> {
                            layer.setProperties(
                                PropertyFactory.fillColor(buildingFlat),
                                PropertyFactory.fillOpacity(if (isDark) 0.72f else 0.85f)
                            )
                        }
                        id.contains("land") || id.contains("residential") || id.contains("urban") ||
                            id.contains("aeroway") -> {
                            layer.setProperties(PropertyFactory.fillColor(landSecondary))
                        }
                    }
                }
                is LineLayer -> {
                    val isCasing = id.contains("casing") || id.contains("outline")
                    when {
                        id.contains("water") || id.contains("river") || id.contains("stream") -> {
                            layer.setProperties(PropertyFactory.lineColor(waterColor))
                        }
                        id.contains("motorway") || id.contains("trunk") || id.contains("highway") -> {
                            layer.setProperties(
                                PropertyFactory.lineColor(if (isCasing) highwayCasingMajor else highwayFill)
                            )
                        }
                        id.contains("primary") || id.contains("secondary") -> {
                            layer.setProperties(
                                PropertyFactory.lineColor(if (isCasing) highwayCasingMinor else highwayFill)
                            )
                        }
                        id.contains("road") || id.contains("street") || id.contains("minor") ||
                            id.contains("service") || id.contains("tertiary") || id.contains("track") ||
                            id.contains("path") || id.contains("bridge") || id.contains("tunnel") -> {
                            layer.setProperties(PropertyFactory.lineColor(localRoad))
                        }
                        id.contains("boundary") || id.contains("border") -> {
                            layer.setProperties(
                                PropertyFactory.lineColor(
                                    if (isDark) AndroidColor.parseColor("#334155") else AndroidColor.parseColor("#CBD5E1")
                                )
                            )
                        }
                    }
                }
                is SymbolLayer -> {
                    layer.setProperties(
                        PropertyFactory.textColor(labelColor),
                        PropertyFactory.textHaloColor(labelHalo),
                        PropertyFactory.textHaloWidth(1.4f)
                    )
                }
            }
        }
    }

    private fun ensureOrUpdate3dBuildings(
        style: Style,
        theme: MapVisualTheme,
        show3dBuildings: Boolean
    ) {
        val color = if (theme == MapVisualTheme.OBSIDIAN_DARK) DARK_BUILDING_3D else LIGHT_BUILDING_3D
        val visibility = if (show3dBuildings) Property.VISIBLE else Property.NONE

        val existing = style.getLayerAs<FillExtrusionLayer>(BUILDINGS_3D_LAYER_ID)
        if (existing != null) {
            existing.setProperties(
                PropertyFactory.fillExtrusionColor(color),
                PropertyFactory.fillExtrusionOpacity(0.75f),
                PropertyFactory.visibility(visibility)
            )
            return
        }

        val vectorSourceId = style.sources
            .filterIsInstance<VectorSource>()
            .firstOrNull()
            ?.id ?: "openmaptiles"

        val extrusionLayer = FillExtrusionLayer(BUILDINGS_3D_LAYER_ID, vectorSourceId).apply {
            sourceLayer = "building"
            minZoom = 15f
            setProperties(
                PropertyFactory.fillExtrusionColor(color),
                PropertyFactory.fillExtrusionHeight(Expression.get("render_height")),
                PropertyFactory.fillExtrusionBase(Expression.get("render_min_height")),
                PropertyFactory.fillExtrusionOpacity(0.75f),
                PropertyFactory.visibility(visibility)
            )
        }

        val labelLayer = style.layers.firstOrNull { it is SymbolLayer && !it.id.startsWith("obsidian-") }
        if (labelLayer != null) {
            style.addLayerBelow(extrusionLayer, labelLayer.id)
        } else {
            style.addLayer(extrusionLayer)
        }
    }

    private fun ensureOrUpdateFirstStrikeLayers(
        style: Style,
        theme: MapVisualTheme,
        displayMode: HeatmapDisplayMode,
        individuals: List<Individual>
    ) {
        val geoJson = buildFirstStrikeGeoJson(individuals)
        val existingSource = style.getSourceAs<GeoJsonSource>(FIRST_STRIKE_SOURCE_ID)
        if (existingSource != null) {
            existingSource.setGeoJson(geoJson)
        } else {
            style.addSource(GeoJsonSource(FIRST_STRIKE_SOURCE_ID, geoJson))
        }

        val heatOpacityExpr = when (displayMode) {
            HeatmapDisplayMode.HEATMAP_GLOW -> Expression.literal(0.92f)
            HeatmapDisplayMode.CLUSTER_PINS -> Expression.literal(0f)
            HeatmapDisplayMode.HYBRID -> Expression.interpolate(
                Expression.linear(),
                Expression.zoom(),
                Expression.stop(10, 0.92f),
                Expression.stop(15, 0.42f)
            )
        }
        val heatVisibility = if (displayMode == HeatmapDisplayMode.CLUSTER_PINS) Property.NONE else Property.VISIBLE

        val existingHeat = style.getLayerAs<HeatmapLayer>(HEATMAP_LAYER_ID)
        if (existingHeat == null) {
            val heatmapLayer = HeatmapLayer(HEATMAP_LAYER_ID, FIRST_STRIKE_SOURCE_ID).apply {
                setProperties(
                    PropertyFactory.heatmapWeight(Expression.get("weight")),
                    PropertyFactory.heatmapIntensity(
                        Expression.interpolate(
                            Expression.linear(),
                            Expression.zoom(),
                            Expression.stop(0, 0.95f),
                            Expression.stop(9, 1.85f),
                            Expression.stop(15, 2.4f)
                        )
                    ),
                    PropertyFactory.heatmapRadius(
                        Expression.interpolate(
                            Expression.linear(),
                            Expression.zoom(),
                            Expression.stop(0, 16f),
                            Expression.stop(9, 30f),
                            Expression.stop(15, 46f)
                        )
                    ),
                    PropertyFactory.heatmapColor(
                        Expression.interpolate(
                            Expression.linear(),
                            Expression.heatmapDensity(),
                            Expression.stop(0.0, Expression.rgba(0, 0, 0, 0.0f)),
                            Expression.stop(0.20, Expression.rgba(34, 211, 238, 0.65f)),
                            Expression.stop(0.55, Expression.rgba(245, 158, 11, 0.85f)),
                            Expression.stop(0.85, Expression.rgba(239, 68, 68, 0.95f)),
                            Expression.stop(1.0, Expression.rgba(255, 244, 230, 1.0f))
                        )
                    ),
                    PropertyFactory.heatmapOpacity(heatOpacityExpr),
                    PropertyFactory.visibility(heatVisibility)
                )
            }
            style.addLayer(heatmapLayer)
        } else {
            existingHeat.setProperties(
                PropertyFactory.heatmapOpacity(heatOpacityExpr),
                PropertyFactory.visibility(heatVisibility)
            )
        }

        val showPins = displayMode != HeatmapDisplayMode.HEATMAP_GLOW
        val circleVisibility = if (showPins) Property.VISIBLE else Property.NONE
        val circleOpacityExpr = when (displayMode) {
            HeatmapDisplayMode.CLUSTER_PINS -> Expression.literal(0.95f)
            HeatmapDisplayMode.HYBRID -> Expression.interpolate(
                Expression.linear(),
                Expression.zoom(),
                Expression.stop(6, 0.0f),
                Expression.stop(9, 0.78f),
                Expression.stop(13, 0.98f)
            )
            HeatmapDisplayMode.HEATMAP_GLOW -> Expression.literal(0.0f)
        }
        val strokeColor = if (theme == MapVisualTheme.OBSIDIAN_DARK) DARK_LABEL_TEXT else LIGHT_LABEL_TEXT

        val existingCircle = style.getLayerAs<CircleLayer>(CIRCLE_LAYER_ID)
        if (existingCircle == null) {
            val circleLayer = CircleLayer(CIRCLE_LAYER_ID, FIRST_STRIKE_SOURCE_ID).apply {
                setProperties(
                    PropertyFactory.circleRadius(
                        Expression.interpolate(
                            Expression.linear(),
                            Expression.zoom(),
                            Expression.stop(4, 5f),
                            Expression.stop(10, 8f),
                            Expression.stop(16, 12f)
                        )
                    ),
                    PropertyFactory.circleColor(AndroidColor.parseColor("#22D3EE")),
                    PropertyFactory.circleStrokeWidth(2.0f),
                    PropertyFactory.circleStrokeColor(strokeColor),
                    PropertyFactory.circleOpacity(circleOpacityExpr),
                    PropertyFactory.circleStrokeOpacity(circleOpacityExpr),
                    PropertyFactory.visibility(circleVisibility)
                )
            }
            style.addLayer(circleLayer)
        } else {
            existingCircle.setProperties(
                PropertyFactory.circleStrokeColor(strokeColor),
                PropertyFactory.circleOpacity(circleOpacityExpr),
                PropertyFactory.circleStrokeOpacity(circleOpacityExpr),
                PropertyFactory.visibility(circleVisibility)
            )
        }

        val labelColor = if (theme == MapVisualTheme.OBSIDIAN_DARK) DARK_LABEL_TEXT else LIGHT_LABEL_TEXT
        val labelHalo = if (theme == MapVisualTheme.OBSIDIAN_DARK) DARK_LABEL_HALO else LIGHT_LABEL_HALO
        val existingSymbol = style.getLayerAs<SymbolLayer>(SYMBOL_LAYER_ID)
        if (existingSymbol == null) {
            val symbolLayer = SymbolLayer(SYMBOL_LAYER_ID, FIRST_STRIKE_SOURCE_ID).apply {
                minZoom = 10f
                setProperties(
                    PropertyFactory.textField(Expression.get("name")),
                    PropertyFactory.textSize(12f),
                    PropertyFactory.textOffset(arrayOf(0f, 1.35f)),
                    PropertyFactory.textAnchor(Property.TEXT_ANCHOR_TOP),
                    PropertyFactory.textColor(labelColor),
                    PropertyFactory.textHaloColor(labelHalo),
                    PropertyFactory.textHaloWidth(1.5f),
                    PropertyFactory.visibility(circleVisibility)
                )
            }
            style.addLayer(symbolLayer)
        } else {
            existingSymbol.setProperties(
                PropertyFactory.textColor(labelColor),
                PropertyFactory.textHaloColor(labelHalo),
                PropertyFactory.visibility(circleVisibility)
            )
        }
    }

    private fun ensureOrUpdateUserLocationLayers(
        style: Style,
        userLat: Double?,
        userLng: Double?
    ) {
        val geoJson = buildUserLocationGeoJson(userLat, userLng)
        val existingSource = style.getSourceAs<GeoJsonSource>(USER_LOCATION_SOURCE_ID)
        if (existingSource != null) {
            existingSource.setGeoJson(geoJson)
        } else {
            style.addSource(GeoJsonSource(USER_LOCATION_SOURCE_ID, geoJson))
        }

        if (style.getLayer(USER_LOCATION_HALO_LAYER_ID) == null) {
            style.addLayer(
                CircleLayer(USER_LOCATION_HALO_LAYER_ID, USER_LOCATION_SOURCE_ID).apply {
                    setProperties(
                        PropertyFactory.circleRadius(18f),
                        PropertyFactory.circleColor(AndroidColor.parseColor("#38BDF8")),
                        PropertyFactory.circleOpacity(0.28f),
                        PropertyFactory.circleStrokeWidth(1.2f),
                        PropertyFactory.circleStrokeColor(AndroidColor.parseColor("#22D3EE")),
                        PropertyFactory.circleStrokeOpacity(0.55f)
                    )
                }
            )
        }
        if (style.getLayer(USER_LOCATION_CORE_LAYER_ID) == null) {
            style.addLayer(
                CircleLayer(USER_LOCATION_CORE_LAYER_ID, USER_LOCATION_SOURCE_ID).apply {
                    setProperties(
                        PropertyFactory.circleRadius(6.5f),
                        PropertyFactory.circleColor(AndroidColor.parseColor("#22D3EE")),
                        PropertyFactory.circleStrokeWidth(2.4f),
                        PropertyFactory.circleStrokeColor(AndroidColor.WHITE)
                    )
                }
            )
        }
    }
}
