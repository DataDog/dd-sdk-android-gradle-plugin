/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.config

import org.gradle.api.Project
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.HasConfigurableKotlinCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinBaseExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

// Must be kept in sync with the JDK installed in ci/Dockerfile.gitlab,
// with gradle/gradle-daemon-jvm.properties, with tools/build-config/build.gradle.kts
// and with .github/workflows/codeql-analysis.yml.
private const val JDK_VERSION = 21

// Bytecode version of the published artifacts, independent of the JDK used to build them.
private const val JAVA_RELEASE = 17

internal fun Project.kotlinConfig(
    languageVersion: KotlinVersion,
    apiVersion: KotlinVersion,
    // will follow default set by plugin
    coreLibrariesVersion: String?
) {
    when (val kotlin = extensions.getByName("kotlin")) {
        is KotlinJvmExtension -> {
            kotlin.applyCommonConfig(languageVersion, apiVersion, coreLibrariesVersion)
            // Compile against the JAVA_RELEASE class library rather than the toolchain one,
            // so that APIs newer than JAVA_RELEASE can't be used.
            kotlin.compilerOptions {
                freeCompilerArgs.add("-Xjdk-release=$JAVA_RELEASE")
            }
            tasks.withType<JavaCompile>().configureEach {
                options.release.set(JAVA_RELEASE)
            }
        }

        // Android modules compile against android.jar, which is incompatible with
        // -Xjdk-release / --release, Java target is set by android.compileOptions instead.
        is KotlinAndroidExtension -> {
            kotlin.applyCommonConfig(languageVersion, apiVersion, coreLibrariesVersion)
        }

        else -> error("Unsupported Kotlin extension type: ${kotlin::class.java.name}")
    }
}

private fun <T> T.applyCommonConfig(
    languageVersion: KotlinVersion,
    apiVersion: KotlinVersion,
    coreLibrariesVersion: String?
) where T : KotlinBaseExtension, T : HasConfigurableKotlinCompilerOptions<KotlinJvmCompilerOptions> {
    if (coreLibrariesVersion != null) {
        this.coreLibrariesVersion = coreLibrariesVersion
    }
    jvmToolchain(JDK_VERSION)
    compilerOptions {
        this.languageVersion.set(languageVersion)
        this.apiVersion.set(apiVersion)
        jvmTarget.set(JvmTarget.fromTarget(JAVA_RELEASE.toString()))
    }
}
