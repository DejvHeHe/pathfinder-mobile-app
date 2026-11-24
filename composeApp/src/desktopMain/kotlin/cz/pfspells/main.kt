package cz.pfspells

import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.loadImageBitmap
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
        icon = remember { appIcon() },
        state = rememberWindowState(width = 1360.dp, height = 880.dp),
        onPreviewKeyEvent = { e ->
            if (e.type == KeyEventType.KeyUp && e.key == Key.Escape) state.pop() else false
        },
    ) {
        App(state)
    }
}

/** Ikona okna (resources/icon.png – generuje tools/build_icons.py); bez ní výchozí ikona. */
private fun appIcon(): BitmapPainter? =
    Thread.currentThread().contextClassLoader?.getResourceAsStream("icon.png")?.use { BitmapPainter(loadImageBitmap(it)) }
