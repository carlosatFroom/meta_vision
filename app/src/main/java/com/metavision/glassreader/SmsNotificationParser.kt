package com.metavision.glassreader

/** Sender and body extracted from a notification's extras. */
internal data class ParsedNotification(val sender: String, val body: String)

/**
 * Pure parsing of notification extras, kept free of Android framework calls
 * so it can be unit tested on the JVM.
 */
internal object SmsNotificationParser {
    const val UNKNOWN_SENDER = "Unknown"

    /**
     * Returns the sender and body, or null when there is no usable body.
     * bigText wins over text because text is often a truncated preview.
     * A missing or blank title falls back to [UNKNOWN_SENDER].
     */
    fun parse(title: CharSequence?, text: CharSequence?, bigText: CharSequence?): ParsedNotification? {
        val body = bigText?.toString()?.takeIf { it.isNotBlank() }
            ?: text?.toString()?.takeIf { it.isNotBlank() }
            ?: return null
        val sender = title?.toString()?.trim()?.takeIf { it.isNotEmpty() } ?: UNKNOWN_SENDER
        return ParsedNotification(sender = sender, body = body)
    }
}
