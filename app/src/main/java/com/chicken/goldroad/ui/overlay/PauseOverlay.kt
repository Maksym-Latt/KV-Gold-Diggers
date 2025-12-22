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
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.chicken.goldroad.R
import com.chicken.goldroad.ui.components.StrokedText
import com.chicken.goldroad.ui.components.WideActionButton

@Composable
fun PauseOverlay(
    musicEnabled: Boolean,
    soundEnabled: Boolean,
    onToggleMusic: () -> Unit,
    onToggleSound: () -> Unit,
    onResume: () -> Unit,
    onHome: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .background(Color(0xFFFFCC80), RoundedCornerShape(24.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StrokedText(text = "Pause", color = Color(0xFF1E5123), strokeColor = Color.White, strokeWidth = 5f)
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ToggleCircle(icon = Icons.Default.VolumeUp, enabled = soundEnabled, onToggle = onToggleSound)
                ToggleCircle(icon = Icons.Default.MusicNote, enabled = musicEnabled, onToggle = onToggleMusic)
            }
            Spacer(modifier = Modifier.height(8.dp))
            WideActionButton(
                text = "Resume",
                background = painterResource(id = R.drawable.btn_bg_green),
                onClick = onResume
            )
            WideActionButton(
                text = "Home",
                background = painterResource(id = R.drawable.btn_bg_red),
                onClick = onHome
            )
        }
    }
}

@Composable
private fun ToggleCircle(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onToggle: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(72.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.btn_bg_round),
            contentDescription = null,
            modifier = Modifier
                .size(72.dp)
                .background(Color.Transparent)
                .padding(0.dp)
                .clickable { onToggle() }
        )
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) Color.White else Color.LightGray,
            modifier = Modifier.size(32.dp)
        )
    }
}
