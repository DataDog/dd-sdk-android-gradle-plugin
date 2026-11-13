/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

buildscript {
    val pluginClasspath = providers.gradleProperty("pluginClasspath").get()
    val kotlinVersion = providers.gradleProperty("kotlinVersion").get()

    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    dependencies {
        // Keep Datadog, AGP and Kotlin in the same buildscript classloader.
        // TestKit's withPluginClasspath() isolates Datadog from the other plugins:
        // https://github.com/gradle/gradle/issues/22466
        classpath(files(pluginClasspath.split(",")))
        classpath("com.android.tools.build:gradle:9.3.1")
        classpath("org.jetbrains.kotlin:compose-compiler-gradle-plugin:$kotlinVersion")
    }
}

allprojects {
    repositories {
        maven { url = rootProject.uri("compiler-plugins") }
        google()
        mavenCentral()
    }
}
