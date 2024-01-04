package com.datadog.example.variants

import android.app.Application
import android.util.Log
import com.datadog.android.Datadog
import com.datadog.android.DatadogSite
import com.datadog.android.core.configuration.Configuration
import com.datadog.android.log.Logs
import com.datadog.android.log.LogsConfiguration
import com.datadog.android.privacy.TrackingConsent
import com.datadog.android.rum.Rum
import com.datadog.android.rum.RumConfiguration

class VariantsApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Datadog.setVerbosity(Log.DEBUG)
        Datadog.initialize(
            this, Configuration.Builder(
                clientToken = "",
                env = "prod",
                variant = BuildConfig.FLAVOR
            )
                .useSite(DatadogSite.STAGING)
                .build(),
            TrackingConsent.GRANTED
        )
        Logs.enable(LogsConfiguration.Builder().build())
        Rum.enable(RumConfiguration.Builder(applicationId = "").build())
    }
}