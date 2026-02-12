/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

import com.datadog.gradle.config.AndroidConfig
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.datadog.android.instrumented"
    compileSdk = AndroidConfig.TARGET_SDK

    defaultConfig {
        applicationId = "com.datadog.android.instrumented"
        // Min SDK must be higher than 30 to make Android instrumented test support
        // space characters in function names
        minSdk = AndroidConfig.TARGET_SDK
        targetSdk = AndroidConfig.TARGET_SDK
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            merges += "META-INF/LICENSE.md"
            merges += "META-INF/LICENSE-notice.md"
        }
    }
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.addAll(
            listOf(
                "-P",
                "plugin:com.datadoghq.kotlin.compiler:INSTRUMENTATION_MODE=AUTO"
            )
        )
    }
}

dependencies {

    kotlinCompilerPluginClasspath(project(":dd-sdk-android-gradle-plugin-kcp-common"))
    kotlinCompilerPluginClasspath(project(":dd-sdk-android-gradle-plugin-kcp-kotlin21"))
    // We use kotlin21 because the kotlin compiler for this project is kotlin 2.1.x
    implementation(project(":samples:lib-module"))
    implementation(libs.androidx.core)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.datadogSdkComposeSnapshot)
    implementation(libs.datadogSdkRumSnapshot)
    implementation(libs.androidx.compose.navigation)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.bundles.jUnit5)
    androidTestImplementation(libs.bundles.testTools)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation(libs.androidx.compose.navigation)
    androidTestImplementation(libs.datadogSdkRumSnapshot)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
