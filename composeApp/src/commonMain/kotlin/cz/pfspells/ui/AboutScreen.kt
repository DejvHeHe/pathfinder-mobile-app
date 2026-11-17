package cz.pfspells.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.pfspells.AppState
import cz.pfspells.data.decodeText
import cz.pfspells.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

private const val REPO_URL = "https://github.com/DejvHeHe/pathfinder-mobile-app"

/**
 * Licence a povinné texty (GPL-3.0, Paizo Community Use, OGL 1.0a se Section 15, zdroje dat, fonty, knihovny).
 * Stejný obsah je v NOTICE.md v repozitáři.
 */
@OptIn(ExperimentalResourceApi::class)
@Composable
fun AboutScreen(state: AppState, wide: Boolean = false) {
    val fonts = LocalNbFonts.current
    var showOgl by remember { mutableStateOf(false) }
    var oglText by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(showOgl) {
        if (showOgl && oglText == null) {
            oglText = try { decodeText(Res.readBytes("files/OGL.txt")) } catch (e: Exception) { "Could not load the license text." }
        }
    }

    NbScreen(
        title = "About & licenses",
        subtitle = null,
        onBack = { if (wide) state.popMain() else state.pop() },
        wide = wide,
    ) {
        SelectionContainer {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(horizontal = if (wide) 0.dp else 16.dp, vertical = 12.dp)
                    .widthIn(max = 720.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                NbPanel(Modifier.fillMaxWidth(), color = Nb.inset) {
                    Text("PF1 Spellbook", fontFamily = fonts.serif, fontWeight = FontWeight.Bold, fontSize = 26.sp, color = Nb.ink)
                    Text("Copyright (C) 2026 David Sobíšek", fontSize = 14.sp, color = Nb.inkMuted)
                    Link(REPO_URL)
                }
                Body(
                    "This program is free software: you can redistribute it and/or modify it under the terms of the " +
                        "GNU General Public License version 3. It is distributed WITHOUT ANY WARRANTY. " +
                        "The full license text is in the LICENSE file of the source code repository.",
                )
                Body("The game content (spells, items) is not covered by the GPL – it remains under its own licenses below.")

                SectionHeader("Paizo Community Use")
                Body(
                    "This app uses trademarks and/or copyrights owned by Paizo Inc., used under Paizo's Community Use Policy " +
                        "(paizo.com/licenses/communityuse). We are expressly prohibited from charging you to use or access this content. " +
                        "This app is not published, endorsed, or specifically approved by Paizo. " +
                        "For more information about Paizo Inc. and Paizo products, visit paizo.com.",
                )

                SectionHeader("Open Game License")
                Body(
                    "Game rules text of Pathfinder Roleplaying Game (1st edition) spells and items is Open Game Content under the " +
                        "Open Game License v1.0a. Proper names of the Pathfinder campaign setting, the \"Pathfinder\" name and trademarks " +
                        "are Product Identity of Paizo Inc., used under the Community Use Policy above.",
                )
                NbDashedButton(if (showOgl) "Hide license text" else "Show Open Game License v1.0a", { showOgl = !showOgl })
                if (showOgl) {
                    Text(
                        oglText ?: "Loading…",
                        fontFamily = fonts.mono, fontSize = 11.5.sp, lineHeight = 16.sp, color = Nb.ink,
                    )
                }

                SectionHeader("Data sources")
                Source("Spells", "SpellTable-pf1e (MIT) by Vladislav Sergeev – data from Archives of Nethys (aonprd.com)")
                Source("Weapons, ammunition, magic items", "Vireche/randomitemgenerator – data from d20pfsrd.com (OGL)")
                Source("Gear, armor, alchemical items, mounts", "Pathfinder 1e system for Foundry VTT (GPL-3.0, content OGL)")

                SectionHeader("Fonts")
                Body("Alegreya, Alegreya Sans and IBM Plex Mono – SIL Open Font License 1.1.")

                SectionHeader("Libraries")
                Body(
                    "Kotlin, Compose Multiplatform, kotlinx.coroutines, kotlinx.serialization, Ktor, Multiplatform Settings, " +
                        "AndroidX – Apache License 2.0.",
                )
            }
        }
    }
}

@Composable
private fun Body(text: String) {
    Text(text, fontSize = 14.5.sp, lineHeight = 21.sp, color = Nb.ink)
}

@Composable
private fun Source(what: String, from: String) {
    Column {
        Text(what, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold, color = Nb.ink)
        Text(from, fontSize = 13.5.sp, lineHeight = 19.sp, color = Nb.inkMuted)
    }
}

@Composable
private fun Link(url: String) {
    val uri = LocalUriHandler.current
    Text(
        url.removePrefix("https://"),
        Modifier.clickable { runCatching { uri.openUri(url) } }.padding(vertical = 4.dp),
        color = Nb.accent, fontSize = 14.sp, textDecoration = TextDecoration.Underline,
    )
}
