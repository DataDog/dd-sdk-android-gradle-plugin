/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

@file:Suppress("unused")
@file:OptIn(org.jetbrains.kotlin.backend.common.extensions.FirIncompatiblePluginAPI::class)

package com.datadog.gradle.plugin.kcp

/**
 * Kotlin 2.3 is API-compatible with Kotlin 2.2 for Compose IR generation.
 * This typealias allows the plugin to support Kotlin 2.3 without code duplication.
 */
typealias ComposeNavHostExtension23 = ComposeNavHostExtension22
