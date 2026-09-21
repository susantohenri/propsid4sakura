package com.test.propsid4sakura.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Favorites : Screen("favorites")
    object Settings : Screen("settings")
    object Detail : Screen("detail/{propId}") {
        fun createRoute(propId: String) = "detail/$propId"
    }
}
