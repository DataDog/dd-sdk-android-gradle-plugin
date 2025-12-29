/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin

import org.gradle.api.JavaVersion

/**
 * Constants for Kotlin 2.3.0 compatibility testing.
 */
internal object Kotlin23TestConstants {

    // Minimum versions required for Kotlin 2.3.0 support
    private const val GRADLE_VERSION_FOR_KOTLIN_23 = "9.0.0"
    private const val AGP_VERSION_FOR_KOTLIN_23 = "8.13.0"

    /**
     * Configuration for testing Kotlin 2.3.0 compatibility.
     */
    val KOTLIN_2_3_TEST_CONFIGURATION = DdAndroidGradlePluginFunctionalTest.Companion.BuildVersionConfig(
        agpVersion = AGP_VERSION_FOR_KOTLIN_23,
        gradleVersion = GRADLE_VERSION_FOR_KOTLIN_23,
        buildToolsVersion = "36.0.0",
        targetSdkVersion = "36",
        kotlinVersion = "2.3.0",
        jvmTarget = JavaVersion.VERSION_17.toString()
    )

    /**
     * Root build file content for Compose tests (requires Kotlin 2.0+).
     */
    const val ROOT_BUILD_FILE_CONTENT_WITH_COMPOSE = """
        buildscript {
            ext {
                targetSdkVersion = targetSdkVersion as Integer
                buildToolsVersion = buildToolsVersion
                datadogSdkDependency = "com.datadoghq:dd-sdk-android-rum:3.4.0"
                jvmTarget = jvmTarget
            }
            repositories {
                gradlePluginPortal()
                google()
            }

            dependencies {
                classpath files(*pluginClasspath.split(","))
                classpath dependencies.create("com.android.tools.build:gradle:${"$"}agpVersion")
                classpath dependencies.create("org.jetbrains.kotlin:kotlin-gradle-plugin:${"$"}kotlinVersion")
                classpath dependencies.create("org.jetbrains.kotlin:compose-compiler-gradle-plugin:${"$"}kotlinVersion")
            }
        }
        allprojects {
            repositories {
                gradlePluginPortal()
                google()
            }
        }
    """

    /**
     * Compose source file with NavHost for testing Compose instrumentation.
     */
    val COMPOSE_NAVHOST_SOURCE_CONTENT = """
        package com.datadog.android.sample

        import android.os.Bundle
        import androidx.activity.ComponentActivity
        import androidx.activity.compose.setContent
        import androidx.compose.foundation.layout.Column
        import androidx.compose.material3.Button
        import androidx.compose.material3.Text
        import androidx.compose.runtime.Composable
        import androidx.navigation.compose.NavHost
        import androidx.navigation.compose.composable
        import androidx.navigation.compose.rememberNavController

        class MainActivity : ComponentActivity() {
            override fun onCreate(savedInstanceState: Bundle?) {
                super.onCreate(savedInstanceState)
                setContent {
                    MainScreen()
                }
            }
        }

        @Composable
        fun MainScreen() {
            val navController = rememberNavController()
            NavHost(navController = navController, startDestination = "home") {
                composable("home") { HomeScreen() }
                composable("detail") { DetailScreen() }
            }
        }

        @Composable
        fun HomeScreen() {
            Column {
                Text("Home Screen")
                Button(onClick = { }) {
                    Text("Go to Detail")
                }
            }
        }

        @Composable
        fun DetailScreen() {
            Column {
                Text("Detail Screen")
                Button(onClick = { }) {
                    Text("Go Back")
                }
            }
        }
    """.trimIndent()
}
