@file:Suppress("UnusedParameter")

package androidx.navigation.compose

import androidx.navigation.NavHostController

/**
 * This is a fake function of Android [androidx.navigation.compose.NavHost] for compilation testing purpose
 */
fun NavHost(
    navHostController: NavHostController,
    destination: String,
    builder: NavGraphBuilder.() -> Unit
) {
    // fake function
}

/**
 * This is a fake class of Android [androidx.navigation.compose.NavHost] for compilation testing purpose
 */
class NavHost
