package androidx.navigation.compose

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController

/**
 * This is a fake function of Android [androidx.navigation.compose.rememberNavController] for compilation testing purpose
 */
@Composable
public fun rememberNavController(): NavHostController {
    return NavHostController()
}
