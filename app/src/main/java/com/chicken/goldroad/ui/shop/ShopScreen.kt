package com.chicken.goldroad.ui.shop

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.chicken.goldroad.R
import com.chicken.goldroad.data.PlayerPreferences
import com.chicken.goldroad.domain.model.BasketType
import com.chicken.goldroad.ui.components.CoinPill
import com.chicken.goldroad.ui.components.RoundIconButton
import com.chicken.goldroad.ui.components.SprayText
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
            .background(Brush.verticalGradient(listOf(Color(0xFF88D86F), Color(0xFFF9E07F))))
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
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0x8088d86f),
                            Color(0x80f9e07f)
                        )
                    )
                )
        )


        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(10.dp))

            ShopTopBar(
                coins = playerPreferences.coins,
                onHomeClick = onBack
            )

            Spacer(Modifier.height(14.dp))

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                SprayText(
                    text = "Shop",
                    fontSize = 48.sp
                )
            }

            Spacer(Modifier.height(14.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                verticalArrangement = Arrangement.spacedBy(44.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(baskets) { basket ->
                    BasketCardLikeScreenshot(
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
private fun ShopTopBar(
    coins: Int,
    onHomeClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CoinPill(
            coins = coins,
            background = painterResource(id = R.drawable.btn_bg_green),
            modifier = Modifier
        )

        Spacer(Modifier.weight(1f))

        RoundIconButton(
            icon = rememberVectorPainter(Icons.Default.Home),
            modifier = Modifier.size(54.dp),
            onClick = onHomeClick
        )
    }
}

@Composable
private fun BasketCardLikeScreenshot(
    basket: BasketType,
    owned: Boolean,
    equipped: Boolean,
    onEquip: () -> Unit,
    onBuy: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xffd39519))
                .padding(bottom = 42.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xffebb236))
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                StrokedText(
                    text = basket.title,
                    color = Color.White,
                    strokeColor = Color(0xFF215427),
                    strokeWidth = 6f
                )
            }

            Spacer(Modifier.height(10.dp))

            Image(
                painter = painterResource(id = basket.imageRes),
                contentDescription = null,
                modifier = Modifier.size(122.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(Modifier.height(10.dp))
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = 16.dp)
                .zIndex(1f)
        ) {
            when {
                equipped -> {
                    WideActionButton(
                        text = "Equipped",
                        background = painterResource(id = R.drawable.buy_non_active_btn_back),
                        onClick = {},
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                owned -> {
                    WideActionButton(
                        text = "Equip",
                        background = painterResource(id = R.drawable.buy_active_btn_back),
                        onClick = onEquip,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                else -> {
                    PriceButtonLikeScreenshot(
                        price = basket.price ?: 0,
                        onClick = onBuy,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}


@Composable
private fun PriceButtonLikeScreenshot(
    price: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(56.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.btn_bg_green),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize()
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_coin),
                contentDescription = null,
                modifier = Modifier.size(30.dp)
            )
            StrokedText(
                text = price.toString(),
                color = Color.White,
                strokeColor = Color(0xFF215427),
                strokeWidth = 5f
            )
        }
    }
}
