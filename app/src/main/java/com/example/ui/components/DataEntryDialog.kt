package com.example.ui.components

import android.Manifest
import android.app.Activity
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Speed
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.Individual
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonRose
import com.example.ui.theme.NeonViolet
import com.example.ui.viewmodel.IndividualViewModel
import com.example.util.ContactsPickerHelper
import com.example.util.ImportedContact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

/**
 * Full-screen Cyberpunk Glassmorphism Data Entry Modal Sheet/Dialog.
 * Triggered directly from the "+" FAB or Edit button inside the Records Screen.
 */
@Composable
fun DataEntryDialog(
    viewModel: IndividualViewModel,
    onDismiss: () -> Unit,
    isDarkTheme: Boolean = isSystemInDarkTheme()
) {
    val name by viewModel.formName.collectAsStateWithLifecycle()
    val phoneNumber by viewModel.formPhoneNumber.collectAsStateWithLifecycle()
    val type by viewModel.formType.collectAsStateWithLifecycle()
    val length by viewModel.formLength.collectAsStateWithLifecycle()
    val width by viewModel.formWidth.collectAsStateWithLifecycle()
    val sScore by viewModel.formSScore.collectAsStateWithLifecycle()
    val fScore by viewModel.formFScore.collectAsStateWithLifecycle()
    val cScore by viewModel.formCScore.collectAsStateWithLifecycle()
    val error by viewModel.formError.collectAsStateWithLifecycle()
    val historicalEntries by viewModel.formHistoricalEntries.collectAsStateWithLifecycle()

    val calculatedSize by viewModel.liveCalculatedSize.collectAsStateWithLifecycle()
    val calculatedBoost by viewModel.liveCalculatedBoost.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSavingForm.collectAsStateWithLifecycle()

    val isEditing = viewModel.editingId != null
    val context = LocalContext.current
    val currentLang by viewModel.currentLanguage.collectAsStateWithLifecycle()
    val isAr = currentLang == "ar"

    val coroutineScope = rememberCoroutineScope()
    var showInAppContactsPicker by remember { mutableStateOf(false) }
    var inAppContactsList by remember { mutableStateOf<List<ImportedContact>>(emptyList()) }
    var showContactsPermissionDialog by remember { mutableStateOf(false) }

    fun applyImportedContact(imported: ImportedContact) {
        if (imported.name.isNotBlank()) {
            viewModel.formName.value = imported.name
        }
        if (imported.phone.isNotBlank()) {
            viewModel.formPhoneNumber.value = imported.phone
        }
        viewModel.formError.value = null
        viewModel.notifyContactImported(imported.name, imported.phone)
    }

    val contactActivityLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val contactUri: Uri? = result.data?.data
            if (contactUri != null) {
                coroutineScope.launch {
                    val imported = try {
                        withContext(Dispatchers.IO) {
                            ContactsPickerHelper.extractContactFromUri(context, contactUri)
                        }
                    } catch (_: Throwable) {
                        null
                    }
                    if (imported != null) {
                        applyImportedContact(imported)
                    } else if (!ContactsPickerHelper.hasReadContactsPermission(context)) {
                        showContactsPermissionDialog = true
                    } else {
                        viewModel.formError.value = when (currentLang) {
                            "ar" -> "تعذر قراءة بيانات جهة الاتصال المختارة"
                            "fr" -> "Impossible de lire le contact sélectionné"
                            else -> "Could not read selected contact details"
                        }
                    }
                }
            }
        }
    }

    fun launchContactsFlow(hasPermission: Boolean) {
        val launched = ContactsPickerHelper.tryLaunchExternalPicker(
            context = context,
            launcher = contactActivityLauncher,
            hasPermission = hasPermission
        )
        if (!launched) {
            if (hasPermission) {
                coroutineScope.launch {
                    val loaded = try {
                        withContext(Dispatchers.IO) {
                            ContactsPickerHelper.loadDeviceContacts(context)
                        }
                    } catch (_: Throwable) {
                        emptyList()
                    }
                    inAppContactsList = loaded
                    showInAppContactsPicker = true
                }
            } else {
                showContactsPermissionDialog = true
            }
        }
    }

    val readContactsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        launchContactsFlow(hasPermission = isGranted)
    }

    fun onOpenContacts() {
        val hasPerm = try {
            ContactsPickerHelper.hasReadContactsPermission(context)
        } catch (_: Throwable) {
            false
        }
        if (hasPerm) {
            launchContactsFlow(hasPermission = true)
        } else {
            try {
                readContactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
            } catch (_: Throwable) {
                launchContactsFlow(hasPermission = false)
            }
        }
    }

    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    var inputYear by remember { mutableStateOf((currentYear - 1).toString()) }
    var inputStrikeCount by remember { mutableStateOf("15") }
    val totalHistoricalStrikes = historicalEntries.sumOf { it.second }

    val modalBg = if (isDarkTheme) Color(0xFF070B14) else Color(0xFFF1F5F9)
    val scrollState = rememberScrollState()

    BackHandler(enabled = !showInAppContactsPicker && !showContactsPermissionDialog) {
        onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(modalBg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
            .testTag("data_entry_overlay")
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = modalBg
        ) {
            LiquidGlassBackground(darkTheme = isDarkTheme) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    // Modal Header with Close button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isEditing) stringResource(R.string.update_record) else stringResource(R.string.nav_entry),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = stringResource(R.string.auto_calculated),
                                style = MaterialTheme.typography.bodySmall,
                                color = NeonCyan,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isEditing) {
                                Button(
                                    onClick = {
                                        viewModel.saveCurrentForm {
                                            onDismiss()
                                        }
                                    },
                                    enabled = !isSaving,
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = NeonCyan,
                                        contentColor = Color(0xFF021B26)
                                    ),
                                    modifier = Modifier
                                        .height(40.dp)
                                        .testTag("save_record_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = stringResource(R.string.update_record),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(NeonCyan.copy(alpha = 0.2f))
                                        .border(1.dp, NeonCyan, RoundedCornerShape(12.dp))
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = if (isAr) "إدخال جديد" else "NEW ENTRY",
                                        color = NeonCyan,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x221E293B))
                                    .border(1.dp, Color(0x4038BDF8), CircleShape)
                                    .testTag("close_data_entry_dialog_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.cancel),
                                    tint = NeonCyan
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(scrollState)
                            .imePadding()
                            .padding(16.dp)
                            .testTag("data_entry_scroll_container"),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Error message banner
                        if (error != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(NeonRose.copy(alpha = 0.15f))
                                    .border(1.dp, NeonRose, RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.ErrorOutline,
                                        contentDescription = null,
                                        tint = NeonRose,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = error ?: "",
                                        color = NeonRose,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        // Section 1: Individual Profile & Type
                        LiquidGlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = stringResource(R.string.full_name),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )

                                        OutlinedButton(
                                            onClick = { onOpenContacts() },
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                            border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                                            modifier = Modifier
                                                .height(34.dp)
                                                .testTag("btn_import_from_contacts")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Contacts,
                                                contentDescription = null,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = stringResource(R.string.import_from_contacts),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))

                                    OutlinedTextField(
                                        value = name,
                                        onValueChange = { viewModel.formName.value = it },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("input_fullname"),
                                        placeholder = { Text(stringResource(R.string.full_name_hint)) },
                                        trailingIcon = {
                                            IconButton(
                                                onClick = { onOpenContacts() },
                                                modifier = Modifier.testTag("trailing_pick_contact_btn")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Contacts,
                                                    contentDescription = stringResource(R.string.import_from_contacts),
                                                    tint = NeonCyan
                                                )
                                            }
                                        },
                                        singleLine = true,
                                        shape = RoundedCornerShape(14.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = NeonCyan,
                                            unfocusedBorderColor = Color(0x3338BDF8)
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(14.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = stringResource(R.string.phone_number),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = stringResource(R.string.phone_number_desc),
                                            fontSize = 11.sp,
                                            color = NeonCyan
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))

                                    OutlinedTextField(
                                        value = phoneNumber,
                                        onValueChange = { viewModel.formPhoneNumber.value = it },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("input_phone_number"),
                                        placeholder = { Text(stringResource(R.string.phone_number_hint)) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Phone,
                                                contentDescription = null,
                                                tint = NeonCyan,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        trailingIcon = {
                                            IconButton(
                                                onClick = { onOpenContacts() },
                                                modifier = Modifier.testTag("pick_phone_contact_btn")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Contacts,
                                                    contentDescription = stringResource(R.string.import_from_contacts),
                                                    tint = NeonCyan
                                                )
                                            }
                                        },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = NeonCyan,
                                            unfocusedBorderColor = Color(0x3338BDF8)
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(14.dp))

                                    Text(
                                        text = stringResource(R.string.type),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Individual.VALID_TYPES.forEach { t ->
                                            val isSelected = type == t
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(
                                                        if (isSelected) NeonCyan.copy(alpha = 0.25f) else Color(0x15FFFFFF)
                                                    )
                                                    .border(
                                                        1.5.dp,
                                                        if (isSelected) NeonCyan else Color(0x25FFFFFF),
                                                        RoundedCornerShape(12.dp)
                                                    )
                                                    .clickable { viewModel.formType.value = t }
                                                    .padding(vertical = 12.dp)
                                                    .testTag("type_chip_$t"),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = t,
                                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                                    color = if (isSelected) NeonCyan else MaterialTheme.colorScheme.onSurface,
                                                    fontSize = 14.sp
                                                )
                                            }
                                        }
                                    }
                                }
                        }

                        // Section 2: Physical Dimensions & Live SIZE Calculation
                        LiquidGlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (isAr) "الأبعاد الفيزيائية" else "Physical Dimensions",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = stringResource(R.string.formula_size),
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = length,
                                            onValueChange = { viewModel.formLength.value = it },
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("input_length"),
                                            label = { Text(stringResource(R.string.length)) },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            singleLine = true,
                                            shape = RoundedCornerShape(12.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = NeonCyan,
                                                unfocusedBorderColor = Color(0x3338BDF8)
                                            )
                                        )

                                        OutlinedTextField(
                                            value = width,
                                            onValueChange = { viewModel.formWidth.value = it },
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("input_width"),
                                            label = { Text(stringResource(R.string.width)) },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            singleLine = true,
                                            shape = RoundedCornerShape(12.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = NeonCyan,
                                                unfocusedBorderColor = Color(0x3338BDF8)
                                            )
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0x2038BDF8))
                                            .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Calculate,
                                                contentDescription = null,
                                                tint = NeonCyan,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = if (isAr) "الحجم المحسوب (الطول × العرض)" else "CALCULATED SIZE (L × W)",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        Text(
                                            text = "$calculatedSize",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Black,
                                            color = NeonCyan,
                                            modifier = Modifier.testTag("preview_live_size")
                                        )
                                    }
                                }
                        }

                        // Section 3: Scores & Live BOOST Calculation
                        LiquidGlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = stringResource(R.string.scores_breakdown),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = stringResource(R.string.formula_boost),
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = sScore,
                                            onValueChange = { viewModel.formSScore.value = it },
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("input_sscore"),
                                            label = { Text("S") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            singleLine = true,
                                            shape = RoundedCornerShape(12.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = NeonCyan,
                                                unfocusedBorderColor = Color(0x3338BDF8)
                                            )
                                        )

                                        OutlinedTextField(
                                            value = fScore,
                                            onValueChange = { viewModel.formFScore.value = it },
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("input_fscore"),
                                            label = { Text("F") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            singleLine = true,
                                            shape = RoundedCornerShape(12.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = NeonViolet,
                                                unfocusedBorderColor = Color(0x33A855F7)
                                            )
                                        )

                                        OutlinedTextField(
                                            value = cScore,
                                            onValueChange = { viewModel.formCScore.value = it },
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("input_cscore"),
                                            label = { Text("C") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            singleLine = true,
                                            shape = RoundedCornerShape(12.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = NeonAmber,
                                                unfocusedBorderColor = Color(0x33F59E0B)
                                            )
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(NeonEmerald.copy(alpha = 0.2f), NeonCyan.copy(alpha = 0.15f))
                                                )
                                            )
                                            .border(1.dp, NeonEmerald.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                                            .padding(14.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Speed,
                                                    contentDescription = null,
                                                    tint = NeonEmerald,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = if (isAr) "التعزيز الديناميكي" else "DYNAMIC BOOST",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }

                                            Text(
                                                text = "$calculatedBoost",
                                                fontSize = 24.sp,
                                                fontWeight = FontWeight.Black,
                                                color = NeonEmerald,
                                                modifier = Modifier.testTag("preview_live_boost")
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        val s = IndividualViewModel.parseLocalizedDouble(sScore) ?: 0.0
                                        val f = IndividualViewModel.parseLocalizedDouble(fScore) ?: 0.0
                                        val c = IndividualViewModel.parseLocalizedDouble(cScore) ?: 0.0
                                        val sumScores = ((s + f + c) * 100.0).toLong() / 100.0
                                        val sizeBonus = ((calculatedSize / 100.0) * 100.0).toLong() / 100.0

                                        Text(
                                            text = if (isAr) {
                                                "($sumScores المجموع) + ($sizeBonus الحجم/100) = $calculatedBoost"
                                            } else {
                                                "($sumScores sum) + ($sizeBonus size/100) = $calculatedBoost"
                                            },
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                        }

                        // Section 4: Historical Strikes Entry per Year
                        LiquidGlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp)
                            ) {
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
                                                tint = NeonAmber,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = stringResource(R.string.historical_strikes_title),
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        if (totalHistoricalStrikes > 0) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(NeonAmber.copy(alpha = 0.2f))
                                                    .border(1.dp, NeonAmber, RoundedCornerShape(8.dp))
                                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = if (isAr) "الإجمالي: $totalHistoricalStrikes" else "Total: $totalHistoricalStrikes",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = NeonAmber
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = stringResource(R.string.historical_strikes_desc),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Spacer(modifier = Modifier.height(14.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = inputYear,
                                            onValueChange = { inputYear = it },
                                            label = { Text(stringResource(R.string.year)) },
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("input_historical_year"),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true,
                                            shape = RoundedCornerShape(12.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = NeonCyan,
                                                unfocusedBorderColor = Color(0x3338BDF8)
                                            )
                                        )

                                        OutlinedTextField(
                                            value = inputStrikeCount,
                                            onValueChange = { inputStrikeCount = it },
                                            label = { Text(stringResource(R.string.strikes_amount)) },
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("input_historical_count"),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true,
                                            shape = RoundedCornerShape(12.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = NeonEmerald,
                                                unfocusedBorderColor = Color(0x3310B981)
                                            )
                                        )

                                        Button(
                                            onClick = {
                                                val y = IndividualViewModel.parseLocalizedInt(inputYear)
                                                val c = IndividualViewModel.parseLocalizedInt(inputStrikeCount)
                                                if (y != null && y in 2000..2050 && c != null && c in 1..100_000) {
                                                    viewModel.addHistoricalYearEntry(y, c)
                                                    inputYear = (y - 1).toString()
                                                    inputStrikeCount = "10"
                                                } else {
                                                    viewModel.formError.value = when (currentLang) {
                                                        "ar" -> "يرجى إدخال سنة صحيحة (2000-2050) وعدد ضربات صالح"
                                                        "fr" -> "Veuillez saisir une année valide (2000-2050) et un nombre positif"
                                                        else -> "Please enter a valid year (2000-2050) and strike count"
                                                    }
                                                }
                                            },
                                            modifier = Modifier
                                                .height(56.dp)
                                                .testTag("add_historical_entry_btn"),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = NeonCyan,
                                                contentColor = Color(0xFF021B26)
                                            )
                                        ) {
                                            Icon(imageVector = Icons.Default.Add, contentDescription = null)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(stringResource(R.string.add_year_entry), fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf(currentYear, currentYear - 1, currentYear - 2, currentYear - 3).forEach { yr ->
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Color(0x18FFFFFF))
                                                    .border(1.dp, Color(0x28FFFFFF), RoundedCornerShape(8.dp))
                                                    .clickable { inputYear = yr.toString() }
                                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                                            ) {
                                                Text(
                                                    text = "$yr",
                                                    fontSize = 11.sp,
                                                    color = NeonCyan,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }

                                    if (historicalEntries.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(14.dp))
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            historicalEntries.forEachIndexed { index, (yr, cnt) ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .background(Color(0x250F172A))
                                                        .border(1.dp, NeonEmerald.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(
                                                            text = if (isAr) "سنة $yr : " else "Year $yr : ",
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 13.sp,
                                                            color = NeonCyan
                                                        )
                                                        Text(
                                                            text = if (isAr) "$cnt ضربة" else "$cnt strikes",
                                                            fontWeight = FontWeight.ExtraBold,
                                                            fontSize = 13.sp,
                                                            color = NeonEmerald
                                                        )
                                                    }

                                                    IconButton(
                                                        onClick = { viewModel.removeHistoricalYearEntry(index) },
                                                        modifier = Modifier.size(28.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Close,
                                                            contentDescription = if (isAr) "حذف" else "Remove",
                                                            tint = NeonRose,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                        }

                        // Action Buttons: Save & Cancel
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = onDismiss,
                                enabled = !isSaving,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .testTag("cancel_btn"),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.cancel))
                            }

                            if (!isEditing) {
                                Button(
                                    onClick = {
                                        viewModel.saveCurrentForm {
                                            onDismiss()
                                        }
                                    },
                                    enabled = !isSaving,
                                    modifier = Modifier
                                        .weight(2f)
                                        .height(52.dp)
                                        .testTag("save_record_btn"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = NeonCyan,
                                        contentColor = Color(0xFF021B26)
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.save_record),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }

        if (showInAppContactsPicker) {
            InAppContactsPickerDialog(
                contacts = inAppContactsList,
                currentLang = currentLang,
                onSelectContact = { imported ->
                    applyImportedContact(imported)
                    showInAppContactsPicker = false
                },
                onDismiss = { showInAppContactsPicker = false },
                isDarkTheme = isDarkTheme,
                useDialogWindow = false
            )
        }

        if (showContactsPermissionDialog) {
            ContactsPermissionRequiredDialog(
                currentLang = currentLang,
                onOpenSettings = {
                    showContactsPermissionDialog = false
                    ContactsPickerHelper.openAppSettings(context)
                },
                onDismiss = { showContactsPermissionDialog = false },
                isDarkTheme = isDarkTheme,
                useDialogWindow = false
            )
        }
    }
}
