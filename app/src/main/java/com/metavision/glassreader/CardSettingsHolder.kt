package com.metavision.glassreader

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Process-wide holder for the current card appearance settings.
 * Initialized at app startup from SharedPreferences, updated live by the
 * settings screen, and read by [SmsCardView] on every render.
 */
object CardSettingsHolder {
    var settings by mutableStateOf(CardSettings())
        private set

    fun load(context: Context) {
        settings = CardSettingsStore.load(context)
    }

    fun update(context: Context, newSettings: CardSettings) {
        settings = newSettings
        CardSettingsStore.save(context, newSettings)
    }
}
