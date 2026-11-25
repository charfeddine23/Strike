package com.example.ui.map

import android.content.Context
import android.graphics.Color as AndroidColor
import android.os.Build
import com.example.data.model.Individual
import com.example.data.model.StrikeEvent
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

data class MappedStrikePoint(
    val key: String,
    val individualId: Long,
    val individualName: String,
    val individualType: String,
    val boost: Double,
    val strikeCount: Int,
    val lat: Double,
    val lng: Double,
    val placeName: String,
    val isFirstStrike: Boolean,
    val eventId: Long? = null,
    val date: String = "",
    val weight: Double = 1.0
)

data class FirstStrikeHotspot(
    val id: String,
    val lat: Double,
    val lng: Double,
    val placeName: String,
    val individuals: List<Individual>,
    val heatScore: Double = 0.0,
    val points: List<MappedStrikePoint> = emptyList(),
    val hasFirstStrikes: Boolean = true,
    val hasRoutineStrikes: Boolean = false,
    val firstStrikeCount: Int = 0,
    val routineStrikeCount: Int = 0
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
    const val FIRST_STRIKE_AURA_LAYER_ID = "obsidian-first-strike-aura"
    const val CIRCLE_LAYER_ID = "obsidian-first-strike-circles"
    const val FIRST_STRIKE_CORE_SPARK_LAYER_ID = "obsidian-first-strike-spark"
    const val SYMBOL_LAYER_ID = "obsidian-first-strike-labels"

    const val ROUTINE_STRIKE_SOURCE_ID = "obsidian-routine-strike-source"
    const val ROUTINE_HEATMAP_LAYER_ID = "obsidian-routine-strike-heatmap"
    const val ROUTINE_HALO_LAYER_ID = "obsidian-routine-strike-halo-outer"
    const val ROUTINE_INNER_HALO_LAYER_ID = "obsidian-routine-strike-halo-inner"
    const val ROUTINE_CIRCLE_LAYER_ID = "obsidian-routine-strike-circles"
    const val ROUTINE_SYMBOL_LAYER_ID = "obsidian-routine-strike-labels"

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

    fun eventHeatWeight(event: StrikeEvent, person: Individual?): Double {
        val base = if (event.isFirstStrike) 1.25 else 0.85
        val countBoost = event.count.coerceAtLeast(1) * 0.18
        val personBoost = ((person?.strikeCount ?: 0) * 0.05)
        return (base + countBoost + personBoost).coerceAtMost(4.0)
    }

    fun collectAllMappedPoints(
        individuals: List<Individual>,
        strikeEvents: List<StrikeEvent>
    ): List<MappedStrikePoint> {
        val byId = individuals.associateBy { it.id }
        val result = mutableListOf<MappedStrikePoint>()
        val mappedEvents = strikeEvents.filter { it.hasLocation }

        for (event in mappedEvents) {
            val lat = event.lat ?: continue
            val lng = event.lng ?: continue
            val person = byId[event.individualId]
            result.add(
                MappedStrikePoint(
                    key = "ev_${event.id}",
                    individualId = event.individualId,
                    individualName = person?.fullName ?: event.individualName,
                    individualType = person?.type ?: "M",
                    boost = person?.boost ?: 0.0,
                    strikeCount = event.count.coerceAtLeast(1),
                    lat = lat,
                    lng = lng,
                    placeName = event.placeName,
                    isFirstStrike = event.isFirstStrike,
                    eventId = event.id,
                    date = event.date,
                    weight = eventHeatWeight(event, person)
                )
            )
        }

        for (person in individuals) {
            if (!person.hasFirstStrikeLocation) continue
            val pLat = person.firstStrikeLat ?: continue
            val pLng = person.firstStrikeLng ?: continue
            val alreadyInEvents = mappedEvents.any { ev ->
                ev.individualId == person.id &&
                    ev.isFirstStrike &&
                    abs((ev.lat ?: 0.0) - pLat) <= 0.0005 &&
                    abs((ev.lng ?: 0.0) - pLng) <= 0.0005
            }
            if (!alreadyInEvents) {
                result.add(
                    MappedStrikePoint(
                        key = "ind_${person.id}",
                        individualId = person.id,
                        individualName = person.fullName,
                        individualType = person.type,
                        boost = person.boost,
                        strikeCount = person.strikeCount.coerceAtLeast(1),
                        lat = pLat,
                        lng = pLng,
                        placeName = person.firstStrikePlaceName,
                        isFirstStrike = true,
                        eventId = null,
                        date = "",
                        weight = pointHeatWeight(person)
                    )
                )
            }
        }
        return result
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
            .textureMode(true)
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

    fun buildFirstStrikeGeoJson(individuals: List<Individual>): String =
        buildFirstStrikeGeoJson(individuals, emptyList())

    fun buildFirstStrikeGeoJson(
        individuals: List<Individual>,
        strikeEvents: List<StrikeEvent>
    ): String {
        val points = collectAllMappedPoints(individuals, strikeEvents).filter { it.isFirstStrike }
        return buildGeoJsonFromPoints(points)
    }

    fun buildRoutineStrikeGeoJson(
        individuals: List<Individual>,
        strikeEvents: List<StrikeEvent>
    ): String {
        val points = collectAllMappedPoints(individuals, strikeEvents).filter { !it.isFirstStrike }
        return buildGeoJsonFromPoints(points)
    }

    private fun buildGeoJsonFromPoints(points: List<MappedStrikePoint>): String {
        val features = JSONArray()
        for (pt in points) {
            val lat = pt.lat
            val lng = pt.lng
            if (!lat.isFinite() || !lng.isFinite()) continue
            if (lat !in -90.0..90.0 || lng !in -180.0..180.0) continue

            val geometry = JSONObject().apply {
                put("type", "Point")
                put("coordinates", JSONArray().apply {
                    put(lng)
                    put(lat)
                })
            }
            val properties = JSONObject().apply {
                put("id", pt.individualId)
                put("eventId", pt.eventId ?: -1L)
                put("name", pt.individualName)
                put("type", pt.individualType)
                put("boost", pt.boost)
                put("strikeCount", pt.strikeCount)
                put("placeName", pt.placeName)
                put("isFirstStrike", pt.isFirstStrike)
                put("weight", pt.weight)
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
    ): List<FirstStrikeHotspot> =
        groupHotspots(individuals, emptyList(), thresholdDegrees)

    fun groupHotspots(
        individuals: List<Individual>,
        strikeEvents: List<StrikeEvent>,
        thresholdDegrees: Double = 0.018
    ): List<FirstStrikeHotspot> {
        val points = collectAllMappedPoints(individuals, strikeEvents)
        if (points.isEmpty()) return emptyList()

        val byId = individuals.associateBy { it.id }
        val clusters = mutableListOf<MutableList<MappedStrikePoint>>()
        for (pt in points) {
            val existing = clusters.firstOrNull { cluster ->
                val anchor = cluster.first()
                abs(pt.lat - anchor.lat) <= thresholdDegrees && abs(pt.lng - anchor.lng) <= thresholdDegrees
            }
            if (existing != null) {
                existing.add(pt)
            } else {
                clusters.add(mutableListOf(pt))
            }
        }

        return clusters.mapIndexed { idx, group ->
            val avgLat = group.map { it.lat }.average()
            val avgLng = group.map { it.lng }.average()
            val place = group.firstOrNull { it.placeName.isNotBlank() }?.placeName
                ?: String.format(java.util.Locale.US, "%.4f, %.4f", avgLat, avgLng)
            val totalHeat = group.sumOf { it.weight }
            val uniquePeople = group.mapNotNull { pt ->
                byId[pt.individualId] ?: Individual(
                    id = pt.individualId,
                    fullName = pt.individualName,
                    type = pt.individualType,
                    length = 0.0,
                    width = 0.0,
                    size = 0.0,
                    sScore = 0.0,
                    fScore = 0.0,
                    cScore = 0.0,
                    boost = pt.boost,
                    strikeCount = pt.strikeCount,
                    firstStrikeLat = pt.lat,
                    firstStrikeLng = pt.lng,
                    firstStrikePlaceName = pt.placeName
                )
            }.distinctBy { it.id }.sortedByDescending { it.boost }
            val firstCount = group.count { it.isFirstStrike }
            val routineCount = group.count { !it.isFirstStrike }
            val leadId = group.first().individualId

            FirstStrikeHotspot(
                id = "hotspot_${idx}_${leadId}",
                lat = avgLat,
                lng = avgLng,
                placeName = place,
                individuals = uniquePeople,
                heatScore = totalHeat,
                points = group,
                hasFirstStrikes = firstCount > 0,
                hasRoutineStrikes = routineCount > 0,
                firstStrikeCount = firstCount,
                routineStrikeCount = routineCount
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
    ): HottestRegionTarget? =
        findHottestRegion(individuals, emptyList(), regionRadiusDegrees)

    fun findHottestRegion(
        individuals: List<Individual>,
        strikeEvents: List<StrikeEvent>,
        regionRadiusDegrees: Double = 0.08
    ): HottestRegionTarget? {
        val points = collectAllMappedPoints(individuals, strikeEvents).filter {
            it.lat.isFinite() && it.lng.isFinite() &&
                it.lat in -90.0..90.0 && it.lng in -180.0..180.0
        }
        if (points.isEmpty()) return null

        val radiusSq = regionRadiusDegrees * regionRadiusDegrees
        var bestCluster: List<MappedStrikePoint> = emptyList()
        var bestHeat = -1.0
        var bestStrikes = -1
        var bestBoost = -1.0

        for (anchor in points) {
            val neighbors = points.filter { candidate ->
                val dLat = candidate.lat - anchor.lat
                val dLng = candidate.lng - anchor.lng
                (dLat * dLat + dLng * dLng) <= radiusSq
            }
            val heat = neighbors.sumOf { it.weight }
            val strikes = neighbors.distinctBy { it.individualId }.sumOf { it.strikeCount }
            val boost = neighbors.distinctBy { it.individualId }.sumOf { it.boost }
            val distinctCount = neighbors.distinctBy { it.individualId }.size
            val bestDistinctCount = bestCluster.distinctBy { it.individualId }.size
            val isBetter = heat > bestHeat + 1e-6 ||
                (abs(heat - bestHeat) <= 1e-6 && distinctCount > bestDistinctCount) ||
                (abs(heat - bestHeat) <= 1e-6 && distinctCount == bestDistinctCount && strikes > bestStrikes) ||
                (abs(heat - bestHeat) <= 1e-6 && distinctCount == bestDistinctCount && strikes == bestStrikes && boost > bestBoost)
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
        for (pt in bestCluster) {
            weightedLatSum += pt.lat * pt.weight
            weightedLngSum += pt.lng * pt.weight
            weightTotal += pt.weight
        }
        val centerLat = weightedLatSum / weightTotal.coerceAtLeast(1e-6)
        val centerLng = weightedLngSum / weightTotal.coerceAtLeast(1e-6)

        val maxSpreadDeg = bestCluster.maxOf { pt ->
            val dLat = pt.lat - centerLat
            val dLng = pt.lng - centerLng
            kotlin.math.hypot(dLat, dLng)
        }
        val recommendedZoom = when {
            maxSpreadDeg <= 0.015 -> 13.0
            maxSpreadDeg <= 0.045 -> 12.2
            else -> 11.4
        }

        val namedLeader = bestCluster
            .filter { it.placeName.isNotBlank() }
            .maxByOrNull { it.weight }
        val placeLabel = namedLeader?.placeName
            ?: String.format(java.util.Locale.US, "%.4f, %.4f", centerLat, centerLng)

        return HottestRegionTarget(
            lat = centerLat,
            lng = centerLng,
            placeName = placeLabel,
            heatScore = bestHeat,
            recommendedZoom = recommendedZoom,
            peopleCount = bestCluster.distinctBy { it.individualId }.size
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
        userLng: Double? = null,
        strikeEvents: List<StrikeEvent> = emptyList()
    ) {
        restyleBaseLayers(style, theme)
        ensureOrUpdate3dBuildings(style, theme, show3dBuildings)
        ensureOrUpdateRoutineStrikeLayers(style, theme, displayMode, individuals, strikeEvents)
        ensureOrUpdateFirstStrikeLayers(style, theme, displayMode, individuals, strikeEvents)
        ensureOrUpdateUserLocationLayers(style, userLat, userLng)
    }

    fun updateFirstStrikeDataOnly(
        style: Style,
        theme: MapVisualTheme,
        displayMode: HeatmapDisplayMode,
        individuals: List<Individual>,
        userLat: Double?,
        userLng: Double?,
        strikeEvents: List<StrikeEvent> = emptyList()
    ) {
        ensureOrUpdateRoutineStrikeLayers(style, theme, displayMode, individuals, strikeEvents)
        ensureOrUpdateFirstStrikeLayers(style, theme, displayMode, individuals, strikeEvents)
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
        individuals: List<Individual>,
        strikeEvents: List<StrikeEvent>
    ) {
        val geoJson = buildFirstStrikeGeoJson(individuals, strikeEvents)
        val existingSource = style.getSourceAs<GeoJsonSource>(FIRST_STRIKE_SOURCE_ID)
        if (existingSource != null) {
            existingSource.setGeoJson(geoJson)
        } else {
            style.addSource(GeoJsonSource(FIRST_STRIKE_SOURCE_ID, geoJson))
        }

        // First Strike renders strictly as a luxurious multi-layered Obsidian-Gold jewel dot (never a sprawling heatmap)
        style.getLayerAs<HeatmapLayer>(HEATMAP_LAYER_ID)?.setProperties(
            PropertyFactory.heatmapOpacity(0f),
            PropertyFactory.visibility(Property.NONE)
        )

        val isDark = theme == MapVisualTheme.OBSIDIAN_DARK
        val auraColor = AndroidColor.parseColor("#F59E0B")
        val coreFill = if (isDark) AndroidColor.parseColor("#0B1221") else AndroidColor.parseColor("#0F172A")
        val luxuryGoldRim = if (isDark) AndroidColor.parseColor("#FBBF24") else AndroidColor.parseColor("#D97706")
        val coreSparkColor = AndroidColor.parseColor("#FFFBEB")

        val existingAura = style.getLayerAs<CircleLayer>(FIRST_STRIKE_AURA_LAYER_ID)
        if (existingAura == null) {
            style.addLayer(
                CircleLayer(FIRST_STRIKE_AURA_LAYER_ID, FIRST_STRIKE_SOURCE_ID).apply {
                    setProperties(
                        PropertyFactory.circleRadius(
                            Expression.interpolate(
                                Expression.linear(),
                                Expression.zoom(),
                                Expression.stop(4, 9f),
                                Expression.stop(10, 13f),
                                Expression.stop(16, 17f)
                            )
                        ),
                        PropertyFactory.circleColor(auraColor),
                        PropertyFactory.circleBlur(0.95f),
                        PropertyFactory.circleOpacity(0.28f),
                        PropertyFactory.visibility(Property.VISIBLE)
                    )
                }
            )
        } else {
            existingAura.setProperties(
                PropertyFactory.circleColor(auraColor),
                PropertyFactory.circleOpacity(0.28f),
                PropertyFactory.visibility(Property.VISIBLE)
            )
        }

        val existingCircle = style.getLayerAs<CircleLayer>(CIRCLE_LAYER_ID)
        if (existingCircle == null) {
            style.addLayer(
                CircleLayer(CIRCLE_LAYER_ID, FIRST_STRIKE_SOURCE_ID).apply {
                    setProperties(
                        PropertyFactory.circleRadius(
                            Expression.interpolate(
                                Expression.linear(),
                                Expression.zoom(),
                                Expression.stop(4, 5.2f),
                                Expression.stop(10, 7.4f),
                                Expression.stop(16, 10.2f)
                            )
                        ),
                        PropertyFactory.circleColor(coreFill),
                        PropertyFactory.circleStrokeWidth(2.6f),
                        PropertyFactory.circleStrokeColor(luxuryGoldRim),
                        PropertyFactory.circleOpacity(0.98f),
                        PropertyFactory.circleStrokeOpacity(0.98f),
                        PropertyFactory.visibility(Property.VISIBLE)
                    )
                }
            )
        } else {
            existingCircle.setProperties(
                PropertyFactory.circleColor(coreFill),
                PropertyFactory.circleStrokeColor(luxuryGoldRim),
                PropertyFactory.circleOpacity(0.98f),
                PropertyFactory.circleStrokeOpacity(0.98f),
                PropertyFactory.visibility(Property.VISIBLE)
            )
        }

        val existingSpark = style.getLayerAs<CircleLayer>(FIRST_STRIKE_CORE_SPARK_LAYER_ID)
        if (existingSpark == null) {
            style.addLayer(
                CircleLayer(FIRST_STRIKE_CORE_SPARK_LAYER_ID, FIRST_STRIKE_SOURCE_ID).apply {
                    setProperties(
                        PropertyFactory.circleRadius(
                            Expression.interpolate(
                                Expression.linear(),
                                Expression.zoom(),
                                Expression.stop(4, 1.8f),
                                Expression.stop(10, 2.5f),
                                Expression.stop(16, 3.4f)
                            )
                        ),
                        PropertyFactory.circleColor(coreSparkColor),
                        PropertyFactory.circleOpacity(0.96f),
                        PropertyFactory.visibility(Property.VISIBLE)
                    )
                }
            )
        } else {
            existingSpark.setProperties(
                PropertyFactory.circleColor(coreSparkColor),
                PropertyFactory.circleOpacity(0.96f),
                PropertyFactory.visibility(Property.VISIBLE)
            )
        }

        val showLabels = displayMode != HeatmapDisplayMode.HEATMAP_GLOW
        val labelVisibility = if (showLabels) Property.VISIBLE else Property.NONE
        val labelColor = if (isDark) DARK_LABEL_TEXT else LIGHT_LABEL_TEXT
        val labelHalo = if (isDark) DARK_LABEL_HALO else LIGHT_LABEL_HALO
        val existingSymbol = style.getLayerAs<SymbolLayer>(SYMBOL_LAYER_ID)
        if (existingSymbol == null) {
            val symbolLayer = SymbolLayer(SYMBOL_LAYER_ID, FIRST_STRIKE_SOURCE_ID).apply {
                minZoom = 11f
                setProperties(
                    PropertyFactory.textField(Expression.get("name")),
                    PropertyFactory.textSize(12f),
                    PropertyFactory.textOffset(arrayOf(0f, 1.35f)),
                    PropertyFactory.textAnchor(Property.TEXT_ANCHOR_TOP),
                    PropertyFactory.textColor(labelColor),
                    PropertyFactory.textHaloColor(labelHalo),
                    PropertyFactory.textHaloWidth(1.5f),
                    PropertyFactory.visibility(labelVisibility)
                )
            }
            style.addLayer(symbolLayer)
        } else {
            existingSymbol.setProperties(
                PropertyFactory.textColor(labelColor),
                PropertyFactory.textHaloColor(labelHalo),
                PropertyFactory.visibility(labelVisibility)
            )
        }
    }

    private fun ensureOrUpdateRoutineStrikeLayers(
        style: Style,
        theme: MapVisualTheme,
        displayMode: HeatmapDisplayMode,
        individuals: List<Individual>,
        strikeEvents: List<StrikeEvent>
    ) {
        val geoJson = buildRoutineStrikeGeoJson(individuals, strikeEvents)
        val existingSource = style.getSourceAs<GeoJsonSource>(ROUTINE_STRIKE_SOURCE_ID)
        if (existingSource != null) {
            existingSource.setGeoJson(geoJson)
        } else {
            style.addSource(GeoJsonSource(ROUTINE_STRIKE_SOURCE_ID, geoJson))
        }

        // Translucent Snap Map / Google Maps Heatmap: map labels remain legible right through hot red zones
        val heatOpacityExpr = when (displayMode) {
            HeatmapDisplayMode.HEATMAP_GLOW -> Expression.literal(0.88f)
            HeatmapDisplayMode.CLUSTER_PINS -> Expression.literal(0f)
            HeatmapDisplayMode.HYBRID -> Expression.interpolate(
                Expression.linear(),
                Expression.zoom(),
                Expression.stop(0, 0.86f),
                Expression.stop(11, 0.84f),
                Expression.stop(16, 0.72f)
            )
        }
        val heatVisibility = if (displayMode == HeatmapDisplayMode.CLUSTER_PINS) Property.NONE else Property.VISIBLE

        val existingHeat = style.getLayerAs<HeatmapLayer>(ROUTINE_HEATMAP_LAYER_ID)
        if (existingHeat == null) {
            val routineHeatmapLayer = HeatmapLayer(ROUTINE_HEATMAP_LAYER_ID, ROUTINE_STRIKE_SOURCE_ID).apply {
                setProperties(
                    PropertyFactory.heatmapWeight(Expression.get("weight")),
                    PropertyFactory.heatmapIntensity(
                        Expression.interpolate(
                            Expression.linear(),
                            Expression.zoom(),
                            Expression.stop(0, 0.88f),
                            Expression.stop(6, 1.25f),
                            Expression.stop(10, 1.65f),
                            Expression.stop(15, 2.10f)
                        )
                    ),
                    // Wide Gaussian kernel so neighboring points merge organically into smooth green bridges & red plateaus
                    PropertyFactory.heatmapRadius(
                        Expression.interpolate(
                            Expression.linear(),
                            Expression.zoom(),
                            Expression.stop(0, 20f),
                            Expression.stop(6, 30f),
                            Expression.stop(10, 44f),
                            Expression.stop(14, 62f),
                            Expression.stop(17, 82f)
                        )
                    ),
                    // Classic translucent Spectral Heatmap matching reference image:
                    // Faint Royal Blue -> Sky Cyan -> Lime Green -> Crisp thin Yellow/Orange rim -> Broad Coral-Red plateau
                    PropertyFactory.heatmapColor(
                        Expression.interpolate(
                            Expression.linear(),
                            Expression.heatmapDensity(),
                            Expression.stop(0.00, Expression.rgba(0, 0, 0, 0.0f)),
                            Expression.stop(0.07, Expression.rgba(64, 128, 255, 0.16f)),
                            Expression.stop(0.15, Expression.rgba(65, 130, 255, 0.50f)),
                            Expression.stop(0.26, Expression.rgba(56, 222, 240, 0.68f)),
                            Expression.stop(0.40, Expression.rgba(66, 242, 82, 0.78f)),
                            Expression.stop(0.50, Expression.rgba(176, 255, 44, 0.82f)),
                            Expression.stop(0.56, Expression.rgba(255, 224, 32, 0.85f)),
                            Expression.stop(0.62, Expression.rgba(255, 138, 28, 0.87f)),
                            Expression.stop(0.68, Expression.rgba(244, 58, 52, 0.88f)),
                            Expression.stop(1.00, Expression.rgba(235, 46, 44, 0.90f))
                        )
                    ),
                    PropertyFactory.heatmapOpacity(heatOpacityExpr),
                    PropertyFactory.visibility(heatVisibility)
                )
            }
            // Place below base map SymbolLayers so city/region/street labels render crisp on top just like the reference image
            val firstBaseLabel = style.layers.firstOrNull { it is SymbolLayer && !it.id.startsWith("obsidian-") }
            if (firstBaseLabel != null) {
                style.addLayerBelow(routineHeatmapLayer, firstBaseLabel.id)
            } else {
                style.addLayer(routineHeatmapLayer)
            }
        } else {
            existingHeat.setProperties(
                PropertyFactory.heatmapOpacity(heatOpacityExpr),
                PropertyFactory.visibility(heatVisibility)
            )
        }

        val showPins = displayMode != HeatmapDisplayMode.HEATMAP_GLOW
        val circleVisibility = if (showPins) Property.VISIBLE else Property.NONE
        val isPinsMode = displayMode == HeatmapDisplayMode.CLUSTER_PINS
        val auraOpacityExpr = when (displayMode) {
            HeatmapDisplayMode.CLUSTER_PINS -> Expression.literal(0.30f)
            HeatmapDisplayMode.HYBRID -> Expression.interpolate(
                Expression.linear(),
                Expression.zoom(),
                Expression.stop(13, 0.0f),
                Expression.stop(15, 0.24f)
            )
            HeatmapDisplayMode.HEATMAP_GLOW -> Expression.literal(0.0f)
        }
        val coreOpacityExpr = when (displayMode) {
            HeatmapDisplayMode.CLUSTER_PINS -> Expression.literal(0.95f)
            HeatmapDisplayMode.HYBRID -> Expression.interpolate(
                Expression.linear(),
                Expression.zoom(),
                Expression.stop(13.5, 0.0f),
                Expression.stop(15.0, 0.88f)
            )
            HeatmapDisplayMode.HEATMAP_GLOW -> Expression.literal(0.0f)
        }
        val strokeColor = if (theme == MapVisualTheme.OBSIDIAN_DARK) DARK_LABEL_TEXT else LIGHT_LABEL_TEXT

        val existingOuterHalo = style.getLayerAs<CircleLayer>(ROUTINE_HALO_LAYER_ID)
        if (existingOuterHalo == null) {
            style.addLayer(
                CircleLayer(ROUTINE_HALO_LAYER_ID, ROUTINE_STRIKE_SOURCE_ID).apply {
                    setProperties(
                        PropertyFactory.circleRadius(
                            Expression.interpolate(
                                Expression.linear(),
                                Expression.zoom(),
                                Expression.stop(4, 14f),
                                Expression.stop(10, 20f),
                                Expression.stop(16, 28f)
                            )
                        ),
                        PropertyFactory.circleColor(AndroidColor.parseColor("#38BDF8")),
                        PropertyFactory.circleBlur(1.0f),
                        PropertyFactory.circleOpacity(auraOpacityExpr),
                        PropertyFactory.visibility(circleVisibility)
                    )
                }
            )
        } else {
            existingOuterHalo.setProperties(
                PropertyFactory.circleOpacity(auraOpacityExpr),
                PropertyFactory.visibility(circleVisibility)
            )
        }

        val existingInnerHalo = style.getLayerAs<CircleLayer>(ROUTINE_INNER_HALO_LAYER_ID)
        if (existingInnerHalo == null) {
            style.addLayer(
                CircleLayer(ROUTINE_INNER_HALO_LAYER_ID, ROUTINE_STRIKE_SOURCE_ID).apply {
                    setProperties(
                        PropertyFactory.circleRadius(
                            Expression.interpolate(
                                Expression.linear(),
                                Expression.zoom(),
                                Expression.stop(4, 9f),
                                Expression.stop(10, 13f),
                                Expression.stop(16, 18f)
                            )
                        ),
                        PropertyFactory.circleColor(AndroidColor.parseColor("#4ADE80")),
                        PropertyFactory.circleBlur(0.9f),
                        PropertyFactory.circleOpacity(auraOpacityExpr),
                        PropertyFactory.visibility(circleVisibility)
                    )
                }
            )
        } else {
            existingInnerHalo.setProperties(
                PropertyFactory.circleOpacity(auraOpacityExpr),
                PropertyFactory.visibility(circleVisibility)
            )
        }

        val existingCircle = style.getLayerAs<CircleLayer>(ROUTINE_CIRCLE_LAYER_ID)
        if (existingCircle == null) {
            style.addLayer(
                CircleLayer(ROUTINE_CIRCLE_LAYER_ID, ROUTINE_STRIKE_SOURCE_ID).apply {
                    setProperties(
                        PropertyFactory.circleRadius(
                            Expression.interpolate(
                                Expression.linear(),
                                Expression.zoom(),
                                Expression.stop(4, 4.5f),
                                Expression.stop(10, 6.5f),
                                Expression.stop(16, 9.5f)
                            )
                        ),
                        PropertyFactory.circleColor(AndroidColor.parseColor("#EF4444")),
                        PropertyFactory.circleBlur(if (isPinsMode) 0.0f else 0.25f),
                        PropertyFactory.circleStrokeWidth(if (isPinsMode) 1.8f else 0.0f),
                        PropertyFactory.circleStrokeColor(strokeColor),
                        PropertyFactory.circleOpacity(coreOpacityExpr),
                        PropertyFactory.circleStrokeOpacity(if (isPinsMode) coreOpacityExpr else Expression.literal(0.0f)),
                        PropertyFactory.visibility(circleVisibility)
                    )
                }
            )
        } else {
            existingCircle.setProperties(
                PropertyFactory.circleBlur(if (isPinsMode) 0.0f else 0.25f),
                PropertyFactory.circleStrokeWidth(if (isPinsMode) 1.8f else 0.0f),
                PropertyFactory.circleStrokeColor(strokeColor),
                PropertyFactory.circleOpacity(coreOpacityExpr),
                PropertyFactory.circleStrokeOpacity(if (isPinsMode) coreOpacityExpr else Expression.literal(0.0f)),
                PropertyFactory.visibility(circleVisibility)
            )
        }

        val labelColor = if (theme == MapVisualTheme.OBSIDIAN_DARK) {
            AndroidColor.parseColor("#86EFAC")
        } else {
            AndroidColor.parseColor("#15803D")
        }
        val labelHalo = if (theme == MapVisualTheme.OBSIDIAN_DARK) DARK_LABEL_HALO else LIGHT_LABEL_HALO
        val existingSymbol = style.getLayerAs<SymbolLayer>(ROUTINE_SYMBOL_LAYER_ID)
        if (existingSymbol == null) {
            style.addLayer(
                SymbolLayer(ROUTINE_SYMBOL_LAYER_ID, ROUTINE_STRIKE_SOURCE_ID).apply {
                    minZoom = 12f
                    setProperties(
                        PropertyFactory.textField(Expression.get("name")),
                        PropertyFactory.textSize(11.5f),
                        PropertyFactory.textOffset(arrayOf(0f, 1.45f)),
                        PropertyFactory.textAnchor(Property.TEXT_ANCHOR_TOP),
                        PropertyFactory.textColor(labelColor),
                        PropertyFactory.textHaloColor(labelHalo),
                        PropertyFactory.textHaloWidth(1.5f),
                        PropertyFactory.visibility(circleVisibility)
                    )
                }
            )
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
