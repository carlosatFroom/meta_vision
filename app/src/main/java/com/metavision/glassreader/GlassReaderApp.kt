package com.metavision.glassreader

import android.app.Application
import com.meta.wearable.dat.core.Wearables

class GlassReaderApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Wearables.initialize(this)
        GlassReaderState.initialize(this)
        DisplaySessionManager.initialize(this)
        CardSettingsHolder.load(this)
        NotificationSourceStore.load(this)
    }

    override fun onTerminate() {
        super.onTerminate()
        GlassReaderState.shutdown()
        DisplaySessionManager.shutdown()
    }
}
