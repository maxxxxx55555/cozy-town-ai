package com.aistudio.cozytown

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.aistudio.cozytown.ui.GameScreen
import com.aistudio.cozytown.ui.GameViewModel
import com.aistudio.cozytown.ui.theme.CozyTownTheme

class MainActivity : ComponentActivity() {

    private val viewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            CozyTownTheme {
                GameScreen(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (viewModel.uiState.value.isMusicEnabled) {
            viewModel.audioManager.startMusic()
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.saveGame()
        viewModel.audioManager.pauseMusic()
    }
}
