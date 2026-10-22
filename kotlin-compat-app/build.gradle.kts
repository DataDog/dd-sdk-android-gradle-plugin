/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

import com.datadog.gradle.plugin.DdExtension
import com.datadog.gradle.plugin.InstrumentationMode
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.android.application)
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.datadoghq.dd-sdk-android-gradle-plugin") apply false
}

val datadogEnabled = providers.gradleProperty("disableDatadog").orNull != "true"

if (datadogEnabled) {
    apply(plugin = "com.datadoghq.dd-sdk-android-gradle-plugin")
    extensions.configure<DdExtension> {
        composeInstrumentation = InstrumentationMode.AUTO
    }
}

android {
    namespace = "com.datadog.kotlincompat"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.datadog.kotlincompat"
        minSdk = 36
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.datadog.rum)
    implementation(libs.datadog.compose)
}

// Symbols injected by the Datadog compiler plugin into the compiled classes.
val instrumentationSymbols = listOf("instrumentedDatadog", "InstrumentedNavigationViewTrackingEffect")

tasks.register("verifyKotlinCompat") {
    description = "Checks the requested Kotlin version is used and that the Datadog compiler plugin ran."
    val expectedKotlinVersion = providers.gradleProperty("kotlinVersion").get()
    val actualKotlinVersion = getKotlinPluginVersion()
    val checkInstrumentation = datadogEnabled
    val classesDir = tasks.named<KotlinCompile>("compileDebugKotlin").flatMap { it.destinationDirectory }
    inputs.dir(classesDir)

    doLast {
        check(actualKotlinVersion == expectedKotlinVersion) {
            "Kotlin Gradle plugin version is $actualKotlinVersion but -PkotlinVersion=$expectedKotlinVersion"
        }
        if (checkInstrumentation) {
            val classesContent = classesDir.get().asFileTree
                .filter { it.name.endsWith(".class") }
                .joinToString(separator = "\n") { String(it.readBytes(), Charsets.ISO_8859_1) }
            val missing = instrumentationSymbols.filterNot { classesContent.contains(it) }
            check(missing.isEmpty()) {
                "Datadog compiler plugin did not instrument the app, missing symbols: $missing"
            }
        }
        logger.lifecycle(
            "Kotlin $actualKotlinVersion OK" + if (checkInstrumentation) ", Datadog instrumentation found" else ""
        )
    }
}
