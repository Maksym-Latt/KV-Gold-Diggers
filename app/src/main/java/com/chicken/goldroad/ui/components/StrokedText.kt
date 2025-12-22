package com.chicken.goldroad.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.chicken.goldroad.ui.theme.RubikFontFamily
import androidx.compose.material3.Text

@Composable
fun StrokedText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    strokeColor: Color = Color(0xFF215427),
    strokeWidth: Float = 6f,
    fontSize: TextUnit = 20.sp,
    fontWeight: FontWeight = FontWeight.ExtraBold,
    textAlign: TextAlign = TextAlign.Center
) {
    val baseStyle = TextStyle(
        fontFamily = RubikFontFamily,
        fontSize = fontSize,
        fontWeight = fontWeight,
        textAlign = textAlign
    )
    Box(modifier = modifier) {
        Text(
            text = text,
            style = baseStyle.copy(drawStyle = Stroke(width = strokeWidth)),
            color = strokeColor,
            modifier = Modifier
        )
        Text(
            text = text,
            style = baseStyle,
            color = color,
            modifier = Modifier
        )
    }
}
