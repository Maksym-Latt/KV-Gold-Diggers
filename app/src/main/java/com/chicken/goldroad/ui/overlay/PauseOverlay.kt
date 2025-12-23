package com.chicken.goldroad.ui.overlay

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chicken.goldroad.R
import com.chicken.goldroad.ui.components.SprayText
import com.chicken.goldroad.ui.components.StrokedText
import com.chicken.goldroad.ui.components.WideActionButton

@Composable
fun PauseOverlay(
    musicEnabled: Boolean,
    soundEnabled: Boolean,
    onToggleMusic: () -> Unit,
    onToggleSound: () -> Unit,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.86f)
                .background(Color(0xFFFFBD43), RoundedCornerShape(24.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                SprayText(text = "Pause", fontSize = 44.sp)
            }

            SettingRow(
                label = "Sound",
                icon = painterResource(id = R.drawable.btn_bg_round),
                enabled = soundEnabled,
                onToggle = onToggleSound,
                indicator = Icons.Default.VolumeUp
            )

            SettingRow(
                label = "Music",
                icon = painterResource(id = R.drawable.btn_bg_round),
                enabled = musicEnabled,
                onToggle = onToggleMusic,
                indicator = Icons.Default.MusicNote
            )

            WideActionButton(
                text = "Resume",
                background = painterResource(id = R.drawable.btn_bg_green),
                onClick = onResume
            )

            WideActionButton(
                text = "Restart",
                background = painterResource(id = R.drawable.btn_bg_red),
                onClick = onRestart
            )

            WideActionButton(
                text = "Menu",
                icon = rememberVectorPainter(image = Icons.Default.Home),
                background = painterResource(id = R.drawable.btn_bg_green),
                onClick = onHome
            )
        }
    }
}
