package cz.pfspells

import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.w3c.dom.PopStateEvent
import org.w3c.dom.events.KeyboardEvent

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val state = AppState()
    // Esc = zpět (jako na desktopu)
    window.addEventListener("keydown") { e ->
        if ((e as? KeyboardEvent)?.key == "Escape") state.pop()
    }
    syncBrowserHistory(state)
    document.getElementById("loading")?.remove()
    ComposeViewport(document.body!!) {
        App(state)
    }
}

/**
 * Propojí tlačítko Zpět prohlížeče s back stackem appky.
 *
 * Každá úroveň zanoření (`stack.size - 1`) = jeden záznam v historii prohlížeče s indexem v `state`.
 * - stack se prohloubí (push) → `pushState` s dalším indexem,
 * - stack se zmenší uvnitř appky (šipka, Esc, přepnutí panelu) → `history.go(-n)`,
 * - Zpět v prohlížeči (`popstate` s nižším indexem) → `state.pop()`, dokud stack neodpovídá,
 * - Vpřed v prohlížeči nejde obnovit (obrazovka už neexistuje) → vrátí se zpět.
 * Událost `popstate` s indexem, který už odpovídá, je ozvěna vlastního `history.go` a ignoruje se.
 * Výměna obrazovky bez změny hloubky (např. jiný detail spellu) historii nemění.
 */
private fun syncBrowserHistory(state: AppState) {
    var depth = 0
    window.history.replaceState(0.toJsNumber(), "")

    window.addEventListener("popstate") { e ->
        val idx = ((e as? PopStateEvent)?.state as? JsNumber)?.toInt() ?: 0
        when {
            idx < depth -> {
                depth = idx
                while (state.stack.size - 1 > idx && state.pop()) Unit
            }
            idx > depth -> window.history.go(depth - idx)
        }
    }

    MainScope().launch {
        snapshotFlow { state.stack.size }.collect { size ->
            val target = size - 1
            if (target > depth) {
                while (depth < target) {
                    depth++
                    window.history.pushState(depth.toJsNumber(), "")
                }
            } else if (target < depth) {
                val n = depth - target
                depth = target
                window.history.go(-n)
            }
        }
    }
}
