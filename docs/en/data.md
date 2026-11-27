# Data

[Čeština](../data.md) | **English**

All data is **bundled with the app** (`composeApp/src/commonMain/composeResources/files/` and generated Kotlin tables),
so the app works offline. Part of it can be re-downloaded from the source with a button in the app.

| File | Content | Source | Updatable in the app |
|---|---|---|---|
| `SpellList.json` (6 MB) | 3,040 records → 3,030 spells with a class list | [SpellTable-pf1e](https://github.com/iociveteres/SpellTable-pf1e) `data/SpellList.json` (MIT, data from Archives of Nethys) | yes |
| `Feats.json` | 3,442 feats | [Archives of Nethys](https://aonprd.com/Feats.aspx) – `tools/build_feats.py` | no (bundled only) |
| `ItemsBase.json` (0.7 MB) | 1,541 entries: gear, clothing, tools, food, alchemy, herbs, animals, armor and shields | [Foundry VTT PF1](https://gitlab.com/foundryvtt_pathfinder1e/foundryvtt-pathfinder1) `packs/items`, `packs/armors-and-shields` (OGL) | no (bundled only) |
| `Weapons.csv` | 243 weapons | [Vireche/randomitemgenerator](https://github.com/Vireche/randomitemgenerator) `data/weapons.csv` (d20pfsrd/AoN) | yes |
| `Ammunition.csv` | 36 kinds of ammunition | same, `data/ammunition.csv` | yes |
| `MagicItems.csv` (4 MB) | 4,241 magic items | same, `data/magic_items_full - Updated 19Jan2020.csv` (d20pfsrd "magic_items_full") | yes |
| `CastingTable.kt` | spells per day / known of 27 casting classes | Archives of Nethys class pages – `tools/build_casting.py` | no |
| `ClassTable.kt` | 49 classes: BAB, saves, HD, skill points, class skills, proficiencies | Foundry VTT PF1 `packs/classes` – `tools/build_classes.py` | no |
| `RaceTable.kt` | 80 races: size, speed, ability and skill modifiers, vision | Foundry VTT PF1 `packs/races` – `tools/build_races.py` | no |
| `ClassAbilityTable.kt`, `ClassAbilities.json` (0.3 MB) | 325 domain / bloodline / school powers; 420 hexes and revelations with text | Foundry VTT PF1 `packs/class-abilities` – `tools/build_class_abilities.py` | no |
| `PrestigeTable.kt`, `PrestigeClasses.json` (0.8 MB) | 118 prestige classes: table by level, spellcasting, requirements; description and features | [Archives of Nethys](https://aonprd.com/PrestigeClasses.aspx) – `tools/build_prestige_classes.py` | no |
| `Archetypes.json` (4.0 MB) | 1,252 archetypes of 40 classes (replaced features, summary, source, PFS, text) | [Archives of Nethys](https://aonprd.com/Archetypes.aspx) – `tools/build_archetypes.py` | no |
| `RacialTraitTable.kt`, `RacialTraits.json` (0.4 MB) | default traits of 77 races; 632 alternate racial traits | Archives of Nethys race pages – `tools/build_racial_traits.py` | no |
| `DeityTable.kt` | 463 deities: pantheon, alignment, areas, domains, subdomains, favored weapon | [Archives of Nethys](https://aonprd.com/DeitiesByGroup.aspx) – `tools/build_deities.py` | no |

## Spells

### Source format
An array of JSON objects with, among others:
`Name`, `School`, `Subschool`, `Descriptors`, `Casting time`, `Components`, `Range`, `Effect`, `Target`, `Duration`,
`Saving throw`, `Spell Resistance`, `Description` (paragraphs separated by `\n`), `Short description`, `Mythic`, `Source`,
`Url aon` and `access_ways.Level` = a list of `[spell list, level]`, e.g. `["sorcerer/wizard", 3]`.

### Conversion (`SpellParser`)
- Values `"None"`, `"NULL"` and empty → `null`.
- `levels`: map spell list → level; exceptions in the source are unified (`wizard` → `sorcerer/wizard`, `oracle` → `cleric/oracle`,
  `summoner (unchained)` → `unchained summoner`). On duplicates the lower level wins.
- Spells without a class list (only SLAs, domains) are skipped.
- `id` = slug of the name (`Abadar's Truthtelling` → `abadar-s-truthtelling`), collisions get `-2`, `-3`.
- The parser rejects a file with ≤ 100 spells (protection against downloading garbage).
- `pfsLegal` from the `PFS legal` field (~340 spells are not allowed in Pathfinder Society; when missing, treated as legal).

Domains, subdomains, mysteries, bloodlines and elemental schools are read from the source as well (`access_ways.*` →
`Spell.subLevels`, key "domain:fire" → spell level). The source has errors the parser fixes: spaces and capitals (" war",
"Plant"), typos ("aristocra", "kyton (evil, kaw)"), "(u)" and "juju(PZO…)" variants, glued records ("Elemental School air"
in bloodlines) are dropped. Result: 37 domains, 113 subdomains, 34 mysteries, 25 bloodlines, 7 elemental schools.

| Class | Subclass | Bonus spells |
|---|---|---|
| cleric | 2 domains (+ a subdomain for each), limited to the deity's domains and subdomains | domain slot: 1 domain spell per spell level (`ClassEntry.bonusSpells`) |
| druid | Nature Bond domain – Air, Animal, Earth, Fire, Plant, Water, Weather (+ subdomain, not Metal) | domain slot as cleric |
| oracle | mystery | known spell of level N from level 2N |
| sorcerer | bloodline | known spell of level N from level 2N + 1 |
| wizard | school / elemental school + opposition schools | +1 slot per spell level for school spells; opposition spells with a warning (2 slots) |
| witch | patron (51, Foundry) | patron spell of level N from level 2N (familiar, normal slots – overview only) |
| shaman | spirit (17, Foundry) | spirit magic: 1 spontaneous cast per day on each spell level she can cast |
| bloodrager | bloodline (22, Foundry) | known spell of level N from level 3N + 4 (7th, 10th, 13th, 16th) |
| psychic | discipline (23, Foundry) | known 1st-level spell from level 1, level N from 2N |
| warpriest | 2 blessings (47, Foundry) – deity's domains only | none; minor (1st) and major (10th) powers |
| inquisitor | 1 domain (+ subdomain) – deity's domains only | none; domain powers |

Patrons, spirits, bloodrager bloodlines and disciplines are not in the spell source – their spells are read from the Foundry
descriptions (`tools/build_class_abilities.py` → `SUBCLASS_SPELL_TABLE`, spell names matched to `SpellList.json`,
"X, Mass" ↔ "Mass X", roman numerals) and `SubclassIndex` adds them to the subclasses from the source (keys `patron:`,
`spirit:`, `brbloodline:`, `discipline:`). Blessing → domain (`BLESSING_DOMAINS`) comes from Foundry tags; Death and Evil
have a wrong tag in Foundry, so the blessing name wins.

A subdomain replaces its domain's spells at the levels it lists. The source does not contain the subdomain → domain link –
it is hand-written in `Subclasses.kt` (`SUBDOMAIN_DOMAINS`, from the table at [aonprd.com/ClericDomains.aspx](https://aonprd.com/ClericDomains.aspx)).
Only subdomains of the chosen domain are offered (archon, azata, devil… belong to two domains); changing the domain removes a
subdomain that does not belong to the new one. Boreal and sleep are missing from the AoN table, so they are not offered.

## Items

### Model (`Item`)
Common fields: `name`, `category` (`ItemCategory`), `subtitle` ("Martial · One-handed", "Medium armor", "Wondrous Item · neck"),
`priceGp`, `weightLb`, `description`, `source`. By kind:
- weapons: `proficiency`, `damageSmall`, `damageMedium`, `critical`, `range`, `damageType` (B/P/S → Bludgeoning/Piercing/Slashing), `special`
- armor: `armorBonus`, `maxDex`, `armorCheck`, `spellFailure`
- magic: `aura`, `casterLevel`, `slot`, `requirements`

`summary` is a short text for the inventory: `1d8 / 19-20/×2`, `+6 AC, ACP −5`.

### Categories
`WEAPON, AMMO, ARMOR, GEAR, ALCHEMICAL, MOUNT, POTION, SCROLL, WAND, RING, ROD, STAFF, WONDROUS, MAGIC_WEAPON, MAGIC_ARMOR, ARTIFACT, OTHER`
(order = order of groups in the inventory).

| Source | Mapping |
|---|---|
| Foundry folders | `adventuring-gear`, `clothing`, `food-and-drink`, `tools-and-skill-kits`, `animal-gear`, `material-components`, `miscellaneous` → GEAR (with a subcategory); `alchemical-items`, `herbs` → ALCHEMICAL; `animals-and-mounts` → MOUNT; `armor-*`, `shields` → ARMOR. The `magic-items` folder is skipped (magic items come from the CSV). |
| `Weapons.csv` | WEAPON (`is_ammunition` → AMMO), handedness from `is_light/one_handed/two_handed/ranged/unarmed` |
| `MagicItems.csv` column `Group` | Wondrous Item → WONDROUS, Ring, Rod, Staff, Wand, Potion (+ names "Potion…", "Oil of…") → POTION, Weapon/Ammunition/Legendary Weapon/Shadow Piercing → MAGIC_WEAPON, Armor → MAGIC_ARMOR, Artifact → ARTIFACT, others (Cursed, Magical Tattoo) → OTHER |

Current counts: WEAPON 243, AMMO 36, ARMOR 70, GEAR 1379, ALCHEMICAL 72, MOUNT 20, POTION 9, WAND 1, RING 228, ROD 235,
STAFF 107, WONDROUS 2505, MAGIC_WEAPON 478, MAGIC_ARMOR 250, ARTIFACT 331, OTHER 96 – **6,060 in total**.

In PF1 potions, scrolls and wands are made from spells, so the database has hardly any; the app creates them from spells
("From spell") or as a "Custom item".

### Conversion (`ItemParser`)
- `Csv` – own RFC 4180 parser (quotes, `""`, newlines inside a field, BOM).
- `decodeText` – UTF-8, on an invalid sequence Windows-1252 (`weapons.csv` is cp1252; broken apostrophes are fixed).
- Prices: `PriceValue` or text "1,500 gp" / "5 sp" / "1 cp" → gp as `Double`. Weights: "4 lbs.", "1/2 lb." → lb.
- `id` = `category-slug` (`weapon-longsword`, `magic-bag-of-holding-minor`), duplicates `-2`…
- `combine` merges all sources and sorts them by name.

### Building the bundled data – `tools/build_items.py`
1. Downloads the `packs/items` and `packs/armors-and-shields` ZIP archives from the Foundry PF1 GitLab API and converts the YAML
   (price in the right currency → gp, weight, description from HTML to text, AC/max Dex/ACP/ASF for armor) into `ItemsBase.json`.
2. Downloads `weapons.csv`, `ammunition.csv` and `magic_items_full…csv`, saves them as UTF-8; magic items are trimmed to the
   columns the app reads (the original has 10 MB because of the HTML column `FullText`).

```bash
pip install pyyaml
python tools/build_items.py
```

The URLs in `build_items.py` (`CSV_SOURCES`) and in `ItemParser` (`WEAPONS_URL`, `AMMO_URL`, `MAGIC_URL`) must match.

### Updating in the app (`ItemRepository.download`)
Downloads the three CSVs from GitHub (`raw.githubusercontent.com/Vireche/randomitemgenerator/master/data/…`), parses them
(validation), saves them to `AppFiles` (`items_weapons.csv`, `items_ammunition.csv`, `items_magic.csv`) and rebuilds the database.
`ItemsBase.json` (Foundry) always stays bundled – it is hundreds of small YAML files and PF1 mundane gear does not change.

## PF1 rules in code

### Spell levels by class level (`CasterClasses.kt`)

| Progression | Classes | Max spell level at level L |
|---|---|---|
| PREPARED_9 | wizard, cleric, druid, witch, shaman | `min(9, (L+1)/2)` |
| SPONTANEOUS_9 | sorcerer, oracle, psychic, arcanist | `L<4 → 1`, else `min(9, L/2)` |
| SIX | bard, skald, inquisitor, magus, alchemist, investigator, summoner (+unchained), warpriest, hunter, mesmerist, occultist, spiritualist | `min(6, (L+2)/3)` |
| FOUR | paladin, antipaladin, ranger, bloodrager | `L<4 → none`, else `min(4, (L-1)/3)` |
| FOUR_KNACKS | medium | `L<4 → 0` (knacks only), else `min(4, (L-1)/3)` |

- Level 0 (cantrips/orisons/knacks) for everyone except FOUR, alchemist and investigator.
- Classes with several lists take the lower level: hunter = hunter/druid/ranger, warpriest = warpriest/cleric,
  skald = skald/bard, investigator = investigator/alchemist, arcanist = sorcerer/wizard, oracle = cleric/oracle.
- Medium (Occult Adventures, Table 1-2): knacks from 1st level, 1st at 4, 2nd at 7, 3rd at 10, 4th at 13.
- Left out: adept (NPC class). Subclasses see above.

### Potions, scrolls and wands from a spell (`SpellItemKind`)
Price by the Core Rulebook: potion 50 gp × SL × CL (max 3rd level), scroll 25 gp × SL × CL, wand 750 gp × SL × CL
(max 4th level, 50 charges); a 0-level spell counts as ½. Default CL = the minimum for the level (1, 1, 3, 5, 7 …),
level from the chosen spell list. Expensive material components are only mentioned, not added to the price.

### Classes for multiclassing (`ClassTable.kt`)
`python tools/build_classes.py` (pyyaml) generates `data/ClassTable.kt` from Foundry VTT PF1 `packs/classes`:
49 classes (core, hybrid, occult, unchained and NPC) with BAB progression (high = L, med = ¾ L, low = ½ L), good saves
(2 + L/2, otherwise L/3), HD, skill points, class skills and armor / weapon proficiencies. Multiclass = sum over all classes.
Prestige classes see below.

### Races (`RaceTable.kt`)
`python tools/build_races.py [races.zip]` (pyyaml) generates `data/RaceTable.kt` from Foundry VTT PF1 `packs/races`: 80 races with
size, base speed, racial ability and skill modifiers, racial class skills, darkvision / low-light vision, creature type and
subtypes. Human, half-elf and half-orc have no fixed modifiers in Foundry – they get "+2 to one ability" (`anyBonus`).
Groups follow the Advanced Race Guide: core (7), featured (16), uncommon (14), other (the rest – Bestiary, Alien…); the
lists are in the script.

- `Character.abilities` are **base** scores, the resulting `Character.scores` = base + `RaceInfo.mod` (for `anyBonus` by
  `Character.raceBonus`). The resulting scores are used by prerequisites, slots, abilities and carrying capacity; STR
  entered in the inventory is converted back to the base.
- Racial bonus feat (`RaceInfo.bonusFeats`): human 1, half-elf 1 (Adaptability – Skill Focus) → `FeatSlots.race`.
- The race size is copied to the linked inventory (`Inventory.size`, carrying capacity) – when choosing the race and when linking.
- Race as a feat prerequisite: text that is a race name / id or a subtype (elf, human, orc, dwarf, gnome, halfling),
  optionally with "subtype" or a qualifier in parentheses. A race qualifies when its name matches or it has the subtype
  (`RaceInfo.countsAs`): a half-elf counts as an elf and a human, a half-orc as an orc, a drow as an elf, a svirfneblin as a gnome.
  "Human (Varisian)" with race human = check yourself, with another race = not met.

### Prestige classes (`PrestigeTable.kt`, `PrestigeClasses.json`)
Foundry VTT PF1 has no prestige classes. `python tools/build_prestige_classes.py [--cache DIR]` downloads the list from
Archives of Nethys ([PrestigeClasses.aspx](https://aonprd.com/PrestigeClasses.aspx)) and the class pages (~120 requests,
1 per second, cached in `build/prestige-cache`):
- **`PrestigeTable.kt`** (`PRESTIGE_TABLE`, `PrestigeInfo`) – HD, skill points, class skills (AoN names → app skill ids),
  BAB and saves per level straight from the table (prestige classes use other save formulas), spellcasting per level from
  the "Spells per Day" column (or Special): "+1 level of (existing) arcane / divine spellcasting class" → `arcane` /
  `divine`, no kind → `any`, "+1 level of witch class" → `class:witch`; Mystic Theurge `arcane+divine`; bonus feats
  (Special "bonus feat"), requirements ("Spells: Able to cast 3rd-level arcane spells."), source, PFS. 118 classes, 60
  advance spellcasting.
- **`PrestigeClasses.json`** – description and class features text (lazily, `ensurePrestigeTexts`).

In the app: `ClassCatalog.prestige` (`ClassInfo.prestige`, group `ClassGroup.PRESTIGE`, `maxLevel`), `Character.stats`
uses the table values, `featSlots` the bonus feats. `ClassEntry.advances` (kind → classId, `Prestige.candidates` by
`CasterClass.tradition`, `defaultAdvances` = the single fitting class) → **`Character.casterLevel(e)`** = class level +
levels of prestige classes advancing it; used for spells per day / known, the highest spell level, the spell picker
(`Spellbook.classLevel`) and prerequisites ("caster level", "able to cast 3rd-level spells") – class features (domains,
hexes…) still follow the class level. Requirements: `PrestigeRequirements.evaluate` (label + text → prerequisite text →
`FeatPrereqs.evaluate`; "Base Attack Bonus: +5" → "base attack bonus +5", "… and 2nd-level arcane spells" → two parts),
strict mode `StrictBlock.PrestigeRequirementUnmet`.

### Archetypes (`Archetypes.json`)
`python tools/build_archetypes.py [--cache DIR]` walks the classes of `ClassTable.kt` (without NPC and unchained) and
downloads from Archives of Nethys the class's archetype table (`Archetypes.aspx?Class=X`: name, PFS, "Replaces",
summary) and every archetype page (`ArchetypeDisplay.aspx?FixedName=X Y`: source, full text) – ~1300 requests,
1 per second, cached in `build/archetype-cache`. Result: 1,252 archetypes, id `class:name-slug`, `replaces` = features
split on ";" as AoN lists them ("Smite Evil", "3rd-level Mercy"). Loaded lazily (`ensureArchetypes`).

"Replaces" entries are split on ";" and "," – a list of levels stays with its feature ("2nd, 4th, 6th-level discovery").

In the app: `ClassEntry.archetypes` (`ArchetypePick` = id, name, copy of `replaces`), chosen below the character's class.
- **Mechanics** (`ArchetypeEffects.kt`, `ClassEntry.archetypeEffect`):
  - automatically from `replaces`: the name of a preset ability (`FeaturePresets`, aliases Bomb → Bombs, Judgement →
    Judgment) → the ability disappears; the class's subclass (`SUBCLASS_FEATURES`: cleric "Domains", druid "Nature Bond",
    wizard "Arcane School"…) → `ClassEntry.subclassKind` = null; "Bloodline Powers" / "Domain Powers"… → no subclass powers,
    "Bonus Spells" / "Patron Spells"… → no bonus spells; "Cantrips" / "Orisons" / "Knacks" → no cantrips; hexes /
    revelations / bonus feats (also combat style and teamwork feats) → fewer by the listed levels ≤ class level (no levels =
    the whole feature),
  - by hand `ARCHETYPE_MODS` (`ArchetypeMods.kt`, from the AoN texts): `noSpells` (`ClassEntry.caster` = null),
    `perDayDelta` / `knownDelta` (`ClassEntry.levelSlots`, cantrips too unless `deltaIncludesCantrips = false`),
    `oneDomain`, `subclass` (Sacred Servant / Temple Champion: a paladin domain), `removes`, `presets` (new limited-use
    abilities), `bonusFeatLevels`, `casting` (`CastingChange`: `lists` / `addLists`, `ability`, `kind`, `table` +
    `progression` – another class's spell table via `CasterClass.tableId`, `tradition`, `abilityBonus`, `spells` by name),
    `subclassLevelOffset` (`ClassEntry.subclassLevel` – Sacred Servant −3), `secondBloodline` (`SubclassSlot.BLOODLINE2`).
    `BundledDataTest` checks that ids, spell lists, tables and spells exist.
  - **Extra spells** from the text (`Archetype.spells`, copied into `ArchetypePick.spells`): the generator reads
    "1st—a, b; 2nd—c", "a (1st), b (2nd)" and "adds x … as a 3rd-level spell" in spell-list paragraphs and checks the
    names against `SpellList.json` (without book abbreviations "APG", "UM"…, "lesser x" → "x, lesser"); "2nd—x (1st)" =
    class level + spell level in parentheses. `ClassEntry.caster` puts them into `CasterClass.extraSpells`
    (spellLevelOf, spell picker).
  - "Spells" / "Spellcasting" in `replaces` does nothing automatically – it usually means altered, not removed, casting.
  - The magic tradition (`CasterClass.tradition`, also when changed by an archetype) decides "ability to cast arcane /
    divine spells" prerequisites.
- Conflict = two chosen archetypes share an entry in `replaces` (case-insensitive) – "Mercy" and "3rd-level Mercy" are
  not treated as a conflict. Strict mode does not add a conflicting archetype.
- Unchained classes (`rogue_unchained`…) get the base class's archetypes (`Archetypes.baseClass`), NPC classes and
  creature HD none (`Archetypes.supports`).

### Racial traits (`RacialTraitTable.kt`, `RacialTraits.json`)
`python tools/build_racial_traits.py [--cache DIR]` downloads the race list from Archives of Nethys
([Races.aspx?Category=Core / NonCore](https://aonprd.com/Races.aspx)) and the race pages (~80 requests, 1 per second,
cache `build/race-cache`). Race ids as in the app (slug of the name; Lashunta → both `lashunta-female` and
`lashunta-male`); the app has no Boggard, Gnoll, Lizardfolk and Ogre have no race page on AoN.

- **`RacialTraitTable.kt`** – default traits from the "X Racial Traits" section (bold names; without the ability line).
  Some AoN pages have unclosed `<b>`, so a bold block is split into lines and the name is before ":" or "(Ex/Su/Sp)".
- **`RacialTraits.json`** – 632 alternate traits from the "X Alternate Racial Traits" section: group "Replaces A, B" →
  `replaces`, name, PFS (Pathfinder Society icon), source, text. Loaded lazily.

In the app: `Character.racialTraits` (`RacialTraitPick` = id, name, copy of `replaces`), chosen at the race (changing
the race drops them).
- Prerequisite "X racial trait": a chosen alternate trait ✓, a replaced default ✗, a default trait of the race per AoN ✓,
  otherwise the hand-made table and "?" as before.
- Replaced **Bonus Feat** (human) / **Adaptability** (half-elf) → no racial bonus feat; replaced **Skilled** (human) →
  no extra skill point per level.
- **Ability adjustments** from alternate traits (`ABILITY_VARIANTS` in `RacialTraits.kt`, entered by hand from the AoN
  text): Witchborn (changeling: +2 INT instead of WIS), Keen Kitsune (+2 INT instead of CHA), Agile (vine leshy: +2 DEX
  instead of CON), Tree-Born (gathlain: no −2 CON); with an ability choice (`RacialTraitPick.abilities`) and instead of
  "+2 to one ability score": Dual Talent (human: +2 to two different ones), Kindred-Raised (half-elf: +2 CHA and +2 to
  another), Orc Atavism (half-orc: +2 STR, −2 to one mental score). `Character.raceMod` applies them, so they count for
  `scores`, carrying capacity and prerequisites.
- Two alternate traits may not replace the same default trait – the app warns, strict mode does not allow it.

### Skills (`Skills.kt`)
The 26 Core Rulebook skills (Knowledge split into 10 specialties), ids as in Foundry VTT PF1 (`disableDevice`,
`knowledge.arcana`); Craft / Perform / Profession have kinds – rank key `craft:alchemy`. Class skills are in `ClassTable.kt`
(`ClassInfo.classSkills` from Foundry `packs/classes`, "knowledge" = all specialties; the background skills artistry / lore
are left out), racial bonuses and class skills in `RaceTable.kt` (`skillMods`, `classSkills`).

- Total (`skillTotal`) = ranks + ability modifier (`scores`) + 3 for a class skill with ≥ 1 rank + racial bonus;
  trained-only without ranks = cannot be used. Armor check penalty is not applied.
- Skill points (`skillPoints`) = Σ class level × max(1, class points + Int modifier) + human 1 per level + `extraSkillPoints`
  (favored class, traits – manual). The current INT is used (INT increases during levelling are not tracked).
- Prerequisites: "Acrobatics 5 ranks", "5 ranks in Sense Motive", "Knowledge (arcana, …, or religion) 5 ranks",
  "Craft (any) 1 rank", "1 rank in at least one Knowledge skill", "Knowledge (nature) or Survival 5 ranks" (the number
  from the last part applies to all). A character without any ranks → "check yourself".

### Deities (`DeityTable.kt`)
`python tools/build_deities.py [page.html] [--cache DIR]` generates `data/DeityTable.kt` from the table at
[aonprd.com/DeitiesByGroup.aspx](https://aonprd.com/DeitiesByGroup.aspx): 463 deities (name, pantheon, alignment, areas
of concern, domains, favored weapon) and their subdomains from each deity's page (`DeityDisplay.aspx`, ~460 requests at
1 per second, cache `build/deity-cache`; subdomains requiring a trait, "*", are included). The character stores the name
(`Character.deity`), `DeityCatalog.get` finds it case-insensitively; other text = a custom deity (the name is compared
loosely, its alignment is unknown). A cleric with a deity from the list chooses domains only from `DeityInfo.domains` and
subdomains only from `DeityInfo.subdomains`; `subdomains = null` = page missing → all subdomains of the domain are offered.
A mismatch (e.g. after changing the deity) is reported by `ClassEntry.deityConflicts`.

### Subclass abilities from AoN (`SubclassAbilityTable.kt`, `SubclassAbilities.json`)
`python tools/build_subclass_abilities.py [--cache DIR]` downloads the shaman spirit, bloodrager bloodline, psychic discipline
and Inquisitions pages (~70 requests, 1 per second, cache `build/subclass-cache`) – Foundry does not link these powers to the subclass:

- **`SubclassAbilityTable.kt`** – `AON_SUBCLASS_POWER_TABLE` (371 powers: spirit ability 1st, greater 8th, true 16th, manifestation
  20th; bloodlines, disciplines and inquisitions take the level from "At Nth level"), uses from the text ("a number of times per
  day equal to 3 + your Wisdom modifier", "once per day"; with "…, plus 1 round…" the formula is skipped). `DISCIPLINE_POOL_ABILITY`
  (phrenic pool ability), `INQUISITION_DEITIES` (empty = any deity, also "n/a").
- **`BLOODLINE_BONUS_FEATS`** – bonus feats of 75 bloodrager and sorcerer bloodlines; the sorcerer bloodline pages
  (SorcererBloodlines.aspx) also give their powers – the app uses them only for bloodlines Foundry lacks.
- **`SubclassAbilities.json`** – 82 spirit hexes with text (`ClassAbilities.json` format, `spirits` field); a hex with the same
  name as a regular shaman hex is skipped by the app.

### Class abilities (`ClassAbilityTable.kt`, `ClassAbilities.json`)
`python tools/build_class_abilities.py [class-abilities.zip] [sources.mjs]` (pyyaml) processes Foundry VTT PF1
`packs/class-abilities` (~4,700 YAML files) and book names from `module/registry/sources.mjs` (code "PZO1115" →
"Advanced Player's Guide"):

- **`ClassAbilityTable.kt`** – subclass powers: the domain, subdomain, bloodline (11 from CRB / APG) and wizard school
  (incl. elemental) containers link their powers in `system.links.supplements` with the level they are gained at;
  `system.uses.maxFormula` comes from the power (per day only). Keys: `domain:fire`, `subdomain:ash`,
  `subdomain:archon@good` (a subdomain under two domains), `bloodline:draconic`, `school:evocation`, `school:element:fire`.
  325 powers of 136 subclasses, 240 with uses. Where Foundry has no formula but the text gives one ("number of rounds
  per day equal to 3 + your Charisma modifier"), the script fills it in. Unit: "rounds per day" in the text → rounds.
- **`ClassAbilities.json`** – hexes (tags Hex / Major Hex / Grand Hex, classes witch and shaman; spirit hexes come from AoN – see above) and
  revelations (tag Revelation, oracle, mysteries from "X Mystery" tags, no final revelations): text from HTML
  (`@UUID[…]{Name}` links → name), uses formula, Ex / Su / Sp, source, minimum level (major hex 10, grand hex 18,
  revelations from "must be at least 7th level"). 420 entries (117 hexes, 303 revelations), loaded lazily.

In the app:
- `SubclassPowers.keysOf(ClassEntry)` turns the character's subclass into keys (a subdomain wins over the domain it
  replaces – its power list is complete), `Character.resources()` turns them into powers available at the class level.
  Unknown domain / bloodline → generic 1st-level power (3 + Wis / Cha). A wizard whose school is in the table has no
  generic "Arcane school power".
- `FoundryFormula` evaluates the formula: `@class.unlevel` / `@class.level` = class level, `@attributes.hd.total` =
  character HD, `@abilities.X.mod`; functions `floor`, `ceil`, `min`, `max`, `clamp`, `abs`, `if`, `gt`, `gte`, `lt`,
  `lte`, `eq`; rounded down, never negative. Both the script and `BundledDataTest` check every formula evaluates.
- Chosen hexes / revelations: `ClassEntry.abilities` (`AbilityPick` = id, name, kind, copy of the formula) – the counter
  works without the database loaded. Count (`AbilityRules.slots`): witch 1 + level / 2, shaman at 2nd, 4th, 8th, 10th,
  12th, 16th, 18th, 20th, oracle at 1st, 3rd, 7th, 11th, 15th, 19th level.

### Feats (`Feats.json`)
`python tools/build_feats.py` downloads the overview at [aonprd.com/Feats.aspx](https://aonprd.com/Feats.aspx) (name,
prerequisites, summary, PFS) and each feat's detail (types, source, flavor, Benefit / Normal / Special sections…). ~3,400
requests at 1 per second (AoN is slow → ~3 h), pages are cached in `build/feat-cache`, so an interrupted run resumes.
AoN marks in names (`*` mythic version, `⊤`, `†`) are removed. The app does **not** update feats (that many requests from
the app would not be reasonable).

Prerequisites (`FeatPrereqs`): the text is split by commas and semicolons outside parentheses; checked are abilities
("Str 13"), BAB, character level, class level ("fighter level 4th", "8th-level fighter"), caster level (highest
spellcasting class level), "ability to cast Nth-level spells", race and the character's feats (for "Weapon Focus
(longsword)" having Weapon Focus is enough; "Weapon Focus with the chosen weapon" and names with a glued book abbreviation
– "Improved Dirty TrickAPG" – are found too). "—" in the source = a feat without prerequisites (248 feats). "A or B" = one
is enough; a shared suffix / prefix applies to all parts ("good or lawful alignment", "Animal or Plant domain",
"Knowledge (nature) or Survival 5 ranks"). Nothing is enforced.

Derived rules (`PrereqRules.kt`):
- **class features** – a table feature → class and level (channel energy, rage, ki pool, wild shape, sneak attack…,
  ~100 entries), dice for "channel energy 3d6" / "sneak attack +2d6"; optional features (a druid's animal companion,
  a cleric's positive / negative energy) = "?". A feature from an archetype is unknown to the app → confirm manually.
- **alignment** (`Character.alignment`) – "good alignment", "nonlawful", "chaotic neutral"…; **deity** (`deity` = a name
  from the list or custom text) – "worshiper of Iomedae", "must worship a deity", "worshiper of a good deity",
  "alignment must match / be within one step of your deity's" (the deity's alignment from the list),
- **size, type and subtype** from the race – "Small size or smaller", "humanoid", "giant subtype"; **Hit Dice** = level,
  **base saves** ("base Fortitude save bonus +3"),
- **proficiencies** with armor / shields / martial weapons from classes (`ClassInfo.armorProf/weaponProf` from Foundry)
  and feats (Armor Proficiency…),
- **spells** – arcane / divine / psychic caster, "ability to cast 3rd-level divine spells", "ability to cast cleric spells",
  a specific spell ("ability to cast dispel magic" – it is on the spell list of a casting class and the level is high enough),
- **domains** of a cleric / druid (also through a subdomain), **feat type** ("any teamwork feat"),
- **racial traits** – the races' default traits (stonecunning, keen senses…), darkvision / low-light vision from Foundry;
  default traits of all races and alternate traits from AoN (see Racial traits above) – chosen ✓, replaced ✗, otherwise "?".

**Manual confirmation** (`Character.confirmedPrereqs`): what the app cannot check or considers unmet, the user confirms in
the feat detail with "I have it" – it applies to every feat with the same prerequisite text (included in exports).

Share of feats with "?" (a human character with skills, alignment and deity filled in): originally 2,475 → 974.

Feat count (`featSlots`): 1st and every odd character level + class bonus feats (fighter 1 + even levels, wizard 5/10/15/20,
monk 1/2/6/10/14/18, magus 5/11/17, warpriest and inquisitor/hunter (teamwork) every 3rd, brawler 2/5/8…, swashbuckler
and gunslinger every 4th, cavalier/samurai 6/12/18, ranger 2/6/10/14/18, sorcerer 7/13/19, bloodrager 6/9/12/15/18)
+ racial bonus feat + manual "Extra" (traits…).

### Slots and spells known (`CastingTable.kt`)
`python tools/build_casting.py` (standard library only, ~27 requests at 1 per second) downloads the class pages from
Archives of Nethys and generates `data/CastingTable.kt` from their tables: spells per day and spells known for each of the
27 casting classes (arcanist: spells prepared). Row = class level 1–20, value for spell level 0–9, `-` = the class does
not have that level (for spontaneous classes cantrips are "at will"). Tests (`SlotTableTest`) check consistency with the
spell level progression and samples from the CRB.

- `CastingKind`: PREPARED (limit = copies by slots), SPONTANEOUS (limit = known spells, casts from slots),
  HYBRID (arcanist – limit of prepared spells, casts from slots). Spontaneous/arcanist have each spell only once.
- Bonus spells from the casting ability (CRB table 1–3): from modifier ≥ spell level `(mod − level) / 4 + 1`, not for cantrips.
  A spell of level N requires the ability ≥ 10 + N (the UI only warns). The occultist has no known table (implements grant spells).

### Carrying capacity (`CarryingCapacity`)
Heavy load by STR 1–29 from the Core Rulebook (10 → 100 lb, 18 → 300 lb, 29 → 1,400 lb), above 29 every +10 STR ×4.
Light = ⌊heavy/3⌋, medium = ⌊heavy·2/3⌋. Coins: 50 = 1 lb.

Multiplier by creature size (`CreatureSize`, stored in the inventory as `size` + `quadruped`):

| Size | Fine | Diminutive | Tiny | Small | Medium | Large | Huge | Gargantuan | Colossal |
|---|---|---|---|---|---|---|---|---|---|
| biped | ×1/8 | ×1/4 | ×1/2 | ×3/4 | ×1 | ×2 | ×4 | ×8 | ×16 |
| quadruped | ×1/4 | ×1/2 | ×3/4 | ×1 | ×1½ | ×3 | ×6 | ×12 | ×24 |

Load penalties (`Load`): medium – max Dex +3, check penalty −3, run ×4; heavy – max Dex +1, check −6, run ×3;
both speed 30 → 20 ft (20 → 15 ft). Overloaded – only 5 ft per round, no Dex bonus to AC.

### Equipped gear (`Equipment.kt`)
An inventory entry (`InvEntry`) has `equipped`, `slot` (`ItemSlot`: head, headband, eyes, shoulders, neck, chest, body,
armor, belt, wrists, hands, 2× ring, feet, main hand, shield) and `bonuses` (`ItemBonus`: target, value, type, skill key).

- Only equipped entries of the **linked inventory** count; a slot holds only as many as fit (first in inventory order),
  the rest are conflicts (`slotConflicts` → red banner). Entries without a slot all count. `AppState.updateInventory`
  and `linkInventory` copy the bonuses into `Character.gear`.
- Stacking (`stackBonuses`): bonuses of the same type don't stack – the highest applies; dodge, untyped and circumstance
  stack; penalties always stack.
- Targets: abilities (`gearMod` → `scores`, so also bonus spells, skill points, prerequisites, carrying capacity),
  saves (`gearSave`), AC, skill (`gearSkill`; a bonus to "knowledge" applies to every Knowledge), `MAX_DEX`
  (the lowest applies) and `ARMOR_CHECK`.
- Armor / shields from the database: `ItemBonusGuess.of(Item)` fills in AC (armor / shield type), max Dex and the
  armor check penalty. Magic items by name: ability headbands and belts, Cloak of Resistance, Ring of Protection,
  Amulet of Natural Armor, Bracers of Armor; anything else is entered by hand.
- The inventory's load (`loadBonuses`) adds max Dex and the skill penalty to `gear` (untyped).

### Defense and hit points (`Defense.kt`)
- **AC** = 10 + armor + shield + Dex (capped by max Dex) + size (`sizeAcMod`: Small +1, Large −1, Tiny +2…)
  + natural armor (gear, animal companion) + deflection + dodge + other types + manual `Character.acMisc`.
  **Touch** without armor, shield and natural armor; **flat-footed** without positive Dex and dodge.
- **Armor check penalty** (`armorCheckPenalty`): armor + shield add up, the load penalty doesn't stack with them –
  the worse one applies. Subtracted from Str and Dex skills.
- **HP** (`maxHp`): the character's 1st HD maximum, the rest average (half + 1, like PFS), creature racial HD average
  (4.5 per d8); + Con modifier per HD (min. 1 HP per class HD), Toughness (+3, +1 per HD from 4th HD), `hpBonus`.
  `hpOverride` (rolled HP) replaces the calculation. Current HP = max − `damage` (may be negative).
- Rest (`AppState.rest`) heals 1 HP per character level. Export carries `hpBonus`, `hpOverride` and `acMisc`,
  not damage (just like cast spells).

### Animal companion (`AnimalCompanions.kt`)
The "Animal Companion Base Statistics" table from the Core Rulebook (`CompanionProgression`, effective druid level 1–20):
HD, natural armor, Str/Dex bonus, bonus tricks, special abilities (link, share spells, evasion, ability increase,
devotion, multiattack, improved evasion). HD, BAB, saves, skill points and feats come from the racial "class" `animal`
(`ClassCatalog.creatures`) – the companion level only sets its level.

- `SpeciesCatalog`: 17 CRB animals (by hand, stable ids) + 140 from `SpeciesTable.kt` (`EXTRA_SPECIES`, GENERATED by
  `tools/build_companions.py` from d20pfsrd – section "Animal Companion Descriptions", without third-party content –
  source opengamingstore.com) with starting abilities, size, speed, attacks and the 4th- or 7th-level advancement
  (size, Str / Dex / Con adjustments, natural armor, attacks). Four legs are guessed by the generator from the name.
- `companionMod` (Str/Dex bonus + advancement) goes into `scores`, `naturalArmor` into AC.
- `suggestedCompanionLevel`: druid and hunter full, ranger level − 3; mounts also paladin and cavalier.

### Familiar (`Familiars.kt`, `FamiliarTable.kt`)
`tools/build_companions.py` reads the familiar table from d20pfsrd (89, name, master's bonus, source). An unconditional
bonus ("+3 bonus on Stealth checks", "+2 bonus on Reflex saves", "+1 natural armor bonus to AC", "+3 hit points") →
`ItemBonus` (skill / save / AC / new target `BonusTarget.HP`), conditional ones (initiative, "in bright light", "to
identify") are text only. AppState keeps a copy of the familiar bonuses on the master (`Character.familiarBonus`,
`syncFamiliarBonus` on companion change / delete and import); `Character.allBonuses` = gear + familiar (skills, saves,
AC, HP). Progression by the master's level (the familiar's `companionLevel`, `FamiliarProgression`): natural armor
(level + 1) / 2, Int 5 + (level + 1) / 2, SR level + 5 from 11th, special abilities; familiar HD = master's level.

### Eidolon (`Eidolons.kt`, `EidolonTable.kt`, `Eidolons.json`)
`python tools/build_eidolons.py [--cache DIR]` (d20pfsrd, summoner and unchained summoner eidolon pages): progression
tables (HD, BAB, saves, skills, feats, armor, Str/Dex, evolution pool, max attacks, special), base forms (size, speed,
natural armor, good saves, attack, abilities, free evolutions; unchained ids "u_…"), unchained subtypes and 79
evolutions with cost (no [3PP]) → `Eidolons.json` (lazy, `ensureEidolonData`).
An eidolon = a companion with `companion = EIDOLON`, `eidolonForm`, `eidolonSubtype`, `evolutions` (`EvolutionPick` –
copy of name and cost); `companionLevel` = summoner level, racial HD `eidolon` (full BAB, d10, 4 skill ranks per HD,
good saves by form in `Character.stats`). Mechanics: Str/Dex from the table + Ability Increase (+2,
`EvolutionPick.ability`), natural armor form + table + Improved Natural Armor (+2), Large → size Large.

### Metamagic (`Metamagic.kt`)
The slot increase is read from the feat text (AoN): "uses up a spell slot two levels higher" → 2, "does not use up
a higher-level spell slot" / "same spell level" → 0, otherwise (Heighten Spell) unknown. `ofCharacter` returns the
character's metamagic feats from the database (no repeats, custom feats excluded) – the spell detail shows the
resulting slot level.

### Strict mode (`StrictMode.kt`)
`AppState.strictMode` (Settings `strict_mode`). Before adding, `StrictRules` checks:
- **spell**: the class casts it at its level, casting ability ≥ 10 + spell level and the spell level has room
  (prepared copies = slots + ability bonus + specialist school slot; spontaneous / arcanist = known / prepared spells),
- **feat**: the feat count (`featSlots`) isn't used up and no prerequisite is ✗ ("?" passes).

Removing always works; import isn't checked by strict mode.
