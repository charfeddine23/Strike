package com.example.ui.components

import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.Individual
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonRose
import com.example.ui.theme.NeonViolet
import com.example.ui.viewmodel.IndividualViewModel
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

    val isEditing = viewModel.editingId != null
    val context = LocalContext.current

    val pickContactLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { contactUri: Uri? ->
        if (contactUri != null) {
            try {
                var contactName = ""
                var contactPhone = ""

                val cursor = context.contentResolver.query(
                    contactUri,
                    arrayOf(
                        ContactsContract.Contacts._ID,
                        ContactsContract.Contacts.DISPLAY_NAME,
                        ContactsContract.Contacts.HAS_PHONE_NUMBER
                    ),
                    null,
                    null,
                    null
                )
                cursor?.use {
                    if (it.moveToFirst()) {
                        val nameIdx = it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                        if (nameIdx != -1) {
                            contactName = it.getString(nameIdx) ?: ""
                        }
                        val idIdx = it.getColumnIndex(ContactsContract.Contacts._ID)
                        val hasPhoneIdx = it.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)
                        val contactId = if (idIdx != -1) it.getString(idIdx) else null
                        val hasPhone = if (hasPhoneIdx != -1) it.getInt(hasPhoneIdx) else 0

                        if (!contactId.isNullOrBlank() && hasPhone > 0) {
                            val phoneCursor = context.contentResolver.query(
                                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                                "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                                arrayOf(contactId),
                                null
                            )
                            phoneCursor?.use { pCursor ->
                                if (pCursor.moveToFirst()) {
                                    val numIdx = pCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                                    if (numIdx != -1) {
                                        contactPhone = pCursor.getString(numIdx) ?: ""
                                    }
                                }
                            }
                        }
                    }
                }

                if (contactName.isNotBlank()) {
                    viewModel.formName.value = contactName
                }
                if (contactPhone.isNotBlank()) {
                    viewModel.formPhoneNumber.value = contactPhone
                }
                viewModel.notifyContactImported(contactName, contactPhone)
            } catch (_: Exception) {
                // Ignore gracefully
            }
        }
    }

    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    var inputYear by remember { mutableStateOf((currentYear - 1).toString()) }
    var inputStrikeCount by remember { mutableStateOf("15") }
    val totalHistoricalStrikes = historicalEntries.sumOf { it.second }

    val modalBg = if (isDarkTheme) Color(0xFF070B14) else Color(0xFFF1F5F9)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
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
                        Column {
                            Text(
                                text = if (isEditing) stringResource(R.string.update_record) else stringResource(R.string.nav_entry),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.auto_calculated),
                                style = MaterialTheme.typography.bodySmall,
                                color = NeonCyan
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isEditing) NeonViolet.copy(alpha = 0.2f) else NeonCyan.copy(alpha = 0.2f))
                                    .border(1.dp, if (isEditing) NeonViolet else NeonCyan, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = if (isEditing) "EDIT MODE" else "NEW ENTRY",
                                    color = if (isEditing) NeonViolet else NeonCyan,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
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

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Error message banner
                        if (error != null) {
                            item {
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
                        }

                        // Section 1: Individual Profile & Type
                        item {
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
                                            onClick = {
                                                try {
                                                    pickContactLauncher.launch(null)
                                                } catch (_: Exception) {
                                                }
                                            },
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
                                                onClick = {
                                                    try {
                                                        pickContactLauncher.launch(null)
                                                    } catch (_: Exception) {
                                                    }
                                                },
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
                                                onClick = {
                                                    try {
                                                        pickContactLauncher.launch(null)
                                                    } catch (_: Exception) {
                                                    }
                                                },
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
                        }

                        // Section 2: Physical Dimensions & Live SIZE Calculation
                        item {
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
                                            text = "الأبعاد الفيزيائية (Physical Dimensions)",
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
                                                text = "CALCULATED SIZE (L × W)",
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
                        }

                        // Section 3: Scores & Live BOOST Calculation
                        item {
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
                                                    text = "DYNAMIC BOOST",
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

                                        val s = sScore.toDoubleOrNull() ?: 0.0
                                        val f = fScore.toDoubleOrNull() ?: 0.0
                                        val c = cScore.toDoubleOrNull() ?: 0.0
                                        val sumScores = ((s + f + c) * 100.0).toLong() / 100.0
                                        val sizeBonus = ((calculatedSize / 100.0) * 100.0).toLong() / 100.0

                                        Text(
                                            text = "($sumScores sum) + ($sizeBonus size/100) = $calculatedBoost",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        // Section 4: Historical Strikes Entry per Year
                        item {
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
                                                    text = "إجمالي: $totalHistoricalStrikes",
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
                                                val y = inputYear.toIntOrNull()
                                                val c = inputStrikeCount.toIntOrNull()
                                                if (y != null && y in 2000..2050 && c != null && c > 0) {
                                                    viewModel.addHistoricalYearEntry(y, c)
                                                    inputYear = (y - 1).toString()
                                                    inputStrikeCount = "10"
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
                                                            text = "سنة $yr : ",
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 13.sp,
                                                            color = NeonCyan
                                                        )
                                                        Text(
                                                            text = "$cnt ضربة",
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
                                                            contentDescription = "Remove",
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
                        }

                        // Action Buttons: Save & Cancel
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onDismiss,
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

                                Button(
                                    onClick = {
                                        viewModel.saveCurrentForm {
                                            onDismiss()
                                        }
                                    },
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
                                        text = if (isEditing) stringResource(R.string.update_record) else stringResource(R.string.save_record),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
            }
        }
    }
}
