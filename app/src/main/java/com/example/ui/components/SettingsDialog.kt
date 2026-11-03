package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import com.example.BuildConfig
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.R
import com.example.ui.theme.CaveatHandwriting
import com.example.ui.theme.DayCyan
import com.example.ui.theme.DayEmerald
import com.example.ui.theme.DayOrange
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonRose
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.PlusJakartaSans
import com.example.ui.viewmodel.IndividualViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SettingsDialog(
    viewModel: IndividualViewModel,
    onDismiss: () -> Unit,
    onLockAppNow: () -> Unit
) {
    val context = LocalContext.current
    val isPatternLockEnabled by viewModel.isPatternLockEnabled
    var showPatternSetupDialog by remember { mutableStateOf(false) }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }
    var showRestoreConfirmDialog by remember { mutableStateOf(false) }
    val isDarkTheme = isSystemInDarkTheme()

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.exportBackup(context, uri)
        }
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            pendingRestoreUri = uri
            showRestoreConfirmDialog = true
        }
    }

    // Liquid Glass v3 Design Aesthetic Color Tokens
    val dialogBg = if (isDarkTheme) Color(0xF00B132B) else Color(0xFFEEF2F6) // #EEF2F6 soft bluish-gray
    val dialogBorder = if (isDarkTheme) Color(0x6038BDF8) else Color(0xFFCBD5E1) // #CBD5E1 subtle glass border
    val cardBg = if (isDarkTheme) Color(0x221E293B) else Color(0xFFFFFFFF) // #FFFFFF clean white card
    val cardBorder = if (isDarkTheme) Color(0x3538BDF8) else Color(0xFFCBD5E1) // #CBD5E1
    val titleColor = if (isDarkTheme) Color(0xFFF8FAFC) else Color(0xFF1E293B) // #1E293B Deep Slate
    val descColor = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF475569) // #475569 Medium Dark Gray
    val activeCyan = Color(0xFF0EA5E9) // #0EA5E9 Vibrant Turquoise/Cyan
    val activeTurquoise = Color(0xFF06B6D4) // #06B6D4
    val primaryActionTeal = Color(0xFF0F766E) // #0F766E
    val sensitiveRed = Color(0xFFE11D48) // #E11D48 Soft Coral Red

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .clip(RoundedCornerShape(24.dp))
                .background(dialogBg)
                .border(1.5.dp, dialogBorder, RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(activeCyan.copy(alpha = 0.15f))
                                .border(1.dp, activeCyan, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = activeCyan,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.settings),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = titleColor
                            )
                            Text(
                                text = stringResource(R.string.app_name),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = activeCyan
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.cancel),
                            tint = descColor
                        )
                    }
                }

                // 1. Pattern Lock Section (قفل التطبيق بالنقش)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(cardBg)
                        .border(1.dp, cardBorder, RoundedCornerShape(16.dp))
                        .padding(14.dp)
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
                            Icon(
                                imageVector = Icons.Default.Gesture,
                                contentDescription = null,
                                tint = activeCyan,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.pattern_lock),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = titleColor
                                )
                                Text(
                                    text = stringResource(R.string.pattern_lock_desc),
                                    fontSize = 11.sp,
                                    color = descColor
                                )
                            }
                        }

                        Switch(
                            checked = isPatternLockEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    showPatternSetupDialog = true
                                } else {
                                    viewModel.disablePatternLock()
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = activeCyan,
                                checkedTrackColor = activeCyan.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier.testTag("pattern_lock_switch")
                        )
                    }

                    if (isPatternLockEnabled) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showPatternSetupDialog = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder)
                            ) {
                                Text(stringResource(R.string.change_pattern), fontSize = 12.sp, color = titleColor)
                            }

                            Button(
                                onClick = {
                                    onDismiss()
                                    onLockAppNow()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = sensitiveRed)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.lock_now),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                // 2. Automatic System Theme Info (وضع الهاتف تلقائياً)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(cardBg)
                        .border(1.dp, cardBorder, RoundedCornerShape(16.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.BrightnessAuto,
                        contentDescription = null,
                        tint = activeTurquoise,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.auto_theme),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = titleColor
                        )
                        Text(
                            text = stringResource(R.string.system_theme_active),
                            fontSize = 11.sp,
                            color = descColor
                        )
                    }
                }

                // 3. Language Selector (اللغة)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(cardBg)
                        .border(1.dp, cardBorder, RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            tint = activeCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.language),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = titleColor
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val currentLang = viewModel.currentLanguage.value
                    val languages = listOf(
                        Triple("ar", "العربية (Arabic - RTL)", "AR"),
                        Triple("fr", "Français (French)", "FR"),
                        Triple("en", "English", "EN")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        languages.forEach { (code, title, tag) ->
                            val isSelected = currentLang == code
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) activeCyan.copy(alpha = 0.15f) else if (isDarkTheme) Color(0x15FFFFFF) else Color(0xFFF8FAFC))
                                    .border(
                                        1.dp,
                                        if (isSelected) activeCyan else cardBorder,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { viewModel.setLanguage(code) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = tag,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal,
                                    color = if (isSelected) activeCyan else descColor
                                )
                            }
                        }
                    }
                }

                // 4. Version Information (معلومات الإصدار)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(cardBg)
                        .border(1.dp, cardBorder, RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = activeCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.version_information),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = titleColor
                            )
                        }

                        // Version Pill Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(primaryActionTeal.copy(alpha = 0.12f))
                                .border(1.dp, primaryActionTeal, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "v${BuildConfig.VERSION_NAME}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryActionTeal
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.app_version),
                            fontSize = 12.sp,
                            color = descColor
                        )
                        Text(
                            text = "v${BuildConfig.VERSION_NAME}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = titleColor
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.build_number),
                            fontSize = 12.sp,
                            color = descColor
                        )
                        Text(
                            text = "#${BuildConfig.VERSION_CODE}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = activeCyan
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Developer Attribution
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isDarkTheme) Color(0x1838BDF8) else Color(0xFFF1F5F9))
                            .border(1.dp, cardBorder, RoundedCornerShape(10.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(activeCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = activeCyan,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Developed by Charfeddine Kahlessenane",
                                fontFamily = CaveatHandwriting,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = if (isDarkTheme) activeCyan else primaryActionTeal
                            )
                        }
                    }
                }

                // 5. Cloud Backup & Restore (Google Drive)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(cardBg)
                        .border(1.dp, cardBorder, RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = null,
                                tint = activeCyan,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.backup_and_restore),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = titleColor
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(primaryActionTeal.copy(alpha = 0.12f))
                                .border(1.dp, primaryActionTeal, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Drive",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryActionTeal
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = stringResource(R.string.backup_desc),
                        fontSize = 11.sp,
                        color = descColor,
                        lineHeight = 15.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
                                createDocumentLauncher.launch("StrikePulse_Backup_$timestamp.json")
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = primaryActionTeal
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.backup_to_drive),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                openDocumentLauncher.launch(arrayOf("application/json", "*/*"))
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = primaryActionTeal
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.restore_from_drive),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = titleColor
                            )
                        }
                    }
                }

                // 6. Sample Data & Reset
                OutlinedButton(
                    onClick = {
                        viewModel.reloadSampleData()
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = activeCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.seed_sample_data),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = descColor
                    )
                }

                // About Info Footer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0x6094A3B8),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "StrikePulse v${BuildConfig.VERSION_NAME} • Developed by Charfeddine Kahlessenane",
                        fontSize = 10.sp,
                        color = Color(0x8094A3B8)
                    )
                }
            }
        }
    }

    // Modal Dialog to Confirm Restore Option (Merge vs Replace)
    if (showRestoreConfirmDialog && pendingRestoreUri != null) {
        val uri = pendingRestoreUri!!
        AlertDialog(
            onDismissRequest = {
                showRestoreConfirmDialog = false
                pendingRestoreUri = null
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CloudSync,
                        contentDescription = null,
                        tint = activeCyan
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.restore_dialog_title),
                        fontWeight = FontWeight.Bold,
                        color = titleColor
                    )
                }
            },
            text = {
                Text(
                    text = stringResource(R.string.restore_dialog_msg),
                    fontSize = 13.sp,
                    color = descColor
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.importBackup(context, uri, replaceExisting = false)
                        showRestoreConfirmDialog = false
                        pendingRestoreUri = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryActionTeal
                    )
                ) {
                    Text(
                        text = stringResource(R.string.restore_merge),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            showRestoreConfirmDialog = false
                            pendingRestoreUri = null
                        }
                    ) {
                        Text(stringResource(R.string.cancel), color = descColor)
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.importBackup(context, uri, replaceExisting = true)
                            showRestoreConfirmDialog = false
                            pendingRestoreUri = null
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = sensitiveRed
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, sensitiveRed.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = stringResource(R.string.restore_replace),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = sensitiveRed
                        )
                    }
                }
            }
        )
    }

    // Modal Dialog to Setup or Change Pattern Lock
    if (showPatternSetupDialog) {
        PatternSetupDialog(
            onDismiss = { showPatternSetupDialog = false },
            onPatternSaved = { pattern ->
                viewModel.setPatternLock(pattern)
                showPatternSetupDialog = false
            }
        )
    }
}

/**
 * Multi-step pattern registration dialog (Step 1: Draw pattern, Step 2: Confirm pattern)
 */
@Composable
fun PatternSetupDialog(
    onDismiss: () -> Unit,
    onPatternSaved: (String) -> Unit
) {
    val drawNewPatternStr = stringResource(R.string.draw_new_pattern)
    val confirmPatternStr = stringResource(R.string.confirm_pattern)
    val minPointsErrorStr = stringResource(R.string.pattern_min_points_error)
    val confirmedSuccessStr = stringResource(R.string.pattern_confirmed_success)
    val mismatchRetryStr = stringResource(R.string.pattern_mismatch_retry)

    var firstPattern by remember { mutableStateOf<String?>(null) }
    var instructionText by remember { mutableStateOf(drawNewPatternStr) }
    var status by remember { mutableStateOf(PatternStatus.IDLE) }
    val scope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xF00A1020))
                .border(1.5.dp, NeonViolet, RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.set_pattern_title),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = null)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = instructionText,
                    fontSize = 13.sp,
                    color = if (status == PatternStatus.ERROR) NeonRose else NeonCyan,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(16.dp))

                PatternLockCanvas(
                    modifier = Modifier.fillMaxWidth(),
                    status = status,
                    onPatternEntered = { entered ->
                        val nodeCount = entered.split("-").size
                        if (firstPattern == null) {
                            if (nodeCount < 4) {
                                status = PatternStatus.ERROR
                                instructionText = minPointsErrorStr
                                scope.launch {
                                    delay(900)
                                    status = PatternStatus.IDLE
                                    instructionText = drawNewPatternStr
                                }
                            } else {
                                firstPattern = entered
                                instructionText = confirmPatternStr
                                status = PatternStatus.IDLE
                            }
                        } else {
                            if (entered == firstPattern) {
                                status = PatternStatus.SUCCESS
                                instructionText = confirmedSuccessStr
                                scope.launch {
                                    delay(400)
                                    onPatternSaved(entered)
                                }
                            } else {
                                status = PatternStatus.ERROR
                                instructionText = mismatchRetryStr
                                scope.launch {
                                    delay(900)
                                    firstPattern = null
                                    status = PatternStatus.IDLE
                                    instructionText = drawNewPatternStr
                                }
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(
                    onClick = {
                        firstPattern = null
                        instructionText = drawNewPatternStr
                        status = PatternStatus.IDLE
                    }
                ) {
                    Text(stringResource(R.string.redraw_from_start), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
