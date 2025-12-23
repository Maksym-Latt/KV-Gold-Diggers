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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chicken.goldroad.R
import com.chicken.goldroad.ui.components.SprayText
import com.chicken.goldroad.ui.components.StrokedText
import com.chicken.goldroad.ui.components.WideActionButton
import kotlin.math.max

@Composable
private fun ScreenEdgeGlowOverlay(
    glowColor: Color,
    edgeSize: Dp,
    intensity: Float,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val edgePx = with(density) { edgeSize.toPx() }.coerceAtLeast(1f)
    val safeIntensity = intensity.coerceIn(0f, 1f)

    val edgeStrong = glowColor.copy(alpha = 0.55f * safeIntensity)
    val edgeSoft = glowColor.copy(alpha = 0.28f * safeIntensity)

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawWithCache {
                val w = size.width
                val h = size.height

                val radial = Brush.radialGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Transparent,
                        edgeSoft,
                        edgeStrong
                    ),
                    center = Offset(w / 2f, h / 2f),
                    radius = maxOf(w, h) * 0.72f
                )

                val top = Brush.verticalGradient(
                    0f to edgeStrong,
                    1f to Color.Transparent,
                    startY = 0f,
                    endY = edgePx
                )
                val bottom = Brush.verticalGradient(
                    0f to Color.Transparent,
                    1f to edgeStrong,
                    startY = h - edgePx,
                    endY = h
                )
                val left = Brush.horizontalGradient(
                    0f to edgeStrong,
                    1f to Color.Transparent,
                    startX = 0f,
                    endX = edgePx
                )
                val right = Brush.horizontalGradient(
                    0f to Color.Transparent,
                    1f to edgeStrong,
                    startX = w - edgePx,
                    endX = w
                )

                onDrawWithContent {
                    drawContent()

                    drawRect(brush = radial, blendMode = BlendMode.SrcOver)
                    drawRect(brush = top, blendMode = BlendMode.SrcOver)
                    drawRect(brush = bottom, blendMode = BlendMode.SrcOver)
                    drawRect(brush = left, blendMode = BlendMode.SrcOver)
                    drawRect(brush = right, blendMode = BlendMode.SrcOver)
                }
            }
    )
}

@Composable
fun ResultOverlay(
    title: String,
    rewardText: String,
    outerGradient: List<Color>,
    panelColor: Color,
    @DrawableRes characterImage: Int,
    onPrimary: () -> Unit,
    onSecondary: () -> Unit,
    primaryLabel: String,
    secondaryLabel: String,
    modifier: Modifier = Modifier
) {
    val centerColor = outerGradient.firstOrNull() ?: Color(0xFFECCF2A)
    val glowColor = outerGradient.getOrNull(1) ?: centerColor

    val bgTop = lerp(centerColor, Color.White, 0.14f)
    val bgBottom = lerp(centerColor, Color.Black, 0.05f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(bgTop, bgBottom)))
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .clip(RoundedCornerShape(22.dp))
                .background(panelColor)
                .padding(horizontal = 18.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(0.30f))

            SprayText(text = title, fontSize = 46.sp)

            Spacer(Modifier.height(10.dp))

            RewardPill(
                text = rewardText,
                modifier = Modifier
                    .height(44.dp)
                    .fillMaxWidth(0.4f)
            )

            Spacer(Modifier.weight(0.10f))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = characterImage),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxHeight(0.9f)
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

            Spacer(Modifier.weight(0.3f))
        }

        ScreenEdgeGlowOverlay(
            glowColor = glowColor,
            edgeSize = 2.dp,
            intensity = 0.6f,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
private fun RewardPill(
    text: String,
    modifier: Modifier = Modifier,
    background: androidx.compose.ui.graphics.painter.Painter = painterResource(id = R.drawable.btn_bg_green)
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        androidx.compose.foundation.Image(
            painter = background,
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize()
        )
        StrokedText(
            text = text,
            color = Color.White,
            strokeColor = Color(0xFF215427),
            strokeWidth = 6f,
            fontSize = 22.sp
        )
    }
}