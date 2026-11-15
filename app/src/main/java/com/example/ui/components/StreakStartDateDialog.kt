package com.example.ui.components

import android.app.DatePickerDialog
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
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
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
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
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
    val initialGirlPerMonth = remember {
        viewModel.getSavedGirlPerMonth().let { if (it > 0) it.toString() else "" }
    }
    val initialDeflamePerMonth = remember {
        viewModel.getSavedDeflamePerMonth().let { if (it > 0) it.toString() else "" }
    }

    var dateInput by remember { mutableStateOf(initialDate) }
    var girlPerMonthInput by remember { mutableStateOf(initialGirlPerMonth) }
    var deflamePerMonthInput by remember { mutableStateOf(initialDeflamePerMonth) }
    var previewResult by remember { mutableStateOf<FlameRecalculationResult?>(null) }
    var isValidDate by remember { mutableStateOf(true) }

    val girlPerMonth = girlPerMonthInput.toIntOrNull()?.coerceAtLeast(0) ?: 0
    val deflamePerMonth = deflamePerMonthInput.toIntOrNull()?.coerceAtLeast(0) ?: 0

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false } }

    fun validateAndPreview(dateStr: String, girlMonthly: Int, deflameMonthly: Int) {
        try {
            val parsed = dateFormat.parse(dateStr.trim())
            val today = Date()
            if (parsed != null && !parsed.after(today)) {
                isValidDate = true
                previewResult = viewModel.previewStartDateRecalculation(
                    startDateStr = dateStr.trim(),
                    girlPerMonth = girlMonthly,
                    deflamePerMonth = deflameMonthly
                )
            } else {
                isValidDate = false
                previewResult = null
            }
        } catch (_: Exception) {
            isValidDate = false
            previewResult = null
        }
    }

    LaunchedEffect(dateInput, girlPerMonth, deflamePerMonth) {
        validateAndPreview(dateInput, girlPerMonth, deflamePerMonth)
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
                dateInput = dateFormat.format(selected.time)
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

    val effectiveLayoutDirection = if (isAr) LayoutDirection.Rtl else LayoutDirection.Ltr

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides effectiveLayoutDirection) {
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
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
                                    text = when (currentLang) {
                                        "ar" -> "تاريخ بداية العد"
                                        "fr" -> "Date de début du suivi"
                                        else -> "Streak Start Date"
                                    },
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    fontFamily = PlusJakartaSans
                                )
                                Text(
                                    text = when (currentLang) {
                                        "ar" -> "إعادة احتساب نقاط الشعلة تلقائياً"
                                        "fr" -> "Recalcul automatique du score de flamme"
                                        else -> "Streak Start Date Recalculation"
                                    },
                                    fontSize = 11.sp,
                                    color = NeonCyan,
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
                                    .background(Color(0x22FFFFFF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = when (currentLang) {
                                        "ar" -> "إغلاق"
                                        "fr" -> "Fermer"
                                        else -> "Close"
                                    },
                                    tint = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Explanatory Subtitle
                    Text(
                        text = when (currentLang) {
                            "ar" -> "أدخل تاريخ بدء السلسلة لحساب الشعلة تلقائياً. تُضاف +3 نقاط لكل يوم مضى، وتُخصم نقاط الضربات بالإضافة لمرات الضغط الشهرية على زر Girl وDeflame في السجلات وتُحتسب نقاط الـ XP الخاصة بها في المسار دون تعديل عداد المهام."
                            "fr" -> "Entrez la date de début pour calculer votre flamme automatiquement (+3 pts/jour, déductions des strikes et appuis mensuels Girl/Deflame enregistrés avec leurs XP dans la progression sans modifier les compteurs de missions)."
                            else -> "Enter your streak start date to calculate your flame automatically (+3 pts/day, strike deductions, and monthly Girl/Deflame presses logged with their XP toward roadmap progress without changing task step counters)."
                        },
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.78f),
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Date Input Field with Calendar Trigger
                    OutlinedTextField(
                        value = dateInput,
                        onValueChange = { dateInput = it },
                        label = {
                            Text(
                                text = when (currentLang) {
                                    "ar" -> "تاريخ البداية (YYYY-MM-DD)"
                                    "fr" -> "Date de début (YYYY-MM-DD)"
                                    else -> "Start Date (YYYY-MM-DD)"
                                },
                                color = NeonCyan.copy(alpha = 0.85f),
                                fontSize = 12.sp
                            )
                        },
                        placeholder = {
                            Text(
                                text = when (currentLang) {
                                    "ar" -> "مثال: 2026-10-15"
                                    "fr" -> "ex. 2026-10-15"
                                    else -> "e.g. 2026-10-15"
                                },
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
                                    contentDescription = when (currentLang) {
                                        "ar" -> "اختيار التاريخ"
                                        "fr" -> "Choisir la date"
                                        else -> "Pick Date"
                                    },
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
                            imeAction = ImeAction.Next
                        ),
                        isError = !isValidDate
                    )

                    if (!isValidDate) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = when (currentLang) {
                                "ar" -> "الرجاء إدخال تاريخ صحيح بصيغة YYYY-MM-DD وليس في المستقبل"
                                "fr" -> "Veuillez entrer une date valide (YYYY-MM-DD) non future"
                                else -> "Please enter a valid date (YYYY-MM-DD) not in the future"
                            },
                            fontSize = 11.sp,
                            color = NeonRose,
                            modifier = Modifier.align(Alignment.Start)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Quick Preset Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PresetDateChip(
                            label = when (currentLang) { "ar" -> "اليوم (0ي)"; "fr" -> "Aujourd'hui (0j)"; else -> "Today (0d)" },
                            onClick = { setQuickPreset(0) }
                        )
                        PresetDateChip(
                            label = when (currentLang) { "ar" -> "منذ أسبوع (7ي)"; "fr" -> "1 Semaine (7j)"; else -> "1 Week (7d)" },
                            onClick = { setQuickPreset(7) }
                        )
                        PresetDateChip(
                            label = when (currentLang) { "ar" -> "منذ 15 يوماً"; "fr" -> "15 Jours"; else -> "15 Days" },
                            onClick = { setQuickPreset(15) }
                        )
                        PresetDateChip(
                            label = when (currentLang) { "ar" -> "منذ شهر (30ي)"; "fr" -> "1 Mois (30j)"; else -> "1 Month (30d)" },
                            onClick = { setQuickPreset(30) }
                        )
                        PresetDateChip(
                            label = when (currentLang) { "ar" -> "منذ شهرين (60ي)"; "fr" -> "2 Mois (60j)"; else -> "2 Months (60d)" },
                            onClick = { setQuickPreset(60) }
                        )
                        PresetDateChip(
                            label = when (currentLang) { "ar" -> "منذ 3 أشهر (90ي)"; "fr" -> "3 Mois (90j)"; else -> "3 Months (90d)" },
                            onClick = { setQuickPreset(90) }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // NEW FIELD 1: Girl Button Monthly Presses
                    val girlAccent = Color(0xFFFB7185)
                    OutlinedTextField(
                        value = girlPerMonthInput,
                        onValueChange = { raw ->
                            val digits = raw.filter { it.isDigit() }.take(4)
                            girlPerMonthInput = digits
                        },
                        label = {
                            Text(
                                text = when (currentLang) {
                                    "ar" -> "عدد مرات الضغط على زر Girl في الشهر"
                                    "fr" -> "Appuis sur le bouton Girl par mois"
                                    else -> "Girl button presses per month"
                                },
                                color = girlAccent.copy(alpha = 0.9f),
                                fontSize = 12.sp
                            )
                        },
                        placeholder = { Text("0", color = Color(0x66FFFFFF)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = girlAccent,
                                modifier = Modifier.size(19.dp)
                            )
                        },
                        supportingText = previewResult?.let { res ->
                            {
                                Text(
                                    text = when (currentLang) {
                                        "ar" -> "الإجمالي من البداية حتى اليوم: ${res.girlPressesCount} مرة (${if (res.girlPressesCount > 0) "-${res.girlPointsDeducted.toInt()} نقطة" else "0 نقطة"})"
                                        "fr" -> "Total sur la période : ${res.girlPressesCount} fois (-${res.girlPointsDeducted.toInt()} pts)"
                                        else -> "Total for period: ${res.girlPressesCount} presses (-${res.girlPointsDeducted.toInt()} pts)"
                                    },
                                    color = girlAccent.copy(alpha = 0.85f),
                                    fontSize = 11.sp
                                )
                            }
                        },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontFamily = PlusJakartaSans
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("girl_per_month_input"),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = girlAccent,
                            unfocusedBorderColor = girlAccent.copy(alpha = 0.35f),
                            focusedContainerColor = Color(0x1A1E293B),
                            unfocusedContainerColor = Color(0x1A1E293B),
                            cursorColor = girlAccent
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // NEW FIELD 2: Deflame Button Monthly Presses
                    val deflameAccent = Color(0xFF38BDF8)
                    OutlinedTextField(
                        value = deflamePerMonthInput,
                        onValueChange = { raw ->
                            val digits = raw.filter { it.isDigit() }.take(4)
                            deflamePerMonthInput = digits
                        },
                        label = {
                            Text(
                                text = when (currentLang) {
                                    "ar" -> "عدد مرات الضغط على زر Deflame في الشهر"
                                    "fr" -> "Appuis sur le bouton Deflame par mois"
                                    else -> "Deflame button presses per month"
                                },
                                color = deflameAccent.copy(alpha = 0.9f),
                                fontSize = 12.sp
                            )
                        },
                        placeholder = { Text("0", color = Color(0x66FFFFFF)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.AcUnit,
                                contentDescription = null,
                                tint = deflameAccent,
                                modifier = Modifier.size(19.dp)
                            )
                        },
                        supportingText = previewResult?.let { res ->
                            {
                                Text(
                                    text = when (currentLang) {
                                        "ar" -> "الإجمالي من البداية حتى اليوم: ${res.deflamePressesCount} مرة (${if (res.deflamePressesCount > 0) "-${res.deflamePointsDeducted.toInt()} نقطة" else "0 نقطة"})"
                                        "fr" -> "Total sur la période : ${res.deflamePressesCount} fois (-${res.deflamePointsDeducted.toInt()} pts)"
                                        else -> "Total for period: ${res.deflamePressesCount} presses (-${res.deflamePointsDeducted.toInt()} pts)"
                                    },
                                    color = deflameAccent.copy(alpha = 0.85f),
                                    fontSize = 11.sp
                                )
                            }
                        },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontFamily = PlusJakartaSans
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("deflame_per_month_input"),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = deflameAccent,
                            unfocusedBorderColor = deflameAccent.copy(alpha = 0.35f),
                            focusedContainerColor = Color(0x1A1E293B),
                            unfocusedContainerColor = Color(0x1A1E293B),
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

                    Spacer(modifier = Modifier.height(14.dp))

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
                                        text = when (currentLang) {
                                            "ar" -> "المعاينة الحسابية التلقائية"
                                            "fr" -> "Aperçu du calcul automatique"
                                            else -> "Automatic Calculation Preview"
                                        },
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
                                            text = when (currentLang) {
                                                "ar" -> "تلقائي ودقيق"
                                                "fr" -> "Auto & Vérifié"
                                                else -> "Auto & Verified"
                                            },
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
                                            text = when (currentLang) {
                                                "ar" -> "الأيام المنقضية (${res.daysElapsed} يوم):"
                                                "fr" -> "Jours écoulés (${res.daysElapsed}j) :"
                                                else -> "Elapsed Days (${res.daysElapsed}d):"
                                            },
                                            fontSize = 12.sp,
                                            color = Color.White.copy(alpha = 0.9f)
                                        )
                                    }
                                    Text(
                                        text = when (currentLang) {
                                            "ar" -> "+${res.inactivityPointsAdded.toInt()} نقطة (+3/يوم)"
                                            "fr" -> "+${res.inactivityPointsAdded.toInt()} PTS (+3/j)"
                                            else -> "+${res.inactivityPointsAdded.toInt()} PTS (+3/day)"
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
                                            text = when (currentLang) {
                                                "ar" -> "الضربات المكتشفة (${res.totalStrikesCount}):"
                                                "fr" -> "Strikes détectés (${res.totalStrikesCount}) :"
                                                else -> "Detected Strikes (${res.totalStrikesCount}):"
                                            },
                                            fontSize = 12.sp,
                                            color = Color.White.copy(alpha = 0.9f)
                                        )
                                    }
                                    Text(
                                        text = when (currentLang) {
                                            "ar" -> "-${String.format(Locale.US, "%.1f", res.totalDeductions)} نقطة"
                                            else -> "-${String.format(Locale.US, "%.1f", res.totalDeductions)} PTS"
                                        },
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NeonRose
                                    )
                                }

                                // Calculated Girl Presses Row
                                if (res.girlPerMonth > 0 || res.girlPressesCount > 0) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Favorite,
                                                contentDescription = null,
                                                tint = girlAccent,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = when (currentLang) {
                                                    "ar" -> "ضغطات Girl (${res.girlPressesCount} مرة):"
                                                    "fr" -> "Appuis Girl (${res.girlPressesCount}×) :"
                                                    else -> "Girl Presses (${res.girlPressesCount}×):"
                                                },
                                                fontSize = 12.sp,
                                                color = Color.White.copy(alpha = 0.9f)
                                            )
                                        }
                                        Text(
                                            text = when (currentLang) {
                                                "ar" -> "-${String.format(Locale.US, "%.1f", res.girlPointsDeducted)} نقطة"
                                                else -> "-${String.format(Locale.US, "%.1f", res.girlPointsDeducted)} PTS"
                                            },
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = girlAccent
                                        )
                                    }
                                }

                                // Calculated Deflame Presses Row
                                if (res.deflamePerMonth > 0 || res.deflamePressesCount > 0) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.AcUnit,
                                                contentDescription = null,
                                                tint = deflameAccent,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = when (currentLang) {
                                                    "ar" -> "ضغطات Deflame (${res.deflamePressesCount} مرة):"
                                                    "fr" -> "Appuis Deflame (${res.deflamePressesCount}×) :"
                                                    else -> "Deflame Presses (${res.deflamePressesCount}×):"
                                                },
                                                fontSize = 12.sp,
                                                color = Color.White.copy(alpha = 0.9f)
                                            )
                                        }
                                        Text(
                                            text = when (currentLang) {
                                                "ar" -> "-${String.format(Locale.US, "%.1f", res.deflamePointsDeducted)} نقطة"
                                                else -> "-${String.format(Locale.US, "%.1f", res.deflamePointsDeducted)} PTS"
                                            },
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = deflameAccent
                                        )
                                    }
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
                                        text = when (currentLang) {
                                            "ar" -> "النتيجة النهائية للشعلة:"
                                            "fr" -> "Score final de la flamme :"
                                            else -> "Resulting Flame Score:"
                                        },
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = when (currentLang) {
                                            "ar" -> "${String.format(Locale.US, "%.1f", res.resultingScore)} نقطة"
                                            else -> "${String.format(Locale.US, "%.1f", res.resultingScore)} PTS"
                                        },
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (res.resultingScore > 2000.0) NeonCyan else NeonAmber,
                                        fontFamily = PlusJakartaSans
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = when (currentLang) {
                                        "ar" -> "ملاحظة: تُسجّل في سجل الشعلة وتُحتسب نقاط الـ XP الخاصة بها (+50 Girl / +20 Deflame) دون إكمال المهام."
                                        "fr" -> "Note : Enregistré dans le journal avec XP (+50 Girl / +20 Deflame) sans valider les missions."
                                        else -> "Note: Logged in Flame history with XP (+50 Girl / +20 Deflame) without mutating task step counters."
                                    },
                                    fontSize = 10.5.sp,
                                    color = Color(0xFF94A3B8)
                                )
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
                                text = when (currentLang) {
                                    "ar" -> "إلغاء"
                                    "fr" -> "Annuler"
                                    else -> "Cancel"
                                },
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 13.sp
                            )
                        }

                        Button(
                            onClick = {
                                if (isValidDate) {
                                    viewModel.recalculateFlameScoreFromStartDate(
                                        startDateStr = dateInput.trim(),
                                        girlPerMonth = girlPerMonth,
                                        deflamePerMonth = deflamePerMonth
                                    )
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
                                text = when (currentLang) {
                                    "ar" -> "تطبيق وإعادة الحساب"
                                    "fr" -> "Appliquer et recalculer"
                                    else -> "Apply & Recalculate"
                                },
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
