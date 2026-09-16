package com.metavision.glassreader

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray

/**
 * Tracks which apps on the phone are allowed to forward notifications to the glasses.
 *
 * Two sets, both persisted to SharedPreferences:
 *  - [discovered]: every package the listener has ever seen post a notification.
 *    Grows naturally as the phone receives notifications; the user picks from this list.
 *  - [enabled]: the subset the user has toggled on. Only notifications from these
 *    packages reach the glasses.
 *
 * Pre-seeded with the original hardcoded SMS packages so existing behavior is
 * preserved on upgrade.
 */
object NotificationSourceStore {
    private const val PREFS = "notification_sources"
    private const val KEY_DISCOVERED = "discovered_packages"
    private const val KEY_ENABLED = "enabled_packages"

    private val seedEnabled = setOf(
        "com.google.android.apps.messaging",  // Google Messages
        "com.android.mms",                   // Stock Android MMS
        "com.samsung.android.messaging",      // Samsung Messages
        "com.oneplus.mms",                   // OnePlus Messages
    )

    var discovered by mutableStateOf<Set<String>>(emptySet())
        private set
    var enabled by mutableStateOf<Set<String>>(emptySet())
        private set

    fun load(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val disc = prefs.getStringSet(KEY_DISCOVERED, null) ?: emptySet()
        val en = prefs.getStringSet(KEY_ENABLED, null) ?: emptySet()
        // Seed: ensure the original SMS packages are always known + enabled so
        // existing users keep working after the upgrade.
        discovered = disc + seedEnabled
        enabled = en + seedEnabled
        persist(context)
    }

    fun isEnabled(packageName: String): Boolean = packageName in enabled

    /** Called by the listener for every notification, regardless of enabled state. */
    fun recordDiscovery(context: Context, packageName: String) {
        if (packageName in discovered) return
        discovered = discovered + packageName
        persist(context)
    }

    fun setEnabled(context: Context, packageName: String, on: Boolean) {
        enabled = if (on) enabled + packageName else enabled - packageName
        persist(context)
    }

    /** Replace the discovered set with whatever the listener just scanned. */
    fun setDiscovered(context: Context, packages: Set<String>) {
        discovered = (discovered + packages)
        persist(context)
    }

    private fun persist(context: Context) {
        val e = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        e.putStringSet(KEY_DISCOVERED, discovered)
        e.putStringSet(KEY_ENABLED, enabled)
        e.apply()
    }
}
