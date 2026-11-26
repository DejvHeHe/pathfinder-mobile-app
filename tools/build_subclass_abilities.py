#!/usr/bin/env python3
"""
Subclass abilities from Archives of Nethys (OGL / Paizo Community Use Policy) – the ones Foundry VTT does not link
to their subclass:

1. composeApp/src/commonMain/kotlin/cz/pfspells/data/SubclassAbilityTable.kt
   - AON_SUBCLASS_POWER_TABLE: powers of shaman spirits (spirit ability 1st, greater 8th, true 16th, manifestation 20th),
     bloodrager bloodlines and psychic disciplines (level from "At Nth level"), inquisitions; max uses per day parsed
     from the text ("a number of times per day equal to 3 + your Wisdom modifier", "once per day"…),
   - DISCIPLINE_POOL_ABILITY: discipline → phrenic pool ability (Wisdom / Charisma),
   - INQUISITION_DEITIES: inquisition → deities (empty = any deity),
   - BLOODLINE_BONUS_FEATS: bloodrager / sorcerer bloodline → bonus feat list; sorcerer bloodline powers too
     (the app prefers Foundry's 11 bloodlines and uses AoN for the rest).
2. composeApp/src/commonMain/composeResources/files/SubclassAbilities.json – shaman spirit hexes with text
   (same format as ClassAbilities.json, `spirits` = the spirits that grant the hex).

Pages: ShamanSpirits.aspx → ShamanSpiritDisplay.aspx, BloodragerBloodlines.aspx → BloodragerBloodlineDisplay.aspx,
PsychicDisciplines.aspx → PsychicDisciplinesDisplay.aspx, SorcererBloodlines.aspx → BloodlineDisplay.aspx,
Inquisitions.aspx (~120 requests, 1 per second,
cached in build/subclass-cache).

Usage:  python tools/build_subclass_abilities.py [--cache DIR]
        (when Python cannot verify the certificate, download the pages with curl into the cache directory –
         file name = the page URL with "?", "=" and " " replaced by "_" plus ".html")
"""
import html
import json
import re
import sys
import time
import urllib.parse
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OUT_KT = ROOT / "composeApp/src/commonMain/kotlin/cz/pfspells/data/SubclassAbilityTable.kt"
OUT_JSON = ROOT / "composeApp/src/commonMain/composeResources/files/SubclassAbilities.json"
BASE = "https://aonprd.com/"
ABILITIES = {"strength": "str", "dexterity": "dex", "constitution": "con", "intelligence": "int", "wisdom": "wis", "charisma": "cha"}
NUMBERS = {"once": 1, "twice": 2, "three times": 3, "four times": 4, "five times": 5}


def fetch(url: str) -> str:
    req = urllib.request.Request(url, headers={"User-Agent": "pf-spellbook-build"})
    with urllib.request.urlopen(req, timeout=120) as r:
        return r.read().decode("utf-8", errors="replace")


def page(cache: Path, path: str) -> str:
    f = cache / (re.sub(r"[?= ]", "_", path) + ".html")
    if f.exists():
        return f.read_text(encoding="utf-8", errors="replace")
    cache.mkdir(parents=True, exist_ok=True)
    time.sleep(1)
    t = fetch(BASE + urllib.parse.quote(path, safe="/?=&-"))
    f.write_text(t, encoding="utf-8")
    return t


def slug(s: str) -> str:
    return re.sub(r"[^a-z0-9]+", "-", s.lower()).strip("-")


def kstr(s: str) -> str:
    return '"' + s.replace("\\", "\\\\").replace('"', '\\"').replace("$", "\\$") + '"'


def lines_of(h: str):
    """Obsah stránky (od prvního nadpisu po patičku) → řádky prostého textu; nadpis sekce tučně „**X:“."""
    i = h.find('<h1 class="title">')
    h = h[i:] if i >= 0 else h
    j = h.find("Site Owner")
    h = h[:j] if j >= 0 else h
    h = re.sub(r"<\s*(br|/p|/li|/h\d)\s*/?>", "\n", h, flags=re.I)
    h = re.sub(r"<\s*h1[^>]*>", "\n# ", h, flags=re.I)
    h = re.sub(r"<\s*b\s*>", "**", h, flags=re.I)
    h = re.sub(r"<[^>]+>", "", h)
    h = html.unescape(h).replace(" ", " ").replace("’", "'").replace("−", "-")
    out = [re.sub(r"\s+", " ", x).strip() for x in h.split("\n")]
    return [x for x in out if x]


ABILITY_LINE = re.compile(r"^([A-Z][^():]{1,70}?)\s*\((Ex|Su|Sp|Ex, Su|Su, Sp)\)\s*:\s*(.*)$")


def level_of(text: str, default: int = 1) -> int:
    m = re.match(r"(?:At|Upon reaching) (\d+)(?:st|nd|rd|th) level", text)
    return int(m.group(1)) if m else default


def uses_of(text: str):
    """(vzorec Foundry, jednotka) – jen denní počty, které jdou spolehlivě přečíst z textu."""
    m = re.search(r"number of (times|rounds|minutes) per day equal to (?:(\d+) \+ )?(?:your|her|his) (\w+) (modifier|bonus)", text, re.I)
    # „…equal to your Wisdom bonus, plus 1 round for every level above 4th“ – vzorec by byl nepřesný
    if m and re.match(r",? plus\b", text[m.end():]):
        return None, None
    if m and m.group(3).lower() in ABILITIES:
        ab = f"@abilities.{ABILITIES[m.group(3).lower()]}.mod"
        f = f"{m.group(2)} + {ab}" if m.group(2) else ab
        if m.group(1).lower() == "minutes":
            return None, None
        return f, "ROUNDS" if m.group(1).lower() == "rounds" else "PER_DAY"
    m = re.search(r"number of (times|rounds) per day equal to (?:your|her|his) (?:\w+ )?level", text, re.I)
    if m:
        return "@class.level", "ROUNDS" if m.group(1).lower() == "rounds" else "PER_DAY"
    m = re.search(r"\b(once|twice|three times|four times|five times) per day\b", text, re.I)
    if m and not re.search(r"(once|twice|three times) per day (per|for each|against)", text, re.I):
        return str(NUMBERS[m.group(1).lower()]), "PER_DAY"
    return None, None


def split_list(s: str):
    """„Cleave, Skill Focus (Knowledge [arcana], Spellcraft), Toughness“ → položky (čárky v závorkách se nedělí)."""
    out, depth, cur = [], 0, ""
    for ch in s:
        depth += ch in "([" and 1 or (-1 if ch in ")]" else 0)
        if ch == "," and depth == 0:
            out.append(cur)
            cur = ""
        else:
            cur += ch
    return out + [cur]


def item_names(h: str, display: str):
    return sorted(set(html.unescape(n) for n in re.findall(display + r"\.aspx\?ItemName=([^\"&]+)\"", h)))


def main():
    args = sys.argv[1:]
    cache = Path(args[args.index("--cache") + 1]) if "--cache" in args else ROOT / "build/subclass-cache"
    powers = []  # (key, level, name, uses, unit)
    hexes = {}  # slug → řádek JSON
    pool = {}
    bonus_feats = {}  # klíč bloodline → bonusové featy
    inq_deities = {}

    def add_power(key, level, name, text):
        f, unit = uses_of(text)
        powers.append((key, level, name, f, unit))

    # ---- spirity šamana ----
    for name in item_names(page(cache, "ShamanSpirits.aspx"), "ShamanSpiritDisplay"):
        key = "spirit:" + name.lower()
        section = None
        for ln in lines_of(page(cache, "ShamanSpiritDisplay.aspx?ItemName=" + name)):
            hm = re.match(r"^\*\*([^:*]+):\s*(.*)$", ln)
            if hm:
                section = hm.group(1).strip().lower()
                if section == "manifestation":
                    add_power(key, 20, "Manifestation", hm.group(2))
                continue
            am = ABILITY_LINE.match(ln)
            if not am:
                continue
            nm, typ, text = am.group(1).strip(), am.group(2).split(",")[0], am.group(3)
            if section == "hexes":
                sid = slug(nm)
                row = hexes.setdefault(sid, {"id": "spirit-" + sid, "name": nm, "kind": "HEX", "classes": ["shaman"], "spirits": []})
                row["spirits"].append(name.lower())
                f, unit = uses_of(text)
                if f:
                    row["uses"], row["unit"] = f, unit
                row["type"] = typ
                row["source"] = "Advanced Class Guide"
                row["text"] = text
            elif section in ("spirit ability", "greater spirit ability", "true spirit ability"):
                lvl = {"spirit ability": 1, "greater spirit ability": 8, "true spirit ability": 16}[section]
                add_power(key, lvl, nm, text)

    # ---- bloodline bloodragera a čaroděje: schopnosti a bonusové featy ----
    for prefix, lst, display in (("brbloodline", "BloodragerBloodlines", "BloodragerBloodlineDisplay"),
                                 ("bloodline", "SorcererBloodlines", "BloodlineDisplay")):
        for name in item_names(page(cache, lst + ".aspx"), display):
            key = f"{prefix}:{name.lower()}"
            section = None
            for ln in lines_of(page(cache, f"{display}.aspx?ItemName={name}")):
                hm = re.match(r"^\*\*([^:*]+):\s*(.*)$", ln)
                if hm:
                    section = hm.group(1).strip().lower()
                    if section == "bonus feats":
                        feats = [re.sub(r"\s+", " ", f).strip() for f in split_list(hm.group(2).rstrip(". "))]
                        bonus_feats[key] = [f for f in feats if f]
                    continue
                am = ABILITY_LINE.match(ln)
                if am and section == "bloodline powers":
                    add_power(key, level_of(am.group(3)), am.group(1).strip(), am.group(3))

    # ---- disciplíny psychica ----
    for name in item_names(page(cache, "PsychicDisciplines.aspx"), "PsychicDisciplinesDisplay"):
        key = "discipline:" + name.lower()
        section = None
        for ln in lines_of(page(cache, "PsychicDisciplinesDisplay.aspx?ItemName=" + name)):
            hm = re.match(r"^\*\*([^:*]+):\s*(.*)$", ln)
            if hm:
                section = hm.group(1).strip().lower()
                if section == "phrenic pool ability":
                    ab = hm.group(2).strip().rstrip(".").lower()
                    if ab in ABILITIES:
                        pool[name.lower()] = ABILITIES[ab]
                continue
            am = ABILITY_LINE.match(ln)
            if am and section == "discipline powers":
                add_power(key, level_of(am.group(3)), am.group(1).strip(), am.group(3))

    # ---- inquisitions (jedna stránka) ----
    key, section, granted_text = None, None, ""
    count_before = 0

    def close_inquisition():
        # inquisition bez pojmenovaných schopností (Black Powder) → jedna schopnost se jménem inquisition
        if key and len(powers) == count_before and granted_text:
            add_power(key, 1, key.split(":")[1].title() + " Inquisition", granted_text)

    for ln in lines_of(page(cache, "Inquisitions.aspx")):
        m = re.match(r"^(?:# )?([A-Z][\w' -]+?) Inquisition$", ln)
        if m:
            close_inquisition()
            key, section, granted_text, count_before = "inquisition:" + m.group(1).strip().lower(), None, "", len(powers)
            continue
        if not key:
            continue
        hm = re.match(r"^\*\*(Deities|Granted Powers|Source|Replacement Power)\b:?\s*(.*)$", ln)
        if hm:
            section = hm.group(1).strip().lower()
            rest = hm.group(2).strip()
            if section == "deities":
                ds = [] if rest.lower().startswith(("any", "n/a")) else [d.strip().lower() for d in re.split(r",|\band\b", rest.rstrip(".")) if d.strip()]
                inq_deities[key.split(":")[1]] = ds
            elif section == "granted powers":
                granted_text = rest
            continue
        am = ABILITY_LINE.match(ln)
        if am and section == "granted powers":
            add_power(key, level_of(am.group(3)), am.group(1).strip(), am.group(3))
    close_inquisition()

    powers.sort(key=lambda r: (r[0], r[1], r[2]))
    lines = [
        "// GENERATED by tools/build_subclass_abilities.py from Archives of Nethys (OGL) – do not edit by hand.",
        "package cz.pfspells.data",
        "",
        "internal val AON_SUBCLASS_POWER_TABLE: List<SubclassPower> = listOf(",
    ]
    for k, level, name, f, unit in powers:
        extra = f", uses = {kstr(f)}, unit = ResourceUnit.{unit}" if f else ""
        lines.append(f"    SubclassPower({kstr(k)}, {level}, {kstr(name)}{extra}),")
    lines += [")", "", "internal val DISCIPLINE_POOL_ABILITY: Map<String, Ability> = mapOf("]
    for d in sorted(pool):
        lines.append(f"    {kstr(d)} to Ability.{pool[d].upper()},")
    lines += [")", "", "internal val BLOODLINE_BONUS_FEATS: Map<String, List<String>> = mapOf("]
    for k in sorted(bonus_feats):
        lines.append(f"    {kstr(k)} to listOf({', '.join(kstr(f) for f in bonus_feats[k])}),")
    lines += [")", "", "internal val INQUISITION_DEITIES: Map<String, List<String>> = mapOf("]
    for q in sorted(inq_deities):
        lines.append(f"    {kstr(q)} to listOf({', '.join(kstr(d) for d in inq_deities[q])}),")
    lines.append(")")
    OUT_KT.write_text("\n".join(lines) + "\n", encoding="utf-8")

    rows = sorted(hexes.values(), key=lambda r: r["name"])
    OUT_JSON.write_text(json.dumps(rows, ensure_ascii=False, indent=0, separators=(",", ":")) + "\n", encoding="utf-8")
    by = {}
    for p in powers:
        by[p[0].split(":")[0]] = by.get(p[0].split(":")[0], 0) + 1
    print(f"{OUT_KT.relative_to(ROOT)}: {len(powers)} powers {by} ({sum(1 for p in powers if p[3])} with uses), "
          f"{len(pool)} phrenic pool abilities, {len(inq_deities)} inquisitions, {len(bonus_feats)} bloodline bonus feat lists")
    print(f"{OUT_JSON.relative_to(ROOT)}: {len(rows)} spirit hexes")


if __name__ == "__main__":
    main()
