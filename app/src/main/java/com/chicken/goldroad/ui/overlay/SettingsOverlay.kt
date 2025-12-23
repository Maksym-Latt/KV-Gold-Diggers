package com.chicken.goldroad.ui.overlay

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chicken.goldroad.R
import com.chicken.goldroad.ui.components.SprayText
import com.chicken.goldroad.ui.components.StrokedText
import com.chicken.goldroad.ui.components.WideActionButton

@Composable
fun SettingsOverlay(
    modifier: Modifier = Modifier,
    title: String = "Settings",
    musicEnabled: Boolean,
    soundEnabled: Boolean,
    onToggleMusic: () -> Unit,
    onToggleSound: () -> Unit,
    onClose: () -> Unit
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
                .fillMaxWidth(0.8f)
                .background(Color(0xffffbd43), RoundedCornerShape(24.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                SprayText(text = title)
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
                text = "Close",
                background = painterResource(id = R.drawable.btn_bg_red),
                onClick = onClose
            )
        }
    }
}

@Composable
fun SettingRow(
    label: String,
    icon: Painter,
    enabled: Boolean,
    indicator: androidx.compose.ui.graphics.vector.ImageVector,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        StrokedText(text = label, color = Color(0xffffffff), strokeColor = Color(0xFF1F4E22),fontSize = 32.sp)
        Box(contentAlignment = Alignment.Center) {
            Image(painter = icon, contentDescription = null, modifier = Modifier
                .size(56.dp)
                .clickable { onToggle() })
            Icon(
                imageVector = indicator,
                contentDescription = null,
                tint = if (enabled) Color.White else Color.LightGray,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}
