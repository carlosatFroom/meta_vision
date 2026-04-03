package com.metavision.glassreader

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    private var hasNotificationAccess by mutableStateOf(false)
    private var hasBluetoothPermission by mutableStateOf(false)
    private var hasContactsPermission by mutableStateOf(false)

    private val bluetoothPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            hasBluetoothPermission = granted
        }

    private val contactsPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            hasContactsPermission = granted
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    GlassReaderScreen(
                        hasNotificationAccess = hasNotificationAccess,
                        hasBluetoothPermission = hasBluetoothPermission,
                        hasContactsPermission = hasContactsPermission,
                        onOpenNotificationSettings = { openNotificationListenerSettings() },
                        onRequestBluetooth = { requestBluetoothPermission() },
                        onRequestContacts = { requestContactsPermission() },
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        hasNotificationAccess = isNotificationListenerEnabled()
        hasBluetoothPermission = checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) ==
            PackageManager.PERMISSION_GRANTED
        hasContactsPermission = checkSelfPermission(Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun isNotificationListenerEnabled(): Boolean {
        val cn = ComponentName(this, SmsNotificationListener::class.java)
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return flat?.contains(cn.flattenToString()) == true
    }

    private fun openNotificationListenerSettings() {
        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }

    private fun requestBluetoothPermission() {
        bluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
    }

    private fun requestContactsPermission() {
        contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
    }
}

@Composable
fun GlassReaderScreen(
    hasNotificationAccess: Boolean,
    hasBluetoothPermission: Boolean,
    hasContactsPermission: Boolean,
    onOpenNotificationSettings: () -> Unit,
    onRequestBluetooth: () -> Unit,
    onRequestContacts: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Text(
            text = "GlassReader",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = "SMS to glasses via TTS",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Master toggle
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("Read SMS Aloud", fontWeight = FontWeight.SemiBold)
                    Text(
                        if (GlassReaderState.isSpeaking) "Speaking..."
                        else if (GlassReaderState.isEnabled) "Listening for messages"
                        else "Disabled",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = GlassReaderState.isEnabled,
                    onCheckedChange = { GlassReaderState.isEnabled = it },
                    enabled = hasNotificationAccess && hasBluetoothPermission &&
                        GlassReaderState.isTtsReady,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Status indicators
        StatusRow("Notification Access", hasNotificationAccess) {
            if (!hasNotificationAccess) {
                Button(
                    onClick = onOpenNotificationSettings,
                    modifier = Modifier.height(32.dp),
                    contentPadding = ButtonDefaults.TextButtonContentPadding,
                ) {
                    Text("Grant", fontSize = 12.sp)
                }
            }
        }
        StatusRow("Bluetooth Permission", hasBluetoothPermission) {
            if (!hasBluetoothPermission) {
                Button(
                    onClick = onRequestBluetooth,
                    modifier = Modifier.height(32.dp),
                    contentPadding = ButtonDefaults.TextButtonContentPadding,
                ) {
                    Text("Grant", fontSize = 12.sp)
                }
            }
        }
        StatusRow("Contacts (for caller names)", hasContactsPermission) {
            if (!hasContactsPermission) {
                Button(
                    onClick = onRequestContacts,
                    modifier = Modifier.height(32.dp),
                    contentPadding = ButtonDefaults.TextButtonContentPadding,
                ) {
                    Text("Grant", fontSize = 12.sp)
                }
            }
        }
        StatusRow("TTS Engine", GlassReaderState.isTtsReady)
        StatusRow("Listener Connected", GlassReaderState.isListenerConnected)

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "Recent Messages",
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (GlassReaderState.recentMessages.isEmpty()) {
            Text(
                "No messages yet. SMS notifications will appear here.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(GlassReaderState.recentMessages) { msg ->
                    MessageCard(msg)
                }
            }
        }
    }
}

@Composable
fun StatusRow(
    label: String,
    isOk: Boolean,
    action: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(if (isOk) Color(0xFF4CAF50) else Color(0xFFFF5722)),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, modifier = Modifier.weight(1f), fontSize = 14.sp)
        action?.invoke()
    }
}

@Composable
fun MessageCard(message: SmsMessage) {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    message.sender,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                )
                Text(
                    timeFormat.format(Date(message.timestamp)),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                message.body,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
