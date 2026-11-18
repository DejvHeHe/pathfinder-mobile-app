# Uživatelská příručka

Appka má dvě části, mezi kterými přepínáte záložkami **Spellbook | Inventory**:
na telefonu jsou pod nadpisem „PF1 Spellbook“, na desktopu v horní liště.

Rozhraní je **anglicky nebo česky** – přepíná se tlačítkem **EN | CZ** v hlavičce (na telefonu vedle nadpisu,
na desktopu v horní liště). Při prvním spuštění se vybere podle jazyka systému a volba se pamatuje.
Názvy a popisy spellů a itemů zůstávají v originále (anglicky); classy a školy magie se v češtině zobrazí česky
(v nabídce class i s anglickým originálem, např. „Kouzelník (Wizard)“).

## Rozložení obrazovky

- **Telefon (úzké okno):** jedna obrazovka za druhou, zpět šipkou vlevo nahoře
  (na Androidu i systémovým tlačítkem Zpět). Hlavní akce je zelené tlačítko vpravo dole.
- **Desktop / tablet na šířku (od 1000 px):** tři sloupce:
  1. vlevo seznam postav nebo inventářů,
  2. uprostřed otevřený deck / inventář / výběr,
  3. vpravo detail spellu nebo itemu (zavře se křížkem nebo klávesou **Esc**).

## Spellbook

### Postava
1. **New character** → jméno, classa a úroveň. Výběr classy má hledání, classy jsou seskupené podle počtu úrovní kouzel
   (9 / 6 / 4 / bez kouzel / NPC) a pod seznamem je náhled spell levelů pro zvolenou úroveň.
2. **Multiclass:** v detailu postavy v panelu **Classes** → **Add class** (i nesesílací – fighter, rogue, swashbuckler…).
   Úroveň každé classy se mění stepperem, celkem max. 20; classu jde odebrat (×) i s jejími kouzly.
3. Pod classami je souhrn – úroveň postavy, **BAB** a záchrany (součet za všechny classy) – a **atributy** STR–CHA
   (klepnutím upravíte hodnotu, vedle je modifikátor).
4. Každá sesílací classa má vlastní sekci kouzel s tlačítkem **Add spells**.

Původní „decky“ se při prvním spuštění nové verze automaticky převedly na postavy (se všemi kouzly).

### Inventář postavy
- Sekce **Inventář** v detailu postavy: **Propojit inventář** (existující) nebo **Nový inventář** (pojmenovaný po postavě).
- Propojený inventář bere **sílu pro nosnost z postavy** – změna SÍL u postavy i stepperu Síla v inventáři se projeví na obou
  místech. U postavy je vidět souhrn (váha, zatížení), **Otevřít** přejde do inventáře, × propojení zruší.
- Jeden inventář patří nejvýš jedné postavě (propojení s jinou postavou ho od té původní odpojí); společný „Party stash“
  může zůstat nepropojený.

### Podtřídy (domény, bloodline, mystérium, škola)
- Pod classou, která podtřídu má, je odkaz (např. **Domény: vybrat…**) – otevře výběr:
  - **Klerik**: 2 domény, ke každé volitelně subdoména (nahrazuje část kouzel domény); **druid**: doména.
  - **Orákulum**: mystérium, **čaroděj**: bloodline.
  - **Kouzelník**: škola specializace (nebo univerzalista / elementární škola) a opoziční školy.
- Klerik / druid: pod kouzly je sekce **Doménová kouzla** – na každé úrovni kouzel jeden doménový slot (počet „x / 1“).
- Orákulum / čaroděj: sekce **Kouzla mystéria / bloodline** – známá automaticky od příslušné úrovně, v herním režimu se sesílají ze slotů.
- Kouzelník specialista má u každé úrovně kouzel „+1 škola“; kouzla opoziční školy mají rezavé ⚠ (příprava stojí 2 sloty).
- Podtřída se ukládá u postavy i v exportu. Patron (čarodějnice), spirit (šaman) a bloodline bloodragera zatím nejsou (chybí data).

### Schopnosti s počtem použití
- Sekce **Schopnosti s počtem použití** se sama naplní podle class postavy: Smite Evil, Lay on Hands, Channel Energy,
  Rage, Bardic Performance, Ki Pool, Arcane Pool, Grit, Panache, Judgment, Challenge, Wild Shape, Fervor, Blessings,
  Bombs, Inspiration, Martial Flexibility, schopnost školy kouzelníka… Maximum se počítá z úrovně classy a atributu.
- Nepotřebnou schopnost skryjete ×, „Zobrazit skryté“ je vrátí.
- **Přidat počítadlo** = vlastní položka (název, maximum, jednotka: za den / kol za den / body, poznámka) – rasové
  schopnosti, magické předměty, náboje hůlky…
- V **herním režimu** se použití odklikává (puntíky, u velkých čísel − / +), **Odpočinek** vše obnoví.

### Featy
- Sekce **Featy** v detailu postavy ukazuje „vybrané / dostupné“: featy za úrovně (1. a každá lichá) + bonusové featy class
  (bojovník, kouzelník, mnich, magus…) + ruční **Navíc** (např. human, traity).
- **Přidat feat** otevře přehled ~3 400 featů z Archives of Nethys: hledání, typ, „Jen featy, na které mám“, PFS, hledání v textu.
  U každého featu je stav prerekvizit: ✓ splněno, ✗ nesplněno, ? ověř ručně (skill ranky, rasa…). Appka nic nezakazuje.
- Klepnutím se otevře detail s plným textem a prerekvizitami po jedné. U featu postavy lze doplnit **poznámku**
  (např. zvolenou zbraň u Weapon Focus).
- **Vlastní feat** = název + popis (homebrew, rasové schopnosti…), u postavy jde upravit tužkou v detailu.
- Featy z databáze se neaktualizují tlačítkem (jsou přibalené k appce).

### Kouzla postavy
1. U každé sesílací classy vidíte rozsah spell levelů, které na své úrovni sesílá (např. *Wizard 5 · spell levels 0–3*).
2. **Add spells** otevře výběr, který nabízí **jen spelly, které daná classa na své úrovni může sesílat**:
   - hledání v názvu (volitelně i v popisu – „search descriptions too“),
   - filtr školy a spell levelu, řazení podle názvu, úrovně nebo školy,
   - „hide added“ skryje spelly, které už postava má, „favorites only“ nechá jen oblíbené,
     „PFS legal only“ jen spelly povolené v Pathfinder Society.
3. Tlačítko **+** u spellu přidá kopii; u postavy pak počty upravíte **− / +** (0 = odebrat).
   Spontánní classy (sorcerer, bard, oracle…) a arcanist mají každé kouzlo jen jednou – odebírá se křížkem.
   U každého spell levelu je vpravo limit: „3 / 3+1 připraveno“ (základ + bonus z atributu) nebo
   „2 / 4 známých · 6+1 za den“. Červeně = překročený limit nebo příliš nízký atribut sesílání.
4. **Herní režim** (zaškrtávátko pod atributy) slouží u stolu: připravující classy odklikávají seslané kopie
   (seslané kouzlo se přeškrtne), spontánní classy a arcanist mají u každého spell levelu řadu slotů
   a u kouzla tlačítko **Seslat**. Triky jsou libovolně. **Odpočinek** vše obnoví. Seslaná kouzla se ukládají u postavy.
4. Klepnutím na spell se otevře detail: casting time, components, range, duration, save, SR, celý popis,
   mythic verze, zdroj a odkaz na Archives of Nethys.

**Oblíbené:** hvězdička v detailu spellu ho přidá do oblíbených (platí pro všechny postavy i kompendium).
Oblíbené spelly mají v seznamech malou hvězdičku.

Úroveň classy můžete kdykoli změnit. Když ji snížíte a některý spell už není dostupný,
zůstane u postavy s varovnou ikonou ⚠.

### Kompendium
Ikona knihy (telefon) / tlačítko **Compendium** (desktop) – všechny spelly s filtrem podle classy, školy a levelu.
Na desktopu se do kompendia dostanete i psaním do horního vyhledávání.

## Inventory

### Inventář = majetek jedné postavy (nebo skupiny)
1. **New inventory** → název (např. „Elara“ nebo „Party stash“).
2. **Purse** – mince PP / GP / SP / CP; klepnutím na minci zadáte počet.
3. **Load** – celková váha včetně mincí (50 mincí = 1 lb) a zatížení podle **Strength** (tabulka z Core Rulebooku):
   - pruh rozdělený na úseky light / medium / heavy se značkami hranic a ukazatelem aktuální váhy,
   - barevný štítek stavu (**Light** olivová, **Medium** okrová, **Heavy** rezavá, **Overloaded** červená)
     a pod ním postihy (max Dex, check penalty, rychlost, běh),
   - **Size** (velikost tvora) a **Four legs** (čtyřnohý tvor – kůň, pes) upraví nosnost,
   - když přidání itemu (nebo změna STR, mincí…) posune postavu do vyššího zatížení, appka na to upozorní.
4. Itemy jsou seskupené podle kategorií (Weapons, Armor & shields, Adventuring gear, Potions…), u každého
   počet (− / +), váha, cena a krátká poznámka (např. *1d8 / 19-20/×2*, *+6 AC, ACP −5*).
5. **Notes** – volný text (klíče, dluhy, kde je schovaná truhla…).

Na desktopu je vlevo i seznam kategorií – klepnutím zobrazíte jen jednu. Horní pole „Search item…“ filtruje otevřený inventář.

### Přidání itemu – **Add item**
- Vyhledávání v ~6 000 itemech, filtr **kategorie**, u zbraní **Simple / Martial / Exotic**, a **max cena** v gp.
- **+** přidá 1 kus rovnou. Klepnutí na řádek otevře detail (na telefonu spodní panel), kde nastavíte
  **množství a poznámku** (např. *masterwork, found in crypt*) a dáte **Add to inventory**.
- **Custom item** – vlastní věc, která v databázi není (kořist, speciální výbava…): název, kategorie, cena, váha, krátké info.
  Se zaškrtnutým **Save to my items** se uloží i do vlastního katalogu.
- **My items** – vlastní katalog sdílený všemi inventáři: co jednou vytvoříte, přidáte pak kamkoli jedním **+**
  (v „All“ jsou vlastní itemy nahoře). V detailu jde item z katalogu odebrat; vlastní položku z inventáře jde do katalogu uložit.
- **From spell** – lektvar, svitek nebo hůlka ze spellu: vyberete druh, spell, spell list (určuje level) a úroveň sesilatele,
  appka spočítá cenu podle pravidel (lektvar 50 gp × level × CL, svitek 25 gp × …, hůlka 750 gp × … s 50 náboji).

Klepnutím na položku v inventáři otevřete její detail: změna počtu, poznámky, odebrání; vlastní item jde i upravit.

### Skryté ceny drahých itemů
Itemy dražší než **200 gp** jsou normálně vidět, ale cena se ukáže jen jako **„??? gp“**
(a u inventáře s takovým itemem i celková hodnota „worth ≈ ???“).

**Odhalení:** ťukněte **10× rychle za sebou kamkoli na obrazovku** (mezi ťuknutími max. ~0,7 s).
Objeví se hláška *All prices revealed*. Dalších 10 ťuknutí ceny zase skryje (*High prices hidden*).
Odhalení platí jen do zavření appky.

## Export a import

Funguje pro postavy, featy i inventáře. Výsledkem je čitelný text (JSON), který jde poslat čímkoli.

| Akce | Kde |
|---|---|
| Export jedné postavy / inventáře | ikona **Sdílet** (šipka nahoru) v jejím detailu |
| Export všech | menu **⋮ → Exportovat všechny postavy / inventáře** |
| Import | menu **⋮ → Importovat postavu / inventář** |
| Export / import jen featů | tlačítka **Export / Import** v sekci Featy postavy |

- **Postava** se exportuje celá: classy a úrovně, atributy, kouzla, featy (i vlastní a poznámky), „Navíc“.
  Má-li propojený inventář, jde ho zaškrtnutím **Včetně propojeného inventáře** přibalit – po importu se založí a propojí.
  Seslaná kouzla z herního režimu se neexportují.
- **Featy** se importují do otevřené postavy (přidají se k existujícím); featy, které postava už má a nejdou vzít víckrát,
  se přeskočí. Import featů přijme i export celé postavy (vezme z něj featy).
- Staré exporty decků (z verze 1.0) jdou dál importovat – z decku vznikne postava s jednou classou.

- **Export:** *Copy* (do schránky) a *Save to file* (desktop) nebo *Share* (Android, iOS).
- **Import:** *Paste from clipboard* nebo *Open file* (desktop, Android), případně text vložit ručně.
  Před potvrzením uvidíte náhled; spelly, které v databázi nejsou, se vypíšou a vynechají.
  Importované decky/inventáře se přidají jako nové (při shodě jména dostanou „(imported)“) – nic se nepřepíše.

## Aktualizace databází

Menu **⋮ → Spell & item database / update**:

- **Download latest spell database** – stáhne aktuální seznam spellů.
- **Download latest item database** – stáhne aktuální tabulky zbraní, munice a magických itemů.
- **Restore bundled version** – vrátí verzi přibalenou v appce.

Pathfinder 1e je uzavřený systém, takže aktualizace přinese hlavně opravy. Decky a inventáře zůstávají –
spelly se párují podle id a jména, položky inventáře si nesou vlastní název, cenu a váhu.

## Kde jsou moje data

Vše je jen v zařízení:

| Platforma | Decky, inventáře | Stažené databáze |
|---|---|---|
| Android | SharedPreferences appky | soukromá složka appky |
| iOS | NSUserDefaults | složka Documents appky |
| Desktop | Java Preferences (Windows: registr `HKCU\Software\JavaSoft\Prefs`) | `~/.pfspells/` |
| Web | `localStorage` prohlížeče (pro danou adresu) | IndexedDB prohlížeče |

Odinstalací appky (Android/iOS) nebo vymazáním dat webu v prohlížeči se data smažou – pro zálohu použijte **Export all**.
Na webu jsou data vázaná na prohlížeč a adresu stránky.

## Webová verze

Appka běží i v prohlížeči (Chrome/Edge 119+, Firefox 120+, Safari 18.2+). První načtení stáhne ~26 MB, další už jdou z cache.
Export ukládá soubor do Stažených, import otevře výběr souboru. Esc i tlačítko Zpět prohlížeče vrátí předchozí obrazovku.
