package com.example.ui.screens

import android.view.SoundEffectConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.flame.ActiveShieldType
import com.example.data.flame.FlameManager
import com.example.data.profile.PotionType
import com.example.data.profile.ProfileManager
import com.example.data.profile.SkipKeyType
import com.example.ui.components.LiquidGlassCard
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonRose
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.PlusJakartaSans
import com.example.ui.viewmodel.IndividualViewModel
import com.example.util.HapticHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private data class InventoryTooltipSpec(
    val title: String,
    val subtitle: String,
    val rarityLabel: String,
    val accentColor: Color,
    val secondaryColor: Color,
    val icon: ImageVector,
    val mechanics: String,
    val bonuses: String,
    val expirationRules: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    viewModel: IndividualViewModel,
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val isHapticEnabled by viewModel.isHapticFeedbackEnabled
    val currentLang by viewModel.currentLanguage.collectAsStateWithLifecycle()
    val isAr = currentLang == "ar"

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
        while (true) {
            remainingMillis = FlameManager.getActiveShieldRemainingMillis()
            delay(1000L)
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

    // 2-Second Long-Press Tooltip Sheet State & Guiding Hint Dialog State
    var selectedTooltip by remember { mutableStateOf<InventoryTooltipSpec?>(null) }
    var showLongPressGuideDialog by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Infinite luxury animation driver
    val infiniteTransition = rememberInfiniteTransition(label = "inventory_luxury_anim")
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

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("inventory_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header Banner
        item {
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(NeonCyan.copy(alpha = 0.25f), NeonViolet.copy(alpha = 0.25f))
                                    )
                                )
                                .border(1.2.dp, NeonCyan.copy(alpha = 0.7f), RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Inventory2,
                                contentDescription = "Cyber Inventory",
                                tint = NeonCyan,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (isAr) "المخزون السيبراني" else "Cyber Inventory",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = PlusJakartaSans,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isAr) "الدروع • مفاتيح التجاوز • الجرعات التكتيكية" else "Shields • Skip-Keys • Tactical Potions",
                                fontSize = 11.sp,
                                color = NeonCyan
                            )
                        }
                    }
                }
            }
        }

        // ============================================================
        // SECTION 1: PROTECTIVE SHIELDS (24h, 48h, 72h, Luxury 7-Day)
        // ============================================================
        item {
            SectionHeaderLabel(
                title = if (isAr) "الدروع الوقائية" else "Protective Shields",
                subtitle = if (shieldActuallyActive) {
                    val shieldLabel = if (isAr) activeShieldType.labelAr else activeShieldType.labelEn
                    if (isAr) "الدرع النشط: $shieldLabel (${FlameManager.formatRemaining(remainingMillis)})"
                    else "Active Shield: $shieldLabel (${FlameManager.formatRemaining(remainingMillis)})"
                } else {
                    if (isAr) "لا يوجد درع نشط حالياً" else "No active shield currently"
                },
                accentColor = NeonEmerald,
                icon = Icons.Default.Shield,
                trailingContent = {
                    IconButton(
                        onClick = {
                            HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                            view.playSoundEffect(SoundEffectConstants.CLICK)
                            showLongPressGuideDialog = true
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(NeonViolet.copy(alpha = 0.16f))
                            .border(1.2.dp, NeonViolet.copy(alpha = 0.7f), CircleShape)
                            .testTag("inventory_longpress_hint_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.TouchApp,
                            contentDescription = if (isAr) "دليل الاستخدام" else "Usage Guide",
                            tint = NeonViolet,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            )
        }

        // Luxury 7-Day Automated Strike Shield Card
        item {
            val is7dActive = shieldActuallyActive && activeShieldType == ActiveShieldType.SHIELD_7D_AUTO
            Luxury7DayAutoShieldCard(
                isAr = isAr,
                isActiveNow = is7dActive,
                remainingText = if (is7dActive) FlameManager.formatRemaining(remainingMillis) else null,
                totalTriggeredCount = sevenDayAutoTriggers,
                animAngle = animAngle,
                pulseWave = pulseWave,
                onLongPress2s = {
                    selectedTooltip = if (isAr) {
                        InventoryTooltipSpec(
                            title = "درع الـ 7 أيام التلقائي",
                            subtitle = "تفعيل تلقائي نخبوي",
                            rarityLabel = "فئة نخبوية",
                            accentColor = Color(0xFFFFD700),
                            secondaryColor = Color(0xFFFF007A),
                            icon = Icons.Default.VerifiedUser,
                            mechanics = "درع وقائي أوتوماتيكي نخبوي يعمل كصمام أمان عالي المستوى عند التعرض لضربات مكثفة في يوم واحد.",
                            bonuses = "يُفعّل تلقائياً وفورياً عند تسجيل 3 إلى 5 سترايكات في نفس اليوم، ليوقف إضافة +3 نقاط خمول يومية لمدة 7 أيام كاملة (168 ساعة).",
                            expirationRules = "تبدأ مدة الـ 7 أيام تلقائياً لحظة تفعيل الكومبو وتنتهي بعد انقضاء 168 ساعة، ولا يمكن تخزينه يدوياً."
                        )
                    } else {
                        InventoryTooltipSpec(
                            title = "7-Day Automated Strike Shield",
                            subtitle = "Luxury Auto Defense",
                            rarityLabel = "LUXURY TIER",
                            accentColor = Color(0xFFFFD700),
                            secondaryColor = Color(0xFFFF007A),
                            icon = Icons.Default.VerifiedUser,
                            mechanics = "An elite automatic protective shield that acts as a high-tier safeguard during intense single-day strikes.",
                            bonuses = "Activates instantly when 3 to 5 strikes are recorded in the same day, halting the +3 daily inactivity accrual for 7 full days (168 hours).",
                            expirationRules = "The 7-day timer starts immediately upon combo trigger and expires after 168 hours. Cannot be stored manually."
                        )
                    }
                }
            )
        }

        // 24h, 48h, 72h Protective Shields
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                InventoryShieldCard(
                    title = if (isAr) "درع الـ 24 ساعة" else "24h Protective Shield",
                    rarity = if (isAr) "شائع" else "COMMON",
                    isAr = isAr,
                    count = shields24h,
                    isCurrentlyActive = shieldActuallyActive && activeShieldType == ActiveShieldType.SHIELD_24H,
                    accentColor = NeonCyan,
                    icon = Icons.Default.Shield,
                    onActivate = {
                        HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                        view.playSoundEffect(SoundEffectConstants.CLICK)
                        viewModel.activate1DayShield()
                    },
                    onLongPress2s = {
                        selectedTooltip = if (isAr) {
                            InventoryTooltipSpec(
                                title = "درع الـ 24 ساعة",
                                subtitle = "حماية تكتيكية ليوم واحد",
                                rarityLabel = "شائع",
                                accentColor = NeonCyan,
                                secondaryColor = NeonEmerald,
                                icon = Icons.Default.Shield,
                                mechanics = "درع تكتيكي يجمّد عداد الخمول اليومي مؤقتاً لحماية رصيد الشعلة.",
                                bonuses = "يمنع إضافة +3 نقاط خمول يومية لمدة 24 ساعة متواصلة، ويُفعّل فورياً عند استلامه من المسار.",
                                expirationRules = "ينتهي مفعوله بعد 24 ساعة من لحظة التفعيل. إذا كان هناك درع نشط، تُضاف الـ 24 ساعة إلى وقت الانتهاء."
                            )
                        } else {
                            InventoryTooltipSpec(
                                title = "24-Hour Protective Shield",
                                subtitle = "1-Day Tactical Barrier",
                                rarityLabel = "COMMON",
                                accentColor = NeonCyan,
                                secondaryColor = NeonEmerald,
                                icon = Icons.Default.Shield,
                                mechanics = "A tactical barrier that temporarily freezes daily inactivity accrual to protect your Flame score.",
                                bonuses = "Prevents +3 daily inactivity points for 24 continuous hours and auto-activates when claimed.",
                                expirationRules = "Expires 24 hours after activation. If a shield is already active, adds 24 hours to its remaining duration."
                            )
                        }
                    },
                    testTag = "inv_shield_24h"
                )

                InventoryShieldCard(
                    title = if (isAr) "درع الـ 48 ساعة" else "48h Protective Shield",
                    rarity = if (isAr) "غير شائع" else "UNCOMMON",
                    isAr = isAr,
                    count = shields48h,
                    isCurrentlyActive = shieldActuallyActive && activeShieldType == ActiveShieldType.SHIELD_48H,
                    accentColor = NeonEmerald,
                    icon = Icons.Default.Shield,
                    onActivate = {
                        HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                        view.playSoundEffect(SoundEffectConstants.CLICK)
                        viewModel.activate48hShield()
                    },
                    onLongPress2s = {
                        selectedTooltip = if (isAr) {
                            InventoryTooltipSpec(
                                title = "درع الـ 48 ساعة",
                                subtitle = "حاجز سيبراني متوسط المدى",
                                rarityLabel = "غير شائع",
                                accentColor = NeonEmerald,
                                secondaryColor = NeonCyan,
                                icon = Icons.Default.Shield,
                                mechanics = "حاجز سيبراني متوسط المدى يحمي استقرار الشعلة عبر يومين متتاليين.",
                                bonuses = "يوقف تزايد نقاط الخمول (+3 نقاط/يوم) لمدة 48 ساعة متواصلة، مع أولوية التفعيل التلقائي عند الاستلام.",
                                expirationRules = "صلاحية الحماية تمتد لـ 48 ساعة متصلة؛ وفي حال وجود درع نشط مسبقاً تتراكم المدة تلقائياً."
                            )
                        } else {
                            InventoryTooltipSpec(
                                title = "48-Hour Protective Shield",
                                subtitle = "Mid-Range Cyber Barrier",
                                rarityLabel = "UNCOMMON",
                                accentColor = NeonEmerald,
                                secondaryColor = NeonCyan,
                                icon = Icons.Default.Shield,
                                mechanics = "A mid-range cyber barrier safeguarding Flame stability across two consecutive days.",
                                bonuses = "Halts inactivity growth (+3 pts/day) for 48 consecutive hours.",
                                expirationRules = "Protection lasts 48 consecutive hours and stacks onto any currently active shield."
                            )
                        }
                    },
                    testTag = "inv_shield_48h"
                )

                InventoryShieldCard(
                    title = if (isAr) "درع الـ 72 ساعة" else "72h Protective Shield",
                    rarity = if (isAr) "نادر" else "RARE",
                    isAr = isAr,
                    count = shields72h,
                    isCurrentlyActive = shieldActuallyActive && activeShieldType == ActiveShieldType.SHIELD_72H,
                    accentColor = NeonAmber,
                    icon = Icons.Default.VerifiedUser,
                    onActivate = {
                        HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                        view.playSoundEffect(SoundEffectConstants.CLICK)
                        viewModel.activateManualShield()
                    },
                    onLongPress2s = {
                        selectedTooltip = if (isAr) {
                            InventoryTooltipSpec(
                                title = "درع الـ 72 ساعة",
                                subtitle = "درع حماية عالي التحمل",
                                rarityLabel = "نادر",
                                accentColor = NeonAmber,
                                secondaryColor = Color(0xFFEA580C),
                                icon = Icons.Default.VerifiedUser,
                                mechanics = "درع حماية عالي التحمل يُمنح من مكافآت المحطات المتقدمة أو كتعويض عند عقوبة الضربة المزدوجة.",
                                bonuses = "يجمّد نقاط الخمول اليومية (+3 نقاط/يوم) لمدة 3 أيام كاملة (72 ساعة).",
                                expirationRules = "يُستهلك الدرع عند التفعيل اليدوي أو الفوري ويمتد مفعوله 72 ساعة كاملة من التوقيت الحالي أو الممدد."
                            )
                        } else {
                            InventoryTooltipSpec(
                                title = "72-Hour Protective Shield",
                                subtitle = "Heavy-Duty Armor",
                                rarityLabel = "RARE",
                                accentColor = NeonAmber,
                                secondaryColor = Color(0xFFEA580C),
                                icon = Icons.Default.VerifiedUser,
                                mechanics = "Heavy-duty shield earned from advanced station rewards or Double Strike compensation.",
                                bonuses = "Freezes daily inactivity points (+3 pts/day) for 3 full days (72 hours).",
                                expirationRules = "Consumed on manual or instant activation and extends protection for 72 full hours."
                            )
                        }
                    },
                    testTag = "inv_shield_72h"
                )
            }
        }

        // ============================================================
        // SECTION 2: SKIP-KEYS (Wooden, Bronze, Silver)
        // ============================================================
        item {
            Spacer(modifier = Modifier.height(4.dp))
            SectionHeaderLabel(
                title = if (isAr) "مفاتيح التجاوز" else "Skip-Keys",
                subtitle = if (isAr) "السعة القصوى لكل فئة: 30 مفتاحاً (تُستثنى المهام السنوية)" else "Max Capacity: 30 per tier (Annual tasks excluded)",
                accentColor = NeonAmber,
                icon = Icons.Default.VpnKey
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                InventoryKeyTile(
                    modifier = Modifier.weight(1f),
                    title = SkipKeyType.WOODEN.getLocalizedName(currentLang),
                    rarity = if (isAr) "شائع" else "COMMON",
                    usesLabel = if (isAr) "استخدام واحد" else "1 Use",
                    count = woodenKeys,
                    accentColor = Color(0xFFD97706),
                    onLongPress2s = {
                        selectedTooltip = if (isAr) {
                            InventoryTooltipSpec(
                                title = "المفتاح الخشبي",
                                subtitle = "مفتاح تجاوز قياسي",
                                rarityLabel = "شائع",
                                accentColor = Color(0xFFD97706),
                                secondaryColor = Color(0xFFB45309),
                                icon = Icons.Default.VpnKey,
                                mechanics = "مفتاح فك شيفرة قياسي لتجاوز المهام اليومية والأسبوعية البسيطة وحصد نقاط الخبرة فوراً.",
                                bonuses = "يمنح استخداماً واحداً لإكمال مهمة متوافقة فوراً مع الحصول على كامل مكافأة الخبرة الخاصة بها.",
                                expirationRules = "لا تنتهي صلاحيته بالتخزين (الحد الأقصى 30 مفتاحاً). ملاحظة صارمة: لا يمكن استخدامه لتجاوز المهام السنوية."
                            )
                        } else {
                            InventoryTooltipSpec(
                                title = "Wooden Skip-Key",
                                subtitle = "Standard Bypass Key",
                                rarityLabel = "COMMON",
                                accentColor = Color(0xFFD97706),
                                secondaryColor = Color(0xFFB45309),
                                icon = Icons.Default.VpnKey,
                                mechanics = "Standard decryption key to bypass basic Daily and Weekly tasks and claim XP immediately.",
                                bonuses = "Provides 1 use to complete a compatible task immediately with full XP bounty.",
                                expirationRules = "Never expires in storage (Max 30 keys). Strictly prohibited on Annual tasks."
                            )
                        }
                    },
                    testTag = "inv_key_wooden"
                )

                InventoryKeyTile(
                    modifier = Modifier.weight(1f),
                    title = SkipKeyType.BRONZE.getLocalizedName(currentLang),
                    rarity = if (isAr) "غير شائع" else "UNCOMMON",
                    usesLabel = if (isAr) "استخدامان" else "2 Uses",
                    count = bronzeKeys,
                    accentColor = Color(0xFFF59E0B),
                    onLongPress2s = {
                        selectedTooltip = if (isAr) {
                            InventoryTooltipSpec(
                                title = "المفتاح البرونزي",
                                subtitle = "مفتاح تجاوز مطوّر",
                                rarityLabel = "غير شائع",
                                accentColor = Color(0xFFF59E0B),
                                secondaryColor = Color(0xFFEA580C),
                                icon = Icons.Default.VpnKey,
                                mechanics = "مفتاح مطوّر يسمح بتخطي المهام ذات الصعوبة المتوسطة ضمن المهام اليومية والأسبوعية والشهرية.",
                                bonuses = "يملك قدرة استخدامين لتمرير المهام المؤهلة وحصد نقاط الخبرة دون تأخير.",
                                expirationRules = "دائم في المخزون حتى استهلاكه (السعة القصوى 30 مفتاحاً). ممنوع استخدامه في المهام السنوية."
                            )
                        } else {
                            InventoryTooltipSpec(
                                title = "Bronze Skip-Key",
                                subtitle = "Upgraded Bypass Key",
                                rarityLabel = "UNCOMMON",
                                accentColor = Color(0xFFF59E0B),
                                secondaryColor = Color(0xFFEA580C),
                                icon = Icons.Default.VpnKey,
                                mechanics = "Upgraded key capable of bypassing medium-difficulty tasks in Daily, Weekly, and Monthly pools.",
                                bonuses = "Provides 2 uses to clear eligible tasks and harvest XP without delay.",
                                expirationRules = "Permanent in inventory until consumed (Max 30 keys). Cannot skip Annual tasks."
                            )
                        }
                    },
                    testTag = "inv_key_bronze"
                )

                InventoryKeyTile(
                    modifier = Modifier.weight(1f),
                    title = SkipKeyType.SILVER.getLocalizedName(currentLang),
                    rarity = if (isAr) "نادر" else "RARE",
                    usesLabel = if (isAr) "3 استخدامات" else "3 Uses",
                    count = silverKeys,
                    accentColor = Color(0xFFE2E8F0),
                    onLongPress2s = {
                        selectedTooltip = if (isAr) {
                            InventoryTooltipSpec(
                                title = "المفتاح الفضي",
                                subtitle = "مفتاح اختراق رفيع المستوى",
                                rarityLabel = "نادر",
                                accentColor = Color(0xFFE2E8F0),
                                secondaryColor = NeonCyan,
                                icon = Icons.Default.VpnKey,
                                mechanics = "مفتاح اختراق رفيع المستوى قادر على تجاوز المهام عالية المتطلبات في القوائم الدورية.",
                                bonuses = "يمتلك 3 استخدامات مع أقصى كفاءة لحصد مكافآت الخبرة العالية فوراً.",
                                expirationRules = "يبقى محفوظاً حتى استنفاد شحناته (الحد الأقصى 30 مفتاحاً). لا يشمل المهام السنوية التي تتطلب إنجازاً فعلياً."
                            )
                        } else {
                            InventoryTooltipSpec(
                                title = "Silver Skip-Key",
                                subtitle = "High-Tier Breach Key",
                                rarityLabel = "RARE",
                                accentColor = Color(0xFFE2E8F0),
                                secondaryColor = NeonCyan,
                                icon = Icons.Default.VpnKey,
                                mechanics = "High-tier breach key capable of bypassing high-requirement recurring tasks.",
                                bonuses = "Carries 3 uses for maximum efficiency in harvesting high XP bounties.",
                                expirationRules = "Retained until charges are exhausted (Max 30 keys). Excludes Annual tasks."
                            )
                        }
                    },
                    testTag = "inv_key_silver"
                )
            }
        }

        // ============================================================
        // SECTION 3: CYBERNETIC POTIONS (Tiers 1 - 4)
        // ============================================================
        item {
            Spacer(modifier = Modifier.height(4.dp))
            SectionHeaderLabel(
                title = if (isAr) "الجرعات السيبرانية" else "Cybernetic Potions",
                subtitle = if (isAr) "السعة القصوى: 3 جرعات لكل فئة • التفاعلات الكيميائية الحيوية" else "Max Capacity: 3 per tier • Biochemical Enhancers",
                accentColor = NeonCyan,
                icon = Icons.Default.Science
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // 1. Pulse Elixir
                InventoryPotionCard(
                    title = PotionType.PULSE_ELIXIR.getLocalizedName(currentLang),
                    subtitle = if (isAr) "الفئة الأولى" else "Tier 1",
                    rarity = if (isAr) "شائع" else "COMMON",
                    isAr = isAr,
                    count = pulseElixirCount,
                    statusBadge = if (pulseUsesLeft > 0) {
                        if (isAr) "نشط ($pulseUsesLeft استخدامات)" else "Active ($pulseUsesLeft uses)"
                    } else null,
                    accentColor = PotionType.PULSE_ELIXIR.color,
                    icon = Icons.Default.Bolt,
                    canActivate = pulseElixirCount > 0,
                    onActivate = {
                        HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                        view.playSoundEffect(SoundEffectConstants.CLICK)
                        ProfileManager.usePulseElixir(context)
                    },
                    onLongPress2s = {
                        selectedTooltip = if (isAr) {
                            InventoryTooltipSpec(
                                title = PotionType.PULSE_ELIXIR.nameAr,
                                subtitle = "الفئة الأولى",
                                rarityLabel = "شائع",
                                accentColor = PotionType.PULSE_ELIXIR.color,
                                secondaryColor = NeonEmerald,
                                icon = Icons.Default.Bolt,
                                mechanics = "منشّط نبضي يضاعف فاعلية زر التبريد لخفض طاقة الشعلة بسرعة أكبر.",
                                bonuses = "يضيف +5 نقاط خصم إضافية لكل ضغطة على زر التبريد خلال التفعيلات الثلاث القادمة.",
                                expirationRules = "ينتهي تأثيره تلقائياً بمجرد استهلاك التفعيلات الثلاث المتتالية."
                            )
                        } else {
                            InventoryTooltipSpec(
                                title = PotionType.PULSE_ELIXIR.nameEn,
                                subtitle = "Tier 1 Enhancer",
                                rarityLabel = "COMMON",
                                accentColor = PotionType.PULSE_ELIXIR.color,
                                secondaryColor = NeonEmerald,
                                icon = Icons.Default.Bolt,
                                mechanics = "Pulse booster that amplifies the Deflame cooldown button for faster Flame reduction.",
                                bonuses = "Adds +5 bonus deduction points per Deflame press for the next 3 activations.",
                                expirationRules = "Automatically expires once the 3 consecutive Deflame uses are consumed."
                            )
                        }
                    },
                    testTag = "inv_potion_pulse"
                )

                // 2. Shadow-Grip Serum
                InventoryPotionCard(
                    title = PotionType.SHADOW_GRIP_SERUM.getLocalizedName(currentLang),
                    subtitle = if (isAr) "الفئة الثانية" else "Tier 2",
                    rarity = if (isAr) "غير شائع" else "UNCOMMON",
                    isAr = isAr,
                    count = shadowSerumCount,
                    statusBadge = if (isShadowActive) {
                        if (isAr) "نشط (تبطيء 50%)" else "Active (50% Slowdown)"
                    } else null,
                    accentColor = PotionType.SHADOW_GRIP_SERUM.color,
                    icon = Icons.Default.Science,
                    canActivate = shadowSerumCount > 0 && !isShadowActive,
                    onActivate = {
                        HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                        view.playSoundEffect(SoundEffectConstants.CLICK)
                        ProfileManager.useShadowSerum(context)
                    },
                    onLongPress2s = {
                        selectedTooltip = if (isAr) {
                            InventoryTooltipSpec(
                                title = PotionType.SHADOW_GRIP_SERUM.nameAr,
                                subtitle = "الفئة الثانية",
                                rarityLabel = "غير شائع",
                                accentColor = PotionType.SHADOW_GRIP_SERUM.color,
                                secondaryColor = Color(0xFF7E22CE),
                                icon = Icons.Default.Science,
                                mechanics = "مصل كابح للانجراف الحراري يقوم بتهدئة تسارع نقاط الخمول اليومي في النظام.",
                                bonuses = "يقلّل وتيرة صعود نقاط الشعلة الناتجة عن الخمول اليومي بنسبة 50% لمدة 3 أيام متتالية.",
                                expirationRules = "يبدأ العد التنازلي لمدة 72 ساعة لحظة الحقن، ويعود معدل الخمول إلى طبيعته بعد انتهاء المدة."
                            )
                        } else {
                            InventoryTooltipSpec(
                                title = PotionType.SHADOW_GRIP_SERUM.nameEn,
                                subtitle = "Tier 2 Stabilizer",
                                rarityLabel = "UNCOMMON",
                                accentColor = PotionType.SHADOW_GRIP_SERUM.color,
                                secondaryColor = Color(0xFF7E22CE),
                                icon = Icons.Default.Science,
                                mechanics = "Thermal-drift suppressor that calms daily inactivity point acceleration.",
                                bonuses = "Reduces Flame daily inactivity climb rate by 50% for 3 consecutive days.",
                                expirationRules = "72-hour countdown begins immediately on injection and reverts automatically upon expiry."
                            )
                        }
                    },
                    testTag = "inv_potion_shadow"
                )

                // 3. Gabriel's Purity Droplet
                InventoryPotionCard(
                    title = PotionType.GABRIEL_PURITY_DROPLET.getLocalizedName(currentLang),
                    subtitle = if (isAr) "الفئة الثالثة" else "Tier 3",
                    rarity = if (isAr) "نادر" else "RARE",
                    isAr = isAr,
                    count = gabrielDropletCount,
                    statusBadge = if (isGabrielActive) {
                        if (isAr) "نشط تلقائياً (+50% نمو)" else "Auto-Active (+50% Growth)"
                    } else {
                        if (isAr) "تفعيل تلقائي عند الكسب" else "Auto-Activates on Receipt"
                    },
                    accentColor = PotionType.GABRIEL_PURITY_DROPLET.color,
                    icon = Icons.Default.WaterDrop,
                    canActivate = false,
                    onActivate = {},
                    onLongPress2s = {
                        selectedTooltip = if (isAr) {
                            InventoryTooltipSpec(
                                title = PotionType.GABRIEL_PURITY_DROPLET.nameAr,
                                subtitle = "الفئة الثالثة",
                                rarityLabel = "نادر",
                                accentColor = PotionType.GABRIEL_PURITY_DROPLET.color,
                                secondaryColor = NeonAmber,
                                icon = Icons.Default.WaterDrop,
                                mechanics = "قطرة نقاء نورانية تتفاعل تلقائياً مع النظام فور استلامها من مكافآت المسار.",
                                bonuses = "تُسرّع وتيرة نمو نقاط الشعلة بنسبة +50% خلال احتساب الخمول لدفع التقدم نحو المسار الملائكي.",
                                expirationRules = "تُفعّل تلقائياً وفورياً عند الجني دون تخزين وتستمر فعاليتها لمدة 7 أيام كاملة (168 ساعة)."
                            )
                        } else {
                            InventoryTooltipSpec(
                                title = PotionType.GABRIEL_PURITY_DROPLET.nameEn,
                                subtitle = "Tier 3 Catalyst",
                                rarityLabel = "RARE",
                                accentColor = PotionType.GABRIEL_PURITY_DROPLET.color,
                                secondaryColor = NeonAmber,
                                icon = Icons.Default.WaterDrop,
                                mechanics = "Radiant purity droplet that reacts automatically upon being claimed from Roadmap rewards.",
                                bonuses = "Accelerates Flame growth by +50% during inactivity calculation toward the Angelic path.",
                                expirationRules = "Auto-activates immediately on receipt for 7 full days (168 hours)."
                            )
                        }
                    },
                    testTag = "inv_potion_gabriel"
                )

                // 4. Absolute Cryo-Infusion
                InventoryPotionCard(
                    title = PotionType.ABSOLUTE_CRYO_INFUSION.getLocalizedName(currentLang),
                    subtitle = if (isAr) "الفئة الرابعة" else "Tier 4",
                    rarity = if (isAr) "ملحمي" else "EPIC",
                    isAr = isAr,
                    count = cryoInfusionCount,
                    statusBadge = if (hasCryoArmed) {
                        if (isAr) "مسلّح للستريك القادم (+100)" else "Armed for Next Strike (+100)"
                    } else null,
                    accentColor = PotionType.ABSOLUTE_CRYO_INFUSION.color,
                    icon = Icons.Default.AcUnit,
                    canActivate = cryoInfusionCount > 0 && !hasCryoArmed,
                    onActivate = {
                        HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                        view.playSoundEffect(SoundEffectConstants.CLICK)
                        ProfileManager.useCryoInfusion(context)
                    },
                    onLongPress2s = {
                        selectedTooltip = if (isAr) {
                            InventoryTooltipSpec(
                                title = PotionType.ABSOLUTE_CRYO_INFUSION.nameAr,
                                subtitle = "الفئة الرابعة",
                                rarityLabel = "ملحمي",
                                accentColor = PotionType.ABSOLUTE_CRYO_INFUSION.color,
                                secondaryColor = Color(0xFF0284C7),
                                icon = Icons.Default.AcUnit,
                                mechanics = "حقنة تبريد مطلقة تسلّح الستريك القادم بطاقة تعزيز استثنائية.",
                                bonuses = "تمنح +100 نقطة مكافأة لتعزيز قوة وتأثير الستريك القادم.",
                                expirationRules = "اختيارية التفعيل من المخزون؛ وتتفعل تلقائياً إذا مرّ 30 يوماً دون استخدامها، وتُستهلك بضربة واحدة."
                            )
                        } else {
                            InventoryTooltipSpec(
                                title = PotionType.ABSOLUTE_CRYO_INFUSION.nameEn,
                                subtitle = "Tier 4 Cryogenic",
                                rarityLabel = "EPIC",
                                accentColor = PotionType.ABSOLUTE_CRYO_INFUSION.color,
                                secondaryColor = Color(0xFF0284C7),
                                icon = Icons.Default.AcUnit,
                                mechanics = "Cryogenic infusion that arms your upcoming strike with exceptional boost power.",
                                bonuses = "Grants +100 bonus points to amplify your next strike.",
                                expirationRules = "Manual activation from Inventory; auto-activates after 30 days if unused and consumes on one strike."
                            )
                        }
                    },
                    testTag = "inv_potion_cryo"
                )
            }
        }

        // ============================================================
        // SECTION 4: LEGENDARY TIER 5TH POTION — ETERNAL RESURGENCE
        // ============================================================
        item {
            Spacer(modifier = Modifier.height(4.dp))
            SectionHeaderLabel(
                title = if (isAr) "الإكسير الأسطوري الفائق (الفئة 5)" else "Legendary Tier 5 Elixir",
                subtitle = if (isAr) "الجائزة الكبرى النهائية في النظام" else "The Ultimate Grand Reward",
                accentColor = Color(0xFFFF007A),
                icon = Icons.Default.LocalFireDepartment
            )
        }

        item {
            EternalResurgenceLegendaryCard(
                isAr = isAr,
                count = eternalRebirthCount,
                isActiveImmunity = hasRebirthImmunity,
                animAngle = animAngle,
                pulseWave = pulseWave,
                onActivate = {
                    HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                    view.playSoundEffect(SoundEffectConstants.CLICK)
                    ProfileManager.useEternalRebirth(context)
                },
                onLongPress2s = {
                    selectedTooltip = if (isAr) {
                        InventoryTooltipSpec(
                            title = PotionType.ETERNAL_REBIRTH_ELIXIR.nameAr,
                            subtitle = "الفئة الخامسة الأسطورية",
                            rarityLabel = "أسطوري مطلق",
                            accentColor = Color(0xFFFF007A),
                            secondaryColor = Color(0xFFFFD700),
                            icon = Icons.Default.AutoAwesome,
                            mechanics = "أندر وأقوى جوهرة كيميائية في النظام؛ تُمنح حصرياً كجائزة ختامية عند الوصول للمحطة 500.",
                            bonuses = "تمنح درع مناعة أبدي ضد التصفير الكامل للنظام، وتفتح فوراً اللقب الأسمى «سيد النقوشة».",
                            expirationRules = "أبدية المفعول؛ بمجرد تفعيلها تظل حصانتها ومجدها مفعّلين للأبد."
                        )
                    } else {
                        InventoryTooltipSpec(
                            title = PotionType.ETERNAL_REBIRTH_ELIXIR.nameEn,
                            subtitle = "Legendary Tier 5",
                            rarityLabel = "ULTIMATE LEGENDARY",
                            accentColor = Color(0xFFFF007A),
                            secondaryColor = Color(0xFFFFD700),
                            icon = Icons.Default.AutoAwesome,
                            mechanics = "The rarest pinnacle potion in StrikePulse, awarded exclusively at Station 500.",
                            bonuses = "Grants eternal immunity against system flame resets and unlocks the Gary Master title.",
                            expirationRules = "Permanent & infinite duration once activated."
                        )
                    }
                }
            )
        }

        item {
            Spacer(modifier = Modifier.height(72.dp))
        }
    }

    // 2-Second Long-Press Translucent Animated Detail ModalBottomSheet
    if (selectedTooltip != null) {
        val spec = selectedTooltip!!
        ModalBottomSheet(
            onDismissRequest = { selectedTooltip = null },
            sheetState = sheetState,
            containerColor = Color(0xEC0B1120),
            scrimColor = Color(0x99020617),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            dragHandle = null
        ) {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(tween(240)) + scaleIn(initialScale = 0.94f, animationSpec = tween(260)),
                exit = fadeOut(tween(180)) + scaleOut(targetScale = 0.95f)
            ) {
                InventoryTooltipSheetContent(
                    spec = spec,
                    isAr = isAr,
                    onClose = { selectedTooltip = null }
                )
            }
        }
    }

    // Guiding Tooltip Hint Dialog
    if (showLongPressGuideDialog) {
        AlertDialog(
            onDismissRequest = { showLongPressGuideDialog = false },
            containerColor = Color(0xF50F172A),
            icon = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(NeonViolet.copy(alpha = 0.18f))
                        .border(1.2.dp, NeonViolet.copy(alpha = 0.7f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.TouchApp,
                        contentDescription = null,
                        tint = NeonViolet,
                        modifier = Modifier.size(26.dp)
                    )
                }
            },
            title = {
                Text(
                    text = if (isAr) "دليل الاستخدام" else "Usage Guide",
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    fontFamily = PlusJakartaSans
                )
            },
            text = {
                Text(
                    text = if (isAr) {
                        "اضغط لمدة ثانيتين متواصلتين على اي عنصر لمعرفة التفاصيل"
                    } else {
                        "Press and hold any item for 2 seconds to view full details."
                    },
                    fontSize = 14.sp,
                    color = Color(0xFFE2E8F0),
                    lineHeight = 22.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { showLongPressGuideDialog = false },
                    modifier = Modifier.testTag("inventory_guide_ok_btn")
                ) {
                    Text(
                        text = if (isAr) "حسناً" else "Got It",
                        color = NeonCyan,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        )
    }
}

@Composable
private fun SectionHeaderLabel(
    title: String,
    subtitle: String,
    accentColor: Color,
    icon: ImageVector,
    trailingContent: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(accentColor.copy(alpha = 0.18f))
                    .border(1.dp, accentColor.copy(alpha = 0.55f), RoundedCornerShape(9.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = PlusJakartaSans,
                    color = accentColor
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (trailingContent != null) {
            Spacer(modifier = Modifier.width(8.dp))
            trailingContent()
        }
    }
}

/**
 * Wraps any inventory card with a smooth 2-second (2000ms) long-press hold detector
 * and renders a live neon progress bar across the bottom edge while holding.
 */
@Composable
private fun LongPressTooltipContainer(
    accentColor: Color,
    onLongPress2s: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (isHolding: Boolean, progress: Float) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var isHolding by remember { mutableStateOf(false) }

    val progress by animateFloatAsState(
        targetValue = if (isHolding) 1f else 0f,
        animationSpec = if (isHolding) {
            tween(durationMillis = 2000, easing = LinearEasing)
        } else {
            tween(durationMillis = 150)
        },
        label = "hold_2s_progress"
    )

    val holdScale by animateFloatAsState(
        targetValue = if (isHolding) 0.985f else 1f,
        animationSpec = tween(150),
        label = "hold_scale"
    )

    Box(
        modifier = modifier
            .scale(holdScale)
            .pointerInput(onLongPress2s) {
                detectTapGestures(
                    onPress = {
                        isHolding = true
                        val job = scope.launch {
                            delay(2000L)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            isHolding = false
                            onLongPress2s()
                        }
                        tryAwaitRelease()
                        isHolding = false
                        job.cancel()
                    }
                )
            }
    ) {
        content(isHolding, progress)
    }
}

@Composable
private fun Luxury7DayAutoShieldCard(
    isAr: Boolean,
    isActiveNow: Boolean,
    remainingText: String?,
    totalTriggeredCount: Int,
    animAngle: Float,
    pulseWave: Float,
    onLongPress2s: () -> Unit
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

    LongPressTooltipContainer(
        accentColor = goldColor,
        onLongPress2s = onLongPress2s
    ) { isHolding, progress ->
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
                                contentDescription = if (isAr) "درع الـ 7 أيام التلقائي" else "7-Day Auto Shield",
                                tint = goldColor,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (isAr) "درع الـ 7 أيام التلقائي" else "7-Day Automated Shield",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = PlusJakartaSans,
                                    color = goldColor
                                )
                            }
                            Text(
                                text = if (isAr) "فئة نخبوية تلقائية • 3-5 سترايكات يومياً" else "LUXURY AUTO TIER • 3-5 Strikes/Day",
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
                                if (isAr) "مفعّل الآن" else "Active Now"
                            } else {
                                if (isAr) "تفعيل تلقائي" else "Auto Trigger"
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
                            if (isAr) "العد التنازلي المتبقي: $remainingText" else "Remaining countdown: $remainingText"
                        } else {
                            if (isAr) "يُمنح ويُفعّل تلقائياً عند تسجيل 3 إلى 5 ضربات في يوم واحد"
                            else "Granted & activated automatically upon recording 3–5 strikes in a single day"
                        },
                        fontSize = 11.sp,
                        color = if (isActiveNow) NeonEmerald else Color(0xFFE2E8F0),
                        fontWeight = if (isActiveNow) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isAr) "مرات التفعيل: $totalTriggeredCount" else "Triggers: $totalTriggeredCount",
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
    isAr: Boolean,
    count: Int,
    isCurrentlyActive: Boolean,
    accentColor: Color,
    icon: ImageVector,
    onActivate: () -> Unit,
    onLongPress2s: () -> Unit,
    testTag: String
) {
    LongPressTooltipContainer(
        accentColor = accentColor,
        onLongPress2s = onLongPress2s
    ) { isHolding, progress ->
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xD90E1726))
                .border(
                    width = if (isCurrentlyActive) 1.8.dp else 1.2.dp,
                    color = if (isCurrentlyActive) NeonEmerald else accentColor.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(18.dp)
                )
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
                            color = MaterialTheme.colorScheme.onSurface
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
                                    text = if (isAr) "• نشط" else "• Active",
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

                    if (count > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onActivate,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accentColor,
                                contentColor = Color(0xFF03141F)
                            ),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text(
                                if (isAr) "تفعيل" else "Activate",
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
private fun InventoryKeyTile(
    title: String,
    rarity: String,
    usesLabel: String,
    count: Int,
    accentColor: Color,
    onLongPress2s: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    LongPressTooltipContainer(
        accentColor = accentColor,
        onLongPress2s = onLongPress2s,
        modifier = modifier
    ) { isHolding, progress ->
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xD90E1726))
                .border(1.3.dp, accentColor.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
                .testTag(testTag)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.2f))
                        .border(1.2.dp, accentColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.VpnKey,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = usesLabel,
                    fontSize = 10.sp,
                    color = accentColor,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = 0.16f))
                        .border(1.dp, accentColor.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "$count / 30",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = accentColor
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = rarity,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF94A3B8)
                )
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
    isAr: Boolean,
    count: Int,
    statusBadge: String?,
    accentColor: Color,
    icon: ImageVector,
    canActivate: Boolean,
    onActivate: () -> Unit,
    onLongPress2s: () -> Unit,
    testTag: String
) {
    LongPressTooltipContainer(
        accentColor = accentColor,
        onLongPress2s = onLongPress2s
    ) { isHolding, progress ->
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
                .background(Color(0xD90E1726))
                .border(1.2.dp, accentColor.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
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
                            color = MaterialTheme.colorScheme.onSurface
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
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accentColor,
                                contentColor = Color(0xFF03141F)
                            ),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text(
                                if (isAr) "استعمال" else "Use",
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
    isAr: Boolean,
    count: Int,
    isActiveImmunity: Boolean,
    animAngle: Float,
    pulseWave: Float,
    onActivate: () -> Unit,
    onLongPress2s: () -> Unit
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

    LongPressTooltipContainer(
        accentColor = crimsonMagenta,
        onLongPress2s = onLongPress2s
    ) { isHolding, progress ->
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
                                contentDescription = if (isAr) "جرعة الانبعاث الأبدي" else "Eternal Resurgence Potion",
                                tint = gold,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (isAr) "جرعة الانبعاث الأبدي" else "Eternal Resurgence Potion",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = PlusJakartaSans,
                                color = Color.White
                            )
                            Text(
                                text = if (isAr) "الفئة الخامسة الأسطورية" else "LEGENDARY TIER 5",
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
                                if (isAr) "أبدي مفعّل ✓" else "Eternal Active ✓"
                            } else "$count / 3",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = gold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = if (isAr) {
                        "جائزة المحطة 500 الكبرى: مناعة أبدية ضد تصفير الشعلة وفتح لقب «سيد النقوشة»."
                    } else {
                        "Grand Reward of Station 500: Grants eternal immunity against Flame resets and unlocks the Gary Master title."
                    },
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
                            .height(44.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = crimsonMagenta,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (isAr) "تفعيل الانبعاث الأبدي" else "Activate Eternal Resurgence",
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
private fun InventoryTooltipSheetContent(
    spec: InventoryTooltipSpec,
    isAr: Boolean,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.5.dp,
                Brush.horizontalGradient(listOf(spec.accentColor, spec.secondaryColor)),
                RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            )
            .padding(horizontal = 20.dp, vertical = 20.dp)
            .testTag("inventory_tooltip_sheet")
    ) {
        // Header row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(spec.accentColor.copy(alpha = 0.22f))
                        .border(1.5.dp, spec.accentColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = spec.icon,
                        contentDescription = spec.title,
                        tint = spec.accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = spec.title,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        text = "${spec.subtitle} • ${spec.rarityLabel}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = spec.accentColor
                    )
                }
            }

            IconButton(onClick = onClose, modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = if (isAr) "إغلاق" else "Close",
                    tint = Color(0xFF94A3B8)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TooltipRuleBlock(
            label = if (isAr) "آلية العمل الدقيقة" else "Exact Mechanics",
            body = spec.mechanics,
            icon = Icons.Default.Info,
            color = spec.accentColor
        )

        Spacer(modifier = Modifier.height(10.dp))

        TooltipRuleBlock(
            label = if (isAr) "المزايا والمكافآت" else "Bonuses & Effects",
            body = spec.bonuses,
            icon = Icons.Default.AutoAwesome,
            color = NeonEmerald
        )

        Spacer(modifier = Modifier.height(10.dp))

        TooltipRuleBlock(
            label = if (isAr) "قواعد الصلاحية والاستهلاك" else "Expiration & Usage Rules",
            body = spec.expirationRules,
            icon = Icons.Default.Timer,
            color = NeonAmber
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun TooltipRuleBlock(
    label: String,
    body: String,
    icon: ImageVector,
    color: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.1f))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = body,
            fontSize = 12.sp,
            color = Color(0xFFE2E8F0),
            lineHeight = 18.sp
        )
    }
}
