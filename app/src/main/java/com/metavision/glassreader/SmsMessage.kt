package com.metavision.glassreader

data class SmsMessage(
    val sender: String,
    val body: String,
    val timestamp: Long = System.currentTimeMillis(),
    /** Human-readable label of the app that posted the notification (e.g. "Gmail"). */
    val sourceLabel: String? = null,
)
