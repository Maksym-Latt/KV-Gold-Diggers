package com.chicken.goldroad.ui.shop

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chicken.goldroad.R
import com.chicken.goldroad.data.PlayerPreferences
import com.chicken.goldroad.domain.model.BasketType
import com.chicken.goldroad.ui.components.RoundIconButton
import com.chicken.goldroad.ui.components.StrokedText
import com.chicken.goldroad.ui.components.WideActionButton

@Composable
fun ShopScreen(
    playerPreferences: PlayerPreferences,
    onBack: () -> Unit,
    onEquip: (BasketType) -> Unit,
    onBuy: (BasketType) -> Unit
) {
    val baskets = BasketType.values().toList()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Color(0xFFFFD976), Color(0xFFFFEAA5)))
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                RoundIconButton(icon = rememberVectorPainter(Icons.Default.ArrowBack), modifier = Modifier.size(64.dp)) {
                    onBack()
                }
                StrokedText(text = "Shop", color = Color(0xFF1E5123), strokeColor = Color.White, fontSize = 26.sp)
                CoinBadge(coins = playerPreferences.coins)
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(baskets) { basket ->
                    BasketCard(
                        basket = basket,
                        owned = basket.id in playerPreferences.ownedBaskets,
                        equipped = basket.id == playerPreferences.selectedBasketId,
                        onEquip = { onEquip(basket) },
                        onBuy = { onBuy(basket) }
                    )
                }
            }
        }
    }
}

@Composable
private fun BasketCard(
    basket: BasketType,
    owned: Boolean,
    equipped: Boolean,
    onEquip: () -> Unit,
    onBuy: () -> Unit
) {
    Column(
        modifier = Modifier
            .background(Color(0xFFFFC85A), RoundedCornerShape(16.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFE89F3A), RoundedCornerShape(12.dp))
                .padding(vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            StrokedText(text = basket.title, color = Color(0xFF1E5123), strokeColor = Color.White)
        }
        Image(
            painter = painterResource(id = basket.imageRes),
            contentDescription = null,
            modifier = Modifier.size(120.dp)
        )
        if (!owned && basket.price != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.egg_1),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                StrokedText(text = "${basket.price}", color = Color.White, strokeColor = Color.Black, strokeWidth = 4f)
            }
        }
        val buttonText = when {
            equipped -> "Equipped"
            owned -> "Equip"
            else -> "Buy"
        }
        val buttonAction = when {
            equipped -> ({})
            owned -> onEquip
            else -> onBuy
        }
        WideActionButton(
            text = buttonText,
            background = painterResource(id = if (equipped) R.drawable.btn_bg_green else R.drawable.btn_bg_red),
            onClick = buttonAction
        )
    }
}

@Composable
private fun CoinBadge(coins: Int) {
    Row(
        modifier = Modifier
            .background(Color(0xFF2C7433), RoundedCornerShape(50))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Image(painter = painterResource(id = R.drawable.egg_1), contentDescription = null, modifier = Modifier.size(24.dp))
        StrokedText(text = coins.toString(), color = Color.White, strokeColor = Color.Black, strokeWidth = 4f)
    }
}
