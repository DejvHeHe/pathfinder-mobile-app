# TODO – PF1 Spellbook

Seznam plánovaných vylepšení. Hotové položky odškrtnout (`- [x]`), nové přidávat podle priority.

## Hlavní vylepšení

### 0. Čeština
- [x] Rozhraní EN / CZ s přepínačem v hlavičce (`Strings.kt`)
- [x] České názvy class, škol magie, subškol a spell listů (v nabídce class i s originálem, např. „Kouzelník (Wizard)“)

### 1. Hezčí výběr classy
- [x] Karty class seskupené podle počtu spell levelů (9 / 6 / 4 / bez kouzel / NPC), hledání, náhled spell levelů pro úroveň
- [x] Stejný výběr pro novou postavu i přidání classy (`ui/ClassPicker.kt`), design „Linkovaný zápisník“
- [ ] Štítky *arcane / divine / psychic* a *prepared / spontaneous* na kartách
- [ ] Výběr podtřídy rovnou jako krok v pickeru (teď se nastavuje odkazem pod classou v detailu postavy)

### 2. Podtřídy a spelly podle nich
- [x] Data z `access_ways` (`SpellParser.parseSubLevels`, `Spell.subLevels`, `SubclassIndex`) včetně oprav chyb ve zdroji
- [x] Cleric 2 domény, druid doména (+ subdomény), doménový slot 1 / spell level (`ClassEntry.bonusSpells`)
- [x] Oracle mystérium, sorcerer bloodline – bonusová známá kouzla podle úrovně
- [x] Wizard specializace / elementární škola + opoziční školy: školní slot, upozornění u opozičních kouzel
- [x] Uložení v postavě (`ClassEntry.subclass`) a v exportu postavy
- [ ] Witch – **patron**, Shaman – **spirit**, Bloodrager – **bloodline**, Psychic – **discipline** (ve zdroji spellů nejsou data)
- [ ] Warpriest / inquisitor – domény a blessingy (bez bonusových kouzel – jen schopnosti)
- [ ] **Lepší rozdělení podtříd:**
  - [ ] druid jen domény Nature Bond (Air, Animal, Earth, Fire, Plant, Water, Weather + jejich subdomény), teď nabízí všech 37
  - [ ] subdomény seskupit pod jejich doménu (zdroj vazbu neobsahuje – teď výběr ze všech 113)
  - [ ] výběr podtřídy jako krok rovnou ve výběru classy
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
- [ ] Další schopnosti (bloodline / domain powers 3 + atribut, hexy, revelations…) – případně z Foundry `class-abilities` (vzorce `uses`)

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
- [ ] **Rasa postavy** (výběr ze seznamu) – automatické ověření rasových prerekvizit featů (~400 featů má „?“ kvůli rase);
  případně i přesvědčení / božstvo (~200 featů)
- [ ] **Skills** u postavy (ranky, class skills, bonusy z atributů) – ověření prerekvizit typu „Acrobatics 5 ranks“ (~650 featů)
- [ ] Ruční potvrzení „?“ prerekvizit (uložené u postavy) → přísný režim povoleno / zakázáno
- [ ] Metamagické featy propojit se spelly (např. u spellu ukázat, o kolik levelů ho metamagie zvýší)
- [x] Export/import featů spolu s postavou (i samostatně); nový zdroj dat v `NOTICE.md`, OGL Section 15 a About

## Další nápady

- [x] Spell sloty za den podle classy/úrovně + bonusové sloty z atributu; režim „hra“ s odškrtáváním seslaných spellů a resetem po odpočinku
- [ ] Spontánní sesilatelé: „spells known“ místo počtu kopií
- [x] Doplnit classu **Medium** (knacky od 1. úrovně, spelly 1.–4. levelu jako paladin)
- [x] Řazení spellů (název / úroveň / škola) a filtr PFS legal
- [x] Oblíbené spelly (hvězdička v detailu, filtr „favorites only“)
- [x] Vlastní katalog custom itemů sdílený mezi inventáři („Moje předměty“)
- [x] Generování lektvarů / svitků / hůlek ze spellů (cena podle pravidel PF1)
- [ ] Ikona appky (Android, iOS, desktop, favicon webu)
- [x] Web: tlačítko Zpět v prohlížeči propojit s navigací appky
- [ ] Web: Umami události z appky (`umami.track(...)` – otevření inventáře, vytvoření decku…)
- [x] iOS: gesto swipe-back (na Macu zatím nevyzkoušeno)

## Vydání / infrastruktura

- [ ] Stabilní podpisový klíč pro Android APK (secrets v GitHubu – postup v `docs/deploy.md`)
- [ ] Vyzkoušet appku na skutečném Android zařízení
- [ ] Vyzkoušet iOS build na Macu (`docs/ios-install.md`)
- [x] Rozšířit Section 15 v OGL o další použité knihy (21 hlavních knih s autory z Paizo PRD a d20pfsrd)
