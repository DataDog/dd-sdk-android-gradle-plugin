/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package androidx.navigation.compose

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController

/**
 * This is a fake function of Android [androidx.navigation.compose.rememberNavController] for compilation testing purpose.
 */
@Composable
public fun rememberNavController(): NavHostController {
    return NavHostController()
}
