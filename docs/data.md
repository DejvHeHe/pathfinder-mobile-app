# Data

Všechna data jsou **přibalená v appce** (`composeApp/src/commonMain/composeResources/files/`), takže appka funguje offline.
Část jde tlačítkem v appce stáhnout znovu ze zdroje.

| Soubor | Obsah | Zdroj | Aktualizace v appce |
|---|---|---|---|
| `SpellList.json` (6 MB) | 3 040 záznamů → 3 030 spellů s class listem | [SpellTable-pf1e](https://github.com/iociveteres/SpellTable-pf1e) `data/SpellList.json` (MIT, data z Archives of Nethys) | ano |
| `ItemsBase.json` (0,7 MB) | 1 541 položek: výstroj, oblečení, nástroje, jídlo, alchymie, byliny, zvířata, zbroje a štíty | [Foundry VTT PF1](https://gitlab.com/foundryvtt_pathfinder1e/foundryvtt-pathfinder1) `packs/items`, `packs/armors-and-shields` (OGL) | ne (jen přibalené) |
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

Ve zdroji jsou i domény, subdomény, mystéria, bloodline a elemental school (`access_ways.*`) – appka je zatím nepoužívá.

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

- Level 0 (cantripy/orisony/knacks) mají všichni kromě FOUR, alchemista a investigatora.
- Classy s více listy berou nižší level: hunter = hunter/druid/ranger, warpriest = warpriest/cleric,
  skald = skald/bard, investigator = investigator/alchemist, arcanist = sorcerer/wizard, oracle = cleric/oracle.
- Vynechané: medium, adept. Domény, mystéria, patroni, bloodline zatím ne.

### Nosnost (`CarryingCapacity`)
Heavy load podle STR 1–29 z Core Rulebooku (10 → 100 lb, 18 → 300 lb, 29 → 1 400 lb), nad 29 každých +10 STR ×4.
Light = ⌊heavy/3⌋, medium = ⌊heavy·2/3⌋. Počítá se pro tvora velikosti Medium. Mince: 50 ks = 1 lb.
