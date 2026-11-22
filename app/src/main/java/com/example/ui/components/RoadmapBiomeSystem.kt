package com.example.ui.components

import androidx.compose.ui.graphics.Color
import com.example.data.roadmap.TaskPools

/**
 * The 4 progressive atmospheric stages across Levels 1..700:
 * 1. GLACIAL  (1..175)   — Starts Level 1 deeply submerged in ice & frost
 * 2. ROCKY    (176..350) — Canyon slate, basalt strata, amber mineral veins
 * 3. FOREST   (351..525) — Bioluminescent emerald canopy, vines & fireflies
 * 4. VOLCANIC (526..700) — Cataclysm beginning fading into molten magma submersion at 700
 */
enum class RoadmapBiome {
    GLACIAL,
    ROCKY,
    FOREST,
    VOLCANIC
}

data class RoadmapBiomeState(
    val continuousStation: Float,
    val dominantBiome: RoadmapBiome,
    val iceWeight: Float,
    val rockWeight: Float,
    val forestWeight: Float,
    val volcanicWeight: Float,
    val iceSubmersion: Float,
    val rockIntensity: Float,
    val forestIntensity: Float,
    val lavaSubmersion: Float,
    val cataclysmFactor: Float
)

// ==========================================
// 4-STAGE NIGHT & DAY BIOME COLOR PALETTES
// ==========================================

// 1. Glacial Ice Stage
val GlacialNightTop = Color(0xFF03101D)
val GlacialNightMid = Color(0xFF061E34)
val GlacialNightBottom = Color(0xFF0B2D4C)
val GlacialNightCyan = Color(0xFF38BDF8)
val GlacialNightIce = Color(0xFFBAE6FD)
val GlacialNightCrystal = Color(0xFFE0F2FE)
val GlacialNightAurora = Color(0xFF22D3EE)

val GlacialDayTop = Color(0xFFF0F9FF)
val GlacialDayMid = Color(0xFFDDF1FE)
val GlacialDayBottom = Color(0xFFC3E6FD)
val GlacialDayCyan = Color(0xFF0284C7)
val GlacialDayDeepBlue = Color(0xFF0369A1)
val GlacialDayIceBorder = Color(0xFF7DD3FC)
val GlacialDayText = Color(0xFF0C1E38)

// 2. Rocky / Stone Stage
val RockyNightTop = Color(0xFF131116)
val RockyNightMid = Color(0xFF1C1820)
val RockyNightBottom = Color(0xFF28201B)
val RockyNightAmber = Color(0xFFF59E0B)
val RockyNightSlate = Color(0xFFCBD5E1)
val RockyNightOchre = Color(0xFFD97706)

val RockyDayTop = Color(0xFFFAF6F0)
val RockyDayMid = Color(0xFFF1E7D8)
val RockyDayBottom = Color(0xFFE5D4BF)
val RockyDayAmber = Color(0xFFB45309)
val RockyDaySlate = Color(0xFF475569)
val RockyDayText = Color(0xFF292018)

// 3. Forest / Jungle Stage
val ForestNightTop = Color(0xFF04130E)
val ForestNightMid = Color(0xFF082218)
val ForestNightBottom = Color(0xFF0D3021)
val ForestNightEmerald = Color(0xFF10B981)
val ForestNightLime = Color(0xFF84CC16)
val ForestNightMint = Color(0xFF6EE7B7)

val ForestDayTop = Color(0xFFF0FDF4)
val ForestDayMid = Color(0xFFDCFCE7)
val ForestDayBottom = Color(0xFFBBF7D0)
val ForestDayEmerald = Color(0xFF047857)
val ForestDayLeaf = Color(0xFF15803D)
val ForestDayText = Color(0xFF052E16)

// 4. Cataclysm & Volcanic Lava Stage
val VolcanicNightTop = Color(0xFF120506)
val VolcanicNightMid = Color(0xFF240908)
val VolcanicNightBottom = Color(0xFF3E0D06)
val VolcanicNightMagma = Color(0xFFFF4500)
val VolcanicNightGold = Color(0xFFFFB703)
val VolcanicNightCrimson = Color(0xFFEF4444)

val VolcanicDayTop = Color(0xFFFFF5F0)
val VolcanicDayMid = Color(0xFFFFE2D4)
val VolcanicDayBottom = Color(0xFFFFC7B0)
val VolcanicDayMagma = Color(0xFFDC2626)
val VolcanicDayOrange = Color(0xFFEA580C)
val VolcanicDayText = Color(0xFF3B0A05)

const val STAGE_1_END = 175f
const val STAGE_2_END = 350f
const val STAGE_3_END = 525f
private const val TRANSITION_HALF_BAND = 28f

private fun smoothstep01(x: Float): Float {
    val t = x.coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

/**
 * Computes C1-smooth normalized biome weights and submersion intensities across Levels 1..700:
 * - Level 1 starts DEEPLY submerged in Glacial ice (`iceSubmersion = 1.0f`).
 * - Smoothly morphs across ~56-station transition bands around 175.5, 350.5, and 525.5.
 * - Stage 4 starts at 526 as the "Cataclysm Beginning" and intensifies until Level 700 is
 *   DEEPLY submerged in molten volcanic lava (`lavaSubmersion = 1.0f`).
 */
fun roadmapBiomeStateForStation(continuousStation: Float): RoadmapBiomeState {
    val maxS = TaskPools.TOTAL_STATIONS_COUNT.toFloat().coerceAtLeast(700f)
    val s = continuousStation.coerceIn(1f, maxS)

    // Smoothstep cross-fades across the 3 stage boundaries
    val boundary12 = STAGE_1_END + 0.5f // 175.5
    val boundary23 = STAGE_2_END + 0.5f // 350.5
    val boundary34 = STAGE_3_END + 0.5f // 525.5

    val t12 = smoothstep01((s - (boundary12 - TRANSITION_HALF_BAND)) / (2f * TRANSITION_HALF_BAND))
    val t23 = smoothstep01((s - (boundary23 - TRANSITION_HALF_BAND)) / (2f * TRANSITION_HALF_BAND))
    val t34 = smoothstep01((s - (boundary34 - TRANSITION_HALF_BAND)) / (2f * TRANSITION_HALF_BAND))

    val iceW = (1f - t12).coerceIn(0f, 1f)
    val rockW = (t12 * (1f - t23)).coerceIn(0f, 1f)
    val forestW = (t23 * (1f - t34)).coerceIn(0f, 1f)
    val volcanicW = t34.coerceIn(0f, 1f)
    val total = (iceW + rockW + forestW + volcanicW).coerceAtLeast(0.0001f)

    val normIce = iceW / total
    val normRock = rockW / total
    val normForest = forestW / total
    val normVolcanic = volcanicW / total

    // Level 1 = 1.0f deeply submerged in ice -> tapers smoothly to ~0.48 before Rocky transition
    val earlyGlacialDepth = (1f - ((s - 1f) / STAGE_1_END).coerceIn(0f, 1f) * 0.52f)
    val iceSubmersion = (normIce * earlyGlacialDepth).coerceIn(0f, 1f)

    // Cataclysm ramps smoothly from ~497 through 526 (Cataclysm Start) up to 700 (Lava Abyss)
    val cataclysmSpan = (maxS - (boundary34 - TRANSITION_HALF_BAND)).coerceAtLeast(1f)
    val cataclysmRaw = smoothstep01((s - (boundary34 - TRANSITION_HALF_BAND)) / cataclysmSpan)
    // Strongest magma immersion ramps sharply across 526..700 to 1.0f at Level 700
    val volcanoSpan = (maxS - STAGE_3_END).coerceAtLeast(1f)
    val volcanoCoreRamp = smoothstep01((s - STAGE_3_END) / volcanoSpan)
    val lavaSubmersion = (0.18f * normVolcanic + 0.82f * volcanoCoreRamp * normVolcanic).coerceIn(0f, 1f)

    val dominant = when {
        normIce >= normRock && normIce >= normForest && normIce >= normVolcanic -> RoadmapBiome.GLACIAL
        normRock >= normForest && normRock >= normVolcanic -> RoadmapBiome.ROCKY
        normForest >= normVolcanic -> RoadmapBiome.FOREST
        else -> RoadmapBiome.VOLCANIC
    }

    return RoadmapBiomeState(
        continuousStation = s,
        dominantBiome = dominant,
        iceWeight = normIce,
        rockWeight = normRock,
        forestWeight = normForest,
        volcanicWeight = normVolcanic,
        iceSubmersion = iceSubmersion,
        rockIntensity = normRock,
        forestIntensity = normForest,
        lavaSubmersion = lavaSubmersion,
        cataclysmFactor = cataclysmRaw
    )
}

/**
 * Legacy helper kept for compatibility: returns glacial ice submersion `[0f..1f]` at [stationId].
 */
fun glacialDepthForStation(stationId: Int): Float =
    roadmapBiomeStateForStation(stationId.toFloat()).iceSubmersion

fun blendBiomeColor(
    glacial: Color,
    rocky: Color,
    forest: Color,
    volcanic: Color,
    state: RoadmapBiomeState
): Color {
    val r = (glacial.red * state.iceWeight +
        rocky.red * state.rockWeight +
        forest.red * state.forestWeight +
        volcanic.red * state.volcanicWeight).coerceIn(0f, 1f)
    val g = (glacial.green * state.iceWeight +
        rocky.green * state.rockWeight +
        forest.green * state.forestWeight +
        volcanic.green * state.volcanicWeight).coerceIn(0f, 1f)
    val b = (glacial.blue * state.iceWeight +
        rocky.blue * state.rockWeight +
        forest.blue * state.forestWeight +
        volcanic.blue * state.volcanicWeight).coerceIn(0f, 1f)
    val a = (glacial.alpha * state.iceWeight +
        rocky.alpha * state.rockWeight +
        forest.alpha * state.forestWeight +
        volcanic.alpha * state.volcanicWeight).coerceIn(0f, 1f)
    return Color(red = r, green = g, blue = b, alpha = a)
}

fun biomePrimaryAccent(state: RoadmapBiomeState, isDarkTheme: Boolean): Color {
    return if (isDarkTheme) {
        blendBiomeColor(
            glacial = GlacialNightCyan,
            rocky = RockyNightAmber,
            forest = ForestNightEmerald,
            volcanic = VolcanicNightMagma,
            state = state
        )
    } else {
        blendBiomeColor(
            glacial = GlacialDayCyan,
            rocky = RockyDayAmber,
            forest = ForestDayEmerald,
            volcanic = VolcanicDayMagma,
            state = state
        )
    }
}

fun biomeSecondaryAccent(state: RoadmapBiomeState, isDarkTheme: Boolean): Color {
    return if (isDarkTheme) {
        blendBiomeColor(
            glacial = GlacialNightAurora,
            rocky = RockyNightSlate,
            forest = ForestNightLime,
            volcanic = VolcanicNightGold,
            state = state
        )
    } else {
        blendBiomeColor(
            glacial = GlacialDayDeepBlue,
            rocky = RockyDaySlate,
            forest = ForestDayLeaf,
            volcanic = VolcanicDayOrange,
            state = state
        )
    }
}
