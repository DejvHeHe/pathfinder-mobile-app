#!/usr/bin/env python3
"""
Prestige classes from Archives of Nethys (OGL / Paizo Community Use Policy):

1. composeApp/src/commonMain/kotlin/cz/pfspells/data/PrestigeTable.kt – per prestige class: max level, hit die, skill
   ranks, class skills, BAB and saves per level (from the class table), spellcasting advancement per level ("+1 level
   of existing arcane spellcasting class" → "arcane"), bonus feat levels, requirements, source, PFS flag.
2. composeApp/src/commonMain/composeResources/files/PrestigeClasses.json – description and class features text (lazy).

List: https://aonprd.com/PrestigeClasses.aspx, pages PrestigeClassesDisplay.aspx?ItemName=X (~120 requests, 1 per
second, cached in build/prestige-cache). Foundry VTT PF1 has no prestige classes.

Usage:  python tools/build_prestige_classes.py [--cache DIR]
        (when Python cannot verify the certificate, download the pages with curl into the cache directory:
         "list.html" and "<url-encoded name>.html")
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
OUT_KT = ROOT / "composeApp/src/commonMain/kotlin/cz/pfspells/data/PrestigeTable.kt"
OUT_JSON = ROOT / "composeApp/src/commonMain/composeResources/files/PrestigeClasses.json"
BASE = "https://aonprd.com/"

SKILLS = {
    "acrobatics": "acrobatics", "appraise": "appraise", "bluff": "bluff", "climb": "climb", "craft": "craft",
    "diplomacy": "diplomacy", "disable device": "disableDevice", "disguise": "disguise", "escape artist": "escapeArtist",
    "fly": "fly", "handle animal": "handleAnimal", "heal": "heal", "intimidate": "intimidate", "knowledge": "knowledge",
    "linguistics": "linguistics", "perception": "perception", "perform": "perform", "profession": "profession",
    "ride": "ride", "sense motive": "senseMotive", "sleight of hand": "sleightOfHand", "spellcraft": "spellcraft",
    "stealth": "stealth", "survival": "survival", "swim": "swim", "use magic device": "useMagicDevice",
}
KNOWLEDGE = ["arcana", "dungeoneering", "engineering", "geography", "history", "local", "nature", "nobility", "planes", "religion"]


def fetch(url: str) -> str:
    req = urllib.request.Request(url, headers={"User-Agent": "pf-spellbook-build"})
    with urllib.request.urlopen(req, timeout=120) as r:
        return r.read().decode("utf-8", errors="replace")


def cached(cache: Path, name: str, url: str) -> str:
    f = cache / name
    if f.exists():
        return f.read_text(encoding="utf-8", errors="replace")
    cache.mkdir(parents=True, exist_ok=True)
    time.sleep(1)
    t = fetch(url)
    f.write_text(t, encoding="utf-8")
    return t


def plain(h: str) -> str:
    t = re.sub(r"<\s*br\s*/?>", "\n", h, flags=re.I)
    t = re.sub(r"<\s*/?(p|li|ul|table|tr|h\d)[^>]*>", "\n", t, flags=re.I)
    t = re.sub(r"<\s*/t[dh]\s*>", " ", t, flags=re.I)
    t = re.sub(r"<[^>]+>", "", t)
    t = html.unescape(t).replace(" ", " ")
    lines = [re.sub(r"[ \t]+", " ", x).strip() for x in t.split("\n")]
    return "\n".join(x for x in lines if x).strip()


def class_id(name: str) -> str:
    return re.sub(r"[^a-z0-9]+", "_", name.lower()).strip("_")


def kstr(s: str) -> str:
    s = " ".join(s.split())  # řádky požadavku do jedné řádky
    return '"' + s.replace("\\", "\\\\").replace('"', '\\"').replace("$", "\\$") + '"'


def num(cell: str) -> int:
    m = re.search(r"[+\-–−]?\d+", plain(cell))
    return int(m.group(0).replace("–", "-").replace("−", "-")) if m else 0


def skill_ids(text: str):
    """„Climb (Str), Knowledge (arcana) (Int), Knowledge (all) (Int)…“ → id skillů appky."""
    out = set()
    for m in re.finditer(r"([A-Z][A-Za-z ]+?)(?:\s*\(([^)]*)\))?\s*\((?:Str|Dex|Con|Int|Wis|Cha)\)", text):
        base = m.group(1).strip().lower()
        sub = (m.group(2) or "").lower()
        if base == "knowledge":
            ks = [k for k in KNOWLEDGE if k in sub]
            if not ks or "all" in sub or "any" in sub:
                out.add("knowledge")
            else:
                out |= {f"knowledge.{k}" for k in ks}
        elif base in SKILLS:
            out.add(SKILLS[base])
    return sorted(out)


ADVANCE = re.compile(r"\+1 (?:level|caster level) (?:of|in|to) (?:an? |any )?(?:existing )?([a-z -]+)", re.I)
CASTER_CLASSES = ["alchemist", "witch", "wizard", "sorcerer", "cleric", "druid", "oracle", "bard", "inquisitor", "magus",
                  "paladin", "ranger", "summoner", "shaman", "psychic", "arcanist", "investigator", "warpriest", "skald"]


def casting_kinds(cell: str):
    """„+1 level of existing arcane spellcasting class“ → [arcane]; mystic theurge → [arcane, divine];
    „+1 level of spellcasting class“ → [any]; „+1 level of witch class“ → [class:witch]."""
    kinds = []
    for m in ADVANCE.finditer(plain(cell)):
        words = m.group(1).lower()
        found = [k for k in ("arcane", "divine", "psychic") if k in words]
        if not found:
            found = ["class:" + c for c in CASTER_CLASSES if re.search(r"\b" + c + r"\b", words)][:1]
        kinds += found if found else ["any"]
    return kinds


def section(page: str, title: str):
    m = re.search(r'<h2 class="title">\s*' + title + r"\s*</h2>", page)
    if not m:
        return None
    end = page.find('<h2 class="title">', m.end())
    return page[m.end(): end if end >= 0 else len(page)]


def parse(page: str):
    start = page.find('<h1 class="title">')
    end = page.find("</span>", start)
    body = page[start:end]
    h1 = body[: body.find("</h1>")]
    name = plain(h1)
    pfs = "PathfinderSociety" in h1
    src = re.search(r"<b>\s*Source\s*</b>\s*((?:<a[^>]*>\s*<i>[^<]*</i>\s*</a>\s*,?\s*)+)", body)
    source = ", ".join(html.unescape(s).strip() for s in re.findall(r"<i>([^<]*)</i>", src.group(1))) if src else ""
    first_h2 = body.find('<h2 class="title">')
    description = plain(body[src.end() if src else body.find("</h1>") + 5: first_h2 if first_h2 > 0 else len(body)])

    req = section(body, "Requirements") or ""
    requirements = []
    for m in re.finditer(r"<b>\s*([^<]+?)\s*</b>\s*:?\s*(.*?)(?=<b>|$)", req, re.S):
        label, text = plain(m.group(1)).rstrip(":"), plain(m.group(2)).lstrip(":").strip()
        if text:
            requirements.append([label, text])

    skills_sec = section(body, "Class Skills") or ""
    class_skills = skill_ids(plain(skills_sec))
    sp = re.search(r"Skill (?:Points|Ranks) at each Level\s*</b>\s*:?\s*(\d+)", skills_sec)
    hd = re.search(r"Hit Die\s*</b>\s*:?\s*d(\d+)", skills_sec)

    feat_sec = section(body, "Class Features") or ""
    tm = re.search(r"<table[^>]*>(.*?)</table>", feat_sec, re.S)
    rows = re.findall(r"<tr[^>]*>(.*?)</tr>", tm.group(1), re.S) if tm else []
    cells = [re.findall(r"<td[^>]*>(.*?)</td>", r, re.S) for r in rows]
    header = [plain(c).lower() for c in cells[0]] if cells else []

    def col(*words):
        for i, h in enumerate(header):
            if any(w in h for w in words):
                return i
        return None

    ci = {k: col(*w) for k, w in {
        "bab": ("base attack", "bab"), "fort": ("fort",), "ref": ("ref",), "will": ("will",),
        "special": ("special",), "spells": ("spell", "casting"),
    }.items()}
    levels = []
    for r in cells[1:]:
        if not r or not re.match(r"\d", plain(r[0])):
            continue
        get = lambda k: r[ci[k]] if ci[k] is not None and ci[k] < len(r) else ""
        cast = casting_kinds(get("spells")) or casting_kinds(get("special"))
        levels.append({
            "bab": num(get("bab")), "fort": num(get("fort")), "ref": num(get("ref")), "will": num(get("will")),
            "cast": cast, "feat": bool(re.search(r"bonus (?:combat |teamwork |metamagic )?feat", plain(get("special")), re.I)),
        })
    features = plain(feat_sec[tm.end():]) if tm else plain(feat_sec)
    return {
        "name": name, "pfs": pfs, "source": source, "description": description, "requirements": requirements,
        "classSkills": class_skills, "skills": int(sp.group(1)) if sp else 2, "hd": int(hd.group(1)) if hd else 8,
        "levels": levels, "features": features,
    }


def main():
    args = sys.argv[1:]
    cache = ROOT / "build/prestige-cache"
    if "--cache" in args:
        cache = Path(args[args.index("--cache") + 1])
    listing = cached(cache, "list.html", BASE + "PrestigeClasses.aspx")
    names = sorted({html.unescape(n) for n in re.findall(r'PrestigeClassesDisplay\.aspx\?ItemName=([^"&]+)"', listing)})
    out, texts, bad = [], [], []
    for name in names:
        page = cached(cache, urllib.parse.quote(name, safe="") + ".html",
                      BASE + "PrestigeClassesDisplay.aspx?ItemName=" + urllib.parse.quote(name))
        p = parse(page)
        if not p["levels"]:
            bad.append(name)
            continue
        p["id"] = class_id(p["name"])
        out.append(p)
        texts.append({"id": p["id"], "description": p["description"], "features": p["features"]})
        print(f"{p['name']}: {len(p['levels'])} levels, casting {sum(len(l['cast']) for l in p['levels'])}", flush=True)

    ints = lambda xs: kstr(" ".join(str(x) for x in xs))
    lines = [
        "// GENERATED by tools/build_prestige_classes.py from Archives of Nethys (prestige class pages, OGL / Paizo CUP) – do not edit by hand.",
        "package cz.pfspells.data",
        "",
        "/** Prestižní classy: BAB a záchrany po úrovních, sesílání po úrovních („arcane“, „divine“, „psychic“, „any“; víc = „arcane+divine“). */",
        "internal val PRESTIGE_TABLE: List<PrestigeInfo> = listOf(",
    ]
    for p in sorted(out, key=lambda x: x["name"]):
        lv = p["levels"]
        lines.append(
            f"    PrestigeInfo({kstr(p['id'])}, {kstr(p['name'])}, hd = {p['hd']}, skills = {p['skills']}, "
            f"classSkills = setOf({', '.join(kstr(s) for s in p['classSkills'])}), "
            f"bab = {ints(l['bab'] for l in lv)}, fort = {ints(l['fort'] for l in lv)}, "
            f"ref = {ints(l['ref'] for l in lv)}, will = {ints(l['will'] for l in lv)}, "
            f"casting = {kstr('|'.join('+'.join(l['cast']) for l in lv))}, "
            f"bonusFeats = listOf({', '.join(str(i + 1) for i, l in enumerate(lv) if l['feat'])}), "
            f"requirements = listOf({', '.join(kstr(a + ': ' + b) for a, b in p['requirements'])}), "
            f"source = {kstr(p['source'])}, pfs = {'true' if p['pfs'] else 'false'}),"
        )
    lines.append(")")
    OUT_KT.write_text("\n".join(lines) + "\n", encoding="utf-8")
    OUT_JSON.write_text(json.dumps(texts, ensure_ascii=False, indent=0, separators=(",", ":")) + "\n", encoding="utf-8")
    print(f"{OUT_KT.relative_to(ROOT)}: {len(out)} prestige classes; without class table: {bad}")


if __name__ == "__main__":
    main()
