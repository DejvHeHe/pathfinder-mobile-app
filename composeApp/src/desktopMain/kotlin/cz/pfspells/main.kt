package cz.pfspells

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() {
    val state = AppState()
    application { AppWindow(state) }
}

@androidx.compose.runtime.Composable
private fun androidx.compose.ui.window.ApplicationScope.AppWindow(state: AppState) {
    Window(
        onCloseRequest = ::exitApplication,
        title = "PF Spellbook",
        state = rememberWindowState(width = 1360.dp, height = 880.dp),
        onPreviewKeyEvent = { e ->
            if (e.type == KeyEventType.KeyUp && e.key == Key.Escape) state.pop() else false
        },
    ) {
        App(state)
    }
}
