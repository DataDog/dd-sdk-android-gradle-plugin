/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

import com.datadog.gradle.config.AndroidConfig

plugins {
    id("com.android.library")
    // Applied before Kotlin on purpose: ktlint-gradle registers Android source-set tasks twice
    // when it is applied after the Kotlin plugin.
    id("ktlint")
    kotlin("android")

    // Analysis tools
    alias(libs.plugins.detektGradlePlugin)

    id("datadogBuildConfig")
}

android {
    compileSdk = AndroidConfig.TARGET_SDK

    defaultConfig {
        minSdk = AndroidConfig.MIN_SDK
    }

    namespace = "com.datadog.example.lib"

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

datadogBuildConfig {
    applyKotlinConfig()
    applyDetektConfig()
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
}
