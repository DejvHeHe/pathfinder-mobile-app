#!/usr/bin/env python3
"""
Animal companions and familiars from d20pfsrd.com (OGL; Paizo content only – "Other Publishers" sections and entries sourced from opengamingstore.com are skipped):

1. composeApp/src/commonMain/kotlin/cz/pfspells/data/SpeciesTable.kt – animal companion species from the section
   "Animal Companion Descriptions" (starting statistics + 4th / 7th-level advancement). The 17 Core Rulebook animals
   already written by hand in AnimalCompanions.kt (stable ids) are skipped.
2. composeApp/src/commonMain/kotlin/cz/pfspells/data/FamiliarTable.kt – familiars with the master's bonus
   (parsed into skill / save / AC / HP bonuses where unconditional) and the source code.

Pages (2 lists + ~90 bestiary pages of the familiars, 1 per second, cached in build/companion-cache):
  https://www.d20pfsrd.com/classes/core-classes/druid/animal-companions/
  https://www.d20pfsrd.com/classes/core-classes/wizard/familiar/

Usage:  python tools/build_companions.py [--cache DIR]
"""
import html
import re
import sys
import time
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OUT_SPECIES = ROOT / "composeApp/src/commonMain/kotlin/cz/pfspells/data/SpeciesTable.kt"
OUT_FAMILIARS = ROOT / "composeApp/src/commonMain/kotlin/cz/pfspells/data/FamiliarTable.kt"
PAGES = {
    "companions.html": "https://www.d20pfsrd.com/classes/core-classes/druid/animal-companions/",
    "familiars.html": "https://www.d20pfsrd.com/classes/core-classes/wizard/familiar/",
}
# ručně zadané druhy v AnimalCompanions.kt (Core Rulebook) – generátor je přeskočí
HAND_WRITTEN = {
    "Ape", "Badger", "Bear", "Bird", "Boar", "Camel", "Cat, Big", "Cat, Small", "Crocodile", "Deinonychus", "Dog",
    "Horse", "Pony", "Shark", "Snake, Constrictor", "Snake, Viper", "Wolf",
}
# dvounozí / beznozí tvorové (nosnost podle tabulky dvounohých); ostatní zvířata jsou čtyřnohá
NOT_QUADRUPED = re.compile(
    r"ape|baboon|chimpanzee|monkey|bird|egret|cassowary|eagle|owl|stork|bustard|falcon|heron|kiwi|moa|ostrich|roc|"
    r"raven|shrike|swan|vulture|axe beak|archaeopteryx|allosaurus|ceratosaurus|deinonychus|giganotosaurus|"
    r"ornithomimosaur|spinosaurus|therizinosaurus|troodon|tyrannosaurus|velociraptor|pachycephalosaurus|"
    r"iguanodon|parasaurolophus|kangaroo|dimorphodon|pteranodon|quetzalcoatlus|bat|snake|python|titanoboa|eel|"
    r"fish|gar|shark|ray|dolphin|whale|squid|octopus|seahorse|plesiosaurus|elasmosaurus|basilosaurus|"
    r"dunkleosteus|tylosaurus|cameroceras|archelon|mantis|drake",
    re.I,
)
SIZES = ["fine", "diminutive", "tiny", "small", "medium", "large", "huge", "gargantuan", "colossal"]


def cached(cache: Path, name: str, url: str) -> str:
    f = cache / name
    if not f.exists():
        cache.mkdir(parents=True, exist_ok=True)
        time.sleep(1)
        req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0 pf-spellbook-build"})
        with urllib.request.urlopen(req, timeout=120) as r:
            f.write_bytes(r.read())
    return f.read_bytes().decode("utf-8", errors="replace")


def plain(h: str) -> str:
    t = re.sub(r"<[^>]+>", "", h)
    t = html.unescape(t).replace(" ", " ").replace("–", "-").replace("−", "-").replace("—", "-")
    return re.sub(r"\s+", " ", t).strip()


def slug(s: str) -> str:
    return re.sub(r"[^a-z0-9]+", "_", s.lower()).strip("_")


def kstr(s: str) -> str:
    return '"' + s.replace("\\", "\\\\").replace('"', '\\"').replace("$", "\\$") + '"'


def field(text: str, label: str, nxt: str = r"(?:Size|Speed|AC|Attacks?|Ability Scores|Special (?:Qualities|Attacks|Abilities)|CMD|Bonus Feat)\b"):
    m = re.search(r"\b" + label + r":?\s+(.*?)(?=;\s*" + nxt + r"|$)", text)
    return m.group(1).strip().rstrip(";.").strip() if m else None


def stat(text: str, ab: str):
    m = re.search(r"\b" + ab + r"\s+([+-]?\d+)", text)
    return int(m.group(1)) if m else None


def size_of(s):
    if not s:
        return None
    w = s.split()[0].strip(",;.").lower()
    return w.upper() if w in SIZES else None


def species(page: str):
    heads = [(m.start(), plain(m.group(1))) for m in re.finditer(r"<h3[^>]*>(.*?)</h3>", page, re.S)]
    st = next(p for p, h in heads if h == "Animal Companion Descriptions")
    en = next(p for p, h in heads if h == "Aberration Companions")
    sec = page[st:en]
    blocks = list(re.finditer(r"<h4[^>]*>(.*?)</h4>", sec, re.S))
    out, seen = [], set()
    for i, b in enumerate(blocks):
        name = plain(b.group(1))
        body = sec[b.end(): blocks[i + 1].start() if i + 1 < len(blocks) else len(sec)]
        # obsah třetích stran (zdroj v obchodě opengamingstore.com) – appka bere jen Paizo
        src = re.search(r'class="source".*?href="https?://(?:www\.)?([^/"]+)', body, re.S)
        if src and "opengamingstore" in src.group(1):
            continue
        parts = re.split(r'<p class="divider">(.*?)</p>', body)
        start, adv, adv_level = None, None, None
        for j in range(1, len(parts) - 1, 2):
            label = plain(parts[j])
            p = re.search(r"<p>(.*?)</p>", parts[j + 1], re.S)
            text = plain(p.group(1)) if p else ""
            if label.lower().startswith("starting statistics") and start is None:
                start = text
            m = re.match(r"(\d+)(?:st|nd|rd|th)-Level Advancement", label, re.I)
            if m and adv is None:
                adv, adv_level = text, int(m.group(1))
        if not start or name in HAND_WRITTEN or slug(name) in seen:
            continue
        seen.add(slug(name))
        abil = field(start, "Ability Scores") or ""
        scores = [stat(abil, a) for a in ("Str", "Dex", "Con", "Int", "Wis", "Cha")]
        if any(v is None for v in scores[:3]):
            continue
        scores = [v if v is not None else 0 for v in scores]  # Int „—“ (bez Int) → 0
        size = size_of(field(start, "Size")) or "MEDIUM"
        ac = re.search(r"\+(\d+)\s*natural armor", field(start, "AC") or "")
        adv_ab = field(adv or "", "Ability Scores") or ""
        adv_ac = re.search(r"\+(\d+)\s*natural armor", field(adv or "", "AC") or "")
        out.append({
            "id": slug(name), "name": name, "size": size, "quadruped": not NOT_QUADRUPED.search(name),
            "speed": field(start, "Speed") or "", "na": int(ac.group(1)) if ac else 0,
            "attacks": field(start, "Attacks?") or "", "scores": scores,
            "qualities": field(start, r"Special Qualities") or "",
            "advLevel": adv_level or 4, "advSize": size_of(field(adv or "", "Size")),
            "advNa": int(adv_ac.group(1)) if adv_ac else 0, "advAttacks": field(adv or "", "Attacks?"),
            "advStr": stat(adv_ab, "Str") or 0, "advDex": stat(adv_ab, "Dex") or 0, "advCon": stat(adv_ab, "Con") or 0,
            "advQualities": field(adv or "", r"Special Qualities"),
        })
    return out


SKILLS = {
    "acrobatics": "acrobatics", "appraise": "appraise", "bluff": "bluff", "climb": "climb", "diplomacy": "diplomacy",
    "escape artist": "escapeArtist", "fly": "fly", "heal": "heal", "intimidate": "intimidate", "linguistics": "linguistics",
    "perception": "perception", "perform": "perform", "sleight of hand": "sleightOfHand", "spellcraft": "spellcraft",
    "stealth": "stealth", "survival": "survival", "swim": "swim",
}
SAVES = {"fortitude": "FORT", "reflex": "REF", "will": "WILL"}


def master_bonus(text: str):
    """Nepodmíněný bonus pánovi → (target, value, skill); podmíněný („in bright light“, „to identify“) → None."""
    t = text.strip().rstrip(".")
    m = re.fullmatch(r"Master gains (?:a )?\+(\d+) bonus on ([A-Za-z ]+?) checks", t)
    if m and m.group(2).lower() in SKILLS:
        return "SKILL", int(m.group(1)), SKILLS[m.group(2).lower()]
    m = re.fullmatch(r"Master gains (?:a )?\+(\d+) bonus on (Fortitude|Reflex|Will) saves", t)
    if m:
        return SAVES[m.group(2).lower()], int(m.group(1)), None
    m = re.fullmatch(r"Master gains (?:a )?\+(\d+) natural armor bonus to AC", t)
    if m:
        return "AC", int(m.group(1)), None
    m = re.fullmatch(r"Master gains \+(\d+) hit points", t)
    if m:
        return "HP", int(m.group(1)), None
    return None


def familiars(page: str):
    table = re.findall(r"<table.*?</table>", page, re.S)[0]
    out, seen = [], set()
    for r in re.findall(r"<tr.*?</tr>", table, re.S)[1:]:
        cells = [plain(c) for c in re.findall(r"<td.*?</td>", r, re.S)]
        if len(cells) < 3:
            continue
        raw, bonus, source = cells[0], cells[1], cells[2]
        name = raw.split(" (")[0].replace("*", "").strip()
        note = raw[len(raw.split(" (")[0]):].strip().strip("()").strip() if " (" in raw else ""
        fid = slug(name)
        if fid in seen:
            continue
        seen.add(fid)
        link = re.search(r'href="([^"]+)"', re.findall(r"<td.*?</td>", r, re.S)[0])
        out.append({"id": fid, "name": name, "note": note, "bonus": bonus.rstrip("."), "parsed": master_bonus(bonus),
                    "source": source, "url": link.group(1) if link else None})
    return out


def statblock(page: str, name: str):
    """Statistiky tvora z bestiáře d20pfsrd: velikost, rychlost, přirozená zbroj, útoky, atributy, smysly.
    Na stránce s víc tvory se bere statblok, jehož název se nejvíc shoduje se jménem familiára."""
    # statblok začíná nadpisem <p class="title"> (někdy v <div class="statblock">); bloky bez atributů se přeskočí
    start = page.find("<h1")
    parts = re.split(r'<p class="title">', page[start if start >= 0 else 0:])[1:]
    blocks = [p for p in parts if re.search(r"<b>\s*Str\s*</b>", p)]
    if not blocks:
        return None
    words = [w for w in re.split(r"[^a-z]+", name.lower()) if w]

    def score(b):
        title = plain(b[: b.find("</p>")]).lower()
        return sum(w in title for w in words) - (0.5 if "dire" in title or "giant" in title and "giant" not in words else 0)
    b = max(blocks, key=score)
    b = b[: b.find('<p class="divider">SPECIAL ABILITIES') if 'SPECIAL ABILITIES' in b else len(b)]
    text = plain(re.sub(r"<br\s*/?>", "; ", b))
    size = re.search(r"\b(Fine|Diminutive|Tiny|Small|Medium|Large|Huge)\b", text)
    na = re.search(r"\+(\d+)\s*natural", text)
    speed = re.search(r"\bSpeed\s+(.*?)(?:;|$)", text)
    melee = re.search(r"\bMelee\s+(.*?)(?:;|$)", text)
    senses = re.search(r"\bSenses\s+(.*?)(?:;\s*Perception|;|$)", text)
    scores = []
    for a in ("Str", "Dex", "Con", "Int", "Wis", "Cha"):
        m = re.search(r"\b" + a + r"\s+(\d+|—|-)", text)
        scores.append(int(m.group(1)) if m and m.group(1).isdigit() else 0)
    if scores[1] == 0:
        return None
    return {
        "size": size.group(1).upper() if size else "TINY", "na": int(na.group(1)) if na else 0,
        "speed": speed.group(1).strip().rstrip(".") if speed else "", "attacks": melee.group(1).strip() if melee else "",
        "scores": scores, "senses": senses.group(1).strip() if senses else "",
    }


def main():
    args = sys.argv[1:]
    cache = ROOT / "build/companion-cache"
    if "--cache" in args:
        cache = Path(args[args.index("--cache") + 1])
    pages = {n: cached(cache, n, u) for n, u in PAGES.items()}

    sp = species(pages["companions.html"])
    lines = [
        "// GENERATED by tools/build_companions.py from d20pfsrd.com (Animal Companions, OGL) – do not edit by hand.",
        "package cz.pfspells.data",
        "",
        "/** Další druhy zvířecích společníků (APG, Ultimate Magic, Bestiary…); Core Rulebook zvířata jsou v AnimalCompanions.kt. */",
        "internal val EXTRA_SPECIES: List<AnimalSpecies> = listOf(",
    ]
    for s in sp:
        sc = s["scores"]
        args_ = [
            kstr(s["id"]), kstr(s["name"]), f"CreatureSize.{s['size']}", "true" if s["quadruped"] else "false",
            kstr(s["speed"]), str(s["na"]), kstr(s["attacks"]), f"Abilities({', '.join(map(str, sc))})", kstr(s["qualities"]),
            str(s["advLevel"]),
            f"advSize = {'CreatureSize.' + s['advSize'] if s['advSize'] else 'null'}", f"advNaturalArmor = {s['advNa']}",
            f"advAttacks = {kstr(s['advAttacks']) if s['advAttacks'] else 'null'}",
            f"advStr = {s['advStr']}", f"advDex = {s['advDex']}", f"advCon = {s['advCon']}",
            f"advQualities = {kstr(s['advQualities']) if s['advQualities'] else 'null'}",
        ]
        lines.append(f"    AnimalSpecies({', '.join(args_)}),")
    lines.append(")")
    OUT_SPECIES.write_text("\n".join(lines) + "\n", encoding="utf-8")

    fam = familiars(pages["familiars.html"])
    lines = [
        "// GENERATED by tools/build_companions.py from d20pfsrd.com (Familiar, OGL) – do not edit by hand.",
        "package cz.pfspells.data",
        "",
        "/** Familiáři: bonus pánovi (text + nepodmíněný bonus jako ItemBonus), poznámka ke statistikám, zdroj. */",
        "internal val FAMILIAR_TABLE: List<FamiliarInfo> = listOf(",
    ]
    missing = []
    for f in fam:
        st = None
        if f["url"]:
            try:
                page = cached(cache / "familiars", f["id"] + ".html", f["url"])
                st = statblock(page, f["name"])
            except Exception as e:  # rozbitý odkaz (404) – familiár zůstane bez statistik
                print(f"  {f['name']}: {e}")
        f["stats"] = st
    # „use butterfly statistics“ – statistiky jiného familiára
    by_name = {x["name"].lower(): x for x in fam}
    for f in fam:
        m = re.search(r"use ([a-z ,]+?) statistics", f["note"].lower())
        if f["stats"] is None and m and by_name.get(m.group(1).strip(), {}).get("stats"):
            f["stats"] = by_name[m.group(1).strip()]["stats"]
    for f in fam:
        st = f["stats"]
        if st is None:
            missing.append(f["name"])
        p = f["parsed"]
        if p is None:
            bonus = "null"
        else:
            target, value, skill = p
            typ = "BonusType.NATURAL_ARMOR" if target == "AC" else "BonusType.UNTYPED"
            bonus = f"ItemBonus(BonusTarget.{target}, {value}, {typ}" + (f", skill = {kstr(skill)})" if skill else ")")
        stats = ""
        if st:
            stats = (f", size = CreatureSize.{st['size']}, speed = {kstr(st['speed'])}, naturalArmor = {st['na']}, "
                     f"attacks = {kstr(st['attacks'])}, abilities = Abilities({', '.join(map(str, st['scores']))}), senses = {kstr(st['senses'])}")
        lines.append(f"    FamiliarInfo({kstr(f['id'])}, {kstr(f['name'])}, {kstr(f['bonus'])}, {bonus}, note = {kstr(f['note'])}, source = {kstr(f['source'])}{stats}),")
    lines.append(")")
    OUT_FAMILIARS.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"{OUT_SPECIES.relative_to(ROOT)}: {len(sp)} species; {OUT_FAMILIARS.relative_to(ROOT)}: {len(fam)} familiars "
          f"({sum(1 for f in fam if f['parsed'])} with a computed master bonus, {len(fam) - len(missing)} with statistics)")
    if missing:
        print("familiars without a statblock:", missing)


if __name__ == "__main__":
    main()
