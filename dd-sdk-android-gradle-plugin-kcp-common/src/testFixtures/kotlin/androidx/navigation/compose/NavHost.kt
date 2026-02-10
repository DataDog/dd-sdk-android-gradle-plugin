/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

@file:Suppress("UnusedParameter", "PackageNaming")

package androidx.navigation.compose

import androidx.navigation.NavHostController

/**
 * This is a fake function of Android [NavHost] for compilation testing purpose.
 */
fun NavHost(
    navHostController: NavHostController,
    destination: String,
    builder: NavGraphBuilder.() -> Unit
) {
    // fake function
}

/**
 * This is a fake class of Android [NavHost] for compilation testing purpose.
 */
class NavHost
