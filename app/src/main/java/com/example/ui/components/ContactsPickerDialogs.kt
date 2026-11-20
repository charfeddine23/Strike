package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.DayCyan
import com.example.ui.theme.LocalAppDarkTheme
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.PlusJakartaSans
import com.example.util.ImportedContact

/**
 * Liquid Glass In-App Contacts Picker fallback dialog.
 * Shown automatically when the device lacks an external Contacts Picker activity
 * or rejects ACTION_PICK, while READ_CONTACTS permission is granted.
 */
@Composable
fun InAppContactsPickerDialog(
    contacts: List<ImportedContact>,
    currentLang: String,
    onSelectContact: (ImportedContact) -> Unit,
    onDismiss: () -> Unit,
    isDarkTheme: Boolean = LocalAppDarkTheme.current,
    useDialogWindow: Boolean = false
) {
    val isAr = currentLang == "ar"
    val isFr = currentLang == "fr"
    var search by remember { mutableStateOf("") }

    val filtered = remember(contacts, search) {
        val q = search.trim()
        if (q.isEmpty()) contacts
        else contacts.filter {
            it.name.contains(q, ignoreCase = true) ||
                it.phone.contains(q, ignoreCase = true)
        }
    }

    val panelBg = if (isDarkTheme) Color(0xF50B1324) else Color(0xFFF8FAFC)
    val accentCyan = if (isDarkTheme) NeonCyan else DayCyan

    val cardContent: @Composable () -> Unit = {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 460.dp)
                .heightIn(max = 560.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
                .shadow(
                    elevation = 24.dp,
                    shape = RoundedCornerShape(24.dp),
                    ambientColor = accentCyan.copy(alpha = 0.25f),
                    spotColor = NeonViolet.copy(alpha = 0.22f)
                )
                .clip(RoundedCornerShape(24.dp))
                .background(panelBg)
                .drawBehind {
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                accentCyan.copy(alpha = if (isDarkTheme) 0.14f else 0.08f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.2f, 0f),
                            radius = size.width * 0.8f
                        )
                    )
                }
                .border(
                    width = 1.2.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            accentCyan.copy(alpha = 0.7f),
                            NeonViolet.copy(alpha = 0.4f),
                            NeonEmerald.copy(alpha = 0.45f)
                        )
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(18.dp)
                .testTag("in_app_contacts_dialog")
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
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
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(accentCyan.copy(alpha = 0.16f))
                                .border(1.dp, accentCyan.copy(alpha = 0.55f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Contacts,
                                contentDescription = null,
                                tint = accentCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = when {
                                    isAr -> "جهات الاتصال"
                                    isFr -> "Contacts"
                                    else -> "Contacts"
                                },
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 17.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = when {
                                    isAr -> "${contacts.size} جهة اتصال متاحة"
                                    isFr -> "${contacts.size} contacts disponibles"
                                    else -> "${contacts.size} contacts available"
                                },
                                fontSize = 11.sp,
                                color = accentCyan,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("close_in_app_contacts_btn")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(if (isDarkTheme) Color(0x281E293B) else Color(0xFFE2E8F0))
                                .border(1.dp, accentCyan.copy(alpha = 0.4f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = if (isAr) "إغلاق" else "Close",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("in_app_contacts_search"),
                    placeholder = {
                        Text(
                            text = when {
                                isAr -> "ابحث بالاسم أو الرقم..."
                                isFr -> "Rechercher par nom ou numéro..."
                                else -> "Search by name or number..."
                            },
                            fontSize = 13.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = accentCyan
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentCyan,
                        unfocusedBorderColor = accentCyan.copy(alpha = 0.35f)
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (filtered.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when {
                                isAr -> "لم يتم العثور على جهات اتصال"
                                isFr -> "Aucun contact trouvé"
                                else -> "No contacts found"
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 360.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 6.dp)
                    ) {
                        itemsIndexed(filtered) { idx, item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        if (isDarkTheme) Color(0x1E1E293B) else Color(0xFFFFFFFF)
                                    )
                                    .border(
                                        1.dp,
                                        accentCyan.copy(alpha = if (isDarkTheme) 0.25f else 0.3f),
                                        RoundedCornerShape(14.dp)
                                    )
                                    .clickable { onSelectContact(item) }
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                                    .testTag("in_app_contact_item_$idx"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(accentCyan.copy(alpha = 0.16f))
                                        .border(1.dp, accentCyan.copy(alpha = 0.45f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = accentCyan,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (item.phone.isNotBlank()) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Phone,
                                                contentDescription = null,
                                                tint = NeonEmerald,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = item.phone,
                                                fontSize = 12.sp,
                                                color = NeonEmerald,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (useDialogWindow) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            cardContent()
        }
    } else {
        BackHandler(onBack = onDismiss)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.62f))
                .imePadding()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            cardContent()
        }
    }
}

/**
 * Shown when Contacts permission was denied AND no zero-permission external Contacts picker could open.
 */
@Composable
fun ContactsPermissionRequiredDialog(
    currentLang: String,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
    isDarkTheme: Boolean = LocalAppDarkTheme.current,
    useDialogWindow: Boolean = false
) {
    val isAr = currentLang == "ar"
    val isFr = currentLang == "fr"
    val panelBg = if (isDarkTheme) Color(0xF50B1324) else Color(0xFFF8FAFC)
    val accentCyan = if (isDarkTheme) NeonCyan else DayCyan

    val cardContent: @Composable () -> Unit = {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .widthIn(max = 420.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
                .shadow(20.dp, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .background(panelBg)
                .border(
                    1.2.dp,
                    Brush.linearGradient(listOf(NeonAmber, accentCyan)),
                    RoundedCornerShape(24.dp)
                )
                .padding(20.dp)
                .testTag("contacts_permission_dialog")
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(NeonAmber.copy(alpha = 0.18f))
                            .border(1.dp, NeonAmber.copy(alpha = 0.6f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Contacts,
                            contentDescription = null,
                            tint = NeonAmber,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = when {
                            isAr -> "إذن جهات الاتصال مطلوب"
                            isFr -> "Autorisation Contacts requise"
                            else -> "Contacts Permission Needed"
                        },
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = when {
                        isAr -> "لم يتمكن التطبيق من فتح جهات الاتصال لعدم توفر إذن الوصول. يرجى تفعيل إذن «جهات الاتصال» من إعدادات التطبيق ثم المحاولة مرة أخرى."
                        isFr -> "Impossible d'ouvrir les contacts sans autorisation. Veuillez activer l'accès aux Contacts dans les paramètres de l'application."
                        else -> "Unable to open Contacts without access permission. Please enable Contacts permission in App Settings and try again."
                    },
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 19.sp
                )

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
                            .testTag("btn_contacts_perm_cancel"),
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
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .weight(1.3f)
                            .height(48.dp)
                            .testTag("open_contacts_settings_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentCyan,
                            contentColor = Color(0xFF021B26)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when {
                                isAr -> "فتح الإعدادات"
                                isFr -> "Paramètres"
                                else -> "Open Settings"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    if (useDialogWindow) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            cardContent()
        }
    } else {
        BackHandler(onBack = onDismiss)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.62f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            cardContent()
        }
    }
}
