package cz.pfspells.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.pfspells.AppState
import cz.pfspells.data.CasterClasses
import cz.pfspells.data.SpellDatabase

/**
 * Detail spellu. [aside] = pravý panel širokého okna (zavírací křížek, tlačítko v textu místo plovoucího).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SpellDetailScreen(state: AppState, db: SpellDatabase, spellId: String, deckId: String?, aside: Boolean = false) {
    val spell = db.get(spellId)
    if (spell == null) {
        LaunchedEffect(spellId) { state.pop() }
        return
    }
    val fonts = LocalNbFonts.current
    val deck = deckId?.let { state.deck(it) }
    val cls = deck?.let { CasterClasses.get(it.classId) }
    val canAdd = deck != null && cls != null && cls.canCast(spell, deck.classLevel)
    val inDeck = deck?.spells?.get(spell.id) ?: 0
    val add = { state.addSpell(deck!!.id, spell.id) }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(start = if (aside) 24.dp else 4.dp, end = 8.dp, top = if (aside) 14.dp else 10.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!aside) BackButton { state.pop() }
                Text(
                    spell.name, Modifier.weight(1f),
                    fontFamily = fonts.serif, fontWeight = FontWeight.Bold,
                    fontSize = if (aside) 36.sp else 24.sp, lineHeight = if (aside) 38.sp else 26.sp,
                    color = Nb.ink, maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
                if (aside) NbIconButton(NbIcons.Close, "Close details", { state.pop() }, Nb.inkMuted)
            }
            SelectionContainer(Modifier.weight(1f)) {
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                        .padding(start = if (aside) 28.dp else 18.dp, end = if (aside) 28.dp else 18.dp, top = 8.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        NbTag(spell.school.replaceFirstChar { it.uppercase() } + (spell.subschool?.let { " ($it)" } ?: ""))
                        spell.descriptor?.let { NbTag("[$it]", Nb.rust) }
                        spell.levels.forEach { (list, lvl) ->
                            NbTag("${list.replaceFirstChar { it.uppercase() }} $lvl")
                        }
                    }

                    NbPanel(Modifier.fillMaxWidth(), color = if (aside) Nb.paper else Nb.panel) {
                        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            StatLine("Casting Time", spell.castingTime)
                            StatLine("Components", spell.components)
                            StatLine("Range", spell.range)
                            StatLine("Effect", spell.effect)
                            StatLine("Target", spell.targets)
                            StatLine("Duration", spell.duration)
                            StatLine("Saving Throw", spell.savingThrow)
                            StatLine("Spell Resistance", spell.spellResistance)
                        }
                    }

                    if (deck != null && cls != null && !canAdd) {
                        Text("${cls.name} ${deck.classLevel} cannot cast this spell.", color = Nb.error, fontSize = 13.sp)
                    }

                    SectionHeader("Description", Modifier.padding(top = 0.dp))
                    spell.shortDescription?.let {
                        Text(it, fontFamily = fonts.serif, fontWeight = FontWeight.Bold, fontSize = if (aside) 20.sp else 19.sp, lineHeight = 25.sp, color = Nb.ink)
                    }
                    spell.paragraphs.forEach {
                        Text(it, fontSize = if (aside) 15.5.sp else 14.5.sp, lineHeight = if (aside) 25.sp else 22.5.sp, color = Nb.ink)
                    }

                    spell.mythic?.let { mythic ->
                        SectionHeader("Mythic")
                        mythic.split('\n').filter { it.isNotBlank() }.forEach {
                            Text(it.trim(), fontSize = 14.5.sp, lineHeight = 22.5.sp, color = Nb.ink)
                        }
                    }

                    spell.source?.let { Text("Source: $it", fontSize = 12.5.sp, color = Nb.inkMuted) }
                    spell.url?.let { url ->
                        val uri = LocalUriHandler.current
                        Text(
                            "Open on Archives of Nethys",
                            Modifier.clickable { runCatching { uri.openUri(url) } }.padding(vertical = 4.dp),
                            color = Nb.accent, fontSize = 14.sp, textDecoration = TextDecoration.Underline,
                        )
                    }

                    if (aside && canAdd) {
                        Spacer(Modifier.height(4.dp))
                        NbPrimaryButton(
                            if (inDeck > 0) "Add a copy ($inDeck in deck)" else "Add to deck",
                            add,
                        )
                    }
                }
            }
        }
        if (!aside && canAdd) {
            NbFab(
                if (inDeck > 0) "Add to deck (×$inDeck)" else "Add to deck",
                add,
                Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 18.dp),
            )
        }
    }
}

@Composable
private fun StatLine(label: String, value: String?) {
    if (value.isNullOrBlank()) return
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            label,
            Modifier.width(132.dp).padding(top = 1.dp),
            fontFamily = LocalNbFonts.current.mono, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Nb.rust,
        )
        Text(value, Modifier.weight(1f), fontSize = 14.sp, color = Nb.ink)
    }
}
