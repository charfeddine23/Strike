package com.example.ui.screens

import android.view.SoundEffectConstants
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.flame.FlameManager
import com.example.data.profile.ProfileManager
import com.example.data.profile.ProfileMasterInventory
import com.example.ui.components.getAvatarVector
import com.example.ui.screens.inventory.AvatarsSubPage
import com.example.ui.screens.inventory.InventoryCategoryTile
import com.example.ui.screens.inventory.InventoryTooltipSheetContent
import com.example.ui.screens.inventory.InventoryTooltipSpec
import com.example.ui.screens.inventory.KeysSubPage
import com.example.ui.screens.inventory.PotionsSubPage
import com.example.ui.screens.inventory.ShieldsSubPage
import com.example.ui.screens.inventory.TitlesSubPage
import com.example.ui.screens.inventory.cardSecondaryTextColor
import com.example.ui.screens.inventory.locText
import com.example.ui.theme.DayCyan
import com.example.ui.theme.DayPurple
import com.example.ui.theme.DayTextPrimary
import com.example.ui.theme.DayTextSecondary
import com.example.ui.theme.LocalAppDarkTheme
import com.example.ui.theme.LocalScaffoldContentPadding
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.PlusJakartaSans
import com.example.ui.viewmodel.IndividualViewModel
import com.example.util.HapticHelper
import kotlinx.coroutines.delay

enum class InventoryCategory {
    SHIELDS,
    KEYS,
    POTIONS,
    TITLES,
    AVATARS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    viewModel: IndividualViewModel,
    isDarkTheme: Boolean = LocalAppDarkTheme.current,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val isHapticEnabled by viewModel.isHapticFeedbackEnabled
    val currentLang by viewModel.currentLanguage.collectAsStateWithLifecycle()

    // Active sub-category state (`null` = 4-container Hub)
    var activeCategory by remember { mutableStateOf<InventoryCategory?>(null) }

    // Intercept system back button inside any category sub-page to return to Hub
    BackHandler(enabled = activeCategory != null) {
        activeCategory = null
    }

    // Shields State
    val isShieldActive by viewModel.isStreakShieldActive.collectAsStateWithLifecycle()
    val activeShieldExpiresAt by viewModel.activeShieldExpiresAt.collectAsStateWithLifecycle()
    val activeShieldType by viewModel.activeShieldType.collectAsStateWithLifecycle()
    val shields24h by viewModel.available1DayShields.collectAsStateWithLifecycle()
    val shields48h by viewModel.available48hShields.collectAsStateWithLifecycle()
    val shields72h by viewModel.availableManualShields.collectAsStateWithLifecycle()
    val sevenDayAutoTriggers by viewModel.sevenDayAutoShieldCount.collectAsStateWithLifecycle()

    var remainingMillis by remember { mutableLongStateOf(FlameManager.getActiveShieldRemainingMillis()) }
    LaunchedEffect(isShieldActive, activeShieldExpiresAt) {
        if (isShieldActive) {
            while (true) {
                remainingMillis = FlameManager.getActiveShieldRemainingMillis()
                if (activeShieldExpiresAt != null && remainingMillis <= 0L) break
                delay(1000L)
            }
        } else {
            remainingMillis = 0L
        }
    }
    val shieldActuallyActive = isShieldActive && (activeShieldExpiresAt == null || remainingMillis > 0L)

    // Skip Keys State
    val woodenKeys by ProfileManager.woodenKeysCount.collectAsStateWithLifecycle()
    val bronzeKeys by ProfileManager.bronzeKeysCount.collectAsStateWithLifecycle()
    val silverKeys by ProfileManager.silverKeysCount.collectAsStateWithLifecycle()

    // Potions State
    val pulseElixirCount by ProfileManager.pulseElixirCount.collectAsStateWithLifecycle()
    val pulseUsesLeft by ProfileManager.pulseElixirUsesLeft.collectAsStateWithLifecycle()
    val shadowSerumCount by ProfileManager.shadowSerumCount.collectAsStateWithLifecycle()
    val shadowSerumExp by ProfileManager.shadowSerumExpiresAt.collectAsStateWithLifecycle()
    val gabrielDropletCount by ProfileManager.gabrielDropletCount.collectAsStateWithLifecycle()
    val gabrielDropletExp by ProfileManager.gabrielDropletExpiresAt.collectAsStateWithLifecycle()
    val cryoInfusionCount by ProfileManager.cryoInfusionCount.collectAsStateWithLifecycle()
    val hasCryoArmed by ProfileManager.hasCryoBuffArmed.collectAsStateWithLifecycle()
    val eternalRebirthCount by ProfileManager.eternalRebirthCount.collectAsStateWithLifecycle()
    val hasRebirthImmunity by ProfileManager.hasRebirthImmunity.collectAsStateWithLifecycle()

    val isShadowActive = shadowSerumExp != null && System.currentTimeMillis() < (shadowSerumExp ?: 0L)
    val isGabrielActive = gabrielDropletExp != null && System.currentTimeMillis() < (gabrielDropletExp ?: 0L)

    // Titles & Avatars State
    val unlockedTitleIds by ProfileManager.unlockedTitleIds.collectAsStateWithLifecycle()
    val equippedTitle by ProfileManager.equippedTitle.collectAsStateWithLifecycle()
    val unlockedAvatarIds by ProfileManager.unlockedAvatarIds.collectAsStateWithLifecycle()
    val equippedAvatar by ProfileManager.equippedAvatar.collectAsStateWithLifecycle()

    // 1-Second Long-Press Tooltip Sheet State & Guide Dialog State
    var selectedTooltip by remember { mutableStateOf<InventoryTooltipSpec?>(null) }
    var showLongPressGuideDialog by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val rtlSign = if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1 else 1

    val navigateToCategory: (InventoryCategory) -> Unit = { category ->
        HapticHelper.triggerActionHaptic(context, isHapticEnabled)
        view.playSoundEffect(SoundEffectConstants.CLICK)
        activeCategory = category
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .testTag("inventory_screen")
    ) {
        AnimatedContent(
            targetState = activeCategory,
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds(),
            transitionSpec = {
                val forward = targetState != null && initialState == null
                val dir = (if (forward) 1 else -1) * rtlSign
                val enter = fadeIn(
                    animationSpec = tween(durationMillis = 155, delayMillis = 65, easing = FastOutSlowInEasing)
                ) + slideInHorizontally(
                    animationSpec = tween(durationMillis = 170, delayMillis = 65, easing = FastOutSlowInEasing)
                ) { fullWidth -> dir * (fullWidth / 14) }
                val exit = fadeOut(
                    animationSpec = tween(durationMillis = 65, easing = FastOutLinearInEasing)
                ) + slideOutHorizontally(
                    animationSpec = tween(durationMillis = 70, easing = FastOutLinearInEasing)
                ) { fullWidth -> -dir * (fullWidth / 18) }
                enter.togetherWith(exit).using(SizeTransform(clip = true))
            },
            label = "inventory_category_transition"
        ) { category ->
            when (category) {
                null -> {
                    InventoryHubContent(
                        currentLang = currentLang,
                        isDarkTheme = isDarkTheme,
                        totalManualShields = shields24h + shields48h + shields72h,
                        totalKeys = woodenKeys + bronzeKeys + silverKeys,
                        totalPotions = pulseElixirCount + shadowSerumCount + gabrielDropletCount + cryoInfusionCount + eternalRebirthCount,
                        unlockedTitlesCount = unlockedTitleIds.size,
                        totalTitlesCount = ProfileMasterInventory.MASTER_TITLES.size + 1,
                        equippedTitleName = equippedTitle.getLocalizedTitle(currentLang),
                        unlockedAvatarsCount = unlockedAvatarIds.size,
                        totalAvatarsCount = ProfileMasterInventory.MASTER_AVATARS.size,
                        equippedAvatarName = equippedAvatar.getLocalizedName(currentLang),
                        equippedAvatarIconName = equippedAvatar.badgeIconName,
                        onSelectCategory = navigateToCategory,
                        onShowTooltip = { selectedTooltip = it }
                    )
                }

                InventoryCategory.SHIELDS -> {
                    ShieldsSubPage(
                        viewModel = viewModel,
                        currentLang = currentLang,
                        isDarkTheme = isDarkTheme,
                        isHapticEnabled = isHapticEnabled,
                        shieldActuallyActive = shieldActuallyActive,
                        activeShieldType = activeShieldType,
                        remainingMillis = remainingMillis,
                        shields24h = shields24h,
                        shields48h = shields48h,
                        shields72h = shields72h,
                        sevenDayAutoTriggers = sevenDayAutoTriggers,
                        onBack = { activeCategory = null },
                        onOpenGuide = { showLongPressGuideDialog = true },
                        onShowTooltip = { selectedTooltip = it }
                    )
                }

                InventoryCategory.KEYS -> {
                    KeysSubPage(
                        currentLang = currentLang,
                        isDarkTheme = isDarkTheme,
                        isHapticEnabled = isHapticEnabled,
                        woodenKeys = woodenKeys,
                        bronzeKeys = bronzeKeys,
                        silverKeys = silverKeys,
                        onBack = { activeCategory = null },
                        onOpenGuide = { showLongPressGuideDialog = true },
                        onShowTooltip = { selectedTooltip = it }
                    )
                }

                InventoryCategory.POTIONS -> {
                    PotionsSubPage(
                        currentLang = currentLang,
                        isDarkTheme = isDarkTheme,
                        isHapticEnabled = isHapticEnabled,
                        pulseElixirCount = pulseElixirCount,
                        pulseUsesLeft = pulseUsesLeft,
                        shadowSerumCount = shadowSerumCount,
                        isShadowActive = isShadowActive,
                        gabrielDropletCount = gabrielDropletCount,
                        isGabrielActive = isGabrielActive,
                        cryoInfusionCount = cryoInfusionCount,
                        hasCryoArmed = hasCryoArmed,
                        eternalRebirthCount = eternalRebirthCount,
                        hasRebirthImmunity = hasRebirthImmunity,
                        onBack = { activeCategory = null },
                        onOpenGuide = { showLongPressGuideDialog = true },
                        onShowTooltip = { selectedTooltip = it }
                    )
                }

                InventoryCategory.TITLES -> {
                    TitlesSubPage(
                        currentLang = currentLang,
                        isDarkTheme = isDarkTheme,
                        isHapticEnabled = isHapticEnabled,
                        unlockedTitleIds = unlockedTitleIds,
                        equippedTitle = equippedTitle,
                        onBack = { activeCategory = null },
                        onOpenGuide = { showLongPressGuideDialog = true },
                        onShowTooltip = { selectedTooltip = it }
                    )
                }

                InventoryCategory.AVATARS -> {
                    AvatarsSubPage(
                        currentLang = currentLang,
                        isDarkTheme = isDarkTheme,
                        isHapticEnabled = isHapticEnabled,
                        unlockedAvatarIds = unlockedAvatarIds,
                        equippedAvatar = equippedAvatar,
                        onBack = { activeCategory = null },
                        onOpenGuide = { showLongPressGuideDialog = true },
                        onShowTooltip = { selectedTooltip = it }
                    )
                }
            }
        }
    }

    // 1-Second Long-Press Translucent Animated Detail ModalBottomSheet
    if (selectedTooltip != null) {
        val spec = selectedTooltip!!
        ModalBottomSheet(
            onDismissRequest = { selectedTooltip = null },
            sheetState = sheetState,
            containerColor = if (isDarkTheme) Color(0xEC0B1120) else Color(0xFAF8FAFC),
            scrimColor = if (isDarkTheme) Color(0x99020617) else Color(0x660F172A),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            dragHandle = null
        ) {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(tween(220)) + scaleIn(initialScale = 0.94f, animationSpec = tween(240)),
                exit = fadeOut(tween(160)) + scaleOut(targetScale = 0.95f)
            ) {
                InventoryTooltipSheetContent(
                    spec = spec,
                    currentLang = currentLang,
                    isDarkTheme = isDarkTheme,
                    onClose = { selectedTooltip = null }
                )
            }
        }
    }

    // Guiding Tooltip Hint Dialog (1 second)
    if (showLongPressGuideDialog) {
        val guideViolet = if (isDarkTheme) NeonViolet else DayPurple
        val guideCyan = if (isDarkTheme) NeonCyan else DayCyan
        AlertDialog(
            onDismissRequest = { showLongPressGuideDialog = false },
            containerColor = if (isDarkTheme) Color(0xF50F172A) else Color(0xFFF8FAFC),
            icon = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(guideViolet.copy(alpha = 0.16f))
                        .border(1.2.dp, guideViolet.copy(alpha = 0.7f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.TouchApp,
                        contentDescription = null,
                        tint = guideViolet,
                        modifier = Modifier.size(26.dp)
                    )
                }
            },
            title = {
                Text(
                    text = locText(currentLang, "دليل الاستخدام", "Guide d'utilisation", "Usage Guide"),
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isDarkTheme) Color.White else DayTextPrimary,
                    fontFamily = PlusJakartaSans
                )
            },
            text = {
                Text(
                    text = locText(
                        currentLang,
                        "اضغط لمدة ثانية واحدة متواصلة على أي عنصر لمعرفة الشرح وكيفية الحصول عليه.",
                        "Maintenez la pression pendant 1 seconde sur n'importe quel élément pour afficher son explication et comment l'obtenir.",
                        "Press and hold any item for 1 second to view its explanation and how to obtain it."
                    ),
                    fontSize = 14.sp,
                    color = if (isDarkTheme) Color(0xFFE2E8F0) else DayTextSecondary,
                    lineHeight = 22.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { showLongPressGuideDialog = false },
                    modifier = Modifier.testTag("inventory_guide_ok_btn")
                ) {
                    Text(
                        text = locText(currentLang, "حسناً", "Compris", "Got It"),
                        color = guideCyan,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        )
    }
}

@Composable
private fun InventoryHubContent(
    currentLang: String,
    isDarkTheme: Boolean,
    totalManualShields: Int,
    totalKeys: Int,
    totalPotions: Int,
    unlockedTitlesCount: Int,
    totalTitlesCount: Int,
    equippedTitleName: String,
    unlockedAvatarsCount: Int,
    totalAvatarsCount: Int,
    equippedAvatarName: String,
    equippedAvatarIconName: String,
    onSelectCategory: (InventoryCategory) -> Unit,
    onShowTooltip: (InventoryTooltipSpec) -> Unit
) {
    val scaffoldPadding = LocalScaffoldContentPadding.current
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("inventory_hub"),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = scaffoldPadding.calculateTopPadding() + 12.dp,
            bottom = scaffoldPadding.calculateBottomPadding() + 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Subtle 1-second long-press hint row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.TouchApp,
                    contentDescription = null,
                    tint = NeonViolet,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = locText(
                        currentLang,
                        "اضغط مطولاً (1 ثانية) على أي عنصر للشرح وكيفية الحصول عليه",
                        "Appui long (1s) sur tout élément pour l'explication et l'obtention",
                        "Hold any item for 1s to view explanation & how to obtain"
                    ),
                    fontSize = 11.sp,
                    color = cardSecondaryTextColor(isDarkTheme),
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Row 1: Shields + Keys side-by-side
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InventoryCategoryTile(
                    modifier = Modifier.weight(1f),
                    title = locText(currentLang, "الدروع", "Boucliers", "Shields"),
                    subtitle = locText(
                        currentLang,
                        "4 أنواع • تفعيل يدوي وتلقائي",
                        "4 types • Manuel & Auto",
                        "4 Tiers • Manual & Auto"
                    ),
                    badgeText = "×$totalManualShields",
                    icon = Icons.Default.Shield,
                    accentColor = NeonEmerald,
                    secondaryColor = NeonCyan,
                    isDarkTheme = isDarkTheme,
                    testTag = "inv_cat_shields",
                    onClick = { onSelectCategory(InventoryCategory.SHIELDS) },
                    onLongPress1s = {
                        onShowTooltip(
                            InventoryTooltipSpec(
                                title = locText(currentLang, "قسم الدروع", "Catégorie Boucliers", "Shields Category"),
                                subtitle = locText(currentLang, "حماية الشعلة", "Protection Flamme", "Flame Defense"),
                                rarityLabel = locText(currentLang, "4 دروع", "4 BOUCLIERS", "4 SHIELDS"),
                                accentColor = NeonEmerald,
                                secondaryColor = NeonCyan,
                                icon = Icons.Default.Shield,
                                mechanics = locText(
                                    currentLang,
                                    "يعرض كل الدروع الوقائية المتوفرة في اللعبة مع كمياتها وإمكانية تفعيل الدروع اليدوية.",
                                    "Affiche tous les boucliers du jeu, leurs quantités et l'activation manuelle.",
                                    "Displays all protective shields in the game with owned quantities and manual activation."
                                ),
                                bonuses = locText(
                                    currentLang,
                                    "توقف الدروع تراكم +3 نقاط خمول يومية لحماية استقرار رصيد الشعلة.",
                                    "Stoppe l'accumulation de +3 points d'inactivité par jour.",
                                    "Halts +3 daily inactivity accrual to safeguard your Flame score."
                                ),
                                expirationRules = locText(
                                    currentLang,
                                    "دروع 24س و48س و72س يدوية التفعيل، بينما درع 7 أيام يتفعّل تلقائياً.",
                                    "24h, 48h, 72h sont manuels ; 7 jours est automatique.",
                                    "24h, 48h & 72h shields activate manually; 7-Day shield triggers automatically."
                                ),
                                howToObtain = locText(
                                    currentLang,
                                    "تُكتسب الدروع من محطات خارطة الطريق أو عبر كومبو 3-5 سترايكات يومية.",
                                    "Obtenus via les stations de la Feuille de route ou combos 3-5 strikes.",
                                    "Earned from Roadmap Station rewards or 3–5 same-day strike combos."
                                )
                            )
                        )
                    }
                )

                InventoryCategoryTile(
                    modifier = Modifier.weight(1f),
                    title = locText(currentLang, "المفاتيح", "Clés", "Keys"),
                    subtitle = locText(
                        currentLang,
                        "مفاتيح تجاوز المهام المتوفرة",
                        "Clés de passe disponibles",
                        "Owned task skip-keys"
                    ),
                    badgeText = "×$totalKeys",
                    icon = Icons.Default.VpnKey,
                    accentColor = NeonAmber,
                    secondaryColor = Color(0xFFEA580C),
                    isDarkTheme = isDarkTheme,
                    testTag = "inv_cat_keys",
                    onClick = { onSelectCategory(InventoryCategory.KEYS) },
                    onLongPress1s = {
                        onShowTooltip(
                            InventoryTooltipSpec(
                                title = locText(currentLang, "قسم المفاتيح", "Catégorie Clés", "Keys Category"),
                                subtitle = locText(currentLang, "مفاتيح التجاوز", "Clés de passe", "Skip-Keys"),
                                rarityLabel = locText(currentLang, "مملوك فقط", "POSSÉDÉES", "OWNED ONLY"),
                                accentColor = NeonAmber,
                                secondaryColor = Color(0xFFEA580C),
                                icon = Icons.Default.VpnKey,
                                mechanics = locText(
                                    currentLang,
                                    "يعرض فقط المفاتيح التي تمتلكها حالياً في مخزونك لتجاوز المهام اليومية والأسبوعية والشهرية.",
                                    "Affiche uniquement les clés actuellement en votre possession.",
                                    "Shows only the Skip-Keys you currently own in your inventory."
                                ),
                                bonuses = locText(
                                    currentLang,
                                    "تُكمل المهام المؤهلة فوراً وتمنح كامل نقاط الخبرة (XP) الخاصة بها.",
                                    "Valide instantanément une mission éligible avec toute son XP.",
                                    "Completes eligible recurring tasks immediately with full XP."
                                ),
                                expirationRules = locText(
                                    currentLang,
                                    "الحد الأقصى 30 مفتاحاً لكل فئة؛ لا يمكن استخدامها في المهام السنوية.",
                                    "Max 30 clés par niveau ; exclut les missions annuelles.",
                                    "Max 30 keys per tier; Annual tasks are excluded."
                                ),
                                howToObtain = locText(
                                    currentLang,
                                    "تُمنح كمكافآت من محطات خارطة الطريق (خاصة المحطات 2، 5، 8 وكل محطة عشرينية ×20).",
                                    "Obtenues aux stations de la Feuille de route (2, 5, 8 et multiples de 20).",
                                    "Earned from Roadmap Station rewards (Stations 2, 5, 8 cycles & 20th milestones)."
                                )
                            )
                        )
                    }
                )
            }
        }

        // Row 2: Potions + Titles side-by-side
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InventoryCategoryTile(
                    modifier = Modifier.weight(1f),
                    title = locText(currentLang, "العقاقير", "Potions", "Potions"),
                    subtitle = locText(
                        currentLang,
                        "جرعاتك المتوفرة مع التفعيل",
                        "Vos potions avec activation",
                        "Owned elixirs & activation"
                    ),
                    badgeText = "×$totalPotions",
                    icon = Icons.Default.Science,
                    accentColor = NeonCyan,
                    secondaryColor = NeonViolet,
                    isDarkTheme = isDarkTheme,
                    testTag = "inv_cat_potions",
                    onClick = { onSelectCategory(InventoryCategory.POTIONS) },
                    onLongPress1s = {
                        onShowTooltip(
                            InventoryTooltipSpec(
                                title = locText(currentLang, "قسم العقاقير", "Catégorie Potions", "Potions Category"),
                                subtitle = locText(currentLang, "الجرعات السيبرانية", "Élixirs Cyber", "Cybernetic Elixirs"),
                                rarityLabel = locText(currentLang, "5 فئات", "5 NIVEAUX", "5 TIERS"),
                                accentColor = NeonCyan,
                                secondaryColor = NeonViolet,
                                icon = Icons.Default.Science,
                                mechanics = locText(
                                    currentLang,
                                    "يعرض العقاقير المتوفرة لديك مع أزرار التفعيل الفوري للعقاقير القابلة للاستخدام.",
                                    "Affiche les potions en stock avec boutons d'activation disponibles.",
                                    "Displays your owned & active potions with instant activation buttons."
                                ),
                                bonuses = locText(
                                    currentLang,
                                    "تُعزز زر التبريد، تبطئ الخمول، تسلّح الستريك (+100)، أو تمنح مناعة أبدية.",
                                    "Booste Deflame, ralentit l'inactivité, arme le prochain strike (+100) ou protège à vie.",
                                    "Boosts Deflame, slows inactivity, arms next strike (+100), or grants eternal immunity."
                                ),
                                expirationRules = locText(
                                    currentLang,
                                    "السعة القصوى 3 جرعات لكل نوع؛ ولكل عقار مدة مفعول مخصصة.",
                                    "Capacité max de 3 flacons par type.",
                                    "Max capacity 3 bottles per potion tier."
                                ),
                                howToObtain = locText(
                                    currentLang,
                                    "تُكتسب من محطات خارطة الطريق (3، 6، 9، 10 والمحطة النهائية 500).",
                                    "Obtenues aux stations (3, 6, 9, 10 et station finale 500).",
                                    "Earned from Roadmap Station rewards (3, 6, 9, 10 cycles & Station 500)."
                                )
                            )
                        )
                    }
                )

                InventoryCategoryTile(
                    modifier = Modifier.weight(1f),
                    title = locText(currentLang, "الألقاب", "Titres", "Titles"),
                    subtitle = equippedTitleName,
                    badgeText = "$unlockedTitlesCount/$totalTitlesCount",
                    icon = Icons.Default.MilitaryTech,
                    accentColor = NeonViolet,
                    secondaryColor = Color(0xFFEC4899),
                    isDarkTheme = isDarkTheme,
                    testTag = "inv_cat_titles",
                    onClick = { onSelectCategory(InventoryCategory.TITLES) },
                    onLongPress1s = {
                        onShowTooltip(
                            InventoryTooltipSpec(
                                title = locText(currentLang, "قسم الألقاب", "Catégorie Titres", "Titles Category"),
                                subtitle = locText(currentLang, "كل ألقاب اللعبة", "Tous les titres", "All Game Titles"),
                                rarityLabel = "$unlockedTitlesCount / $totalTitlesCount",
                                accentColor = NeonViolet,
                                secondaryColor = Color(0xFFEC4899),
                                icon = Icons.Default.MilitaryTech,
                                mechanics = locText(
                                    currentLang,
                                    "يعرض كل الألقاب الموجودة في اللعبة (51 لقباً) مع إمكانية تفعيل أي لقب مفتوح مباشرة.",
                                    "Affiche tous les titres du jeu (51) et permet d'activer ceux débloqués.",
                                    "Lists all 51 titles in the game and lets you activate any unlocked title directly."
                                ),
                                bonuses = locText(
                                    currentLang,
                                    "يُظهر رتبتك وهالتك البصرية النادرة في بطاقة الملف الشخصي.",
                                    "Affiche votre rang et halo lumineux sur la carte de profil.",
                                    "Displays your prestige rank and rarity halo across your Profile."
                                ),
                                expirationRules = locText(
                                    currentLang,
                                    "الألقاب المفتوحة دائمة ولا تنتهي صلاحيتها ويمكن التبديل بينها في أي وقت.",
                                    "Les titres débloqués sont permanents et interchangeables à tout moment.",
                                    "Unlocked titles are permanent and can be swapped anytime."
                                ),
                                howToObtain = locText(
                                    currentLang,
                                    "تُفتح بتقدمك عبر محطات خارطة الطريق (1 إلى 500) أو ببلوغ 20,000 نقطة شعلة.",
                                    "Débloqués via les stations (1 à 500) ou à 20 000 pts Flamme.",
                                    "Unlocked by advancing through Roadmap Stations (1–500) or reaching 20,000 Flame Score."
                                )
                            )
                        )
                    }
                )
            }
        }

        // Row 3: Avatars full-width showcase container
        item {
            InventoryCategoryTile(
                modifier = Modifier.fillMaxWidth(),
                title = locText(currentLang, "الأفاتار", "Avatars", "Avatars"),
                subtitle = locText(
                    currentLang,
                    "كل أفاتارات اللعبة ($totalAvatarsCount) • المفعّل: $equippedAvatarName",
                    "Tous les avatars ($totalAvatarsCount) • Actif : $equippedAvatarName",
                    "All Game Avatars ($totalAvatarsCount) • Active: $equippedAvatarName"
                ),
                badgeText = "$unlockedAvatarsCount/$totalAvatarsCount",
                icon = getAvatarVector(equippedAvatarIconName),
                accentColor = Color(0xFFEC4899),
                secondaryColor = NeonViolet,
                isDarkTheme = isDarkTheme,
                testTag = "inv_cat_avatars",
                onClick = { onSelectCategory(InventoryCategory.AVATARS) },
                onLongPress1s = {
                    onShowTooltip(
                        InventoryTooltipSpec(
                            title = locText(currentLang, "قسم الأفاتار", "Catégorie Avatars", "Avatars Category"),
                            subtitle = locText(currentLang, "كل أفاتارات اللعبة", "Tous les avatars", "All Game Avatars"),
                            rarityLabel = "$unlockedAvatarsCount / $totalAvatarsCount",
                            accentColor = Color(0xFFEC4899),
                            secondaryColor = NeonViolet,
                            icon = getAvatarVector(equippedAvatarIconName),
                            mechanics = locText(
                                currentLang,
                                "يجمع كل أفاتارات اللعبة ($totalAvatarsCount أفاتاراً) من الشائع إلى الأسطوري مع إمكانية تفعيل أي أفاتار متوفر.",
                                "Rassemble tous les avatars du jeu ($totalAvatarsCount) et permet d'équiper ceux débloqués.",
                                "Collects all $totalAvatarsCount avatars in the game with instant activation for unlocked avatars."
                            ),
                            bonuses = locText(
                                currentLang,
                                "يغيّر أيقونة وهالة حسابك في الشريط العلوي والملف الشخصي حسب ندرة الأفاتار.",
                                "Personnalise l'icône et le halo de votre profil selon sa rareté.",
                                "Customizes your top bar and profile badge icon with rarity-based neon glow."
                            ),
                            expirationRules = locText(
                                currentLang,
                                "الأفاتارات المفتوحة دائمة ولا تنتهي صلاحيتها ويمكن التبديل بينها في أي وقت.",
                                "Les avatars débloqués sont permanents et interchangeables à tout moment.",
                                "Unlocked avatars are permanent and can be swapped anytime."
                            ),
                            howToObtain = locText(
                                currentLang,
                                "تُفتح الأفاتارات تلقائياً عند التقدم عبر محطات خارطة الطريق (من المحطة #1 حتى المحطة #500).",
                                "Débloqués automatiquement en progressant sur les stations (1 à 500).",
                                "Unlocked automatically by progressing through Roadmap Stations (Station #1 to #500)."
                            )
                        )
                    )
                }
            )
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }
    }
}
