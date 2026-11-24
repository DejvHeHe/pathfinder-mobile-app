package cz.pfspells.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.pfspells.AppState
import cz.pfspells.data.Character
import cz.pfspells.data.armorCheckPenalty
import cz.pfspells.data.currentHp
import cz.pfspells.data.defense
import cz.pfspells.data.maxHp
import cz.pfspells.str

/**
 * Obrana a životy v detailu postavy: AC, dotyk, zaskočený (z vybavení, Obratnosti, velikosti, přirozené zbroje),
 * HP aktuální / max se stepperem zranění a nastavení (HP navíc, házené HP, AC – ostatní).
 */
@Composable
fun DefensePanel(state: AppState, c: Character) {
    val s = str
    val d = c.defense()
    val max = c.maxHp()
    val cur = c.currentHp()
    var edit by remember { mutableStateOf(false) }
    Column(Modifier.padding(top = 4.dp)) {
        SectionHeader(s.defenseHeader)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            DefenseTile(s.acLabel, "${d.ac}", Modifier.weight(1f))
            DefenseTile(s.touchLabel, "${d.touch}", Modifier.weight(1f))
            DefenseTile(s.flatFootedLabel, "${d.flatFooted}", Modifier.weight(1f))
            DefenseTile(s.hpLabel, "$cur / $max", Modifier.weight(1.4f), alert = cur <= 0)
        }
        Text(s.acBreakdown(d), fontSize = 12.5.sp, color = Nb.inkMuted, modifier = Modifier.padding(top = 6.dp))
        val acp = c.armorCheckPenalty()
        if (acp != 0) Text(s.armorCheckPenalty(acp), fontSize = 12.5.sp, color = Nb.rust, modifier = Modifier.padding(top = 2.dp))
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                MonoLabel(s.hpLabel)
                Text(s.damageHint, fontSize = 12.sp, color = Nb.inkMuted)
            }
            // stepper aktuálních HP: − = zranění, + = léčení (max = plné HP)
            CountStepper(cur, { v -> state.setDamage(c.id, max - v) }, min = -max, max = max, size = 32.dp, numberSize = 17.sp)
        }
        Text(s.hpHint(c.hpOverride == null), fontSize = 12.sp, color = Nb.inkMuted, modifier = Modifier.padding(top = 4.dp))
        NbDashedButton(s.adjust, { edit = true }, Modifier.padding(top = 6.dp), icon = NbIcons.Pencil)
    }
    if (edit) DefenseDialog(c, onDismiss = { edit = false }) { bonus, override, misc ->
        edit = false
        state.setDefenseSettings(c.id, bonus, override, misc)
    }
}

@Composable
private fun DefenseTile(label: String, value: String, modifier: Modifier, alert: Boolean = false) {
    Column(
        modifier.clip(Nb.shape).background(Nb.panel).dashedBorder().padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MonoLabel(label, color = Nb.rust, size = 10.5.sp)
        Text(
            value, fontFamily = LocalNbFonts.current.serif, fontWeight = FontWeight.Bold, fontSize = 19.sp,
            color = if (alert) Nb.error else Nb.ink, maxLines = 1,
        )
    }
}

@Composable
private fun DefenseDialog(c: Character, onDismiss: () -> Unit, onSave: (hpBonus: Int, hpOverride: Int?, acMisc: Int) -> Unit) {
    val s = str
    var bonus by remember { mutableIntStateOf(c.hpBonus) }
    var override by remember { mutableStateOf(c.hpOverride?.toString().orEmpty()) }
    var misc by remember { mutableIntStateOf(c.acMisc) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Nb.panel,
        title = { Text(s.defenseHeader, color = Nb.ink) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(s.hpBonusLabel, Modifier.weight(1f), fontSize = 14.sp, color = Nb.ink)
                    CountStepper(bonus, { bonus = it }, min = -99, max = 999, size = 30.dp, numberSize = 16.sp)
                }
                OutlinedTextField(
                    override, { t -> override = t.filter { it.isDigit() }.take(4) },
                    label = { Text(s.hpOverrideLabel) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(s.acMiscLabel, Modifier.weight(1f), fontSize = 14.sp, color = Nb.ink)
                    CountStepper(misc, { misc = it }, min = -20, max = 30, size = 30.dp, numberSize = 16.sp)
                }
            }
        },
        confirmButton = {
            NbPrimaryButton(s.save, { onSave(bonus, override.toIntOrNull()?.takeIf { it > 0 }, misc) }, icon = null)
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s.cancel, color = Nb.ink) } },
    )
}
