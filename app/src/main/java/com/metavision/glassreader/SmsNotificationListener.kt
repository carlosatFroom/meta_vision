package com.metavision.glassreader

import android.app.Notification
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class SmsNotificationListener : NotificationListenerService() {

    companion object {
        private const val TAG = "SmsNotificationListener"

        // Static reference so the UI can trigger a rescan. Set in onListenerConnected,
        // cleared in onListenerDisconnected. Null when the listener isn't bound.
        @Volatile
        private var instance: SmsNotificationListener? = null

        fun rescan() {
            instance?.scanActivePackages()
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pkg = sbn.packageName

        // Always record the package so it appears in the settings list, even
        // if the user hasn't enabled it yet. This is how the "possible sources"
        // list grows as the phone receives notifications.
        NotificationSourceStore.recordDiscovery(applicationContext, pkg)

        if (!GlassReaderState.isEnabled) return
        if (!NotificationSourceStore.isEnabled(pkg)) return

        val extras = sbn.notification.extras
        val rawSender = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: "Unknown"
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            ?: return

        // Skip summary/group notifications
        if (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return

        val sender = tryResolveContact(rawSender, pkg)
        val sourceLabel = tryResolveAppLabel(pkg)
        Log.d(TAG, "Notification from $sender via $pkg (${sourceLabel ?: "?"}): $text")

        val message = SmsMessage(sender = sender, body = text, sourceLabel = sourceLabel)
        GlassReaderState.onSmsReceived(message)
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        Log.d(TAG, "Notification listener connected")
        GlassReaderState.isListenerConnected = true
        // Seed the discovered set with everything currently active so the
        // settings list isn't empty on first open.
        scanActivePackages()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        instance = null
        Log.d(TAG, "Notification listener disconnected")
        GlassReaderState.isListenerConnected = false
    }

    fun scanActivePackages() {
        runCatching {
            val active = getActiveNotifications().map { it.packageName }.toSet()
            NotificationSourceStore.setDiscovered(applicationContext, active)
        }.onFailure { Log.w(TAG, "scanActivePackages failed", it) }
    }

    private fun tryResolveContact(rawSender: String, pkg: String): String {
        // Only attempt phone-number-to-contact resolution for the original SMS
        // packages; for everything else the notification title is already the
        // display name (e.g. "Mom", "Gmail", "ntfy topic").
        val smsPackages = setOf(
            "com.google.android.apps.messaging",
            "com.android.mms",
            "com.samsung.android.messaging",
            "com.oneplus.mms",
        )
        return if (pkg in smsPackages) ContactResolver.resolve(this, rawSender) else rawSender
    }

    private fun tryResolveAppLabel(pkg: String): String? {
        return runCatching {
            val pm = packageManager
            val ai: ApplicationInfo? = pm.getApplicationInfo(pkg, 0)
            ai?.let { pm.getApplicationLabel(it).toString() }
        }.getOrNull()
    }
}
