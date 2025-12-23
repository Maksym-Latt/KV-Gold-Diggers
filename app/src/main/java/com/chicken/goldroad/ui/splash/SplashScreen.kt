package com.chicken.goldroad.ui.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chicken.goldroad.R
import com.chicken.goldroad.ui.components.SprayText
import com.chicken.goldroad.ui.components.StrokedText
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(3000)
        onFinished()
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF3E2723))) {
        Image(
                painter = painterResource(id = R.drawable.bg),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
        )

        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                    painter = painterResource(id = R.drawable.menu_chicken),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier =
                            Modifier.align(Alignment.BottomStart)
                                    .padding(start = 10.dp, bottom = 200.dp)
                                    .fillMaxWidth(0.56f)
            )

            Image(
                    painter = painterResource(id = R.drawable.menu_basket),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier =
                            Modifier.align(Alignment.BottomEnd)
                                    .padding(end = 12.dp, bottom = 200.dp)
                                    .fillMaxWidth(0.32f)
            )
        }

        Column(
                modifier =
                        Modifier.fillMaxSize()
                                .windowInsetsPadding(WindowInsets.safeDrawing)
                                .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(0.22f))
            SprayText(text = "Chicken Gold Diggers Road", fontSize = 44.sp)

            Spacer(modifier = Modifier.weight(0.58f))

            StrokedText(text = "EGG FLOW DIGGER", modifier = Modifier.padding(bottom = 42.dp))

            Spacer(modifier = Modifier.weight(0.20f))
        }
    }
}
