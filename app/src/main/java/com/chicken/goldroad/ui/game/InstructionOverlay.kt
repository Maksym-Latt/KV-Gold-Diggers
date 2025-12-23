package com.chicken.goldroad.ui.game

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chicken.goldroad.R
import com.chicken.goldroad.ui.components.StrokedText


@Composable
fun InstructionOverlay(isLoading: Boolean, onPlay: () -> Unit) {
    Box(
        modifier =
            Modifier.fillMaxSize()
                .background(Color(0xff422924)), // Semi-transparent dark brown
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.bg_ground_1),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier =
                Modifier.padding(24.dp)
                    .background(
                        Color(0xFF5D4037).copy(alpha = 0.9f),
                        RoundedCornerShape(24.dp)
                    )
                    .padding(32.dp)
        ) {
            StrokedText(text = "HOW TO PLAY", fontSize = 32.sp, color = Color(0xFFF7D26B))

            Spacer(modifier = Modifier.size(32.dp))

            InstructionItem(R.drawable.egg_3, "Collect eggs to reach target!")
            InstructionItem(R.drawable.ic_hand, "Dig tunnels to guide eggs!")

            Spacer(modifier = Modifier.size(48.dp))

            if (isLoading) {
                StrokedText(text = "INITIALIZING...", fontSize = 24.sp, color = Color.White)
            } else {
                androidx.compose.material3.Button(
                    onClick = onPlay,
                    colors =
                        androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF7D26B)
                        ),
                    modifier = Modifier.size(160.dp, 60.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                        StrokedText(text = "PLAY", fontSize = 24.sp, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun InstructionItem(iconRes: Int?, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth()
    ) {
        if (iconRes != null) {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.size(12.dp))
        }
        StrokedText(text = text, fontSize = 18.sp, color = Color.White)
    }
}
