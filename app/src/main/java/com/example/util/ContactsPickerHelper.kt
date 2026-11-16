package com.example.util

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat

data class ImportedContact(
    val name: String,
    val phone: String
)

object ContactsPickerHelper {

    fun hasReadContactsPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Ordered list of standard Android Contact Picker intents.
     * - When READ_CONTACTS is granted, Contacts.CONTENT_URI is preferred so contacts
     *   both with and without phone numbers can be picked cleanly.
     * - When READ_CONTACTS is not granted, Phone.CONTENT_URI is preferred first because
     *   the returned data URI includes DISPLAY_NAME and NUMBER directly via temporary grant.
     */
    fun buildCandidatePickerIntents(hasPermission: Boolean): List<Intent> {
        val rawIntents = if (hasPermission) {
            listOf(
                Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI),
                Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI),
                Intent(Intent.ACTION_PICK).apply {
                    type = ContactsContract.Contacts.CONTENT_TYPE
                },
                Intent(Intent.ACTION_PICK).apply {
                    type = ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE
                },
                Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE
                },
                Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = ContactsContract.Contacts.CONTENT_ITEM_TYPE
                }
            )
        } else {
            listOf(
                Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI),
                Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI),
                Intent(Intent.ACTION_PICK).apply {
                    type = ContactsContract.Contacts.CONTENT_TYPE
                },
                Intent(Intent.ACTION_PICK).apply {
                    type = ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE
                },
                Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE
                },
                Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = ContactsContract.Contacts.CONTENT_ITEM_TYPE
                }
            )
        }

        return rawIntents.map { intent ->
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    /**
     * Attempts to open the system Contacts picker using resolved candidates first,
     * and falling back across all candidate intents if needed.
     */
    fun tryLaunchExternalPicker(
        context: Context,
        launcher: ActivityResultLauncher<Intent>,
        hasPermission: Boolean
    ): Boolean {
        val candidates = buildCandidatePickerIntents(hasPermission)
        val pm = context.packageManager

        val resolved = try {
            candidates.filter { it.resolveActivity(pm) != null }
        } catch (_: Exception) {
            emptyList()
        }

        val orderedToTry = (resolved + candidates).distinct()
        for (intent in orderedToTry) {
            try {
                launcher.launch(intent)
                return true
            } catch (_: ActivityNotFoundException) {
                // Try next intent
            } catch (_: SecurityException) {
                // Try next intent
            } catch (_: Exception) {
                // Try next intent
            }
        }
        return false
    }

    /**
     * Extracts contact name and phone number from either:
     * - A CommonDataKinds.Phone URI (data/#) which carries DISPLAY_NAME & NUMBER directly
     * - A Contacts URI (contacts/lookup/# or contacts/#) with optional secondary Phone query
     */
    fun extractContactFromUri(context: Context, contactUri: Uri): ImportedContact? {
        var contactName = ""
        var contactPhone = ""
        var contactId: String? = null
        var lookupKey: String? = null
        var hasPhone = 0

        try {
            context.contentResolver.query(contactUri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                        .takeIf { it != -1 }
                        ?: cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    if (nameIdx != -1) {
                        contactName = cursor.getString(nameIdx)?.trim().orEmpty()
                    }

                    val mimeIdx = cursor.getColumnIndex(ContactsContract.Data.MIMETYPE)
                    val mimeType = if (mimeIdx != -1) cursor.getString(mimeIdx) else null
                    val phoneColIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    val hasPhoneColIdx = cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)

                    // Direct Phone/Data URI row contains Phone.NUMBER directly (no Contacts.HAS_PHONE_NUMBER)
                    if (phoneColIdx != -1 && (
                            mimeType == ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE ||
                                hasPhoneColIdx == -1
                            )
                    ) {
                        contactPhone = cursor.getString(phoneColIdx)?.trim().orEmpty()
                    }

                    val contactIdIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                        .takeIf { it != -1 }
                        ?: cursor.getColumnIndex(ContactsContract.Contacts._ID)
                    if (contactIdIdx != -1) {
                        contactId = cursor.getString(contactIdIdx)
                    }

                    val lookupIdx = cursor.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY)
                    if (lookupIdx != -1) {
                        lookupKey = cursor.getString(lookupIdx)
                    }

                    if (hasPhoneColIdx != -1) {
                        hasPhone = cursor.getInt(hasPhoneColIdx)
                    }
                }
            }
        } catch (_: Exception) {
            return null
        }

        // Secondary Phone lookup when URI was a Contacts URI (not a Phone data URI)
        if (contactPhone.isBlank() && (!contactId.isNullOrBlank() || !lookupKey.isNullOrBlank())) {
            val shouldQueryPhones = hasPhone > 0 || !contactId.isNullOrBlank()
            if (shouldQueryPhones) {
                try {
                    val selection: String
                    val selectionArgs: Array<String>
                    if (!contactId.isNullOrBlank()) {
                        selection = "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?"
                        selectionArgs = arrayOf(contactId!!)
                    } else {
                        selection = "${ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY} = ?"
                        selectionArgs = arrayOf(lookupKey!!)
                    }

                    context.contentResolver.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                        selection,
                        selectionArgs,
                        null
                    )?.use { phoneCursor ->
                        if (phoneCursor.moveToFirst()) {
                            val numIdx = phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                            if (numIdx != -1) {
                                contactPhone = phoneCursor.getString(numIdx)?.trim().orEmpty()
                            }
                        }
                    }
                } catch (_: SecurityException) {
                    // Keep contactName even if Phone.CONTENT_URI requires READ_CONTACTS
                } catch (_: Exception) {
                    // Ignore secondary query errors gracefully
                }
            }
        }

        if (contactName.isBlank() && contactPhone.isBlank()) {
            return null
        }
        return ImportedContact(name = contactName, phone = contactPhone)
    }

    /**
     * Queries device contacts directly for the in-app fallback contacts selector
     * when READ_CONTACTS is granted.
     */
    fun loadDeviceContacts(context: Context): List<ImportedContact> {
        if (!hasReadContactsPermission(context)) return emptyList()

        val result = mutableListOf<ImportedContact>()
        val seenKeys = mutableSetOf<String>()

        try {
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                null,
                null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )?.use { cursor ->
                val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (cursor.moveToNext()) {
                    val name = if (nameIdx != -1) cursor.getString(nameIdx)?.trim().orEmpty() else ""
                    val phone = if (numIdx != -1) cursor.getString(numIdx)?.trim().orEmpty() else ""
                    if (name.isNotBlank() || phone.isNotBlank()) {
                        val key = "${name.lowercase()}|${phone.filter { it.isDigit() || it == '+' }}"
                        if (seenKeys.add(key)) {
                            result.add(ImportedContact(name = name.ifBlank { phone }, phone = phone))
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Fall through gracefully
        }

        // Also include contacts without phone numbers if they aren't already listed by name
        try {
            val existingNames = result.map { it.name.lowercase() }.toSet()
            context.contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                arrayOf(ContactsContract.Contacts.DISPLAY_NAME),
                null,
                null,
                "${ContactsContract.Contacts.DISPLAY_NAME} ASC"
            )?.use { cursor ->
                val nameIdx = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                while (cursor.moveToNext()) {
                    val name = if (nameIdx != -1) cursor.getString(nameIdx)?.trim().orEmpty() else ""
                    if (name.isNotBlank() && name.lowercase() !in existingNames) {
                        result.add(ImportedContact(name = name, phone = ""))
                    }
                }
            }
        } catch (_: Exception) {
            // Ignore gracefully
        }

        return result.sortedBy { it.name.lowercase() }
    }

    fun openAppSettings(context: Context): Boolean {
        return try {
            val intent = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", context.packageName, null)
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }
}
