package com.example.ui.components

import android.view.SoundEffectConstants
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonRose
import com.example.ui.theme.PlusJakartaSans
import com.example.util.HapticHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Complete Luxurious Liquid Glass Modal Sheet for Streak Shield management:
 * - Active shield status, digital countdown & settings toggle
 * - Shield inventory: 1-Day Shield, 3-Day Shield, 7-Day Auto Shield
 * - Comprehensive rules explanation for inactivity, penalties, and rewards
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreakShieldModalSheet(
    isShieldActive: Boolean,
    activeShieldExpiresAt: Long?,
    remainingMillis: Long,
    availableManualShields: Int,
    available1DayShields: Int,
    onActivateManualShield: () -> Unit,
    onActivate1DayShield: () -> Unit,
    onGrant1DayShield: () -> Unit,
    onToggleShieldActive: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    isHapticEnabled: Boolean = true,
    currentLang: String = "ar"
) {
    val isAr = currentLang == "ar"
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val view = LocalView.current

    val infiniteTransition = rememberInfiniteTransition(label = "shield_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    fun formatDetailedCountdown(millis: Long): String {
        if (millis <= 0) return if (isAr) "00ي 00س 00د 00ث" else "00d 00h 00m 00s"
        val totalSecs = millis / 1000
        val days = totalSecs / (24 * 3600)
        val hours = (totalSecs % (24 * 3600)) / 3600
        val mins = (totalSecs % 3600) / 60
        val secs = totalSecs % 60
        return if (isAr) {
            String.format(Locale.US, "%02dي %02dس %02dد %02dث", days, hours, mins, secs)
        } else {
            String.format(Locale.US, "%02dd %02dh %02dm %02ds", days, hours, mins, secs)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0F172A),
        tonalElevation = 12.dp,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .scale(if (isShieldActive) pulseScale else 1f)
                            .clip(CircleShape)
                            .background(if (isShieldActive) Color(0x3310B981) else Color(0x3338BDF8))
                            .border(1.5.dp, if (isShieldActive) NeonEmerald else NeonCyan, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = if (isShieldActive) NeonEmerald else NeonCyan,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.shield_modal_title),
                            fontWeight = FontWeight.Black,
                            fontFamily = PlusJakartaSans,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Text(
                            text = stringResource(R.string.shield_modal_subtitle),
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = if (isAr) "إغلاق" else "Close",
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // SECTION 1: Active Shield Status & Settings Switch
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isShieldActive) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x3310B981))
                                .border(1.dp, NeonEmerald, RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.shield_status_active),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp,
                                color = NeonEmerald
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Large Digital Countdown
                        Text(
                            text = formatDetailedCountdown(remainingMillis),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = PlusJakartaSans,
                            color = NeonEmerald,
                            letterSpacing = 1.sp
                        )

                        Text(
                            text = stringResource(R.string.shield_countdown_label),
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8),
                            modifier = Modifier.padding(top = 2.dp)
                        )

                        if (activeShieldExpiresAt != null) {
                            val expiryStr = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
                                .format(Date(activeShieldExpiresAt))
                            Text(
                                text = if (isAr) "ينتهي في: $expiryStr" else "Expires on: $expiryStr",
                                fontSize = 11.sp,
                                color = Color(0xFFCBD5E1),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.shield_1day_desc),
                            fontSize = 12.sp,
                            color = Color(0xFFE2E8F0),
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x33F43F5E))
                                .border(1.dp, NeonRose, RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.shield_status_inactive),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp,
                                color = NeonRose
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = stringResource(R.string.shield_unprotected).uppercase(Locale.ROOT),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = PlusJakartaSans,
                            color = NeonRose
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isAr) {
                                "الزيادة اليومية عند الخمول (+3 نقاط/يوم) نشطة حالياً. احمِ شعلتك بتفعيل أحد الدروع أدناه أو تشغيل الحماية."
                            } else {
                                "Daily inactivity accrual (+3 pts/day) is active. Protect your flame by activating a shield below or toggling protection on."
                            },
                            fontSize = 12.sp,
                            color = Color(0xFFCBD5E1),
                            fontWeight = FontWeight.Normal
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Settings Toggle Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x221E293B))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.shield_settings_toggle),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = stringResource(R.string.shield_settings_toggle_desc),
                                fontSize = 10.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                        Switch(
                            checked = isShieldActive,
                            onCheckedChange = { active ->
                                HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                                view.playSoundEffect(SoundEffectConstants.CLICK)
                                onToggleShieldActive(active)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = NeonEmerald
                            ),
                            modifier = Modifier.testTag("switch_shield_modal")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // SECTION 2: Available Shield Inventory (كمية الدروع المتوفرة لديك)
            Text(
                text = stringResource(R.string.shield_inventory_title),
                fontWeight = FontWeight.Black,
                fontFamily = PlusJakartaSans,
                fontSize = 15.sp,
                color = NeonCyan,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )

            // INVENTORY ITEM 1: 1-Day Streak Shield (درع 1 يوم)
            LiquidGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_1day_shield"),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
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
                                    .background(Color(0x3338BDF8)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.shield_1day_title),
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = PlusJakartaSans,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = stringResource(R.string.shield_1day_desc),
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (available1DayShields > 0) Color(0x3338BDF8) else Color(0x2264748B))
                                .border(1.dp, if (available1DayShields > 0) NeonCyan else Color.Transparent, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.shield_available_qty, available1DayShields),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.sp,
                                color = if (available1DayShields > 0) NeonCyan else Color(0xFF94A3B8)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                                view.playSoundEffect(SoundEffectConstants.CLICK)
                                onActivate1DayShield()
                            },
                            enabled = available1DayShields > 0,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF0284C7),
                                disabledContainerColor = Color(0x331E293B)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .testTag("btn_activate_1day_shield")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.shield_activate_1day),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                                view.playSoundEffect(SoundEffectConstants.CLICK)
                                onGrant1DayShield()
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .height(42.dp)
                                .testTag("btn_claim_1day_shield")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.shield_claim_1day),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = NeonCyan
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // INVENTORY ITEM 2: 3-Day Streak Shield (درع 3 أيام)
            LiquidGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_3day_shield"),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
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
                                    .background(Color(0x33F59E0B)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = null,
                                    tint = NeonAmber,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.shield_3day_title),
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = PlusJakartaSans,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = stringResource(R.string.shield_3day_desc),
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (availableManualShields > 0) Color(0x33F59E0B) else Color(0x2264748B))
                                .border(1.dp, if (availableManualShields > 0) NeonAmber else Color.Transparent, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.shield_available_qty, availableManualShields),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.sp,
                                color = if (availableManualShields > 0) NeonAmber else Color(0xFF94A3B8)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                            view.playSoundEffect(SoundEffectConstants.CLICK)
                            onActivateManualShield()
                        },
                        enabled = availableManualShields > 0,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFD97706),
                            disabledContainerColor = Color(0x331E293B)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .testTag("btn_activate_3day_shield")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.shield_activate_3day),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // INVENTORY ITEM 3: 7-Day Auto Shield (درع 7 أيام تلقائي)
            LiquidGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_7day_shield"),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
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
                                    .background(Color(0x33EC4899)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocalFireDepartment,
                                    contentDescription = null,
                                    tint = Color(0xFFF472B6),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.shield_7day_title),
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = PlusJakartaSans,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = stringResource(R.string.shield_7day_desc),
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x33EC4899))
                                .border(1.dp, Color(0xFFF472B6), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = if (isAr) "تلقائي" else "AUTO",
                                fontWeight = FontWeight.Black,
                                fontSize = 10.sp,
                                color = Color(0xFFF472B6)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // SECTION 3: Complete Explanation of Shield & Penalty Rules (شرح قوانين نظام الدرع)
            Text(
                text = stringResource(R.string.shield_rules_summary_title),
                fontWeight = FontWeight.Black,
                fontFamily = PlusJakartaSans,
                fontSize = 15.sp,
                color = NeonCyan,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )

            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Rule 1: Inactivity Protection
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0x3310B981)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = NeonEmerald,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.rule_inactivity_title),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = NeonEmerald
                            )
                            Text(
                                text = stringResource(R.string.rule_inactivity_desc),
                                fontSize = 11.sp,
                                color = Color(0xFFCBD5E1)
                            )
                        }
                    }

                    // Rule 2: 1-Day Shield
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0x3338BDF8)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.shield_1day_title),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = NeonCyan
                            )
                            Text(
                                text = stringResource(R.string.shield_1day_desc),
                                fontSize = 11.sp,
                                color = Color(0xFFCBD5E1)
                            )
                        }
                    }

                    // Rule 3: Double Strike Penalty & 3-Day Shield
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0x33F59E0B)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = NeonAmber,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.rule_double_strike_title),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = NeonAmber
                            )
                            Text(
                                text = stringResource(R.string.rule_double_strike_desc),
                                fontSize = 11.sp,
                                color = Color(0xFFCBD5E1)
                            )
                        }
                    }

                    // Rule 4: Multi-Strike Combo (3-5) & 7-Day Auto Shield
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0x33EC4899)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                tint = Color(0xFFF472B6),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.rule_multi_strike_title),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFFF472B6)
                            )
                            Text(
                                text = stringResource(R.string.rule_multi_strike_desc),
                                fontSize = 11.sp,
                                color = Color(0xFFCBD5E1)
                            )
                        }
                    }

                    // Rule 5: Extreme Overload (6+)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0x33EF4444)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.rule_extreme_overload_title),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFFEF4444)
                            )
                            Text(
                                text = stringResource(R.string.rule_extreme_overload_desc),
                                fontSize = 11.sp,
                                color = Color(0xFFCBD5E1)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}
