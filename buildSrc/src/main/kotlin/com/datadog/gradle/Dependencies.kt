/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.gradle

object Dependencies {

    object Versions {
        // Commons
        const val Kotlin = "1.4.20"
        const val OkHttp = "3.12.6"

        // Android
        const val AndroidToolsPlugin = "4.1.2"

        // JUnit
        const val JUnitJupiter = "5.6.2"
        const val JUnitPlatform = "1.6.2"
        const val JUnitVintage = "5.6.2"
        const val JunitMockitoExt = "3.4.6"

        // Tests Tools
        const val AssertJ = "0.2.1"
        const val Elmyr = "1.2.0"
        const val Jacoco = "0.8.4"
        const val MockitoKotlin = "2.2.0"

        // Tools
        const val Detekt = "1.6.0"
        const val KtLint = "9.4.0"
        const val Dokka = "1.4.10"
    }

    object Libraries {

        const val Kotlin = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:${Versions.Kotlin}"
        const val KotlinReflect = "org.jetbrains.kotlin:kotlin-reflect:${Versions.Kotlin}"

        const val OkHttp = "com.squareup.okhttp3:okhttp:${Versions.OkHttp}"

        @JvmField
        val JUnit5 = arrayOf(
            "org.junit.platform:junit-platform-launcher:${Versions.JUnitPlatform}",
            "org.junit.vintage:junit-vintage-engine:${Versions.JUnitVintage}",
            "org.junit.jupiter:junit-jupiter:${Versions.JUnitJupiter}",
            "org.mockito:mockito-junit-jupiter:${Versions.JunitMockitoExt}"
        )

        @JvmField
        val TestTools = arrayOf(
            "net.wuerl.kotlin:assertj-core-kotlin:${Versions.AssertJ}",
            "com.github.xgouchet.Elmyr:core:${Versions.Elmyr}",
            "com.github.xgouchet.Elmyr:inject:${Versions.Elmyr}",
            "com.github.xgouchet.Elmyr:junit5:${Versions.Elmyr}",
            "com.github.xgouchet.Elmyr:jvm:${Versions.Elmyr}",
            "com.nhaarman.mockitokotlin2:mockito-kotlin:${Versions.MockitoKotlin}"
        )

        // Tools
        const val DetektCli = "io.gitlab.arturbosch.detekt:detekt-cli:${Versions.Detekt}"
        const val OkHttpMock = "com.squareup.okhttp3:mockwebserver:${Versions.OkHttp}"
    }

    object ClassPaths {
        const val AndroidTools = "com.android.tools.build:gradle:${Versions.AndroidToolsPlugin}"
        const val Kotlin = "org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.Kotlin}"
        const val KtLint = "org.jlleitschuh.gradle:ktlint-gradle:${Versions.KtLint}"
        const val Dokka = "org.jetbrains.dokka:dokka-gradle-plugin:${Versions.Dokka}"
    }

    object Repositories {
        const val Gradle = "https://plugins.gradle.org/m2/"
        const val Jitpack = "https://jitpack.io"
    }
}
