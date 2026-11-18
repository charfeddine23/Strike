package com.example.ui.screens.inventory

import android.view.SoundEffectConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.PlusJakartaSans
import com.example.ui.viewmodel.IndividualViewModel
import com.example.util.HapticHelper
import kotlin.math.cos
import kotlin.math.sin

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
    animAngle: Float,
    pulseWave: Float,
    onBack: () -> Unit,
    onOpenGuide: () -> Unit,
    onShowTooltip: (InventoryTooltipSpec) -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current

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
                accentColor = NeonEmerald,
                icon = Icons.Default.Shield,
                currentLang = currentLang,
                isDarkTheme = isDarkTheme,
                isHapticEnabled = isHapticEnabled,
                onBack = onBack,
                onOpenGuide = onOpenGuide
            )
        }

        // 1. Luxury 7-Day Automated Strike Shield (Auto trigger)
        item {
            val is7dActive = shieldActuallyActive && activeShieldType == ActiveShieldType.SHIELD_7D_AUTO
            Luxury7DayAutoShieldCard(
                currentLang = currentLang,
                isActiveNow = is7dActive,
                remainingText = if (is7dActive) FlameManager.formatRemaining(remainingMillis) else null,
                totalTriggeredCount = sevenDayAutoTriggers,
                animAngle = animAngle,
                pulseWave = pulseWave,
                onLongPress1s = { onShowTooltip(InventoryTooltipFactory.for7DayAutoShield(currentLang)) }
            )
        }

        // 2. 24h Shield (Manual activation)
        item {
            InventoryShieldCard(
                title = locText(currentLang, "درع الـ 24 ساعة", "Bouclier 24h", "24h Protective Shield"),
                rarity = locText(currentLang, "شائع • تفعيل يدوي", "COMMUN • MANUEL", "COMMON • MANUAL"),
                currentLang = currentLang,
                isDarkTheme = isDarkTheme,
                count = shields24h,
                isCurrentlyActive = shieldActuallyActive && activeShieldType == ActiveShieldType.SHIELD_24H,
                accentColor = NeonCyan,
                icon = Icons.Default.Shield,
                onActivate = {
                    HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                    view.playSoundEffect(SoundEffectConstants.CLICK)
                    viewModel.activate1DayShield()
                },
                onLongPress1s = { onShowTooltip(InventoryTooltipFactory.for24hShield(currentLang)) },
                cardTag = "inv_shield_24h",
                activateBtnTag = "inv_activate_shield_24h"
            )
        }

        // 3. 48h Shield (Manual activation)
        item {
            InventoryShieldCard(
                title = locText(currentLang, "درع الـ 48 ساعة", "Bouclier 48h", "48h Protective Shield"),
                rarity = locText(currentLang, "غير شائع • تفعيل يدوي", "PEU COMMUN • MANUEL", "UNCOMMON • MANUAL"),
                currentLang = currentLang,
                isDarkTheme = isDarkTheme,
                count = shields48h,
                isCurrentlyActive = shieldActuallyActive && activeShieldType == ActiveShieldType.SHIELD_48H,
                accentColor = NeonEmerald,
                icon = Icons.Default.Shield,
                onActivate = {
                    HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                    view.playSoundEffect(SoundEffectConstants.CLICK)
                    viewModel.activate48hShield()
                },
                onLongPress1s = { onShowTooltip(InventoryTooltipFactory.for48hShield(currentLang)) },
                cardTag = "inv_shield_48h",
                activateBtnTag = "inv_activate_shield_48h"
            )
        }

        // 4. 72h Shield (Manual activation)
        item {
            InventoryShieldCard(
                title = locText(currentLang, "درع الـ 72 ساعة", "Bouclier 72h", "72h Protective Shield"),
                rarity = locText(currentLang, "نادر • تفعيل يدوي", "RARE • MANUEL", "RARE • MANUAL"),
                currentLang = currentLang,
                isDarkTheme = isDarkTheme,
                count = shields72h,
                isCurrentlyActive = shieldActuallyActive && activeShieldType == ActiveShieldType.SHIELD_72H,
                accentColor = NeonAmber,
                icon = Icons.Default.VerifiedUser,
                onActivate = {
                    HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                    view.playSoundEffect(SoundEffectConstants.CLICK)
                    viewModel.activateManualShield()
                },
                onLongPress1s = { onShowTooltip(InventoryTooltipFactory.for72hShield(currentLang)) },
                cardTag = "inv_shield_72h",
                activateBtnTag = "inv_activate_shield_72h"
            )
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
                accentColor = NeonAmber,
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
                    accentColor = NeonAmber,
                    isDarkTheme = isDarkTheme,
                    testTag = "inv_keys_empty_state"
                )
            }
        } else {
            items(ownedKeys, key = { it.first.id }) { (type, count) ->
                val (rarity, usesLabel, accent, tag) = when (type) {
                    SkipKeyType.WOODEN -> Quad(
                        locText(currentLang, "شائع", "COMMUN", "COMMON"),
                        locText(currentLang, "استخدام واحد لكل مفتاح", "1 Utilisation / clé", "1 Use per key"),
                        Color(0xFFD97706),
                        "inv_key_wooden"
                    )
                    SkipKeyType.BRONZE -> Quad(
                        locText(currentLang, "غير شائع", "PEU COMMUN", "UNCOMMON"),
                        locText(currentLang, "استخدامان لكل مفتاح", "2 Utilisations / clé", "2 Uses per key"),
                        Color(0xFFF59E0B),
                        "inv_key_bronze"
                    )
                    SkipKeyType.SILVER -> Quad(
                        locText(currentLang, "نادر", "RARE", "RARE"),
                        locText(currentLang, "3 استخدامات لكل مفتاح", "3 Utilisations / clé", "3 Uses per key"),
                        Color(0xFFE2E8F0),
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
                    testTag = tag
                )
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
    animAngle: Float,
    pulseWave: Float,
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

    val anyVisible = showPulse || showShadow || showGabriel || showCryo || showEternal
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
                accentColor = NeonCyan,
                icon = Icons.Default.Science,
                currentLang = currentLang,
                isDarkTheme = isDarkTheme,
                isHapticEnabled = isHapticEnabled,
                onBack = onBack,
                onOpenGuide = onOpenGuide
            )
        }

        if (!anyVisible) {
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
                    accentColor = NeonCyan,
                    isDarkTheme = isDarkTheme,
                    testTag = "inv_potions_empty_state"
                )
            }
        } else {
            if (showPulse) {
                item {
                    InventoryPotionCard(
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
                                "Actif ($pulseUsesLeft utilisations)",
                                "Active ($pulseUsesLeft uses)"
                            )
                        } else null,
                        accentColor = PotionType.PULSE_ELIXIR.color,
                        icon = Icons.Default.Bolt,
                        canActivate = pulseElixirCount > 0,
                        onActivate = {
                            HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                            view.playSoundEffect(SoundEffectConstants.CLICK)
                            ProfileManager.usePulseElixir(context)
                        },
                        onLongPress1s = { onShowTooltip(InventoryTooltipFactory.forPotion(PotionType.PULSE_ELIXIR, currentLang)) },
                        cardTag = "inv_potion_pulse",
                        activateBtnTag = "inv_activate_potion_pulse"
                    )
                }
            }

            if (showShadow) {
                item {
                    InventoryPotionCard(
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
                                "Actif (-50 % rythme)",
                                "Active (50% Slowdown)"
                            )
                        } else null,
                        accentColor = PotionType.SHADOW_GRIP_SERUM.color,
                        icon = Icons.Default.Science,
                        canActivate = shadowSerumCount > 0 && !isShadowActive,
                        onActivate = {
                            HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                            view.playSoundEffect(SoundEffectConstants.CLICK)
                            ProfileManager.useShadowSerum(context)
                        },
                        onLongPress1s = { onShowTooltip(InventoryTooltipFactory.forPotion(PotionType.SHADOW_GRIP_SERUM, currentLang)) },
                        cardTag = "inv_potion_shadow",
                        activateBtnTag = "inv_activate_potion_shadow"
                    )
                }
            }

            if (showGabriel) {
                item {
                    InventoryPotionCard(
                        title = PotionType.GABRIEL_PURITY_DROPLET.getLocalizedName(currentLang),
                        subtitle = locText(currentLang, "الفئة الثالثة", "Niveau 3", "Tier 3"),
                        rarity = locText(currentLang, "نادر", "RARE", "RARE"),
                        currentLang = currentLang,
                        isDarkTheme = isDarkTheme,
                        count = gabrielDropletCount,
                        statusBadge = if (isGabrielActive) {
                            locText(
                                currentLang,
                                "نشط تلقائياً (+50% نمو)",
                                "Actif Auto (+50 % croissance)",
                                "Auto-Active (+50% Growth)"
                            )
                        } else null,
                        accentColor = PotionType.GABRIEL_PURITY_DROPLET.color,
                        icon = Icons.Default.WaterDrop,
                        canActivate = false,
                        onActivate = {},
                        onLongPress1s = { onShowTooltip(InventoryTooltipFactory.forPotion(PotionType.GABRIEL_PURITY_DROPLET, currentLang)) },
                        cardTag = "inv_potion_gabriel",
                        activateBtnTag = "inv_activate_potion_gabriel"
                    )
                }
            }

            if (showCryo) {
                item {
                    InventoryPotionCard(
                        title = PotionType.ABSOLUTE_CRYO_INFUSION.getLocalizedName(currentLang),
                        subtitle = locText(currentLang, "الفئة الرابعة", "Niveau 4", "Tier 4"),
                        rarity = locText(currentLang, "ملحمي", "EPIC", "EPIC"),
                        currentLang = currentLang,
                        isDarkTheme = isDarkTheme,
                        count = cryoInfusionCount,
                        statusBadge = if (hasCryoArmed) {
                            locText(
                                currentLang,
                                "مسلّح للستريك القادم (+100)",
                                "Armé pour prochain strike (+100)",
                                "Armed for Next Strike (+100)"
                            )
                        } else null,
                        accentColor = PotionType.ABSOLUTE_CRYO_INFUSION.color,
                        icon = Icons.Default.AcUnit,
                        canActivate = cryoInfusionCount > 0 && !hasCryoArmed,
                        onActivate = {
                            HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                            view.playSoundEffect(SoundEffectConstants.CLICK)
                            ProfileManager.useCryoInfusion(context)
                        },
                        onLongPress1s = { onShowTooltip(InventoryTooltipFactory.forPotion(PotionType.ABSOLUTE_CRYO_INFUSION, currentLang)) },
                        cardTag = "inv_potion_cryo",
                        activateBtnTag = "inv_activate_potion_cryo"
                    )
                }
            }

            if (showEternal) {
                item {
                    EternalResurgenceLegendaryCard(
                        currentLang = currentLang,
                        count = eternalRebirthCount,
                        isActiveImmunity = hasRebirthImmunity,
                        animAngle = animAngle,
                        pulseWave = pulseWave,
                        onActivate = {
                            HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                            view.playSoundEffect(SoundEffectConstants.CLICK)
                            ProfileManager.useEternalRebirth(context)
                        },
                        onLongPress1s = { onShowTooltip(InventoryTooltipFactory.forPotion(PotionType.ETERNAL_REBIRTH_ELIXIR, currentLang)) }
                    )
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
        verticalArrangement = Arrangement.spacedBy(10.dp)
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
                accentColor = NeonViolet,
                icon = Icons.Default.MilitaryTech,
                currentLang = currentLang,
                isDarkTheme = isDarkTheme,
                isHapticEnabled = isHapticEnabled,
                onBack = onBack,
                onOpenGuide = onOpenGuide
            )
        }

        items(allGameTitles, key = { it.id }) { titleItem ->
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
                }
            )
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
    animAngle: Float,
    pulseWave: Float,
    onLongPress1s: () -> Unit
) {
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

    LongPressTooltipContainer(onLongPress1s = onLongPress1s) { isHolding, progress ->
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                goldColor.copy(alpha = 0.24f * pulseWave),
                                magentaGlow.copy(alpha = 0.14f * pulseWave),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.8f, size.height * 0.25f),
                            radius = size.width * 0.65f
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
                .border(2.dp, luxuryBorder, RoundedCornerShape(22.dp))
                .testTag("inv_shield_7day_luxury")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(goldColor.copy(alpha = 0.35f), magentaGlow.copy(alpha = 0.18f))
                                    )
                                )
                                .border(1.8.dp, luxuryBorder, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = locText(currentLang, "درع الـ 7 أيام التلقائي", "Bouclier Auto 7j", "7-Day Auto Shield"),
                                tint = goldColor,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = locText(currentLang, "درع الـ 7 أيام التلقائي", "Bouclier Automatique 7 Jours", "7-Day Automated Shield"),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = PlusJakartaSans,
                                color = goldColor
                            )
                            Text(
                                text = locText(
                                    currentLang,
                                    "فئة نخبوية تلقائية • 3-5 سترايكات يومياً",
                                    "NIVEAU ÉLITE AUTO • 3-5 Strikes/Jour",
                                    "LUXURY AUTO TIER • 3-5 Strikes/Day"
                                ),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFDA4AF)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isActiveNow) NeonEmerald.copy(alpha = 0.25f)
                                else goldColor.copy(alpha = 0.18f)
                            )
                            .border(
                                1.dp,
                                if (isActiveNow) NeonEmerald else goldColor.copy(alpha = 0.7f),
                                RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = if (isActiveNow) {
                                locText(currentLang, "مفعّل الآن", "Actif", "Active Now")
                            } else {
                                locText(currentLang, "تفعيل تلقائي", "Déclenchement Auto", "Auto Trigger")
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isActiveNow) NeonEmerald else goldColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isActiveNow && remainingText != null) {
                            locText(
                                currentLang,
                                "العد التنازلي المتبقي: $remainingText",
                                "Temps restant : $remainingText",
                                "Remaining countdown: $remainingText"
                            )
                        } else {
                            locText(
                                currentLang,
                                "يُمنح ويُفعّل تلقائياً عند تسجيل 3 إلى 5 ضربات في يوم واحد",
                                "Activé automatiquement dès 3 à 5 strikes en une journée",
                                "Granted & activated automatically upon recording 3–5 strikes in a single day"
                            )
                        },
                        fontSize = 11.sp,
                        color = if (isActiveNow) NeonEmerald else Color(0xFFE2E8F0),
                        fontWeight = if (isActiveNow) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = locText(
                            currentLang,
                            "الكمية / التفعيل: $totalTriggeredCount",
                            "Déclenchements : $totalTriggeredCount",
                            "Qty / Triggers: $totalTriggeredCount"
                        ),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = goldColor
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
    activateBtnTag: String
) {
    LongPressTooltipContainer(onLongPress1s = onLongPress1s) { isHolding, progress ->
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(cardGlassBackground(isDarkTheme))
                .border(
                    width = if (isCurrentlyActive) 1.8.dp else 1.2.dp,
                    color = if (isCurrentlyActive) NeonEmerald else accentColor.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(18.dp)
                )
                .testTag(cardTag)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.18f))
                            .border(1.2.dp, accentColor.copy(alpha = 0.8f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = accentColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = cardPrimaryTextColor(isDarkTheme)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = rarity,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = accentColor
                            )
                            if (isCurrentlyActive) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = locText(currentLang, "• نشط", "• Actif", "• Active"),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = NeonEmerald
                                )
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(accentColor.copy(alpha = 0.16f))
                            .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "×$count",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = accentColor
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = onActivate,
                        enabled = count > 0,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor,
                            contentColor = Color(0xFF03141F),
                            disabledContainerColor = accentColor.copy(alpha = 0.16f),
                            disabledContentColor = cardSecondaryTextColor(isDarkTheme)
                        ),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .height(40.dp)
                            .testTag(activateBtnTag)
                    ) {
                        Text(
                            locText(currentLang, "تفعيل", "Activer", "Activate"),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
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
private fun InventoryKeyCard(
    title: String,
    rarity: String,
    usesLabel: String,
    count: Int,
    accentColor: Color,
    isDarkTheme: Boolean,
    onLongPress1s: () -> Unit,
    testTag: String
) {
    LongPressTooltipContainer(onLongPress1s = onLongPress1s) { isHolding, progress ->
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(cardGlassBackground(isDarkTheme))
                .border(1.3.dp, accentColor.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
                .testTag(testTag)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.2f))
                            .border(1.3.dp, accentColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VpnKey,
                            contentDescription = title,
                            tint = accentColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = cardPrimaryTextColor(isDarkTheme)
                        )
                        Text(
                            text = "$usesLabel • $rarity",
                            fontSize = 11.sp,
                            color = accentColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(accentColor.copy(alpha = 0.16f))
                        .border(1.dp, accentColor.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "$count / 30",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = accentColor
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
    activateBtnTag: String
) {
    LongPressTooltipContainer(onLongPress1s = onLongPress1s) { isHolding, progress ->
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(accentColor.copy(alpha = 0.14f), Color.Transparent),
                            center = Offset(48f, size.height / 2f),
                            radius = 120f
                        )
                    )
                }
                .background(cardGlassBackground(isDarkTheme))
                .border(1.2.dp, accentColor.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
                .testTag(cardTag)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.2f))
                            .border(1.3.dp, accentColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = accentColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = cardPrimaryTextColor(isDarkTheme)
                        )
                        Text(
                            text = "$subtitle • $rarity",
                            fontSize = 10.sp,
                            color = accentColor,
                            fontWeight = FontWeight.Bold
                        )
                        if (!statusBadge.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = statusBadge,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonEmerald
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(accentColor.copy(alpha = 0.16f))
                            .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "$count / 3",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = accentColor
                        )
                    }

                    if (canActivate) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onActivate,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accentColor,
                                contentColor = Color(0xFF03141F)
                            ),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .height(40.dp)
                                .testTag(activateBtnTag)
                        ) {
                            Text(
                                locText(currentLang, "تفعيل", "Activer", "Activate"),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold
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
private fun EternalResurgenceLegendaryCard(
    currentLang: String,
    count: Int,
    isActiveImmunity: Boolean,
    animAngle: Float,
    pulseWave: Float,
    onActivate: () -> Unit,
    onLongPress1s: () -> Unit
) {
    val crimsonMagenta = Color(0xFFFF007A)
    val gold = Color(0xFFFFD700)
    val violet = Color(0xFFD946EF)

    val rad = Math.toRadians(animAngle.toDouble())
    val dx = cos(rad).toFloat()
    val dy = sin(rad).toFloat()

    val ultraLuxuryBrush = Brush.linearGradient(
        colors = listOf(crimsonMagenta, gold, violet, crimsonMagenta),
        start = Offset(300f * (1f - dx), 140f * (1f - dy)),
        end = Offset(300f * (1f + dx), 140f * (1f + dy))
    )

    LongPressTooltipContainer(onLongPress1s = onLongPress1s) { isHolding, progress ->
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
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
                .border(2.2.dp, ultraLuxuryBrush, RoundedCornerShape(24.dp))
                .testTag("inv_potion_eternal_resurgence")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .scale(0.96f + pulseWave * 0.06f)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(crimsonMagenta.copy(alpha = 0.45f), gold.copy(alpha = 0.25f))
                                    )
                                )
                                .border(2.dp, ultraLuxuryBrush, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = PotionType.ETERNAL_REBIRTH_ELIXIR.getLocalizedName(currentLang),
                                tint = gold,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = PotionType.ETERNAL_REBIRTH_ELIXIR.getLocalizedName(currentLang),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = PlusJakartaSans,
                                color = Color.White
                            )
                            Text(
                                text = locText(currentLang, "الفئة الخامسة الأسطورية", "NIVEAU 5 LÉGENDAIRE", "LEGENDARY TIER 5"),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = gold
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(crimsonMagenta.copy(alpha = 0.25f))
                            .border(1.2.dp, gold.copy(alpha = 0.8f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = if (isActiveImmunity) {
                                locText(currentLang, "أبدي مفعّل ✓", "Éternel Actif ✓", "Eternal Active ✓")
                            } else "$count / 3",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = gold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = PotionType.ETERNAL_REBIRTH_ELIXIR.getLocalizedDescription(currentLang),
                    fontSize = 11.sp,
                    color = Color(0xFFFBCFE8),
                    lineHeight = 16.sp
                )

                if (count > 0 && !isActiveImmunity) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onActivate,
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .height(48.dp)
                            .testTag("inv_activate_potion_eternal"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = crimsonMagenta,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            locText(currentLang, "تفعيل الانبعاث الأبدي", "Activer Résurgence Éternelle", "Activate Eternal Resurgence"),
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp
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
    onLongPress1s: () -> Unit
) {
    val accentColor = title.rarity.primaryColor
    val rarityLabel = when (currentLang) {
        "ar" -> title.rarity.labelAr
        else -> title.rarity.labelEn
    }
    val statusText = when {
        isEquipped -> locText(currentLang, "مفعّل على ملفك الشخصي", "Équipé sur votre profil", "Equipped on your profile")
        isUnlocked -> locText(currentLang, "متوفر وجاهز للتفعيل", "Disponible et prêt à activer", "Available & ready to activate")
        title.id == ProfileMasterInventory.MILESTONE_PEAK_TITLE.id -> locText(
            currentLang,
            "يُفتح عند 20,000 نقطة شعلة",
            "Débloqué à 20 000 pts Flamme",
            "Unlocks at 20,000 Flame Score"
        )
        title.unlockStation != null -> locText(
            currentLang,
            "يُفتح عند المحطة #${title.unlockStation}",
            "Débloqué à la Station #${title.unlockStation}",
            "Unlocks at Station #${title.unlockStation}"
        )
        else -> locText(currentLang, "مقفل", "Verrouillé", "Locked")
    }

    LongPressTooltipContainer(onLongPress1s = onLongPress1s) { isHolding, progress ->
        Box(
            modifier = Modifier
                .fillMaxWidth()
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
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
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = title.getLocalizedTitle(currentLang),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isUnlocked) cardPrimaryTextColor(isDarkTheme) else Color(0xFF64748B),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(accentColor.copy(alpha = if (isUnlocked) 0.18f else 0.08f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = rarityLabel,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isUnlocked) accentColor else Color(0xFF64748B)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(3.dp))

                        Text(
                            text = statusText,
                            fontSize = 11.sp,
                            fontWeight = if (isEquipped) FontWeight.Bold else FontWeight.Medium,
                            color = when {
                                isEquipped -> accentColor
                                isUnlocked -> NeonEmerald
                                else -> cardSecondaryTextColor(isDarkTheme)
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                when {
                    isEquipped -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(accentColor.copy(alpha = 0.22f))
                                .border(1.2.dp, accentColor, RoundedCornerShape(10.dp))
                                .padding(horizontal = 10.dp, vertical = 7.dp)
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
                                color = accentColor
                            )
                        }
                    }

                    isUnlocked -> {
                        Button(
                            onClick = onActivate,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accentColor,
                                contentColor = Color(0xFF03141F)
                            ),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .height(40.dp)
                                .testTag("inv_activate_title_${title.id}")
                        ) {
                            Text(
                                text = locText(currentLang, "تفعيل", "Activer", "Activate"),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    else -> {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0x1864748B))
                                .border(1.dp, Color(0x4464748B), RoundedCornerShape(10.dp))
                                .padding(horizontal = 10.dp, vertical = 7.dp)
                                .testTag("inv_title_locked_${title.id}")
                        ) {
                            Text(
                                text = locText(currentLang, "مقفل", "Verrouillé", "Locked"),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF64748B)
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
        verticalArrangement = Arrangement.spacedBy(10.dp)
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

        items(allGameAvatars, key = { it.id }) { avatar ->
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
                }
            )
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
    onLongPress1s: () -> Unit
) {
    val accentColor = avatar.rarity.primaryColor
    val rarityLabel = avatar.rarity.getLocalizedLabel(currentLang)
    val statusText = when {
        isEquipped -> locText(currentLang, "مفعّل على ملفك الشخصي", "Équipé sur votre profil", "Equipped on your profile")
        isUnlocked -> locText(currentLang, "متوفر وجاهز للتفعيل", "Disponible et prêt à activer", "Available & ready to activate")
        else -> locText(
            currentLang,
            "يُفتح عند المحطة #${avatar.unlockStation}",
            "Débloqué à la Station #${avatar.unlockStation}",
            "Unlocks at Station #${avatar.unlockStation}"
        )
    }

    LongPressTooltipContainer(onLongPress1s = onLongPress1s) { isHolding, progress ->
        Box(
            modifier = Modifier
                .fillMaxWidth()
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
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
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = avatar.getLocalizedName(currentLang),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isUnlocked) cardPrimaryTextColor(isDarkTheme) else Color(0xFF64748B),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(accentColor.copy(alpha = if (isUnlocked) 0.18f else 0.08f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = rarityLabel,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isUnlocked) accentColor else Color(0xFF64748B)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(3.dp))

                        Text(
                            text = statusText,
                            fontSize = 11.sp,
                            fontWeight = if (isEquipped) FontWeight.Bold else FontWeight.Medium,
                            color = when {
                                isEquipped -> accentColor
                                isUnlocked -> NeonEmerald
                                else -> cardSecondaryTextColor(isDarkTheme)
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                when {
                    isEquipped -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(accentColor.copy(alpha = 0.22f))
                                .border(1.2.dp, accentColor, RoundedCornerShape(10.dp))
                                .padding(horizontal = 10.dp, vertical = 7.dp)
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
                                color = accentColor
                            )
                        }
                    }

                    isUnlocked -> {
                        Button(
                            onClick = onActivate,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accentColor,
                                contentColor = Color(0xFF03141F)
                            ),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .height(40.dp)
                                .testTag("inv_activate_avatar_${avatar.id}")
                        ) {
                            Text(
                                text = locText(currentLang, "تفعيل", "Activer", "Activate"),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    else -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0x1864748B))
                                .border(1.dp, Color(0x4464748B), RoundedCornerShape(10.dp))
                                .padding(horizontal = 10.dp, vertical = 7.dp)
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
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF64748B)
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

