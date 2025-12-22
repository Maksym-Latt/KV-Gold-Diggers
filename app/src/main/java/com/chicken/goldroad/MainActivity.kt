package com.chicken.goldroad

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.chicken.goldroad.data.AudioController
import com.chicken.goldroad.data.SoundManager
import com.chicken.goldroad.ui.AppRootNavigation
import com.chicken.goldroad.ui.theme.ChickenGoldDiggersRoadTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var soundManager: SoundManager
    @Inject lateinit var audioController: AudioController

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideSystemBars()

        setContent { ChickenGoldDiggersRoadTheme { AppRootNavigation() } }
    }

    override fun onResume() {
        super.onResume()
        soundManager.resumeAfterLifecycle()
        audioController.resume()
    }

    override fun onPause() {
        audioController.pause()
        soundManager.pauseForLifecycle()
        super.onPause()
    }

    override fun onDestroy() {
        audioController.release()
        soundManager.release()
        super.onDestroy()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    private fun hideSystemBars() {
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }
}
