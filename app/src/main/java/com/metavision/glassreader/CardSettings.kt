package com.metavision.glassreader

import android.content.Context
import com.meta.wearable.dat.display.views.Alignment
import com.meta.wearable.dat.display.views.ButtonStyle
import com.meta.wearable.dat.display.views.Direction
import com.meta.wearable.dat.display.views.FlexBoxBackground
import com.meta.wearable.dat.display.views.TextColor
import com.meta.wearable.dat.display.views.TextStyle

/**
 * User-tunable card appearance. Every field maps to a real DAT 0.8.0 DSL option —
 * the SDK exposes only fixed presets (no arbitrary RGB / font size / font family),
 * so this is the full customization surface.
 *
 * Persisted to SharedPreferences so the chosen combination survives restarts
 * and applies to every real SMS card.
 */
data class CardSettings(
    val background: FlexBoxBackground = FlexBoxBackground.NONE,
    val direction: Direction = Direction.COLUMN,
    val alignment: Alignment = Alignment.START,
    val gap: Int = 8,
    val padding: Int = 16,
    val showCaption: Boolean = true,
    val showTimestamp: Boolean = true,
    val showDismiss: Boolean = true,
    val senderStyle: TextStyle = TextStyle.HEADING,
    val senderColor: TextColor = TextColor.PRIMARY,
    val bodyStyle: TextStyle = TextStyle.BODY,
    val bodyColor: TextColor = TextColor.PRIMARY,
    val captionStyle: TextStyle = TextStyle.META,
    val captionColor: TextColor = TextColor.SECONDARY,
    val timestampStyle: TextStyle = TextStyle.META,
    val timestampColor: TextColor = TextColor.SECONDARY,
    val dismissStyle: ButtonStyle = ButtonStyle.PRIMARY,
)

object CardSettingsStore {
    private const val PREFS = "card_settings"
    private const val KEY_PREFIX = "card_"

    fun load(context: Context): CardSettings {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return CardSettings(
            background = enumOrDefault(prefs, "background", FlexBoxBackground.NONE),
            direction = enumOrDefault(prefs, "direction", Direction.COLUMN),
            alignment = enumOrDefault(prefs, "alignment", Alignment.START),
            gap = prefs.getInt("${KEY_PREFIX}gap", 8),
            padding = prefs.getInt("${KEY_PREFIX}padding", 16),
            showCaption = prefs.getBoolean("${KEY_PREFIX}showCaption", true),
            showTimestamp = prefs.getBoolean("${KEY_PREFIX}showTimestamp", true),
            showDismiss = prefs.getBoolean("${KEY_PREFIX}showDismiss", true),
            senderStyle = enumOrDefault(prefs, "senderStyle", TextStyle.HEADING),
            senderColor = enumOrDefault(prefs, "senderColor", TextColor.PRIMARY),
            bodyStyle = enumOrDefault(prefs, "bodyStyle", TextStyle.BODY),
            bodyColor = enumOrDefault(prefs, "bodyColor", TextColor.PRIMARY),
            captionStyle = enumOrDefault(prefs, "captionStyle", TextStyle.META),
            captionColor = enumOrDefault(prefs, "captionColor", TextColor.SECONDARY),
            timestampStyle = enumOrDefault(prefs, "timestampStyle", TextStyle.META),
            timestampColor = enumOrDefault(prefs, "timestampColor", TextColor.SECONDARY),
            dismissStyle = enumOrDefault(prefs, "dismissStyle", ButtonStyle.PRIMARY),
        )
    }

    fun save(context: Context, settings: CardSettings) {
        val e = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        e.putString("${KEY_PREFIX}background", settings.background.name)
        e.putString("${KEY_PREFIX}direction", settings.direction.name)
        e.putString("${KEY_PREFIX}alignment", settings.alignment.name)
        e.putInt("${KEY_PREFIX}gap", settings.gap)
        e.putInt("${KEY_PREFIX}padding", settings.padding)
        e.putBoolean("${KEY_PREFIX}showCaption", settings.showCaption)
        e.putBoolean("${KEY_PREFIX}showTimestamp", settings.showTimestamp)
        e.putBoolean("${KEY_PREFIX}showDismiss", settings.showDismiss)
        e.putString("${KEY_PREFIX}senderStyle", settings.senderStyle.name)
        e.putString("${KEY_PREFIX}senderColor", settings.senderColor.name)
        e.putString("${KEY_PREFIX}bodyStyle", settings.bodyStyle.name)
        e.putString("${KEY_PREFIX}bodyColor", settings.bodyColor.name)
        e.putString("${KEY_PREFIX}captionStyle", settings.captionStyle.name)
        e.putString("${KEY_PREFIX}captionColor", settings.captionColor.name)
        e.putString("${KEY_PREFIX}timestampStyle", settings.timestampStyle.name)
        e.putString("${KEY_PREFIX}timestampColor", settings.timestampColor.name)
        e.putString("${KEY_PREFIX}dismissStyle", settings.dismissStyle.name)
        e.apply()
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(
        prefs: android.content.SharedPreferences,
        key: String,
        default: T,
    ): T {
        val name = prefs.getString("${KEY_PREFIX}$key", null) ?: return default
        return runCatching { enumValueOf<T>(name) }.getOrDefault(default)
    }
}
