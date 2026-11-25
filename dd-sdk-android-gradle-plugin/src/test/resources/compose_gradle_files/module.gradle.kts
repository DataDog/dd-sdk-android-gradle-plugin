/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

@file:Suppress("PackageDirectoryMismatch")

import com.android.build.api.dsl.CommonExtension
import com.datadog.gradle.plugin.DdExtension
import com.datadog.gradle.plugin.InstrumentationMode
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidExtension

if (providers.gradleProperty("datadogFirst").get().toBoolean()) {
    apply(plugin = "com.datadoghq.dd-sdk-android-gradle-plugin")
}
apply(plugin = if (project.name == "app") "com.android.application" else "com.android.library")
if (!providers.gradleProperty("android.builtInKotlin").get().toBoolean()) {
    apply(plugin = "org.jetbrains.kotlin.android")
}
apply(plugin = "org.jetbrains.kotlin.plugin.compose")
apply(plugin = "com.datadoghq.dd-sdk-android-gradle-plugin")

configure<CommonExtension> {
    namespace = "com.datadog.test.${project.name}"
    compileSdk = 36
    defaultConfig.minSdk = 24
    compileOptions.apply {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures.compose = true
}
configure<KotlinAndroidExtension> {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}
configure<DdExtension> {
    composeInstrumentation = InstrumentationMode.valueOf(
        providers.gradleProperty("instrumentationMode").getOrElse("AUTO")
    )
}

dependencies {
    "implementation"(platform("androidx.compose:compose-bom:2024.04.01"))
    "implementation"("androidx.compose.material3:material3")
    "implementation"("androidx.navigation:navigation-compose:2.8.8")
    "implementation"("com.datadoghq:dd-sdk-android-compose:3.15.0")
    "implementation"("com.datadoghq:dd-sdk-android-rum:3.15.0")
}
