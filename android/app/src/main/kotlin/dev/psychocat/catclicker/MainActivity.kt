package dev.psychocat.catclicker

import android.content.pm.ApplicationInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.psychocat.catclicker.assets.AssetRegistry
import dev.psychocat.catclicker.assets.LocalAssets
import dev.psychocat.catclicker.game.save.SaveRepository
import dev.psychocat.catclicker.game.session.GameSession
import dev.psychocat.catclicker.ui.GameScreen
import dev.psychocat.catclicker.ui.settings.DebugTools
import dev.psychocat.catclicker.ui.theme.CatClickerTheme
import java.io.File

class MainActivity : ComponentActivity() {
    private val viewModel: GameViewModel by viewModels {
        viewModelFactory {
            initializer { GameViewModel(GameSession(SaveRepository(File(filesDir, "save")), AndroidClock)) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val assets = remember { AssetRegistry(applicationContext) }
            val debugTools = remember {
                val debuggable = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
                if (debuggable) DebugTools(grantFish = { viewModel.debugGrantFish(1_000_000.0) }, completeRoom = viewModel::debugCompleteRoom) else null
            }
            CatClickerTheme {
                CompositionLocalProvider(LocalAssets provides assets) {
                    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        val state by viewModel.state.collectAsStateWithLifecycle()
                        GameScreen(state = state, dispatch = viewModel::dispatch, debug = debugTools)
                    }
                }
            }
        }
    }

    // Both rotation and going Home pass through onStop/onStart; the session credits the time in between.
    override fun onStart() {
        super.onStart()
        viewModel.onForeground()
    }

    override fun onStop() {
        viewModel.onBackground()
        super.onStop()
    }
}
