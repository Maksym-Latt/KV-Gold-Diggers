package com.chicken.goldroad.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chicken.goldroad.R
import com.chicken.goldroad.ui.theme.RubikFontFamily
import java.time.format.TextStyle

@Composable
fun SprayText(
    text: String,
    modifier: Modifier = Modifier,

    fontSize: TextUnit = 44.sp,

    fillColor: Color = Color(0xFF2F6B2F),
    sprayColor: Color = Color.White,

    sprayOffsetX: Dp = (-3).dp,
    sprayOffsetY: Dp = 2.dp,

    fontWeight: FontWeight = FontWeight.ExtraBold,
    textAlign: TextAlign = TextAlign.Center
) {
    val sprayFontFamily = FontFamily(
        Font(R.font.rubik_spray_paint, FontWeight.Normal),
        Font(R.font.rubik_spray_paint, FontWeight.Bold)
    )

    val baseStyle = androidx.compose.ui.text.TextStyle(
        fontFamily = sprayFontFamily,
        fontSize = fontSize,
        fontWeight = fontWeight,
        textAlign = textAlign
    )

    Box(modifier = modifier) {
        Text(
            text = text,
            style = baseStyle,
            color = sprayColor,
            modifier = Modifier.offset(
                x = sprayOffsetX,
                y = sprayOffsetY
            )
        )

        Text(
            text = text,
            style = baseStyle,
            color = fillColor
        )
    }
}
