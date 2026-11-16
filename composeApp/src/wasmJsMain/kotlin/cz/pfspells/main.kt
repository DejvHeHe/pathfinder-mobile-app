package cz.pfspells

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.events.KeyboardEvent

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val state = AppState()
    // Esc = zpět (jako na desktopu)
    window.addEventListener("keydown") { e ->
        if ((e as? KeyboardEvent)?.key == "Escape") state.pop()
    }
    document.getElementById("loading")?.remove()
    ComposeViewport(document.body!!) {
        App(state)
    }
}
