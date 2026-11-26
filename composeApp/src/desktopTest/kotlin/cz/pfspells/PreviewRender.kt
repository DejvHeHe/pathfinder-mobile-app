package cz.pfspells

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import androidx.compose.ui.use
import com.russhwolf.settings.PreferencesSettings
import cz.pfspells.data.InvEntry
import cz.pfspells.data.InventoryStore
import cz.pfspells.data.ItemCategory
import cz.pfspells.data.ItemRepository
import cz.pfspells.ui.ClassPickerDialog
import cz.pfspells.ui.PfTheme
import cz.pfspells.ui.RacePickerDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.skia.EncodedImageFormat
import java.io.File
import java.util.prefs.Preferences
import kotlin.test.Test

/**
 * Vykreslí ukázkové obrazovky do composeApp/build/previews (ruční kontrola designu).
 * Spouští se jen s -Dpreviews=true, aby nezdržoval běžné testy.
 */
class PreviewRender {
    @Test
    fun render() = runBlocking {
        if (System.getProperty("previews") != "true") return@runBlocking
        val node = Preferences.userRoot().node("pfspells-preview-test")
        node.clear()
        val settings = PreferencesSettings(node)
        val state = withContext(Dispatchers.Main) {
            AppState(inventoryStore = InventoryStore(settings), itemRepo = ItemRepository(settings), settings = settings)
                .also { it.changeLang(Lang.EN) }
        }
        while (state.db !is DbState.Ready) delay(50)
        val elara = withContext(Dispatchers.Main) {
            val thomas = state.createCharacter("Brother Thomas", "cleric", 7)
            state.addClass(thomas.id, "fighter", 2)
            state.setAbility(thomas.id, cz.pfspells.data.Ability.WIS, 17)
            state.setAbility(thomas.id, cz.pfspells.data.Ability.STR, 14)
            state.setRace(thomas.id, "dwarf")
            state.setAlignment(thomas.id, cz.pfspells.data.Alignment.LG)
            state.setDeity(thomas.id, "Torag")
            mapOf("knowledge.religion" to 5, "heal" to 4, "diplomacy" to 3, "sense motive" to 0, "intimidate" to 2, "craft:armor" to 2)
                .filterValues { it > 0 }.forEach { (k, v) -> state.setSkillRanks(thomas.id, k, v) }
            state.updateCharacter(thomas.id) {
                it.copy(
                    feats = listOf(
                        cz.pfspells.data.FeatPick("power-attack", "Power Attack"),
                        cz.pfspells.data.FeatPick("dodge", "Dodge"),
                        cz.pfspells.data.FeatPick("weapon-focus", "Weapon Focus", note = "morningstar"),
                        cz.pfspells.data.FeatPick(null, "Blessed by Iomedae", text = "Once per day re-roll a Will save."),
                    ),
                )
            }
            state.ensureFeats()
            state.setSubclass(thomas.id, "cleric", cz.pfspells.data.SubclassSlot.DOMAIN1, "fire")
            state.setSubclass(thomas.id, "cleric", cz.pfspells.data.SubclassSlot.DOMAIN2, "magic")
            state.setSubclass(thomas.id, "cleric", cz.pfspells.data.SubclassSlot.SUBDOMAIN2, "arcane")
            state.setBonusSpellCount(thomas.id, "cleric", "burning-hands", 1)
            state.setAbility(thomas.id, cz.pfspells.data.Ability.CHA, 14)
            state.addTracker(thomas.id, "Wand of Cure Light Wounds", 50, cz.pfspells.data.ResourceUnit.POINTS, "charges")
            state.addTracker(thomas.id, "Pearl of Power (1st)", 1, cz.pfspells.data.ResourceUnit.PER_DAY, "")
            state.setBonusSpellCount(thomas.id, "cleric", "fireball", 1)
            val nyx = state.createCharacter("Nyx", "sorcerer", 4)
            state.setAbility(nyx.id, cz.pfspells.data.Ability.CHA, 18)
            state.setSubclass(nyx.id, "sorcerer", cz.pfspells.data.SubclassSlot.BLOODLINE, "arcane")
            listOf("detect-magic", "light", "magic-missile", "shield", "mage-armor", "invisibility")
                .forEach { state.addSpell(nyx.id, null, it) }
            val d = state.createCharacter("Elara", "wizard", 5)
            state.setAbility(d.id, cz.pfspells.data.Ability.INT, 16)
            state.setAbility(d.id, cz.pfspells.data.Ability.DEX, 14)
            state.setAbility(d.id, cz.pfspells.data.Ability.WIS, 12)
            state.setRace(d.id, "elf")
            state.setAlignment(d.id, cz.pfspells.data.Alignment.NG)
            state.setDeity(d.id, "Nethys")
            mapOf("spellcraft" to 5, "knowledge.arcana" to 5, "knowledge.planes" to 3, "perception" to 2, "linguistics" to 1)
                .forEach { (k, v) -> state.setSkillRanks(d.id, k, v) }
            state.updateCharacter(d.id) {
                it.copy(feats = listOf(
                    cz.pfspells.data.FeatPick("spell-focus", "Spell Focus", note = "evocation"),
                    cz.pfspells.data.FeatPick("scribe-scroll", "Scribe Scroll"),
                    cz.pfspells.data.FeatPick("improved-initiative", "Improved Initiative"),
                ))
            }
            state.setSubclass(d.id, "wizard", cz.pfspells.data.SubclassSlot.SCHOOL, "evocation")
            state.setSubclass(d.id, "wizard", cz.pfspells.data.SubclassSlot.OPPOSITION1, "enchantment")
            state.setSubclass(d.id, "wizard", cz.pfspells.data.SubclassSlot.OPPOSITION2, "illusion")
            mapOf("detect-magic" to 1, "light" to 1, "magic-missile" to 2, "shield" to 1, "mage-armor" to 1,
                "web" to 1, "invisibility" to 1, "fireball" to 1).forEach { (k, v) -> state.setSpellCount(d.id, null, k, v) }
            state.toggleFavorite("fireball"); state.toggleFavorite("magic-missile"); state.toggleFavorite("ablative-barrier")
            d
        }
        withContext(Dispatchers.Main) { state.ensureItems() }
        while (state.items !is ItemDbState.Ready) delay(50)
        val inv = withContext(Dispatchers.Main) {
            state.createInventory("Party stash")
            val i = state.createInventory("Elara")
            state.linkInventory(elara.id, i.id)
            val db = state.itemDb!!
            listOf("Longsword" to 1, "Shortbow" to 1, "Arrows (20)" to 1, "Leather armor" to 1, "Backpack, common" to 1,
                "Bedroll" to 1, "Rope, hemp (50 ft.)" to 1, "Torch" to 3, "Trail rations (per day)" to 5, "Bag of Holding (Type I)" to 1)
                .forEach { (n, q) -> (db.findByName(n) ?: db.items.firstOrNull { it.name.startsWith(n.substringBefore(" (").substringBefore(",")) })?.let { state.addItem(i.id, it, q) } }
            state.addCustomEntry(i.id, InvEntry("p", null, "Potion of Cure Light Wounds", ItemCategory.POTION.name, 2, 50.0, null, info = "1d8+1"))
            state.updateInventory(i.id) { it.copy(gp = 124, sp = 6, cp = 12, notes = "Chest at the Golden Stag inn – 2 scrolls, spare rope.") }
            state.saveCustomItem(cz.pfspells.data.CustomItem("p1", "Elven rope", ItemCategory.GEAR.name, 25.0, 3.0, "50 ft., DC 15 to break"))
            state.inventory(i.id)!!
        }
        // čarodějnice s hexy a orákulum se zjeveními (Foundry)
        withContext(Dispatchers.Main) { state.ensureClassAbilities() }
        while (state.abilityDb == null) delay(50)
        val morgana = withContext(Dispatchers.Main) {
            val m = state.createCharacter("Morgana", "witch", 10)
            state.setAbility(m.id, cz.pfspells.data.Ability.INT, 18)
            listOf("evil-eye", "slumber", "cackle", "misfortune", "agony").forEach { id -> state.abilityDb!!.get(id)?.let { state.addClassAbility(m.id, "witch", it) } }
            state.addClass(m.id, "oracle", 3)
            state.setSubclass(m.id, "oracle", cz.pfspells.data.SubclassSlot.MYSTERY, "flame")
            state.abilityDb!!.forClass("oracle").filter { "flame" in it.mysteries && it.uses != null }.take(1)
                .forEach { state.addClassAbility(m.id, "oracle", it) }
            state.character(m.id)!!
        }
        // půlčík s alternativními rasovými traity (AoN)
        withContext(Dispatchers.Main) { state.ensureRacialTraits() }
        while (state.traitDb == null) delay(50)
        val pip = withContext(Dispatchers.Main) {
            val p = state.createCharacter("Pip", "rogue", 3)
            state.setRace(p.id, "halfling")
            state.traitDb!!.forRace("halfling").filter { it.name in setOf("Halfling Jinx", "Underfoot Dodger") }.forEach { state.addRacialTrait(p.id, it) }
            state.character(p.id)!!
        }
        val aria = withContext(Dispatchers.Main) {
            val h = state.createCharacter("Aria", "sorcerer", 3)
            state.setRace(h.id, "human")
            state.traitDb!!.get("human:dual-talent")?.let { state.addRacialTrait(h.id, it) }
            state.setRacialTraitAbility(h.id, "human:dual-talent", 0, cz.pfspells.data.Ability.CHA)
            state.character(h.id)!!
        }
        // podtřídy z Foundry: patron (Morgana), spirit, blessingy warpriesta, doména inquisitora
        val kaya = withContext(Dispatchers.Main) {
            state.setSubclass(morgana.id, "witch", cz.pfspells.data.SubclassSlot.PATRON, "agility")
            val k = state.createCharacter("Kaya", "shaman", 5)
            state.setAbility(k.id, cz.pfspells.data.Ability.WIS, 16)
            state.setSubclass(k.id, "shaman", cz.pfspells.data.SubclassSlot.SPIRIT, "flame")
            state.setSubclass(k.id, "shaman", cz.pfspells.data.SubclassSlot.WANDERING_SPIRIT, "life")
            state.character(k.id)!!
        }
        val brannoc = withContext(Dispatchers.Main) {
            val b = state.createCharacter("Brannoc", "warpriest", 10)
            state.setDeity(b.id, "Iomedae")
            state.setSubclass(b.id, "warpriest", cz.pfspells.data.SubclassSlot.BLESSING1, "glory")
            state.setSubclass(b.id, "warpriest", cz.pfspells.data.SubclassSlot.BLESSING2, "war")
            state.addClass(b.id, "inquisitor", 3)
            state.setSubclass(b.id, "inquisitor", cz.pfspells.data.SubclassSlot.DOMAIN1, "sun")
            state.character(b.id)!!
        }
        val out = File("build/previews").apply { mkdirs() }

        suspend fun shot(name: String, w: Int, h: Int, overlay: (@Composable () -> Unit)? = null, nav: AppState.() -> Unit) {
            withContext(Dispatchers.Main) { state.nav() }
            ImageComposeScene(w, h, Density(1f)) {
                App(state)
                overlay?.let { PfTheme { CompositionLocalProvider(LocalStrings provides state.strings) { it() } } }
            }.use { scene ->
                var t = 0L
                repeat(12) { scene.render(t); t += 50_000_000; delay(40) }
                val img = scene.render(t)
                File(out, "$name.png").writeBytes(img.encodeToData(EncodedImageFormat.PNG)!!.bytes)
            }
        }

        shot("phone-decks", 390, 844) { openRoot(Screen.Decks) }
        shot("phone-deck", 390, 844) { openRoot(Screen.CharacterDetail(elara.id)) }
        shot("phone-spell", 390, 844) { openRoot(Screen.CharacterDetail(elara.id)); showSpell("fireball", elara.id) }
        shot("phone-picker", 390, 844) { openRoot(Screen.CharacterDetail(elara.id)); push(Screen.Picker(elara.id)) }
        shot("desktop", 1440, 1000) { openRoot(Screen.CharacterDetail(elara.id)); showSpell("fireball", elara.id) }
        shot("desktop-compendium", 1440, 1000) { openRoot(Screen.Compendium) }
        shot("phone-inventory", 390, 844) { switchSection(Section.INVENTORY); push(Screen.InventoryDetail(inv.id)) }
        shot("phone-itempicker", 390, 844) { itemFilter = ItemFilter(category = ItemCategory.WEAPON); push(Screen.ItemPicker(inv.id)) }
        shot("desktop-inventory", 1440, 1000) { openRoot(Screen.InventoryDetail(inv.id)) }
        shot("desktop-itempicker", 1440, 1000) {
            openRoot(Screen.ItemPicker(inv.id)); itemFilter = ItemFilter(category = ItemCategory.WEAPON, proficiency = setOf("Simple", "Martial"), maxPrice = 50.0)
            showItem(itemDb!!.findByName("Longsword")!!.id, inv.id)
        }
        shot("desktop-wondrous", 1440, 1000) {
            openRoot(Screen.ItemPicker(inv.id)); itemFilter = ItemFilter(category = ItemCategory.WONDROUS)
            showItem(itemDb!!.items.first { it.name.startsWith("Bag of Holding") }.id, inv.id)
        }
        shot("phone-decks-inventory-tab", 390, 844) { switchSection(Section.INVENTORY) }
        shot("phone-about", 390, 844) { openRoot(Screen.Decks); push(Screen.About) }
        shot("desktop-about", 1440, 1000) { openRoot(Screen.About) }

        // čeština
        withContext(Dispatchers.Main) { state.changeLang(Lang.CS) }
        shot("cs-phone-decks", 390, 844) { switchSection(Section.SPELLBOOK) }
        shot("cs-phone-spell", 390, 844) { openRoot(Screen.CharacterDetail(elara.id)); showSpell("fireball", elara.id) }
        shot("cs-phone-inventory", 390, 844) { switchSection(Section.INVENTORY); push(Screen.InventoryDetail(inv.id)) }
        shot("cs-desktop", 1440, 1000) { openRoot(Screen.CharacterDetail(elara.id)); showSpell("fireball", elara.id) }
        shot("cs-desktop-inventory", 1440, 1000) {
            openRoot(Screen.InventoryDetail(inv.id)); showItem(itemDb!!.findByName("Longsword")!!.id, inv.id)
        }
        shot("cs-desktop-itempicker", 1440, 1000) {
            openRoot(Screen.ItemPicker(inv.id)); itemFilter = ItemFilter(category = ItemCategory.WEAPON, proficiency = setOf("Simple", "Martial"), maxPrice = 50.0)
        }
        shot("cs-phone-multiclass", 390, 844) { openRoot(Screen.CharacterDetail(characters.first { it.name == "Brother Thomas" }.id)) }
        shot("cs-desktop-multiclass", 1440, 1000) { openRoot(Screen.CharacterDetail(characters.first { it.name == "Brother Thomas" }.id)) }
        shot("phone-spontaneous", 390, 844) { openRoot(Screen.CharacterDetail(characters.first { it.name == "Nyx" }.id)) }
        shot("cs-phone-game-spontaneous", 390, 844) {
            val n = characters.first { it.name == "Nyx" }
            gameMode = true
            setCast(n.id, cz.pfspells.data.Character.slotKey("sorcerer", 1), 4)
            openRoot(Screen.CharacterDetail(n.id))
        }
        shot("cs-phone-game-prepared", 390, 844) {
            gameMode = true
            setCast(elara.id, cz.pfspells.data.Character.castKey("wizard", "magic-missile"), 1)
            setCast(elara.id, cz.pfspells.data.Character.castKey("wizard", "shield"), 1)
            openRoot(Screen.CharacterDetail(elara.id))
        }
        shot("cs-desktop-game", 1440, 1000) { gameMode = true; openRoot(Screen.CharacterDetail(elara.id)) }
        val thomasId = state.characters.first { it.name == "Brother Thomas" }.id
        shot("cs-phone-feats", 390, 1400) { gameMode = false; openRoot(Screen.CharacterDetail(thomasId)) }
        shot("cs-phone-featpicker", 390, 844) { openRoot(Screen.CharacterDetail(thomasId)); push(Screen.FeatPicker(thomasId)) }
        shot("cs-desktop-feat", 1440, 1000) { featFilter = FeatFilter(query = "power att"); openRoot(Screen.FeatPicker(thomasId)); showFeat("power-attack", thomasId) }
        shot("cs-phone-domains", 390, 2000) { gameMode = false; openRoot(Screen.CharacterDetail(thomasId)) }
        shot("cs-phone-bloodline", 390, 1300) { gameMode = false; openRoot(Screen.CharacterDetail(characters.first { it.name == "Nyx" }.id)) }
        shot("cs-phone-wizard", 390, 1700) { gameMode = false; openRoot(Screen.CharacterDetail(elara.id)) }
        shot("cs-phone-resources-game", 390, 1500) {
            gameMode = true
            setCast(thomasId, "res:cleric-channel", 2)
            openRoot(Screen.CharacterDetail(thomasId))
        }
        shot("cs-phone-myitems", 390, 844) { itemFilter = ItemFilter(mine = true); openRoot(Screen.Decks); push(Screen.InventoryDetail(inv.id)); push(Screen.ItemPicker(inv.id)) }
        shot("cs-desktop-myitem", 1440, 1000) {
            openRoot(Screen.ItemPicker(inv.id)); itemFilter = ItemFilter(mine = true); showItem("my-p1", inv.id)
        }
        shot("cs-phone-deitypicker", 390, 844, overlay = { cz.pfspells.ui.DeityPickerDialog("Torag", onDismiss = {}, onCustom = {}) { } }) { openRoot(Screen.Decks) }
        shot("cs-phone-racial-traits", 390, 844, overlay = {
            cz.pfspells.ui.RacialTraitsDialog(state, state.character(pip.id)!!) { }
        }) { openRoot(Screen.Decks) }
        shot("desktop-racial-traits", 1440, 1000, overlay = {
            cz.pfspells.ui.RacialTraitsDialog(state, state.character(pip.id)!!) { }
        }) { openRoot(Screen.CharacterDetail(pip.id)) }
        shot("cs-phone-race-panel", 390, 1000) { openRoot(Screen.CharacterDetail(pip.id)) }
        shot("cs-phone-dual-talent", 390, 844, overlay = {
            cz.pfspells.ui.RacialTraitsDialog(state, state.character(aria.id)!!) { }
        }) { openRoot(Screen.Decks) }
        shot("cs-phone-race-dual-talent", 390, 900) { openRoot(Screen.CharacterDetail(aria.id)) }
        shot("cs-phone-hexes", 390, 2600) { gameMode = false; openRoot(Screen.CharacterDetail(morgana.id)) }
        shot("cs-phone-hexpicker", 390, 844, overlay = {
            cz.pfspells.ui.ClassAbilityPickerDialog(state, state.character(morgana.id)!!, state.character(morgana.id)!!.classes[0],
                cz.pfspells.data.AbilityGroup.HEXES, onDismiss = {}) { }
        }) { openRoot(Screen.Decks) }
        shot("desktop-revelations", 1440, 1000, overlay = {
            cz.pfspells.ui.ClassAbilityPickerDialog(state, state.character(morgana.id)!!, state.character(morgana.id)!!.classes[1],
                cz.pfspells.data.AbilityGroup.REVELATIONS, onDismiss = {}) { }
        }) { openRoot(Screen.CharacterDetail(morgana.id)) }
        shot("cs-phone-feat-prereqs", 390, 1000) { openRoot(Screen.CharacterDetail(thomasId)); showFeat("cloak-of-stone", thomasId) }
        shot("cs-phone-database", 390, 844) { openRoot(Screen.Decks); push(Screen.Database) }
        // výběr classy s podtřídou (nová postava – klerik)
        val subIdx = (state.db as DbState.Ready).db.subclasses
        shot("cs-phone-classpicker-subclass", 390, 844, overlay = {
            ClassPickerDialog("Nová postava", "Vytvořit", withName = true, subclasses = subIdx, initialClassId = "cleric", initialRace = "half-elf", onDismiss = {}) { }
        }) { openRoot(Screen.Decks) }
        shot("cs-phone-classpicker-druid", 390, 844, overlay = {
            ClassPickerDialog(
                "Nová postava", "Vytvořit", withName = true, subclasses = subIdx, initialClassId = "druid",
                initialSubclass = mapOf(cz.pfspells.data.SubclassSlot.DOMAIN1 to "fire"), onDismiss = {},
            ) { }
        }) { openRoot(Screen.Decks) }
        shot("cs-desktop-classpicker-cleric", 1440, 1000, overlay = {
            ClassPickerDialog(
                "Nová postava", "Vytvořit", withName = true, subclasses = subIdx, initialClassId = "cleric",
                initialSubclass = mapOf(cz.pfspells.data.SubclassSlot.DOMAIN1 to "sun", cz.pfspells.data.SubclassSlot.DOMAIN2 to "law"),
                characterDeity = "Iomedae", onDismiss = {},
            ) { }
        }) { switchSection(Section.SPELLBOOK); openRoot(Screen.Decks) }
        shot("cs-phone-racepicker", 390, 844, overlay = { RacePickerDialog("dwarf", onDismiss = {}) { } }) { openRoot(Screen.Decks) }
        shot("cs-desktop-racepicker", 1440, 1000, overlay = { RacePickerDialog(null, onDismiss = {}) { } }) { openRoot(Screen.Decks) }
        val spellDb = (state.db as DbState.Ready).db
        shot("cs-phone-spirit", 390, 2900) { gameMode = false; openRoot(Screen.CharacterDetail(kaya.id)) }
        shot("cs-phone-spirit-game", 390, 2900) {
            gameMode = true
            setCast(kaya.id, cz.pfspells.data.Character.castKey("shaman", "spirit:1"), 1)
            openRoot(Screen.CharacterDetail(kaya.id))
        }
        shot("cs-phone-warpriest-subclass", 390, 844, overlay = {
            val b = state.character(brannoc.id)!!
            cz.pfspells.ui.SubclassDialog(state, spellDb, b, b.classes[0]) { }
        }) { gameMode = false; openRoot(Screen.Decks) }
        shot("cs-phone-inquisitor-subclass", 390, 844, overlay = {
            val b = state.character(brannoc.id)!!
            cz.pfspells.ui.SubclassDialog(state, spellDb, b, b.classes[1]) { }
        }) { openRoot(Screen.Decks) }
        shot("cs-phone-warpriest", 390, 1200) { openRoot(Screen.CharacterDetail(brannoc.id)) }
        shot("cs-phone-spirit-hexpicker", 390, 844, overlay = {
            val k = state.character(kaya.id)!!
            cz.pfspells.ui.ClassAbilityPickerDialog(state, k, k.classes[0], cz.pfspells.data.AbilityGroup.HEXES, onDismiss = {}) { }
        }) { openRoot(Screen.Decks) }
        shot("cs-phone-inquisition", 390, 844, overlay = {
            val b = state.character(brannoc.id)!!
            val e = b.classes[1].copy(subclass = mapOf(cz.pfspells.data.SubclassSlot.INQUISITION to "banishment"))
            cz.pfspells.ui.SubclassDialog(state, spellDb, b, e) { }
        }) { openRoot(Screen.Decks) }
        shot("cs-phone-witch-patron", 390, 2400) { openRoot(Screen.CharacterDetail(morgana.id)) }
        shot("cs-phone-shaman-subclass", 390, 1000, overlay = {
            val k = state.character(kaya.id)!!
            cz.pfspells.ui.SubclassDialog(state, spellDb, k, k.classes[0]) { }
        }) { openRoot(Screen.Decks) }
        shot("cs-phone-classpicker-witch", 390, 844, overlay = {
            ClassPickerDialog(
                "Nová postava", "Vytvořit", withName = false, subclasses = subIdx, initialClassId = "witch",
                initialSubclass = mapOf(cz.pfspells.data.SubclassSlot.PATRON to "agility"), onDismiss = {},
            ) { }
        }) { openRoot(Screen.Decks) }
        shot("desktop-classpicker-wizard", 1440, 1000, overlay = {
            ClassPickerDialog("New character", "Create", withName = true, subclasses = subIdx, initialClassId = "wizard", onDismiss = {}) { }
        }) { changeLang(Lang.EN); openRoot(Screen.Decks) }
        node.removeNode()
    }
}
