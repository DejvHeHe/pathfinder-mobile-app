package cz.pfspells

import androidx.compose.ui.window.ComposeUIViewController

private val appState by lazy { AppState() }

@Suppress("FunctionName", "unused")
fun MainViewController() = ComposeUIViewController { App(appState) }
