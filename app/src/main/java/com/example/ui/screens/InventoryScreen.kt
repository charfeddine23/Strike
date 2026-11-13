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
    val titleAr: String,
    val titleEn: String,
    val rarityLabel: String,
    val accentColor: Color,
    val secondaryColor: Color,
    val icon: ImageVector,
    val mechanicsAr: String,
    val bonusesAr: String,
    val expirationRulesAr: String
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
                                text = "المخزون السيبراني (Inventory)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = PlusJakartaSans,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "الدروع • مفاتيح التجاوز • الجرعات التكتيكية",
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
                title = "الدروع الوقائية (Protective Shields)",
                subtitle = if (shieldActuallyActive) {
                    "الدرع النشط: ${activeShieldType.labelAr} (${FlameManager.formatRemaining(remainingMillis)})"
                } else {
                    "لا يوجد درع نشط حالياً"
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
                            contentDescription = "دليل الاستخدام",
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
                isActiveNow = is7dActive,
                remainingText = if (is7dActive) FlameManager.formatRemaining(remainingMillis) else null,
                totalTriggeredCount = sevenDayAutoTriggers,
                animAngle = animAngle,
                pulseWave = pulseWave,
                onLongPress2s = {
                    selectedTooltip = InventoryTooltipSpec(
                        titleAr = "درع الـ 7 أيام التلقائي (Luxury Auto Shield)",
                        titleEn = "7-Day Automated Strike Shield",
                        rarityLabel = "LUXURY TIER • تلقائي نخبوي",
                        accentColor = Color(0xFFFFD700),
                        secondaryColor = Color(0xFFFF007A),
                        icon = Icons.Default.VerifiedUser,
                        mechanicsAr = "درع وقائي أوتوماتيكي نخبوي يعمل كصمام أمان عالي المستوى عند التعرض لضربات مكثفة في يوم واحد.",
                        bonusesAr = "يُفعّل تلقائياً وفورياً عند تسجيل 3 إلى 5 سترايكات في نفس اليوم، ليوقف إضافة +3 نقاط خمول يومية لمدة 7 أيام كاملة (168 ساعة).",
                        expirationRulesAr = "تبدأ مدة الـ 7 أيام تلقائياً لحظة تفعيل الكومبو وتنتهي بعد انقضاء 168 ساعة، ولا يمكن تخزينه يدوياً."
                    )
                }
            )
        }

        // 24h, 48h, 72h Protective Shields
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                InventoryShieldCard(
                    title = "درع الـ 24 ساعة (24h Shield)",
                    rarity = "COMMON • شائع",
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
                        selectedTooltip = InventoryTooltipSpec(
                            titleAr = "درع الـ 24 ساعة",
                            titleEn = "24-Hour Protective Shield",
                            rarityLabel = "COMMON • شائع",
                            accentColor = NeonCyan,
                            secondaryColor = NeonEmerald,
                            icon = Icons.Default.Shield,
                            mechanicsAr = "درع تكتيكي يجمّد عداد الخمول اليومي مؤقتاً لحماية رصيد الشعلة.",
                            bonusesAr = "يمنع إضافة +3 نقاط خمول يومية لمدة 24 ساعة متواصلة، ويُفعّل فورياً عند استلامه من المسار.",
                            expirationRulesAr = "ينتهي مفعوله بعد 24 ساعة من لحظة التفعيل. إذا كان هناك درع نشط، تُضاف الـ 24 ساعة إلى وقت الانتهاء."
                        )
                    },
                    testTag = "inv_shield_24h"
                )

                InventoryShieldCard(
                    title = "درع الـ 48 ساعة (48h Shield)",
                    rarity = "UNCOMMON • غير شائع",
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
                        selectedTooltip = InventoryTooltipSpec(
                            titleAr = "درع الـ 48 ساعة",
                            titleEn = "48-Hour Protective Shield",
                            rarityLabel = "UNCOMMON • غير شائع",
                            accentColor = NeonEmerald,
                            secondaryColor = NeonCyan,
                            icon = Icons.Default.Shield,
                            mechanicsAr = "حاجز سيبراني متوسط المدى يحمي استقرار الشعلة عبر يومين متتاليين.",
                            bonusesAr = "يوقف تزايد نقاط الخمول (+3 نقاط/يوم) لمدة 48 ساعة متواصلة، مع أولوية التفعيل التلقائي عند الاستلام.",
                            expirationRulesAr = "صلاحية الحماية تمتد لـ 48 ساعة متصلة؛ وفي حال وجود درع نشط مسبقاً تتراكم المدة تلقائياً."
                        )
                    },
                    testTag = "inv_shield_48h"
                )

                InventoryShieldCard(
                    title = "درع الـ 72 ساعة (72h Shield)",
                    rarity = "RARE • نادر",
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
                        selectedTooltip = InventoryTooltipSpec(
                            titleAr = "درع الـ 72 ساعة",
                            titleEn = "72-Hour Protective Shield",
                            rarityLabel = "RARE • نادر",
                            accentColor = NeonAmber,
                            secondaryColor = Color(0xFFEA580C),
                            icon = Icons.Default.VerifiedUser,
                            mechanicsAr = "درع حماية عالي التحمل يُمنح من مكافآت المحطات المتقدمة أو كتعويض عند عقوبة الضربة المزدوجة (Double Strike).",
                            bonusesAr = "يجمّد نقاط الخمول اليومية (+3 نقاط/يوم) لمدة 3 أيام كاملة (72 ساعة).",
                            expirationRulesAr = "يُستهلك الدرع عند التفعيل اليدوي أو الفوري ويمتد مفعوله 72 ساعة كاملة من التوقيت الحالي أو الممدد."
                        )
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
                title = "مفاتيح التجاوز (Skip-Keys)",
                subtitle = "السعة القصوى لكل فئة: 30 مفتاح (تُستثنى المهام السنوية)",
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
                    title = SkipKeyType.WOODEN.nameAr,
                    rarity = "COMMON",
                    usesLabel = "استخدام 1",
                    count = woodenKeys,
                    accentColor = Color(0xFFD97706),
                    onLongPress2s = {
                        selectedTooltip = InventoryTooltipSpec(
                            titleAr = "المفتاح الخشبي (Wooden Key)",
                            titleEn = "Wooden Skip-Key",
                            rarityLabel = "COMMON • شائع",
                            accentColor = Color(0xFFD97706),
                            secondaryColor = Color(0xFFB45309),
                            icon = Icons.Default.VpnKey,
                            mechanicsAr = "مفتاح فك شيفرة قياسي لتجاوز المهام اليومية والأسبوعية البسيطة وحصد نقاط الـ XP فوراً.",
                            bonusesAr = "يمنح استخداماً واحداً (1x) لإكمال مهمة متوافقة فوراً مع الحصول على كامل مكافأة الـ XP الخاصة بها.",
                            expirationRulesAr = "لا تنتهي صلاحيته بالتخزين (الحد الأقصى 30 مفتاح). ملاحظة صارمة: لا يمكن استخدامه لتجاوز المهام السنوية."
                        )
                    },
                    testTag = "inv_key_wooden"
                )

                InventoryKeyTile(
                    modifier = Modifier.weight(1f),
                    title = SkipKeyType.BRONZE.nameAr,
                    rarity = "UNCOMMON",
                    usesLabel = "استخدامان",
                    count = bronzeKeys,
                    accentColor = Color(0xFFF59E0B),
                    onLongPress2s = {
                        selectedTooltip = InventoryTooltipSpec(
                            titleAr = "المفتاح البرونزي (Bronze Key)",
                            titleEn = "Bronze Skip-Key",
                            rarityLabel = "UNCOMMON • غير شائع",
                            accentColor = Color(0xFFF59E0B),
                            secondaryColor = Color(0xFFEA580C),
                            icon = Icons.Default.VpnKey,
                            mechanicsAr = "مفتاح مطوّر يسمح بتخطي المهام ذات الصعوبة المتوسطة ضمن المهام اليومية والأسبوعية والشهرية.",
                            bonusesAr = "يملك قدرة استخدامين (2x) لتمرير المهام المؤهلة وحصد نقاط الـ XP دون تأخير.",
                            expirationRulesAr = "دائم في المخزون حتى استهلاكه (السعة القصوى 30 مفتاح). ممنوع استخدامه في المهام السنوية."
                        )
                    },
                    testTag = "inv_key_bronze"
                )

                InventoryKeyTile(
                    modifier = Modifier.weight(1f),
                    title = SkipKeyType.SILVER.nameAr,
                    rarity = "RARE",
                    usesLabel = "3 استخدامات",
                    count = silverKeys,
                    accentColor = Color(0xFFE2E8F0),
                    onLongPress2s = {
                        selectedTooltip = InventoryTooltipSpec(
                            titleAr = "المفتاح الفضي (Silver Key)",
                            titleEn = "Silver Skip-Key",
                            rarityLabel = "RARE • نادر",
                            accentColor = Color(0xFFE2E8F0),
                            secondaryColor = NeonCyan,
                            icon = Icons.Default.VpnKey,
                            mechanicsAr = "مفتاح اختراق رفيع المستوى قادر على تجاوز المهام عالية المتطلبات في القوائم الدورية.",
                            bonusesAr = "يمتلك 3 استخدامات (3x) مع أقصى كفاءة لحصد مكافآت الـ XP العالية فوراً.",
                            expirationRulesAr = "يبقى محفوظاً حتى استنفاد شحناته (الحد الأقصى 30 مفتاح). لا يشمل المهام السنوية التي تتطلب إنجازاً فعلياً."
                        )
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
                title = "الجرعات السيبرانية (Cybernetic Potions)",
                subtitle = "السعة القصوى: 3 جرعات لكل فئة • التفاعلات الكيميائية الحيوية",
                accentColor = NeonCyan,
                icon = Icons.Default.Science
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // 1. Pulse Elixir
                InventoryPotionCard(
                    title = PotionType.PULSE_ELIXIR.nameAr,
                    subtitleEn = "Pulse Elixir",
                    rarity = "COMMON • الفئة 1",
                    count = pulseElixirCount,
                    statusBadge = if (pulseUsesLeft > 0) "نشط ($pulseUsesLeft استخدامات)" else null,
                    accentColor = PotionType.PULSE_ELIXIR.color,
                    icon = Icons.Default.Bolt,
                    canActivate = pulseElixirCount > 0,
                    onActivate = {
                        HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                        view.playSoundEffect(SoundEffectConstants.CLICK)
                        ProfileManager.usePulseElixir(context)
                    },
                    onLongPress2s = {
                        selectedTooltip = InventoryTooltipSpec(
                            titleAr = PotionType.PULSE_ELIXIR.nameAr,
                            titleEn = "Pulse Elixir",
                            rarityLabel = "COMMON • الفئة الأولى",
                            accentColor = PotionType.PULSE_ELIXIR.color,
                            secondaryColor = NeonEmerald,
                            icon = Icons.Default.Bolt,
                            mechanicsAr = "منشط نبضي يضاعف فاعلية زر التبريد (Deflame) لخفض طاقة الشعلة بسرعة أكبر.",
                            bonusesAr = "يضيف +5 نقاط خصم إضافية لكل ضغطة على زر Deflame خلال الـ 3 تفعيلات القادمة.",
                            expirationRulesAr = "ينتهي تأثيره تلقائياً بمجرد استهلاك الـ 3 تفعيلات المتتالية لزر Deflame."
                        )
                    },
                    testTag = "inv_potion_pulse"
                )

                // 2. Shadow-Grip Serum
                InventoryPotionCard(
                    title = PotionType.SHADOW_GRIP_SERUM.nameAr,
                    subtitleEn = "Shadow-Grip Serum",
                    rarity = "UNCOMMON • الفئة 2",
                    count = shadowSerumCount,
                    statusBadge = if (isShadowActive) "نشط (تبطيء 50%)" else null,
                    accentColor = PotionType.SHADOW_GRIP_SERUM.color,
                    icon = Icons.Default.Science,
                    canActivate = shadowSerumCount > 0 && !isShadowActive,
                    onActivate = {
                        HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                        view.playSoundEffect(SoundEffectConstants.CLICK)
                        ProfileManager.useShadowSerum(context)
                    },
                    onLongPress2s = {
                        selectedTooltip = InventoryTooltipSpec(
                            titleAr = PotionType.SHADOW_GRIP_SERUM.nameAr,
                            titleEn = "Shadow-Grip Serum",
                            rarityLabel = "UNCOMMON • الفئة الثانية",
                            accentColor = PotionType.SHADOW_GRIP_SERUM.color,
                            secondaryColor = Color(0xFF7E22CE),
                            icon = Icons.Default.Science,
                            mechanicsAr = "مصل كابح للانجراف الحراري يقوم بتهدئة تسارع نقاط الخمول اليومي في النظام.",
                            bonusesAr = "يقلل وتيرة صعود نقاط الشعلة الناتجة عن الخمول اليومي بنسبة 50% لمدة 3 أيام متتالية.",
                            expirationRulesAr = "يبدأ العد التنازلي لمدة 72 ساعة لحظة الحقن، ويعود معدل الخمول إلى طبيعته بعد انتهاء المدة."
                        )
                    },
                    testTag = "inv_potion_shadow"
                )

                // 3. Gabriel's Purity Droplet
                InventoryPotionCard(
                    title = PotionType.GABRIEL_PURITY_DROPLET.nameAr,
                    subtitleEn = "Gabriel's Purity Droplet",
                    rarity = "RARE • الفئة 3",
                    count = gabrielDropletCount,
                    statusBadge = if (isGabrielActive) "نشط تلقائياً (+50% نمو)" else "تفعيل تلقائي عند الكسب",
                    accentColor = PotionType.GABRIEL_PURITY_DROPLET.color,
                    icon = Icons.Default.WaterDrop,
                    canActivate = false,
                    onActivate = {},
                    onLongPress2s = {
                        selectedTooltip = InventoryTooltipSpec(
                            titleAr = PotionType.GABRIEL_PURITY_DROPLET.nameAr,
                            titleEn = "Gabriel's Purity Droplet",
                            rarityLabel = "RARE • الفئة الثالثة",
                            accentColor = PotionType.GABRIEL_PURITY_DROPLET.color,
                            secondaryColor = NeonAmber,
                            icon = Icons.Default.WaterDrop,
                            mechanicsAr = "قطرة نقاء نورانية تتفاعل تلقائياً مع الحمض النووي فور استلامها من مكافآت المسار.",
                            bonusesAr = "تسرّع وتيرة نمو نقاط الشعلة بنسبة +50% خلال احتساب الخمول لدفع التقدم نحو المسار الملائكي.",
                            expirationRulesAr = "تُفعّل تلقائياً وفورياً عند الجني دون تخزين وتستمر فعاليتها لمدة 7 أيام كاملة (168 ساعة)."
                        )
                    },
                    testTag = "inv_potion_gabriel"
                )

                // 4. Absolute Cryo-Infusion
                InventoryPotionCard(
                    title = PotionType.ABSOLUTE_CRYO_INFUSION.nameAr,
                    subtitleEn = "Absolute Cryo-Infusion",
                    rarity = "EPIC • الفئة 4",
                    count = cryoInfusionCount,
                    statusBadge = if (hasCryoArmed) "مسلّح للستريك القادم (+100)" else null,
                    accentColor = PotionType.ABSOLUTE_CRYO_INFUSION.color,
                    icon = Icons.Default.AcUnit,
                    canActivate = cryoInfusionCount > 0 && !hasCryoArmed,
                    onActivate = {
                        HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                        view.playSoundEffect(SoundEffectConstants.CLICK)
                        ProfileManager.useCryoInfusion(context)
                    },
                    onLongPress2s = {
                        selectedTooltip = InventoryTooltipSpec(
                            titleAr = PotionType.ABSOLUTE_CRYO_INFUSION.nameAr,
                            titleEn = "Absolute Cryo-Infusion",
                            rarityLabel = "EPIC / RARE • الفئة الرابعة",
                            accentColor = PotionType.ABSOLUTE_CRYO_INFUSION.color,
                            secondaryColor = Color(0xFF0284C7),
                            icon = Icons.Default.AcUnit,
                            mechanicsAr = "حقنة تبريد كريوجينية مطلقة تسلّح الستريك القادم بطاقة تعزيز استثنائية.",
                            bonusesAr = "تمنح +100 نقطة بونص لتعزيز قوة وتأثير الستريك القادم.",
                            expirationRulesAr = "اختيارية التفعيل من المخزون؛ وتتفعل تلقائياً إذا مرّ 30 يوماً دون استخدامها، وتُستهلك بضربة واحدة."
                        )
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
                title = "الإكسير الأسطوري الفائق (Legendary Tier 5)",
                subtitle = "الجائزة الكبرى النهائية في نظام StrikePulse",
                accentColor = Color(0xFFFF007A),
                icon = Icons.Default.LocalFireDepartment
            )
        }

        item {
            EternalResurgenceLegendaryCard(
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
                    selectedTooltip = InventoryTooltipSpec(
                        titleAr = "جرعة الانبعاث الأبدي (Eternal Resurgence Potion)",
                        titleEn = "Eternal Resurgence Potion • Tier 5",
                        rarityLabel = "LEGENDARY TIER 5 • أسطوري مطلق",
                        accentColor = Color(0xFFFF007A),
                        secondaryColor = Color(0xFFFFD700),
                        icon = Icons.Default.AutoAwesome,
                        mechanicsAr = "أندر وأقوى جوهرة كيميائية في StrikePulse؛ تُمنح حصرياً كجائزة ختامية عند الوصول للمحطة 500.",
                        bonusesAr = "تمنح درع مناعة أبدي ضد التصفير الكامل للنظام، وتفتح فوراً اللقب الأسمى «سيد النقوشة (Gary Master)».",
                        expirationRulesAr = "أبدية المفعول (Permanent / Infinite)؛ بمجرد تفعيلها تظل حصانتها ومجدها مفعّلين للأبد."
                    )
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
                    text = "دليل الاستخدام",
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    fontFamily = PlusJakartaSans
                )
            },
            text = {
                Text(
                    text = "اضغط لمدة ثانيتين متواصلتين على اي عنصر لمعرفة التفاصيل",
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
                        text = "حسناً",
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
                                contentDescription = "7-Day Auto Shield",
                                tint = goldColor,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "درع الـ 7 أيام التلقائي",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = PlusJakartaSans,
                                    color = goldColor
                                )
                            }
                            Text(
                                text = "LUXURY AUTO TIER • 3-5 سترايكات يومياً",
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
                            text = if (isActiveNow) "مفعّل الآن" else "تفعيل تلقائي",
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
                            "العد التنازلي المتبقي: $remainingText"
                        } else {
                            "يُمنح ويتفعل تلقائياً عند تسجيل 3 إلى 5 ضربات في يوم واحد"
                        },
                        fontSize = 11.sp,
                        color = if (isActiveNow) NeonEmerald else Color(0xFFE2E8F0),
                        fontWeight = if (isActiveNow) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "مرات التفعيل: $totalTriggeredCount",
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
                                    text = "• نشط",
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
                            Text("تفعيل", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
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
    subtitleEn: String,
    rarity: String,
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
                            text = "$subtitleEn • $rarity",
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
                            Text("استعمال", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
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
                                contentDescription = "Eternal Resurgence Potion",
                                tint = gold,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "جرعة الانبعاث الأبدي",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = PlusJakartaSans,
                                color = Color.White
                            )
                            Text(
                                text = "Eternal Resurgence Potion • LEGENDARY TIER 5",
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
                            text = if (isActiveImmunity) "أبدي مفعل ✓" else "$count / 3",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = gold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "جائزة المحطة 500 الكبرى: مناعة أبدية ضد تصفير الشعلة وفتح لقب سيد النقوشة (Gary Master).",
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
                        Text("تفعيل الانبعاث الأبدي", fontWeight = FontWeight.Black, fontSize = 13.sp)
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
                        contentDescription = spec.titleAr,
                        tint = spec.accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = spec.titleAr,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        text = "${spec.titleEn} • ${spec.rarityLabel}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = spec.accentColor
                    )
                }
            }

            IconButton(onClick = onClose, modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color(0xFF94A3B8)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TooltipRuleBlock(
            label = "آلية العمل الدقيقة (Exact Mechanics)",
            body = spec.mechanicsAr,
            icon = Icons.Default.Info,
            color = spec.accentColor
        )

        Spacer(modifier = Modifier.height(10.dp))

        TooltipRuleBlock(
            label = "المزايا والمكافآت (Bonuses)",
            body = spec.bonusesAr,
            icon = Icons.Default.AutoAwesome,
            color = NeonEmerald
        )

        Spacer(modifier = Modifier.height(10.dp))

        TooltipRuleBlock(
            label = "قواعد الصلاحية والاستهلاك (Expiration Rules)",
            body = spec.expirationRulesAr,
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
