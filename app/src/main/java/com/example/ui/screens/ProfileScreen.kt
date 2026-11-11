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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val isHapticEnabled by viewModel.isHapticFeedbackEnabled

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
    val darkRank = remember(flameScore) { DeviationsManager.getDarkRank(flameScore) }
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
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0x221E293B))
                        .border(1.dp, Color(0x4038BDF8), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = NeonCyan
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "الملف الشخصي والعتاد",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = PlusJakartaSans,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Cyber Profile & Arsenal Matrix",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonCyan,
                        fontSize = 11.sp
                    )
                }
            }

            // Edit username button
            IconButton(
                onClick = {
                    tempName = userName
                    showEditNameDialog = true
                },
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0x221E293B))
                    .border(1.dp, Color(0x4038BDF8), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Name",
                    tint = NeonCyan,
                    modifier = Modifier.size(18.dp)
                )
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
                    shape = RoundedCornerShape(22.dp)
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
                            Text(
                                text = userName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                fontFamily = PlusJakartaSans,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "المستوى الحالي: محطة #$unlockedStation",
                                style = MaterialTheme.typography.labelMedium,
                                color = NeonAmber,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 2. DYNAMIC TITLE CARD
            item {
                val rarity = equippedTitle.rarity
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0x181E293B))
                        .border(
                            1.5.dp,
                            rarity.primaryColor.copy(alpha = titleGlowPulse),
                            RoundedCornerShape(18.dp)
                        )
                        .padding(14.dp)
                ) {
                    // Background ambient particle glow
                    Canvas(modifier = Modifier.matchParentSize()) {
                        drawCircle(
                            color = rarity.glowColor.copy(alpha = 0.15f * titleGlowPulse),
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
                                    tint = rarity.primaryColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "اللقب المجهز (${rarity.labelAr})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = rarity.primaryColor
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(rarity.primaryColor.copy(alpha = 0.2f))
                                    .border(1.dp, rarity.primaryColor.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = rarity.labelEn.uppercase(),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = rarity.primaryColor
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = equippedTitle.titleAr,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = PlusJakartaSans,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = equippedTitle.titleEn,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (equippedTitle.description.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = equippedTitle.description,
                                fontSize = 11.sp,
                                color = NeonCyan.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            // 3. STRIKERS METRICS PANEL
            item {
                LiquidGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp)
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
                                tint = NeonCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "مؤشرات وتيليمتري اللاعب (Strikers Metrics)",
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
                            // Flame Score / 20000
                            MetricTile(
                                modifier = Modifier.weight(1f),
                                label = "نقاط الشعلة",
                                value = "${String.format(Locale.US, "%.0f", flameScore)} / 20k",
                                sub = "${String.format(Locale.US, "%.1f", (flameScore / 20000.0) * 100)}%",
                                icon = Icons.Default.LocalFireDepartment,
                                color = NeonAmber
                            )

                            // Shadow Grip %
                            MetricTile(
                                modifier = Modifier.weight(1f),
                                label = "قبضة الظل",
                                value = "${String.format(Locale.US, "%.1f", shadowGripPercent)}%",
                                sub = darkRank.title,
                                icon = Icons.Default.Shield,
                                color = if (shadowGripPercent > 50f) Color(0xFFEF4444) else NeonEmerald
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Total XP
                            MetricTile(
                                modifier = Modifier.weight(1f),
                                label = "إجمالي الـ XP",
                                value = "$totalXp XP",
                                sub = "من 434,725 XP",
                                icon = Icons.Default.Bolt,
                                color = NeonCyan
                            )

                            // Active Station
                            MetricTile(
                                modifier = Modifier.weight(1f),
                                label = "المحطة النشطة",
                                value = "محطة #$unlockedStation",
                                sub = "من 500 محطة",
                                icon = Icons.Default.AutoAwesome,
                                color = NeonViolet
                            )
                        }
                    }
                }
            }

            // 4. TAB SELECTOR (Avatars / Titles / Arsenal)
            item {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0x181E293B),
                    contentColor = NeonCyan,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = NeonCyan,
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
                                "الأفاتار (${unlockedAvatars.size}/20)",
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
                                "الألقاب (${unlockedTitles.size}/50)",
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
                                "الجرعات والمفاتيح",
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
                            text = "الجرعات السيبرانية (Cybernetic Potions - الحد الأقصى: 3 لكل نوع)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonAmber
                        )

                        // 1. Pulse Elixir
                        PotionCard(
                            name = "إكسير النبض (Pulse Elixir)",
                            desc = "+5 نقاط إضافية لزر Deflame للـ 3 تفعيلات القادمة",
                            count = pulseElixirCount,
                            statusNote = if (pulseUsesLeft > 0) "نشط حالياً ($pulseUsesLeft استخدامات متبقية)" else "",
                            color = Color(0xFF38BDF8),
                            canUse = pulseElixirCount > 0,
                            onUse = {
                                ProfileManager.usePulseElixir(context)
                            }
                        )

                        // 2. Shadow-Grip Serum
                        PotionCard(
                            name = "مصل قبضة الظل (Shadow-Grip Serum)",
                            desc = "يقلل وتيرة صعود نقاط الشعلة بنسبة 50% لمدة 3 أيام",
                            count = shadowSerumCount,
                            statusNote = if (isShadowActive) "نشط حالياً (حماية 50% لمدة 3 أيام)" else "",
                            color = Color(0xFFA855F7),
                            canUse = shadowSerumCount > 0 && !isShadowActive,
                            onUse = {
                                ProfileManager.useShadowSerum(context)
                            }
                        )

                        // 3. Gabriel's Purity Droplet
                        PotionCard(
                            name = "قطرة نقاء جبريل (Gabriel's Droplet)",
                            desc = "تسارع نمو نقاط الشعلة بنسبة 50% لمدة 7 أيام (تفعيل تلقائي وفوري)",
                            count = gabrielDropletCount,
                            statusNote = if (isGabrielActive) "نشط تلقائياً (تسارع 50% لمدة 7 أيام)" else "خامل",
                            color = Color(0xFFFACC15),
                            canUse = false,
                            onUse = {}
                        )

                        // 4. Absolute Cryo-Infusion
                        PotionCard(
                            name = "الحقن التجميدي المطلق (Cryo-Infusion)",
                            desc = "+100 نقطة بونص لتعزيز الستريك التالي (تفعيل اختياري)",
                            count = cryoInfusionCount,
                            statusNote = if (ProfileManager.hasCryoBuffArmed.value) "جاهز ومسلح للستريك القادم (+100)" else "",
                            color = Color(0xFF00E5FF),
                            canUse = cryoInfusionCount > 0 && !ProfileManager.hasCryoBuffArmed.value,
                            onUse = {
                                ProfileManager.useCryoInfusion(context)
                            }
                        )

                        // 5. Eternal Rebirth Elixir
                        PotionCard(
                            name = "جرعة الانبعاث الأبدي (Eternal Rebirth)",
                            desc = "جائزة المحطة 500: مناعة أبدية ضد التصفير وفتح لقب Gary Master",
                            count = eternalRebirthCount,
                            statusNote = if (ProfileManager.hasRebirthImmunity.value) "مفعل: مناعة أبدية وسيد النقوشة" else "",
                            color = Color(0xFFFF007A),
                            canUse = eternalRebirthCount > 0 && !ProfileManager.hasRebirthImmunity.value,
                            onUse = {
                                ProfileManager.useEternalRebirth(context)
                            }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "مفاتيح تجاوز المهام (Skip Keys - السعة القصوى: 30 مفتاح)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            KeyTile(modifier = Modifier.weight(1f), label = "خشبي", uses = "استخدام 1", count = woodenKeys, color = Color(0xFFB45309))
                            KeyTile(modifier = Modifier.weight(1f), label = "برونزي", uses = "استخدامين", count = bronzeKeys, color = Color(0xFFD97706))
                            KeyTile(modifier = Modifier.weight(1f), label = "فضي", uses = "3 استخدامات", count = silverKeys, color = Color(0xFFCBD5E1))
                        }
                    }
                }
            }
        }
    }

    // Edit username dialog
    if (showEditNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = {
                Text(
                    text = "تعديل اسم المستخدم السيبراني",
                    fontWeight = FontWeight.Bold,
                    fontFamily = PlusJakartaSans
                )
            },
            text = {
                OutlinedTextField(
                    value = tempName,
                    onValueChange = { tempName = it },
                    label = { Text("الاسم") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = Color(0x5538BDF8)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        ProfileManager.setUserName(context, tempName)
                        showEditNameDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                ) {
                    Text("حفظ", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) {
                    Text("إلغاء", color = Color(0xFF94A3B8))
                }
            }
        )
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
    isUnlocked: Boolean,
    isEquipped: Boolean,
    onEquip: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isEquipped) avatar.rarity.primaryColor.copy(alpha = 0.15f) else Color(0x181E293B))
            .border(
                1.dp,
                if (isEquipped) avatar.rarity.primaryColor else Color(0x2238BDF8),
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
                        .background(Color(0xFF0F172A))
                        .border(
                            1.5.dp,
                            if (isUnlocked) avatar.rarity.primaryColor else Color(0xFF64748B),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isUnlocked) {
                        Icon(
                            imageVector = getAvatarVector(avatar.badgeIconName),
                            contentDescription = avatar.nameAr,
                            tint = avatar.rarity.primaryColor,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Locked",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = avatar.nameAr,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isUnlocked) MaterialTheme.colorScheme.onSurface else Color(0xFF64748B)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(avatar.rarity.primaryColor.copy(alpha = 0.15f))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = avatar.rarity.labelAr,
                                fontSize = 8.sp,
                                color = avatar.rarity.primaryColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = if (isUnlocked) avatar.nameEn else "يُفتح عند المحطة #${avatar.unlockStation}",
                        fontSize = 10.sp,
                        color = if (isUnlocked) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF94A3B8)
                    )
                }
            }

            if (isEquipped) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(avatar.rarity.primaryColor.copy(alpha = 0.2f))
                        .border(1.dp, avatar.rarity.primaryColor, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("مجهز ✓", fontSize = 10.sp, fontWeight = FontWeight.Black, color = avatar.rarity.primaryColor)
                }
            } else if (isUnlocked) {
                Text("تجهيز", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
            }
        }
    }
}

@Composable
private fun TitleItemCard(
    title: TitleItem,
    isUnlocked: Boolean,
    isEquipped: Boolean,
    onEquip: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isEquipped) title.rarity.primaryColor.copy(alpha = 0.15f) else Color(0x181E293B))
            .border(
                1.dp,
                if (isEquipped) title.rarity.primaryColor else Color(0x2238BDF8),
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
                        text = title.titleAr,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isUnlocked) title.rarity.primaryColor else Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(title.rarity.primaryColor.copy(alpha = 0.15f))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = title.rarity.labelAr,
                            fontSize = 8.sp,
                            color = title.rarity.primaryColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(
                    text = title.titleEn,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!isUnlocked && title.unlockStation != null) {
                    Text(
                        text = "يُفتح عند إتمام المحطة #${title.unlockStation}",
                        fontSize = 9.sp,
                        color = NeonAmber
                    )
                }
            }

            if (isEquipped) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(title.rarity.primaryColor.copy(alpha = 0.2f))
                        .border(1.dp, title.rarity.primaryColor, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("مجهز ✓", fontSize = 10.sp, fontWeight = FontWeight.Black, color = title.rarity.primaryColor)
                }
            } else if (isUnlocked) {
                Text("تجهيز", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
            } else {
                Icon(imageVector = Icons.Default.Lock, contentDescription = "Locked", tint = Color(0xFF64748B), modifier = Modifier.size(16.dp))
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
    onUse: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x181E293B))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
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
                            .background(color.copy(alpha = 0.2f))
                            .border(1.dp, color, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Science, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(text = name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text(text = "المخزون: $count / 3", fontSize = 10.sp, color = color, fontWeight = FontWeight.Bold)
                    }
                }

                if (canUse) {
                    Button(
                        onClick = onUse,
                        colors = ButtonDefaults.buttonColors(containerColor = color),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text("تفعيل", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
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
