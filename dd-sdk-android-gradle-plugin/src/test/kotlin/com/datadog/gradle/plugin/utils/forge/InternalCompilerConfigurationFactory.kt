package com.datadog.gradle.plugin.utils.forge

import com.datadog.gradle.plugin.InstrumentationMode
import com.datadog.gradle.plugin.kcp.InternalCompilerConfiguration
import fr.xgouchet.elmyr.Forge
import fr.xgouchet.elmyr.ForgeryFactory

internal class InternalCompilerConfigurationFactory : ForgeryFactory<InternalCompilerConfiguration> {
    override fun getForgery(forge: Forge): InternalCompilerConfiguration {
        return InternalCompilerConfiguration(
            trackActions = forge.aValueFrom(InstrumentationMode::class.java),
            trackViews = forge.aValueFrom(InstrumentationMode::class.java),
            recordImages = forge.aValueFrom(InstrumentationMode::class.java)
        )
    }
}
