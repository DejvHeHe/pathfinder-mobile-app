package cz.pfspells.ui

import cz.pfspells.data.caster
import cz.pfspells.data.bonusSpellsByLevel
import cz.pfspells.str
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
import cz.pfspells.data.Metamagic
import cz.pfspells.data.patronSpellLevel
import cz.pfspells.data.spellLevelFor
import cz.pfspells.data.SpellDatabase

/**
 * Detail spellu. [aside] = pravý panel širokého okna (zavírací křížek, tlačítko v textu místo plovoucího).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SpellDetailScreen(state: AppState, db: SpellDatabase, spellId: String, charId: String?, classId: String? = null, aside: Boolean = false) {
    val spell = db.get(spellId)
    if (spell == null) {
        LaunchedEffect(spellId) { state.pop() }
        return
    }
    val fonts = LocalNbFonts.current
    val s = str
    val deck = charId?.let { state.spellbook(it, classId) }
    val cls = deck?.entry?.caster
    val inDeck = deck?.spells?.get(spell.id) ?: 0
    // spontánní classa: známé kouzlo podruhé nepřidává
    // kouzlo patrona čarodějnice jde připravit, i když není na jejím spell listu
    val patronSpell = deck != null && deck.entry.patronSpellLevel(spell, db.subclasses) != null
    val canAdd = deck != null && cls != null && (cls.canCast(spell, deck.classLevel) || patronSpell) && !(cls.distinctSpells && inDeck > 0)
    val add = { state.addSpell(deck!!.id, deck.classId, spell.id) }

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
                val fav = state.isFavorite(spell.id)
                NbIconButton(
                    if (fav) NbIcons.StarFilled else NbIcons.Star,
                    if (fav) s.removeFavorite else s.addFavorite,
                    { state.toggleFavorite(spell.id) },
                    if (fav) Nb.rust else Nb.inkMuted,
                )
                if (aside) NbIconButton(NbIcons.Close, s.closeDetails, { state.pop() }, Nb.inkMuted)
            }
            SelectionContainer(Modifier.weight(1f)) {
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                        .padding(start = if (aside) 28.dp else 18.dp, end = if (aside) 28.dp else 18.dp, top = 8.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        NbTag(s.school(spell.school) + (spell.subschool?.let { " (${s.subschool(it)})" } ?: ""))
                        spell.descriptor?.let { NbTag("[$it]", Nb.rust) }
                        spell.levels.forEach { (list, lvl) ->
                            NbTag("${s.spellListName(list)} $lvl")
                        }
                        if (!spell.pfsLegal) NbTag(s.notPfsLegal, Nb.error)
                    }

                    NbPanel(Modifier.fillMaxWidth(), color = if (aside) Nb.paper else Nb.panel) {
                        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            StatLine(s.castingTime, spell.castingTime)
                            StatLine(s.components, spell.components)
                            StatLine(s.range, spell.range)
                            StatLine(s.effect, spell.effect)
                            StatLine(s.target, spell.targets)
                            StatLine(s.duration, spell.duration)
                            StatLine(s.savingThrow, spell.savingThrow)
                            StatLine(s.spellResistance, spell.spellResistance)
                        }
                    }

                    // kouzlo podtřídy (doména, mystérium…) – mimo class list, ale postava ho má
                    val subclassSpell = deck != null && deck.entry.bonusSpellsByLevel(db.subclasses, deck.classLevel).values.any { l -> l.any { it.id == spell.id } }
                    if (deck != null && cls != null && !canAdd && !subclassSpell && inDeck == 0) {
                        Text(s.cannotCastThis(s.nameOf(cls), deck.classLevel), color = Nb.error, fontSize = 13.sp)
                    }

                    // metamagické featy postavy: výsledná úroveň slotu pro toto kouzlo
                    val owner = charId?.let { state.character(it) }
                    LaunchedEffect(charId) { if (charId != null) state.ensureFeats() }
                    val spellLevel = cls?.let { deck?.entry?.spellLevelFor(it, spell, db.subclasses) ?: it.spellLevelOf(spell) }
                    val meta = if (owner != null) Metamagic.ofCharacter(owner, state.featDb) else emptyList()
                    if (deck != null && cls != null && spellLevel != null && meta.isNotEmpty()) {
                        SectionHeader(s.metamagicHeader, Modifier.padding(top = 0.dp))
                        meta.forEach { (feat, levels) ->
                            val slot = levels?.let { spellLevel + it }
                            val tooHigh = slot != null && slot > cls.maxSpellLevel(deck.classLevel)
                            Text(
                                s.metamagicLine(feat.name, levels, slot, tooHigh),
                                fontSize = 14.sp, color = if (tooHigh) Nb.error else Nb.ink,
                            )
                        }
                    }

                    SectionHeader(s.description, Modifier.padding(top = 0.dp))
                    spell.shortDescription?.let {
                        Text(it, fontFamily = fonts.serif, fontWeight = FontWeight.Bold, fontSize = if (aside) 20.sp else 19.sp, lineHeight = 25.sp, color = Nb.ink)
                    }
                    spell.paragraphs.forEach {
                        Text(it, fontSize = if (aside) 15.5.sp else 14.5.sp, lineHeight = if (aside) 25.sp else 22.5.sp, color = Nb.ink)
                    }

                    spell.mythic?.let { mythic ->
                        SectionHeader(s.mythic)
                        mythic.split('\n').filter { it.isNotBlank() }.forEach {
                            Text(it.trim(), fontSize = 14.5.sp, lineHeight = 22.5.sp, color = Nb.ink)
                        }
                    }

                    spell.source?.let { Text(s.sourceLine(it), fontSize = 12.5.sp, color = Nb.inkMuted) }
                    spell.url?.let { url ->
                        val uri = LocalUriHandler.current
                        Text(
                            s.openOnAon,
                            Modifier.clickable { runCatching { uri.openUri(url) } }.padding(vertical = 4.dp),
                            color = Nb.accent, fontSize = 14.sp, textDecoration = TextDecoration.Underline,
                        )
                    }

                    if (aside && canAdd) {
                        Spacer(Modifier.height(4.dp))
                        NbPrimaryButton(
                            if (inDeck > 0) s.addCopy(inDeck) else s.addToCharacter,
                            add,
                        )
                    }
                }
            }
        }
        if (!aside && canAdd) {
            NbFab(
                if (inDeck > 0) s.addToCharacterCount(inDeck) else s.addToCharacter,
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
