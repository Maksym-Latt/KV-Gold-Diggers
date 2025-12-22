package com.chicken.goldroad.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.chicken.goldroad.ui.menu.MenuScreen
import com.chicken.goldroad.ui.game.GameScreen

@Composable
fun AppRootNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "menu") {
        composable("menu") {
            MenuScreen(
                onPlayClick = { navController.navigate("game") },
                onSettingsClick = { /* TODO */ }
            )
        }
        composable("game") {
            GameScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
