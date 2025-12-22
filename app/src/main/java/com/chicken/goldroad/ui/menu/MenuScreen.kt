package com.chicken.goldroad.ui.menu

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.chicken.goldroad.R
import com.chicken.goldroad.data.PlayerPreferences
import com.chicken.goldroad.domain.model.BasketType
import com.chicken.goldroad.ui.components.RoundIconButton
import com.chicken.goldroad.ui.components.StrokedText
import com.chicken.goldroad.ui.components.WideActionButton
import com.chicken.goldroad.ui.overlay.SettingsOverlay

@Composable
fun MenuScreen(
    playerPreferences: PlayerPreferences,
    onPlayClick: () -> Unit,
    onShopClick: () -> Unit,
    onToggleMusic: (Boolean) -> Unit,
    onToggleSound: (Boolean) -> Unit,
    startMusic: () -> Unit
) {
    var showSettings by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { startMusic() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF83D260), Color(0xFFF7D26B))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CoinPill(coins = playerPreferences.coins)
                RoundIconButton(
                    icon = rememberVectorPainter(image = Icons.Default.Settings),
                    modifier = Modifier.size(64.dp)
                ) {
                    showSettings = true
                }
            }
            Image(
                painter = painterResource(id = R.drawable.title),
                contentDescription = null,
                modifier = Modifier
                    .padding(top = 12.dp)
                    .size(220.dp)
            )
            Image(
                painter = painterResource(id = BasketType.fromId(playerPreferences.selectedBasketId).imageRes),
                contentDescription = null,
                modifier = Modifier.size(160.dp)
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                WideActionButton(
                    text = "Play",
                    background = painterResource(id = R.drawable.btn_bg_green),
                    onClick = onPlayClick
                )
                WideActionButton(
                    text = "Shop",
                    icon = rememberVectorPainter(image = Icons.Default.ShoppingCart),
                    background = painterResource(id = R.drawable.btn_bg_red),
                    onClick = onShopClick
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
        if (showSettings) {
            SettingsOverlay(
                musicEnabled = playerPreferences.musicEnabled,
                soundEnabled = playerPreferences.soundEnabled,
                onToggleMusic = { onToggleMusic(!playerPreferences.musicEnabled) },
                onToggleSound = { onToggleSound(!playerPreferences.soundEnabled) },
                onClose = { showSettings = false },
                title = "Settings"
            )
        }
    }
}

@Composable
private fun CoinPill(coins: Int) {
    Row(
        modifier = Modifier
            .background(Color(0xFF2C7433), shape = androidx.compose.foundation.shape.RoundedCornerShape(50))
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.egg_1),
            contentDescription = null,
            modifier = Modifier.size(32.dp)
        )
        StrokedText(text = coins.toString(), color = Color.White, strokeColor = Color.Black, strokeWidth = 4f)
    }
}
