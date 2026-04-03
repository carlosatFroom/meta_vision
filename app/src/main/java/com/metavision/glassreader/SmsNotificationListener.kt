package com.metavision.glassreader

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class SmsNotificationListener : NotificationListenerService() {

    companion object {
        private const val TAG = "SmsNotificationListener"

        // Packages that send SMS/MMS notifications
        private val SMS_PACKAGES = setOf(
            "com.google.android.apps.messaging",  // Google Messages
            "com.android.mms",                     // Stock Android MMS
            "com.samsung.android.messaging",       // Samsung Messages
            "com.oneplus.mms",                     // OnePlus Messages
        )
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (!GlassReaderState.isEnabled) return

        val pkg = sbn.packageName
        if (pkg !in SMS_PACKAGES) return

        val extras = sbn.notification.extras
        val rawSender = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: "Unknown"
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: return

        // Skip summary/group notifications
        if (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return

        // Resolve phone numbers to contact names (local lookup, nothing leaves the device)
        val sender = ContactResolver.resolve(this, rawSender)
        Log.d(TAG, "SMS from $sender: $text")

        val message = SmsMessage(sender = sender, body = text)
        GlassReaderState.onSmsReceived(message)
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "Notification listener connected")
        GlassReaderState.isListenerConnected = true
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.d(TAG, "Notification listener disconnected")
        GlassReaderState.isListenerConnected = false
    }
}
