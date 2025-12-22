package com.chicken.goldroad.ui.overlay

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.annotation.DrawableRes
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chicken.goldroad.R
import com.chicken.goldroad.ui.components.StrokedText
import com.chicken.goldroad.ui.components.WideActionButton

@Composable
fun ResultOverlay(
    title: String,
    rewardText: String,
    backgroundColor: Color,
    @DrawableRes centerImage: Int = R.drawable.basket_1,
    onPrimary: () -> Unit,
    onSecondary: () -> Unit,
    primaryLabel: String,
    secondaryLabel: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 42.dp, vertical = 64.dp)
                .background(backgroundColor, RoundedCornerShape(18.dp))
                .padding(vertical = 28.dp, horizontal = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                StrokedText(text = title, color = Color(0xFF1E5123), strokeColor = Color.White, fontSize = 28.sp)
                Image(
                    painter = painterResource(id = centerImage),
                    contentDescription = null,
                    modifier = Modifier.size(96.dp)
                )
                StrokedText(
                    text = rewardText,
                    color = Color.White,
                    strokeColor = Color(0xFF1E5123)
                )
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                WideActionButton(text = primaryLabel, background = painterResource(id = R.drawable.btn_bg_green), onClick = onPrimary)
                WideActionButton(text = secondaryLabel, background = painterResource(id = R.drawable.btn_bg_red), onClick = onSecondary)
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}
