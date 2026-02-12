/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin.kcp

import com.datadog.gradle.plugin.DdExtension
import com.datadog.gradle.plugin.InstrumentationMode
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion

/**
 * Support class for configuring the Datadog Kotlin Compiler Plugin based on the Kotlin version.
 * This class implements [KotlinCompilerPluginSupportPlugin] to properly integrate with the
 * Kotlin Gradle Plugin and target the correct version-specific subplugin.
 *
 * The plugin points to different subplugin based on kotlin compiler version (kotlin20, kotlin21, kotlin22).
 *
 */
class DatadogKotlinCompilerPluginSupport : KotlinCompilerPluginSupportPlugin {

    private lateinit var kgpVersion: String
    private lateinit var kotlinVersion: KotlinVersion

    @Suppress("ReturnCount")
    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean {
        val project = kotlinCompilation.target.project
        kgpVersion = project.getKotlinPluginVersion()
        kotlinVersion = KotlinVersion.from(kgpVersion)
        if (kotlinVersion == KotlinVersion.UNSUPPORTED) {
            LOGGER.debug("Unsupported Kotlin version for the Datadog Jetpack Compose instrumentation: \$kgpVersion\"")
            return false
        }
        val ddExtension = project.extensions.findByType(DdExtension::class.java) ?: return false

        if (ddExtension.composeInstrumentation == InstrumentationMode.DISABLE) {
            LOGGER.debug("Datadog Jetpack Compose instrumentation is disabled")
            return false
        }

        return true
    }

    override fun applyToCompilation(
        kotlinCompilation: KotlinCompilation<*>
    ): Provider<List<SubpluginOption>> {
        val project = kotlinCompilation.target.project
        val ddExtension = project.extensions.getByType(DdExtension::class.java)
        val composeInstrumentation = ddExtension.composeInstrumentation
        return project.provider {
            listOf(
                SubpluginOption(
                    key = INSTRUMENTATION_MODE,
                    value = composeInstrumentation.name
                )
            )
        }
    }

    override fun getCompilerPluginId(): String = PLUGIN_ID

    override fun getPluginArtifact(): SubpluginArtifact {
        val artifactId = when (kotlinVersion) {
            KotlinVersion.KOTLIN19, KotlinVersion.KOTLIN20 -> "$PLUGIN_ARTIFACT_ID_PREFIX-kotlin20"
            KotlinVersion.KOTLIN21 -> "$PLUGIN_ARTIFACT_ID_PREFIX-kotlin21"
            KotlinVersion.KOTLIN22 -> "$PLUGIN_ARTIFACT_ID_PREFIX-kotlin22"
            KotlinVersion.UNSUPPORTED -> error("Unsupported Kotlin version: $kgpVersion")
        }
        return SubpluginArtifact(
            groupId = GROUP_ID,
            artifactId = artifactId,
            version = BuildConfig.DD_PLUGIN_VERSION
        )
    }

    companion object {
        private const val PLUGIN_ID = "com.datadoghq.kotlin.compiler"
        private const val GROUP_ID = "com.datadoghq"
        private const val PLUGIN_ARTIFACT_ID_PREFIX = "dd-sdk-android-gradle-plugin-kcp"
        private const val INSTRUMENTATION_MODE = "INSTRUMENTATION_MODE"
        private val LOGGER = Logging.getLogger("DatadogKotlinCompilerPluginSupport")
    }
}
