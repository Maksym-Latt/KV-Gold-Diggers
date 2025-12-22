package com.chicken.goldroad.ui.menu

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chicken.goldroad.R

@Composable
fun MenuScreen(onPlayClick: () -> Unit, onSettingsClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                    painter = painterResource(id = R.drawable.egg_1),
                    contentDescription = null,
                    modifier = Modifier.size(100.dp)
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = onPlayClick) { Text(text = "PLAY", fontSize = 24.sp) }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onSettingsClick) { Text(text = "SETTINGS", fontSize = 18.sp) }
        }
    }
}
