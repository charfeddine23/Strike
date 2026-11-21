package com.example.ui.screens.inventory

import android.view.SoundEffectConstants
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.flame.ActiveShieldType
import com.example.data.flame.FlameManager
import com.example.data.profile.AvatarItem
import com.example.data.profile.PotionType
import com.example.data.profile.ProfileManager
import com.example.data.profile.ProfileMasterInventory
import com.example.data.profile.SkipKeyType
import com.example.data.profile.TitleItem
import com.example.ui.components.getAvatarVector
import com.example.ui.theme.DayCyan
import com.example.ui.theme.DayEmerald
import com.example.ui.theme.DayOrange
import com.example.ui.theme.DayPurple
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.PlusJakartaSans
import com.example.ui.viewmodel.IndividualViewModel
import com.example.util.HapticHelper
import kotlin.math.cos
import kotlin.math.sin

private sealed interface ShieldGridEntry {
    val id: String

    data object Luxury7Day : ShieldGridEntry {
        override val id: String = "shield_7day"
    }

    data class Manual(
        override val id: String,
        val title: String,
        val rarity: String,
        val count: Int,
        val isCurrentlyActive: Boolean,
        val accentColor: Color,
        val icon: ImageVector,
        val cardTag: String,
        val activateBtnTag: String,
        val onActivate: () -> Unit,
        val onLongPress1s: () -> Unit
    ) : ShieldGridEntry
}

private enum class PotionGridKind {
    PULSE,
    SHADOW,
    GABRIEL,
    CRYO,
    ETERNAL
}

@Composable
fun ShieldsSubPage(
    viewModel: IndividualViewModel,
    currentLang: String,
    isDarkTheme: Boolean,
    isHapticEnabled: Boolean,
    shieldActuallyActive: Boolean,
    activeShieldType: ActiveShieldType,
    remainingMillis: Long,
    shields24h: Int,
    shields48h: Int,
    shields72h: Int,
    sevenDayAutoTriggers: Int,
    onBack: () -> Unit,
    onOpenGuide: () -> Unit,
    onShowTooltip: (InventoryTooltipSpec) -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current

    val shieldEntries: List<ShieldGridEntry> = remember(
        currentLang,
        isDarkTheme,
        isHapticEnabled,
        shieldActuallyActive,
        activeShieldType,
        shields24h,
        shields48h,
        shields72h
    ) {
        listOf(
            ShieldGridEntry.Luxury7Day,
            ShieldGridEntry.Manual(
                id = "shield_24h",
                title = locText(currentLang, "درع الـ 24 ساعة", "Bouclier 24h", "24h Protective Shield"),
                rarity = locText(currentLang, "شائع • يدوي", "COMMUN • MANUEL", "COMMON • MANUAL"),
                count = shields24h,
                isCurrentlyActive = shieldActuallyActive && activeShieldType == ActiveShieldType.SHIELD_24H,
                accentColor = if (isDarkTheme) NeonCyan else DayCyan,
                icon = Icons.Default.Shield,
                cardTag = "inv_shield_24h",
                activateBtnTag = "inv_activate_shield_24h",
                onActivate = {
                    HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                    view.playSoundEffect(SoundEffectConstants.CLICK)
                    viewModel.activate1DayShield()
                },
                onLongPress1s = { onShowTooltip(InventoryTooltipFactory.for24hShield(currentLang)) }
            ),
            ShieldGridEntry.Manual(
                id = "shield_48h",
                title = locText(currentLang, "درع الـ 48 ساعة", "Bouclier 48h", "48h Protective Shield"),
                rarity = locText(currentLang, "غير شائع • يدوي", "PEU COMMUN • MANUEL", "UNCOMMON • MANUAL"),
                count = shields48h,
                isCurrentlyActive = shieldActuallyActive && activeShieldType == ActiveShieldType.SHIELD_48H,
                accentColor = if (isDarkTheme) NeonEmerald else DayEmerald,
                icon = Icons.Default.Shield,
                cardTag = "inv_shield_48h",
                activateBtnTag = "inv_activate_shield_48h",
                onActivate = {
                    HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                    view.playSoundEffect(SoundEffectConstants.CLICK)
                    viewModel.activate48hShield()
                },
                onLongPress1s = { onShowTooltip(InventoryTooltipFactory.for48hShield(currentLang)) }
            ),
            ShieldGridEntry.Manual(
                id = "shield_72h",
                title = locText(currentLang, "درع الـ 72 ساعة", "Bouclier 72h", "72h Protective Shield"),
                rarity = locText(currentLang, "نادر • يدوي", "RARE • MANUEL", "RARE • MANUAL"),
                count = shields72h,
                isCurrentlyActive = shieldActuallyActive && activeShieldType == ActiveShieldType.SHIELD_72H,
                accentColor = if (isDarkTheme) NeonAmber else DayOrange,
                icon = Icons.Default.VerifiedUser,
                cardTag = "inv_shield_72h",
                activateBtnTag = "inv_activate_shield_72h",
                onActivate = {
                    HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                    view.playSoundEffect(SoundEffectConstants.CLICK)
                    viewModel.activateManualShield()
                },
                onLongPress1s = { onShowTooltip(InventoryTooltipFactory.for72hShield(currentLang)) }
            )
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("inv_subpage_shields"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SubPageTopBar(
                title = locText(currentLang, "الدروع", "Boucliers", "Shields"),
                subtitle = if (shieldActuallyActive) {
                    val label = if (currentLang == "ar") activeShieldType.labelAr else activeShieldType.labelEn
                    locText(
                        currentLang,
                        "نشط: $label (${FlameManager.formatRemaining(remainingMillis)})",
                        "Actif : $label (${FlameManager.formatRemaining(remainingMillis)})",
                        "Active: $label (${FlameManager.formatRemaining(remainingMillis)})"
                    )
                } else {
                    locText(
                        currentLang,
                        "جميع الدروع والكميات المتوفرة",
                        "Tous les boucliers et quantités",
                        "All shields & available quantities"
                    )
                },
                accentColor = if (isDarkTheme) NeonEmerald else DayEmerald,
                icon = Icons.Default.Shield,
                currentLang = currentLang,
                isDarkTheme = isDarkTheme,
                isHapticEnabled = isHapticEnabled,
                onBack = onBack,
                onOpenGuide = onOpenGuide
            )
        }

        items(
            items = shieldEntries.chunked(2),
            key = { row -> row.joinToString("_") { it.id } }
        ) { rowEntries ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowEntries.forEach { entry ->
                    when (entry) {
                        ShieldGridEntry.Luxury7Day -> {
                            val is7dActive = shieldActuallyActive && activeShieldType == ActiveShieldType.SHIELD_7D_AUTO
                            Luxury7DayAutoShieldCard(
                                currentLang = currentLang,
                                isActiveNow = is7dActive,
                                remainingText = if (is7dActive) FlameManager.formatRemaining(remainingMillis) else null,
                                totalTriggeredCount = sevenDayAutoTriggers,
                                onLongPress1s = { onShowTooltip(InventoryTooltipFactory.for7DayAutoShield(currentLang)) },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                        }

                        is ShieldGridEntry.Manual -> {
                            InventoryShieldCard(
                                title = entry.title,
                                rarity = entry.rarity,
                                currentLang = currentLang,
                                isDarkTheme = isDarkTheme,
                                count = entry.count,
                                isCurrentlyActive = entry.isCurrentlyActive,
                                accentColor = entry.accentColor,
                                icon = entry.icon,
                                onActivate = entry.onActivate,
                                onLongPress1s = entry.onLongPress1s,
                                cardTag = entry.cardTag,
                                activateBtnTag = entry.activateBtnTag,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                        }
                    }
                }
                if (rowEntries.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        item { Spacer(modifier = Modifier.height(72.dp)) }
    }
}

@Composable
fun KeysSubPage(
    currentLang: String,
    isDarkTheme: Boolean,
    isHapticEnabled: Boolean,
    woodenKeys: Int,
    bronzeKeys: Int,
    silverKeys: Int,
    onBack: () -> Unit,
    onOpenGuide: () -> Unit,
    onShowTooltip: (InventoryTooltipSpec) -> Unit
) {
    val ownedKeys = remember(woodenKeys, bronzeKeys, silverKeys) {
        buildList {
            if (woodenKeys > 0) add(SkipKeyType.WOODEN to woodenKeys)
            if (bronzeKeys > 0) add(SkipKeyType.BRONZE to bronzeKeys)
            if (silverKeys > 0) add(SkipKeyType.SILVER to silverKeys)
        }
    }
    val totalKeys = woodenKeys + bronzeKeys + silverKeys

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("inv_subpage_keys"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SubPageTopBar(
                title = locText(currentLang, "المفاتيح", "Clés", "Keys"),
                subtitle = locText(
                    currentLang,
                    "المفاتيح المتوفرة لديك: $totalKeys",
                    "Clés possédées : $totalKeys",
                    "Owned Keys: $totalKeys"
                ),
                accentColor = if (isDarkTheme) NeonAmber else DayOrange,
                icon = Icons.Default.VpnKey,
                currentLang = currentLang,
                isDarkTheme = isDarkTheme,
                isHapticEnabled = isHapticEnabled,
                onBack = onBack,
                onOpenGuide = onOpenGuide
            )
        }

        if (ownedKeys.isEmpty()) {
            item {
                EmptyInventoryCategoryCard(
                    icon = Icons.Default.VpnKey,
                    title = locText(
                        currentLang,
                        "لا توجد مفاتيح في مخزونك حالياً",
                        "Aucune clé dans votre inventaire",
                        "No Keys in Your Inventory Yet"
                    ),
                    subtitle = locText(
                        currentLang,
                        "احصد مفاتيح التجاوز من محطات خارطة الطريق لتظهر هنا.",
                        "Gagnez des clés via les stations de la Feuille de route pour les voir ici.",
                        "Earn Skip-Keys from Roadmap stations to see them listed here."
                    ),
                    accentColor = if (isDarkTheme) NeonAmber else DayOrange,
                    isDarkTheme = isDarkTheme,
                    testTag = "inv_keys_empty_state"
                )
            }
        } else {
            items(
                items = ownedKeys.chunked(2),
                key = { row -> row.joinToString("_") { it.first.id } }
            ) { rowKeys ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowKeys.forEach { (type, count) ->
                        val (rarity, usesLabel, accent, tag) = when (type) {
                            SkipKeyType.WOODEN -> Quad(
                                locText(currentLang, "شائع", "COMMUN", "COMMON"),
                                locText(currentLang, "استخدام واحد لكل مفتاح", "1 Utilisation / clé", "1 Use per key"),
                                if (isDarkTheme) Color(0xFFD97706) else Color(0xFFB45309),
                                "inv_key_wooden"
                            )
                            SkipKeyType.BRONZE -> Quad(
                                locText(currentLang, "غير شائع", "PEU COMMUN", "UNCOMMON"),
                                locText(currentLang, "استخدامان لكل مفتاح", "2 Utilisations / clé", "2 Uses per key"),
                                if (isDarkTheme) Color(0xFFF59E0B) else DayOrange,
                                "inv_key_bronze"
                            )
                            SkipKeyType.SILVER -> Quad(
                                locText(currentLang, "نادر", "RARE", "RARE"),
                                locText(currentLang, "3 استخدامات لكل مفتاح", "3 Utilisations / clé", "3 Uses per key"),
                                if (isDarkTheme) Color(0xFFE2E8F0) else Color(0xFF475569),
                                "inv_key_silver"
                            )
                        }

                        InventoryKeyCard(
                            title = type.getLocalizedName(currentLang),
                            rarity = rarity,
                            usesLabel = usesLabel,
                            count = count,
                            accentColor = accent,
                            isDarkTheme = isDarkTheme,
                            onLongPress1s = { onShowTooltip(InventoryTooltipFactory.forSkipKey(type, currentLang)) },
                            testTag = tag,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                    if (rowKeys.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(72.dp)) }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun PotionsSubPage(
    currentLang: String,
    isDarkTheme: Boolean,
    isHapticEnabled: Boolean,
    pulseElixirCount: Int,
    pulseUsesLeft: Int,
    shadowSerumCount: Int,
    isShadowActive: Boolean,
    gabrielDropletCount: Int,
    isGabrielActive: Boolean,
    cryoInfusionCount: Int,
    hasCryoArmed: Boolean,
    eternalRebirthCount: Int,
    hasRebirthImmunity: Boolean,
    onBack: () -> Unit,
    onOpenGuide: () -> Unit,
    onShowTooltip: (InventoryTooltipSpec) -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current

    val showPulse = pulseElixirCount > 0 || pulseUsesLeft > 0
    val showShadow = shadowSerumCount > 0 || isShadowActive
    val showGabriel = gabrielDropletCount > 0 || isGabrielActive
    val showCryo = cryoInfusionCount > 0 || hasCryoArmed
    val showEternal = eternalRebirthCount > 0 || hasRebirthImmunity

    val visiblePotions = remember(showPulse, showShadow, showGabriel, showCryo, showEternal) {
        buildList {
            if (showPulse) add(PotionGridKind.PULSE)
            if (showShadow) add(PotionGridKind.SHADOW)
            if (showGabriel) add(PotionGridKind.GABRIEL)
            if (showCryo) add(PotionGridKind.CRYO)
            if (showEternal) add(PotionGridKind.ETERNAL)
        }
    }
    val ownedTotal = pulseElixirCount + shadowSerumCount + gabrielDropletCount + cryoInfusionCount + eternalRebirthCount

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("inv_subpage_potions"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SubPageTopBar(
                title = locText(currentLang, "العقاقير", "Potions", "Potions"),
                subtitle = locText(
                    currentLang,
                    "العقاقير المتوفرة: $ownedTotal",
                    "Potions disponibles : $ownedTotal",
                    "Owned Potions: $ownedTotal"
                ),
                accentColor = if (isDarkTheme) NeonCyan else DayCyan,
                icon = Icons.Default.Science,
                currentLang = currentLang,
                isDarkTheme = isDarkTheme,
                isHapticEnabled = isHapticEnabled,
                onBack = onBack,
                onOpenGuide = onOpenGuide
            )
        }

        if (visiblePotions.isEmpty()) {
            item {
                EmptyInventoryCategoryCard(
                    icon = Icons.Default.Science,
                    title = locText(
                        currentLang,
                        "لا توجد عقاقير في مخزونك حالياً",
                        "Aucune potion dans votre inventaire",
                        "No Potions in Your Inventory Yet"
                    ),
                    subtitle = locText(
                        currentLang,
                        "افتح المحطات في خارطة الطريق لكسب العقاقير والجرعات السيبرانية.",
                        "Débloquez des stations dans la Feuille de route pour gagner des potions.",
                        "Unlock Roadmap stations to earn cybernetic potions and activate them here."
                    ),
                    accentColor = if (isDarkTheme) NeonCyan else DayCyan,
                    isDarkTheme = isDarkTheme,
                    testTag = "inv_potions_empty_state"
                )
            }
        } else {
            items(
                items = visiblePotions.chunked(2),
                key = { row -> row.joinToString("_") { it.name } }
            ) { rowPotions ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowPotions.forEach { kind ->
                        when (kind) {
                            PotionGridKind.PULSE -> InventoryPotionCard(
                                title = PotionType.PULSE_ELIXIR.getLocalizedName(currentLang),
                                subtitle = locText(currentLang, "الفئة الأولى", "Niveau 1", "Tier 1"),
                                rarity = locText(currentLang, "شائع", "COMMON", "COMMON"),
                                currentLang = currentLang,
                                isDarkTheme = isDarkTheme,
                                count = pulseElixirCount,
                                statusBadge = if (pulseUsesLeft > 0) {
                                    locText(
                                        currentLang,
                                        "نشط ($pulseUsesLeft استخدامات)",
                                        "Actif ($pulseUsesLeft util.)",
                                        "Active ($pulseUsesLeft uses)"
                                    )
                                } else null,
                                accentColor = PotionType.PULSE_ELIXIR.adaptiveColor(isDarkTheme),
                                icon = Icons.Default.Bolt,
                                canActivate = pulseElixirCount > 0,
                                onActivate = {
                                    HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                                    view.playSoundEffect(SoundEffectConstants.CLICK)
                                    ProfileManager.usePulseElixir(context)
                                },
                                onLongPress1s = {
                                    onShowTooltip(InventoryTooltipFactory.forPotion(PotionType.PULSE_ELIXIR, currentLang))
                                },
                                cardTag = "inv_potion_pulse",
                                activateBtnTag = "inv_activate_potion_pulse",
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )

                            PotionGridKind.SHADOW -> InventoryPotionCard(
                                title = PotionType.SHADOW_GRIP_SERUM.getLocalizedName(currentLang),
                                subtitle = locText(currentLang, "الفئة الثانية", "Niveau 2", "Tier 2"),
                                rarity = locText(currentLang, "غير شائع", "UNCOMMON", "UNCOMMON"),
                                currentLang = currentLang,
                                isDarkTheme = isDarkTheme,
                                count = shadowSerumCount,
                                statusBadge = if (isShadowActive) {
                                    locText(
                                        currentLang,
                                        "نشط (تبطيء 50%)",
                                        "Actif (-50 %)",
                                        "Active (50% Slow)"
                                    )
                                } else null,
                                accentColor = PotionType.SHADOW_GRIP_SERUM.adaptiveColor(isDarkTheme),
                                icon = Icons.Default.Science,
                                canActivate = shadowSerumCount > 0 && !isShadowActive,
                                onActivate = {
                                    HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                                    view.playSoundEffect(SoundEffectConstants.CLICK)
                                    ProfileManager.useShadowSerum(context)
                                },
                                onLongPress1s = {
                                    onShowTooltip(InventoryTooltipFactory.forPotion(PotionType.SHADOW_GRIP_SERUM, currentLang))
                                },
                                cardTag = "inv_potion_shadow",
                                activateBtnTag = "inv_activate_potion_shadow",
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )

                            PotionGridKind.GABRIEL -> InventoryPotionCard(
                                title = PotionType.GABRIEL_PURITY_DROPLET.getLocalizedName(currentLang),
                                subtitle = locText(currentLang, "الفئة الثالثة", "Niveau 3", "Tier 3"),
                                rarity = locText(currentLang, "نادر", "RARE", "RARE"),
                                currentLang = currentLang,
                                isDarkTheme = isDarkTheme,
                                count = gabrielDropletCount,
                                statusBadge = if (isGabrielActive) {
                                    locText(
                                        currentLang,
                                        "نشط تلقائياً (+50%)",
                                        "Actif Auto (+50 %)",
                                        "Auto-Active (+50%)"
                                    )
                                } else null,
                                accentColor = PotionType.GABRIEL_PURITY_DROPLET.adaptiveColor(isDarkTheme),
                                icon = Icons.Default.WaterDrop,
                                canActivate = false,
                                onActivate = {},
                                onLongPress1s = {
                                    onShowTooltip(InventoryTooltipFactory.forPotion(PotionType.GABRIEL_PURITY_DROPLET, currentLang))
                                },
                                cardTag = "inv_potion_gabriel",
                                activateBtnTag = "inv_activate_potion_gabriel",
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )

                            PotionGridKind.CRYO -> InventoryPotionCard(
                                title = PotionType.ABSOLUTE_CRYO_INFUSION.getLocalizedName(currentLang),
                                subtitle = locText(currentLang, "الفئة الرابعة", "Niveau 4", "Tier 4"),
                                rarity = locText(currentLang, "ملحمي", "EPIC", "EPIC"),
                                currentLang = currentLang,
                                isDarkTheme = isDarkTheme,
                                count = cryoInfusionCount,
                                statusBadge = if (hasCryoArmed) {
                                    locText(
                                        currentLang,
                                        "مسلّح (+100)",
                                        "Armé (+100)",
                                        "Armed (+100)"
                                    )
                                } else null,
                                accentColor = PotionType.ABSOLUTE_CRYO_INFUSION.adaptiveColor(isDarkTheme),
                                icon = Icons.Default.AcUnit,
                                canActivate = cryoInfusionCount > 0 && !hasCryoArmed,
                                onActivate = {
                                    HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                                    view.playSoundEffect(SoundEffectConstants.CLICK)
                                    ProfileManager.useCryoInfusion(context)
                                },
                                onLongPress1s = {
                                    onShowTooltip(InventoryTooltipFactory.forPotion(PotionType.ABSOLUTE_CRYO_INFUSION, currentLang))
                                },
                                cardTag = "inv_potion_cryo",
                                activateBtnTag = "inv_activate_potion_cryo",
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )

                            PotionGridKind.ETERNAL -> EternalResurgenceLegendaryCard(
                                currentLang = currentLang,
                                count = eternalRebirthCount,
                                isActiveImmunity = hasRebirthImmunity,
                                onActivate = {
                                    HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                                    view.playSoundEffect(SoundEffectConstants.CLICK)
                                    ProfileManager.useEternalRebirth(context)
                                },
                                onLongPress1s = {
                                    onShowTooltip(InventoryTooltipFactory.forPotion(PotionType.ETERNAL_REBIRTH_ELIXIR, currentLang))
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                        }
                    }
                    if (rowPotions.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(72.dp)) }
    }
}

@Composable
fun TitlesSubPage(
    currentLang: String,
    isDarkTheme: Boolean,
    isHapticEnabled: Boolean,
    unlockedTitleIds: Set<Int>,
    equippedTitle: TitleItem,
    onBack: () -> Unit,
    onOpenGuide: () -> Unit,
    onShowTooltip: (InventoryTooltipSpec) -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val allGameTitles = remember {
        ProfileMasterInventory.MASTER_TITLES + ProfileMasterInventory.MILESTONE_PEAK_TITLE
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("inv_subpage_titles"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SubPageTopBar(
                title = locText(currentLang, "الألقاب", "Titres", "Titles"),
                subtitle = locText(
                    currentLang,
                    "المتوفر: ${unlockedTitleIds.size} / ${allGameTitles.size} • المفعّل: ${equippedTitle.getLocalizedTitle(currentLang)}",
                    "Débloqués : ${unlockedTitleIds.size} / ${allGameTitles.size} • Actif : ${equippedTitle.getLocalizedTitle(currentLang)}",
                    "Unlocked: ${unlockedTitleIds.size} / ${allGameTitles.size} • Active: ${equippedTitle.getLocalizedTitle(currentLang)}"
                ),
                accentColor = if (isDarkTheme) NeonViolet else DayPurple,
                icon = Icons.Default.MilitaryTech,
                currentLang = currentLang,
                isDarkTheme = isDarkTheme,
                isHapticEnabled = isHapticEnabled,
                onBack = onBack,
                onOpenGuide = onOpenGuide
            )
        }

        items(
            items = allGameTitles.chunked(2),
            key = { row -> row.joinToString("_") { it.id.toString() } }
        ) { rowTitles ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowTitles.forEach { titleItem ->
                    val isUnlocked = unlockedTitleIds.contains(titleItem.id)
                    val isEquipped = equippedTitle.id == titleItem.id

                    InventoryTitleCard(
                        title = titleItem,
                        currentLang = currentLang,
                        isDarkTheme = isDarkTheme,
                        isUnlocked = isUnlocked,
                        isEquipped = isEquipped,
                        onActivate = {
                            HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                            view.playSoundEffect(SoundEffectConstants.CLICK)
                            ProfileManager.equipTitle(context, titleItem.id)
                        },
                        onLongPress1s = {
                            onShowTooltip(InventoryTooltipFactory.forTitle(titleItem, currentLang, isUnlocked))
                        },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
                if (rowTitles.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        item { Spacer(modifier = Modifier.height(72.dp)) }
    }
}

@Composable
private fun Luxury7DayAutoShieldCard(
    currentLang: String,
    isActiveNow: Boolean,
    remainingText: String?,
    totalTriggeredCount: Int,
    onLongPress1s: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "luxury_7d_shield_anim")
    val animAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep_angle"
    )
    val pulseWave by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1350, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_wave"
    )
    val goldColor = Color(0xFFFFD700)
    val magentaGlow = Color(0xFFFF007A)
    val rad = Math.toRadians(animAngle.toDouble())
    val dx = cos(rad).toFloat()
    val dy = sin(rad).toFloat()

    val luxuryBorder = Brush.linearGradient(
        colors = listOf(goldColor, magentaGlow, NeonCyan, goldColor),
        start = Offset(200f * (1f - dx), 100f * (1f - dy)),
        end = Offset(200f * (1f + dx), 100f * (1f + dy))
    )

    LongPressTooltipContainer(onLongPress1s = onLongPress1s, modifier = modifier) { isHolding, progress ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .heightIn(min = 186.dp)
                .clip(RoundedCornerShape(20.dp))
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                goldColor.copy(alpha = 0.24f * pulseWave),
                                magentaGlow.copy(alpha = 0.14f * pulseWave),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.8f, size.height * 0.2f),
                            radius = size.width * 0.72f
                        )
                    )
                }
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xE81A1025),
                            Color(0xE81E170A),
                            Color(0xE80F172A)
                        )
                    )
                )
                .border(1.8.dp, luxuryBorder, RoundedCornerShape(20.dp))
                .testTag("inv_shield_7day_luxury")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(13.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(goldColor.copy(alpha = 0.35f), magentaGlow.copy(alpha = 0.18f))
                                )
                            )
                            .border(1.6.dp, luxuryBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = locText(currentLang, "درع الـ 7 أيام التلقائي", "Bouclier Auto 7j", "7-Day Auto Shield"),
                            tint = goldColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(goldColor.copy(alpha = 0.18f))
                            .border(1.dp, goldColor.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "×$totalTriggeredCount",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = goldColor,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Column {
                    Text(
                        text = locText(currentLang, "درع الـ 7 أيام التلقائي", "Bouclier Auto 7j", "7-Day Auto Shield"),
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = PlusJakartaSans,
                        color = goldColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 17.sp
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = locText(
                            currentLang,
                            "نخبوي تلقائي • 3–5/يوم",
                            "ÉLITE AUTO • 3–5/Jour",
                            "LUXURY AUTO • 3–5/Day"
                        ),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFDA4AF),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isActiveNow && remainingText != null) {
                            locText(
                                currentLang,
                                "متبقي: $remainingText",
                                "Reste : $remainingText",
                                "Left: $remainingText"
                            )
                        } else {
                            locText(
                                currentLang,
                                "تفعيل تلقائي عند 3–5 سترايكات",
                                "Auto dès 3–5 strikes/jour",
                                "Triggers on 3–5 daily strikes"
                            )
                        },
                        fontSize = 10.5.sp,
                        color = if (isActiveNow) NeonEmerald else Color(0xFFE2E8F0),
                        fontWeight = if (isActiveNow) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isActiveNow) NeonEmerald.copy(alpha = 0.22f)
                            else goldColor.copy(alpha = 0.16f)
                        )
                        .border(
                            1.dp,
                            if (isActiveNow) NeonEmerald else goldColor.copy(alpha = 0.7f),
                            RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isActiveNow) {
                            locText(currentLang, "مفعّل الآن", "Actif", "Active Now")
                        } else {
                            locText(currentLang, "تفعيل تلقائي", "Déclenchement Auto", "Auto Trigger")
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isActiveNow) NeonEmerald else goldColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (isHolding) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth(progress)
                        .height(3.dp)
                        .background(goldColor)
                )
            }
        }
    }
}

@Composable
private fun InventoryShieldCard(
    title: String,
    rarity: String,
    currentLang: String,
    isDarkTheme: Boolean,
    count: Int,
    isCurrentlyActive: Boolean,
    accentColor: Color,
    icon: ImageVector,
    onActivate: () -> Unit,
    onLongPress1s: () -> Unit,
    cardTag: String,
    activateBtnTag: String,
    modifier: Modifier = Modifier
) {
    val activeEmerald = if (isDarkTheme) NeonEmerald else DayEmerald
    val buttonContentLuminance = accentColor.red * 0.299f + accentColor.green * 0.587f + accentColor.blue * 0.114f
    val buttonContentColor = if (buttonContentLuminance > 0.55f) Color(0xFF03141F) else Color.White

    LongPressTooltipContainer(onLongPress1s = onLongPress1s, modifier = modifier) { isHolding, progress ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .heightIn(min = 186.dp)
                .clip(RoundedCornerShape(20.dp))
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(accentColor.copy(alpha = 0.16f), Color.Transparent),
                            center = Offset(size.width * 0.85f, size.height * 0.15f),
                            radius = size.width * 0.65f
                        )
                    )
                }
                .background(cardGlassBackground(isDarkTheme))
                .border(
                    width = if (isCurrentlyActive) 1.8.dp else 1.2.dp,
                    color = if (isCurrentlyActive) activeEmerald else accentColor.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(20.dp)
                )
                .testTag(cardTag)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(13.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.18f))
                            .border(1.2.dp, accentColor.copy(alpha = 0.8f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = accentColor,
                            modifier = Modifier.size(21.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(accentColor.copy(alpha = 0.16f))
                            .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "×$count",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = accentColor,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Column {
                    Text(
                        text = title,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = cardPrimaryTextColor(isDarkTheme),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 17.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = rarity,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isCurrentlyActive) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = locText(currentLang, "• نشط الآن", "• Actif", "• Active Now"),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = activeEmerald,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onActivate,
                    enabled = count > 0,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = buttonContentColor,
                        disabledContainerColor = accentColor.copy(alpha = 0.16f),
                        disabledContentColor = cardSecondaryTextColor(isDarkTheme)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .height(38.dp)
                        .testTag(activateBtnTag)
                ) {
                    Text(
                        locText(currentLang, "تفعيل", "Activer", "Activate"),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1
                    )
                }
            }

            if (isHolding) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth(progress)
                        .height(3.dp)
                        .background(accentColor)
                )
            }
        }
    }
}

@Composable
private fun InventoryKeyCard(
    title: String,
    rarity: String,
    usesLabel: String,
    count: Int,
    accentColor: Color,
    isDarkTheme: Boolean,
    onLongPress1s: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    LongPressTooltipContainer(onLongPress1s = onLongPress1s, modifier = modifier) { isHolding, progress ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .heightIn(min = 152.dp)
                .clip(RoundedCornerShape(20.dp))
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(accentColor.copy(alpha = 0.16f), Color.Transparent),
                            center = Offset(size.width * 0.85f, size.height * 0.18f),
                            radius = size.width * 0.65f
                        )
                    )
                }
                .background(cardGlassBackground(isDarkTheme))
                .border(1.3.dp, accentColor.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                .testTag(testTag)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(13.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.2f))
                            .border(1.3.dp, accentColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VpnKey,
                            contentDescription = title,
                            tint = accentColor,
                            modifier = Modifier.size(21.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(9.dp))
                            .background(accentColor.copy(alpha = 0.16f))
                            .border(1.dp, accentColor.copy(alpha = 0.55f), RoundedCornerShape(9.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "$count / 30",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Black,
                            color = accentColor,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Column {
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = cardPrimaryTextColor(isDarkTheme),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = rarity,
                        fontSize = 10.sp,
                        color = accentColor,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = usesLabel,
                        fontSize = 10.5.sp,
                        color = cardSecondaryTextColor(isDarkTheme),
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 14.sp
                    )
                }
            }

            if (isHolding) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth(progress)
                        .height(3.dp)
                        .background(accentColor)
                )
            }
        }
    }
}

@Composable
private fun InventoryPotionCard(
    title: String,
    subtitle: String,
    rarity: String,
    currentLang: String,
    isDarkTheme: Boolean,
    count: Int,
    statusBadge: String?,
    accentColor: Color,
    icon: ImageVector,
    canActivate: Boolean,
    onActivate: () -> Unit,
    onLongPress1s: () -> Unit,
    cardTag: String,
    activateBtnTag: String,
    modifier: Modifier = Modifier
) {
    val activeEmerald = if (isDarkTheme) NeonEmerald else DayEmerald
    val buttonContentLuminance = accentColor.red * 0.299f + accentColor.green * 0.587f + accentColor.blue * 0.114f
    val buttonContentColor = if (buttonContentLuminance > 0.55f) Color(0xFF03141F) else Color.White

    LongPressTooltipContainer(onLongPress1s = onLongPress1s, modifier = modifier) { isHolding, progress ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .heightIn(min = 186.dp)
                .clip(RoundedCornerShape(20.dp))
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(accentColor.copy(alpha = 0.16f), Color.Transparent),
                            center = Offset(size.width * 0.85f, size.height * 0.16f),
                            radius = size.width * 0.68f
                        )
                    )
                }
                .background(cardGlassBackground(isDarkTheme))
                .border(1.2.dp, accentColor.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
                .testTag(cardTag)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(13.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.2f))
                            .border(1.3.dp, accentColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = accentColor,
                            modifier = Modifier.size(21.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(accentColor.copy(alpha = 0.16f))
                            .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "$count / 3",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Black,
                            color = accentColor,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Column {
                    Text(
                        text = title,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = cardPrimaryTextColor(isDarkTheme),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 17.sp
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "$subtitle • $rarity",
                        fontSize = 10.sp,
                        color = accentColor,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!statusBadge.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = statusBadge,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = activeEmerald,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (canActivate) {
                    Button(
                        onClick = onActivate,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor,
                            contentColor = buttonContentColor
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .height(38.dp)
                            .testTag(activateBtnTag)
                    ) {
                        Text(
                            locText(currentLang, "تفعيل", "Activer", "Activate"),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (!statusBadge.isNullOrBlank()) activeEmerald.copy(alpha = 0.14f)
                                else accentColor.copy(alpha = 0.10f)
                            )
                            .border(
                                1.dp,
                                if (!statusBadge.isNullOrBlank()) activeEmerald.copy(alpha = 0.5f)
                                else accentColor.copy(alpha = 0.3f),
                                RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = statusBadge ?: locText(currentLang, "جاهز", "Prêt", "Ready"),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (!statusBadge.isNullOrBlank()) activeEmerald else accentColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (isHolding) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth(progress)
                        .height(3.dp)
                        .background(accentColor)
                )
            }
        }
    }
}

@Composable
private fun EternalResurgenceLegendaryCard(
    currentLang: String,
    count: Int,
    isActiveImmunity: Boolean,
    onActivate: () -> Unit,
    onLongPress1s: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "eternal_potion_anim")
    val animAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep_angle"
    )
    val pulseWave by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1350, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_wave"
    )
    val crimsonMagenta = Color(0xFFFF007A)
    val gold = Color(0xFFFFD700)
    val violet = Color(0xFFD946EF)

    val rad = Math.toRadians(animAngle.toDouble())
    val dx = cos(rad).toFloat()
    val dy = sin(rad).toFloat()

    val ultraLuxuryBrush = Brush.linearGradient(
        colors = listOf(crimsonMagenta, gold, violet, crimsonMagenta),
        start = Offset(220f * (1f - dx), 120f * (1f - dy)),
        end = Offset(220f * (1f + dx), 120f * (1f + dy))
    )

    LongPressTooltipContainer(onLongPress1s = onLongPress1s, modifier = modifier) { isHolding, progress ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .heightIn(min = 186.dp)
                .clip(RoundedCornerShape(20.dp))
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                crimsonMagenta.copy(alpha = 0.38f * pulseWave),
                                gold.copy(alpha = 0.20f * pulseWave),
                                Color.Transparent
                            ),
                            center = center,
                            radius = size.width * 0.72f
                        )
                    )
                }
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xEE2B0921),
                            Color(0xEE18081C),
                            Color(0xEE0C1021)
                        )
                    )
                )
                .border(2.dp, ultraLuxuryBrush, RoundedCornerShape(20.dp))
                .testTag("inv_potion_eternal_resurgence")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(13.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .scale(0.96f + pulseWave * 0.05f)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(crimsonMagenta.copy(alpha = 0.45f), gold.copy(alpha = 0.25f))
                                )
                            )
                            .border(1.8.dp, ultraLuxuryBrush, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = PotionType.ETERNAL_REBIRTH_ELIXIR.getLocalizedName(currentLang),
                            tint = gold,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(crimsonMagenta.copy(alpha = 0.25f))
                            .border(1.dp, gold.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isActiveImmunity) {
                                locText(currentLang, "أبدي ✓", "Éternel ✓", "Eternal ✓")
                            } else "$count / 3",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = gold,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Column {
                    Text(
                        text = PotionType.ETERNAL_REBIRTH_ELIXIR.getLocalizedName(currentLang),
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = PlusJakartaSans,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 17.sp
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = locText(currentLang, "أسطوري • الفئة 5", "LÉGENDAIRE 5", "LEGENDARY TIER 5"),
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = gold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = PotionType.ETERNAL_REBIRTH_ELIXIR.getLocalizedDescription(currentLang),
                        fontSize = 10.sp,
                        color = Color(0xFFFBCFE8),
                        lineHeight = 14.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (count > 0 && !isActiveImmunity) {
                    Button(
                        onClick = onActivate,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .height(38.dp)
                            .testTag("inv_activate_potion_eternal"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = crimsonMagenta,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            locText(currentLang, "تفعيل الأبدي", "Activer Éternel", "Activate"),
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(gold.copy(alpha = 0.16f))
                            .border(1.dp, gold.copy(alpha = 0.7f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isActiveImmunity) {
                                locText(currentLang, "أبدي مفعّل ✓", "Éternel Actif ✓", "Eternal Active ✓")
                            } else {
                                locText(currentLang, "مناعة أبدية", "Immunité", "Immunity")
                            },
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Black,
                            color = gold,
                            maxLines = 1
                        )
                    }
                }
            }

            if (isHolding) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth(progress)
                        .height(3.dp)
                        .background(gold)
                )
            }
        }
    }
}

@Composable
private fun InventoryTitleCard(
    title: TitleItem,
    currentLang: String,
    isDarkTheme: Boolean,
    isUnlocked: Boolean,
    isEquipped: Boolean,
    onActivate: () -> Unit,
    onLongPress1s: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = title.rarity.adaptiveColor(isDarkTheme)
    val activeEmerald = if (isDarkTheme) NeonEmerald else DayEmerald
    val buttonContentLuminance = accentColor.red * 0.299f + accentColor.green * 0.587f + accentColor.blue * 0.114f
    val buttonContentColor = if (buttonContentLuminance > 0.55f) Color(0xFF03141F) else Color.White
    val rarityLabel = when (currentLang) {
        "ar" -> title.rarity.labelAr
        else -> title.rarity.labelEn
    }
    val statusText = when {
        isEquipped -> locText(currentLang, "مفعّل بملفك", "Équipé", "Equipped")
        isUnlocked -> locText(currentLang, "جاهز للتفعيل", "Prêt à activer", "Ready to equip")
        title.id == ProfileMasterInventory.MILESTONE_PEAK_TITLE.id -> locText(
            currentLang,
            "20,000 نقطة شعلة",
            "20 000 pts Flamme",
            "20,000 Flame Score"
        )
        title.unlockStation != null -> locText(
            currentLang,
            "المحطة #${title.unlockStation}",
            "Station #${title.unlockStation}",
            "Station #${title.unlockStation}"
        )
        else -> locText(currentLang, "مقفل", "Verrouillé", "Locked")
    }

    LongPressTooltipContainer(onLongPress1s = onLongPress1s, modifier = modifier) { isHolding, progress ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .heightIn(min = 172.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(
                    if (isEquipped) accentColor.copy(alpha = if (isDarkTheme) 0.18f else 0.12f)
                    else cardGlassBackground(isDarkTheme)
                )
                .border(
                    width = if (isEquipped) 1.8.dp else 1.1.dp,
                    color = when {
                        isEquipped -> accentColor
                        isUnlocked -> accentColor.copy(alpha = 0.55f)
                        else -> Color(0x4464748B)
                    },
                    shape = RoundedCornerShape(18.dp)
                )
                .testTag("inv_title_card_${title.id}")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                if (isUnlocked) accentColor.copy(alpha = 0.18f)
                                else Color(0x2264748B)
                            )
                            .border(
                                1.2.dp,
                                if (isUnlocked) accentColor else Color(0x6664748B),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isUnlocked) Icons.Default.MilitaryTech else Icons.Default.Lock,
                            contentDescription = title.getLocalizedTitle(currentLang),
                            tint = if (isUnlocked) accentColor else Color(0xFF64748B),
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(7.dp))
                            .background(accentColor.copy(alpha = if (isUnlocked) 0.18f else 0.08f))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = rarityLabel,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isUnlocked) accentColor else Color(0xFF64748B),
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Column {
                    Text(
                        text = title.getLocalizedTitle(currentLang),
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isUnlocked) cardPrimaryTextColor(isDarkTheme) else Color(0xFF64748B),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 17.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = statusText,
                        fontSize = 10.5.sp,
                        fontWeight = if (isEquipped) FontWeight.Bold else FontWeight.Medium,
                        color = when {
                            isEquipped -> accentColor
                            isUnlocked -> activeEmerald
                            else -> cardSecondaryTextColor(isDarkTheme)
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                when {
                    isEquipped -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(accentColor.copy(alpha = 0.22f))
                                .border(1.2.dp, accentColor, RoundedCornerShape(10.dp))
                                .testTag("inv_title_active_${title.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = locText(currentLang, "مفعّل", "Actif", "Active"),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = accentColor,
                                maxLines = 1
                            )
                        }
                    }

                    isUnlocked -> {
                        Button(
                            onClick = onActivate,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accentColor,
                                contentColor = buttonContentColor
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .height(38.dp)
                                .testTag("inv_activate_title_${title.id}")
                        ) {
                            Text(
                                text = locText(currentLang, "تفعيل", "Activer", "Activate"),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1
                            )
                        }
                    }

                    else -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0x1864748B))
                                .border(1.dp, Color(0x4464748B), RoundedCornerShape(10.dp))
                                .testTag("inv_title_locked_${title.id}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = locText(currentLang, "مقفل", "Verrouillé", "Locked"),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF64748B),
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            if (isHolding) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth(progress)
                        .height(3.dp)
                        .background(accentColor)
                )
            }
        }
    }
}

@Composable
fun AvatarsSubPage(
    currentLang: String,
    isDarkTheme: Boolean,
    isHapticEnabled: Boolean,
    unlockedAvatarIds: Set<String>,
    equippedAvatar: AvatarItem,
    onBack: () -> Unit,
    onOpenGuide: () -> Unit,
    onShowTooltip: (InventoryTooltipSpec) -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val allGameAvatars = ProfileMasterInventory.MASTER_AVATARS

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("inv_subpage_avatars"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SubPageTopBar(
                title = locText(currentLang, "الأفاتار", "Avatars", "Avatars"),
                subtitle = locText(
                    currentLang,
                    "المتوفر: ${unlockedAvatarIds.size} / ${allGameAvatars.size} • المفعّل: ${equippedAvatar.getLocalizedName(currentLang)}",
                    "Débloqués : ${unlockedAvatarIds.size} / ${allGameAvatars.size} • Actif : ${equippedAvatar.getLocalizedName(currentLang)}",
                    "Unlocked: ${unlockedAvatarIds.size} / ${allGameAvatars.size} • Active: ${equippedAvatar.getLocalizedName(currentLang)}"
                ),
                accentColor = Color(0xFFEC4899),
                icon = getAvatarVector(equippedAvatar.badgeIconName),
                currentLang = currentLang,
                isDarkTheme = isDarkTheme,
                isHapticEnabled = isHapticEnabled,
                onBack = onBack,
                onOpenGuide = onOpenGuide
            )
        }

        items(
            items = allGameAvatars.chunked(2),
            key = { row -> row.joinToString("_") { it.id } }
        ) { rowAvatars ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowAvatars.forEach { avatar ->
                    val isUnlocked = unlockedAvatarIds.contains(avatar.id)
                    val isEquipped = equippedAvatar.id == avatar.id

                    InventoryAvatarCard(
                        avatar = avatar,
                        currentLang = currentLang,
                        isDarkTheme = isDarkTheme,
                        isUnlocked = isUnlocked,
                        isEquipped = isEquipped,
                        onActivate = {
                            HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                            view.playSoundEffect(SoundEffectConstants.CLICK)
                            ProfileManager.equipAvatar(context, avatar.id)
                        },
                        onLongPress1s = {
                            onShowTooltip(InventoryTooltipFactory.forAvatar(avatar, currentLang, isUnlocked))
                        },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
                if (rowAvatars.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        item { Spacer(modifier = Modifier.height(72.dp)) }
    }
}

@Composable
private fun InventoryAvatarCard(
    avatar: AvatarItem,
    currentLang: String,
    isDarkTheme: Boolean,
    isUnlocked: Boolean,
    isEquipped: Boolean,
    onActivate: () -> Unit,
    onLongPress1s: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = avatar.rarity.adaptiveColor(isDarkTheme)
    val activeEmerald = if (isDarkTheme) NeonEmerald else DayEmerald
    val buttonContentLuminance = accentColor.red * 0.299f + accentColor.green * 0.587f + accentColor.blue * 0.114f
    val buttonContentColor = if (buttonContentLuminance > 0.55f) Color(0xFF03141F) else Color.White
    val rarityLabel = avatar.rarity.getLocalizedLabel(currentLang)
    val statusText = when {
        isEquipped -> locText(currentLang, "مفعّل بملفك", "Équipé", "Equipped")
        isUnlocked -> locText(currentLang, "جاهز للتفعيل", "Prêt à activer", "Ready to equip")
        else -> locText(
            currentLang,
            "المحطة #${avatar.unlockStation}",
            "Station #${avatar.unlockStation}",
            "Station #${avatar.unlockStation}"
        )
    }

    LongPressTooltipContainer(onLongPress1s = onLongPress1s, modifier = modifier) { isHolding, progress ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .heightIn(min = 172.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(
                    if (isEquipped) accentColor.copy(alpha = if (isDarkTheme) 0.18f else 0.12f)
                    else cardGlassBackground(isDarkTheme)
                )
                .border(
                    width = if (isEquipped) 1.8.dp else 1.1.dp,
                    color = when {
                        isEquipped -> accentColor
                        isUnlocked -> accentColor.copy(alpha = 0.55f)
                        else -> Color(0x4464748B)
                    },
                    shape = RoundedCornerShape(18.dp)
                )
                .testTag("inv_avatar_card_${avatar.id}")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                if (isUnlocked) accentColor.copy(alpha = 0.18f)
                                else Color(0x2264748B)
                            )
                            .border(
                                1.3.dp,
                                if (isUnlocked) accentColor else Color(0x6664748B),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getAvatarVector(avatar.badgeIconName),
                            contentDescription = avatar.getLocalizedName(currentLang),
                            tint = if (isUnlocked) accentColor else Color(0xFF64748B),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(7.dp))
                            .background(accentColor.copy(alpha = if (isUnlocked) 0.18f else 0.08f))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = rarityLabel,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isUnlocked) accentColor else Color(0xFF64748B),
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Column {
                    Text(
                        text = avatar.getLocalizedName(currentLang),
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isUnlocked) cardPrimaryTextColor(isDarkTheme) else Color(0xFF64748B),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 17.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = statusText,
                        fontSize = 10.5.sp,
                        fontWeight = if (isEquipped) FontWeight.Bold else FontWeight.Medium,
                        color = when {
                            isEquipped -> accentColor
                            isUnlocked -> activeEmerald
                            else -> cardSecondaryTextColor(isDarkTheme)
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                when {
                    isEquipped -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(accentColor.copy(alpha = 0.22f))
                                .border(1.2.dp, accentColor, RoundedCornerShape(10.dp))
                                .testTag("inv_avatar_active_${avatar.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = locText(currentLang, "مفعّل", "Actif", "Active"),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = accentColor,
                                maxLines = 1
                            )
                        }
                    }

                    isUnlocked -> {
                        Button(
                            onClick = onActivate,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accentColor,
                                contentColor = buttonContentColor
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .height(38.dp)
                                .testTag("inv_activate_avatar_${avatar.id}")
                        ) {
                            Text(
                                text = locText(currentLang, "تفعيل", "Activer", "Activate"),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1
                            )
                        }
                    }

                    else -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0x1864748B))
                                .border(1.dp, Color(0x4464748B), RoundedCornerShape(10.dp))
                                .testTag("inv_avatar_locked_${avatar.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = locText(currentLang, "مقفل", "Verrouillé", "Locked"),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF64748B),
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            if (isHolding) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth(progress)
                        .height(3.dp)
                        .background(accentColor)
                )
            }
        }
    }
}
