package com.metavision.glassreader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meta.wearable.dat.display.views.Alignment as DisplayAlignment
import com.meta.wearable.dat.display.views.ButtonStyle
import com.meta.wearable.dat.display.views.Direction
import com.meta.wearable.dat.display.views.FlexBoxBackground
import com.meta.wearable.dat.display.views.TextColor
import com.meta.wearable.dat.display.views.TextStyle

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onSendTestCard: () -> Unit,
    onUpdate: (CardSettings) -> Unit,
    settings: CardSettings,
    displayReady: Boolean,
    onOpenSources: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Card Settings", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            OutlinedButton(onClick = onBack) { Text("Back") }
        }
        Text(
            "Tune the card for best visibility on the waveguide. " +
                "Changes save automatically and apply to the next SMS.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(20.dp))

        // Notification sources entry point
        OutlinedButton(
            onClick = onOpenSources,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Notification Sources")
        }

        Spacer(Modifier.height(20.dp))

        SectionLabel("Layout")
        EnumDropdown("Background", FlexBoxBackground.entries, settings.background) {
            onUpdate(settings.copy(background = it))
        }
        EnumDropdown("Direction", Direction.entries, settings.direction) {
            onUpdate(settings.copy(direction = it))
        }
        EnumDropdown("Alignment", DisplayAlignment.entries, settings.alignment) {
            onUpdate(settings.copy(alignment = it))
        }
        Text("Gap: ${settings.gap}", fontSize = 13.sp)
        Slider(
            value = settings.gap.toFloat(),
            onValueChange = { onUpdate(settings.copy(gap = it.toInt().coerceIn(0, 32))) },
            valueRange = 0f..32f,
        )
        Text("Padding: ${settings.padding}", fontSize = 13.sp)
        Slider(
            value = settings.padding.toFloat(),
            onValueChange = { onUpdate(settings.copy(padding = it.toInt().coerceIn(0, 48))) },
            valueRange = 0f..48f,
        )

        Spacer(Modifier.height(12.dp))

        SectionLabel("Elements")
        SwitchRow("Show \"New message\" caption", settings.showCaption) {
            onUpdate(settings.copy(showCaption = it))
        }
        SwitchRow("Show timestamp", settings.showTimestamp) {
            onUpdate(settings.copy(showTimestamp = it))
        }
        SwitchRow("Show Dismiss button", settings.showDismiss) {
            onUpdate(settings.copy(showDismiss = it))
        }

        Spacer(Modifier.height(12.dp))

        SectionLabel("Sender")
        EnumDropdown("Style", TextStyle.entries, settings.senderStyle) {
            onUpdate(settings.copy(senderStyle = it))
        }
        EnumDropdown("Color", TextColor.entries, settings.senderColor) {
            onUpdate(settings.copy(senderColor = it))
        }

        SectionLabel("Body")
        EnumDropdown("Style", TextStyle.entries, settings.bodyStyle) {
            onUpdate(settings.copy(bodyStyle = it))
        }
        EnumDropdown("Color", TextColor.entries, settings.bodyColor) {
            onUpdate(settings.copy(bodyColor = it))
        }

        SectionLabel("Caption")
        EnumDropdown("Style", TextStyle.entries, settings.captionStyle) {
            onUpdate(settings.copy(captionStyle = it))
        }
        EnumDropdown("Color", TextColor.entries, settings.captionColor) {
            onUpdate(settings.copy(captionColor = it))
        }

        SectionLabel("Timestamp")
        EnumDropdown("Style", TextStyle.entries, settings.timestampStyle) {
            onUpdate(settings.copy(timestampStyle = it))
        }
        EnumDropdown("Color", TextColor.entries, settings.timestampColor) {
            onUpdate(settings.copy(timestampColor = it))
        }

        SectionLabel("Dismiss Button")
        EnumDropdown("Style", ButtonStyle.entries, settings.dismissStyle) {
            onUpdate(settings.copy(dismissStyle = it))
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = onSendTestCard,
            enabled = displayReady,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (displayReady) "Send Test Card to Glasses" else "Connect glasses first")
        }
        if (!displayReady) {
            Text(
                "The glasses display must be connected to preview the card.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
        }

        Spacer(Modifier.height(60.dp))
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 12.dp, bottom = 6.dp),
    )
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 14.sp)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun <T : Enum<T>> EnumDropdown(
    label: String,
    options: Collection<T>,
    selected: T,
    onSelect: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 14.sp)
        Column {
            OutlinedButton(onClick = { expanded = true }) {
                Text(selected.name, fontSize = 13.sp)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { opt ->
                    DropdownMenuItem(
                        text = { Text(opt.name) },
                        onClick = { onSelect(opt); expanded = false },
                    )
                }
            }
        }
    }
}
