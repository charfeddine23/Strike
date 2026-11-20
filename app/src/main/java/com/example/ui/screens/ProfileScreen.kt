package com.example.ui.screens

import android.view.SoundEffectConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.deviations.DeviationsManager
import com.example.data.profile.AvatarItem
import com.example.data.profile.ProfileManager
import com.example.data.profile.ProfileMasterInventory
import com.example.data.profile.TitleItem
import com.example.data.profile.TitleRarity
import com.example.data.roadmap.RoadmapManager
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.ProfileAvatarHeaderIcon
import com.example.ui.components.getAvatarVector
import com.example.ui.theme.CaveatHandwriting
import com.example.ui.theme.DayCyan
import com.example.ui.theme.DayEmerald
import com.example.ui.theme.DayOrange
import com.example.ui.theme.DayPurple
import com.example.ui.theme.DayTextPrimary
import com.example.ui.theme.DayTextSecondary
import com.example.ui.theme.LocalAppDarkTheme
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.PlusJakartaSans
import com.example.ui.viewmodel.IndividualViewModel
import com.example.util.HapticHelper
import java.util.Locale

/**
 * Cyberpunk Glassmorphism Profile Dashboard Screen:
 * - Dynamic Profile Header & Editable Username
 * - Dynamic Title Card with Rarity-based neon glows, particle accents, and borders
 * - Strikers Metrics Panel (Flame Score / 20,000, Shadow Grip %, Total XP, Active Station)
 * - Cybernetic Potions & Skip-Keys Arsenal Status
 * - 20 Profile Avatars Selector Grid
 * - 50 Titles Master Inventory List + Milestone Peak Title
 */
@Composable
fun ProfileScreen(
    viewModel: IndividualViewModel,
    onNavigateBack: () -> Unit,
    isDarkTheme: Boolean = LocalAppDarkTheme.current
) {
    val context = LocalContext.current
    val view = LocalView.current
    val isHapticEnabled by viewModel.isHapticFeedbackEnabled
    val currentLang by viewModel.currentLanguage.collectAsStateWithLifecycle()
    val isAr = currentLang == "ar"
    val accentCyan = if (isDarkTheme) NeonCyan else DayCyan
    val accentAmber = if (isDarkTheme) NeonAmber else DayOrange
    val accentViolet = if (isDarkTheme) NeonViolet else DayPurple
    val accentEmerald = if (isDarkTheme) NeonEmerald else DayEmerald

    // Profile state flows
    val userName by ProfileManager.userName.collectAsStateWithLifecycle()
    val equippedAvatar by ProfileManager.equippedAvatar.collectAsStateWithLifecycle()
    val equippedTitle by ProfileManager.equippedTitle.collectAsStateWithLifecycle()
    val unlockedAvatars by ProfileManager.unlockedAvatarIds.collectAsStateWithLifecycle()
    val unlockedTitles by ProfileManager.unlockedTitleIds.collectAsStateWithLifecycle()

    // Telemetry metrics
    val flameScore by viewModel.flameScore.collectAsStateWithLifecycle()
    val totalXp by viewModel.totalXp.collectAsStateWithLifecycle()
    val unlockedStation = remember(totalXp) { RoadmapManager.calculateUnlockedStationId(totalXp) }
    val darkRank = remember(flameScore, currentLang) { DeviationsManager.getDarkRank(flameScore, currentLang) }
    val shadowGripPercent = (darkRank.gripProgress * 100f).coerceIn(0f, 100f)

    // Arsenal flows
    val pulseElixirCount by ProfileManager.pulseElixirCount.collectAsStateWithLifecycle()
    val shadowSerumCount by ProfileManager.shadowSerumCount.collectAsStateWithLifecycle()
    val gabrielDropletCount by ProfileManager.gabrielDropletCount.collectAsStateWithLifecycle()
    val cryoInfusionCount by ProfileManager.cryoInfusionCount.collectAsStateWithLifecycle()
    val eternalRebirthCount by ProfileManager.eternalRebirthCount.collectAsStateWithLifecycle()
    val pulseUsesLeft by ProfileManager.pulseElixirUsesLeft.collectAsStateWithLifecycle()
    val isShadowActive = ProfileManager.isShadowSerumActive()
    val isGabrielActive = ProfileManager.isGabrielDropletActive()

    val woodenKeys by ProfileManager.woodenKeysCount.collectAsStateWithLifecycle()
    val bronzeKeys by ProfileManager.bronzeKeysCount.collectAsStateWithLifecycle()
    val silverKeys by ProfileManager.silverKeysCount.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Avatars, 1 = Titles, 2 = Cyber Arsenal
    var showEditNameDialog by remember { mutableStateOf(false) }
    var tempName by remember { mutableStateOf(userName) }

    // Rarity animation transition for the dynamic title card
    val infiniteTransition = rememberInfiniteTransition(label = "title_glow_transition")
    val titleGlowPulse by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_pulse"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (isDarkTheme) Color(0x221E293B) else Color(0xCCFFFFFF))
                        .border(1.dp, if (isDarkTheme) Color(0x4038BDF8) else Color(0x660284C7), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = if (isAr) "رجوع" else "Back",
                        tint = accentCyan
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (isAr) "الملف الشخصي والعتاد" else "Profile & Arsenal",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = PlusJakartaSans,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isAr) "العتاد السيبراني ومصفوفة القدرات" else "Cyber Profile & Arsenal Matrix",
                        style = MaterialTheme.typography.labelSmall,
                        color = accentCyan,
                        fontSize = 11.sp
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. PROFILE HEADER COMPONENT
            item {
                LiquidGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    darkTheme = isDarkTheme
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Large Avatar with Dynamic Rarity Halo
                        ProfileAvatarHeaderIcon(
                            avatar = equippedAvatar,
                            title = equippedTitle,
                            onClick = { selectedTab = 0 },
                            size = 72.dp
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                                        view.playSoundEffect(SoundEffectConstants.CLICK)
                                        tempName = userName
                                        showEditNameDialog = true
                                    }
                                    .padding(horizontal = 4.dp, vertical = 6.dp)
                                    .testTag("profile_username_tap")
                            ) {
                                Text(
                                    text = userName,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = PlusJakartaSans,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isAr) "المستوى الحالي: محطة #$unlockedStation" else "Current Level: Station #$unlockedStation",
                                style = MaterialTheme.typography.labelMedium,
                                color = accentAmber,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 2. DYNAMIC TITLE CARD
            item {
                val rarity = equippedTitle.rarity
                val rarityAccent = rarity.adaptiveColor(isDarkTheme)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (isDarkTheme) Color(0x181E293B) else Color(0xF5FFFFFF))
                        .border(
                            1.5.dp,
                            rarityAccent.copy(alpha = titleGlowPulse),
                            RoundedCornerShape(18.dp)
                        )
                        .padding(14.dp)
                ) {
                    // Background ambient particle glow
                    Canvas(modifier = Modifier.matchParentSize()) {
                        drawCircle(
                            color = rarityAccent.copy(alpha = (if (isDarkTheme) 0.15f else 0.10f) * titleGlowPulse),
                            radius = size.width * 0.45f,
                            center = Offset(size.width * 0.85f, size.height * 0.5f)
                        )
                    }

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = rarityAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isAr) "اللقب المجهّز (${rarity.labelAr})" else "Equipped Title (${rarity.labelEn})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = rarityAccent
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(rarityAccent.copy(alpha = if (isDarkTheme) 0.2f else 0.14f))
                                    .border(1.dp, rarityAccent.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = (if (isAr) rarity.labelAr else rarity.labelEn).uppercase(Locale.US),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = rarityAccent
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = equippedTitle.getLocalizedTitle(currentLang),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = PlusJakartaSans,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        val localizedDesc = equippedTitle.getLocalizedDescription(currentLang)
                        if (localizedDesc.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = localizedDesc,
                                fontSize = 11.sp,
                                color = accentCyan.copy(alpha = 0.88f)
                            )
                        }
                    }
                }
            }

            // 3. STRIKERS METRICS PANEL
            item {
                LiquidGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    darkTheme = isDarkTheme
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Timeline,
                                contentDescription = null,
                                tint = accentCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isAr) "مؤشرات وقياس أداء اللاعب" else "Strikers Metrics Telemetry",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Unlimited Flame Score (0 -> Infinity)
                            MetricTile(
                                modifier = Modifier.weight(1f),
                                label = if (isAr) "نقاط الشعلة" else "Flame Score",
                                value = String.format(Locale.US, "%,.0f", flameScore),
                                sub = if (flameScore > 20000.0) {
                                    if (isAr) "تخطى حاجز 20k ⚡ (∞)" else "Beyond 20k ⚡ (∞)"
                                } else {
                                    "${String.format(Locale.US, "%.1f", (flameScore / 20000.0) * 100)}%"
                                },
                                icon = Icons.Default.LocalFireDepartment,
                                color = accentAmber
                            )

                            // Shadow Grip %
                            MetricTile(
                                modifier = Modifier.weight(1f),
                                label = if (isAr) "قبضة الظل" else "Shadow Grip",
                                value = "${String.format(Locale.US, "%.1f", shadowGripPercent)}%",
                                sub = darkRank.title,
                                icon = Icons.Default.Shield,
                                color = if (shadowGripPercent > 50f) Color(0xFFDC2626) else accentEmerald
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Stage XP Progress (Resets from 0 per station)
                            val stageProg = remember(totalXp) { RoadmapManager.getStageProgress(totalXp) }
                            MetricTile(
                                modifier = Modifier.weight(1f),
                                label = if (isAr) "XP المحطة ${stageProg.nextStationId}" else "LVL ${stageProg.nextStationId} XP",
                                value = "${stageProg.currentStageXp} / ${stageProg.nextStageRequiredXp}",
                                sub = if (isAr) "الإجمالي: $totalXp XP" else "Total: $totalXp XP",
                                icon = Icons.Default.Bolt,
                                color = accentCyan
                            )

                            // Active Station
                            MetricTile(
                                modifier = Modifier.weight(1f),
                                label = if (isAr) "المحطة النشطة" else "Active Station",
                                value = if (isAr) "محطة #$unlockedStation" else "Station #$unlockedStation",
                                sub = if (isAr) "من 500 محطة" else "of 500 Stations",
                                icon = Icons.Default.AutoAwesome,
                                color = accentViolet
                            )
                        }
                    }
                }
            }

            // 4. TAB SELECTOR (Avatars / Titles / Arsenal)
            item {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = if (isDarkTheme) Color(0x181E293B) else Color(0xFFE6EFF8),
                    contentColor = accentCyan,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = accentCyan,
                            height = 3.dp
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                if (isAr) "الأفاتار (${unlockedAvatars.size}/20)" else "Avatars (${unlockedAvatars.size}/20)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                if (isAr) "الألقاب (${unlockedTitles.size}/50)" else "Titles (${unlockedTitles.size}/50)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = {
                            Text(
                                if (isAr) "الجرعات والمفاتيح" else "Potions & Keys",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    )
                }
            }

            // TAB 0: 20 PROFILE AVATARS
            if (selectedTab == 0) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        ProfileMasterInventory.MASTER_AVATARS.forEach { avatar ->
                            val isUnlocked = unlockedAvatars.contains(avatar.id)
                            val isEquipped = equippedAvatar.id == avatar.id
                            AvatarItemCard(
                                avatar = avatar,
                                currentLang = currentLang,
                                isDarkTheme = isDarkTheme,
                                isUnlocked = isUnlocked,
                                isEquipped = isEquipped,
                                onEquip = {
                                    HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                                    view.playSoundEffect(SoundEffectConstants.CLICK)
                                    ProfileManager.equipAvatar(context, avatar.id)
                                }
                            )
                        }
                    }
                }
            }

            // TAB 1: 50 TITLES MASTER INVENTORY + MILESTONE
            if (selectedTab == 1) {
                // Milestone Title if unlocked
                item {
                    val isMilestoneUnlocked = unlockedTitles.contains(ProfileMasterInventory.MILESTONE_PEAK_TITLE.id)
                    val isEquipped = equippedTitle.id == ProfileMasterInventory.MILESTONE_PEAK_TITLE.id
                    TitleItemCard(
                        title = ProfileMasterInventory.MILESTONE_PEAK_TITLE,
                        currentLang = currentLang,
                        isDarkTheme = isDarkTheme,
                        isUnlocked = isMilestoneUnlocked,
                        isEquipped = isEquipped,
                        onEquip = {
                            HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                            view.playSoundEffect(SoundEffectConstants.CLICK)
                            ProfileManager.equipTitle(context, ProfileMasterInventory.MILESTONE_PEAK_TITLE.id)
                        }
                    )
                }

                items(ProfileMasterInventory.MASTER_TITLES, key = { it.id }) { title ->
                    val isUnlocked = unlockedTitles.contains(title.id)
                    val isEquipped = equippedTitle.id == title.id
                    TitleItemCard(
                        title = title,
                        currentLang = currentLang,
                        isDarkTheme = isDarkTheme,
                        isUnlocked = isUnlocked,
                        isEquipped = isEquipped,
                        onEquip = {
                            HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                            view.playSoundEffect(SoundEffectConstants.CLICK)
                            ProfileManager.equipTitle(context, title.id)
                        }
                    )
                }
            }

            // TAB 2: CYBER POTIONS & KEYS ARSENAL
            if (selectedTab == 2) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = if (isAr) "الجرعات السيبرانية (الحد الأقصى: 3 لكل نوع)" else "Cybernetic Potions (Max Capacity: 3 each)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentAmber
                        )

                        // 1. Pulse Elixir
                        PotionCard(
                            name = com.example.data.profile.PotionType.PULSE_ELIXIR.getLocalizedName(currentLang),
                            desc = com.example.data.profile.PotionType.PULSE_ELIXIR.getLocalizedDescription(currentLang),
                            count = pulseElixirCount,
                            statusNote = if (pulseUsesLeft > 0) {
                                if (isAr) "نشط حالياً ($pulseUsesLeft استخدامات متبقية)" else "Currently active ($pulseUsesLeft uses left)"
                            } else "",
                            color = com.example.data.profile.PotionType.PULSE_ELIXIR.adaptiveColor(isDarkTheme),
                            canUse = pulseElixirCount > 0,
                            currentLang = currentLang,
                            isDarkTheme = isDarkTheme,
                            onUse = {
                                ProfileManager.usePulseElixir(context)
                            }
                        )

                        // 2. Shadow-Grip Serum
                        PotionCard(
                            name = com.example.data.profile.PotionType.SHADOW_GRIP_SERUM.getLocalizedName(currentLang),
                            desc = com.example.data.profile.PotionType.SHADOW_GRIP_SERUM.getLocalizedDescription(currentLang),
                            count = shadowSerumCount,
                            statusNote = if (isShadowActive) {
                                if (isAr) "نشط حالياً (حماية 50% لمدة 3 أيام)" else "Currently active (50% protection for 3 days)"
                            } else "",
                            color = com.example.data.profile.PotionType.SHADOW_GRIP_SERUM.adaptiveColor(isDarkTheme),
                            canUse = shadowSerumCount > 0 && !isShadowActive,
                            currentLang = currentLang,
                            isDarkTheme = isDarkTheme,
                            onUse = {
                                ProfileManager.useShadowSerum(context)
                            }
                        )

                        // 3. Gabriel's Purity Droplet
                        PotionCard(
                            name = com.example.data.profile.PotionType.GABRIEL_PURITY_DROPLET.getLocalizedName(currentLang),
                            desc = com.example.data.profile.PotionType.GABRIEL_PURITY_DROPLET.getLocalizedDescription(currentLang),
                            count = gabrielDropletCount,
                            statusNote = if (isGabrielActive) {
                                if (isAr) "نشط تلقائياً (تسارع 50% لمدة 7 أيام)" else "Auto-active (+50% growth for 7 days)"
                            } else {
                                if (isAr) "خامل" else "Inactive"
                            },
                            color = com.example.data.profile.PotionType.GABRIEL_PURITY_DROPLET.adaptiveColor(isDarkTheme),
                            canUse = false,
                            currentLang = currentLang,
                            isDarkTheme = isDarkTheme,
                            onUse = {}
                        )

                        // 4. Absolute Cryo-Infusion
                        PotionCard(
                            name = com.example.data.profile.PotionType.ABSOLUTE_CRYO_INFUSION.getLocalizedName(currentLang),
                            desc = com.example.data.profile.PotionType.ABSOLUTE_CRYO_INFUSION.getLocalizedDescription(currentLang),
                            count = cryoInfusionCount,
                            statusNote = if (ProfileManager.hasCryoBuffArmed.value) {
                                if (isAr) "جاهز ومسلّح للستريك القادم (+100)" else "Armed for next strike (+100)"
                            } else "",
                            color = com.example.data.profile.PotionType.ABSOLUTE_CRYO_INFUSION.adaptiveColor(isDarkTheme),
                            canUse = cryoInfusionCount > 0 && !ProfileManager.hasCryoBuffArmed.value,
                            currentLang = currentLang,
                            isDarkTheme = isDarkTheme,
                            onUse = {
                                ProfileManager.useCryoInfusion(context)
                            }
                        )

                        // 5. Eternal Rebirth Elixir
                        PotionCard(
                            name = com.example.data.profile.PotionType.ETERNAL_REBIRTH_ELIXIR.getLocalizedName(currentLang),
                            desc = com.example.data.profile.PotionType.ETERNAL_REBIRTH_ELIXIR.getLocalizedDescription(currentLang),
                            count = eternalRebirthCount,
                            statusNote = if (ProfileManager.hasRebirthImmunity.value) {
                                if (isAr) "مفعّل: مناعة أبدية وسيد النقوشة" else "Activated: Eternal immunity & Gary Master"
                            } else "",
                            color = com.example.data.profile.PotionType.ETERNAL_REBIRTH_ELIXIR.adaptiveColor(isDarkTheme),
                            canUse = eternalRebirthCount > 0 && !ProfileManager.hasRebirthImmunity.value,
                            currentLang = currentLang,
                            isDarkTheme = isDarkTheme,
                            onUse = {
                                ProfileManager.useEternalRebirth(context)
                            }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = if (isAr) "مفاتيح تجاوز المهام (السعة القصوى: 30 مفتاحاً)" else "Skip Keys (Max Capacity: 30 Keys)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentCyan
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            KeyTile(
                                modifier = Modifier.weight(1f),
                                label = com.example.data.profile.SkipKeyType.WOODEN.getLocalizedName(currentLang),
                                uses = if (isAr) "استخدام واحد" else "1 Use",
                                count = woodenKeys,
                                color = Color(0xFFB45309)
                            )
                            KeyTile(
                                modifier = Modifier.weight(1f),
                                label = com.example.data.profile.SkipKeyType.BRONZE.getLocalizedName(currentLang),
                                uses = if (isAr) "استخدامان" else "2 Uses",
                                count = bronzeKeys,
                                color = if (isDarkTheme) Color(0xFFD97706) else Color(0xFFB45309)
                            )
                            KeyTile(
                                modifier = Modifier.weight(1f),
                                label = com.example.data.profile.SkipKeyType.SILVER.getLocalizedName(currentLang),
                                uses = if (isAr) "3 استخدامات" else "3 Uses",
                                count = silverKeys,
                                color = if (isDarkTheme) Color(0xFFCBD5E1) else Color(0xFF475569)
                            )
                        }
                    }
                }
            }
        }
    }

    // Luxurious Cyberpunk Username Customization Dialog
    if (showEditNameDialog) {
        val allGameTitles = remember {
            (ProfileMasterInventory.MASTER_TITLES + ProfileMasterInventory.MILESTONE_PEAK_TITLE)
        }
        Dialog(
            onDismissRequest = { showEditNameDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.90f)
                    .widthIn(max = 400.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = if (isDarkTheme) {
                                listOf(Color(0xFF0E1526), Color(0xFF090D18))
                            } else {
                                listOf(Color(0xFFFFFFFF), Color(0xFFF1F5F9))
                            }
                        )
                    )
                    .border(
                        width = 1.6.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(accentCyan, accentViolet, accentAmber)
                        ),
                        shape = RoundedCornerShape(26.dp)
                    )
                    .testTag("edit_username_luxury_dialog")
            ) {
                // Ambient radial cyberpunk glow behind header
                Canvas(modifier = Modifier.matchParentSize()) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(accentViolet.copy(alpha = if (isDarkTheme) 0.22f else 0.10f), Color.Transparent),
                            center = Offset(size.width * 0.5f, 0f),
                            radius = size.width * 0.75f
                        ),
                        center = Offset(size.width * 0.5f, 0f),
                        radius = size.width * 0.75f
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Close icon at top-end
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(
                            onClick = { showEditNameDialog = false },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = if (isAr) "إغلاق" else "Close",
                                tint = if (isDarkTheme) Color(0xFF94A3B8) else DayTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Crown / Spark Emblem Badge
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = if (isDarkTheme) {
                                        listOf(NeonCyan.copy(alpha = 0.28f), Color(0xFF0B1324))
                                    } else {
                                        listOf(DayCyan.copy(alpha = 0.20f), Color(0xFFE0F2FE))
                                    }
                                )
                            )
                            .border(
                                1.5.dp,
                                Brush.linearGradient(listOf(accentCyan, accentAmber)),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = accentAmber,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = if (isAr) "تخصيص الهوية السيبرانية" else "Customize Cyber Identity",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = PlusJakartaSans,
                        color = if (isDarkTheme) Color.White else DayTextPrimary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (isAr) "اكتب اسمك المفضّل أو ولّد اسماً فخماً من ألقاب اللعبة" else "Enter your preferred name or generate one from game titles",
                        fontSize = 11.sp,
                        color = if (isDarkTheme) Color(0xFF94A3B8) else DayTextSecondary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    OutlinedTextField(
                        value = tempName,
                        onValueChange = { tempName = it },
                        singleLine = true,
                        label = { Text(if (isAr) "الاسم الجديد" else "New Name", fontSize = 12.sp) },
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentCyan,
                            unfocusedBorderColor = if (isDarkTheme) Color(0x5538BDF8) else Color(0x660284C7),
                            focusedTextColor = if (isDarkTheme) Color.White else DayTextPrimary,
                            unfocusedTextColor = if (isDarkTheme) Color.White else DayTextPrimary,
                            focusedLabelColor = accentCyan,
                            unfocusedLabelColor = if (isDarkTheme) Color(0xFF94A3B8) else DayTextSecondary,
                            cursorColor = accentCyan
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_name_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Random name generator from the game's Master Titles inventory
                    OutlinedButton(
                        onClick = {
                            HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                            view.playSoundEffect(SoundEffectConstants.CLICK)
                            val candidates = allGameTitles.filter { it.getLocalizedTitle(currentLang) != tempName.trim() }
                            val picked = (if (candidates.isNotEmpty()) candidates else allGameTitles).random()
                            tempName = picked.getLocalizedTitle(currentLang)
                        },
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.2.dp,
                            Brush.horizontalGradient(listOf(accentViolet, accentCyan))
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("profile_random_title_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Casino,
                            contentDescription = null,
                            tint = accentAmber,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isAr) "توليد اسم عشوائي من الألقاب" else "Generate Random Title Name",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkTheme) Color.White else DayTextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Confirm Button (Full-width vibrant cyber gradient look)
                    Button(
                        onClick = {
                            HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                            view.playSoundEffect(SoundEffectConstants.CLICK)
                            ProfileManager.setUserName(context, tempName)
                            showEditNameDialog = false
                        },
                        enabled = tempName.trim().isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentCyan,
                            contentColor = if (isDarkTheme) Color(0xFF090D18) else Color.White,
                            disabledContainerColor = accentCyan.copy(alpha = 0.35f)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("profile_confirm_name_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isAr) "تأكيد الاسم" else "Confirm Name",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricTile(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    sub: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.10f))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = label, fontSize = 10.sp, color = color, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
            Text(text = sub, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AvatarItemCard(
    avatar: AvatarItem,
    currentLang: String,
    isDarkTheme: Boolean = LocalAppDarkTheme.current,
    isUnlocked: Boolean,
    isEquipped: Boolean,
    onEquip: () -> Unit
) {
    val isAr = currentLang == "ar"
    val rarityColor = avatar.rarity.adaptiveColor(isDarkTheme)
    val equipColor = if (isDarkTheme) NeonCyan else DayCyan
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isEquipped) rarityColor.copy(alpha = if (isDarkTheme) 0.15f else 0.12f)
                else if (isDarkTheme) Color(0x181E293B) else Color(0xF5FFFFFF)
            )
            .border(
                1.dp,
                if (isEquipped) rarityColor else if (isDarkTheme) Color(0x2238BDF8) else Color(0x55CBD5E1),
                RoundedCornerShape(14.dp)
            )
            .clickable(enabled = isUnlocked && !isEquipped, onClick = onEquip)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(if (isDarkTheme) Color(0xFF0F172A) else Color(0xFFF1F5F9))
                        .border(
                            1.5.dp,
                            if (isUnlocked) rarityColor else Color(0xFF64748B),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isUnlocked) {
                        Icon(
                            imageVector = getAvatarVector(avatar.badgeIconName),
                            contentDescription = avatar.getLocalizedName(currentLang),
                            tint = rarityColor,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = if (isAr) "مقفل" else "Locked",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = avatar.getLocalizedName(currentLang),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isUnlocked) MaterialTheme.colorScheme.onSurface else Color(0xFF64748B)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(rarityColor.copy(alpha = 0.15f))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = avatar.rarity.getLocalizedLabel(currentLang),
                                fontSize = 8.sp,
                                color = rarityColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = if (isUnlocked) {
                            if (isAr) "مفتوح وجاهز للتجهيز" else "Unlocked & Ready"
                        } else {
                            if (isAr) "يُفتح عند المحطة #${avatar.unlockStation}" else "Unlocks at Station #${avatar.unlockStation}"
                        },
                        fontSize = 10.sp,
                        color = if (isUnlocked) MaterialTheme.colorScheme.onSurfaceVariant else if (isDarkTheme) Color(0xFF94A3B8) else DayTextSecondary
                    )
                }
            }

            if (isEquipped) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(rarityColor.copy(alpha = 0.2f))
                        .border(1.dp, rarityColor, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        if (isAr) "مجهّز ✓" else "Equipped ✓",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = rarityColor
                    )
                }
            } else if (isUnlocked) {
                Text(if (isAr) "تجهيز" else "Equip", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = equipColor)
            }
        }
    }
}

@Composable
private fun TitleItemCard(
    title: TitleItem,
    currentLang: String,
    isDarkTheme: Boolean = LocalAppDarkTheme.current,
    isUnlocked: Boolean,
    isEquipped: Boolean,
    onEquip: () -> Unit
) {
    val isAr = currentLang == "ar"
    val rarityColor = title.rarity.adaptiveColor(isDarkTheme)
    val equipColor = if (isDarkTheme) NeonCyan else DayCyan
    val unlockHintColor = if (isDarkTheme) NeonAmber else DayOrange
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isEquipped) rarityColor.copy(alpha = if (isDarkTheme) 0.15f else 0.12f)
                else if (isDarkTheme) Color(0x181E293B) else Color(0xF5FFFFFF)
            )
            .border(
                1.dp,
                if (isEquipped) rarityColor else if (isDarkTheme) Color(0x2238BDF8) else Color(0x55CBD5E1),
                RoundedCornerShape(12.dp)
            )
            .clickable(enabled = isUnlocked && !isEquipped, onClick = onEquip)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title.getLocalizedTitle(currentLang),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isUnlocked) rarityColor else Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(rarityColor.copy(alpha = 0.15f))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = if (isAr) title.rarity.labelAr else title.rarity.labelEn,
                            fontSize = 8.sp,
                            color = rarityColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                val titleDesc = title.getLocalizedDescription(currentLang)
                if (titleDesc.isNotBlank()) {
                    Text(
                        text = titleDesc,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (!isUnlocked && title.unlockStation != null) {
                    Text(
                        text = if (isAr) "يُفتح عند إتمام المحطة #${title.unlockStation}" else "Unlocks at Station #${title.unlockStation}",
                        fontSize = 9.sp,
                        color = unlockHintColor
                    )
                }
            }

            if (isEquipped) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(rarityColor.copy(alpha = 0.2f))
                        .border(1.dp, rarityColor, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        if (isAr) "مجهّز ✓" else "Equipped ✓",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = rarityColor
                    )
                }
            } else if (isUnlocked) {
                Text(if (isAr) "تجهيز" else "Equip", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = equipColor)
            } else {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = if (isAr) "مقفل" else "Locked",
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun PotionCard(
    name: String,
    desc: String,
    count: Int,
    statusNote: String,
    color: Color,
    canUse: Boolean,
    currentLang: String,
    isDarkTheme: Boolean = LocalAppDarkTheme.current,
    onUse: () -> Unit
) {
    val isAr = currentLang == "ar"
    val buttonContentLuminance = color.red * 0.299f + color.green * 0.587f + color.blue * 0.114f
    val buttonContentColor = if (buttonContentLuminance > 0.55f) Color(0xFF0F172A) else Color.White
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isDarkTheme) Color(0x181E293B) else Color(0xF5FFFFFF))
            .border(1.dp, color.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(color.copy(alpha = if (isDarkTheme) 0.2f else 0.14f))
                            .border(1.dp, color, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Science, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(text = name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            text = if (isAr) "المخزون: $count / 3" else "Stock: $count / 3",
                            fontSize = 10.sp,
                            color = color,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (canUse) {
                    Button(
                        onClick = onUse,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = color,
                            contentColor = buttonContentColor
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            if (isAr) "تفعيل" else "Activate",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(text = desc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

            if (statusNote.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = statusNote, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color)
            }
        }
    }
}

@Composable
private fun KeyTile(
    modifier: Modifier = Modifier,
    label: String,
    uses: String,
    count: Int,
    color: Color
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(imageVector = Icons.Default.VpnKey, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
            Text(text = "$count / 30", fontSize = 12.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
            Text(text = uses, fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
