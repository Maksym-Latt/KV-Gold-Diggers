package com.chicken.goldroad.ui.components

import android.R.attr.strokeColor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chicken.goldroad.R

@Composable
fun CoinPill(
    coins: Int,
    modifier: Modifier = Modifier,
    background: Painter = painterResource(id = R.drawable.btn_bg_green)
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = background,
            contentDescription = null,
            modifier = Modifier.height(45.dp),
            contentScale = ContentScale.FillBounds,
        )

        Row(
            modifier = Modifier.padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StrokedText(
                text = coins.toString(),
                color = Color.White,
                fontSize = 26.sp,
                strokeColor = Color.Black,
                strokeWidth = 4f
            )
        }
    }
}
