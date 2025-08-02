package com.focusfloat.app

import android.app.Application

class FocusFloatApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        container.pauseNotificationController.ensureChannels()
        container.distractingNotificationController.ensureChannels()
    }
}
