# Design brief – PF1 Spellbook („Linkovaný zápisník“)

> **Stav 28. 11. 2026:** podle tohoto briefu vznikl v Claude Design redesign **„Deník dobrodruha“** a je implementovaný
> (záložky detailu postavy, papírové prvky, animace – viz `ui/Journal.kt`). Tokeny a komponenty jsou v design systému
> „Linkovaný zápisník“ v Claude Design. Body 1–6 v části „Co doladit“ návrh pokryl, tmavá varianta zůstává otevřená.

Podklad pro Claude Design: současný vzhled appky a co na něm doladit. Jen UI – funkce zůstávají, jak jsou.
Původní návrh: artboardy „4a · Linkovaný zápisník“ (mobil + desktop), „Inventory“, „Add item“. Část novějších
obrazovek a panelů vznikla bez návrhu a potřebuje sjednotit.

Appka: offline list postavy a inventář pro Pathfinder 1e. Mobil, tablet, desktop i web; texty EN / CZ
(čeština bývá o 20–40 % delší). Jen text, čísla a ikony – žádné obrázky.

## 1. Vizuální jazyk – současné tokeny

List z linkovaného zápisníku: teplý papír, inkoust, čárkované okraje jako tužkou, olivová pro akce,
rezavá pro nadpisy. Jen světlý režim.
### Barvy
| Token | Hex | Použití |
|---|---|---|
| `paper` | `#E6D8BF` | podklad obrazovky (s linkami po 28 dp) |
| `panel` | `#F2E8D5` | panely, karty, hlavička širokého okna, dialogy |
| `inset` | `#EADCC2` | vnořené panely, vybraná položka seznamu |
| `tag` | `#DCCBAA` | podklad štítků |
| `ink` | `#2E2418` | hlavní text |
| `inkMuted` | `#5E4D38` | vedlejší text, popisky |
| `placeholder` | `#6E5B45` | placeholdery |
| `accent` (olivová) | `#4E5D2B` | hlavní akce, vybraná karta, odkazy, zaškrtávátka |
| `rust` (rezavá) | `#8A4B2A` | nadpisy sekcí, zvýraznění hodnot, popisky dlaždic |
| `ochre` | `#9A6B14` | varování / střední stav, sekundární zvýraznění |
| `error` | `#9A2A1A` | chyby, překročené limity, nesplněno |
| `dash` | `#9C8662` | čárkované okraje |
| `rule` / `ruleSoft` | `#A8916A` / 45 % | plné linky pod řádky seznamu |
| `line` | `#4E5D2B` @ 13 % | linky papíru |

### Typografie

- **Alegreya Bold / Bold Italic** – nadpisy, jména, hodnoty v dlaždicích
  (titulek obrazovky 40 sp desktop / 24 sp mobil, logo 28 sp italic, nadpis sekce 15–20 sp).
- **Alegreya Sans** (Regular, Medium, Bold) – text a tlačítka (15–16 sp, vedlejší 12,5–13,5 sp).
- **IBM Plex Mono** (Regular, Medium, SemiBold) – malé popisky VERZÁLKAMI s prostrkáním (10,5–11,5 sp),
  čísla, ceny, váhy, limity.

### Tvary, linky, ikony

- Radius **4 dp** všude.
- **Čárkovaný okraj** 1,5 dp (čárka 3×, mezera 2× tloušťky) – karty, sekundární tlačítka, oddělovače sloupců.
- Řádky seznamů oddělené **plnou linkou** 1 dp.
- Ikony: **tahové 24×24** (tloušťka 1,8–2,2, kulaté konce): zpět, koš, plus, lupa, kniha, zavřít, sdílet, tužka,
  menu, fajfka, varování, obnovit, šipka dolů, hvězda.
- Ikona appky: tyrkysová d20 (`#2A9D8F` → `#155750`) na linkovaném papíře – tyrkysová zatím v UI není.

## 2. Rozložení

- **Mobil (< 1000 dp)**: jedna obrazovka. Hlavička = zpět + titulek + podtitulek + ikony akcí; hlavní akce jako plné
  olivové tlačítko nebo FAB vpravo dole. Detaily na celou obrazovku, výběr itemu jako bottom sheet.
- **Široké okno (≥ 1000 dp)**: hlavička přes celou šířku (logo, záložky **Spellbook | Inventory**, EN | CZ, menu) →
  **3 sloupce**: sidebar 300 dp · hlavní sloupec · pravý detail 380–500 dp. Sloupce oddělené čárkovanou linkou.

## 3. Komponenty

| Komponenta | Současný vzhled |
|---|---|
| Kostra obrazovky | titulek (Alegreya Bold), podtitulek (muted), akce vpravo |
| Hlavní tlačítko | plné olivové, bílý text, výška 44 dp, volitelná ikona |
| Sekundární tlačítko | průhledné, čárkovaný okraj, text ink, výška 44 dp |
| Ikonové tlačítko | ikona v ploše 44 × 44 dp |
| Štítek | podklad `tag`, SemiBold 12,5 sp, barva textu podle významu |
| Malý štítek (karta class) | 11,5 sp, padding 6 × 1 dp |
| Panel | `panel` / `inset`, čárkovaný okraj, padding 14 × 12 dp |
| Nadpis sekce | rezavý Alegreya Bold + linka do konce řádku + volitelný text vpravo (mono) |
| Mono popisek | malé verzálky, muted nebo rust |
| Stepper | − číslo +, čtvercová tlačítka 30–36 dp |
| Filtr chip | mono, výška 34 dp, vybraný olivový |
| Zaškrtávátko | checkbox (olivový) + text |
| Dropdown | text + šipka dolů, menu na `panel` |
| Vyhledávání | pole s lupou |
| Dlaždice hodnoty | panel s čárkovaným okrajem: mono popisek (rust) + velké číslo (Alegreya) + malý mono řádek |
| Pruh zatížení | úseky light / medium / heavy, značka aktuální váhy, štítek stavu v barvě |
| Dialog | na `panel`, radius 4 dp |
| Toast | krátká hláška dole |

## 4. Obrazovky (struktura, odshora)

**Úvod** – logo, EN | CZ, kniha, menu; záložky Spellbook / Inventory; karty postav (jméno, podtitulek, rozsah);
pod postavou **odsazené menší karty mazlíčků**; FAB.

**Výběr classy (dialog)** – hledání; karty class ve skupinách s nadpisy; karta = název + originál + „d8“ vpravo,
pod tím 2 **barevné malé štítky**; vybraná karta olivová. Vpravo / pod tím: jméno, rasa, božstvo, úroveň, podtřída.

**Detail postavy** – jeden dlouhý scroll, 12 sekcí:
1. (mazlíček) odkaz na vlastníka + štítek rarity + tlačítko nastavení; panel zvířete (název, velikost, stepper úrovně,
   řádky statistik); odstavec „Když zemře“
2. panel class (`inset`): rasa, přesvědčení, božstvo, classy se steppery, odkazy na podtřídu, přidat classu
3. řádek souhrnu (úroveň · BAB · záchrany)
4. atributy – **6 dlaždic** v řadě + 1–3 řádky nápovědy
5. obrana a životy – **4 dlaždice** (AC, Dotyk, Zaskočen, Životy „30 / 42“), rozpis, stepper HP, tlačítko Upravit
6. inventář – souhrnná věta + Otevřít + ×, řádek bonusů vybavení
7. mazlíčci – karty + přidat
8. featy – počet, řádky s ikonou stavu (✓ ✗ ?), tlačítka
9. skills – řádky s ranky a součty
10. schopnosti s počtem použití – řádky s puntíky / stepperem
11. přepínač herního režimu + Odpočinek
12. kouzla – nadpis classy, nadpisy spell levelů s limity vpravo (mono, červeně při překročení), řádky kouzel se
    steppery; v herním režimu puntíky / tlačítko Seslat

**Výběr kouzel / kompendium** – hledání, dropdowny, řada zaškrtávátek, řada chipů úrovní, řádky kouzel (+).

**Detail kouzla** – velký název + hvězda, řada štítků, panel statistik (popisek : hodnota), popis, sekce Metamagie
(řádky, červeně nad limitem), zdroj.

**Detail featu** – štítky, rezavý řádek metamagie, prerekvizity po řádcích (ikona stavu + text + „Mám“), text sekcí.

**Inventář** – mince, pruh zatížení, položky po kategoriích (řádek: název, stepper, váha · cena · poznámky
„Nasazeno (Čelenka)“), červený řádek konfliktu slotů, poznámky. Desktop: tabulka Item · Qty · Weight · Price · Notes.

**Detail položky** – štítky, panel statistik, popis, množství, poznámka, **Vybavení** (zaškrtávátko + dropdown slotu,
řádky bonusů s ×, přidat bonus), tlačítka.

**Dialogy** – nový / nastavení mazlíčka (dlouhý formulář: pole, dropdowny, zaškrtávátko, stepper, víceřádkový text),
přidat bonus, obrana (stepper, pole, stepper).

## 5. Co doladit (priority)

1. **Detail postavy** – 12 sekcí v jednom scrollu je nepřehledných. Vizuální členění (záložky / segmenty / kotvy),
   co je nahoře na první pohled, kompaktnější sekce.
2. **Sjednotit panely bez návrhu** – dlaždice obrany vs. atributů, panel mazlíčka, sekce Vybavení, karty mazlíčků,
   malé štítky na kartách class, položka přísného režimu v menu (text + popis + checkbox).
3. **Barvy stavů** – jasný systém: chyba / nad limit, varování, splněno, neověřeno, neaktivní (nenasazeno, skryté).
   Dnes rezavá slouží pro nadpisy i hodnoty a splývá.
4. **Hustota na mobilu (360–390 dp)** – řádky se steppery, 6 dlaždic atributů a 4 dlaždice obrany v jedné řadě.
5. **Hierarchie textu** – příliš mnoho velikostí mezi 12 a 15 sp; sjednotit škálu.
6. **Prázdné stavy** – prázdný seznam kouzel, inventář, mazlíčci.
7. (volitelně) **tmavá varianta** palety.

## 6. Omezení

- Jen tři přibalené fonty (web nemá náhradní) – v textech žádné ozdobné symboly ani emoji, místo nich ikony.
- Ikony ve stylu stávajících tahových 24×24; chybí např. štít, srdce, meč, tlapka, oko.
- Popisky musí unést delší češtinu – žádné pevné šířky textu.
- Dotykové cíle aspoň 44 dp; mobil od 360 dp, desktop běžně 1360 × 880.

## 7. Výstup z Claude Design

- Artboardy **mobil 390 × 844** a **desktop 1360 × 880**: detail postavy, detail kouzla, inventář + detail položky,
  list mazlíčka, výběr classy.
- Úpravy tokenů jako tabulka (hex, sp, dp).
- Specifikace upravených / nových komponent s rozměry a stavy (výchozí, vybraný, neaktivní, chyba).

## Přílohy

Screenshoty `docs/images/*.png` jsou starší (před postavami a mazlíčky); aktuální náhledy přiložím zvlášť.
