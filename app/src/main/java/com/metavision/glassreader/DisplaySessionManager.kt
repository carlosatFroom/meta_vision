package com.metavision.glassreader

import android.app.Activity
import android.content.Context
import android.util.Log
import com.meta.wearable.dat.core.Wearables
import com.meta.wearable.dat.core.selectors.AutoDeviceSelector
import com.meta.wearable.dat.core.session.DeviceSession
import com.meta.wearable.dat.core.session.DeviceSessionState
import com.meta.wearable.dat.core.types.DeviceSessionError
import com.meta.wearable.dat.core.types.RegistrationState
import com.meta.wearable.dat.display.Display
import com.meta.wearable.dat.display.addDisplay
import com.meta.wearable.dat.display.removeDisplay
import com.meta.wearable.dat.display.types.DisplayConfiguration
import com.meta.wearable.dat.display.types.DisplayState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

object DisplaySessionManager {
    private const val TAG = "DisplaySession"
    private const val SESSION_START_TIMEOUT_MS = 15_000L
    private const val SESSION_RETRY_DELAY_MS = 2_000L
    private const val MAX_START_RETRIES = 1

    enum class ConnectionState { UNREGISTERED, REGISTERED, CONNECTING, DISPLAY_READY, FAILED }

    private val _connectionState = MutableStateFlow(ConnectionState.UNREGISTERED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var session: DeviceSession? = null
    private var display: Display? = null

    fun initialize(context: Context) {
        _connectionState.value =
            if (Wearables.registrationState.value == RegistrationState.REGISTERED)
                ConnectionState.REGISTERED
            else ConnectionState.UNREGISTERED

        scope.launch {
            Wearables.registrationState.collectLatest { state ->
                if (state == RegistrationState.REGISTERED) {
                    if (_connectionState.value == ConnectionState.UNREGISTERED)
                        _connectionState.value = ConnectionState.REGISTERED
                } else {
                    teardown()
                    _connectionState.value = ConnectionState.UNREGISTERED
                }
            }
        }
    }

    fun startRegistration(activity: Activity) {
        runCatching { Wearables.startRegistration(activity) }
            .onFailure { Log.e(TAG, "startRegistration failed", it) }
    }

    fun startUnregistration(activity: Activity) {
        runCatching { Wearables.startUnregistration(activity) }
            .onFailure { Log.e(TAG, "startUnregistration failed", it) }
    }

    fun connect(activity: Activity) {
        when (_connectionState.value) {
            ConnectionState.UNREGISTERED -> startRegistration(activity)
            ConnectionState.REGISTERED, ConnectionState.FAILED -> openSessionAndDisplay()
            ConnectionState.CONNECTING, ConnectionState.DISPLAY_READY -> Unit
        }
    }

    private fun openSessionAndDisplay() {
        _connectionState.value = ConnectionState.CONNECTING
        scope.launch {
            // Tear down any leftover session/display before creating a new one.
            teardown()
            openSessionAndDisplayInternal(attempt = 0)
        }
    }

    private suspend fun openSessionAndDisplayInternal(attempt: Int) {
        val newSession = try {
            Wearables.createSession(AutoDeviceSelector()).getOrThrow()
        } catch (e: Throwable) {
            Log.e(TAG, "createSession failed: ${e.message}", e)
            _connectionState.value = ConnectionState.FAILED
            return
        }
        session = newSession
        newSession.start()
        Log.i(TAG, "session starting…")

        val outcome = CompletableDeferred<Pair<Boolean, DeviceSessionError?>>()

        val stateJob = scope.launch {
            val st = newSession.state.first {
                it == DeviceSessionState.STARTED || it == DeviceSessionState.STOPPED
            }
            outcome.complete((st == DeviceSessionState.STARTED) to null)
        }
        val errorJob = scope.launch {
            val err = newSession.errors.first()
            outcome.complete(false to err)
        }

        val (success, err) = withTimeoutOrNull(SESSION_START_TIMEOUT_MS) {
            outcome.await()
        } ?: (false to null)
        stateJob.cancelAndJoin()
        errorJob.cancelAndJoin()

        if (success != true) {
            val label = err?.name ?: "timeout"
            Log.e(TAG, "session start failed: $label")
            runCatching { newSession.stop() }
            session = null
            // The glasses DAT app may need a moment to wake up — retry once.
            if (attempt < MAX_START_RETRIES && shouldRetry(err)) {
                Log.i(TAG, "retrying session start after $SESSION_RETRY_DELAY_MS ms")
                delay(SESSION_RETRY_DELAY_MS)
                openSessionAndDisplayInternal(attempt + 1)
            } else {
                _connectionState.value = ConnectionState.FAILED
            }
            return
        }
        Log.i(TAG, "session started")

        val newDisplay = try {
            newSession.addDisplay(DisplayConfiguration()).getOrThrow()
        } catch (e: Throwable) {
            Log.e(TAG, "addDisplay failed: ${e.message}", e)
            runCatching { newSession.stop() }
            session = null
            _connectionState.value = ConnectionState.FAILED
            return
        }
        display = newDisplay
        Log.i(TAG, "display added, observing state…")
        observeDisplayState(newDisplay)
    }

    private fun shouldRetry(err: DeviceSessionError?): Boolean =
        err == null || // timeout — device may not have responded yet
            err == DeviceSessionError.DWA_UNAVAILABLE ||
            err == DeviceSessionError.DEVICE_DISCONNECTED

    private fun observeDisplayState(d: Display) {
        scope.launch {
            d.state.collectLatest { state ->
                Log.i(TAG, "display state: $state")
                when (state) {
                    DisplayState.STARTED -> {
                        _connectionState.value = ConnectionState.DISPLAY_READY
                    }
                    DisplayState.STOPPED, DisplayState.CLOSED ->
                        _connectionState.value = ConnectionState.REGISTERED
                    DisplayState.STARTING, DisplayState.STOPPING -> Unit
                }
            }
        }
    }

    suspend fun showMessage(message: SmsMessage) {
        val d = display ?: return
        if (_connectionState.value != ConnectionState.DISPLAY_READY) return
        try {
            val result = SmsCardView.render(d, message)
            val ok = result.getOrThrow()
            Log.d(TAG, "card sent to glasses: ok=$ok sender=${message.sender}")
        } catch (e: Throwable) {
            Log.e(TAG, "sendContent failed: ${e.message}", e)
        }
    }

    /** Non-suspend helper that pushes a sample SMS to the glasses for visibility testing. */
    fun sendTestCard() {
        val d = display ?: return
        if (_connectionState.value != ConnectionState.DISPLAY_READY) return
        scope.launch {
            val sample = SmsMessage(
                sender = "Test Sender",
                body = "The quick brown fox jumps over the lazy dog. 0123456789",
                timestamp = System.currentTimeMillis(),
            )
            try {
                val result = SmsCardView.render(d, sample)
                val ok = result.getOrThrow()
                Log.d(TAG, "test card sent to glasses: ok=$ok")
            } catch (e: Throwable) {
                Log.e(TAG, "test card failed: ${e.message}", e)
            }
        }
    }

    fun clearDisplay() {
        val d = display ?: return
        scope.launch {
            runCatching { d.clearDisplay() }
                .onFailure { Log.w(TAG, "clearDisplay failed", it) }
        }
    }

    private fun teardown() {
        val d = display
        val s = session
        session = null
        display = null
        scope.launch {
            d?.let { runCatching { it.clearDisplay() } }
            s?.let {
                runCatching { it.removeDisplay() }
                runCatching { it.stop() }
            }
        }
    }

    fun shutdown() {
        teardown()
    }
}
