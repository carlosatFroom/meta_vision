package com.metavision.glassreader

import com.meta.wearable.dat.display.Display
import com.meta.wearable.dat.display.views.ButtonGroupAlignment
import com.meta.wearable.dat.display.views.ButtonStyle
import com.meta.wearable.dat.display.views.Direction
import com.meta.wearable.dat.display.views.FlexBoxBackground
import com.meta.wearable.dat.display.views.TextColor
import com.meta.wearable.dat.display.views.TextStyle
import com.meta.wearable.dat.core.types.DatResult
import com.meta.wearable.dat.display.types.DisplayError
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SmsCardView {

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    suspend fun render(display: Display, message: SmsMessage): DatResult<Boolean, DisplayError> {
        return render(display, message, CardSettingsHolder.settings)
    }

    suspend fun render(
        display: Display,
        message: SmsMessage,
        s: CardSettings,
    ): DatResult<Boolean, DisplayError> {
        return display.sendContent {
            flexBox(
                direction = s.direction,
                gap = s.gap,
                alignment = s.alignment,
                background = s.background,
                padding = s.padding,
            ) {
                if (s.showCaption) {
                    val caption = message.sourceLabel?.let { "via $it" } ?: "New message"
                    text(
                        caption,
                        style = s.captionStyle,
                        color = s.captionColor,
                    )
                }
                text(message.sender, style = s.senderStyle, color = s.senderColor)
                text(message.body, style = s.bodyStyle, color = s.bodyColor)
                if (s.showTimestamp) {
                    text(
                        timeFormat.format(Date(message.timestamp)),
                        style = s.timestampStyle,
                        color = s.timestampColor,
                    )
                }
                if (s.showDismiss) {
                    buttonGroup(alignment = ButtonGroupAlignment.CENTER) {
                        button(
                            "Dismiss",
                            style = s.dismissStyle,
                            onClick = { DisplaySessionManager.clearDisplay() },
                        )
                    }
                }
            }
        }
    }
}
