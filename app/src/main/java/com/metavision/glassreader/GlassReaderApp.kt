package com.metavision.glassreader

import android.app.Application

class GlassReaderApp : Application() {
    override fun onCreate() {
        super.onCreate()
        GlassReaderState.initialize(this)
    }

    override fun onTerminate() {
        super.onTerminate()
        GlassReaderState.shutdown()
    }
}
