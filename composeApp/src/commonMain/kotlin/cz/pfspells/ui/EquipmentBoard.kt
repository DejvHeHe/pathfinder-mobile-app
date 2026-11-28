package cz.pfspells.ui

import androidx.compose.ui.graphics.drawscope.translate

import cz.pfspells.data.ItemCategory

import androidx.compose.runtime.key

import androidx.compose.ui.graphics.StrokeJoin

import androidx.compose.ui.graphics.StrokeCap

import androidx.compose.ui.graphics.PathMeasure

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.pfspells.AppState
import cz.pfspells.data.InvEntry
import cz.pfspells.data.Inventory
import cz.pfspells.data.ItemSlot
import cz.pfspells.data.equippedIn
import cz.pfspells.data.isWorking
import cz.pfspells.data.slotConflicts
import cz.pfspells.str

/*
 * Inventář 2.0 (návrh „Inventory 2.0 – figurka a equip sloty“): sloty ve dvou sloupcích kolem siluety postavy,
 * pod nimi ruce a předměty bez slotu; stavy dlaždic plný / prázdný / vybraný / konflikt; banner konfliktu
 * a sheet slotu (mobil bottom sheet, desktop dialog) s volbou, který předmět platí.
 */

/** Jedna dlaždice: slot a pro prsteny pořadí (Prsten 1 / 2). */
private data class SlotPos(val slot: ItemSlot, val index: Int = 0)

private val LEFT = listOf(ItemSlot.HEAD, ItemSlot.HEADBAND, ItemSlot.EYES, ItemSlot.NECK, ItemSlot.SHOULDERS, ItemSlot.CHEST, ItemSlot.BODY)
    .map { SlotPos(it) }
private val RIGHT = listOf(
    SlotPos(ItemSlot.ARMOR), SlotPos(ItemSlot.BELT), SlotPos(ItemSlot.WRISTS), SlotPos(ItemSlot.HANDS),
    SlotPos(ItemSlot.RING, 0), SlotPos(ItemSlot.RING, 1), SlotPos(ItemSlot.FEET),
)
private val HANDS = listOf(SlotPos(ItemSlot.MAIN_HAND), SlotPos(ItemSlot.SHIELD))

/**
 * Tvar těla nositele inventáře: humanoid, nebo zvíře (mazlíček / čtyřnohý inventář). Zvířata mají sloty podle
 * Ultimate Equipment – bez rukou, prstenů, zápěstí a zbraní; had navíc bez ramen, opasku a nohou.
 */
enum class BodyShape { HUMANOID, QUADRUPED, BIRD, SERPENT }

private val BIRD_WORDS = listOf(
    "bird", "raven", "owl", "hawk", "eagle", "falcon", "parrot", "crow", "roc", "vulture", "thrush", "bat", "pteranodon",
    "archaeopteryx", "axe beak", "condor", "heron", "stork", "seagull", "gull", "rooster", "chicken", "goose", "duck",
)
private val SNAKE_WORDS = listOf("snake", "viper", "constrictor", "serpent", "python", "cobra", "eel", "adder")

/** Tvar podle mazlíčka propojeného s inventářem (druh zvířete, familiár, forma eidolona), jinak podle „čtyřnohý“. */
fun bodyShapeOf(inv: Inventory, owner: cz.pfspells.data.Character?): BodyShape {
    if (owner != null && owner.isCompanion) {
        when (owner.eidolonForm?.removePrefix("u_")) {
            "biped" -> return BodyShape.HUMANOID
            "quadruped" -> return BodyShape.QUADRUPED
            "serpentine" -> return BodyShape.SERPENT
            "avian" -> return BodyShape.BIRD
        }
        val name = (cz.pfspells.data.SpeciesCatalog.get(owner.species)?.name
            ?: cz.pfspells.data.FamiliarCatalog.get(owner.familiar)?.name ?: owner.name).lowercase()
        when {
            SNAKE_WORDS.any { it in name } -> return BodyShape.SERPENT
            BIRD_WORDS.any { Regex("\\b$it").containsMatchIn(name) } -> return BodyShape.BIRD
        }
        if (owner.species != null || owner.familiar != null || owner.quadruped) return BodyShape.QUADRUPED
    }
    return if (inv.quadruped) BodyShape.QUADRUPED else BodyShape.HUMANOID
}

/** Sloty, které tvar těla má (pořadí dlaždic). */
private fun shapeSlots(shape: BodyShape): Set<ItemSlot> = when (shape) {
    BodyShape.HUMANOID -> ItemSlot.entries.toSet()
    BodyShape.QUADRUPED, BodyShape.BIRD -> setOf(
        ItemSlot.HEAD, ItemSlot.HEADBAND, ItemSlot.EYES, ItemSlot.NECK, ItemSlot.SHOULDERS,
        ItemSlot.CHEST, ItemSlot.BODY, ItemSlot.ARMOR, ItemSlot.BELT, ItemSlot.FEET,
    )
    BodyShape.SERPENT -> setOf(ItemSlot.HEAD, ItemSlot.HEADBAND, ItemSlot.EYES, ItemSlot.NECK, ItemSlot.CHEST, ItemSlot.BODY, ItemSlot.ARMOR)
}

/** Dlaždice desky: sloty tvaru + sloty, ve kterých něco nasazeného je (ať se předmět „neztratí“). */
private fun boardLayout(shape: BodyShape, inv: Inventory): Triple<List<SlotPos>, List<SlotPos>, List<SlotPos>> {
    if (shape == BodyShape.HUMANOID) return Triple(LEFT, RIGHT, HANDS)
    val used = inv.entries.filter { it.equipped && it.slot != null }.map { it.slot!! }.toSet()
    val keep = shapeSlots(shape) + used
    val all = (LEFT + RIGHT + HANDS).filter { it.slot in keep && (it.slot != ItemSlot.RING || it.index < inv.equippedIn(ItemSlot.RING).size) }
    val half = (all.size + 1) / 2
    return Triple(all.take(half), all.drop(half), emptyList())
}

private enum class TileState { EMPTY, FILLED, SELECTED, CONFLICT }

/** Předmět zobrazený na dlaždici (u prstenů podle pořadí). */
private fun Inventory.tileEntry(p: SlotPos): InvEntry? = equippedIn(p.slot).getOrNull(p.index)

/** Nadpis sekce „Vybavení · 8 z 16 slotů“. */
@Composable
fun EquipmentHeader(inv: Inventory, shape: BodyShape = BodyShape.HUMANOID) {
    val s = str
    val (l, r, h) = boardLayout(shape, inv)
    val all = l + r + h
    SectionHeader(s.equipmentHeader) { MonoLabel(s.slotsFilled(all.count { inv.tileEntry(it) != null }, all.size)) }
}

/** Banner konfliktu slotu: „Ramena: 2 předměty, platí jen první. Vyřešit“ (pro každý přeplněný slot). */
@Composable
fun SlotConflictBanners(inv: Inventory, onResolve: (ItemSlot) -> Unit) {
    val s = str
    inv.slotConflicts().forEach { slot ->
        val n = inv.equippedIn(slot).sumOf { it.qty.coerceAtLeast(1) }
        // roztržený lístek: konflikt slotu
        NbTornNote(
            title = s.conflictBanner(s.itemSlot(slot), n),
            text = inv.equippedIn(slot).joinToString(" · ") { it.name },
            action = s.resolve, onAction = { onResolve(slot) },
            modifier = Modifier.padding(vertical = 4.dp),
        )
    }
}

/**
 * Figurka se sloty: levý sloupec (hlava…tělo), silueta, pravý sloupec (zbroj…nohy), pod tím ruce a bez slotu.
 * [selectedEntryId] = položka otevřená v detailu (dlaždice „vybraná“); klepnutí → [onSlot] (null = bez slotu).
 */
@Composable
fun EquipmentBoard(
    inv: Inventory,
    wide: Boolean,
    selectedEntryId: String?,
    shape: BodyShape = BodyShape.HUMANOID,
    onSlot: (ItemSlot?) -> Unit,
) {
    val s = str
    val slotless = inv.entries.filter { it.equipped && it.slot == null }
    val (left, right, hands) = boardLayout(shape, inv)
    val animal = shape != BodyShape.HUMANOID
    // zvíře je na šířku – na mobilu nad dlaždicemi přes celou šířku, ne v úzkém prostředním sloupci
    val figureOnTop = animal && !wide
    val figureW = if (wide) 200.dp else 92.dp
    val body: @Composable ColumnScope.() -> Unit = {
        if (figureOnTop) {
            Box(Modifier.fillMaxWidth().padding(bottom = 8.dp), contentAlignment = Alignment.Center) {
                Silhouette(inv, 240.dp, shape, left + right + hands)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(if (wide) 18.dp else 8.dp), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(if (wide) 8.dp else 6.dp)) {
                left.forEach { p -> BoardTile(inv, p, wide, selectedEntryId) { onSlot(p.slot) } }
            }
            if (!figureOnTop) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Silhouette(inv, figureW, shape, left + right + hands)
                    if (wide) MonoLabel(s.creatureSize(inv.creatureSize) + if (inv.quadruped) " · ${s.quadruped}" else "")
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(if (wide) 8.dp else 6.dp)) {
                right.forEach { p -> BoardTile(inv, p, wide, selectedEntryId) { onSlot(p.slot) } }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = if (wide) 12.dp else 8.dp), horizontalArrangement = Arrangement.spacedBy(if (wide) 8.dp else 6.dp)) {
            hands.forEach { p -> Box(Modifier.weight(1f)) { BoardTile(inv, p, wide, selectedEntryId) { onSlot(p.slot) } } }
            // předměty bez slotu (ioun stones…): na desktopu vždy, na mobilu jen když nějaké jsou
            if (wide || slotless.isNotEmpty()) {
                Box(Modifier.weight(1f)) {
                    SlotTile(
                        label = s.noSlot,
                        name = slotless.joinToString { it.name }.ifEmpty { null },
                        state = if (slotless.any { it.id == selectedEntryId }) TileState.SELECTED
                        else if (slotless.isEmpty()) TileState.EMPTY else TileState.FILLED,
                        note = null, wide = wide,
                    ) { onSlot(null) }
                }
            }
        }
    }
    if (wide) {
        Column(Modifier.fillMaxWidth().clip(Nb.shape).background(Nb.inset).dashedBorder().padding(16.dp), content = body)
    } else {
        Column(Modifier.fillMaxWidth(), content = body)
    }
}

@Composable
private fun BoardTile(inv: Inventory, p: SlotPos, wide: Boolean, selectedEntryId: String?, onClick: () -> Unit) {
    val s = str
    val all = inv.equippedIn(p.slot)
    val count = all.sumOf { it.qty.coerceAtLeast(1) }
    val e = inv.tileEntry(p)
    val conflict = count > p.slot.capacity && e != null
    val label = if (p.slot == ItemSlot.RING) s.ringSlot(p.index + 1) else s.itemSlot(p.slot)
    val state = when {
        e == null -> TileState.EMPTY
        conflict -> TileState.CONFLICT
        e.id == selectedEntryId -> TileState.SELECTED
        else -> TileState.FILLED
    }
    SlotTile(label, e?.name, state, note = if (conflict) "$count×" else null, wide = wide, onClick = onClick)
}

/** Dlaždice slotu ve stavech z návrhu (plný, prázdný, vybraný, konflikt). */
@Composable
private fun SlotTile(label: String, name: String?, state: TileState, note: String?, wide: Boolean, onClick: () -> Unit) {
    val s = str
    val base = Modifier.fillMaxWidth().heightIn(min = if (wide) 52.dp else 44.dp).clip(Nb.shape)
    val styled = when (state) {
        TileState.EMPTY -> base.dashedBorder(color = Nb.dash.copy(alpha = 0.55f))
        TileState.FILLED -> base.background(Nb.panel).dashedBorder()
        TileState.SELECTED -> base.background(Nb.inset).border(2.dp, Nb.accent, Nb.shape)
        TileState.CONFLICT -> base.background(Nb.panel).border(1.5.dp, Nb.error, Nb.shape)
    }
    Column(
        styled.clickable(onClick = onClick).padding(horizontal = if (wide) 10.dp else 8.dp, vertical = if (wide) 6.dp else 4.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        MonoLabel(
            label, size = if (wide) 10.5.sp else 10.sp,
            color = when (state) {
                TileState.EMPTY -> Nb.inkMuted
                TileState.CONFLICT -> Nb.error
                else -> Nb.rust
            },
        )
        Text(
            name ?: s.emptySlot, maxLines = 1, overflow = TextOverflow.Ellipsis,
            fontSize = if (wide) 14.sp else 12.5.sp,
            fontWeight = if (name == null) FontWeight.Normal else FontWeight.SemiBold,
            color = if (name == null) Nb.placeholder else Nb.ink,
        )
        note?.let { MonoLabel(it, color = Nb.error, size = 10.sp) }
    }
}

// silueta z návrhu (viewBox 200 × 360): hlava + tělo, čárkovaný obrys
private val BODY_PATH = PathParser().parsePathString(
    "M90,62 L60,76 L46,90 L38,150 L32,204 L27,222 L40,228 L46,206 L54,152 L64,112 L70,190 L66,214 L72,290 L76,338 " +
        "L64,352 L96,352 L95,338 L98,236 L100,228 L102,236 L105,338 L104,352 L136,352 L124,338 L128,290 L134,214 " +
        "L130,190 L136,112 L146,152 L154,206 L160,228 L173,222 L168,204 L162,150 L154,90 L140,76 L110,62 Z",
).toPath()

/** Obrys nasazeného předmětu na siluetě (viewBox 200 × 360): cesta, výplň (jen uzavřené tvary), kreslení za tělem. */
private class Outline(val d: String, val filled: Boolean = true, val behind: Boolean = false)

/** Druh věci v ruce podle jména / kategorie předmětu. */
private enum class HandItem { SWORD, DAGGER, AXE, MACE, POLE, STAFF, BOW, WAND, SHIELD, BUCKLER, TOWER }

private fun handItem(e: InvEntry): HandItem {
    val n = e.name.lowercase()
    fun has(vararg w: String) = w.any { it in n }
    return when {
        has("tower shield") -> HandItem.TOWER
        has("buckler") -> HandItem.BUCKLER
        has("shield") -> HandItem.SHIELD
        has("bow") -> HandItem.BOW
        e.cat == ItemCategory.STAFF || has("staff") -> HandItem.STAFF
        e.cat == ItemCategory.WAND || e.cat == ItemCategory.ROD || has("wand", "rod of") -> HandItem.WAND
        has("spear", "lance", "glaive", "halberd", "pike", "trident", "guisarme", "ranseur", "bardiche", "scythe", "naginata") -> HandItem.POLE
        has("axe") -> HandItem.AXE
        has("dagger", "knife", "kukri", "sickle", "dirk", "stiletto") -> HandItem.DAGGER
        has("mace", "hammer", "club", "morningstar", "flail", "maul") -> HandItem.MACE
        else -> HandItem.SWORD
    }
}

/** Věc v hlavní ruce (levá strana siluety, ruka kolem x 33, y 225); pro druhou ruku se zrcadlí. */
private fun handOutline(h: HandItem): List<Outline> = when (h) {
    HandItem.SWORD -> listOf(Outline("M27 213 L14 124 L19 122 L33 211 Z"), Outline("M20 216 L40 212", false), Outline("M31 216 L35 240", false))
    HandItem.DAGGER -> listOf(Outline("M28 214 L21 176 L26 175 L33 212 Z"), Outline("M21 216 L39 213", false))
    HandItem.AXE -> listOf(Outline("M34 240 L20 136", false), Outline("M22 148 L6 140 C1 152 3 166 10 172 L24 160 Z"))
    HandItem.MACE -> listOf(Outline("M34 240 L22 162", false), Outline("M11 150 a9 9 0 1 0 18 0 a9 9 0 1 0 -18 0 Z"))
    HandItem.POLE -> listOf(Outline("M38 268 L16 100", false), Outline("M16 100 L9 80 L22 97 Z"))
    HandItem.STAFF -> listOf(Outline("M40 300 L17 110", false), Outline("M10 104 a6 6 0 1 0 12 0 a6 6 0 1 0 -12 0 Z"))
    HandItem.BOW -> listOf(Outline("M30 140 Q4 222 30 304", false), Outline("M30 140 L30 304", false))
    HandItem.WAND -> listOf(Outline("M33 224 L22 186", false), Outline("M19 183 a3 3 0 1 0 6 0 a3 3 0 1 0 -6 0 Z"))
    HandItem.SHIELD -> listOf(Outline("M4 196 L40 196 L40 224 Q22 250 4 224 Z"), Outline("M22 200 L22 240", false))
    HandItem.BUCKLER -> listOf(Outline("M8 214 a14 14 0 1 0 28 0 a14 14 0 1 0 -28 0 Z"), Outline("M19 214 a3 3 0 1 0 6 0 a3 3 0 1 0 -6 0 Z"))
    HandItem.TOWER -> listOf(Outline("M2 168 L44 168 L44 264 L2 264 Z"), Outline("M23 172 L23 260", false))
}

/** Obrys věci ve slotu (u rukou podle druhu věci, druhá ruka zrcadlově). */
private fun outlines(slot: ItemSlot, index: Int, e: InvEntry): List<Outline> {
    val n = e.name.lowercase()
    return when (slot) {
        ItemSlot.HEAD -> when {
            listOf("hat", "cap", "hood").any { it in n } -> listOf(Outline("M66 27 L134 27", false), Outline("M80 27 C80 2 120 2 120 27 Z"))
            listOf("circlet", "crown", "tiara").any { it in n } -> listOf(Outline("M78 22 L84 9 L92 19 L100 5 L108 19 L116 9 L122 22 Z"))
            else -> listOf(Outline("M74 34 C74 6 126 6 126 34 L126 40 L74 40 Z"), Outline("M100 40 L100 52", false))
        }
        ItemSlot.HEADBAND -> listOf(Outline("M77 27 C90 22 110 22 123 27", false), Outline("M96 24 a4 4 0 1 0 8 0 a4 4 0 1 0 -8 0 Z"))
        ItemSlot.EYES -> listOf(
            Outline("M84 38 a7 6 0 1 0 14 0 a7 6 0 1 0 -14 0 Z"), Outline("M102 38 a7 6 0 1 0 14 0 a7 6 0 1 0 -14 0 Z"),
            Outline("M98 37 L102 37", false),
        )
        ItemSlot.NECK -> listOf(Outline("M86 60 Q100 76 114 60", false), Outline("M96 78 a4 4 0 1 0 8 0 a4 4 0 1 0 -8 0 Z"))
        // plášť za zády – vidět je jen to, co přesahuje tělo
        ItemSlot.SHOULDERS -> listOf(Outline("M86 62 L58 74 L34 236 Q100 252 166 236 L142 74 L114 62 Z", behind = true))
        ItemSlot.CHEST -> listOf(Outline("M74 80 L90 70 L100 86 L110 70 L126 80 L124 132 L76 132 Z"))
        ItemSlot.BODY -> listOf(Outline("M72 74 L128 74 L148 266 Q100 278 52 266 Z"))
        ItemSlot.ARMOR -> buildList {
            add(Outline("M68 78 L132 78 L130 152 Q100 168 70 152 Z"))
            add(Outline("M100 82 L100 160", false))
            // těžká zbroj: nárameníky
            if (listOf("plate", "banded", "splint", "lamellar").any { it in n }) {
                add(Outline("M56 92 C58 76 72 72 80 78", false)); add(Outline("M144 92 C142 76 128 72 120 78", false))
            }
        }
        ItemSlot.BELT -> listOf(Outline("M68 184 L132 184 L132 196 L68 196 Z"), Outline("M94 182 L106 182 L106 198 L94 198 Z"))
        ItemSlot.WRISTS -> listOf(Outline("M36 178 L52 180 L49 202 L33 200 Z"), Outline("M164 178 L148 180 L151 202 L167 200 Z"))
        ItemSlot.HANDS -> listOf(
            Outline("M25 222 a9 8 0 1 0 18 0 a9 8 0 1 0 -18 0 Z"), Outline("M157 222 a9 8 0 1 0 18 0 a9 8 0 1 0 -18 0 Z"),
        )
        ItemSlot.RING -> if (index == 0) listOf(Outline("M26 238 a4 4 0 1 0 8 0 a4 4 0 1 0 -8 0 Z")) else listOf(Outline("M166 238 a4 4 0 1 0 8 0 a4 4 0 1 0 -8 0 Z"))
        ItemSlot.FEET -> listOf(Outline("M64 326 L96 326 L97 354 L58 354 Z"), Outline("M104 326 L136 326 L142 354 L103 354 Z"))
        ItemSlot.MAIN_HAND -> handOutline(handItem(e))
        ItemSlot.SHIELD -> handOutline(handItem(e)).map { Outline(mirror(it.d), it.filled, it.behind) }
    }
}

// ---------- zvířata (viewBox 200 × 200, hlava vlevo) ----------

private val QUADRUPED_PATH = PathParser().parsePathString(
    "M18 80 L28 62 L40 54 L50 54 L56 42 L62 50 L62 64 L72 82 L148 82 C160 82 166 90 166 100 L180 74 L186 78 L172 108 " +
        "L166 116 L164 172 L152 172 L150 132 L146 132 L144 172 L132 172 L130 124 L100 124 L98 172 L86 172 L86 130 " +
        "L82 130 L80 172 L68 172 L66 116 C60 108 56 98 54 90 L42 90 L28 88 Z",
).toPath()
private val BIRD_PATH = PathParser().parsePathString(
    "M56 48 C58 36 76 32 86 42 C94 50 92 60 96 68 C122 80 142 104 162 150 L152 154 C134 136 118 128 102 126 " +
        "C90 130 82 128 76 118 C66 104 64 88 70 74 L60 66 L36 62 L56 56 Z",
).toPath()
private const val BIRD_LEGS = "M90 126 L88 150 M102 126 L104 150 M82 150 L94 150 M98 150 L110 150"
private val SERPENT_PATH = PathParser().parsePathString(
    "M34 58 C36 42 60 38 68 52 C74 64 70 74 82 80 C104 90 142 76 152 100 C162 126 124 134 102 134 " +
        "C74 134 58 148 72 164 C82 174 128 172 164 166 L166 176 C124 186 70 190 56 172 C40 150 64 122 98 122 " +
        "C122 122 146 118 140 102 C134 88 102 100 76 92 C58 86 60 72 54 66 C48 60 40 70 34 66 Z",
).toPath()

/** Obrys předmětu na zvířeti (podle slotu; sedlo, brašny a uzda podle jména předmětu bez slotu). */
private fun animalOutlines(shape: BodyShape, slot: ItemSlot?, e: InvEntry): List<Outline> {
    val n = e.name.lowercase()
    if (slot == null) {
        return when {
            shape == BodyShape.SERPENT -> emptyList()
            "saddlebag" in n -> if (shape == BodyShape.BIRD) listOf(Outline("M108 96 L124 102 L120 118 L104 112 Z"))
                else listOf(Outline("M132 84 L152 84 L152 106 L132 106 Z"))
            "saddle" in n -> if (shape == BodyShape.BIRD) listOf(Outline("M86 80 C92 70 108 72 114 86 Z"))
                else listOf(Outline("M96 82 C100 66 132 66 136 82 Z"), Outline("M116 82 L116 110 M110 110 L122 110", false))
            "bridle" in n || "bit and" in n || "harness" in n -> if (shape == BodyShape.BIRD) emptyList()
                else listOf(Outline("M20 80 L44 72 L58 52", false), Outline("M44 72 L64 84", false))
            else -> emptyList()
        }
    }
    return when (shape) {
        BodyShape.QUADRUPED -> when (slot) {
            ItemSlot.HEAD -> listOf(Outline("M28 62 C32 46 50 42 58 54 L54 62 L32 64 Z"))
            ItemSlot.HEADBAND -> listOf(Outline("M32 64 C40 60 50 60 58 64", false), Outline("M42 60 a3 3 0 1 0 6 0 a3 3 0 1 0 -6 0 Z"))
            ItemSlot.EYES -> listOf(Outline("M34 68 a6 5 0 1 0 12 0 a6 5 0 1 0 -12 0 Z"))
            ItemSlot.NECK -> listOf(Outline("M54 70 L62 64 L74 86 L66 90 Z"), Outline("M64 96 a4 4 0 1 0 8 0 a4 4 0 1 0 -8 0 Z"))
            ItemSlot.SHOULDERS -> listOf(Outline("M68 80 L106 80 L102 114 L72 106 Z"))
            ItemSlot.CHEST -> listOf(Outline("M58 94 L72 86 L78 118 L66 120 Z"))
            ItemSlot.BODY -> listOf(Outline("M80 82 L150 82 L148 120 L82 120 Z"))
            ItemSlot.ARMOR -> listOf(Outline("M70 82 L154 82 C164 86 166 98 164 112 L160 124 L72 124 Z"), Outline("M54 70 L64 64 L74 86 L64 90 Z"), Outline("M112 82 L112 124", false))
            ItemSlot.BELT -> listOf(Outline("M106 82 L114 82 L116 124 L108 124 Z"))
            ItemSlot.FEET -> listOf(
                Outline("M68 166 L80 166 L80 174 L68 174 Z"), Outline("M86 166 L98 166 L98 174 L86 174 Z"),
                Outline("M132 166 L144 166 L144 174 L132 174 Z"), Outline("M152 166 L164 166 L164 174 L152 174 Z"),
            )
            else -> emptyList()
        }
        BodyShape.BIRD -> when (slot) {
            ItemSlot.HEAD -> listOf(Outline("M58 46 C62 30 84 30 88 42 Z"))
            ItemSlot.HEADBAND -> listOf(Outline("M60 48 C70 44 80 44 90 48", false))
            ItemSlot.EYES -> listOf(Outline("M66 52 a6 5 0 1 0 12 0 a6 5 0 1 0 -12 0 Z"))
            ItemSlot.NECK -> listOf(Outline("M70 72 C78 78 88 78 96 72", false), Outline("M80 82 a3 3 0 1 0 6 0 a3 3 0 1 0 -6 0 Z"))
            ItemSlot.SHOULDERS -> listOf(Outline("M82 78 L120 92 L112 114 L80 100 Z"))
            ItemSlot.CHEST -> listOf(Outline("M70 80 L84 82 L84 112 L74 110 Z"))
            ItemSlot.BODY -> listOf(Outline("M78 86 L132 106 L122 126 L82 118 Z"))
            ItemSlot.ARMOR -> listOf(Outline("M70 76 L124 96 L130 120 L86 126 C72 114 68 96 70 76 Z"))
            ItemSlot.BELT -> listOf(Outline("M94 84 L102 86 L100 126 L92 126 Z"))
            ItemSlot.FEET -> listOf(Outline("M84 140 L94 140 L94 148 L84 148 Z"), Outline("M98 140 L108 140 L108 148 L98 148 Z"))
            else -> emptyList()
        }
        BodyShape.SERPENT -> when (slot) {
            ItemSlot.HEAD -> listOf(Outline("M34 52 C38 36 62 36 66 50 Z"))
            ItemSlot.HEADBAND -> listOf(Outline("M38 54 C46 50 56 50 64 54", false))
            ItemSlot.EYES -> listOf(Outline("M42 56 a5 4 0 1 0 10 0 a5 4 0 1 0 -10 0 Z"))
            ItemSlot.NECK -> listOf(Outline("M64 66 L78 64 L84 82 L72 86 Z"))
            ItemSlot.CHEST -> listOf(Outline("M96 86 L110 84 L112 102 L98 104 Z"))
            ItemSlot.BODY -> listOf(Outline("M114 90 L148 96 L150 118 L116 124 Z"))
            ItemSlot.ARMOR -> listOf(Outline("M76 82 C104 92 140 78 150 100 C158 122 124 132 100 132 L98 122 C122 122 146 118 140 102 C134 88 102 100 76 92 Z"))
            else -> emptyList()
        }
        BodyShape.HUMANOID -> emptyList()
    }
}

/** Zrcadlení cesty kolem svislé osy siluety (x → 200 − x) – druhá ruka. Relativní oblouky „a“ jen otočí směr. */
private fun mirror(d: String): String {
    val out = StringBuilder()
    val tokens = Regex("""[A-Za-z]|-?\d*\.?\d+""").findAll(d).map { it.value }.toList()
    var cmd = 'M'
    var i = 0
    var argIndex = 0
    while (i < tokens.size) {
        val tk = tokens[i]
        if (tk[0].isLetter()) { cmd = tk[0]; argIndex = 0; out.append(tk).append(' '); i++; continue }
        val v = tk.toFloat()
        val mirrored = when (cmd) {
            'M', 'L', 'Q', 'C' -> if (argIndex % 2 == 0) 200f - v else v
            // relativní oblouk: rx ry rot large sweep dx dy – obrátit sweep a dx
            'a' -> when (argIndex % 7) { 4 -> 1f - v; 5 -> -v; else -> v }
            else -> v
        }
        out.append(mirrored).append(' ')
        argIndex++
        i++
    }
    return out.toString().trim()
}

@Composable
private fun Silhouette(inv: Inventory, width: Dp, shape: BodyShape, tiles: List<SlotPos>) {
    val fill = Nb.tag.copy(alpha = 0.65f)
    val dash = Nb.dash
    val animal = shape != BodyShape.HUMANOID
    // nasazené předměty jako obrysy na postavičce (helma, plášť, meč, štít…), konflikt slotu červeně
    val slotted = tiles.mapNotNull { p ->
        val e = inv.tileEntry(p) ?: return@mapNotNull null
        val conflict = inv.equippedIn(p.slot).sumOf { it.qty.coerceAtLeast(1) } > p.slot.capacity
        if (!inv.isWorking(e) && !conflict) return@mapNotNull null
        Triple(e.id + p.slot.name + p.index, if (animal) animalOutlines(shape, p.slot, e) else outlines(p.slot, p.index, e), conflict)
    }
    // zvíře: sedlo, brašny a uzda (nasazené předměty bez slotu, podle jména)
    val tack = if (!animal) emptyList() else inv.entries.filter { it.equipped && it.slot == null }
        .map { Triple(it.id + "-tack", animalOutlines(shape, null, it), false) }.filter { it.second.isNotEmpty() }
    val items = slotted + tack
    // každý obrys se při nasazení dokreslí tahem
    val progress = items.map { (key, _, _) -> key(key) { rememberRevealProgress(key, durationMs = 600) } }
    Canvas(Modifier.size(width, if (animal) width * .8f else width * 1.8f)) {
        val k = size.width / 200f
        val line = (2.4f).coerceAtLeast(1.3.dp.toPx() / k)
        scale(k, k, pivot = Offset.Zero) { translate(top = if (animal) -30f else 0f) {
            fun drawItems(behind: Boolean) = items.forEachIndexed { i, (_, list, conflict) ->
                val color = if (conflict) Nb.error else Nb.accent
                val p = progress[i].value
                list.filter { it.behind == behind }.forEach { o ->
                    val path = PathParser().parsePathString(o.d).toPath()
                    if (o.filled && p >= 1f) drawPath(path, color.copy(alpha = if (behind) .10f else .16f))
                    if (p >= 1f) drawPath(path, color, style = Stroke(line, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    else {
                        val m = PathMeasure().apply { setPath(path, false) }
                        val dst = androidx.compose.ui.graphics.Path()
                        m.getSegment(0f, m.length * p, dst, true)
                        drawPath(dst, color, style = Stroke(line, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    }
                }
            }
            drawItems(behind = true)
            val stroke = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f)))
            if (animal) {
                val body = when (shape) {
                    BodyShape.BIRD -> BIRD_PATH
                    BodyShape.SERPENT -> SERPENT_PATH
                    else -> QUADRUPED_PATH
                }
                drawPath(body, fill)
                drawPath(body, dash, style = stroke)
                if (shape == BodyShape.BIRD) drawPath(PathParser().parsePathString(BIRD_LEGS).toPath(), dash, style = Stroke(2f, cap = StrokeCap.Round))
                // oko
                val eye = when (shape) { BodyShape.BIRD -> Offset(72f, 52f); BodyShape.SERPENT -> Offset(47f, 56f); else -> Offset(40f, 68f) }
                drawCircle(dash, 2.5f, eye)
            } else {
                // neprůhledný podklad těla – plášť za zády přes postavu neprosvítá
                if (items.any { (_, list, _) -> list.any { it.behind } }) drawPath(BODY_PATH, Nb.inset)
                drawCircle(fill, 24f, Offset(100f, 36f))
                drawCircle(dash, 24f, Offset(100f, 36f), style = stroke)
                drawPath(BODY_PATH, fill)
                drawPath(BODY_PATH, dash, style = stroke)
            }
            drawItems(behind = false)
        } }
    }
}

/**
 * Sheet slotu: které předměty v něm jsou (volba, který platí), odložení ostatních do batohu, nasazení jiného.
 * Mobil: bottom sheet, desktop: dialog. [slot] null = předměty bez slotu.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SlotSheet(state: AppState, inv: Inventory, slot: ItemSlot?, wide: Boolean, onOpen: (InvEntry) -> Unit, onDismiss: () -> Unit) {
    if (wide) {
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = Nb.panel,
            text = { Column(Modifier.verticalScroll(rememberScrollState())) { SlotSheetContent(state, inv, slot, onOpen, onDismiss) } },
            confirmButton = { TextButton(onClick = onDismiss) { Text(str.close, color = Nb.ink) } },
        )
    } else {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Nb.panel,
        ) {
            Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 22.dp).verticalScroll(rememberScrollState())) {
                SlotSheetContent(state, inv, slot, onOpen, onDismiss)
            }
        }
    }
}

@Composable
private fun SlotSheetContent(state: AppState, inv: Inventory, slot: ItemSlot?, onOpen: (InvEntry) -> Unit, onDismiss: () -> Unit) {
    val s = str
    val worn = if (slot == null) inv.entries.filter { it.equipped && it.slot == null } else inv.equippedIn(slot)
    val conflict = slot != null && worn.sumOf { it.qty.coerceAtLeast(1) } > slot.capacity
    val candidates = inv.entries.filter { !it.equipped && (it.slot == slot || (slot != null && it.slot == null)) }
    var showCandidates by remember(slot) { mutableStateOf(worn.isEmpty()) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                MonoLabel(s.slotSheetLabel, color = Nb.rust)
                Text(
                    slot?.let(s::itemSlot) ?: s.noSlot,
                    fontFamily = LocalNbFonts.current.serif, fontWeight = FontWeight.Bold, fontSize = 26.sp, color = Nb.ink,
                )
            }
            NbIconButton(NbIcons.Close, s.close, onDismiss, Nb.ink)
        }
        if (conflict) Text(s.conflictExplain, fontSize = 14.sp, color = Nb.error)
        worn.forEach { e ->
            val working = inv.isWorking(e)
            Row(
                Modifier.fillMaxWidth().clip(Nb.shape)
                    .then(if (working) Modifier.background(Nb.inset).dashedBorder() else Modifier)
                    .clickable { if (slot != null) state.preferInSlot(inv.id, e.id) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // přepínač „tenhle platí“ (u slotů pro jeden předmět)
                Box(
                    Modifier.size(22.dp).clip(CircleShape).border(2.dp, Nb.accent, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { if (working) Box(Modifier.size(10.dp).clip(CircleShape).background(Nb.accent)) }
                Column(Modifier.weight(1f)) {
                    Text(e.name, fontSize = 15.5.sp, fontWeight = FontWeight.SemiBold, color = Nb.ink)
                    val bonuses = e.bonuses.joinToString(", ") { bonusText(s, it) }
                    val detail = listOfNotNull(bonuses.ifEmpty { null }, formatWeight(e.weightLb).takeIf { it != "—" }).joinToString(" · ")
                    Text(
                        if (working) detail else listOf(detail, s.ignoredNotWorn).filter { it.isNotEmpty() }.joinToString(" · "),
                        fontSize = 12.5.sp, color = if (working) Nb.inkMuted else Nb.error,
                    )
                }
                Text(
                    s.open, Modifier.clip(Nb.shape).clickable { onOpen(e) }.padding(4.dp),
                    fontSize = 13.sp, color = Nb.accent, textDecoration = TextDecoration.Underline,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (conflict) NbPrimaryButton(s.moveOthersToCarried, { state.unequipExtra(inv.id, slot) }, icon = null)
            if (!showCandidates) NbDashedButton(s.equipAnother, { showCandidates = true }, icon = NbIcons.Plus)
        }
        if (showCandidates) {
            MonoLabel(s.equipAnother)
            if (candidates.isEmpty()) Text(s.noSlotCandidates, fontSize = 13.5.sp, color = Nb.inkMuted)
            candidates.forEach { e ->
                Row(Modifier.fillMaxWidth().bottomRule(Nb.ruleSoft).padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(e.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Nb.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        val bonuses = e.bonuses.joinToString(", ") { bonusText(s, it) }
                        if (bonuses.isNotEmpty()) Text(bonuses, fontSize = 12.5.sp, color = Nb.inkMuted)
                    }
                    NbDashedButton(s.equip, {
                        // nasadit do slotu; co se pak nevejde, jde do batohu
                        state.preferInSlot(inv.id, e.id, slot)
                        if (slot != null) state.unequipExtra(inv.id, slot)
                    })
                }
            }
        }
    }
}

/** Štítek slotu v seznamu: olivový (platí), červený (navíc v plném slotu), u neseného mono „neseno“. */
@Composable
fun SlotBadge(inv: Inventory, e: InvEntry) {
    val s = str
    if (!e.equipped) {
        Text(s.carriedLabel, fontFamily = LocalNbFonts.current.mono, fontSize = 12.5.sp, color = Nb.inkMuted, maxLines = 1)
        return
    }
    val label = e.slot?.let(s::itemSlot) ?: s.equippedOnly
    Text(
        label,
        Modifier.clip(Nb.shape).background(if (inv.isWorking(e)) Nb.accent else Nb.error).padding(horizontal = 8.dp, vertical = 3.dp),
        color = Color.White, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis,
    )
}
