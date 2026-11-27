# Data

**Čeština** | [English](en/data.md)

Všechna data jsou **přibalená v appce** (`composeApp/src/commonMain/composeResources/files/`), takže appka funguje offline.
Část jde tlačítkem v appce stáhnout znovu ze zdroje.

| Soubor | Obsah | Zdroj | Aktualizace v appce |
|---|---|---|---|
| `SpellList.json` (6 MB) | 3 040 záznamů → 3 030 spellů s class listem | [SpellTable-pf1e](https://github.com/iociveteres/SpellTable-pf1e) `data/SpellList.json` (MIT, data z Archives of Nethys) | ano |
| `ItemsBase.json` (0,7 MB) | 1 541 položek: výstroj, oblečení, nástroje, jídlo, alchymie, byliny, zvířata, zbroje a štíty | [Foundry VTT PF1](https://gitlab.com/foundryvtt_pathfinder1e/foundryvtt-pathfinder1) `packs/items`, `packs/armors-and-shields` (OGL) | ne (jen přibalené) |
| `ClassAbilities.json` (0,3 MB) | 420 hexů a zjevení s textem; schopnosti podtříd v `ClassAbilityTable.kt` | [Foundry VTT PF1](https://gitlab.com/foundryvtt_pathfinder1e/foundryvtt-pathfinder1) `packs/class-abilities` (OGL) – `tools/build_class_abilities.py` | ne (jen přibalené) |
| `PrestigeTable.kt`, `PrestigeClasses.json` (0,8 MB) | 118 prestižních class: tabulka po úrovních, sesílání, požadavky; popis a schopnosti | [Archives of Nethys](https://aonprd.com/PrestigeClasses.aspx) – `tools/build_prestige_classes.py` | ne (jen přibalené) |
| `Archetypes.json` (4,0 MB) | 1 252 archetypů 40 class (nahrazené schopnosti, shrnutí, zdroj, PFS, text) | [Archives of Nethys](https://aonprd.com/Archetypes.aspx) – `tools/build_archetypes.py` | ne (jen přibalené) |
| `RacialTraits.json` (0,4 MB) | 632 alternativních rasových traitů; výchozí traity v `RacialTraitTable.kt` | [Archives of Nethys](https://aonprd.com/Races.aspx) – `tools/build_racial_traits.py` | ne (jen přibalené) |
| `Weapons.csv` | 243 zbraní | [Vireche/randomitemgenerator](https://github.com/Vireche/randomitemgenerator) `data/weapons.csv` (d20pfsrd/AoN) | ano |
| `Ammunition.csv` | 36 druhů munice | tamtéž `data/ammunition.csv` | ano |
| `MagicItems.csv` (4 MB) | 4 241 magických itemů | tamtéž `data/magic_items_full - Updated 19Jan2020.csv` (d20pfsrd „magic_items_full“) | ano |

## Spelly

### Formát zdroje
Pole JSON objektů, mimo jiné:
`Name`, `School`, `Subschool`, `Descriptors`, `Casting time`, `Components`, `Range`, `Effect`, `Target`, `Duration`,
`Saving throw`, `Spell Resistance`, `Description` (odstavce oddělené `\n`), `Short description`, `Mythic`, `Source`,
`Url aon` a `access_ways.Level` = seznam `[spell list, level]`, např. `["sorcerer/wizard", 3]`.

### Převod (`SpellParser`)
- Hodnoty `"None"`, `"NULL"` a prázdné → `null`.
- `levels`: mapa spell list → level; výjimky ve zdroji se sjednotí (`wizard` → `sorcerer/wizard`, `oracle` → `cleric/oracle`,
  `summoner (unchained)` → `unchained summoner`). Při duplicitě platí nižší level.
- Spelly bez class listu (jen SLA, domény) se přeskočí.
- `id` = slug jména (`Abadar's Truthtelling` → `abadar-s-truthtelling`), kolize dostanou `-2`, `-3`.
- Parser odmítne soubor s ≤ 100 spelly (ochrana proti stažení nesmyslu).
- `pfsLegal` z pole `PFS legal` (~340 spellů není povolených v Pathfinder Society; chybí-li, bere se jako povolené).

Ze zdroje se čtou i domény, subdomény, mystéria, bloodline a elementární školy (`access_ways.*` → `Spell.subLevels`,
klíč „domain:fire“ → spell level). Zdroj má chyby, které parser opravuje: mezery a velká písmena („ war“, „Plant“),
překlepy („aristocra“, „kyton (evil, kaw)“), varianty „(u)“ a „juju(PZO…)“, slepené záznamy („Elemental School air“
v bloodline) se zahodí. Výsledek: 37 domén, 113 subdomén, 34 mystérií, 25 bloodlin, 7 elementárních škol.

| Classa | Podtřída | Bonusová kouzla |
|---|---|---|
| cleric | 2 domény (+ subdoména ke každé) | doménový slot: 1 kouzlo domény na spell level (`ClassEntry.bonusSpells`) |
| druid | doména Nature Bond – Air, Animal, Earth, Fire, Plant, Water, Weather (+ subdoména, ne Metal) | doménový slot jako cleric |
| oracle | mystérium | známé kouzlo levelu N od úrovně 2N |
| sorcerer | bloodline | známé kouzlo levelu N od úrovně 2N + 1 |
| wizard | škola / elementární škola + opoziční školy | +1 slot na spell level pro kouzla školy; opoziční kouzla s upozorněním (2 sloty) |
| witch | patron (51, Foundry) | kouzlo patrona levelu N od úrovně 2N (familiár, běžné sloty – jen přehled) |
| shaman | spirit (17, Foundry) | magie ducha: 1 spontánní seslání za den na každém spell levelu, který umí |
| bloodrager | bloodline (22, Foundry) | známé kouzlo levelu N od úrovně 3N + 4 (7., 10., 13., 16.) |
| psychic | disciplína (23, Foundry) | známé kouzlo 1. levelu od 1. úrovně, levelu N od 2N |
| warpriest | 2 blessingy (47, Foundry) – jen domény božstva | žádná; schopnosti menší (1.) a větší (10.) |
| inquisitor | 1 doména (+ subdoména) – jen domény božstva | žádná; schopnosti domény |

Patroni, spirity, bloodline bloodragera a disciplíny ve zdroji kouzel nejsou – kouzla se čtou z popisů ve Foundry
(`tools/build_class_abilities.py` → `SUBCLASS_SPELL_TABLE`, jména kouzel spárovaná se `SpellList.json`, „X, Mass“ ↔ „Mass X“,
římské číslice) a `SubclassIndex` je přidá k podtřídám ze zdroje (klíče `patron:`, `spirit:`, `brbloodline:`, `discipline:`).
Blessing → doména (`BLESSING_DOMAINS`) z tagů Foundry; Death a Evil mají ve Foundry chybný tag, proto má přednost jméno blessingu.

Subdoména nahrazuje kouzla své domény na levelech, které uvádí. Vazbu subdoména → doména zdroj neobsahuje – je ručně
v `Subclasses.kt` (`SUBDOMAIN_DOMAINS`, podle tabulky [aonprd.com/ClericDomains.aspx](https://aonprd.com/ClericDomains.aspx)).
Nabízí se jen subdomény zvolené domény (archon, azata, devil… patří ke dvěma doménám); změna domény zruší subdoménu,
která k nové doméně nepatří. Boreal a sleep v tabulce AoN nejsou, takže se nenabízí.

## Itemy

### Model (`Item`)
Společná pole: `name`, `category` (`ItemCategory`), `subtitle` („Martial · One-handed“, „Medium armor“, „Wondrous Item · neck“),
`priceGp`, `weightLb`, `description`, `source`. Podle druhu:
- zbraně: `proficiency`, `damageSmall`, `damageMedium`, `critical`, `range`, `damageType` (B/P/S → Bludgeoning/Piercing/Slashing), `special`
- zbroje: `armorBonus`, `maxDex`, `armorCheck`, `spellFailure`
- magické: `aura`, `casterLevel`, `slot`, `requirements`

`summary` je krátký text do inventáře: `1d8 / 19-20/×2`, `+6 AC, ACP −5`.

### Kategorie
`WEAPON, AMMO, ARMOR, GEAR, ALCHEMICAL, MOUNT, POTION, SCROLL, WAND, RING, ROD, STAFF, WONDROUS, MAGIC_WEAPON, MAGIC_ARMOR, ARTIFACT, OTHER`
(pořadí = pořadí skupin v inventáři).

| Zdroj | Mapování |
|---|---|
| Foundry složky | `adventuring-gear`, `clothing`, `food-and-drink`, `tools-and-skill-kits`, `animal-gear`, `material-components`, `miscellaneous` → GEAR (se subkategorií); `alchemical-items`, `herbs` → ALCHEMICAL; `animals-and-mounts` → MOUNT; `armor-*`, `shields` → ARMOR. Složka `magic-items` se přeskakuje (magické itemy jsou z CSV). |
| `Weapons.csv` | WEAPON (`is_ammunition` → AMMO), typ ruky z `is_light/one_handed/two_handed/ranged/unarmed` |
| `MagicItems.csv` sloupec `Group` | Wondrous Item → WONDROUS, Ring, Rod, Staff, Wand, Potion (+ názvy „Potion…“, „Oil of…“) → POTION, Weapon/Ammunition/Legendary Weapon/Shadow Piercing → MAGIC_WEAPON, Armor → MAGIC_ARMOR, Artifact → ARTIFACT, ostatní (Cursed, Magical Tattoo) → OTHER |

Aktuální počty: WEAPON 243, AMMO 36, ARMOR 70, GEAR 1379, ALCHEMICAL 72, MOUNT 20, POTION 9, WAND 1, RING 228, ROD 235,
STAFF 107, WONDROUS 2505, MAGIC_WEAPON 478, MAGIC_ARMOR 250, ARTIFACT 331, OTHER 96 – **celkem 6 060**.

Lektvary, svitky a hůlky v PF1 vznikají ze spellů, proto jich v databázi skoro není; v appce je pro ně „Custom item“.

### Převod (`ItemParser`)
- `Csv` – vlastní parser podle RFC 4180 (uvozovky, `""`, nové řádky uvnitř pole, BOM).
- `decodeText` – UTF-8, při neplatné sekvenci Windows-1252 (`weapons.csv` je v cp1252; rozbité apostrofy se opraví).
- Ceny: `PriceValue` nebo text „1,500 gp“ / „5 sp“ / „1 cp“ → gp jako `Double`. Váhy: „4 lbs.“, „1/2 lb.“ → lb.
- `id` = `kategorie-slug` (`weapon-longsword`, `magic-bag-of-holding-minor`), duplicity `-2`…
- `combine` spojí všechny zdroje a seřadí podle jména.

### Sestavení přibalených dat – `tools/build_items.py`
1. Stáhne ZIP archivy `packs/items` a `packs/armors-and-shields` z GitLab API Foundry PF1, převede YAML
   (cena ve správné měně → gp, váha, popis z HTML na text, AC/max Dex/ACP/ASF u zbrojí) do `ItemsBase.json`.
2. Stáhne `weapons.csv`, `ammunition.csv` a `magic_items_full…csv`, uloží je jako UTF-8; magické itemy ořízne
   jen na sloupce, které appka čte (originál má 10 MB kvůli HTML sloupci `FullText`).

```bash
pip install pyyaml
python tools/build_items.py
```

URL v `build_items.py` (`CSV_SOURCES`) a v `ItemParser` (`WEAPONS_URL`, `AMMO_URL`, `MAGIC_URL`) musí odpovídat.

### Aktualizace v appce (`ItemRepository.download`)
Stáhne tři CSV z GitHubu (`raw.githubusercontent.com/Vireche/randomitemgenerator/master/data/…`), naparsuje je
(validace), uloží do `AppFiles` (`items_weapons.csv`, `items_ammunition.csv`, `items_magic.csv`) a znovu sestaví databázi.
`ItemsBase.json` (Foundry) zůstává vždy přibalený – je to stovky malých YAML souborů a běžné vybavení PF1 se nemění.

## Pravidla PF1 v kódu

### Spell levely podle úrovně (`CasterClasses.kt`)

| Progrese | Classy | Max spell level na úrovni L |
|---|---|---|
| PREPARED_9 | wizard, cleric, druid, witch, shaman | `min(9, (L+1)/2)` |
| SPONTANEOUS_9 | sorcerer, oracle, psychic, arcanist | `L<4 → 1`, jinak `min(9, L/2)` |
| SIX | bard, skald, inquisitor, magus, alchemist, investigator, summoner (+unchained), warpriest, hunter, mesmerist, occultist, spiritualist | `min(6, (L+2)/3)` |
| FOUR | paladin, antipaladin, ranger, bloodrager | `L<4 → žádné`, jinak `min(4, (L-1)/3)` |
| FOUR_KNACKS | medium | `L<4 → 0` (jen knacky), jinak `min(4, (L-1)/3)` |

- Level 0 (cantripy/orisony/knacks) mají všichni kromě FOUR, alchemista a investigatora.
- Classy s více listy berou nižší level: hunter = hunter/druid/ranger, warpriest = warpriest/cleric,
  skald = skald/bard, investigator = investigator/alchemist, arcanist = sorcerer/wizard, oracle = cleric/oracle.
- Medium (Occult Adventures, Table 1-2): knacky od 1. úrovně, 1st na 4, 2nd na 7, 3rd na 10, 4th na 13.
- Vynechané: adept (NPC classa). Podtřídy viz výše.

### Lektvary, svitky a hůlky ze spellu (`SpellItemKind`)
Cena podle Core Rulebooku: lektvar 50 gp × SL × CL (max 3. level), svitek 25 gp × SL × CL, hůlka 750 gp × SL × CL
(max 4. level, 50 nábojů); spell 0. levelu se počítá jako ½. Výchozí CL = minimum pro daný level (1, 1, 3, 5, 7 …),
level podle zvoleného spell listu. Drahé materiální složky appka jen připomene, k ceně je nepřičítá.

### Classy pro multiclass (`ClassTable.kt`)
`python tools/build_classes.py` (pyyaml) vygeneruje `data/ClassTable.kt` z Foundry VTT PF1 `packs/classes`:
49 class (základní, hybridní, occult, unchained a NPC) s progresí BAB (high = L, med = ¾ L, low = ½ L), dobrými
záchranami (2 + L/2, jinak L/3), HD a skill body. Multiclass = součet za každou classu. Prestižní classy viz níže.

### Rasy (`RaceTable.kt`)
`python tools/build_races.py` (pyyaml) vygeneruje `data/RaceTable.kt` z Foundry VTT PF1 `packs/races`: 80 ras s velikostí,
základní rychlostí, rasovými úpravami atributů, typem a podtypy tvora. Human, half-elf a half-orc nemají ve Foundry pevné
úpravy – mají „+2 k jednomu atributu“ (`anyBonus`). Skupiny podle Advanced Race Guide: základní (7), významné (featured, 16),
neobvyklé (14), ostatní (zbytek – Bestiary, Alien…), seznamy jsou ve skriptu.

- `Character.abilities` jsou **základní** hodnoty, výsledné `Character.scores` = základ + `RaceInfo.mod` (u `anyBonus`
  podle `Character.raceBonus`). Výsledné hodnoty používají prerekvizity, sloty, schopnosti i nosnost; STR zadaná
  v inventáři se přepočte zpět na základ.
- Rasový bonusový feat (`RaceInfo.bonusFeats`): human 1, half-elf 1 (Adaptability – Skill Focus) → `FeatSlots.race`.
- Velikost rasy se propíše do propojeného inventáře (`Inventory.size`, nosnost) – při výběru rasy i při propojení.
- Rasa jako prerekvizita featu: text, který je jménem / id rasy nebo podtypem (elf, human, orc, dwarf, gnome, halfling),
  případně s „subtype“ nebo upřesněním v závorce. Rasa splňuje, když se jménem shoduje nebo má daný podtyp
  (`RaceInfo.countsAs`): half-elf platí jako elf i člověk, half-orc jako orc, drow jako elf, svirfneblin jako gnóm.
  „Human (Varisian)“ s rasou human = ověř ručně, s jinou rasou = nesplněno. Rasové traity viz níže.

### Prestižní classy (`PrestigeTable.kt`, `PrestigeClasses.json`)
Foundry VTT PF1 prestižní classy nemá. `python tools/build_prestige_classes.py [--cache DIR]` stáhne seznam
z Archives of Nethys ([PrestigeClasses.aspx](https://aonprd.com/PrestigeClasses.aspx)) a stránky class (~120 požadavků
po 1 s, cache `build/prestige-cache`):
- **`PrestigeTable.kt`** (`PRESTIGE_TABLE`, `PrestigeInfo`) – HD, skill body, class skills (jména AoN → id skillů appky),
  BAB a záchrany po úrovních přímo z tabulky (prestižní classy mají jiné vzorce záchran), sesílání po úrovních ze sloupce
  „Spells per Day“ (případně Special): „+1 level of (existing) arcane / divine spellcasting class“ → `arcane` / `divine`,
  bez druhu → `any`, „+1 level of witch class“ → `class:witch`; Mystic Theurge `arcane+divine`; bonusové featy (Special
  „bonus feat“), požadavky („Spells: Able to cast 3rd-level arcane spells.“), zdroj, PFS. 118 class, 60 zvyšuje sesílání.
- **`PrestigeClasses.json`** – popis a text schopností (líně, `ensurePrestigeTexts`).

V appce: `ClassCatalog.prestige` (`ClassInfo.prestige`, skupina `ClassGroup.PRESTIGE`, `maxLevel`), `Character.stats` bere
hodnoty z tabulky, `featSlots` bonusové featy. `ClassEntry.advances` (druh → classId, `Prestige.candidates` podle
`CasterClass.tradition`, `defaultAdvances` = jediná vhodná) → **`Character.casterLevel(e)`** = úroveň classy + úrovně
prestižních class, které ji zvyšují; platí pro kouzla za den / známá, nejvyšší spell level, výběr kouzel (`Spellbook.classLevel`),
prerekvizity („caster level“, „able to cast 3rd-level spells“) – schopnosti classy (domény, hexy…) dál podle úrovně classy.
Požadavky: `PrestigeRequirements.evaluate` (štítek + text → text prerekvizity → `FeatPrereqs.evaluate`; „Base Attack Bonus:
+5“ → „base attack bonus +5“, „… and 2nd-level arcane spells“ → dvě části), přísný režim `StrictBlock.PrestigeRequirementUnmet`.

### Archetypy (`Archetypes.json`)
`python tools/build_archetypes.py [--cache DIR]` projde classy z `ClassTable.kt` (bez NPC a unchained) a stáhne
z Archives of Nethys tabulku archetypů classy (`Archetypes.aspx?Class=X`: název, PFS, „Replaces“, shrnutí) a stránku
každého archetypu (`ArchetypeDisplay.aspx?FixedName=X Y`: zdroj, plný text) – ~1300 požadavků po 1 s, cache
`build/archetype-cache`. Výsledek: 1 252 archetypů, id `classa:slug-jména`, `replaces` = schopnosti oddělené „;“
tak, jak je uvádí AoN („Smite Evil“, „3rd-level Mercy“). Načítá se líně (`ensureArchetypes`).

Položky „Replaces“ se dělí podle „;“ i „,“ – seznam úrovní zůstane u své schopnosti („2nd, 4th, 6th-level discovery“).

V appce: `ClassEntry.archetypes` (`ArchetypePick` = id, jméno, kopie `replaces`), výběr pod classou postavy.
- **Mechanika** (`ArchetypeEffects.kt`, `ClassEntry.archetypeEffect`):
  - automaticky z `replaces`: jméno předvyplněné schopnosti (`FeaturePresets`, aliasy Bomb → Bombs, Judgement → Judgment)
    → schopnost zmizí; podtřída classy (`SUBCLASS_FEATURES`: cleric „Domains“, druid „Nature Bond“, wizard „Arcane School“…)
    → `ClassEntry.subclassKind` = null; „Bloodline Powers“ / „Domain Powers“… → bez schopností podtřídy, „Bonus Spells“ /
    „Patron Spells“… → bez bonusových kouzel; „Cantrips“ / „Orisons“ / „Knacks“ → bez triků; hexy / revelations / bonusové
    featy (i combat style a teamwork featy) → o uvedené úrovně ≤ úroveň classy méně (bez úrovní celá schopnost),
  - ručně `ARCHETYPE_MODS` (`ArchetypeMods.kt`, podle textů AoN): `noSpells` (`ClassEntry.caster` = null), `perDayDelta` /
    `knownDelta` (`ClassEntry.levelSlots`, i triky – kromě `deltaIncludesCantrips = false`), `oneDomain`, `subclass`
    (Sacred Servant / Temple Champion: doména paladina), `removes`, `presets` (nové schopnosti s počtem použití),
    `bonusFeatLevels`, `casting` (`CastingChange`: `lists` / `addLists`, `ability`, `kind`, `table` + `progression` –
    tabulka kouzel jiné classy přes `CasterClass.tableId`, `tradition`, `abilityBonus`, `spells` jménem),
    `subclassLevelOffset` (`ClassEntry.subclassLevel` – Sacred Servant −3), `secondBloodline` (`SubclassSlot.BLOODLINE2`).
    Test `BundledDataTest` hlídá, že id, spell listy, tabulky a kouzla existují.
  - **Kouzla navíc** z textu (`Archetype.spells`, kopie v `ArchetypePick.spells`): generátor čte „1st—a, b; 2nd—c“,
    „a (1st), b (2nd)“ a „adds x … as a 3rd-level spell“ v odstavcích o spell listu, jména ověří proti `SpellList.json`
    (bez zkratek knih „APG“, „UM“…, „lesser x“ → „x, lesser“); „2nd—x (1st)“ = úroveň classy + spell level v závorce.
    `ClassEntry.caster` je dá do `CasterClass.extraSpells` (spellLevelOf, výběr kouzel).
  - „Spells“ / „Spellcasting“ v `replaces` automaticky nic nedělá – většinou jde o změněné, ne zrušené sesílání.
  - Druh magie (`CasterClass.tradition`, i změněný archetypem) určuje prerekvizity „ability to cast arcane / divine spells“.
- Konflikt = dva vybrané archetypy mají stejnou položku v `replaces` (bez ohledu na velikost písmen) – „Mercy“
  a „3rd-level Mercy“ appka za konflikt nepovažuje. Přísný režim konfliktní archetyp nepřidá.
- Unchained classy (`rogue_unchained`…) dostanou archetypy základní classy (`Archetypes.baseClass`), NPC classy a rasové
  HD tvorů žádné (`Archetypes.supports`).

### Rasové traity (`RacialTraitTable.kt`, `RacialTraits.json`)
`python tools/build_racial_traits.py [--cache DIR]` stáhne seznam ras z Archives of Nethys
([Races.aspx?Category=Core / NonCore](https://aonprd.com/Races.aspx)) a stránky ras (~80 požadavků po 1 s, cache
`build/race-cache`). Id ras jako v appce (slug jména; Lashunta → `lashunta-female` i `lashunta-male`); Boggard appka nemá,
Gnoll, Lizardfolk a Ogre na AoN stránku rasy nemají.

- **`RacialTraitTable.kt`** – výchozí traity rasy ze sekce „X Racial Traits“ (tučná jména; bez řádku atributů).
  AoN má u některých ras neuzavřené `<b>`, proto se tučný blok dělí po řádcích a jméno je před „:“ nebo „(Ex/Su/Sp)“.
- **`RacialTraits.json`** – 632 alternativních traitů ze sekce „X Alternate Racial Traits“: skupina „Replaces A, B“ →
  `replaces`, jméno, PFS (ikona Pathfinder Society), zdroj, text. Načítá se líně.

V appce: `Character.racialTraits` (`RacialTraitPick` = id, jméno, kopie `replaces`), výběr u rasy (změna rasy výběr zahodí).
- Prerekvizita „X racial trait“: vybraný alternativní trait ✓, nahrazený výchozí ✗, výchozí trait rasy podle AoN ✓,
  jinak dál ruční tabulka a „?“.
- Nahrazený **Bonus Feat** (human) / **Adaptability** (half-elf) → bez rasového bonusového featu; nahrazený **Skilled**
  (human) → bez skill bodu navíc za úroveň.
- **Úpravy atributů** z alternativních traitů (`ABILITY_VARIANTS` v `RacialTraits.kt`, zadané ručně podle textu AoN):
  Witchborn (changeling: +2 INT místo WIS), Keen Kitsune (+2 INT místo CHA), Agile (vine leshy: +2 DEX místo CON),
  Tree-Born (gathlain: bez −2 CON); s volbou atributu (`RacialTraitPick.abilities`) a místo „+2 k jednomu atributu“:
  Dual Talent (human: +2 ke dvěma různým), Kindred-Raised (half-elf: +2 CHA a +2 k jinému), Orc Atavism (half-orc:
  +2 STR, −2 k jednomu mentálnímu). Počítá je `Character.raceMod`, takže platí pro `scores`, nosnost i prerekvizity.
- Dva alternativní traity nesmí nahradit stejný výchozí trait – appka varuje, přísný režim to nedovolí.

### Skilly (`Skills.kt`)
26 skillů Core Rulebooku (Knowledge rozdělená na 10 specializací), id jako ve Foundry VTT PF1 (`disableDevice`,
`knowledge.arcana`); Craft / Perform / Profession mají druhy – klíč ranků `craft:alchemy`. Class skills jsou v `ClassTable.kt`
(`ClassInfo.classSkills` z Foundry `packs/classes`, „knowledge“ = všechny specializace; Background skills artistry / lore
se vynechávají), rasové bonusy a class skills v `RaceTable.kt` (`skillMods`, `classSkills`).

- Celkem (`skillTotal`) = ranky + mod. atributu (`scores`) + 3 za class skill s ≥ 1 rankem + rasový bonus;
  „trained only“ bez ranků = nelze použít. ACP zbroje se nepočítá.
- Skill body (`skillPoints`) = Σ úroveň classy × max(1, body classy + mod. INT) + člověk 1 / úroveň + `extraSkillPoints`
  (oblíbená classa, traity – ručně). Používá se aktuální INT (zvýšení INT během úrovní se nerozlišuje).
- Prerekvizity: „Acrobatics 5 ranks“, „5 ranks in Sense Motive“, „Knowledge (arcana, …, or religion) 5 ranks“,
  „Craft (any) 1 rank“, „1 rank in at least one Knowledge skill“, „Knowledge (nature) or Survival 5 ranks“ (číslo
  z poslední části platí pro všechny). Postava bez zadaných ranků → „ověř ručně“.

### Božstva (`DeityTable.kt`)
`python tools/build_deities.py [stránka.html]` vygeneruje `data/DeityTable.kt` z tabulky
[aonprd.com/DeitiesByGroup.aspx](https://aonprd.com/DeitiesByGroup.aspx): 463 božstev (jméno, panteon, přesvědčení,
oblasti, domény, oblíbená zbraň). Postava ukládá jméno (`Character.deity`), `DeityCatalog.get` ho najde bez ohledu na
velikost písmen; jiný text = vlastní božstvo (jméno se porovnává volně, přesvědčení je neznámé). Klerik s božstvem
ze seznamu volí domény jen z `DeityInfo.domains` a subdomény jen z `DeityInfo.subdomains` (ze stránek jednotlivých božstev
`DeityDisplay.aspx`, ~460 požadavků po 1 s, cache `build/deity-cache`; subdomény podmíněné traitem, „*“, se počítají).
`subdomains = null` = stránka chybí → nabídnou se všechny subdomény domény.

### Schopnosti podtříd z AoN (`SubclassAbilityTable.kt`, `SubclassAbilities.json`)
`python tools/build_subclass_abilities.py [--cache DIR]` stáhne stránky spiritů šamana, bloodline bloodragera, disciplín psychica
a Inquisitions (~70 požadavků, 1 za sekundu, cache `build/subclass-cache`) – Foundry tyto schopnosti s podtřídou nepropojuje:

- **`SubclassAbilityTable.kt`** – `AON_SUBCLASS_POWER_TABLE` (371 schopností: spirit ability 1., greater 8., true 16., manifestation 20.;
  u bloodline, disciplín a inquisitions úroveň z „At Nth level“), počet použití z textu („a number of times per day equal to
  3 + your Wisdom modifier“, „once per day“; s „…, plus 1 round…“ se vzorec vynechá). `DISCIPLINE_POOL_ABILITY` (atribut phrenic
  poolu), `INQUISITION_DEITIES` (prázdné = jakékoli božstvo, i „n/a“).
- **`BLOODLINE_BONUS_FEATS`** – bonusové featy 75 bloodline bloodragera a čaroděje; ze stránek bloodline čaroděje
  (SorcererBloodlines.aspx) i jejich schopnosti – appka je bere jen pro bloodline, které Foundry nemá.
- **`SubclassAbilities.json`** – 82 hexů duchů s textem (formát `ClassAbilities.json`, pole `spirits`); hex se stejným jménem jako
  běžný hex šamana appka vynechá.

### Schopnosti class (`ClassAbilityTable.kt`, `ClassAbilities.json`)
`python tools/build_class_abilities.py [class-abilities.zip] [sources.mjs]` (pyyaml) zpracuje Foundry VTT PF1
`packs/class-abilities` (~4 700 YAML) a názvy knih z `module/registry/sources.mjs` (kód „PZO1115“ → „Advanced Player's Guide“):

- **`ClassAbilityTable.kt`** – schopnosti podtříd: kontejnery domén, subdomén, bloodline (11 z CRB / APG) a škol
  kouzelníka (i elementárních) mají v `system.links.supplements` odkazy na schopnosti s úrovní, kdy je postava získá;
  ze schopnosti se bere `system.uses.maxFormula` (jen „per day“). Klíče: `domain:fire`, `subdomain:ash`,
  `subdomain:archon@good` (subdoména pod dvěma doménami), `bloodline:draconic`, `school:evocation`, `school:element:fire`.
  325 schopností ze 136 podtříd, 240 s počtem použití. Kde Foundry vzorec nemá, ale text ho uvádí („number of rounds per day
  equal to 3 + your Charisma modifier“), skript ho doplní. Druh jednotky: „rounds per day“ v textu → kola.
- **`ClassAbilities.json`** – hexy (tagy Hex / Major Hex / Grand Hex, classy witch a shaman; hexy duchů jsou z AoN – viz výše) a zjevení
  (tag Revelation, oracle, mystéria z tagů „X Mystery“, bez finálních zjevení): text z HTML (odkazy `@UUID[…]{Jméno}` → jméno),
  vzorec použití, Ex / Su / Sp, zdroj, minimální úroveň (velký hex 10, nejvyšší 18, zjevení z textu „must be at least 7th
  level“). 420 položek (117 hexů, 303 zjevení), načítá se líně.

V appce:
- `SubclassPowers.keysOf(ClassEntry)` převede podtřídu postavy na klíče (subdoména má přednost před doménou, kterou
  nahrazuje – její seznam schopností je úplný), `Character.resources()` z nich udělá schopnosti dostupné na úrovni classy.
  Neznámá doména / bloodline → obecná schopnost 1. úrovně (3 + Wis / Cha). Kouzelník se školou z tabulky nemá obecnou
  „Arcane school power“.
- `FoundryFormula` vyhodnotí vzorec: `@class.unlevel` / `@class.level` = úroveň classy, `@attributes.hd.total` = HD postavy,
  `@abilities.X.mod`; funkce `floor`, `ceil`, `min`, `max`, `clamp`, `abs`, `if`, `gt`, `gte`, `lt`, `lte`, `eq`; výsledek
  dolů, nikdy záporný. Skript i test `BundledDataTest` hlídají, že každý vzorec jde spočítat.
- Vybrané hexy / zjevení: `ClassEntry.abilities` (`AbilityPick` = id, jméno, druh, kopie vzorce) – počítadlo funguje i bez
  načtené databáze. Počet (`AbilityRules.slots`): witch 1 + úroveň / 2, shaman na 2., 4., 8., 10., 12., 16., 18., 20.,
  oracle na 1., 3., 7., 11., 15., 19. úrovni.

### Featy (`Feats.json`)
`python tools/build_feats.py` stáhne přehled [aonprd.com/Feats.aspx](https://aonprd.com/Feats.aspx) (jméno, prerekvizity,
shrnutí, PFS) a detail každého featu (typy, zdroj, flavor, sekce Benefit / Normal / Special…). ~3 400 požadavků po 1 s
(AoN odpovídá pomalu → ~3 h), stránky se cachují v `build/feat-cache`, takže přerušený běh pokračuje. Značky AoN ve jménech
(`*` mythic verze, `⊤`, `†`) se odstraní. Featy appka **neaktualizuje** (tolik požadavků z aplikace není rozumné).

Prerekvizity (`FeatPrereqs`): text se rozdělí podle čárek a středníků mimo závorky; ověřují se atributy („Str 13“),
BAB, úroveň postavy, úroveň classy („fighter level 4th“), caster level (nejvyšší úroveň sesílací classy), „ability to cast
Nth-level spells“, rasu (viz výše, když je u postavy vybraná) a featy postavy (u „Weapon Focus (longsword)“ stačí mít
Weapon Focus; „Weapon Focus with the chosen weapon“ a jména s přilepenou zkratkou knihy – „Improved Dirty TrickAPG“ –
se taky najdou). „—“ ve zdroji = feat bez prerekvizit (248 featů). „A or B“ = stačí jedna; společná přípona / předpona
platí pro všechny části („good or lawful alignment“, „Animal or Plant domain“, „Knowledge (nature) or Survival 5 ranks“).
Nic se nevynucuje.

Odvozená pravidla (`PrereqRules.kt`):
- **class features** – tabulka schopnost → classa a úroveň (channel energy, rage, ki pool, wild shape, sneak attack…,
  ~100 položek), u „channel energy 3d6“ / „sneak attack +2d6“ počet kostek; volitelné schopnosti (zvířecí společník
  druida, pozitivní / negativní energie klerika) = „?“. Schopnost z archetypu appka nezná → potvrdit ručně.
- **přesvědčení** (`Character.alignment`) – „good alignment“, „nonlawful“, „chaotic neutral“…; **božstvo** (`deity` = jméno
  ze seznamu nebo vlastní text) – „worshiper of Iomedae“, „must worship a deity“, „worshiper of a good deity“,
  „alignment must match / be within one step of your deity's“ (přesvědčení božstva ze seznamu),
- **velikost, typ a podtyp** podle rasy – „Small size or smaller“, „humanoid“, „giant subtype“; **Hit Dice** = úroveň,
  **základní záchrany** („base Fortitude save bonus +3“),
- **proficience** se zbrojí / štíty / martial zbraněmi z class (`ClassInfo.armorProf/weaponProf` z Foundry) a featů
  (Armor Proficiency…),
- **kouzla** – arcane / divine / psychic sesilatel, „ability to cast 3rd-level divine spells“, „ability to cast cleric spells“,
  konkrétní kouzlo („ability to cast dispel magic“ – je na spell listu sesílající classy a úroveň stačí),
- **domény** klerika / druida (i přes subdoménu), **typ featu** („any teamwork feat“),
- **rasové traity** – výchozí traity ras (stonecunning, keen senses…), darkvision / low-light vision z Foundry;
  výchozí traity všech ras a alternativní traity z AoN (viz Rasové traity výše) – vybraný ✓, nahrazený ✗, jinak „?“.

**Ruční potvrzení** (`Character.confirmedPrereqs`): co appka neověří nebo považuje za nesplněné, potvrdí uživatel
v detailu featu tlačítkem „Mám“ – platí pro všechny featy se stejným textem prerekvizity (export ho přenáší).

Podíl featů s „?“ (postava člověk se zadanými skilly, přesvědčením a božstvem): původně 2 475 → 974.

Počet featů (`featSlots`): 1. a každá lichá úroveň postavy + bonusové featy class (fighter 1 + sudé úrovně, wizard 5/10/15/20,
monk 1/2/6/10/14/18, magus 5/11/17, warpriest a inquisitor/hunter (teamwork) každá 3., brawler 2/5/8…, swashbuckler
a gunslinger každá 4., cavalier/samurai 6/12/18, ranger 2/6/10/14/18, sorcerer 7/13/19, bloodrager 6/9/12/15/18)
+ ruční „Navíc“ (human, traity…).

### Sloty a spells known (`CastingTable.kt`)
`python tools/build_casting.py` (jen standardní knihovna, ~27 požadavků po 1 s) stáhne stránky class z Archives of Nethys
a z jejich tabulek vygeneruje `data/CastingTable.kt`: pro každou z 27 sesílajících class spells per day a spells known
(arcanist: spells prepared). Řádek = úroveň classy 1–20, hodnota pro spell level 0–9, `-` = classa level nemá
(u spontánních tříd triky „libovolně“). Testy (`SlotTableTest`) hlídají shodu s progresí spell levelů a vzorky z CRB.

- `CastingKind`: PREPARED (limit = kopie podle slotů), SPONTANEOUS (limit = známá kouzla, sesílá ze slotů),
  HYBRID (arcanist – limit připravených, sesílá ze slotů). Spontánní/arcanist mají každé kouzlo jen jednou.
- Bonusová kouzla z atributu sesílání (CRB tab. 1–3): od modifikátoru ≥ spell level `(mod − level) / 4 + 1`, triky nemají.
  Kouzlo levelu N vyžaduje atribut ≥ 10 + N (UI jen upozorní). Occultist nemá tabulku known (kouzla dávají implementy).

### Nosnost (`CarryingCapacity`)
Heavy load podle STR 1–29 z Core Rulebooku (10 → 100 lb, 18 → 300 lb, 29 → 1 400 lb), nad 29 každých +10 STR ×4.
Light = ⌊heavy/3⌋, medium = ⌊heavy·2/3⌋. Mince: 50 ks = 1 lb.

Násobitel podle velikosti tvora (`CreatureSize`, uložený v inventáři jako `size` + `quadruped`):

| Velikost | Fine | Diminutive | Tiny | Small | Medium | Large | Huge | Gargantuan | Colossal |
|---|---|---|---|---|---|---|---|---|---|
| dvounohý | ×1/8 | ×1/4 | ×1/2 | ×3/4 | ×1 | ×2 | ×4 | ×8 | ×16 |
| čtyřnohý | ×1/4 | ×1/2 | ×3/4 | ×1 | ×1½ | ×3 | ×6 | ×12 | ×24 |

Postihy zatížení (`Load`): medium – max Dex +3, check penalty −3, běh ×4; heavy – max Dex +1, check −6, běh ×3;
obojí rychlost 30 → 20 ft (20 → 15 ft). Přetížení – jen 5 ft za kolo, bez Dex bonusu k AC.

### Nasazené vybavení (`Equipment.kt`)
Položka inventáře (`InvEntry`) má `equipped`, `slot` (`ItemSlot`: hlava, čelenka, oči, ramena, krk, hruď, tělo, zbroj,
opasek, zápěstí, ruce, 2× prsten, nohy, hlavní ruka, štít) a `bonuses` (`ItemBonus`: cíl, hodnota, typ, u skillu klíč).

- Platí jen nasazené položky **propojeného inventáře**; ve slotu jen tolik, kolik se vejde (první v pořadí inventáře),
  další jsou konflikt (`slotConflicts` → červený banner). Položky bez slotu platí všechny. `AppState.updateInventory`
  a `linkInventory` kopírují bonusy do `Character.gear`.
- Sčítání (`stackBonuses`): bonusy stejného typu se nesčítají – platí nejvyšší; dodge, untyped a circumstance se sčítají;
  postihy se sčítají vždy.
- Cíle: atributy (`gearMod` → `scores`, tedy i bonusová kouzla, skill body, prerekvizity, nosnost), záchrany (`gearSave`),
  AC, skill (`gearSkill`; bonus ke „knowledge“ platí pro všechny Knowledge), `MAX_DEX` (platí nejnižší) a `ARMOR_CHECK`.
- Zbroj / štít z databáze: `ItemBonusGuess.of(Item)` vyplní AC (typ armor / shield), max Dex a postih zbroje.
  Magické předměty podle jména: čelenky a opasky atributů, Cloak of Resistance, Ring of Protection, Amulet of Natural Armor,
  Bracers of Armor; ostatní se zadají ručně.
- Zatížení inventáře (`loadBonuses`) přidá do `gear` max Dex a postih ke skillům (typ untyped).

### Obrana a životy (`Defense.kt`)
- **AC** = 10 + zbroj + štít + Obratnost (omezená max Dex) + velikost (`sizeAcMod`: Small +1, Large −1, Tiny +2…)
  + přirozená zbroj (vybavení, zvířecí společník) + deflection + dodge + ostatní typy + ruční `Character.acMisc`.
  **Dotyk** bez zbroje, štítu a přirozené zbroje; **zaskočený** bez kladné Obratnosti a dodge.
- **Postih zbroje** (`armorCheckPenalty`): zbroj + štít se sčítají, postih zatížení se s nimi nesčítá – platí horší.
  Odečte se od skillů Síly a Obratnosti.
- **HP** (`maxHp`): 1. HD postavy naplno, další průměr (polovina + 1, jako PFS), rasové HD tvorů průměr (4,5 za d8);
  + mod. Odolnosti za HD (min. 1 HP za HD u class), Toughness (+3, od 4. HD +1 za HD), `hpBonus`.
  `hpOverride` (házené HP) přebije výpočet. Aktuální HP = max − `damage` (může být záporné).
- Odpočinek (`AppState.rest`) vyléčí 1 HP za úroveň postavy. Export přenáší `hpBonus`, `hpOverride` a `acMisc`,
  zranění ne (stejně jako seslaná kouzla).

### Zvířecí společník (`AnimalCompanions.kt`)
Tabulka „Animal Companion Base Statistics“ z Core Rulebooku (`CompanionProgression`, efektivní úroveň druida 1–20):
HD, přirozená zbroj, bonus Str/Dex, triky navíc, zvláštní schopnosti (link, share spells, evasion, ability increase,
devotion, multiattack, improved evasion). HD, BAB, záchrany, skill body a featy dává rasová „classa“ `animal`
(`ClassCatalog.creatures`) – úroveň společníka jen nastaví její úroveň.

- `SpeciesCatalog`: 17 zvířat CRB (ručně, stabilní id) + 140 z `SpeciesTable.kt` (`EXTRA_SPECIES`, GENEROVANÉ
  `tools/build_companions.py` z d20pfsrd – sekce „Animal Companion Descriptions“, bez obsahu třetích stran – zdroj
  opengamingstore.com) se startovními atributy, velikostí, rychlostí, útoky a vylepšením na 4. nebo 7. úrovni
  (velikost, úpravy Str / Dex / Con, přirozená zbroj, útoky). Čtyřnohost odhaduje generátor podle jména.
- `companionMod` (bonus Str/Dex + vylepšení) jde do `scores`, `naturalArmor` do AC.
- `suggestedCompanionLevel`: druid a hunter plně, ranger úroveň − 3; jezdecké zvíře i paladin a cavalier.

### Familiár (`Familiars.kt`, `FamiliarTable.kt`)
`tools/build_companions.py` čte z d20pfsrd tabulku familiárů (89, jméno, bonus pánovi, zdroj). Nepodmíněný bonus
(„+3 bonus on Stealth checks“, „+2 bonus on Reflex saves“, „+1 natural armor bonus to AC“, „+3 hit points“) → `ItemBonus`
(skill / záchrana / AC / nový cíl `BonusTarget.HP`), podmíněný (iniciativa, „in bright light“, „to identify“) jen text.
AppState drží kopii bonusů od familiárů u pána (`Character.familiarBonus`, `syncFamiliarBonus` při změně / smazání
mazlíčka a importu); `Character.allBonuses` = vybavení + familiár (skilly, záchrany, AC, HP). Progrese podle úrovně pána
(`companionLevel` familiára, `FamiliarProgression`): přirozená zbroj (úroveň + 1) / 2, Int 5 + (úroveň + 1) / 2,
SR úroveň + 5 od 11., schopnosti; HD familiára = úroveň pána (rasové HD „animal“). Statistiky tvora čte generátor ze
stránky bestiáře, na kterou odkazuje tabulka (statblok `<p class="title">`, víc tvorů na stránce → nejbližší název;
„use butterfly statistics“ → statistiky jiného familiára; 86 z 89, Crab king, Penguin a Spiny Starfish nemají stránku).
`withCompanion` je dá familiárovi při změně druhu nebo dokud má výchozí atributy.

### Eidolon (`Eidolons.kt`, `EidolonTable.kt`, `Eidolons.json`)
`python tools/build_eidolons.py [--cache DIR]` (d20pfsrd, stránky eidolonů klasického a unchained summonera):
tabulky progrese (HD, BAB, záchrany, skilly, featy, zbroj, Str/Dex, body evolucí, max. útoků, schopnosti), základní
formy (velikost, rychlost, přirozená zbroj, dobré záchrany, útok, atributy, volné evoluce; unchained s id „u_…“),
podtypy unchained a 79 evolucí s cenou (bez [3PP]) → `Eidolons.json` (líně, `ensureEidolonData`).
Eidolon = mazlíček s `companion = EIDOLON`, `eidolonForm`, `eidolonSubtype`, `evolutions` (`EvolutionPick` – kopie jména
a ceny); `companionLevel` = úroveň summonera, rasové HD `eidolon` (plný BAB, d10, 4 skill ranky na HD, dobré záchrany
podle formy v `Character.stats`). Mechanicky: Str/Dex z tabulky + Ability Increase (+2, `EvolutionPick.ability`),
přirozená zbroj forma + tabulka + Improved Natural Armor (+2), Large → velikost Large.

### Metamagie (`Metamagic.kt`)
Zvýšení slotu se čte z textu featu (AoN): „uses up a spell slot two levels higher“ → 2, „does not use up
a higher-level spell slot“ / „same spell level“ → 0, jinak (Heighten Spell) neznámé. `ofCharacter` vrací
metamagické featy postavy z databáze (bez opakování, vlastní featy ne) – detail kouzla ukáže výslednou úroveň slotu.

### Přísný režim (`StrictMode.kt`)
`AppState.strictMode` (Settings `strict_mode`). Před přidáním kontroluje `StrictRules`:
- **kouzlo**: classa ho na své úrovni sesílá, atribut sesílání ≥ 10 + spell level a na spell levelu je místo
  (připravené kopie = sloty + bonus z atributu + slot školy specialisty; spontánní / arcanist = známá / připravená kouzla),
- **feat**: počet featů (`featSlots`) není vyčerpaný a žádná prerekvizita není ✗ („?“ projde).

Ubírat jde vždy; import přísný režim nekontroluje.
