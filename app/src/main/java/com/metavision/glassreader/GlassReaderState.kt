package com.metavision.glassreader

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale
import java.util.concurrent.LinkedBlockingQueue

/**
 * Singleton that coordinates SMS listening and TTS playback.
 * Manages a queue so messages are read one at a time without overlap.
 */
object GlassReaderState {
    private const val TAG = "GlassReaderState"
    private const val MAX_HISTORY = 50

    var isEnabled by mutableStateOf(false)
    var isListenerConnected by mutableStateOf(false)
    var isTtsReady by mutableStateOf(false)
    var isSpeaking by mutableStateOf(false)

    val recentMessages = mutableStateListOf<SmsMessage>()

    private var tts: TextToSpeech? = null
    private var audioManager: AudioManager? = null
    private val messageQueue = LinkedBlockingQueue<SmsMessage>()

    fun initialize(context: Context) {
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        isSpeaking = true
                    }

                    override fun onDone(utteranceId: String?) {
                        isSpeaking = false
                        abandonAudioFocus()
                        processNextMessage()
                    }

                    @Deprecated("Deprecated in API 21+")
                    override fun onError(utteranceId: String?) {
                        isSpeaking = false
                        abandonAudioFocus()
                        processNextMessage()
                    }
                })
                isTtsReady = true
                Log.d(TAG, "TTS initialized")
            } else {
                Log.e(TAG, "TTS init failed with status: $status")
            }
        }
    }

    fun onSmsReceived(message: SmsMessage) {
        recentMessages.add(0, message)
        if (recentMessages.size > MAX_HISTORY) {
            recentMessages.removeRange(MAX_HISTORY, recentMessages.size)
        }

        if (isEnabled && isTtsReady) {
            messageQueue.offer(message)
            if (!isSpeaking) {
                processNextMessage()
            }
        }
    }

    private fun processNextMessage() {
        val message = messageQueue.poll() ?: return
        val utterance = "Message from ${message.sender}: ${message.body}"

        requestAudioFocus()
        routeToBluetoothSco()

        val params = android.os.Bundle().apply {
            putInt(
                TextToSpeech.Engine.KEY_PARAM_STREAM,
                AudioManager.STREAM_VOICE_CALL
            )
        }
        tts?.speak(utterance, TextToSpeech.QUEUE_ADD, params, "sms_${message.timestamp}")
    }

    /**
     * Route audio through Bluetooth SCO (Synchronous Connection-Oriented) link.
     * This is the standard way to push audio to Bluetooth headsets/glasses
     * when they're connected via HFP (Hands-Free Profile).
     */
    private fun routeToBluetoothSco() {
        val am = audioManager ?: return
        if (isBluetoothHeadsetConnected()) {
            @Suppress("DEPRECATION")
            am.mode = AudioManager.MODE_IN_COMMUNICATION
            am.startBluetoothSco()
            am.isBluetoothScoOn = true
        }
    }

    private var focusRequest: AudioFocusRequest? = null

    private fun requestAudioFocus() {
        val am = audioManager ?: return
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .build()
        focusRequest = request
        am.requestAudioFocus(request)
    }

    private fun abandonAudioFocus() {
        val am = audioManager ?: return
        focusRequest?.let { am.abandonAudioFocusRequest(it) }
        focusRequest = null

        am.stopBluetoothSco()
        am.isBluetoothScoOn = false
        @Suppress("DEPRECATION")
        am.mode = AudioManager.MODE_NORMAL
    }

    private fun isBluetoothHeadsetConnected(): Boolean {
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return false
        return try {
            adapter.getProfileConnectionState(BluetoothProfile.HEADSET) ==
                BluetoothProfile.STATE_CONNECTED
        } catch (e: SecurityException) {
            Log.w(TAG, "Missing BLUETOOTH_CONNECT permission", e)
            false
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isTtsReady = false
        abandonAudioFocus()
    }
}
