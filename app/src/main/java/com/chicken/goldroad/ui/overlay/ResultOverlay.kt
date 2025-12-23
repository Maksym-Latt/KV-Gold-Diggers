package com.chicken.goldroad.ui.overlay

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chicken.goldroad.R
import com.chicken.goldroad.ui.components.SprayText
import com.chicken.goldroad.ui.components.StrokedText
import com.chicken.goldroad.ui.components.WideActionButton
import kotlin.math.max

@Composable
fun ResultOverlay(
    title: String,
    rewardText: String,
    outerGradient: List<Color>,
    panelColor: Color,
    @DrawableRes characterImage: Int,
    @DrawableRes basketImage: Int,
    onPrimary: () -> Unit,
    onSecondary: () -> Unit,
    primaryLabel: String,
    secondaryLabel: String,
    modifier: Modifier = Modifier
) {
    val edgeTint = outerGradient.firstOrNull() ?: Color(0xFF7EDB6C)
    val baseTop = soften(outerGradient.getOrNull(0) ?: edgeTint)
    val baseBottom = soften(outerGradient.getOrNull(1) ?: edgeTint)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(baseTop, baseBottom)))
            .softEdgeTint(edgeTint = edgeTint),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.78f)
                .fillMaxHeight(0.78f)
                .clip(RoundedCornerShape(22.dp))
                .background(panelColor)
                .padding(horizontal = 18.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(0.10f))

            SprayText(
                text = title,
                fontSize = 46.sp
            )

            Spacer(Modifier.height(10.dp))

            RewardPill(
                text = rewardText,
                modifier = Modifier
                    .height(44.dp)
                    .fillMaxWidth(0.46f)
            )

            Spacer(Modifier.weight(0.10f))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .fillMaxHeight(0.42f)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.12f),
                                    Color.Transparent
                                ),
                                radius = 520f
                            )
                        )
                )

                Image(
                    painter = painterResource(id = characterImage),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxHeight(0.78f)
                        .align(Alignment.TopCenter)
                )
            }

            Spacer(Modifier.height(14.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                WideActionButton(
                    text = primaryLabel,
                    background = painterResource(id = R.drawable.btn_bg_green),
                    onClick = onPrimary,
                    modifier = Modifier.fillMaxWidth(0.78f)
                )

                Spacer(Modifier.height(12.dp))

                WideActionButton(
                    text = secondaryLabel,
                    background = painterResource(id = R.drawable.btn_bg_red),
                    onClick = onSecondary,
                    modifier = Modifier.fillMaxWidth(0.78f)
                )
            }

            Spacer(Modifier.weight(0.08f))
        }
    }
}

private fun soften(c: Color): Color = lerp(c, Color.White, 0.22f)

private fun Modifier.softEdgeTint(edgeTint: Color): Modifier = this.then(
    Modifier.drawWithCache {
        val radius = max(size.width, size.height) * 0.95f
        val edge = edgeTint.copy(alpha = 0.28f)
        val dark = Color.Black.copy(alpha = 0.06f)

        val colorVignette = Brush.radialGradient(
            colors = listOf(Color.Transparent, edge),
            radius = radius
        )

        val darkVignette = Brush.radialGradient(
            colors = listOf(Color.Transparent, dark),
            radius = radius * 1.05f
        )

        onDrawBehind {
            drawRect(colorVignette)
            drawRect(darkVignette)
        }
    }
)

@Composable
private fun RewardPill(
    text: String,
    modifier: Modifier = Modifier,
    background: Painter = painterResource(id = R.drawable.btn_bg_green)
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Image(
            painter = background,
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize()
        )
        StrokedText(
            text =
                text,
            color = Color.White,
            strokeColor = Color(0xFF215427),
            strokeWidth = 6f,
            fontSize = 22.sp
        )
    }
}