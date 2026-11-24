package cz.pfspells.ui

import cz.pfspells.str
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
    val s = str
    var showOgl by remember { mutableStateOf(false) }
    var oglText by remember { mutableStateOf<String?>(null) }
    var oglLoaded by remember { mutableStateOf(false) }
    LaunchedEffect(showOgl) {
        if (showOgl && !oglLoaded) {
            oglText = try { decodeText(Res.readBytes("files/OGL.txt")) } catch (e: Exception) { null }
            oglLoaded = true
        }
    }

    NbScreen(
        title = s.aboutTitle,
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
                SectionHeader(s.privacyHeader)
                Body(s.privacyText)

                SectionHeader(s.licenseHeader)
                Body(s.gplText)
                Body(s.gameContentNotGpl)

                SectionHeader("Paizo Community Use")
                Body(
                    "This app uses trademarks and/or copyrights owned by Paizo Inc., used under Paizo's Community Use Policy " +
                        "(paizo.com/licenses/communityuse). We are expressly prohibited from charging you to use or access this content. " +
                        "This app is not published, endorsed, or specifically approved by Paizo. " +
                        "For more information about Paizo Inc. and Paizo products, visit paizo.com.",
                )

                SectionHeader("Open Game License")
                Body(s.oglText)
                NbDashedButton(if (showOgl) s.hideOgl else s.showOgl, { showOgl = !showOgl })
                if (showOgl) {
                    Text(
                        oglText ?: if (oglLoaded) s.couldNotLoadLicense else s.loading,
                        fontFamily = fonts.mono, fontSize = 11.5.sp, lineHeight = 16.sp, color = Nb.ink,
                    )
                }

                SectionHeader(s.dataSources)
                Source(s.sourceSpells, "SpellTable-pf1e (MIT) by Vladislav Sergeev – data from Archives of Nethys (aonprd.com)")
                Source(s.sourceWeapons, "Vireche/randomitemgenerator – data from d20pfsrd.com (OGL)")
                Source(s.sourceGear, "Pathfinder 1e system for Foundry VTT (GPL-3.0, content OGL)")
                Source(s.sourceFeats, "Archives of Nethys (aonprd.com) – OGL / Paizo Community Use Policy")
                Source(s.sourceClasses, "Archives of Nethys class tables; Foundry VTT PF1 packs/classes (GPL-3.0, content OGL)")
                Source(s.sourceRaces, "Foundry VTT PF1 packs/races (GPL-3.0, content OGL)")
                Source(s.sourceDeities, "Archives of Nethys (aonprd.com) – Paizo Community Use Policy (deity names are Paizo Product Identity)")
                Source(s.sourceCompanions, "Core Rulebook – Animal Companions (d20pfsrd.com, OGL)")
                // MIT vyžaduje přiložit copyright a text licence (SpellList.json)
                Text(MIT_SPELLTABLE, fontFamily = fonts.mono, fontSize = 11.sp, lineHeight = 15.sp, color = Nb.inkMuted)

                SectionHeader(s.fonts)
                Body(s.fontsText)

                SectionHeader(s.libraries)
                Body(s.librariesText)
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

/** Licence projektu SpellTable-pf1e (zdroj SpellList.json) – MIT vyžaduje přiložit tento text. */
private const val MIT_SPELLTABLE = """SpellTable-pf1e – MIT License
Copyright (c) 2025 Vladislav Sergeev

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE."""
