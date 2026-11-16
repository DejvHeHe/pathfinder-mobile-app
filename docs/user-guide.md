# Uživatelská příručka

Appka má dvě části, mezi kterými přepínáte záložkami **Spellbook | Inventory**:
na telefonu jsou pod nadpisem „PF1 Spellbook“, na desktopu v horní liště.

Rozhraní je anglicky; popisy spellů a itemů jsou v originále.

## Rozložení obrazovky

- **Telefon (úzké okno):** jedna obrazovka za druhou, zpět šipkou vlevo nahoře
  (na Androidu i systémovým tlačítkem Zpět). Hlavní akce je zelené tlačítko vpravo dole.
- **Desktop / tablet na šířku (od 1000 px):** tři sloupce:
  1. vlevo seznam decků nebo inventářů,
  2. uprostřed otevřený deck / inventář / výběr,
  3. vpravo detail spellu nebo itemu (zavře se křížkem nebo klávesou **Esc**).

## Spellbook

### Deck = spellbook jedné postavy
1. **New deck** → název, classa (26 sesílacích class) a úroveň 1–20.
2. Pod nadpisem decku vidíte rozsah spell levelů, které classa na této úrovni sesílá (např. *Wizard 5 · spell levels 0–3*).
3. **Add spells** otevře výběr, který nabízí **jen spelly, které postava může sesílat**:
   - hledání v názvu (volitelně i v popisu – „search descriptions too“),
   - filtr školy a spell levelu,
   - „hide added“ skryje spelly, které už v decku jsou.
4. Tlačítko **+** u spellu přidá kopii; v decku pak počty upravíte **− / +** (0 = odebrat).
5. Klepnutím na spell se otevře detail: casting time, components, range, duration, save, SR, celý popis,
   mythic verze, zdroj a odkaz na Archives of Nethys.

Úroveň postavy můžete v decku kdykoli změnit. Když ji snížíte a některý spell už není dostupný,
zůstane v decku s varovnou ikonou ⚠.

### Kompendium
Ikona knihy (telefon) / tlačítko **Compendium** (desktop) – všechny spelly s filtrem podle classy, školy a levelu.
Na desktopu se do kompendia dostanete i psaním do horního vyhledávání.

## Inventory

### Inventář = majetek jedné postavy (nebo skupiny)
1. **New inventory** → název (např. „Elara“ nebo „Party stash“).
2. **Purse** – mince PP / GP / SP / CP; klepnutím na minci zadáte počet.
3. **Load** – celková váha včetně mincí (50 mincí = 1 lb) a ukazatel zatížení podle **Strength**
   (light / medium / heavy load podle tabulky z Core Rulebooku, pro tvora velikosti Medium).
4. Itemy jsou seskupené podle kategorií (Weapons, Armor & shields, Adventuring gear, Potions…), u každého
   počet (− / +), váha, cena a krátká poznámka (např. *1d8 / 19-20/×2*, *+6 AC, ACP −5*).
5. **Notes** – volný text (klíče, dluhy, kde je schovaná truhla…).

Na desktopu je vlevo i seznam kategorií – klepnutím zobrazíte jen jednu. Horní pole „Search item…“ filtruje otevřený inventář.

### Přidání itemu – **Add item**
- Vyhledávání v ~6 000 itemech, filtr **kategorie**, u zbraní **Simple / Martial / Exotic**, a **max cena** v gp.
- **+** přidá 1 kus rovnou. Klepnutí na řádek otevře detail (na telefonu spodní panel), kde nastavíte
  **množství a poznámku** (např. *masterwork, found in crypt*) a dáte **Add to inventory**.
- **Custom item** – vlastní věc, která v databázi není (svitek, lektvar, kořist…): název, kategorie, cena, váha, krátké info.

Klepnutím na položku v inventáři otevřete její detail: změna počtu, poznámky, odebrání; vlastní item jde i upravit.

### Skryté ceny drahých itemů
Itemy dražší než **200 gp** jsou normálně vidět, ale cena se ukáže jen jako **„??? gp“**
(a u inventáře s takovým itemem i celková hodnota „worth ≈ ???“).

**Odhalení:** ťukněte **10× rychle za sebou kamkoli na obrazovku** (mezi ťuknutími max. ~0,7 s).
Objeví se hláška *All prices revealed*. Dalších 10 ťuknutí ceny zase skryje (*High prices hidden*).
Odhalení platí jen do zavření appky.

## Export a import

Funguje stejně pro decky i inventáře. Výsledkem je čitelný text (JSON), který jde poslat čímkoli.

| Akce | Kde |
|---|---|
| Export jednoho decku / inventáře | ikona **Sdílet** (šipka nahoru) v jeho detailu |
| Export všech | menu **⋮ → Export all decks / Export all inventories** |
| Import | menu **⋮ → Import deck / Import inventory** |

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
Export ukládá soubor do Stažených, import otevře výběr souboru. Esc = zpět.
