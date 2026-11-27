package cz.pfspells.data

/*
 * Ručně zadané efekty archetypů, které nejdou vyčíst ze sloupce „Replaces“ (podle textů AoN v `Archetypes.json`).
 * Klíč = id archetypu (`classa:slug-jména`). Test `BundledDataTest.archetypeModsExist` hlídá, že id existují.
 * Doplňovat podle textu archetypu – vzorce jen tam, kde je text uvádí přesně.
 */

private fun Character.mod(a: Ability) = abilityMod(scores[a])

/** „Diminished Spellcasting: … one fewer spell of each level than normal“ */
private val ONE_FEWER = ArchetypeMod(perDayDelta = -1)

/** „… knows one fewer spell of each level“ */
private val ONE_FEWER_KNOWN = ArchetypeMod(knownDelta = -1)

/** „chooses only one domain“ */
private val ONE_DOMAIN = ArchetypeMod(oneDomain = true)

/** „does not gain any spells or spellcasting abilities, does not have a caster level“ */
private val NO_SPELLS = ArchetypeMod(noSpells = true)

internal val ARCHETYPE_MODS: Map<String, ArchetypeMod> = mapOf(
    // ---- bez sesílání ----
    "paladin:warrior-of-the-holy-light" to NO_SPELLS,
    "paladin:stonelord" to NO_SPELLS,
    "antipaladin:dread-vanguard" to NO_SPELLS,
    "paladin:divine-guardian" to NO_SPELLS,
    "paladin:virtuous-bravo" to NO_SPELLS,
    "paladin:tempered-champion" to NO_SPELLS,
    // bez kouzel, ale doména božstva – jen schopnosti (podle úrovně paladina), bez kouzel domény
    "paladin:temple-champion" to ArchetypeMod(noSpells = true, subclass = SubclassType.INQUISITOR_DOMAIN),
    // „instead of gaining spells, a bloodrager becomes resistant to spells“
    "bloodrager:untouchable-rager" to NO_SPELLS,
    // bez kouzel: bonusové featy na 4. a každé 3. další; Smite Impudence 1× + 1× na 4. a každé 3. další (místo Smite Good)
    "antipaladin:insinuator" to ArchetypeMod(
        noSpells = true,
        bonusFeatLevels = (4..19 step 3).toList(),
        presets = listOf(ArchetypePreset("smite", "Smite Impudence", ResourceUnit.PER_DAY) { l, _ -> 1 + (l - 1) / 3 }),
    ),
    "ranger:ilsurian-archer" to NO_SPELLS,
    "ranger:sword-devil" to NO_SPELLS,
    // Hunter's Tricks: od 5. úrovně, ½ úrovně + Wis za den
    "ranger:skirmisher" to ArchetypeMod(
        noSpells = true,
        presets = listOf(
            ArchetypePreset("tricks", "Hunter's Tricks", ResourceUnit.PER_DAY, minLevel = 5) { l, c -> (l / 2 + c.mod(Ability.WIS)).coerceAtLeast(0) },
        ),
    ),
    // Trap: od 5. úrovně, ½ úrovně + Wis za den
    "ranger:trapper" to ArchetypeMod(
        noSpells = true,
        presets = listOf(
            ArchetypePreset("traps", "Ranger Traps", ResourceUnit.PER_DAY, minLevel = 5) { l, c -> (l / 2 + c.mod(Ability.WIS)).coerceAtLeast(0) },
        ),
    ),
    // Brachiation: od 5. úrovně, kola za den = úroveň rangera
    "ranger:jungle-lord" to ArchetypeMod(
        noSpells = true,
        presets = listOf(ArchetypePreset("brachiation", "Brachiation", ResourceUnit.ROUNDS, minLevel = 5) { l, _ -> l }),
    ),

    // ---- o kouzlo méně za den na každém levelu ----
    "bard:arrowsong-minstrel" to ONE_FEWER,
    "cleric:angelfire-apostle" to ONE_FEWER,
    "cleric:channeler-of-the-unknown" to ONE_FEWER,
    "druid:planar-extremist" to ONE_FEWER,
    "druid:survivor" to ONE_FEWER,
    "magus:esoteric" to ONE_FEWER,
    "magus:iron-ring-striker" to ONE_FEWER,
    "magus:jistkan-artificer" to ONE_FEWER,
    "magus:kapenia-dancer" to ONE_FEWER,
    "magus:kensai" to ONE_FEWER,
    "magus:myrmidarch" to ONE_FEWER,
    "magus:skirnir" to ONE_FEWER,
    "magus:soul-forger" to ONE_FEWER,
    "mesmerist:umbral-mesmerist" to ONE_FEWER,
    "oracle:purifier" to ONE_FEWER,
    "paladin:silver-champion" to ONE_FEWER,
    "sorcerer:umbral-scion" to ONE_FEWER,
    "summoner:pyroclast" to ONE_FEWER,
    "witch:witch-watcher" to ONE_FEWER,

    // ---- o známé kouzlo méně ----
    "bloodrager:bloody-knuckled-rowdy" to ONE_FEWER_KNOWN,
    "medium:rivethun-spirit-channeler" to ONE_FEWER_KNOWN,
    // „one fewer spell known at each level (including cantrips)“ + druhá bloodline
    "sorcerer:crossblooded" to ArchetypeMod(knownDelta = -1, secondBloodline = true),
    // „one fewer spell known per spell level for spell levels 1–6“
    "summoner:morphic-savant" to ArchetypeMod(knownDelta = -1, deltaIncludesCantrips = false),
    "summoner:unwavering-conduit" to ArchetypeMod(knownDelta = -1, deltaIncludesCantrips = false),

    // ---- klerik s jednou doménou ----
    "cleric:cardinal" to ONE_DOMAIN,
    "cleric:divine-strategist" to ONE_DOMAIN,
    "cleric:elder-mythos-cultist" to ONE_DOMAIN,
    "cleric:evangelist" to ONE_DOMAIN,
    "cleric:forgemaster" to ONE_DOMAIN,
    "cleric:herald-caller" to ONE_DOMAIN,
    "cleric:mendevian-priest" to ONE_DOMAIN,
    "cleric:roaming-exorcist" to ONE_DOMAIN,
    "cleric:sacred-attendant" to ONE_DOMAIN,
    "cleric:stoic-caregiver" to ONE_DOMAIN,
    "cleric:theologian" to ONE_DOMAIN,
    "cleric:triadic-priest" to ONE_DOMAIN,
    // jedna doména a o kouzlo (bez domén) méně za den
    "cleric:cloistered-cleric" to ArchetypeMod(oneDomain = true, perDayDelta = -1),
    "cleric:crusader" to ArchetypeMod(oneDomain = true, perDayDelta = -1),

    // ---- jiný spell list / způsob sesílání / atribut / druh magie ----
    // „uses the witch spell list instead of the wizard/sorcerer spell list“
    "arcanist:unlettered-arcanist" to ArchetypeMod(casting = CastingChange(lists = listOf("witch"))),
    // „casts psychic spells from the mesmerist list … otherwise … as a bard“
    "bard:speaker-of-the-palatine-eye" to ArchetypeMod(casting = CastingChange(lists = listOf("mesmerist"), tradition = MagicTradition.PSYCHIC)),
    // „casts divine spells drawn from the druid and ranger spell lists, in just the same way as a hunter“
    "mesmerist:fey-trickster" to ArchetypeMod(casting = CastingChange(lists = listOf("druid", "ranger"), tradition = MagicTradition.DIVINE)),
    // „prepare and cast one spell per day from the ranger / druid spell list rather than the paladin spell list“
    "paladin:hunting-paladin" to ArchetypeMod(casting = CastingChange(lists = listOf("ranger"))),
    "paladin:wilderness-warden" to ArchetypeMod(casting = CastingChange(lists = listOf("druid"))),
    // „Charisma … spontaneously from the bard spell list … spells known and spells per day as a medium of his ranger level“
    "ranger:dandy" to ArchetypeMod(
        casting = CastingChange(
            lists = listOf("bard"), ability = Ability.CHA, kind = CastingKind.SPONTANEOUS,
            table = "medium", progression = Progression.FOUR_KNACKS, tradition = MagicTradition.ARCANE,
        ),
    ),
    // „must prepare his spells … Intelligence … same number of spell slots per day as a warpriest“
    "inquisitor:living-grimoire" to ArchetypeMod(
        casting = CastingChange(ability = Ability.INT, kind = CastingKind.PREPARED, table = "warpriest"),
    ),
    // „casts … without preparing … Charisma … spells per day and spells known as a bard“;
    // Eldritch Pool = ½ úrovně (min. 1) + Cha místo Arcane Pool (bloodline bloodragera jen v textu)
    "magus:eldritch-scion" to ArchetypeMod(
        casting = CastingChange(ability = Ability.CHA, kind = CastingKind.SPONTANEOUS, table = "bard"),
        presets = listOf(
            ArchetypePreset("pool", "Eldritch Pool", ResourceUnit.POINTS) { l, c -> (maxOf(1, l / 2) + c.mod(Ability.CHA)).coerceAtLeast(0) },
        ),
    ),
    // „casts spells from the magus spell list as psychic spells … same number of spells known and slots as a bard“
    "magus:mindblade" to ArchetypeMod(
        casting = CastingChange(kind = CastingKind.SPONTANEOUS, table = "bard", tradition = MagicTradition.PSYCHIC),
    ),
    // „a spontaneous spellcaster … same number of spells known and spell slots per day as a sorcerer“
    "witch:ley-line-guardian" to ArchetypeMod(
        casting = CastingChange(kind = CastingKind.SPONTANEOUS, table = "sorcerer", progression = Progression.SPONTANEOUS_9),
    ),
    // „casts divine spells spontaneously, using the spells known and spells per day from the bloodrager table“ + kouzla navíc
    "paladin:pearl-seeker" to ArchetypeMod(
        casting = CastingChange(
            kind = CastingKind.SPONTANEOUS, table = "bloodrager",
            spells = mapOf(
                "Slipstream" to 1, "Hydraulic Push" to 1, "Ride the Waves" to 2,
                "Fluid Form" to 3, "Hydraulic Torrent" to 3, "Seamantle" to 4,
            ),
        ),
    ),
    // „uses her Charisma score rather than her Wisdom score“
    "spiritualist:fractured-mind" to ArchetypeMod(casting = CastingChange(ability = Ability.CHA)),
    // arcane místo psychic; bestow curse (3.) a major curse (6.) jako kouzla navíc
    "spiritualist:hag-haunted" to ArchetypeMod(
        casting = CastingChange(tradition = MagicTradition.ARCANE, spells = mapOf("Bestow Curse" to 3, "Curse, Major" to 6)),
    ),
    "spiritualist:onmyoji" to ArchetypeMod(casting = CastingChange(tradition = MagicTradition.DIVINE)),
    "bard:filidh" to ArchetypeMod(casting = CastingChange(tradition = MagicTradition.DIVINE)),
    "occultist:silksworn" to ArchetypeMod(casting = CastingChange(tradition = MagicTradition.ARCANE)),
    // „treats her Intelligence score as 2 points higher when determining … spells per day“
    "witch:scarred-witch-doctor" to ArchetypeMod(casting = CastingChange(abilityBonus = 2)),
    // doména božstva (occultist level = cleric level) – schopnosti; kouzla domény jen v textu
    "occultist:reliquarian" to ArchetypeMod(subclass = SubclassType.INQUISITOR_DOMAIN),

    // ---- nové / změněné schopnosti s počtem použití ----
    // Smite Evil 1× + 1× na 7. a každé 6. další; doména božstva + doménový slot; Divine Bond se svatým symbolem
    // 1× za den na 5., +1 každé 4 další; schopnosti domény na efektivní úrovni klerika = paladin −3
    "paladin:sacred-servant" to ArchetypeMod(
        subclass = SubclassType.ONE_DOMAIN,
        subclassLevelOffset = -3,
        removes = setOf("smite evil"),
        presets = listOf(
            ArchetypePreset("smite", "Smite Evil", ResourceUnit.PER_DAY) { l, _ -> 1 + (l - 1) / 6 },
            ArchetypePreset("bond", "Divine Bond (holy symbol)", ResourceUnit.PER_DAY, minLevel = 5) { l, _ -> 1 + (l - 5) / 4 },
        ),
    ),
    // Channel Positive Energy od 4. úrovně, 3 + Cha za den (nespotřebuje Lay on Hands)
    "paladin:hospitaler" to ArchetypeMod(
        presets = listOf(
            ArchetypePreset("channel", "Channel Positive Energy", ResourceUnit.PER_DAY, minLevel = 4) { _, c -> (3 + c.mod(Ability.CHA)).coerceAtLeast(0) },
        ),
    ),
    // arcane pool = 1/3 úrovně (min. 1) + Int
    "magus:bladebound" to ArchetypeMod(
        removes = setOf("arcane pool"),
        presets = listOf(
            ArchetypePreset("pool", "Arcane Pool (bladebound)", ResourceUnit.POINTS) { l, c -> (maxOf(1, l / 3) + c.mod(Ability.INT)).coerceAtLeast(0) },
        ),
    ),
    // Arcane Reservoir arcanisty (úroveň kouzelníka = úroveň arcanisty): max. 3 + úroveň bodů
    "wizard:exploiter-wizard" to ArchetypeMod(
        presets = listOf(ArchetypePreset("reservoir", "Arcane Reservoir", ResourceUnit.POINTS) { l, _ -> 3 + l }),
    ),
    // Archaeologist's Luck: 4 + Cha kol za den (místo bardic performance)
    "bard:archaeologist" to ArchetypeMod(
        removes = setOf("bardic performance"),
        presets = listOf(
            ArchetypePreset("luck", "Archaeologist's Luck", ResourceUnit.ROUNDS) { _, c -> (4 + c.mod(Ability.CHA)).coerceAtLeast(0) },
        ),
    ),
)
