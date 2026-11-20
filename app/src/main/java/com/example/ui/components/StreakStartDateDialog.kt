package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.flame.DateRangeConfig
import com.example.ui.theme.DayCyan
import com.example.ui.theme.DayEmerald
import com.example.ui.theme.DayOrange
import com.example.ui.theme.DayRose
import com.example.ui.theme.LocalAppDarkTheme
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
import java.util.TimeZone

private enum class PickerTarget {
    GLOBAL_START,
    GLOBAL_END,
    INACTIVITY_START,
    INACTIVITY_END,
    STRIKES_START,
    STRIKES_END,
    GIRL_START,
    GIRL_END,
    DEFLAME_START,
    DEFLAME_END
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreakStartDateDialog(
    viewModel: IndividualViewModel,
    onDismiss: () -> Unit,
    isDarkTheme: Boolean = LocalAppDarkTheme.current
) {
    val focusManager = LocalFocusManager.current
    val currentLang by viewModel.currentLanguage.collectAsStateWithLifecycle()
    val isAr = currentLang == "ar"
    val isFr = currentLang == "fr"

    val savedConfig = remember { viewModel.getSavedRecalculationConfig() }
    val todayStr = remember { localTodayString() }

    var globalStartDate by remember { mutableStateOf(savedConfig.globalStartDate.coerceMax(todayStr)) }
    var globalEndDate by remember {
        mutableStateOf(
            savedConfig.globalEndDate.coerceInRange(globalStartDate, todayStr)
        )
    }

    // Per-element custom date range states
    var inactivityRange by remember {
        mutableStateOf(
            savedConfig.inactivityRange ?: DateRangeConfig(globalStartDate, globalEndDate, isCustom = false)
        )
    }
    var strikesRange by remember {
        mutableStateOf(
            savedConfig.strikesRange ?: DateRangeConfig(globalStartDate, globalEndDate, isCustom = false)
        )
    }
    var girlRange by remember {
        mutableStateOf(
            savedConfig.girlRange ?: DateRangeConfig(globalStartDate, globalEndDate, isCustom = false)
        )
    }
    var deflameRange by remember {
        mutableStateOf(
            savedConfig.deflameRange ?: DateRangeConfig(globalStartDate, globalEndDate, isCustom = false)
        )
    }

    var girlPerMonthInput by remember {
        mutableStateOf(if (savedConfig.girlPerMonth > 0) savedConfig.girlPerMonth.toString() else "")
    }
    var deflamePerMonthInput by remember {
        mutableStateOf(if (savedConfig.deflamePerMonth > 0) savedConfig.deflamePerMonth.toString() else "")
    }

    val girlPerMonth = girlPerMonthInput.toIntOrNull()?.coerceAtLeast(0) ?: 0
    val deflamePerMonth = deflamePerMonthInput.toIntOrNull()?.coerceAtLeast(0) ?: 0

    var activePickerTarget by remember { mutableStateOf<PickerTarget?>(null) }

    val allStrikeEvents by viewModel.allStrikeEvents.collectAsStateWithLifecycle()

    val previewResult = remember(
        globalStartDate,
        globalEndDate,
        inactivityRange,
        strikesRange,
        girlRange,
        deflameRange,
        girlPerMonth,
        deflamePerMonth,
        allStrikeEvents
    ) {
        viewModel.previewStartDateRecalculation(
            startDateStr = globalStartDate,
            girlPerMonth = girlPerMonth,
            deflamePerMonth = deflamePerMonth,
            endDateStr = globalEndDate,
            inactivityRange = inactivityRange,
            strikesRange = strikesRange,
            girlRange = girlRange,
            deflameRange = deflameRange
        )
    }

    val effectiveLayoutDirection = if (isAr) LayoutDirection.Rtl else LayoutDirection.Ltr
    val accentCyan = if (isDarkTheme) NeonCyan else DayCyan
    val accentEmerald = if (isDarkTheme) NeonEmerald else DayEmerald
    val accentAmber = if (isDarkTheme) NeonAmber else DayOrange
    val accentRose = if (isDarkTheme) NeonRose else DayRose
    val girlAccent = if (isDarkTheme) Color(0xFFFB7185) else DayRose
    val deflameAccent = accentCyan

    val dialogBg = if (isDarkTheme) Color(0xFF0B1426) else Color(0xFFF8FAFC)
    val mainTextColor = if (isDarkTheme) Color.White else Color(0xFF0F172A)
    val secondaryTextColor = if (isDarkTheme) Color(0xFFE2E8F0) else Color(0xFF334155)
    val mutedTextColor = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)
    val sectionBgColor = if (isDarkTheme) Color(0x2B16233B) else Color(0xFFFFFFFF)
    val previewCardBg = if (isDarkTheme) Color(0x221E293B) else Color(0xFFFFFFFF)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides effectiveLayoutDirection) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .heightIn(max = 760.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            listOf(
                                accentCyan.copy(alpha = 0.75f),
                                accentAmber.copy(alpha = 0.45f),
                                accentCyan.copy(alpha = 0.25f)
                            )
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .testTag("streak_start_date_dialog"),
                color = dialogBg,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(accentAmber.copy(alpha = 0.18f))
                                    .border(1.dp, accentAmber.copy(alpha = 0.65f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocalFireDepartment,
                                    contentDescription = null,
                                    tint = accentAmber,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = when {
                                        isAr -> "فترة بداية ونهاية العد"
                                        isFr -> "Période de calcul"
                                        else -> "Streak Date Range"
                                    },
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = mainTextColor,
                                    fontFamily = PlusJakartaSans
                                )
                                Text(
                                    text = when {
                                        isAr -> "تحديد من التقويم وتخصيص لكل عنصر"
                                        isFr -> "Sélection par calendrier & par élément"
                                        else -> "Calendar-only & per-element ranges"
                                    },
                                    fontSize = 11.sp,
                                    color = accentCyan,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("btn_close_start_date_dialog")
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(if (isDarkTheme) Color(0x22FFFFFF) else Color(0xFFE2E8F0)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = when {
                                        isAr -> "إغلاق"
                                        isFr -> "Fermer"
                                        else -> "Close"
                                    },
                                    tint = mainTextColor.copy(alpha = 0.75f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = when {
                            isAr -> "اختر تاريخ البداية والنهاية من التقويم فقط لمنع تداخل الحسابات، مع إمكانية تخصيص فترة مستقلة لكل عنصر على حدة."
                            isFr -> "Choisissez la date de début et de fin uniquement depuis le calendrier et personnalisez chaque élément indépendamment."
                            else -> "Pick start & end dates exclusively from the calendar to keep period math isolated, and customize dates per element if needed."
                        },
                        fontSize = 11.5.sp,
                        color = secondaryTextColor.copy(alpha = 0.82f),
                        lineHeight = 17.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // MAIN GLOBAL DATE RANGE CARD (CALENDAR PICKER ONLY)
                    val globalDaysCount = remember(globalStartDate, globalEndDate) {
                        daysBetweenStrings(globalStartDate, globalEndDate)
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(sectionBgColor)
                            .border(1.dp, accentCyan.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = null,
                                        tint = accentCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = when {
                                            isAr -> "الفترة الزمنية الرئيسية"
                                            isFr -> "Période principale"
                                            else -> "Main Counting Period"
                                        },
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = mainTextColor,
                                        fontFamily = PlusJakartaSans
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(accentCyan.copy(alpha = 0.14f))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = when {
                                            isAr -> "$globalDaysCount يوم"
                                            isFr -> "$globalDaysCount j"
                                            else -> "$globalDaysCount days"
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = accentCyan
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CalendarDateButton(
                                    label = when {
                                        isAr -> "تاريخ بداية العد"
                                        isFr -> "Date de début"
                                        else -> "Start Date"
                                    },
                                    dateStr = globalStartDate,
                                    accentColor = accentCyan,
                                    isDarkTheme = isDarkTheme,
                                    testTag = "btn_pick_global_start_date",
                                    onClick = { activePickerTarget = PickerTarget.GLOBAL_START },
                                    modifier = Modifier.weight(1f)
                                )
                                CalendarDateButton(
                                    label = when {
                                        isAr -> "تاريخ نهاية العد"
                                        isFr -> "Date de fin"
                                        else -> "End Date"
                                    },
                                    dateStr = globalEndDate,
                                    accentColor = accentEmerald,
                                    isDarkTheme = isDarkTheme,
                                    testTag = "btn_pick_global_end_date",
                                    onClick = { activePickerTarget = PickerTarget.GLOBAL_END },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // PER-ELEMENT DATE CUSTOMIZATION SECTION HEADER
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when {
                                isAr -> "تخصيص التاريخ لكل عنصر على حدة"
                                isFr -> "Dates personnalisées par élément"
                                else -> "Customize Dates Per Element"
                            },
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = secondaryTextColor
                        )
                        Text(
                            text = when {
                                isAr -> "اختيار من التقويم"
                                isFr -> "Calendrier uniquement"
                                else -> "Calendar only"
                            },
                            fontSize = 10.sp,
                            color = mutedTextColor
                        )
                    }

                    // ELEMENT 1: INACTIVITY / STREAK DAYS (+3 PTS / DAY)
                    PerElementDateCard(
                        title = when {
                            isAr -> "أيام السلسلة (+3 نقاط/يوم)"
                            isFr -> "Jours d'inactivité (+3 pts/j)"
                            else -> "Streak Days (+3 PTS/day)"
                        },
                        subtitle = "${previewResult.daysElapsed} " + (if (isAr) "يوم" else if (isFr) "jours" else "days") +
                            " • +${previewResult.inactivityPointsAdded.toInt()} " + (if (isAr) "نقطة" else "PTS"),
                        icon = Icons.Default.TrendingUp,
                        accentColor = accentAmber,
                        isDarkTheme = isDarkTheme,
                        range = inactivityRange,
                        globalStartDate = globalStartDate,
                        globalEndDate = globalEndDate,
                        isAr = isAr,
                        isFr = isFr,
                        toggleTestTag = "toggle_custom_inactivity",
                        startBtnTestTag = "btn_pick_inactivity_start",
                        endBtnTestTag = "btn_pick_inactivity_end",
                        onToggleCustom = { enabled ->
                            inactivityRange = if (enabled) {
                                DateRangeConfig(
                                    startDate = inactivityRange.startDate.ifBlank { globalStartDate }.coerceMax(todayStr),
                                    endDate = inactivityRange.endDate.ifBlank { globalEndDate }.coerceInRange(globalStartDate, todayStr),
                                    isCustom = true
                                )
                            } else {
                                inactivityRange.copy(isCustom = false)
                            }
                        },
                        onPickStart = { activePickerTarget = PickerTarget.INACTIVITY_START },
                        onPickEnd = { activePickerTarget = PickerTarget.INACTIVITY_END }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // ELEMENT 2: DETECTED STRIKES
                    PerElementDateCard(
                        title = when {
                            isAr -> "الضربات المسجلة (Strikes)"
                            isFr -> "Strikes détectés"
                            else -> "Recorded Strikes"
                        },
                        subtitle = "${previewResult.totalStrikesCount} " +
                            (if (isAr) "سترايك" else "strikes") +
                            " • -${String.format(Locale.US, "%.1f", previewResult.totalDeductions)} " +
                            (if (isAr) "نقطة" else "PTS"),
                        icon = Icons.Default.Bolt,
                        accentColor = accentRose,
                        isDarkTheme = isDarkTheme,
                        range = strikesRange,
                        globalStartDate = globalStartDate,
                        globalEndDate = globalEndDate,
                        isAr = isAr,
                        isFr = isFr,
                        toggleTestTag = "toggle_custom_strikes",
                        startBtnTestTag = "btn_pick_strikes_start",
                        endBtnTestTag = "btn_pick_strikes_end",
                        onToggleCustom = { enabled ->
                            strikesRange = if (enabled) {
                                DateRangeConfig(
                                    startDate = strikesRange.startDate.ifBlank { globalStartDate }.coerceMax(todayStr),
                                    endDate = strikesRange.endDate.ifBlank { globalEndDate }.coerceInRange(globalStartDate, todayStr),
                                    isCustom = true
                                )
                            } else {
                                strikesRange.copy(isCustom = false)
                            }
                        },
                        onPickStart = { activePickerTarget = PickerTarget.STRIKES_START },
                        onPickEnd = { activePickerTarget = PickerTarget.STRIKES_END }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // ELEMENT 3: GIRL BUTTON PRESSES + PER-ELEMENT DATES
                    PerElementDateCard(
                        title = when {
                            isAr -> "مرات الضغط على زر Girl"
                            isFr -> "Appuis bouton Girl"
                            else -> "Girl Button Presses"
                        },
                        subtitle = "${previewResult.girlPressesCount} " +
                            (if (isAr) "مرة خلال" else if (isFr) "fois sur" else "presses over") +
                            " ${previewResult.girlDaysElapsed} " +
                            (if (isAr) "يوم" else if (isFr) "j" else "d"),
                        icon = Icons.Default.Favorite,
                        accentColor = girlAccent,
                        isDarkTheme = isDarkTheme,
                        range = girlRange,
                        globalStartDate = globalStartDate,
                        globalEndDate = globalEndDate,
                        isAr = isAr,
                        isFr = isFr,
                        toggleTestTag = "toggle_custom_girl",
                        startBtnTestTag = "btn_pick_girl_start",
                        endBtnTestTag = "btn_pick_girl_end",
                        onToggleCustom = { enabled ->
                            girlRange = if (enabled) {
                                DateRangeConfig(
                                    startDate = girlRange.startDate.ifBlank { globalStartDate }.coerceMax(todayStr),
                                    endDate = girlRange.endDate.ifBlank { globalEndDate }.coerceInRange(globalStartDate, todayStr),
                                    isCustom = true
                                )
                            } else {
                                girlRange.copy(isCustom = false)
                            }
                        },
                        onPickStart = { activePickerTarget = PickerTarget.GIRL_START },
                        onPickEnd = { activePickerTarget = PickerTarget.GIRL_END },
                        extraContent = {
                            OutlinedTextField(
                                value = girlPerMonthInput,
                                onValueChange = { raw ->
                                    girlPerMonthInput = raw.filter { it.isDigit() }.take(4)
                                },
                                label = {
                                    Text(
                                        text = when {
                                            isAr -> "عدد مرات الضغط على Girl في الشهر"
                                            isFr -> "Appuis Girl par mois"
                                            else -> "Girl presses per month"
                                        },
                                        color = girlAccent.copy(alpha = 0.9f),
                                        fontSize = 11.5.sp
                                    )
                                },
                                placeholder = { Text("0", color = mutedTextColor.copy(alpha = 0.6f)) },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    color = mainTextColor,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = PlusJakartaSans
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("girl_per_month_input"),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = girlAccent,
                                    unfocusedBorderColor = girlAccent.copy(alpha = 0.35f),
                                    focusedContainerColor = if (isDarkTheme) Color(0x1A1E293B) else Color(0xFFF1F5F9),
                                    unfocusedContainerColor = if (isDarkTheme) Color(0x1A1E293B) else Color(0xFFF1F5F9),
                                    cursorColor = girlAccent
                                ),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Next
                                )
                            )
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // ELEMENT 4: DEFLAME BUTTON PRESSES + PER-ELEMENT DATES
                    PerElementDateCard(
                        title = when {
                            isAr -> "مرات الضغط على زر Deflame"
                            isFr -> "Appuis bouton Deflame"
                            else -> "Deflame Button Presses"
                        },
                        subtitle = "${previewResult.deflamePressesCount} " +
                            (if (isAr) "مرة خلال" else if (isFr) "fois sur" else "presses over") +
                            " ${previewResult.deflameDaysElapsed} " +
                            (if (isAr) "يوم" else if (isFr) "j" else "d"),
                        icon = Icons.Default.AcUnit,
                        accentColor = deflameAccent,
                        isDarkTheme = isDarkTheme,
                        range = deflameRange,
                        globalStartDate = globalStartDate,
                        globalEndDate = globalEndDate,
                        isAr = isAr,
                        isFr = isFr,
                        toggleTestTag = "toggle_custom_deflame",
                        startBtnTestTag = "btn_pick_deflame_start",
                        endBtnTestTag = "btn_pick_deflame_end",
                        onToggleCustom = { enabled ->
                            deflameRange = if (enabled) {
                                DateRangeConfig(
                                    startDate = deflameRange.startDate.ifBlank { globalStartDate }.coerceMax(todayStr),
                                    endDate = deflameRange.endDate.ifBlank { globalEndDate }.coerceInRange(globalStartDate, todayStr),
                                    isCustom = true
                                )
                            } else {
                                deflameRange.copy(isCustom = false)
                            }
                        },
                        onPickStart = { activePickerTarget = PickerTarget.DEFLAME_START },
                        onPickEnd = { activePickerTarget = PickerTarget.DEFLAME_END },
                        extraContent = {
                            OutlinedTextField(
                                value = deflamePerMonthInput,
                                onValueChange = { raw ->
                                    deflamePerMonthInput = raw.filter { it.isDigit() }.take(4)
                                },
                                label = {
                                    Text(
                                        text = when {
                                            isAr -> "عدد مرات الضغط على Deflame في الشهر"
                                            isFr -> "Appuis Deflame par mois"
                                            else -> "Deflame presses per month"
                                        },
                                        color = deflameAccent.copy(alpha = 0.9f),
                                        fontSize = 11.5.sp
                                    )
                                },
                                placeholder = { Text("0", color = mutedTextColor.copy(alpha = 0.6f)) },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    color = mainTextColor,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = PlusJakartaSans
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("deflame_per_month_input"),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = deflameAccent,
                                    unfocusedBorderColor = deflameAccent.copy(alpha = 0.35f),
                                    focusedContainerColor = if (isDarkTheme) Color(0x1A1E293B) else Color(0xFFF1F5F9),
                                    unfocusedContainerColor = if (isDarkTheme) Color(0x1A1E293B) else Color(0xFFF1F5F9),
                                    cursorColor = deflameAccent
                                ),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = { focusManager.clearFocus() }
                                )
                            )
                        }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // LIVE AUTOMATIC PREVIEW CARD
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(previewCardBg)
                            .border(1.dp, accentCyan.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                            .padding(14.dp)
                            .testTag("preview_calculation_card")
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = when {
                                        isAr -> "المعاينة الحسابية التلقائية"
                                        isFr -> "Aperçu du calcul"
                                        else -> "Calculation Preview"
                                    },
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = accentCyan,
                                    fontFamily = PlusJakartaSans
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(accentEmerald.copy(alpha = 0.16f))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = when {
                                            isAr -> "تلقائي ودقيق"
                                            isFr -> "Auto & Vérifié"
                                            else -> "Auto & Verified"
                                        },
                                        fontSize = 10.sp,
                                        color = accentEmerald,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            PreviewLineRow(
                                icon = Icons.Default.TrendingUp,
                                tint = accentAmber,
                                label = when {
                                    isAr -> "الأيام (${previewResult.daysElapsed}ي • ${previewResult.inactivityStartDate}→${previewResult.inactivityEndDate}):"
                                    else -> "Days (${previewResult.daysElapsed}d • ${previewResult.inactivityStartDate}→${previewResult.inactivityEndDate}):"
                                },
                                value = "+${previewResult.inactivityPointsAdded.toInt()} " + if (isAr) "نقطة" else "PTS",
                                isDarkTheme = isDarkTheme
                            )

                            PreviewLineRow(
                                icon = Icons.Default.TrendingDown,
                                tint = accentRose,
                                label = when {
                                    isAr -> "الضربات (${previewResult.totalStrikesCount} • ${previewResult.strikesStartDate}→${previewResult.strikesEndDate}):"
                                    else -> "Strikes (${previewResult.totalStrikesCount} • ${previewResult.strikesStartDate}→${previewResult.strikesEndDate}):"
                                },
                                value = "-${String.format(Locale.US, "%.1f", previewResult.totalDeductions)} " + if (isAr) "نقطة" else "PTS",
                                isDarkTheme = isDarkTheme
                            )

                            if (previewResult.girlPerMonth > 0 || previewResult.girlPressesCount > 0) {
                                PreviewLineRow(
                                    icon = Icons.Default.Favorite,
                                    tint = girlAccent,
                                    label = when {
                                        isAr -> "Girl (${previewResult.girlPressesCount}× • ${previewResult.girlStartDate}→${previewResult.girlEndDate}):"
                                        else -> "Girl (${previewResult.girlPressesCount}× • ${previewResult.girlStartDate}→${previewResult.girlEndDate}):"
                                    },
                                    value = "-${String.format(Locale.US, "%.1f", previewResult.girlPointsDeducted)} " + if (isAr) "نقطة" else "PTS",
                                    isDarkTheme = isDarkTheme
                                )
                            }

                            if (previewResult.deflamePerMonth > 0 || previewResult.deflamePressesCount > 0) {
                                PreviewLineRow(
                                    icon = Icons.Default.AcUnit,
                                    tint = deflameAccent,
                                    label = when {
                                        isAr -> "Deflame (${previewResult.deflamePressesCount}× • ${previewResult.deflameStartDate}→${previewResult.deflameEndDate}):"
                                        else -> "Deflame (${previewResult.deflamePressesCount}× • ${previewResult.deflameStartDate}→${previewResult.deflameEndDate}):"
                                    },
                                    value = "-${String.format(Locale.US, "%.1f", previewResult.deflamePointsDeducted)} " + if (isAr) "نقطة" else "PTS",
                                    isDarkTheme = isDarkTheme
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(if (isDarkTheme) Color(0x33FFFFFF) else Color(0xFFE2E8F0))
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = when {
                                        isAr -> "ذروة الشعلة المقدرة:"
                                        isFr -> "Pic estimé :"
                                        else -> "Estimated Peak:"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = accentAmber
                                )
                                Text(
                                    text = "${String.format(Locale.US, "%.1f", previewResult.estimatedPeakScore)} " + if (isAr) "نقطة" else "PTS",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = accentAmber,
                                    fontFamily = PlusJakartaSans
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = when {
                                        isAr -> "إجمالي الإطفاء:"
                                        isFr -> "Total éteint :"
                                        else -> "Total Extinguished:"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = accentCyan
                                )
                                Text(
                                    text = "-${String.format(Locale.US, "%.1f", previewResult.totalExtinguishedPoints)} " + if (isAr) "نقطة" else "PTS",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = accentCyan,
                                    fontFamily = PlusJakartaSans
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = when {
                                        isAr -> "النتيجة النهائية للشعلة:"
                                        isFr -> "Score final :"
                                        else -> "Resulting Flame Score:"
                                    },
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = mainTextColor
                                )
                                Text(
                                    text = if (isAr) {
                                        "${String.format(Locale.US, "%.1f", previewResult.resultingScore)} نقطة"
                                    } else {
                                        "${String.format(Locale.US, "%.1f", previewResult.resultingScore)} PTS"
                                    },
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (previewResult.resultingScore > 2000.0) accentCyan else accentAmber,
                                    fontFamily = PlusJakartaSans
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

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
                            border = BorderStroke(1.dp, accentCyan.copy(alpha = 0.45f))
                        ) {
                            Text(
                                text = when {
                                    isAr -> "إلغاء"
                                    isFr -> "Annuler"
                                    else -> "Cancel"
                                },
                                color = secondaryTextColor,
                                fontSize = 13.sp
                            )
                        }

                        Button(
                            onClick = {
                                viewModel.recalculateFlameScoreFromStartDate(
                                    startDateStr = globalStartDate,
                                    girlPerMonth = girlPerMonth,
                                    deflamePerMonth = deflamePerMonth,
                                    endDateStr = globalEndDate,
                                    inactivityRange = inactivityRange,
                                    strikesRange = strikesRange,
                                    girlRange = girlRange,
                                    deflameRange = deflameRange
                                )
                                onDismiss()
                            },
                            modifier = Modifier
                                .weight(1.4f)
                                .height(48.dp)
                                .testTag("btn_apply_start_date"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accentCyan,
                                contentColor = Color(0xFF021B26),
                                disabledContainerColor = if (isDarkTheme) Color(0x331E293B) else Color(0xFFE2E8F0)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF021B26),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = when {
                                    isAr -> "تطبيق وإعادة الحساب"
                                    isFr -> "Appliquer et recalculer"
                                    else -> "Apply & Recalculate"
                                },
                                color = Color(0xFF021B26),
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

    // CALENDAR-ONLY PICKER DIALOG (Material 3 Calendar Grid with Dark Liquid Glass styling)
    activePickerTarget?.let { target ->
        val (currentSelectedStr, minBoundStr) = when (target) {
            PickerTarget.GLOBAL_START -> globalStartDate to null
            PickerTarget.GLOBAL_END -> globalEndDate to globalStartDate
            PickerTarget.INACTIVITY_START -> inactivityRange.startDate to null
            PickerTarget.INACTIVITY_END -> inactivityRange.endDate to inactivityRange.startDate
            PickerTarget.STRIKES_START -> strikesRange.startDate to null
            PickerTarget.STRIKES_END -> strikesRange.endDate to strikesRange.startDate
            PickerTarget.GIRL_START -> girlRange.startDate to null
            PickerTarget.GIRL_END -> girlRange.endDate to girlRange.startDate
            PickerTarget.DEFLAME_START -> deflameRange.startDate to null
            PickerTarget.DEFLAME_END -> deflameRange.endDate to deflameRange.startDate
        }

        val initialUtcMillis = remember(target, currentSelectedStr) {
            dateStringToUtcMidnightMillis(currentSelectedStr) ?: todayUtcMidnightMillis()
        }
        val todayUtcMillis = remember { todayUtcMidnightMillis() }
        val minUtcMillis = remember(minBoundStr) {
            minBoundStr?.let { dateStringToUtcMidnightMillis(it) }
        }

        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialUtcMillis,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val notFuture = utcTimeMillis <= todayUtcMillis
                    val afterMin = minUtcMillis == null || utcTimeMillis >= minUtcMillis
                    return notFuture && afterMin
                }
            }
        )

        DatePickerDialog(
            onDismissRequest = { activePickerTarget = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        val pickedMillis = datePickerState.selectedDateMillis ?: initialUtcMillis
                        val pickedStr = utcMidnightMillisToDateString(pickedMillis).coerceMax(todayStr)
                        when (target) {
                            PickerTarget.GLOBAL_START -> {
                                globalStartDate = pickedStr
                                if (globalEndDate < pickedStr) globalEndDate = pickedStr
                            }
                            PickerTarget.GLOBAL_END -> {
                                globalEndDate = pickedStr.coerceInRange(globalStartDate, todayStr)
                            }
                            PickerTarget.INACTIVITY_START -> {
                                val nextEnd = if (inactivityRange.endDate < pickedStr) pickedStr else inactivityRange.endDate
                                inactivityRange = inactivityRange.copy(startDate = pickedStr, endDate = nextEnd, isCustom = true)
                            }
                            PickerTarget.INACTIVITY_END -> {
                                inactivityRange = inactivityRange.copy(
                                    endDate = pickedStr.coerceInRange(inactivityRange.startDate, todayStr),
                                    isCustom = true
                                )
                            }
                            PickerTarget.STRIKES_START -> {
                                val nextEnd = if (strikesRange.endDate < pickedStr) pickedStr else strikesRange.endDate
                                strikesRange = strikesRange.copy(startDate = pickedStr, endDate = nextEnd, isCustom = true)
                            }
                            PickerTarget.STRIKES_END -> {
                                strikesRange = strikesRange.copy(
                                    endDate = pickedStr.coerceInRange(strikesRange.startDate, todayStr),
                                    isCustom = true
                                )
                            }
                            PickerTarget.GIRL_START -> {
                                val nextEnd = if (girlRange.endDate < pickedStr) pickedStr else girlRange.endDate
                                girlRange = girlRange.copy(startDate = pickedStr, endDate = nextEnd, isCustom = true)
                            }
                            PickerTarget.GIRL_END -> {
                                girlRange = girlRange.copy(
                                    endDate = pickedStr.coerceInRange(girlRange.startDate, todayStr),
                                    isCustom = true
                                )
                            }
                            PickerTarget.DEFLAME_START -> {
                                val nextEnd = if (deflameRange.endDate < pickedStr) pickedStr else deflameRange.endDate
                                deflameRange = deflameRange.copy(startDate = pickedStr, endDate = nextEnd, isCustom = true)
                            }
                            PickerTarget.DEFLAME_END -> {
                                deflameRange = deflameRange.copy(
                                    endDate = pickedStr.coerceInRange(deflameRange.startDate, todayStr),
                                    isCustom = true
                                )
                            }
                        }
                        activePickerTarget = null
                    },
                    modifier = Modifier.testTag("calendar_confirm_btn")
                ) {
                    Text(
                        text = when {
                            isAr -> "تأكيد"
                            isFr -> "Confirmer"
                            else -> "Confirm"
                        },
                        color = accentCyan,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { activePickerTarget = null }) {
                    Text(
                        text = when {
                            isAr -> "إلغاء"
                            isFr -> "Annuler"
                            else -> "Cancel"
                        },
                        color = mutedTextColor
                    )
                }
            },
            colors = DatePickerDefaults.colors(
                containerColor = if (isDarkTheme) Color(0xFF0E172A) else Color(0xFFFFFFFF)
            )
        ) {
            DatePicker(
                state = datePickerState,
                showModeToggle = false,
                colors = DatePickerDefaults.colors(
                    containerColor = if (isDarkTheme) Color(0xFF0E172A) else Color(0xFFFFFFFF),
                    titleContentColor = accentCyan,
                    headlineContentColor = mainTextColor,
                    weekdayContentColor = mutedTextColor,
                    subheadContentColor = secondaryTextColor,
                    yearContentColor = mainTextColor,
                    currentYearContentColor = accentCyan,
                    selectedYearContentColor = Color.White,
                    selectedYearContainerColor = accentCyan,
                    dayContentColor = mainTextColor,
                    disabledDayContentColor = if (isDarkTheme) Color(0x33FFFFFF) else Color(0x4064748B),
                    selectedDayContentColor = if (isDarkTheme) Color.Black else Color.White,
                    selectedDayContainerColor = accentCyan,
                    todayContentColor = accentAmber,
                    todayDateBorderColor = accentAmber
                )
            )
        }
    }
}

@Composable
private fun CalendarDateButton(
    label: String,
    dateStr: String,
    accentColor: Color,
    isDarkTheme: Boolean,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isDarkTheme) Color(0x330F172A) else Color(0xFFF1F5F9))
            .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(accentColor.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = label,
                tint = accentColor,
                modifier = Modifier.size(16.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 10.sp,
                color = accentColor.copy(alpha = 0.9f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = dateStr,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Black,
                fontFamily = PlusJakartaSans,
                color = if (isDarkTheme) Color.White else Color(0xFF0F172A),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun PerElementDateCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    isDarkTheme: Boolean,
    range: DateRangeConfig,
    globalStartDate: String,
    globalEndDate: String,
    isAr: Boolean,
    isFr: Boolean,
    toggleTestTag: String,
    startBtnTestTag: String,
    endBtnTestTag: String,
    onToggleCustom: (Boolean) -> Unit,
    onPickStart: () -> Unit,
    onPickEnd: () -> Unit,
    extraContent: (@Composable () -> Unit)? = null
) {
    val activeStart = if (range.isCustom) range.startDate else globalStartDate
    val activeEnd = if (range.isCustom) range.endDate else globalEndDate
    val cardBg = if (isDarkTheme) Color(0x22162238) else Color(0xFFFFFFFF)
    val defaultBorder = if (isDarkTheme) Color(0x29FFFFFF) else Color(0xFFE2E8F0)
    val titleColor = if (isDarkTheme) Color.White else Color(0xFF0F172A)
    val mutedColor = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(15.dp))
            .background(cardBg)
            .border(
                width = if (range.isCustom) 1.2.dp else 1.dp,
                color = if (range.isCustom) accentColor.copy(alpha = 0.7f) else defaultBorder,
                shape = RoundedCornerShape(15.dp)
            )
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(accentColor.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = titleColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = subtitle,
                            fontSize = 10.5.sp,
                            color = accentColor,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = when {
                            isAr -> "تخصيص"
                            isFr -> "Perso"
                            else -> "Custom"
                        },
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (range.isCustom) accentColor else mutedColor
                    )
                    Switch(
                        checked = range.isCustom,
                        onCheckedChange = onToggleCustom,
                        modifier = Modifier.testTag(toggleTestTag),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = if (isDarkTheme) Color.Black else Color.White,
                            checkedTrackColor = accentColor,
                            uncheckedThumbColor = mutedColor,
                            uncheckedTrackColor = if (isDarkTheme) Color(0xFF1E293B) else Color(0xFFE2E8F0),
                            uncheckedBorderColor = if (isDarkTheme) Color(0x44FFFFFF) else Color(0xFFCBD5E1)
                        )
                    )
                }
            }

            if (extraContent != null) {
                extraContent()
            }

            if (!range.isCustom) {
                Text(
                    text = when {
                        isAr -> "يتبع الفترة الرئيسية: $activeStart ← $activeEnd"
                        isFr -> "Suit la période principale : $activeStart → $activeEnd"
                        else -> "Using main range: $activeStart → $activeEnd"
                    },
                    fontSize = 10.5.sp,
                    color = mutedColor
                )
            }

            AnimatedVisibility(visible = range.isCustom) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CalendarDateButton(
                        label = when {
                            isAr -> "من (التقويم)"
                            isFr -> "Début"
                            else -> "From"
                        },
                        dateStr = range.startDate,
                        accentColor = accentColor,
                        isDarkTheme = isDarkTheme,
                        testTag = startBtnTestTag,
                        onClick = onPickStart,
                        modifier = Modifier.weight(1f)
                    )
                    CalendarDateButton(
                        label = when {
                            isAr -> "إلى (التقويم)"
                            isFr -> "Fin"
                            else -> "To"
                        },
                        dateStr = range.endDate,
                        accentColor = accentColor,
                        isDarkTheme = isDarkTheme,
                        testTag = endBtnTestTag,
                        onClick = onPickEnd,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun PreviewLineRow(
    icon: ImageVector,
    tint: Color,
    label: String,
    value: String,
    isDarkTheme: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                color = if (isDarkTheme) Color.White.copy(alpha = 0.88f) else Color(0xFF334155),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            color = tint
        )
    }
}

private fun localTodayString(): String {
    return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
}

private fun daysBetweenStrings(fromStr: String, toStr: String): Int {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
        val from = sdf.parse(fromStr) ?: return 0
        val to = sdf.parse(toStr) ?: return 0
        ((to.time - from.time) / (24L * 3600L * 1000L)).toInt().coerceAtLeast(0)
    } catch (_: Exception) {
        0
    }
}

private fun dateStringToUtcMidnightMillis(dateStr: String): Long? {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            isLenient = false
            timeZone = TimeZone.getTimeZone("UTC")
        }
        sdf.parse(dateStr.trim())?.time
    } catch (_: Exception) {
        null
    }
}

private fun utcMidnightMillisToDateString(utcMillis: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    return sdf.format(Date(utcMillis))
}

private fun todayUtcMidnightMillis(): Long {
    return dateStringToUtcMidnightMillis(localTodayString()) ?: System.currentTimeMillis()
}

private fun String.coerceMax(maxDateStr: String): String {
    return if (this.isBlank() || this > maxDateStr) maxDateStr else this
}

private fun String.coerceInRange(minDateStr: String, maxDateStr: String): String {
    if (this.isBlank()) return maxDateStr
    return when {
        this < minDateStr -> minDateStr
        this > maxDateStr -> maxDateStr
        else -> this
    }
}
