/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

pluginManagement {
    repositories {
        // Magic Mirror Depot proxy (only set in CI via `.gitlab-ci.yml`).
        listOf("gradlePluginProxy", "mavenRepositoryProxy")
            .mapNotNull { providers.gradleProperty(it).orNull?.takeIf { url -> url.isNotBlank() } }
            .forEach { url -> maven(url) }
        google()
        gradlePluginPortal()
        mavenCentral()
        maven {
            setUrl("https://jitpack.io")
            mavenContent {
                includeGroupByRegex("com\\.github\\..*")
            }
        }
    }
    includeBuild("./tools/build-config")
}

include(":dd-sdk-android-gradle-plugin")

include(":dd-sdk-android-gradle-plugin-kcp-common")
include(":dd-sdk-android-gradle-plugin-kcp-kotlin20")
include(":dd-sdk-android-gradle-plugin-kcp-kotlin21")
include(":dd-sdk-android-gradle-plugin-kcp-kotlin22")
include(":dd-sdk-android-gradle-plugin-kcp-kotlin24")

include(":samples:basic")
include(":samples:ndk")
include(":samples:variants")
include(":samples:variants-kotlin")
include(":samples:lib-module")
include(":instrumented")
