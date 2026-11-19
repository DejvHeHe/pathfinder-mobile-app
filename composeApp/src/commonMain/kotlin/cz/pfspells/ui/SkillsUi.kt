package cz.pfspells.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.pfspells.AppState
import cz.pfspells.Strings
import cz.pfspells.data.Character
import cz.pfspells.data.SkillInfo
import cz.pfspells.data.Skills
import cz.pfspells.data.isClassSkill
import cz.pfspells.data.ranks
import cz.pfspells.data.skillPoints
import cz.pfspells.data.skillPointsUsed
import cz.pfspells.data.skillTotal
import cz.pfspells.str

/** Název skillu v jazyce rozhraní; u druhu „Řemeslo (alchemy)“. */
fun Strings.skillLabel(key: String): String {
    val info = Skills.get(Skills.idOf(key)) ?: return key
    val base = skillName(info.id, info.name)
    return Skills.subtypeOf(key)?.let { "$base ($it)" } ?: base
}

/**
 * Sekce „Dovednosti“ v detailu postavy: body (utracené / dostupné + navíc), ranky ve skillech.
 * Normálně jen skilly s ranky, „Upravit dovednosti“ ukáže všechny se steppery.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SkillsPanel(state: AppState, c: Character) {
    val s = str
    val fonts = LocalNbFonts.current
    var editing by remember { mutableStateOf(false) }
    var addSub by remember { mutableStateOf<SkillInfo?>(null) }
    val total = c.skillPoints()
    val used = c.skillPointsUsed()

    Column(Modifier.padding(top = 4.dp)) {
        SectionHeader(s.skillsHeader) {
            Text("$used / $total", fontFamily = fonts.mono, fontSize = 12.sp, color = if (used > total) Nb.error else Nb.inkMuted)
        }
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            NbDashedButton(if (editing) s.showTrainedSkills else s.showAllSkills, { editing = !editing })
            if (editing) Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.align(Alignment.CenterVertically)) {
                Text(s.extraFeats, fontSize = 12.5.sp, color = Nb.inkMuted, modifier = Modifier.padding(end = 6.dp))
                CountStepper(c.extraSkillPoints, { state.setExtraSkillPoints(c.id, it) }, min = -50, max = 200, size = 28.dp, numberSize = 13.sp)
            }
        }
        if (editing) {
            Text(s.skillsHint, fontSize = 12.5.sp, color = Nb.inkMuted, modifier = Modifier.padding(top = 6.dp))
            Skills.all.forEach { info ->
                if (info.subtyped) {
                    c.skills.keys.filter { Skills.idOf(it) == info.id }.sorted().forEach { key -> SkillRow(state, c, key, removable = true) }
                    NbDashedButton(s.addSubSkill(s.skillName(info.id, info.name)), { addSub = info }, Modifier.padding(vertical = 4.dp), icon = NbIcons.Plus)
                } else {
                    SkillRow(state, c, info.id, removable = false)
                }
            }
        } else if (c.skills.isEmpty()) {
            Text(s.noSkillRanks, fontSize = 13.sp, color = Nb.inkMuted, modifier = Modifier.padding(top = 6.dp))
        } else {
            // přehled: jen skilly s ranky, v pořadí seznamu
            val order = Skills.all.map { it.id }
            c.skills.keys.sortedWith(compareBy({ order.indexOf(Skills.idOf(it)) }, { it })).forEach { key ->
                SkillRow(state, c, key, removable = false, compact = true)
            }
        }
    }

    addSub?.let { info ->
        TextFieldDialog(s.addSubSkill(s.skillName(info.id, info.name)), "", s.subSkillLabel, onDismiss = { addSub = null }) { kind ->
            addSub = null
            val key = Skills.key(info.id, kind)
            if (c.ranks(key) == 0) state.setSkillRanks(c.id, key, 1)
        }
    }
}

/** Řádek skillu: název, atribut / třídní, celkový bonus, ranky (stepper, v přehledu jen číslo). */
@Composable
private fun SkillRow(state: AppState, c: Character, key: String, removable: Boolean, compact: Boolean = false) {
    val s = str
    val fonts = LocalNbFonts.current
    val info = Skills.get(Skills.idOf(key)) ?: return
    val ranks = c.ranks(key)
    val classSkill = c.isClassSkill(key)
    val totalBonus = c.skillTotal(key)
    Row(
        Modifier.fillMaxWidth().bottomRule(Nb.ruleSoft).padding(vertical = if (compact) 4.dp else 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                s.skillLabel(key), fontSize = 14.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                fontWeight = if (ranks > 0) FontWeight.SemiBold else FontWeight.Normal,
                color = if (ranks > 0 || totalBonus != null) Nb.ink else Nb.inkMuted,
            )
            Text(
                listOfNotNull(s.abilityShort(info.ability), s.classSkillShort.takeIf { classSkill }).joinToString(" · "),
                fontFamily = fonts.mono, fontSize = 10.5.sp, color = if (classSkill) Nb.accent else Nb.inkMuted,
            )
        }
        Text(
            totalBonus?.let { if (it < 0) "−${-it}" else "+$it" } ?: "—",
            Modifier.width(36.dp), textAlign = TextAlign.End,
            fontFamily = fonts.serif, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Nb.ink,
        )
        if (compact) {
            Text(s.ranks(ranks), Modifier.width(72.dp), textAlign = TextAlign.End, fontFamily = fonts.mono, fontSize = 11.5.sp, color = Nb.inkMuted)
        } else {
            CountStepper(ranks, { state.setSkillRanks(c.id, key, it) }, min = 0, max = c.level, size = 28.dp, numberSize = 13.sp)
            if (removable) NbIconButton(NbIcons.Close, s.delete, { state.removeSkill(c.id, key) }, Nb.inkMuted, 14.dp)
        }
    }
}
