package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.Individual
import com.example.data.model.StrikeEvent
import com.example.data.repository.IndividualRepository
import com.example.ui.components.CosmicStrikeButton
import com.example.ui.components.GlassTypeBadge
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.StrikerStatus
import com.example.ui.components.StrikerStatusBadge
import com.example.ui.theme.DayCyan
import com.example.ui.theme.DayEmerald
import com.example.ui.theme.DayOrange
import com.example.ui.theme.DayPurple
import com.example.ui.theme.DayRose
import com.example.ui.theme.LocalAppDarkTheme
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonRose
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.PlusJakartaSans
import com.example.ui.viewmodel.IndividualViewModel
import com.example.util.BiometricAvailability
import com.example.util.FingerprintAuthHelper
import com.example.util.HapticHelper
import com.example.util.rememberFragmentActivity
import java.text.NumberFormat
import java.util.Locale

@Composable
fun StrikeScreen(
    viewModel: IndividualViewModel,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = LocalAppDarkTheme.current,
    onAuthenticateDelete: ((
        title: String,
        subtitle: String,
        cancelText: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onCancel: () -> Unit
    ) -> Unit)? = null
) {
    val context = LocalContext.current
    val isHapticFeedbackEnabled by viewModel.isHapticFeedbackEnabled
    val allList by viewModel.allIndividuals.collectAsStateWithLifecycle()
    val allStrikeEvents by viewModel.allStrikeEvents.collectAsStateWithLifecycle()
    val allTimeStrikesCount by viewModel.allTimeStrikesCount.collectAsStateWithLifecycle()
    val lastStrikeMap by viewModel.individualLastStrikeMap.collectAsStateWithLifecycle()
    val currentLang by viewModel.currentLanguage.collectAsStateWithLifecycle()
    val isAr = currentLang == "ar"
    val nf = remember { NumberFormat.getNumberInstance(Locale.US) }
    val accentCyan = if (isDarkTheme) NeonCyan else DayCyan
    val accentRose = if (isDarkTheme) NeonRose else DayRose

    val totalAllTimeStrikes = maxOf(allTimeStrikesCount, allList.sumOf { it.strikeCount })

    var showStrikeTargetPicker by remember { mutableStateOf(false) }
    var pendingDeleteEvent by remember { mutableStateOf<StrikeEvent?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ==========================================
            // HERO STRIKE BUTTON (Displays Total All-Time Strikes)
            // ==========================================
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CosmicStrikeButton(
                        onClick = {
                            HapticHelper.triggerActionHaptic(context, isHapticFeedbackEnabled)
                            showStrikeTargetPicker = true
                        },
                        strikeCount = totalAllTimeStrikes,
                        subtitle = stringResource(R.string.strike_action),
                        darkTheme = isDarkTheme
                    )
                }
            }

            // ==========================================
            // RECENT STRIKES FEED (Biometric Delete Confirmation)
            // ==========================================
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = accentCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.recent_activity),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = null,
                                tint = accentRose.copy(alpha = 0.9f),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.swipe_to_delete_hint),
                                fontSize = 11.sp,
                                color = accentRose.copy(alpha = 0.9f),
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "(${nf.format(allStrikeEvents.size)} / ${nf.format(IndividualRepository.MAX_LITERAL_RECENT_STRIKES)})",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                }
            }

            if (allStrikeEvents.isEmpty()) {
                item {
                    LiquidGlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        darkTheme = isDarkTheme
                    ) {
                        Text(
                            text = when (currentLang) {
                                "ar" -> "اضغط على زر الضربة واختر الفرد لتسجيل أول ضربة تلقائياً بالتاريخ والوقت."
                                "fr" -> "Appuyez sur Strike et choisissez un individu pour enregistrer votre premier strike."
                                else -> "Tap the Strike button and select an individual to record your first strike."
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                items(
                    items = allStrikeEvents,
                    key = { "recent_${it.id}" },
                    contentType = { "recent_strike_log" }
                ) { event ->
                    SwipeableStrikeLogItem(
                        event = event,
                        onRequestDelete = { targetEvent ->
                            pendingDeleteEvent = targetEvent
                        },
                        darkTheme = isDarkTheme
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(64.dp))
            }
        }

        // Overlay 1: Select Individual & Confirm to Register Strike
        if (showStrikeTargetPicker) {
            StrikeTargetPickerDialog(
                individuals = allList,
                getStatus = { person -> viewModel.getStatusForIndividual(person, lastStrikeMap) },
                currentLang = currentLang,
                isDarkTheme = isDarkTheme,
                onDismiss = { showStrikeTargetPicker = false },
                onConfirmStrike = { chosen ->
                    HapticHelper.triggerStrikeHaptic(context, isHapticFeedbackEnabled)
                    viewModel.selectStrikeIndividual(chosen.id)
                    viewModel.incrementStrikeForId(chosen.id, 1)
                    showStrikeTargetPicker = false
                }
            )
        }

        // Overlay 2: Biometric (Fingerprint) Confirmation to Delete a Recent Strike
        pendingDeleteEvent?.let { targetEvent ->
            BiometricDeleteStrikeDialog(
                event = targetEvent,
                currentLang = currentLang,
                isDarkTheme = isDarkTheme,
                onDismiss = { pendingDeleteEvent = null },
                onConfirmDelete = { confirmedEvent ->
                    viewModel.deleteStrikeEvent(confirmedEvent)
                    pendingDeleteEvent = null
                },
                onAuthenticateDelete = onAuthenticateDelete
            )
        }
    }
}

@Composable
private fun StrikeTargetPickerDialog(
    individuals: List<Individual>,
    getStatus: (Individual) -> StrikerStatus,
    currentLang: String,
    isDarkTheme: Boolean,
    onDismiss: () -> Unit,
    onConfirmStrike: (Individual) -> Unit
) {
    val isAr = currentLang == "ar"
    val isFr = currentLang == "fr"
    val accentCyan = if (isDarkTheme) NeonCyan else DayCyan
    val accentViolet = if (isDarkTheme) NeonViolet else DayPurple
    val accentAmber = if (isDarkTheme) NeonAmber else DayOrange
    val panelBg = if (isDarkTheme) Color(0xF80B1324) else Color(0xFFF8FAFC)

    var selectedId by remember { mutableStateOf<Long?>(null) }
    val selectedCandidate = remember(selectedId, individuals) {
        individuals.firstOrNull { it.id == selectedId }
    }

    BackHandler(onBack = onDismiss)
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = if (isDarkTheme) 0.62f else 0.42f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 460.dp)
                .pointerInput(Unit) { detectTapGestures { } }
                .shadow(22.dp, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .background(panelBg)
                .border(
                    1.25.dp,
                    Brush.linearGradient(listOf(accentCyan, accentViolet)),
                    RoundedCornerShape(24.dp)
                )
                .padding(20.dp)
                .testTag("strike_target_picker_dialog")
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(accentCyan.copy(alpha = 0.18f))
                            .border(1.dp, accentCyan.copy(alpha = 0.6f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = accentCyan,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when {
                                isAr -> "اختر الفرد لتسجيل سترايك"
                                isFr -> "Sélectionner l'individu"
                                else -> "Select Individual for Strike"
                            },
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = when {
                                isAr -> "اختر الفرد ثم اضغط تأكيد لتسجيل الضربة"
                                isFr -> "Choisissez une personne puis confirmez"
                                else -> "Choose a person then confirm to record strike"
                            },
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (individuals.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isDarkTheme) Color(0x181E293B) else Color(0xFFE2E8F0).copy(alpha = 0.5f)
                            )
                            .padding(18.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when {
                                isAr -> "لا يوجد أفراد مسجلون حالياً. أضف فرداً من صفحة السجلات أولاً."
                                isFr -> "Aucun individu enregistré. Ajoutez-en un d'abord."
                                else -> "No individuals registered yet. Add one in Records first."
                            },
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp)
                            .testTag("strike_target_picker_list"),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(individuals, key = { "pick_${it.id}" }) { person ->
                            val isSelected = selectedId == person.id
                            val status = getStatus(person)
                            val rowBg = if (isSelected) {
                                accentCyan.copy(alpha = if (isDarkTheme) 0.22f else 0.14f)
                            } else if (isDarkTheme) {
                                Color(0x1E1E293B)
                            } else {
                                Color.White
                            }
                            val rowBorder = if (isSelected) {
                                accentCyan
                            } else if (isDarkTheme) {
                                Color(0x3038BDF8)
                            } else {
                                Color(0xFFDCE4EE)
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(rowBg)
                                    .border(if (isSelected) 1.5.dp else 1.dp, rowBorder, RoundedCornerShape(14.dp))
                                    .clickable { selectedId = person.id }
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                                    .testTag("strike_picker_item_${person.id}"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                GlassTypeBadge(type = person.type, darkTheme = isDarkTheme)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = person.fullName,
                                            fontFamily = PlusJakartaSans,
                                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                            fontSize = 14.sp,
                                            color = if (isSelected) accentCyan else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        StrikerStatusBadge(status = status, darkTheme = isDarkTheme)
                                    }
                                    Text(
                                        text = when {
                                            isAr -> "${person.strikeCount} ضربة • تعزيز ${person.boost}"
                                            isFr -> "${person.strikeCount} strikes • BOOST ${person.boost}"
                                            else -> "${person.strikeCount} strikes • BOOST ${person.boost}"
                                        },
                                        fontSize = 11.5.sp,
                                        color = if (isSelected) accentAmber else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = if (isSelected) {
                                        Icons.Default.CheckCircle
                                    } else {
                                        Icons.Default.RadioButtonUnchecked
                                    },
                                    contentDescription = null,
                                    tint = if (isSelected) accentCyan else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("btn_cancel_strike_target"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, accentCyan.copy(alpha = 0.45f))
                    ) {
                        Text(
                            text = when {
                                isAr -> "إلغاء"
                                isFr -> "Annuler"
                                else -> "Cancel"
                            },
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Button(
                        onClick = {
                            selectedCandidate?.let { onConfirmStrike(it) }
                        },
                        enabled = selectedCandidate != null,
                        modifier = Modifier
                            .weight(1.3f)
                            .height(48.dp)
                            .testTag("btn_confirm_strike_target"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentCyan,
                            contentColor = if (isDarkTheme) Color(0xFF021B26) else Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when {
                                isAr -> "تأكيد السترايك"
                                isFr -> "Confirmer"
                                else -> "Confirm Strike"
                            },
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BiometricDeleteStrikeDialog(
    event: StrikeEvent,
    currentLang: String,
    isDarkTheme: Boolean,
    onDismiss: () -> Unit,
    onConfirmDelete: (StrikeEvent) -> Unit,
    onAuthenticateDelete: ((
        title: String,
        subtitle: String,
        cancelText: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onCancel: () -> Unit
    ) -> Unit)? = null
) {
    val context = LocalContext.current
    val hostActivity = rememberFragmentActivity()
    val isAr = currentLang == "ar"
    val isFr = currentLang == "fr"
    val accentCyan = if (isDarkTheme) NeonCyan else DayCyan
    val accentRose = if (isDarkTheme) NeonRose else DayRose
    val accentAmber = if (isDarkTheme) NeonAmber else DayOrange
    val panelBg = if (isDarkTheme) Color(0xF80B1324) else Color(0xFFF8FAFC)

    var biometricAvailability by remember {
        mutableStateOf(FingerprintAuthHelper.checkAvailability(hostActivity ?: context))
    }
    var authErrorMessage by remember { mutableStateOf<String?>(null) }

    val promptTitle = when {
        isAr -> "تأكيد حذف السترايك"
        isFr -> "Confirmer la suppression"
        else -> "Confirm Strike Deletion"
    }
    val promptSubtitle = when {
        isAr -> "تحقق بالبصمة لحذف ضربة ${event.individualName} (${event.date})"
        isFr -> "Authentifiez-vous pour supprimer (${event.individualName})"
        else -> "Verify fingerprint to delete ${event.individualName}'s strike (${event.date})"
    }
    val cancelLabel = when {
        isAr -> "إلغاء"
        isFr -> "Annuler"
        else -> "Cancel"
    }
    val notEnrolledMessage = when {
        isAr -> "لم يتم تسجيل بصمة أو قفل شاشة على الجهاز. فعّل البصمة من الإعدادات لإتمام الحذف."
        isFr -> "Aucune empreinte enregistrée sur cet appareil. Activez la biométrie dans les paramètres."
        else -> "No fingerprint or screen lock enrolled. Enable biometrics in device settings to delete."
    }
    val unavailableMessage = when {
        isAr -> "مستشعر البصمة غير متاح على هذا الجهاز."
        isFr -> "Authentification biométrique indisponible sur cet appareil."
        else -> "Biometric authentication is unavailable on this device."
    }

    BackHandler(onBack = onDismiss)
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = if (isDarkTheme) 0.62f else 0.42f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(0.90f)
                .widthIn(max = 430.dp)
                .pointerInput(Unit) { detectTapGestures { } }
                .shadow(22.dp, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .background(panelBg)
                .border(
                    1.25.dp,
                    Brush.linearGradient(listOf(accentRose, accentCyan)),
                    RoundedCornerShape(24.dp)
                )
                .padding(20.dp)
                .testTag("delete_strike_biometric_dialog")
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(accentRose.copy(alpha = 0.16f))
                            .border(1.dp, accentRose.copy(alpha = 0.6f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = null,
                            tint = accentRose,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = when {
                                isAr -> "تأكيد الحذف بالبصمة"
                                isFr -> "Suppression par empreinte"
                                else -> "Fingerprint Delete Confirmation"
                            },
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${event.individualName} • +${event.count} (${event.date})",
                            fontSize = 12.sp,
                            color = accentAmber,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = when {
                        isAr -> "لحماية السجل، يتطلب حذف هذا السترايك التحقق ببصمة الإصبع أو قفل الأمان."
                        isFr -> "Pour protéger l'historique, la suppression requiert une vérification par empreinte."
                        else -> "To protect your strike history, deleting this record requires fingerprint verification."
                    },
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 19.sp
                )

                if (!authErrorMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("biometric_delete_error_msg")
                            .clip(RoundedCornerShape(10.dp))
                            .background(accentRose.copy(alpha = 0.14f))
                            .border(1.dp, accentRose.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = authErrorMessage!!,
                            fontSize = 12.sp,
                            color = accentRose,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                if (onAuthenticateDelete == null && biometricAvailability == BiometricAvailability.NONE_ENROLLED) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = { FingerprintAuthHelper.openSecuritySettings(context) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("biometric_delete_open_settings"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, accentCyan.copy(alpha = 0.55f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = accentCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when {
                                isAr -> "فتح إعدادات البصمة"
                                isFr -> "Paramètres biométriques"
                                else -> "Open Biometric Settings"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("btn_cancel_delete_strike"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, accentCyan.copy(alpha = 0.45f))
                    ) {
                        Text(
                            text = cancelLabel,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Button(
                        onClick = {
                            authErrorMessage = null
                            if (onAuthenticateDelete != null) {
                                onAuthenticateDelete(
                                    promptTitle,
                                    promptSubtitle,
                                    cancelLabel,
                                    { onConfirmDelete(event) },
                                    { err -> authErrorMessage = err.ifBlank { unavailableMessage } },
                                    { /* User dismissed prompt */ }
                                )
                            } else {
                                val checkContext = hostActivity ?: context
                                val latest = FingerprintAuthHelper.checkAvailability(checkContext)
                                biometricAvailability = latest
                                when (latest) {
                                    BiometricAvailability.AVAILABLE -> {
                                        if (hostActivity != null) {
                                            FingerprintAuthHelper.showFingerprintPrompt(
                                                activity = hostActivity,
                                                title = promptTitle,
                                                subtitle = promptSubtitle,
                                                cancelButtonText = cancelLabel,
                                                onSuccess = { onConfirmDelete(event) },
                                                onError = { err ->
                                                    authErrorMessage = err.ifBlank { unavailableMessage }
                                                },
                                                onCancel = {
                                                    // Keep dialog open without deleting
                                                }
                                            )
                                        } else {
                                            authErrorMessage = unavailableMessage
                                        }
                                    }
                                    BiometricAvailability.NONE_ENROLLED -> {
                                        authErrorMessage = notEnrolledMessage
                                    }
                                    BiometricAvailability.UNAVAILABLE -> {
                                        authErrorMessage = unavailableMessage
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1.4f)
                            .height(48.dp)
                            .testTag("btn_verify_fingerprint_delete"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentRose,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when {
                                isAr -> "تحقق واحذف"
                                isFr -> "Vérifier & Supprimer"
                                else -> "Verify & Delete"
                            },
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StrikeLogItem(
    event: StrikeEvent,
    onDeleteClick: (() -> Unit)? = null,
    darkTheme: Boolean = LocalAppDarkTheme.current
) {
    val cardBg = if (darkTheme) Color(0xFF0D1527) else Color(0xFFFFFFFF)
    val cardBorder = if (darkTheme) Color(0x3538BDF8) else Color(0x5500B4D8)
    val boltBg = if (darkTheme) NeonCyan.copy(alpha = 0.2f) else Color(0x2000B4D8)
    val boltColor = if (darkTheme) NeonCyan else DayCyan
    val yearColor = if (darkTheme) NeonAmber else DayOrange
    val countColor = if (darkTheme) NeonEmerald else DayEmerald
    val countBg = if (darkTheme) NeonEmerald.copy(alpha = 0.2f) else Color(0x2010B981)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(cardBg)
            .border(1.dp, cardBorder, RoundedCornerShape(14.dp))
            .padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 6.dp)
            .testTag("recent_strike_item_${event.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(boltBg)
                        .border(1.dp, boltColor.copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = boltColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = event.individualName,
                        fontWeight = FontWeight.Bold,
                        fontFamily = PlusJakartaSans,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${event.date} • ${event.time}",
                            fontSize = 10.sp,
                            fontFamily = PlusJakartaSans,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "[${event.year}]",
                            fontSize = 10.sp,
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.Bold,
                            color = yearColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(countBg)
                        .border(1.dp, countColor.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "+${event.count}",
                        fontSize = 13.sp,
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Black,
                        color = countColor
                    )
                }

                if (onDeleteClick != null) {
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("delete_strike_btn_${event.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.delete),
                            tint = if (darkTheme) NeonRose.copy(alpha = 0.85f) else DayRose,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableStrikeLogItem(
    event: StrikeEvent,
    onRequestDelete: (StrikeEvent) -> Unit,
    modifier: Modifier = Modifier,
    darkTheme: Boolean = LocalAppDarkTheme.current
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.EndToStart || dismissValue == SwipeToDismissBoxValue.StartToEnd) {
                // Keep card settled until biometric fingerprint confirmation succeeds
                onRequestDelete(event)
                false
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp)),
        backgroundContent = {
            val direction = dismissState.dismissDirection
            if (direction == SwipeToDismissBoxValue.StartToEnd || direction == SwipeToDismissBoxValue.EndToStart) {
                val alignment = if (direction == SwipeToDismissBoxValue.StartToEnd) {
                    Alignment.CenterStart
                } else {
                    Alignment.CenterEnd
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFDC2626))
                        .padding(horizontal = 20.dp),
                    contentAlignment = alignment
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = stringResource(R.string.delete),
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = stringResource(R.string.delete),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    ) {
        StrikeLogItem(
            event = event,
            onDeleteClick = { onRequestDelete(event) },
            darkTheme = darkTheme
        )
    }
}
