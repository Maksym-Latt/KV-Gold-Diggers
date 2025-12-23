package com.chicken.goldroad.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.chicken.goldroad.ui.game.GameScreen
import com.chicken.goldroad.ui.menu.MenuScreen
import com.chicken.goldroad.ui.PlayerViewModel
import com.chicken.goldroad.ui.shop.ShopScreen
import com.chicken.goldroad.ui.splash.SplashScreen
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun AppRootNavigation() {
    val navController = rememberNavController()
    val playerViewModel: PlayerViewModel = hiltViewModel()
    val playerState by playerViewModel.uiState.collectAsState()

    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") {
            SplashScreen {
                navController.navigate("menu") {
                    popUpTo("splash") { inclusive = true }
                }
            }
        }
        composable("menu") {
            MenuScreen(
                playerPreferences = playerState,
                onPlayClick = { navController.navigate("game") },
                onShopClick = { navController.navigate("shop") },
                onToggleMusic = playerViewModel::setMusicEnabled,
                onToggleSound = playerViewModel::setSoundEnabled,
                startMusic = playerViewModel::playMenuMusic
            )
        }
        composable("shop") {
            ShopScreen(
                playerPreferences = playerState,
                onBack = { navController.popBackStack() },
                onEquip = playerViewModel::selectBasket,
                onBuy = playerViewModel::buyAndEquip
            )
        }
        composable("game") {
            GameScreen(
                playerPreferences = playerState,
                playerViewModel = playerViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
