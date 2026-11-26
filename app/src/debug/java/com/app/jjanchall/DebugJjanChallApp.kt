package com.app.jjanchall

import android.app.Application
import com.app.jjanchall.ui.setting.getReportFirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

class DebugJjanChallApp : Application() {
    override fun onCreate() {
        super.onCreate()

        val debugProvider = DebugAppCheckProviderFactory.getInstance()

        FirebaseAppCheck.getInstance()
            .installAppCheckProviderFactory(debugProvider)

        val reportApp = getReportFirebaseApp(this)
        FirebaseAppCheck.getInstance(reportApp)
            .installAppCheckProviderFactory(debugProvider)
    }
}