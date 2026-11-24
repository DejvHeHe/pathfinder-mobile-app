# Notices & licenses

**PF1 Spellbook** – Copyright (C) 2026 David Sobíšek.

The source code of this project is free software: you can redistribute it and/or modify it under the terms of the
**GNU General Public License version 3** as published by the Free Software Foundation (see [`LICENSE`](LICENSE)).
It is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

The game content bundled with or downloaded by the app (spells, items, feats, class, race and deity tables) is **not**
covered by the GPL – it remains under its own licenses listed below.

## Privacy

The app collects no personal data: no account, no tracking, no ads. All user data (characters, inventories, settings) is
stored only on the device / in the browser. The app connects to the internet only when the user asks it to download an
updated spell or item database (GitHub). A website hosting the web version may use cookie-less, anonymous analytics
(Umami) configured by its operator – page visits and anonymous app events (only the kind of action, e.g. "inventory opened",
"character created"; never names, notes or character contents).

## Paizo Community Use

This app uses trademarks and/or copyrights owned by Paizo Inc., used under Paizo's Community Use Policy
([paizo.com/licenses/communityuse](https://paizo.com/licenses/communityuse)). We are expressly prohibited from charging
you to use or access this content. This app is not published, endorsed, or specifically approved by Paizo.
For more information about Paizo Inc. and Paizo products, visit [paizo.com](https://paizo.com).

## Open Game License

Game mechanics and rules text of Pathfinder Roleplaying Game (1st edition) spells, items, feats, classes and races are Open Game Content under
the **Open Game License v1.0a** – full text including the Section 15 copyright notice:
[`licenses/OGL-1.0a.txt`](licenses/OGL-1.0a.txt) (the same text is shown in the app under *About & licenses*).

Designation of Product Identity: proper names of the Pathfinder campaign setting (deities, places, characters, etc.)
that appear in spell and item names or descriptions, the "Pathfinder" name and trademarks are Product Identity of Paizo Inc.
and are used under the Community Use Policy above, not under the OGL.
Designation of Open Game Content: the game rules text of spells, feats, items, classes and races (statistics and
descriptions) as published under the OGL by their respective publishers.

## Data sources

| Data | Source | License |
|---|---|---|
| Spells (`SpellList.json`) | [SpellTable-pf1e](https://github.com/iociveteres/SpellTable-pf1e) by Vladislav Sergeev – data from [Archives of Nethys](https://aonprd.com) | project MIT ([`licenses/MIT-SpellTable-pf1e.txt`](licenses/MIT-SpellTable-pf1e.txt)); content OGL 1.0a / Paizo CUP |
| Weapons, ammunition, magic items (`Weapons.csv`, `Ammunition.csv`, `MagicItems.csv`) | [Vireche/randomitemgenerator](https://github.com/Vireche/randomitemgenerator) – data from [d20pfsrd.com](https://www.d20pfsrd.com) | repository without a license; content OGL 1.0a / Paizo CUP |
| Feats (`Feats.json`) | [Archives of Nethys](https://aonprd.com/Feats.aspx) – collected by `tools/build_feats.py` | content OGL 1.0a / Paizo CUP |
| Spells per day / spells known (`CastingTable.kt`) | class tables on [Archives of Nethys](https://aonprd.com) – `tools/build_casting.py` | content OGL 1.0a / Paizo CUP |
| Deities – pantheon, alignment, areas, domains, subdomains, favored weapon (`DeityTable.kt`) | [Archives of Nethys](https://aonprd.com/DeitiesByGroup.aspx) and the deity pages – `tools/build_deities.py` | deity names and descriptions are Paizo Product Identity – used under the Paizo Community Use Policy |
| Subdomain → domain table (`Subclasses.kt`) | [Archives of Nethys – Cleric Domains](https://aonprd.com/ClericDomains.aspx) | content OGL 1.0a / Paizo CUP |
| Races – size, speed, ability and skill modifiers, vision (`RaceTable.kt`) | `packs/races` of the [Pathfinder 1e system for Foundry VTT](https://gitlab.com/foundryvtt_pathfinder1e/foundryvtt-pathfinder1) – `tools/build_races.py` | project GPL-3.0; content OGL 1.0a |
| Class BAB, saves, hit dice, class skills, proficiencies (`ClassTable.kt`) | `packs/classes` of the [Pathfinder 1e system for Foundry VTT](https://gitlab.com/foundryvtt_pathfinder1e/foundryvtt-pathfinder1) – `tools/build_classes.py` | project GPL-3.0; content OGL 1.0a |
| Domain, subdomain, bloodline and wizard school powers – level and uses per day (`ClassAbilityTable.kt`); witch / shaman hexes and oracle revelations with text (`ClassAbilities.json`) | `packs/class-abilities` of the [Pathfinder 1e system for Foundry VTT](https://gitlab.com/foundryvtt_pathfinder1e/foundryvtt-pathfinder1), book names from its `module/registry/sources.mjs` – `tools/build_class_abilities.py` | project GPL-3.0; content OGL 1.0a |
| Animal companion progression and Core Rulebook animal companions (`AnimalCompanions.kt`) | Core Rulebook – Druid, Animal Companions, as published on [d20pfsrd.com](https://www.d20pfsrd.com/classes/core-classes/druid/animal-companions/) (entered by hand) | content OGL 1.0a / Paizo CUP |
| Mundane gear, armor & shields, alchemical items, mounts (`ItemsBase.json`) | converted from the compendium packs of the [Pathfinder 1e system for Foundry VTT](https://gitlab.com/foundryvtt_pathfinder1e/foundryvtt-pathfinder1) | project GPL-3.0; content OGL 1.0a |

Rules implemented in code (spell slots, carrying capacity, skills, feat prerequisites, default racial traits, item prices)
follow the Core Rulebook, Advanced Player's Guide, Advanced Race Guide and other books listed in the OGL Section 15.

## Fonts

| Font | License |
|---|---|
| Alegreya (bold instances generated from the variable font) | SIL Open Font License 1.1 – [`licenses/OFL-Alegreya.txt`](licenses/OFL-Alegreya.txt) |
| Alegreya Sans | SIL Open Font License 1.1 – [`licenses/OFL-AlegreyaSans.txt`](licenses/OFL-AlegreyaSans.txt) |
| IBM Plex Mono | SIL Open Font License 1.1 – [`licenses/OFL-IBMPlexMono.txt`](licenses/OFL-IBMPlexMono.txt) |

## Libraries

Kotlin, Kotlin Multiplatform, Compose Multiplatform, kotlinx.coroutines, kotlinx.serialization, Ktor,
Multiplatform Settings, AndroidX – all under the **Apache License 2.0**
([apache.org/licenses/LICENSE-2.0](https://www.apache.org/licenses/LICENSE-2.0)).
