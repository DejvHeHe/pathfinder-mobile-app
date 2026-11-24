# TODO – PF1 Spellbook

Seznam plánovaných vylepšení. Hotové položky odškrtnout (`- [x]`), nové přidávat podle priority.

## Hlavní vylepšení

### 0. Čeština
- [x] Rozhraní EN / CZ s přepínačem v hlavičce (`Strings.kt`)
- [x] České názvy class, škol magie, subškol a spell listů (v nabídce class i s originálem, např. „Kouzelník (Wizard)“)

### 1. Hezčí výběr classy
- [x] Karty class seskupené podle počtu spell levelů (9 / 6 / 4 / bez kouzel / NPC), hledání, náhled spell levelů pro úroveň
- [x] Stejný výběr pro novou postavu i přidání classy (`ui/ClassPicker.kt`), design „Linkovaný zápisník“
- [x] Štítky *arcane / divine / psychic* (+ *extracts* u alchemisty a investigatora) a *prepared / spontaneous* na kartách
- [x] Výběr podtřídy rovnou v pickeru podle vybrané classy (nová postava i přidání classy; změna dál odkazem pod classou)

### 2. Podtřídy a spelly podle nich
- [x] Data z `access_ways` (`SpellParser.parseSubLevels`, `Spell.subLevels`, `SubclassIndex`) včetně oprav chyb ve zdroji
- [x] Cleric 2 domény, druid doména (+ subdomény), doménový slot 1 / spell level (`ClassEntry.bonusSpells`)
- [x] Klerik volí jen domény a subdomény svého božstva (božstvo ze seznamu AoN, subdomény ze stránek božstev)
- [x] Oracle mystérium, sorcerer bloodline – bonusová známá kouzla podle úrovně
- [x] Wizard specializace / elementární škola + opoziční školy: školní slot, upozornění u opozičních kouzel
- [x] Uložení v postavě (`ClassEntry.subclass`) a v exportu postavy
- [ ] Witch – **patron**, Shaman – **spirit**, Bloodrager – **bloodline**, Psychic – **discipline** (ve zdroji spellů nejsou data)
- [ ] Warpriest / inquisitor – domény a blessingy (bez bonusových kouzel – jen schopnosti)
- [x] **Lepší rozdělení podtříd:**
  - [x] druid jen domény Nature Bond (Air, Animal, Earth, Fire, Plant, Water, Weather + jejich subdomény kromě Metal)
  - [x] subdomény jen ke zvolené doméně (`SUBDOMAIN_DOMAINS` podle AoN; boreal a sleep v tabulce chybí)
  - [x] výběr podtřídy jako krok rovnou ve výběru classy
- [ ] **Archetypy** (Grey Paladin, Sacred Servant…, 1 000+): seznam z AoN (název, classa, zdroj, nahrazené class features)
  stáhnout jako featy, u classy postavy vybrat jeden či víc a ukázat jejich text – jen evidence, mechanicky nepočítat
  - archetypy měnící spell list nebo sesílání (Sacred Servant, Thassilonian Specialist, Vigilante – Warlock / Zealot…) jen informativně

### 3. Multiclass
- [x] Postava = seznam class a úrovní (součet max 20), spelly zvlášť pro každou sesílající classu
- [x] Classy bez spellů (Fighter, Rogue, Swashbuckler…) z Foundry – BAB, záchrany, HD
- [x] Migrace decků → postav (`characters_v1`), export/import nové verze (`pf-spellbook-characters`)
- [ ] Prestižní classy, které zvyšují sesílací úroveň existující classy (Eldritch Knight, Mystic Theurge, Arcane Trickster…)

### 3b. Schopnosti s počtem použití
- [x] Vlastní počítadla u postavy (název, maximum, jednotka, poznámka), v herním režimu odklikávání, Odpočinek
- [x] Předvyplněné schopnosti běžných class se vzorci (Smite Evil, Lay on Hands, Channel, Rage, Ki, Arcane Pool, Grit…)
- [x] Doménové schopnosti klerika / druida (3 + Wis) a bloodline čaroděje (3 + Cha) podle podtřídy
- [x] Hexy, revelations, schopnosti vyšších úrovní domén a bloodline
  - [x] `tools/build_class_abilities.py` (vzor `tools/build_races.py`): Foundry `packs/class-abilities` → generovaná tabulka
    (název, classa, podtřída – doména / bloodline / mystérium / škola, úroveň, `system.uses.maxFormula` → vzorec max použití)
  - [x] vyšší schopnosti domén (6./8. úr.) a bloodline (3./9./15./20. úr.) i škol kouzelníka z Foundry (`ClassAbilityTable.kt`, `SubclassPowers`)
    – bloodline z pozdějších knih a domény Jungle / Inevitable Foundry nemá → obecná schopnost 1. úrovně
  - [x] hexy (witch, shaman) a revelations (oracle): výběr ze seznamu u classy postavy + počet použití, kde ho mají
  - [x] nový zdroj dat → `NOTICE.md`, OGL Section 15, About
  - [ ] hexy patrona / spiritu, featy Extra Hex / Extra Revelation do počtu (teď jen podle úrovně)

### 4. Zvýraznění aktuálního zatížení (load) v inventáři
- [x] Výrazný štítek aktuálního stavu: **Light / Medium / Heavy / Overloaded** s barvou (např. olivová → okrová → rezavá → červená)
- [x] Pruh rozdělený na úseky light / medium / heavy se značkami hranic, aktuální váha jako ukazatel
- [x] U Medium / Heavy load ukázat postihy podle pravidel PF1:
  - Medium: max Dex +3, armor check −3, rychlost 30 → 20 ft, běh ×4
  - Heavy: max Dex +1, armor check −6, rychlost 30 → 20 ft, běh ×3
- [x] Upozornění, když přidání itemu posune postavu do vyššího zatížení
- [x] Volitelně velikost tvora (Small = ¾ nosnosti, Large = ×2 …) a čtyřnohé tvory

### 5. Podpora featů
Databáze featů a jejich výběr pro postavu – podobně jako spelly v decku.

- [x] Databáze featů: název, typ (Combat, Metamagic, Item Creation, Teamwork, Critical, Style…), prerekvizity, benefit,
  normal, special, zdroj
  - zdroj: Archives of Nethys (`tools/build_feats.py`), jen přibalená verze bez aktualizace tlačítkem; NOTICE / OGL / About doplněné
- [x] Prohlížeč featů (výběr k postavě): hledání, filtr podle typu, detail s plným textem (samostatné kompendium featů zatím ne)
- [x] Featy u postavy – nejlépe společně s multiclassem (bod 3), aby deck byl „postava“ se spelly i featy
  - [x] kontrola prerekvizit (úroveň, BAB, atributy, jiné featy) – aspoň upozornění, nevynucovat
  - [x] počet featů podle úrovně postavy (1 + každá lichá úroveň) + bonusové featy classy (fighter, wizard…)
- [x] **Rasa postavy** (80 ras z Foundry `packs/races`) – ověření rasových prerekvizit featů, velikost → nosnost inventáře, export
  - [x] výchozí rasové traity (tabulka v `PrereqRules`), darkvision / low-light vision z Foundry
  - [ ] alternativní rasové traity (Halfling Jinx…) – teď jen ruční potvrzení. Foundry VTT PF1 rasové traity **nemá**
    (žádný `packs/racial-traits`, ani ve `feats` / `races`) → zdroj jen Archives of Nethys (stránky ras, sekce Alternate Racial Traits);
    výběr u rasy, nahrazené výchozí traity se odeberou z prerekvizit (`PrereqRules`)
  - [x] výběr rasy rovnou v dialogu nové postavy, úpravy atributů (+2 podle volby), bonusový feat (human, half-elf)
  - [x] přesvědčení / božstvo
- [x] **Skills** u postavy (ranky, class skills z Foundry, bonusy z atributů a rasy, skill body) – ověření prerekvizit
  typu „Acrobatics 5 ranks“ (featů s „?“ při vyplněných skillech 2 131 → 1 728)
- [x] Ruční potvrzení „?“ / ✗ prerekvizit (uložené u postavy, tlačítko „Mám“ v detailu featu)
- [x] Odvozené prerekvizity: class features, velikost / typ, HD, záchrany, proficience, druh kouzel, konkrétní kouzla, domény
  (featů s „?“ 2 475 → 974 u vyplněné postavy)
- [x] přísný režim (menu ⋮ / zaškrtávátko v pickerech): featy jen do počtu a bez ✗ prerekvizit, kouzla jen do limitu spell levelu
- [x] Metamagické featy propojit se spelly (u spellu výsledná úroveň slotu, u featu zvýšení – parsováno z textu AoN)
- [x] Export/import featů spolu s postavou (i samostatně); nový zdroj dat v `NOTICE.md`, OGL Section 15 a About

## Další nápady

- [x] **Mazlíčci a společníci** (zvířecí společník, jezdecké zvíře, familiár, eidolon) jako postava s vlastníkem – rasové HD Animal / Magical Beast, velikost → nosnost
  - [x] progrese zvířecího společníka podle úrovně druida (bonus Str/Dex, přirozená zbroj, triky) a 17 zvířat z Core Rulebooku
  - [ ] další zvířata (APG, Ultimate Magic…), familiáři (bonusy pánovi, přirozená zbroj a Int podle úrovně) a eidolon

- [x] Spell sloty za den podle classy/úrovně + bonusové sloty z atributu; režim „hra“ s odškrtáváním seslaných spellů a resetem po odpočinku
- [x] Spontánní sesilatelé: „spells known“ místo počtu kopií (limit „x / y známých“ z `CastingTable`)
- [x] Doplnit classu **Medium** (knacky od 1. úrovně, spelly 1.–4. levelu jako paladin)
- [x] Řazení spellů (název / úroveň / škola) a filtr PFS legal
- [x] Oblíbené spelly (hvězdička v detailu, filtr „favorites only“)
- [x] Vlastní katalog custom itemů sdílený mezi inventáři („Moje předměty“)
- [x] Generování lektvarů / svitků / hůlek ze spellů (cena podle pravidel PF1)
- [x] Ikona appky (Android, iOS, desktop, favicon webu) – `art/icon.svg` → `tools/build_icons.py`
- [x] Web: tlačítko Zpět v prohlížeči propojit s navigací appky
- [x] Web: Umami události z appky (`umami.track(...)` – otevření inventáře, vytvoření decku…)
- [x] iOS: gesto swipe-back

- [x] **Nasazené vybavení**: položka inventáře nasazeno / slot / bonusy (atributy, záchrany, AC, skills; sčítání typů PF1), odhad bonusu podle jména magického předmětu
  - [x] AC (dotyk, zaskočený), max Dex, postih zbroje ke skillům, HP (1. HD max, průměr, Toughness, házené HP), zranění a léčení odpočinkem
  - [ ] (odloženo – appka zatím nemá útoky) CMB / CMD, útoky zbraní

## Vydání / infrastruktura

- [ ] Stabilní podpisový klíč pro Android APK (secrets v GitHubu – postup v `docs/deploy.md`)
- [x] Vyzkoušet appku na skutečném Android zařízení
- [x] Vyzkoušet iOS build na Macu (`docs/ios-install.md`)
- [x] Web: v `docker/nginx.conf` dát `.js` / `.wasm` místo `max-age=3600` `no-cache` (revalidace přes ETag) – po nové verzi
  teď prohlížeč až hodinu ukazuje starou appku, dokud uživatel nedá Ctrl+F5
- [x] CI: job `ios` (macOS runner) – pipeline znovu zelená
- [x] Rozšířit Section 15 v OGL o další použité knihy (21 hlavních knih s autory z Paizo PRD a d20pfsrd)
