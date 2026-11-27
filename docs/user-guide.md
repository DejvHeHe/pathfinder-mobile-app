# Uživatelská příručka

**Čeština** | [English](en/user-guide.md)

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
  2. uprostřed otevřená postava / inventář / výběr,
  3. vpravo detail spellu nebo itemu (zavře se křížkem nebo klávesou **Esc**).

## Spellbook

### Postava
1. **New character** → jméno, rasa a božstvo (nepovinné), classa a úroveň. Výběr classy má hledání, classy jsou seskupené podle počtu úrovní kouzel
   (9 / 6 / 4 / bez kouzel / NPC) a pod seznamem je náhled spell levelů pro zvolenou úroveň. Sesílající classy mají na kartě
   štítky druhu magie (*tajemná / božská / psychická / extrakty*) a způsobu sesílání (*připravuje / spontánní*, arcanist obojí).
   Má-li vybraná classa podtřídu (klerik, druid, orákulum, čaroděj, kouzelník), objeví se pod úrovní rovnou její výběr –
   domény, mystérium, bloodline nebo škola. Je nepovinný, jde nastavit i později.
2. **Multiclass:** v detailu postavy v panelu **Classes** → **Add class** (i nesesílací – fighter, rogue, swashbuckler…);
   podtřídu přidávané classy jde vybrat rovnou ve stejném dialogu.
   Úroveň každé classy se mění stepperem, celkem max. 20; classu jde odebrat (×) i s jejími kouzly.
   **Prestižní classy** (118 z Archives of Nethys – Eldritch Knight, Mystic Theurge, Arcane Trickster…) jsou při přidání
   classy ve vlastní skupině (u nové postavy ne); karta ukazuje počet úrovní (obvykle 10) a druh sesílání, který zvyšuje,
   po výběru požadavky a odkaz **Požadavky a schopnosti**. Požadavky se ověřují jako prerekvizity featů (✓ / ✗ / ?, ruční
   potvrzení „Mám“); přísný režim prestižní classu s nesplněným požadavkem nepřidá. BAB, záchrany, kostky životů,
   skill body, class skills a bonusové featy se berou z tabulky classy. U prestižní classy, která zvyšuje sesílání
   („+1 level of existing arcane spellcasting class“), se vybírá **classa, kterou zvyšuje** (jediná vhodná se vybere sama;
   Mystic Theurge zvlášť tajemnou a božskou) – ta pak má vyšší **sesílací úroveň** (kouzla za den, známá, nejvyšší spell
   level, „sesílací úroveň 7“ pod classou). Schopnosti prestižní classy jsou jen text v detailu.
3. **Rasa** se vybírá při vytvoření postavy nebo později nahoře v panelu s classami (80 ras po skupinách: základní,
   významné, neobvyklé, ostatní, s velikostí, rychlostí a úpravami atributů). Rasa:
   - **přičte úpravy atributů** – u atributů zadáváte základní hodnoty, dlaždice ukazují výsledek včetně rasy
     (pod nimi je rozpis); člověk, půlelf a půlork si vyberou, ke kterému atributu dostanou +2,
   - dá člověku a půlelfovi **bonusový feat** (u půlelfa Skill Focus),
   - ověří rasové prerekvizity featů a její velikost se propíše do nosnosti propojeného inventáře.
   **Alternativní rasové traity** (pod rasou, ~630 z Archives of Nethys): dialog ukáže výchozí traity rasy, vybrané
   alternativní (× odebere) a seznam k přidání – hledání, „Nahrazuje: …“, PFS, klepnutím text a zdroj. Nahrazený výchozí
   trait je přeškrtnutý a přestane platit pro prerekvizity featů (Halfling Jinx → bez „halfling luck“), člověk bez
   Bonus Feat / Skilled přijde o bonusový feat / skill body navíc. Traity, které mění atributy (Dual Talent, Keen Kitsune,
   Orc Atavism…), se přičtou k atributům; kde se atribut volí, je u traitu výběr a dokud chybí, panel rasy na to upozorní. Dva traity nahrazující stejný trait appka ohlásí
   (přísný režim nedovolí). Změna rasy výběr zruší; export ho přenáší.
   **Archetypy** (pod každou classou, ~1 300 z Archives of Nethys): dialog ukáže vybrané archetypy (× odebere,
   klepnutím text) a seznam archetypů classy – hledání v názvu, nahrazených schopnostech i textu, „Nahrazuje: …“, PFS,
   krátké shrnutí, klepnutím plný text a zdroj. Pod classou je souhrn toho, co archetypy mění („Ruší: Smite Evil ·
   bez sesílání · −1 bonusový feat · Získává: Hunter's Tricks“):
   - **nahrazené schopnosti** zmizí – schopnosti s počtem použití (Smite Evil, Rage, Channel Energy, Bombs, Ki Pool,
     Wild Shape…), podtřída („Domains“, „Arcane School“, „Bloodline“…, i jen její kouzla nebo schopnosti), triky,
     a **počty se sníží** podle úrovně („2nd, 6th-level hex“ → o hex méně od 2. a od 6. úrovně; bonusové featy, revelations),
   - **kouzla navíc na spell listu** (~30 archetypů, ~260 kouzel z textu – Cultivator, Death Druid, Forest Preserver…)
     – nabídnou se ve výběru kouzel classy na své úrovni,
   - **známé archetypy** (~75, ručně podle textu) navíc: bez sesílání (Skirmisher, Warrior of the Holy Light…),
     o kouzlo méně za den / známé na každém levelu (Kensai, Crossblooded…), jen jedna doména (Cloistered Cleric…),
     doména paladina (Sacred Servant – schopnosti domény na úrovni paladin −3, Temple Champion), nové schopnosti s počtem
     použití (Hunter's Tricks, Arcane Reservoir, Eldritch Pool…) a bonusové featy (Insinuator),
   - **jiné sesílání**: jiný spell list (Unlettered Arcanist → witch, Hunting Paladin → ranger, Fey Trickster → druid
     a ranger…), jiný způsob a sloty (Eldritch Scion a Mindblade spontánně se sloty barda, Living Grimoire připravuje
     se sloty warpriesta, Ley Line Guardian jako sorcerer, Dandy z bardova listu se sloty media, Pearl Seeker jako
     bloodrager), jiný atribut (Fractured Mind – Cha, Scarred Witch Doctor – Int +2) nebo druh magie (Filidh – divine);
     pod classou „Sesílání: spontánní · CHA · sloty jako Bard“,
   - **Crossblooded** vybírá v dialogu podtřídy **druhou bloodline** – nabídnou se kouzla, schopnosti i bonusové featy obou.

   Archetypy, které dávají schopnosti bez jasného vzorce nebo kouzla „podle vlastní volby“ (Raven Master, Onmyoji…),
   jsou jen text. Dva archetypy, které nahrazují
   stejnou schopnost classy, appka ohlásí (přísný režim nedovolí). Unchained classy nabízejí archetypy základní classy,
   NPC classy archetypy nemají. Export postavy archetypy přenáší.
4. Pod classami je souhrn – úroveň postavy, **BAB** a záchrany (součet za všechny classy) – a **atributy** STR–CHA
   (klepnutím upravíte hodnotu, vedle je modifikátor).
5. Každá sesílací classa má vlastní sekci kouzel s tlačítkem **Add spells**.

Původní „decky“ se při prvním spuštění nové verze automaticky převedly na postavy (se všemi kouzly).

### Inventář postavy
- Sekce **Inventář** v detailu postavy: **Propojit inventář** (existující) nebo **Nový inventář** (pojmenovaný po postavě).
- Propojený inventář bere **sílu pro nosnost z postavy** – změna SÍL u postavy i stepperu Síla v inventáři se projeví na obou
  místech. U postavy je vidět souhrn (váha, zatížení), **Otevřít** přejde do inventáře, × propojení zruší.
- Jeden inventář patří nejvýš jedné postavě (propojení s jinou postavou ho od té původní odpojí); společný „Party stash“
  může zůstat nepropojený.
- **Nasazené vybavení**: v detailu položky inventáře je sekce **Vybavení** – **Nasazeno**, slot (hlava, čelenka, krk,
  opasek, prsten…) a **bonusy** (atribut, záchrana, AC nebo skill s typem bonusu). Známé magické předměty mají bonus
  předvyplněný podle jména (Headband of Vast Intelligence, Belt of Giant Strength, Cloak of Resistance, Ring of Protection,
  Amulet of Natural Armor, Bracers of Armor…), ostatním ho přidáte tlačítkem **Přidat bonus**.
- **Detail inventáře**: na mobilu záložky **Vybavení** a **Předměty**. Vybavení = figurka se sloty ve dvou sloupcích
  (hlava, čelenka, oči, krk, ramena, hruď, tělo | zbroj, opasek, zápěstí, ruce, 2 prsteny, nohy), pod ní hlavní
  a druhá ruka a předměty bez slotu; tečky na siluetě ukazují nasazená místa. Klepnutím na slot se otevře výběr: který
  předmět platí, **Ostatní odložit do batohu**, **Nasadit jiný předmět** (na desktopu plný slot rovnou otevře detail).
  Když je ve slotu víc předmětů, než se vejde, platí jen první – ukáže to červený banner s **Vyřešit**.
  Seznam předmětů je rozdělený na **Nasazeno** (štítek slotu) a **Neseno**; na desktopu je v bočním panelu filtr „Nasazeno“.
- Bonusy **nasazených** předmětů propojeného inventáře platí pro postavu: atributy (a s nimi bonusová kouzla, skill body,
  prerekvizity, nosnost), součty skillů; záchrany a AC se ukážou jako souhrn „Vybavení: …“ u inventáře postavy.
  Bonusy stejného typu se nesčítají (platí nejvyšší), dodge, untyped a circumstance ano. Dva předměty v jednom slotu
  (tři prsteny) appka ohlásí v inventáři.

### Mazlíčci a společníci
- Sekce **Mazlíčci a společníci** v detailu postavy: **Přidat mazlíčka** – jméno, druh (zvířecí společník, jezdecké zvíře,
  familiár, eidolon, mazlíček), druh tvora a kostky životů (**Zvíře** – d8, BAB ¾, dobrá Odolnost a Reflexy; **Magická bestie** –
  d10, plný BAB), velikost a „čtyřnohý“.
- Mazlíček má **vlastní list** jako postava: atributy, featy (za každé liché HD), skills (2 + Int za HD), počítadla, vlastní
  inventář (velikost a čtyřnohost → nosnost – sedlové brašny koně) a může mít i classy (eidolon, NPC classy…).
- V seznamu postav je odsazeně pod svým vlastníkem; nahoře v jeho listu je odkaz na vlastníka a **Nastavení mazlíčka**
  (druh, velikost). Smazáním postavy se její mazlíčci nesmažou – zůstanou jako samostatné postavy.
- **Rarita** (běžný / neobvyklý / vzácný / jedinečný) je jen štítek pro vlastní kampaň – pravidla PF1 ji nemají.
- **Když zemře (návrat / náhrada)**: text v listu mazlíčka, předvyplněný oficiálním pravidlem podle druhu (zvířecí
  společník 24 h modlitby, oř paladina 30 dní, familiár týden + rituál, eidolon další den); jde přepsat vlastním,
  **Výchozí text** ho vrátí.
- **Vlastní mazlíček**: zvíře *Vlastní* a vypnutá progrese = atributy, velikost, druh tvora i HD ručně.
- Export postavy přibalí i její mazlíčky (po importu zůstanou propojení).
- **Zvíře** ze seznamu (157 zvířecích společníků – 17 z Core Rulebooku a 140 z dalších knih Paizo: dinosauři, sloni,
  žraloci, ptáci…; výběr s hledáním) doplní startovní atributy, velikost, čtyřnohost, rychlost, útoky a zvláštní
  schopnosti. **Vlastní** = vše ručně.
- **Progrese zvířecího společníka** (výchozí u zvířecího společníka a jezdeckého zvířete): z **efektivní úrovně druida**
  (navrhne se z class vlastníka – druid a hunter plně, ranger −3, u jezdeckého zvířete i paladin a cavalier) se
  dopočítají HD, BAB, záchrany, featy, skill body, **bonus k Síle a Obratnosti**, **přirozená zbroj**, **triky navíc**
  a zvláštní schopnosti (pouto, sdílení kouzel, vyhýbání, oddanost, multiattack…). Na 4. nebo 7. úrovni se zvíře
  **vylepší** (např. vlk na 7. úrovni: Large, Síla +8, Obratnost −2, Odolnost +4, silnější kousnutí).
- Úroveň jde měnit stepperem nahoře v listu mazlíčka; bonusy se ukazují u atributů („Včetně bonusů společníka“).
  **Zvýšení atributu** (+1 na 4., 9., 14. a 20. úrovni) přičtěte ručně k základní hodnotě, kterou chcete.
- **Familiár**: druh z 89 familiárů (kočka, ropucha, krkavec, sova…; výběr s hledáním) a **úroveň pána** (navrhne se
  z úrovní wizard, witch, sorcerer, arcanist a shaman). List familiára ukáže **bonus pánovi** (Cat: +3 Stealth, Toad: +3 HP,
  Rat: +2 Fortitude…), který se **počítá na listu pána** (skilly, záchrany, AC, HP – jako vybavení); podmíněné bonusy
  („in bright light“) jsou jen text. Podle úrovně pána přirozená zbroj (+1 za každé 2 úrovně), Int (6–15), odolnost vůči
  kouzlům od 11. úrovně a schopnosti (alertness, sdílení kouzel, řeč s pánem…); HD = úroveň pána. Výběr druhu vyplní
  **statistiky z bestiáře** (atributy, velikost, rychlost, útoky, přirozená zbroj, smysly – 86 z 89 familiárů); familiár
  založený ve v1.9.0 s atributy 10 je dostane při další úpravě nastavení nebo úrovně pána.
- **Eidolon**: **základní forma** (biped, quadruped, serpentine, aquatic, avian, tauric; u unchained summonera unchained
  formy včetně aberrant a **podtyp** – angel, demon, devil…) a **úroveň summonera**. Z tabulky klasického / unchained
  summonera HD, BAB, záchrany podle formy, bonus Str / Obr, přirozená zbroj, max. útoků a schopnosti. **Evoluce** z 79
  (body 1–4, hledání, text) s počítadlem **bodů evolucí** (červeně při překročení); Ability Increase (+2 k vybranému
  atributu), Improved Natural Armor (+2) a Large (velikost) se počítají, ostatní jsou text.

### Podtřídy (domény, bloodline, mystérium, škola)
- Podtřídu jde vybrat už při vytvoření postavy nebo přidání classy (viz výše).
- Později ji změníte odkazem pod classou (např. **Domény: vybrat…**) – otevře výběr:
  - **Klerik**: 2 domény, ke každé volitelně subdoména (nahrazuje část kouzel domény) – nabízí se jen subdomény zvolené
    domény (např. k Fire: Arson, Ash, Smoke). Má-li postava božstvo ze seznamu, nabízí se jen jeho domény
    (Iomedae: Glory, Good, Law, Sun, War) a jen subdomény, které bůh dává (Iomedae u Sun: Day, Light, Revelation);
    bez božstva nebo s vlastním všechny. Když vybraná doména / subdoména božstvu neodpovídá (např. po změně božstva),
    pod classou se objeví červené upozornění „Torag nedává: Fire – vyberte znovu“ (klepnutím otevře výběr).
  - **Druid**: jedna přírodní doména (Air, Animal, Earth, Fire, Plant, Water, Weather), volitelně její subdoména (kromě Metal).
  - **Orákulum**: mystérium, **čaroděj**: bloodline.
  - **Čarodějnice**: patron, **šaman**: duch (spirit), **bloodrager**: bloodline, **psychic**: disciplína.
  - **Warpriest**: dvě požehnání (blessings), **inquisitor**: jedna doména (volitelně subdoména) – obojí jen podle domén
    božstva ze seznamu (jinak všechna); nesoulad s božstvem ukáže stejné červené upozornění jako u klerika.
  - **Kouzelník**: škola specializace (nebo univerzalista / elementární škola) a opoziční školy.
- Dialog podtřídy ukazuje i **Schopnosti podtřídy** (domény, bloodline, školy, požehnání) s úrovní, od které je classa má.
- Klerik / druid: pod kouzly je sekce **Doménová kouzla** – na každé úrovni kouzel jeden doménový slot (počet „x / 1“).
- Orákulum / čaroděj / bloodrager / psychic: sekce **Kouzla mystéria / bloodline / disciplíny** – známá automaticky od příslušné
  úrovně (bloodrager 7., 10., 13., 16.; psychic 1. a pak sudé), v herním režimu se sesílají ze slotů.
- Čarodějnice: **Kouzla patrona** od sudých úrovní – přibývají familiárovi a připravují se do běžných slotů: tlačítky +/− v sekci
  (nebo Přidat v detailu kouzla), pak jsou mezi kouzly čarodějnice na svém levelu, i když nejsou na jejím spell listu.
- Inquisitor může místo domény zvolit **inquisition** (AoN, jen inquisitions svého božstva). Schopnosti spiritů, bloodline
  bloodragera, disciplín a inquisitions (AoN) jsou v dialogu podtřídy; ty s počtem použití i v sekci Schopnosti.
  Psychic má podle disciplíny počítadlo **Phrenic Pool** (½ úrovně + Moudrost / Charisma).
- Šaman může zvolit i **toulavého ducha** (wandering spirit): od 4. úrovně jeho schopnost (větší od 12., pravá od 20.) a jeho kouzla
  v magii ducha, od 6. úrovně jeden jeho hex navíc (od 14. dva). Duch se mění denně – stačí ho přepnout v dialogu podtřídy.
- Čaroděj a bloodrager vidí u featů i v dialogu podtřídy **bonusové featy své bloodline** (AoN). Bloodline čaroděje, které
  Foundry nezná (Aquatic, Accursed…), mají schopnosti z AoN.
- Šaman: **Magie ducha** – na každé úrovni kouzel, kterou umí, jedno spontánní seslání za den (v herním režimu „x / 1 sesláno“).
- Warpriest / inquisitor nemají bonusová kouzla, jen schopnosti (počet použití požehnání je společný – Blessings).
- Kouzelník specialista má u každé úrovně kouzel „+1 škola“; kouzla opoziční školy mají rezavé ⚠ (příprava stojí 2 sloty).
- Podtřída se ukládá u postavy i v exportu.

### Schopnosti s počtem použití
- Sekce **Schopnosti s počtem použití** se sama naplní podle class postavy: Smite Evil, Lay on Hands, Channel Energy,
  Rage, Bardic Performance, Ki Pool, Arcane Pool, Grit, Panache, Judgment, Challenge, Wild Shape, Fervor, Blessings,
  Bombs, Inspiration, Martial Flexibility, schopnost školy kouzelníka… Maximum se počítá z úrovně classy a atributu.
- Podle podtřídy přibudou její schopnosti s počtem použití, **i vyšších úrovní** – domény a subdomény klerika a druida
  (např. Fire Bolt 3 + Moudrost za den, od 8. úrovně Holy Lance), bloodline čaroděje (Draconic Claws, od 9. úrovně
  Breath Weapon…) a škola kouzelníka (Force Missile, od 8. úrovně Elemental Wall). Subdoména nahradí schopnost, kterou
  mění. Název je doplněný podtřídou: „Fire Bolt (Fire)“. Data jsou z Foundry VTT; podtřída, kterou nezná (některé
  bloodline z pozdějších knih, Jungle, Inevitable), má obecnou schopnost 1. úrovně (3 + Moudrost / Charisma za den).
- Vybrané **hexy a zjevení** s počtem použití se sem přidají samy (viz níže).
- Nepotřebnou schopnost skryjete ×, „Zobrazit skryté“ je vrátí.
- **Přidat počítadlo** = vlastní položka (název, maximum, jednotka: za den / kol za den / body, poznámka) – rasové
  schopnosti, magické předměty, náboje hůlky…
- V **herním režimu** se použití odklikává (puntíky, u velkých čísel − / +), **Odpočinek** vše obnoví.

### Hexy a zjevení (revelations)
- Čarodějnice a šaman mají v detailu postavy sekci **Hexy**, orákulum sekci **Zjevení**. Vpravo je počet „vybrané / dostupné“
  podle úrovně classy (čarodějnice 1 + 1 za každou sudou úroveň, šaman na 2., 4., 8., 10., 12., 16., 18. a 20. úrovni,
  orákulum na 1., 3., 7., 11., 15. a 19.) plus jeden za každý feat Extra Hex / Extra Revelation u postavy.
- Šaman vybírá i **hexy svého ducha** (AoN, ~80 hexů): v seznamu jsou nahoře, přepínač **Hexy všech duchů** ukáže i ostatní.
- **Přidat hex / Přidat zjevení** otevře seznam z Foundry VTT (~120 hexů, ~300 zjevení): hledání v názvu i textu,
  u každého druh (hex, velký hex, nejvyšší hex), Ex / Su / Sp, počet použití a minimální úroveň. Klepnutím se rozbalí
  celý text a zdroj. Orákulum vidí zjevení svého mystéria, **Všechna mystéria** ukáže i ostatní.
- Velké hexy jsou od 10. úrovně, nejvyšší od 18., zjevení podle textu („must be at least 7th level“). Mimo přísný režim
  jdou vybrat i dřív (červeně „od N. úrovně“); přísný režim nedovolí vybrat víc, než je dostupných, ani nad úroveň.
- Klepnutím na vybraný hex se ukáže jeho text, × ho odebere. Hexy a zjevení se exportují s postavou.

### Obrana a životy
- Sekce **Obrana a životy**: **AC**, **dotyk** a **zaskočený** – 10 + zbroj + štít + Obratnost (omezená max. Dex zbroje,
  štítu nebo zatížení) + velikost + přirozená zbroj (zvíře, Amulet of Natural Armor) + deflection + dodge + ostatní bonusy
  z nasazeného vybavení + ruční **AC – ostatní** (monk, featy, kouzla). Pod dlaždicemi je rozpis.
- Zbroj a štít z databáze mají po přidání do inventáře bonus k AC, max. Dex a postih zbroje vyplněné – stačí je nasadit.
  **Postih zbroje** (zbroj + štít, nebo horší postih zatížení) se odečte od skills Síly a Obratnosti.
- **Životy**: 1. HD naplno, další průměr (polovina + 1), rasové HD tvorů průměr; + Odolnost za HD, Toughness a **HP navíc**
  (oblíbená classa, traity). Házené HP zadáte v **Upravit** (prázdné = automaticky).
- Stepper životů: − zranění, + léčení; **Odpočinek** vyléčí 1 HP za úroveň postavy.

### Featy
- Sekce **Featy** v detailu postavy ukazuje „vybrané / dostupné“: featy za úrovně (1. a každá lichá) + bonusové featy class
  (bojovník, kouzelník, mnich, magus…) + ruční **Navíc** (např. human, traity).
- **Přidat feat** otevře přehled ~3 400 featů z Archives of Nethys: hledání, typ, „Jen featy, na které mám“, PFS, hledání v textu.
  U každého featu je stav prerekvizit: ✓ splněno, ✗ nesplněno, ? ověř ručně (skill ranky, rasové traity…). Rasa se ověří,
  když ji má postava vybranou (half-elf platí i jako elf a člověk). Appka nic nezakazuje – kromě **přísného režimu**
  (menu ⋮ nebo zaškrtávátko ve výběru featů / kouzel): pak nejde přidat feat nad počet ani feat s ✗ prerekvizitou
  (featy s „?“ jdou) a kouzel jen tolik, kolik classa na spell levelu smí (připravené kopie / známá kouzla, slot školy
  specialisty; atribut sesílání musí stačit). Ubírat jde vždy, import přísný režim nekontroluje.
- Ověřuje se i: class features podle class a úrovní (channel energy, rage, ki pool…), **přesvědčení** a **božstvo**
  (nastavíte nahoře v panelu postavy pod rasou; božstvo ze seznamu ~460 božstev s přesvědčením, panteonem a oblíbenou
  zbraní – hledání i podle oblasti, např. „healing“; **Vlastní…** = homebrew božstvo jako text), velikost a typ tvora podle rasy, Hit Dice, základní záchrany,
  proficience se zbrojí a zbraněmi, sesílání kouzel (arcane / divine, konkrétní kouzlo), domény a výchozí rasové traity.
- Co appka ověřit nemůže (archetypy, zvláštní podmínky), potvrdíte v detailu featu
  tlačítkem **Mám** u dané prerekvizity – potvrzení platí pro všechny featy se stejnou prerekvizitou, **Zrušit** ho vrátí.
- Klepnutím se otevře detail s plným textem a prerekvizitami po jedné. U featu postavy lze doplnit **poznámku**
  (např. zvolenou zbraň u Weapon Focus).
- **Vlastní feat** = název + popis (homebrew, rasové schopnosti…), u postavy jde upravit tužkou v detailu.
- Featy z databáze se neaktualizují tlačítkem (jsou přibalené k appce).
- **Metamagie**: detail metamagického featu ukazuje, o kolik zvyšuje slot kouzla; v detailu kouzla postavy je sekce
  **Metamagie** s výslednou úrovní slotu pro každý její metamagický feat (červeně, když tu úroveň ještě nesesílá).

### Dovednosti (skills)
- Sekce **Dovednosti** v detailu postavy ukazuje skilly s ranky (celkový bonus a počet ranků) a vpravo nahoře
  „utracené / dostupné“ skill body.
- **Upravit dovednosti** ukáže všechny skilly se steppery ranků. U každého je atribut a „třídní“ (class skill některé
  z class nebo rasy – s aspoň 1 rankem +3). Řemeslo, Vystupování a Profese mají druhy: **Přidat: Řemeslo** → „alchemy“.
- Celkový bonus = ranky + modifikátor atributu (včetně rasy) + 3 za třídní + rasový bonus (např. elf +2 Vnímání).
  „—“ = dovednost jen pro trénované bez ranků.
- Body: za každou úroveň classy její body + mod. INT (min. 1), člověk +1 za úroveň; bonus za oblíbenou classu a jiné
  přidejte do **Navíc**. Max. ranků v jedné dovednosti = úroveň postavy.
- Prerekvizity featů typu „Acrobatics 5 ranks“ appka ověří, jakmile má postava zadaný aspoň jeden rank
  (do té doby „?“, aby postava bez vyplněných skillů neměla všude ✗).

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
5. Klepnutím na spell se otevře detail: casting time, components, range, duration, save, SR, celý popis,
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

- **Postava** se exportuje celá: rasa, přesvědčení, božstvo, classy a úrovně, atributy, dovednosti, kouzla, featy (i vlastní
  a poznámky), „Navíc“, počítadla, nastavení obrany (HP navíc, házené HP, AC – ostatní) a mazlíčci.
  Má-li propojený inventář, jde ho zaškrtnutím **Včetně propojeného inventáře** přibalit (i s nasazeným vybavením) –
  po importu se založí a propojí. Seslaná kouzla z herního režimu a utržené zranění se neexportují.
- **Featy** se importují do otevřené postavy (přidají se k existujícím); featy, které postava už má a nejdou vzít víckrát,
  se přeskočí. Import featů přijme i export celé postavy (vezme z něj featy).
- Staré exporty decků (z verze 1.0) jdou dál importovat – z decku vznikne postava s jednou classou.

- **Export:** *Copy* (do schránky) a *Save to file* (desktop) nebo *Share* (Android, iOS).
- **Import:** *Paste from clipboard* nebo *Open file* (desktop, Android), případně text vložit ručně.
  Před potvrzením uvidíte náhled; spelly, které v databázi nejsou, se vypíšou a vynechají.
  Importované postavy/inventáře se přidají jako nové (při shodě jména dostanou „(imported)“) – nic se nepřepíše.

## Aktualizace databází

Menu **⋮ → Databáze a aktualizace** (EN: Databases & updates):

- **Download latest spell database** – stáhne aktuální seznam spellů.
- **Download latest item database** – stáhne aktuální tabulky zbraní, munice a magických itemů.
- **Restore bundled version** – vrátí verzi přibalenou v appce.

Pathfinder 1e je uzavřený systém, takže aktualizace přinese hlavně opravy. Postavy a inventáře zůstávají –
spelly se párují podle id a jména, položky inventáře si nesou vlastní název, cenu a váhu.

## Aktualizace appky

Na stejné obrazovce je nahoře sekce **Aktualizace appky** (jen Android a desktop): nainstalovaná verze a tlačítko
**Zkontrolovat aktualizace**, které se podívá na poslední vydání v GitHub Releases.

- **Android**: **Stáhnout a nainstalovat** stáhne APK přímo v appce (s průběhem) a otevře systémový instalátor. Poprvé je
  potřeba povolit instalaci z této appky. Aktualizace přes starou verzi funguje jen u APK podepsaných stejným klíčem.
- **Desktop**: **Stáhnout a nainstalovat** stáhne `.msi` / `.dmg` / `.deb` do složky Stažené soubory (s průběhem), dotaz nabídne
  export postav a **Nainstalovat a zavřít appku** spustí instalátor a appku ukončí, aby ji mohl nahradit. Když stažení
  nebo spuštění selže, otevře se stažení v prohlížeči.
- **Co je nového** otevře stránku vydání s popisem změn.
- Před instalací se appka **zeptá** a nabídne **export postav a inventářů** (záloha). Na Androidu po stažení porovná podpis
  APK s nainstalovanou verzí: když se liší, Android aktualizaci přes starou verzi nepovolí – appka pak místo instalace ukáže
  postup (export → stáhnout APK v prohlížeči → odinstalovat → nainstalovat → import), protože odinstalace smaže data.

Appka (build z GitHub Releases) se sama podívá nejvýš jednou denně při startu. Když najde novou verzi, ukáže dole
banner: stáhnout, **Později**, nebo **Tuto verzi přeskočit** (pak ji už automaticky nehlásí, ruční kontrola ano).
Webová verze je vždy aktuální ze serveru, na iOS se appka staví v Xcode – tam se aktualizace nenabízí.
Postavy a inventáře aktualizace nemění.

## Kde jsou moje data

Vše je jen v zařízení:

| Platforma | Postavy, inventáře | Stažené databáze |
|---|---|---|
| Android | SharedPreferences appky | soukromá složka appky |
| iOS | NSUserDefaults | složka Documents appky |
| Desktop | Java Preferences (Windows: registr `HKCU\Software\JavaSoft\Prefs`) | `~/.pfspells/` |
| Web | `localStorage` prohlížeče (pro danou adresu) | IndexedDB prohlížeče |

Odinstalací appky (Android/iOS) nebo vymazáním dat webu v prohlížeči se data smažou – pro zálohu použijte **⋮ → Exportovat všechny postavy / inventáře**.
Na webu jsou data vázaná na prohlížeč a adresu stránky.

## Webová verze

Appka běží i v prohlížeči (Chrome/Edge 119+, Firefox 120+, Safari 18.2+). První načtení stáhne ~26 MB, další už jdou z cache.
Export ukládá soubor do Stažených, import otevře výběr souboru. Esc i tlačítko Zpět prohlížeče vrátí předchozí obrazovku.
