# TODO – PF1 Spellbook

Seznam plánovaných vylepšení. Hotové položky odškrtnout (`- [x]`), nové přidávat podle priority.

## Hlavní vylepšení

### 1. Hezčí výběr classy
Teď je to obyčejný rozbalovací seznam 26 class v dialogu „New deck“.

- [ ] Místo dropdownu mřížka / karty class (název + krátký popis), seskupené podle typu sesílání:
  - plní sesilatelé (9 spell levelů), sesilatelé do 6. levelu, do 4. levelu
  - případně štítky *arcane / divine / psychic* a *prepared / spontaneous*
- [ ] Hledání / filtr v seznamu class
- [ ] Hned při výběru ukázat, jaké spell levely má classa na zvolené úrovni (náhled se mění se stepperem úrovně)
- [ ] Stejný výběr použít i při změně classy existujícího decku (teď jde měnit jen úroveň)
- [ ] Odpovídat designu „Linkovaný zápisník“ (panel s čárkovaným okrajem, vybraná karta olivová)

### 2. Výběr podclassy (archetypu) a spelly podle něj
Po výběru classy volitelně vybrat archetyp / podclassu, která ovlivní nabízené spelly.

- [ ] Volby, které mění spell list nebo přidávají bonusové spelly:
  - Cleric / Warpriest – **domény** (a subdomény)
  - Oracle – **mystérium**
  - Sorcerer / Bloodrager – **bloodline**
  - Wizard – **škola specializace** (+ zakázané školy), elemental school
  - Witch – **patron**, Shaman – **spirit**, Druid – doména
  - archetypy, které mění seznam spellů nebo dávají sesílání i classám bez spellů
    (např. Vigilante – Warlock / Zealot)
- [ ] Data: domény, subdomény, mystéria, bloodline a elemental school jsou už ve zdroji spellů
  (`access_ways` v `SpellList.json`) – appka je zatím nečte. Archetypy obecně budou potřebovat další zdroj dat (AoN).
- [ ] V pickeru spellů zobrazit bonusové spelly archetypu zvlášť (např. sekce „Domain spells“) a u zakázaných škol upozornit
- [ ] Uložit archetyp v decku → nová verze formátu decku (`decks_v2`) + migrace a export/import formátu `version: 2`

### 3. Multiclass
Postava s úrovněmi ve víc classách (např. Swashbuckler 5 / Wizard 3).

- [ ] Deck = postava se seznamem class a úrovní (celková úroveň postavy = součet)
- [ ] Spelly se počítají zvlášť pro každou sesílající classu (vlastní spell levely, vlastní sekce v decku)
- [ ] Umožnit i classy bez spellů (Swashbuckler, Fighter, Rogue…) – započítají se do úrovně postavy, ale spelly nepřidávají
- [ ] Prestižní classy, které zvyšují sesílací úroveň existující classy (Eldritch Knight, Mystic Theurge, Arcane Trickster…)
- [ ] Migrace stávajících decků (jedna classa → seznam s jednou položkou), export/import nové verze

### 4. Zvýraznění aktuálního zatížení (load) v inventáři
Teď je jen tenký pruh a malý text „Medium load · light ≤ 33 lb…“.

- [ ] Výrazný štítek aktuálního stavu: **Light / Medium / Heavy / Overloaded** s barvou (např. olivová → okrová → rezavá → červená)
- [ ] Pruh rozdělený na úseky light / medium / heavy se značkami hranic, aktuální váha jako ukazatel
- [ ] U Medium / Heavy load ukázat postihy podle pravidel PF1:
  - Medium: max Dex +3, armor check −3, rychlost 30 → 20 ft, běh ×4
  - Heavy: max Dex +1, armor check −6, rychlost 30 → 20 ft, běh ×3
- [ ] Upozornění, když přidání itemu posune postavu do vyššího zatížení
- [ ] Volitelně velikost tvora (Small = ¾ nosnosti, Large = ×2 …) a čtyřnohé tvory

### 5. Podpora featů
Databáze featů a jejich výběr pro postavu – podobně jako spelly v decku.

- [ ] Databáze featů: název, typ (Combat, Metamagic, Item Creation, Teamwork, Critical, Style…), prerekvizity, benefit,
  normal, special, zdroj
  - zdroj dat: Foundry VTT PF1 (`packs/feats`, GPL + OGL – stejně jako běžné vybavení), případně AoN / d20pfsrd;
    stejný princip jako spelly a itemy (přibalená verze + aktualizace tlačítkem, pokud zdroj umožní)
- [ ] Prohlížeč featů (kompendium): hledání, filtr podle typu, detail s plným textem
- [ ] Featy u postavy – nejlépe společně s multiclassem (bod 3), aby deck byl „postava“ se spelly i featy
  - [ ] kontrola prerekvizit (úroveň, BAB, atributy, jiné featy) – aspoň upozornění, nevynucovat
  - [ ] počet featů podle úrovně postavy (1 + každá lichá úroveň) + bonusové featy classy (fighter, wizard…)
- [ ] Metamagické featy propojit se spelly (např. u spellu ukázat, o kolik levelů ho metamagie zvýší)
- [ ] Export/import featů spolu s postavou; doplnit nový zdroj dat do `NOTICE.md`, OGL Section 15 a About obrazovky

## Další nápady

- [ ] Spell sloty za den podle classy/úrovně + bonusové sloty z atributu; režim „hra“ s odškrtáváním seslaných spellů a resetem po odpočinku
- [ ] Spontánní sesilatelé: „spells known“ místo počtu kopií
- [ ] Doplnit classu **Medium** (zatím vynechaná kvůli neobvyklé progresi)
- [ ] Řazení, oblíbené spelly, filtr PFS legal (pole `PFS legal` ve zdroji)
- [ ] Vlastní katalog custom itemů sdílený mezi inventáři (teď patří custom item jen do jednoho inventáře)
- [ ] Generování lektvarů / svitků / hůlek ze spellů (cena podle pravidel PF1)
- [ ] Ikona appky (Android, iOS, desktop, favicon webu)
- [ ] Web: tlačítko Zpět v prohlížeči propojit s navigací appky
- [ ] Web: Umami události z appky (`umami.track(...)` – otevření inventáře, vytvoření decku…)
- [ ] iOS: gesto swipe-back

## Vydání / infrastruktura

- [ ] Stabilní podpisový klíč pro Android APK (secrets v GitHubu – postup v `docs/deploy.md`)
- [ ] Vyzkoušet appku na skutečném Android zařízení
- [ ] Vyzkoušet iOS build na Macu (`docs/ios-install.md`)
- [ ] Rozšířit Section 15 v OGL o další použité knihy
