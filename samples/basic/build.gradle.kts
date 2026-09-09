/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

import com.datadog.gradle.config.AndroidConfig
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    // Applied before Kotlin on purpose: ktlint-gradle registers Android source-set tasks twice
    // when it is applied after the Kotlin plugin.
    id("ktlint")
    kotlin("android")
    // you don't need this in your project, it is just to be able to reference AndroidConfig class
    id("noopBuildConfigClasspath") apply false

    // id("com.datadoghq.dd-sdk-android-gradle-plugin")
}

android {
    compileSdk = AndroidConfig.TARGET_SDK
    buildToolsVersion = AndroidConfig.BUILD_TOOLS_VERSION

    defaultConfig {
        applicationId = "com.datadog.example.basic"
        minSdk = AndroidConfig.MIN_SDK
        targetSdk = AndroidConfig.TARGET_SDK
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    namespace = "com.datadog.example.basic"

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    implementation(project(":samples:lib-module"))
    implementation(libs.kotlin)

    implementation(libs.androidx.core)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)

    // implementation(libs.datadogSdkRum)
}

// datadog {
//    site = "US"
//    versionName = "prod"
// }
