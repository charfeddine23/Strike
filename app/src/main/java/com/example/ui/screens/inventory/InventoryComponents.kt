package com.example.ui.screens.inventory

import android.view.SoundEffectConstants
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.example.util.HapticHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

const val INVENTORY_LONG_PRESS_MS = 1000L

@Composable
fun cardGlassBackground(isDarkTheme: Boolean): Color =
    if (isDarkTheme) Color(0xD90E1726) else Color(0xF2FFFFFF)

@Composable
fun cardPrimaryTextColor(isDarkTheme: Boolean): Color =
    if (isDarkTheme) Color(0xFFF8FAFC) else Color(0xFF0F172A)

@Composable
fun cardSecondaryTextColor(isDarkTheme: Boolean): Color =
    if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF475569)

/**
 * Wraps any inventory card with a smooth 1-second (1000ms) long-press hold detector
 * and renders a live progress state while holding.
 */
@Composable
fun LongPressTooltipContainer(
    onLongPress1s: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (isHolding: Boolean, progress: Float) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var isHolding by remember { mutableStateOf(false) }

    val progress by animateFloatAsState(
        targetValue = if (isHolding) 1f else 0f,
        animationSpec = if (isHolding) {
            tween(durationMillis = INVENTORY_LONG_PRESS_MS.toInt(), easing = LinearEasing)
        } else {
            tween(durationMillis = 120)
        },
        label = "hold_1s_progress"
    )

    val holdScale by animateFloatAsState(
        targetValue = if (isHolding) 0.985f else 1f,
        animationSpec = tween(140),
        label = "hold_scale"
    )

    Box(
        modifier = modifier
            .scale(holdScale)
            .pointerInput(onLongPress1s) {
                detectTapGestures(
                    onPress = {
                        isHolding = true
                        val job = scope.launch {
                            delay(INVENTORY_LONG_PRESS_MS)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            isHolding = false
                            onLongPress1s()
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

/**
 * Protective Shields header section anchored at the top of the Inventory hub.
 */
@Composable
fun ProtectiveShieldsHeaderCard(
    currentLang: String,
    shieldActuallyActive: Boolean,
    activeShieldLabel: String,
    remainingFormatted: String,
    isDarkTheme: Boolean,
    isHapticEnabled: Boolean,
    onOpenGuide: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val surfaceBg = cardGlassBackground(isDarkTheme)
    val borderColor = if (shieldActuallyActive) NeonEmerald.copy(alpha = 0.72f) else NeonEmerald.copy(alpha = 0.42f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            NeonEmerald.copy(alpha = if (shieldActuallyActive) 0.22f else 0.12f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.12f, size.height * 0.5f),
                        radius = size.width * 0.5f
                    )
                )
            }
            .background(surfaceBg)
            .border(1.3.dp, borderColor, RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag("inv_protective_shields_section")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(NeonEmerald.copy(alpha = 0.18f))
                        .border(1.2.dp, NeonEmerald.copy(alpha = 0.65f), RoundedCornerShape(13.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = locText(currentLang, "الدروع الوقائية", "Boucliers Protecteurs", "Protective Shields"),
                        tint = NeonEmerald,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = locText(currentLang, "الدروع الوقائية", "Boucliers Protecteurs", "Protective Shields"),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = PlusJakartaSans,
                        color = NeonEmerald
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (shieldActuallyActive) {
                            locText(
                                currentLang,
                                "الدرع النشط: $activeShieldLabel ($remainingFormatted)",
                                "Bouclier actif : $activeShieldLabel ($remainingFormatted)",
                                "Active Shield: $activeShieldLabel ($remainingFormatted)"
                            )
                        } else {
                            locText(
                                currentLang,
                                "لا يوجد درع نشط حالياً",
                                "Aucun bouclier actif actuellement",
                                "No active shield currently"
                            )
                        },
                        fontSize = 11.5.sp,
                        fontWeight = if (shieldActuallyActive) FontWeight.Bold else FontWeight.Medium,
                        color = if (shieldActuallyActive) NeonEmerald else cardSecondaryTextColor(isDarkTheme),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                    view.playSoundEffect(SoundEffectConstants.CLICK)
                    onOpenGuide()
                },
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(NeonViolet.copy(alpha = 0.16f))
                    .border(1.2.dp, NeonViolet.copy(alpha = 0.7f), CircleShape)
                    .testTag("inventory_longpress_hint_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.TouchApp,
                    contentDescription = locText(currentLang, "دليل الاستخدام", "Guide d'utilisation", "Usage Guide"),
                    tint = NeonViolet,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

/**
 * Distinct 2x2 hub category card supporting tap-to-open AND 1-second long-press explanation.
 */
@Composable
fun InventoryCategoryTile(
    title: String,
    subtitle: String,
    badgeText: String,
    icon: ImageVector,
    accentColor: Color,
    secondaryColor: Color,
    isDarkTheme: Boolean,
    testTag: String,
    onClick: () -> Unit,
    onLongPress1s: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var isHolding by remember { mutableStateOf(false) }

    val progress by animateFloatAsState(
        targetValue = if (isHolding) 1f else 0f,
        animationSpec = if (isHolding) {
            tween(durationMillis = INVENTORY_LONG_PRESS_MS.toInt(), easing = LinearEasing)
        } else {
            tween(durationMillis = 120)
        },
        label = "cat_hold_progress"
    )
    val scale by animateFloatAsState(
        targetValue = if (isHolding) 0.97f else 1f,
        animationSpec = tween(140),
        label = "cat_hold_scale"
    )

    val cardBg = cardGlassBackground(isDarkTheme)

    Box(
        modifier = modifier
            .scale(scale)
            .heightIn(min = 158.dp)
            .clip(RoundedCornerShape(22.dp))
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.22f),
                            secondaryColor.copy(alpha = 0.08f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.85f, size.height * 0.18f),
                        radius = size.width * 0.72f
                    )
                )
            }
            .background(cardBg)
            .border(
                width = 1.4.dp,
                brush = Brush.linearGradient(
                    colors = listOf(accentColor.copy(alpha = 0.78f), secondaryColor.copy(alpha = 0.42f))
                ),
                shape = RoundedCornerShape(22.dp)
            )
            .clickable(onClick = onClick)
            .pointerInput(onLongPress1s) {
                val slop = viewConfiguration.touchSlop
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    isHolding = true
                    var longPressed = false
                    val job = scope.launch {
                        delay(INVENTORY_LONG_PRESS_MS)
                        longPressed = true
                        isHolding = false
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLongPress1s()
                    }
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        val distX = abs(change.position.x - down.position.x)
                        val distY = abs(change.position.y - down.position.y)
                        if (distX > slop || distY > slop) {
                            break
                        }
                        if (change.changedToUpIgnoreConsumed()) {
                            if (longPressed) {
                                change.consume()
                            }
                            break
                        }
                        if (longPressed) {
                            change.consume()
                        }
                    }
                    isHolding = false
                    job.cancel()
                }
            }
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.radialGradient(
                                listOf(accentColor.copy(alpha = 0.30f), accentColor.copy(alpha = 0.10f))
                            )
                        )
                        .border(1.2.dp, accentColor.copy(alpha = 0.75f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(accentColor.copy(alpha = 0.16f))
                        .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = badgeText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = accentColor,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = PlusJakartaSans,
                    color = cardPrimaryTextColor(isDarkTheme),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = cardSecondaryTextColor(isDarkTheme),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 15.sp
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

@Composable
fun SubPageTopBar(
    title: String,
    subtitle: String,
    accentColor: Color,
    icon: ImageVector,
    currentLang: String,
    isDarkTheme: Boolean,
    isHapticEnabled: Boolean,
    onBack: () -> Unit,
    onOpenGuide: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                    view.playSoundEffect(SoundEffectConstants.CLICK)
                    onBack()
                },
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.14f))
                    .border(1.2.dp, accentColor.copy(alpha = 0.55f), CircleShape)
                    .testTag("inv_subpage_back_btn")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = locText(currentLang, "رجوع", "Retour", "Back"),
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.16f))
                    .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = PlusJakartaSans,
                    color = cardPrimaryTextColor(isDarkTheme),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = accentColor,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        val guideAccent = if (isDarkTheme) NeonViolet else DayPurple
        IconButton(
            onClick = {
                HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                view.playSoundEffect(SoundEffectConstants.CLICK)
                onOpenGuide()
            },
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .size(48.dp)
                .clip(CircleShape)
                .background(guideAccent.copy(alpha = 0.15f))
                .border(1.dp, guideAccent.copy(alpha = 0.6f), CircleShape)
                .testTag("inventory_longpress_hint_btn")
        ) {
            Icon(
                imageVector = Icons.Default.TouchApp,
                contentDescription = locText(currentLang, "دليل الاستخدام", "Guide d'utilisation", "Usage Guide"),
                tint = guideAccent,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
fun EmptyInventoryCategoryCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accentColor: Color,
    isDarkTheme: Boolean,
    testTag: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(cardGlassBackground(isDarkTheme))
            .border(1.2.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(22.dp))
            .padding(horizontal = 24.dp, vertical = 34.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.14f))
                    .border(1.3.dp, accentColor.copy(alpha = 0.55f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = PlusJakartaSans,
                color = cardPrimaryTextColor(isDarkTheme),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = cardSecondaryTextColor(isDarkTheme),
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun InventoryTooltipSheetContent(
    spec: InventoryTooltipSpec,
    currentLang: String,
    isDarkTheme: Boolean = LocalAppDarkTheme.current,
    onClose: () -> Unit
) {
    val goldOrAmberAccent = if (isDarkTheme) Color(0xFFFFD700) else Color(0xFFB45309)
    val emeraldAccent = if (isDarkTheme) NeonEmerald else DayEmerald
    val amberAccent = if (isDarkTheme) NeonAmber else DayOrange

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
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
                        .background(spec.accentColor.copy(alpha = 0.18f))
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
                        color = if (isDarkTheme) Color.White else DayTextPrimary
                    )
                    Text(
                        text = "${spec.subtitle} • ${spec.rarityLabel}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = spec.accentColor
                    )
                }
            }

            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = locText(currentLang, "إغلاق", "Fermer", "Close"),
                    tint = if (isDarkTheme) Color(0xFF94A3B8) else DayTextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TooltipRuleBlock(
            label = locText(currentLang, "الشرح وآلية العمل", "Explication et Mécanisme", "Explanation & Mechanics"),
            body = spec.mechanics,
            icon = Icons.Default.Info,
            color = spec.accentColor,
            isDarkTheme = isDarkTheme
        )

        Spacer(modifier = Modifier.height(10.dp))

        TooltipRuleBlock(
            label = locText(currentLang, "كيفية الحصول على الشيء", "Comment l'obtenir", "How to Obtain"),
            body = spec.howToObtain,
            icon = Icons.Default.EmojiEvents,
            color = goldOrAmberAccent,
            isDarkTheme = isDarkTheme,
            modifier = Modifier.testTag("inv_tooltip_how_to_obtain")
        )

        Spacer(modifier = Modifier.height(10.dp))

        TooltipRuleBlock(
            label = locText(currentLang, "المزايا والمكافآت", "Avantages et Effets", "Bonuses & Effects"),
            body = spec.bonuses,
            icon = Icons.Default.AutoAwesome,
            color = emeraldAccent,
            isDarkTheme = isDarkTheme
        )

        Spacer(modifier = Modifier.height(10.dp))

        TooltipRuleBlock(
            label = locText(currentLang, "قواعد الصلاحية والاستهلاك", "Règles d'utilisation", "Expiration & Usage Rules"),
            body = spec.expirationRules,
            icon = Icons.Default.Timer,
            color = amberAccent,
            isDarkTheme = isDarkTheme
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun TooltipRuleBlock(
    label: String,
    body: String,
    icon: ImageVector,
    color: Color,
    isDarkTheme: Boolean = LocalAppDarkTheme.current,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = if (isDarkTheme) 0.1f else 0.08f))
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
            color = if (isDarkTheme) Color(0xFFE2E8F0) else DayTextPrimary,
            lineHeight = 18.sp
        )
    }
}
