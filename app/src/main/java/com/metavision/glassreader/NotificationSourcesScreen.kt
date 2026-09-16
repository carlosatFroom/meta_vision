package com.metavision.glassreader

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun NotificationSourcesScreen(
    context: Context,
    onBack: () -> Unit,
    onRescan: () -> Unit,
) {
    val discovered = NotificationSourceStore.discovered
    val enabled = NotificationSourceStore.enabled

    // Resolve app labels once per discovery change.
    val labels = remember(discovered) { resolveAppLabels(context, discovered) }
    // Sort: enabled first, then alphabetically by label.
    val sorted = remember(discovered, enabled, labels) {
        discovered.toList().sortedWith(
            compareByDescending<String> { it in enabled }
                .thenBy { labels[it]?.lowercase() ?: it.lowercase() }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Notification Sources", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            OutlinedButton(onClick = onBack) { Text("Back") }
        }
        Text(
            "Toggle which apps can forward notifications to the glasses. " +
                "New apps appear here automatically as they post notifications.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("${enabled.size} enabled · ${discovered.size} discovered", fontSize = 13.sp)
            OutlinedButton(onClick = onRescan) { Text("Rescan now") }
        }

        Spacer(Modifier.height(12.dp))

        if (discovered.isEmpty()) {
            Text(
                "No notifications seen yet. Tap \"Rescan now\" to pick up currently-active " +
                    "notifications, or just use the phone — apps will appear here as they " +
                    "post notifications.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(sorted, key = { it }) { pkg ->
                    SourceRow(
                        packageName = pkg,
                        label = labels[pkg] ?: pkg,
                        checked = pkg in enabled,
                        onToggle = { NotificationSourceStore.setEnabled(context, pkg, it) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SourceRow(
    packageName: String,
    label: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(
                    packageName,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = checked, onCheckedChange = onToggle)
        }
    }
}

private fun resolveAppLabels(context: Context, packages: Set<String>): Map<String, String> {
    val pm = context.packageManager
    val out = mutableMapOf<String, String>()
    for (pkg in packages) {
        out[pkg] = runCatching {
            val ai: ApplicationInfo = pm.getApplicationInfo(pkg, 0)
            pm.getApplicationLabel(ai).toString()
        }.getOrNull() ?: pkg
    }
    return out
}
