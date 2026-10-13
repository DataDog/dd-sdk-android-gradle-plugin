/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

@file:Suppress("StringLiteralDuplication")

import com.datadog.gradle.config.AndroidConfig

plugins {
    id("com.android.application")
    // Applied before Kotlin on purpose: ktlint-gradle registers Android source-set tasks twice
    // when it is applied after the Kotlin plugin.
    id("ktlint")
    kotlin("android")

    // Analysis tools
    alias(libs.plugins.detektGradlePlugin)

    id("datadogBuildConfig")

//    id("com.datadoghq.dd-sdk-android-gradle-plugin")
}

android {
    compileSdk = AndroidConfig.TARGET_SDK
    buildToolsVersion = AndroidConfig.BUILD_TOOLS_VERSION

    namespace = "com.datadog.example.variants"
    defaultConfig {
        applicationId = "com.datadog.example.variants.kotlin"

        minSdk = AndroidConfig.MIN_SDK
        targetSdk = AndroidConfig.TARGET_SDK
        versionCode = 1
        versionName = "1.0"
        multiDexEnabled = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    sourceSets.named("main") {
        java.srcDir("src/main/kotlin")
    }

    flavorDimensions.add("version")
    flavorDimensions.add("colour")
    productFlavors {
        register("demo") {
            dimension = "version"
            applicationIdSuffix = ".demo"
            versionNameSuffix = "-demo"
        }
        register("full") {
            dimension = "version"
            applicationIdSuffix = ".full"
            versionNameSuffix = "-full"
        }
        register("pro") {
            dimension = "version"
            applicationIdSuffix = ".pro"
            versionNameSuffix = "-pro"
        }

        register("red") {
            dimension = "colour"
        }
        register("green") {
            dimension = "colour"
        }
        register("blue") {
            dimension = "colour"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

datadogBuildConfig {
    applyKotlinConfig()
    // Colours don't have dedicated sources, analyzing one variant per version flavor is enough
    // (Detekt matches the combined flavor name of each variant)
    applyDetektConfig(
        ignoredFlavors = listOf("demoGreen", "demoBlue", "fullGreen", "fullBlue", "proGreen", "proBlue")
    )
}

dependencies {
    implementation(project(":samples:lib-module"))
    implementation(libs.kotlinRuntime23)

    implementation(libs.androidx.core)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
}

/*
datadog {
    site = "US"
    checkProjectDependencies = com.datadog.gradle.plugin.SdkCheckLevel.FAIL
    mappingFilePath = "path/to/mapping.txt"

    variants {
        register("demo") {
            site = "US3"
            versionName = "demo"
        }
        register("full") {
            versionName = "full"
        }
        register("pro") {
            versionName = "pro"
        }
    }
}
*/
