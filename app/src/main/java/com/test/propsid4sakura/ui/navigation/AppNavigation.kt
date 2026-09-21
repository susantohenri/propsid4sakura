package com.test.propsid4sakura.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.test.propsid4sakura.ads.AdManager
import com.test.propsid4sakura.ads.ConsentManager
import com.test.propsid4sakura.ui.detail.DetailScreen
import com.test.propsid4sakura.ui.favorites.FavoritesScreen
import com.test.propsid4sakura.ui.home.HomeScreen
import com.test.propsid4sakura.ui.settings.SettingsScreen

@Composable
fun AppNavigation(
    adManager: AdManager,
    consentManager: ConsentManager
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onPropClick = { propId ->
                    navController.navigate(Screen.Detail.createRoute(propId))
                },
                onFavoritesClick = { navController.navigate(Screen.Favorites.route) },
                onSettingsClick = { navController.navigate(Screen.Settings.route) }
            )
        }

        composable(
            route = Screen.Detail.route,
            arguments = listOf(navArgument("propId") { type = NavType.StringType })
        ) { backStackEntry ->
            val propId = backStackEntry.arguments?.getString("propId") ?: return@composable
            DetailScreen(
                propId = propId,
                adManager = adManager,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Favorites.route) {
            FavoritesScreen(
                onPropClick = { propId ->
                    navController.navigate(Screen.Detail.createRoute(propId))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                consentManager = consentManager,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
