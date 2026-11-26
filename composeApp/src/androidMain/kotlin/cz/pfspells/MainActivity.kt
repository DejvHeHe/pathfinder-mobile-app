package cz.pfspells

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import cz.pfspells.data.AndroidAppUpdater
import cz.pfspells.data.AndroidDeckFileIo
import cz.pfspells.data.AppFiles

class MainActivity : ComponentActivity() {
    // configChanges v manifestu → přežije rotaci; po process death se stack resetuje (decky zůstanou).
    private val state by lazy { AppState() }

    override fun onCreate(savedInstanceState: Bundle?) {
        AppFiles.init(this)
        super.onCreate(savedInstanceState)
        AndroidDeckFileIo.register(this)
        AndroidAppUpdater.register(this)
        // světlý „papír“ → tmavé ikony stavového řádku i v systémovém tmavém režimu
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        onBackPressedDispatcher.addCallback(this) {
            if (!state.pop()) finish()
        }
        setContent { App(state) }
    }
}
