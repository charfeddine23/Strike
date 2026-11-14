package com.example.ui.components

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.flame.FlameRecalculationResult
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonRose
import com.example.ui.theme.PlusJakartaSans
import com.example.ui.viewmodel.IndividualViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun StreakStartDateDialog(
    viewModel: IndividualViewModel,
    onDismiss: () -> Unit,
    isDarkTheme: Boolean = isSystemInDarkTheme()
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val currentLang by viewModel.currentLanguage.collectAsStateWithLifecycle()
    val isAr = currentLang == "ar"

    val initialDate = remember {
        viewModel.getStreakStartDate() ?: SimpleDateFormat("yyyy-MM-dd", Locale.US).format(
            Date(System.currentTimeMillis() - 30L * 24 * 3600 * 1000L) // Default suggestion: 30 days ago
        )
    }

    var dateInput by remember { mutableStateOf(initialDate) }
    var previewResult by remember { mutableStateOf<FlameRecalculationResult?>(null) }
    var isValidDate by remember { mutableStateOf(true) }

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false } }

    fun validateAndPreview(dateStr: String) {
        try {
            val parsed = dateFormat.parse(dateStr.trim())
            val today = Date()
            if (parsed != null && !parsed.after(today)) {
                isValidDate = true
                previewResult = viewModel.previewStartDateRecalculation(dateStr.trim())
            } else {
                isValidDate = false
                previewResult = null
            }
        } catch (_: Exception) {
            isValidDate = false
            previewResult = null
        }
    }

    LaunchedEffect(dateInput) {
        validateAndPreview(dateInput)
    }

    fun openCalendarPicker() {
        val calendar = Calendar.getInstance()
        try {
            val parsed = dateFormat.parse(dateInput)
            if (parsed != null) calendar.time = parsed
        } catch (_: Exception) {
        }

        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val selected = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                }
                val formatted = dateFormat.format(selected.time)
                dateInput = formatted
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.maxDate = System.currentTimeMillis()
            show()
        }
    }

    fun setQuickPreset(daysAgo: Int) {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
        dateInput = dateFormat.format(cal.time)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(24.dp))
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(NeonCyan.copy(alpha = 0.8f), NeonAmber.copy(alpha = 0.4f), Color(0x3338BDF8))
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .testTag("streak_start_date_dialog"),
            color = Color(0xFF0B1426),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
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
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(NeonAmber.copy(alpha = 0.2f))
                                .border(1.dp, NeonAmber.copy(alpha = 0.7f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                tint = NeonAmber,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (isAr) "تاريخ بداية العد" else "Streak Start Date",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                fontFamily = PlusJakartaSans
                            )
                            Text(
                                text = if (isAr) "إعادة احتساب نقاط الشعلة تلقائياً" else "Streak Start Date Recalculation",
                                fontSize = 11.sp,
                                color = NeonCyan,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0x22FFFFFF))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = if (isAr) "إغلاق" else "Close",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Explanatory Subtitle
                Text(
                    text = if (isAr) {
                        "أدخل تاريخ بدء السلسلة لحساب الشعلة تلقائياً. تُضاف +3 نقاط لكل يوم خمول مضى، وتُخصم نقاط كافة الضربات المسجّلة خلال هذه المدة تلقائياً."
                    } else {
                        "Enter your streak start date to calculate your flame automatically. +3 pts are added for each elapsed clean day, and all recorded strikes during this period are automatically deducted."
                    },
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.75f),
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Date Input Field with Calendar Trigger
                OutlinedTextField(
                    value = dateInput,
                    onValueChange = { dateInput = it },
                    label = {
                        Text(
                            text = if (isAr) "تاريخ البداية (YYYY-MM-DD)" else "Start Date (YYYY-MM-DD)",
                            color = NeonCyan.copy(alpha = 0.8f),
                            fontSize = 12.sp
                        )
                    },
                    placeholder = {
                        Text(
                            text = if (isAr) "مثال: 2026-10-15" else "e.g. 2026-10-15",
                            color = Color(0x66FFFFFF)
                        )
                    },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontFamily = PlusJakartaSans
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("date_input_field"),
                    shape = RoundedCornerShape(14.dp),
                    trailingIcon = {
                        IconButton(onClick = { openCalendarPicker() }) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = if (isAr) "اختيار التاريخ" else "Pick Date",
                                tint = NeonCyan
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = Color(0x4438BDF8),
                        focusedContainerColor = Color(0x1A1E293B),
                        unfocusedContainerColor = Color(0x1A1E293B),
                        cursorColor = NeonCyan
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { focusManager.clearFocus() }
                    ),
                    isError = !isValidDate
                )

                if (!isValidDate) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isAr) {
                            "الرجاء إدخال تاريخ صحيح بصيغة YYYY-MM-DD وليس في المستقبل"
                        } else {
                            "Please enter a valid date (YYYY-MM-DD) not in the future"
                        },
                        fontSize = 11.sp,
                        color = NeonRose,
                        modifier = Modifier.align(Alignment.Start)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Quick Preset Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PresetDateChip(if (isAr) "اليوم (0ي)" else "Today (0d)", onClick = { setQuickPreset(0) })
                    PresetDateChip(if (isAr) "منذ أسبوع (7ي)" else "1 Week (7d)", onClick = { setQuickPreset(7) })
                    PresetDateChip(if (isAr) "منذ 15 يوماً" else "15 Days", onClick = { setQuickPreset(15) })
                    PresetDateChip(if (isAr) "منذ شهر (30ي)" else "1 Month (30d)", onClick = { setQuickPreset(30) })
                    PresetDateChip(if (isAr) "منذ شهرين (60ي)" else "2 Months (60d)", onClick = { setQuickPreset(60) })
                    PresetDateChip(if (isAr) "منذ 3 أشهر (90ي)" else "3 Months (90d)", onClick = { setQuickPreset(90) })
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Live Preview Calculation Card
                previewResult?.let { res ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0x221E293B))
                            .border(1.dp, Color(0x4038BDF8), RoundedCornerShape(16.dp))
                            .padding(14.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isAr) "المعاينة الحسابية التلقائية" else "Automatic Calculation Preview",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonCyan,
                                    fontFamily = PlusJakartaSans
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(NeonEmerald.copy(alpha = 0.2f))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (isAr) "تلقائي ودقيق" else "Auto & Verified",
                                        fontSize = 10.sp,
                                        color = NeonEmerald,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Elapsed Days & Inactivity Points
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.TrendingUp,
                                        contentDescription = null,
                                        tint = NeonAmber,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isAr) "الأيام المنقضية (${res.daysElapsed} يوم):" else "Elapsed Days (${res.daysElapsed}d):",
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.9f)
                                    )
                                }
                                Text(
                                    text = if (isAr) {
                                        "+${res.inactivityPointsAdded.toInt()} نقطة (+3/يوم)"
                                    } else {
                                        "+${res.inactivityPointsAdded.toInt()} PTS (+3/day)"
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonAmber
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Registered Strikes in Period
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.TrendingDown,
                                        contentDescription = null,
                                        tint = NeonRose,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isAr) {
                                            "الضربات المكتشفة (${res.totalStrikesCount}):"
                                        } else {
                                            "Detected Strikes (${res.totalStrikesCount}):"
                                        },
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.9f)
                                    )
                                }
                                Text(
                                    text = if (isAr) {
                                        "-${String.format(Locale.US, "%.1f", res.totalDeductions)} نقطة"
                                    } else {
                                        "-${String.format(Locale.US, "%.1f", res.totalDeductions)} PTS"
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonRose
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(Color(0x33FFFFFF))
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            // Resulting Final Score
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isAr) "النتيجة النهائية للشعلة:" else "Resulting Flame Score:",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Text(
                                    text = if (isAr) {
                                        "${String.format(Locale.US, "%.1f", res.resultingScore)} نقطة"
                                    } else {
                                        "${String.format(Locale.US, "%.1f", res.resultingScore)} PTS"
                                    },
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (res.resultingScore > 2000.0) NeonCyan else NeonAmber,
                                    fontFamily = PlusJakartaSans
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("btn_cancel_start_date"),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x4038BDF8))
                    ) {
                        Text(
                            text = if (isAr) "إلغاء" else "Cancel",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 13.sp
                        )
                    }

                    Button(
                        onClick = {
                            if (isValidDate) {
                                viewModel.recalculateFlameScoreFromStartDate(dateInput.trim())
                                onDismiss()
                            }
                        },
                        enabled = isValidDate && previewResult != null,
                        modifier = Modifier
                            .weight(1.4f)
                            .height(48.dp)
                            .testTag("btn_apply_start_date"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonCyan,
                            disabledContainerColor = Color(0x331E293B)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isAr) "تطبيق وإعادة الحساب" else "Apply & Recalculate",
                            color = Color.Black,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            fontFamily = PlusJakartaSans
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PresetDateChip(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0x221E293B))
            .border(1.dp, Color(0x3338BDF8), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = NeonCyan,
            fontWeight = FontWeight.SemiBold
        )
    }
}
