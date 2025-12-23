package com.chicken.goldroad.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chicken.goldroad.R

@Composable
fun WideActionButton(
    text: String,
    modifier: Modifier = Modifier,
    background: Painter = painterResource(id = R.drawable.btn_bg_green),
    icon: Painter? = null,
    contentColor: Color = Color.White,
    strokeColor: Color = Color(0xFF194221),
    height: Dp = 64.dp,
    onClick: () -> Unit
) {
    Surface(color = Color.Transparent, modifier = modifier.clickable(onClick = onClick)) {
        Box(contentAlignment = Alignment.Center) {
            Image(
                painter = background,
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().height(height)
            )
            Row(
                modifier = Modifier.padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon != null) {
                    Image(
                        painter = icon,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        colorFilter = ColorFilter.tint(contentColor)
                    )
                }
                StrokedText(text = text, color = contentColor, strokeColor = strokeColor)
            }
        }
    }
}

@Composable
fun RoundIconButton(
    modifier: Modifier = Modifier,
    icon: Painter,
    onClick: () -> Unit
) {
    Surface(color = Color.Transparent) {
        Box(
            modifier = modifier
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.btn_bg_round),
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.FillBounds
            )
            Icon(
                painter = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}
