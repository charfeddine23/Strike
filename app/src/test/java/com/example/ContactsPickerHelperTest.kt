package com.example

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Intent
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.ContactsContract
import com.example.util.ContactsPickerHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowContentResolver

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ContactsPickerHelperTest {

    @Test
    fun `buildCandidatePickerIntents grants read uri permission and prioritizes properly`() {
        val withPermission = ContactsPickerHelper.buildCandidatePickerIntents(hasPermission = true)
        assertTrue(withPermission.isNotEmpty())
        assertEquals(ContactsContract.Contacts.CONTENT_URI, withPermission.first().data)
        assertTrue(withPermission.all { (it.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0 })

        val withoutPermission = ContactsPickerHelper.buildCandidatePickerIntents(hasPermission = false)
        assertTrue(withoutPermission.isNotEmpty())
        assertEquals(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, withoutPermission.first().data)
        assertTrue(withoutPermission.all { (it.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0 })
    }

    @Test
    fun `extractContactFromUri handles direct Phone data URI without secondary query`() {
        val authority = "com.example.test.contacts"
        val sampleUri = Uri.parse("content://$authority/data/42")

        val provider = object : ContentProvider() {
            override fun onCreate(): Boolean = true
            override fun query(
                uri: Uri,
                projection: Array<out String>?,
                selection: String?,
                selectionArgs: Array<out String>?,
                sortOrder: String?
            ): Cursor {
                return MatrixCursor(
                    arrayOf(
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.Phone.NUMBER,
                        ContactsContract.Data.MIMETYPE
                    )
                ).apply {
                    addRow(
                        arrayOf(
                            "Alex Mercer",
                            "+213555123456",
                            ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE
                        )
                    )
                }
            }

            override fun getType(uri: Uri): String? = null
            override fun insert(uri: Uri, values: ContentValues?): Uri? = null
            override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
            override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
        }

        ShadowContentResolver.registerProviderInternal(authority, provider)
        val context = RuntimeEnvironment.getApplication()
        val imported = ContactsPickerHelper.extractContactFromUri(context, sampleUri)

        assertNotNull(imported)
        assertEquals("Alex Mercer", imported?.name)
        assertEquals("+213555123456", imported?.phone)
    }

    @Test
    fun `extractContactFromUri preserves contact name even if secondary Phone query fails`() {
        val contactAuthority = "com.example.test.contacts2"
        val sampleContactUri = Uri.parse("content://$contactAuthority/contacts/7")

        val contactProvider = object : ContentProvider() {
            override fun onCreate(): Boolean = true
            override fun query(
                uri: Uri,
                projection: Array<out String>?,
                selection: String?,
                selectionArgs: Array<out String>?,
                sortOrder: String?
            ): Cursor {
                return MatrixCursor(
                    arrayOf(
                        ContactsContract.Contacts._ID,
                        ContactsContract.Contacts.DISPLAY_NAME,
                        ContactsContract.Contacts.HAS_PHONE_NUMBER
                    )
                ).apply {
                    addRow(arrayOf("7", "Sami Karim", 1))
                }
            }

            override fun getType(uri: Uri): String? = null
            override fun insert(uri: Uri, values: ContentValues?): Uri? = null
            override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
            override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
        }

        val throwingPhoneProvider = object : ContentProvider() {
            override fun onCreate(): Boolean = true
            override fun query(
                uri: Uri,
                projection: Array<out String>?,
                selection: String?,
                selectionArgs: Array<out String>?,
                sortOrder: String?
            ): Cursor {
                throw SecurityException("READ_CONTACTS required")
            }

            override fun getType(uri: Uri): String? = null
            override fun insert(uri: Uri, values: ContentValues?): Uri? = null
            override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
            override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
        }

        ShadowContentResolver.registerProviderInternal(contactAuthority, contactProvider)
        ShadowContentResolver.registerProviderInternal(ContactsContract.AUTHORITY, throwingPhoneProvider)

        val context = RuntimeEnvironment.getApplication()
        val imported = ContactsPickerHelper.extractContactFromUri(context, sampleContactUri)

        assertNotNull(imported)
        assertEquals("Sami Karim", imported?.name)
        assertEquals("", imported?.phone)
    }
}
