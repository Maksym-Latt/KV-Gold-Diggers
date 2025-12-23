package com.chicken.goldroad.ui.menu

import android.R.attr.text
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.chicken.goldroad.R
import com.chicken.goldroad.data.PlayerPreferences
import com.chicken.goldroad.domain.model.BasketType
import com.chicken.goldroad.ui.components.CoinPill
import com.chicken.goldroad.ui.components.RoundIconButton
import com.chicken.goldroad.ui.components.SprayText
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
        Image(
            painter = painterResource(id = R.drawable.bg),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Image(
                painter = painterResource(id = R.drawable.menu_chicken),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 10.dp, bottom = 200.dp)
                    .fillMaxWidth(0.56f),
            )

            Image(
                painter = painterResource(id = R.drawable.menu_basket),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = 200.dp)
                    .fillMaxWidth(0.32f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CoinPill(coins = playerPreferences.coins)
                RoundIconButton(
                    icon = rememberVectorPainter(image = Icons.Default.Settings),
                    modifier = Modifier.size(56.dp)
                ) { showSettings = true }
            }

            Spacer(modifier = Modifier.weight(0.06f))

            SprayText(
                text = "Chicken Gold Diggers Road",
                modifier = Modifier
            )

            Spacer(modifier = Modifier.weight(0.42f))

            WideActionButton(
                text = "Shop",
                icon = rememberVectorPainter(image = Icons.Default.ShoppingCart),
                background = painterResource(id = R.drawable.btn_bg_green),
                onClick = onShopClick,
                modifier = Modifier.fillMaxWidth(0.55f)
            )
            Spacer(modifier = Modifier.weight(0.02f))
            WideActionButton(
                text = "Play",
                background = painterResource(id = R.drawable.btn_bg_green),
                onClick = onPlayClick
            )


            Spacer(modifier = Modifier.weight(0.16f))
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
