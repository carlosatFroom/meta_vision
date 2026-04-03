package com.metavision.glassreader

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract

/**
 * Resolves phone numbers to contact display names using the local contacts database.
 * All lookups are on-device — nothing leaves the phone.
 */
object ContactResolver {

    /**
     * Given a sender string (which may be a phone number or already a name),
     * try to resolve it to a contact name. Returns the original string if
     * no match is found or if it doesn't look like a phone number.
     */
    fun resolve(context: Context, sender: String): String {
        // If it already looks like a name (has letters, not just digits/punctuation), keep it
        if (sender.any { it.isLetter() }) return sender

        return lookupByNumber(context, sender) ?: sender
    }

    private fun lookupByNumber(context: Context, phoneNumber: String): String? {
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(phoneNumber),
        )
        return try {
            context.contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
                null,
                null,
                null,
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getString(0)
                } else {
                    null
                }
            }
        } catch (_: SecurityException) {
            null
        }
    }
}
