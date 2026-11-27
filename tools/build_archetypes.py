#!/usr/bin/env python3
"""
Class archetypes from Archives of Nethys (OGL / Paizo Community Use Policy):

composeApp/src/commonMain/composeResources/files/Archetypes.json – archetypes of every class with the class features
they replace, short summary, source, PFS flag and full text. The app shows them only as a record (nothing is computed).

Lists: https://aonprd.com/Archetypes.aspx?Class=<Class> (table Name · Replaces · Summary), archetype pages
ArchetypeDisplay.aspx?FixedName=<Class> <Name> (~1 300 requests, 1 per second, cached in build/archetype-cache).
Class names come from ClassTable.kt (the app); unchained classes use the archetypes of the base class (the app maps
them), NPC classes have none.

Usage:  python tools/build_archetypes.py [--cache DIR]
        (when Python cannot verify the certificate, download the pages with curl into the cache directory:
         "list-<Class>.html" and "<url-encoded FixedName>.html")
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
OUT_JSON = ROOT / "composeApp/src/commonMain/composeResources/files/Archetypes.json"
CLASS_TABLE = ROOT / "composeApp/src/commonMain/kotlin/cz/pfspells/data/ClassTable.kt"
SPELL_LIST = ROOT / "composeApp/src/commonMain/composeResources/files/SpellList.json"
BASE = "https://aonprd.com/"
NPC = {"adept", "aristocrat", "commoner", "expert", "warrior"}
# kouzla v textu nepatří na spell list classy (magie ducha šamana, objevy alchymisty místo discovery)
NO_ADDED_SPELLS = {"shaman:animist", "alchemist:dimensional-excavator"}
# „x (4th)“ = úroveň classy, ne spell level
PAREN_IS_CLASS_LEVEL = {"mesmerist:projectionist"}


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


def slug(s: str) -> str:
    return re.sub(r"[^a-z0-9]+", "-", s.lower()).strip("-")


def plain(h: str) -> str:
    t = re.sub(r"<\s*br\s*/?>", "\n", h, flags=re.I)
    t = re.sub(r"<\s*/?(p|li|ul|table|tr|h\d)[^>]*>", "\n", t, flags=re.I)
    t = re.sub(r"<\s*/t[dh]\s*>", " ", t, flags=re.I)
    t = re.sub(r"<[^>]+>", "", t)
    t = html.unescape(t).replace(" ", " ")
    lines = [re.sub(r"[ \t]+", " ", x).strip() for x in t.split("\n")]
    return "\n".join(x for x in lines if x).strip()


ORDINAL = re.compile(r"(?:and\s+)?\d+(?:-?(?:st|nd|rd|th))?", re.I)


def split_replaces(cell: str):
    """„Smite Evil; 2nd, 4th, 6th-level hex; Brew Potion, Poison Use“ → položky; seznam úrovní zůstane u své schopnosti."""
    out = []
    for part in re.split(r";", cell):
        pending = []
        for p in (x.strip() for x in plain(part).split(",")):
            if not p:
                continue
            if ORDINAL.fullmatch(p):  # „2nd“ před „4th-level discovery“
                pending.append(p)
                continue
            if pending:
                p = ", ".join(pending + [p])
                pending = []
            out.append(p)
        if pending:  # „outside contact 2, 3, and 4“ – čísla na konci patří k předchozí položce
            if out:
                out[-1] = ", ".join([out[-1]] + pending)
            else:
                out.append(", ".join(pending))
    return [r for r in out if r.lower() not in ("none", "—", "-")]


ROW = re.compile(
    r'<tr[^>]*>\s*<td>\s*<a href="ArchetypeDisplay\.aspx\?FixedName=([^"]+)">(.*?)</a>\s*</td>\s*<td>(.*?)</td>\s*<td>(.*?)</td>',
    re.S,
)


def class_list(page: str):
    out = []
    for m in ROW.finditer(page):
        fixed = html.unescape(m.group(1))
        replaces = split_replaces(m.group(3))
        out.append({
            "fixed": fixed,
            "name": plain(m.group(2)),
            "pfs": "PathfinderSociety" in m.group(2),
            "replaces": replaces,
            "summary": plain(m.group(4)),
        })
    return out


SOURCE = re.compile(r"<b>\s*Source\s*</b>\s*((?:<a[^>]*>\s*<i>[^<]*</i>\s*</a>\s*,?\s*)+)", re.S)


def archetype_page(page: str):
    start = page.find('<h1 class="title">')
    if start < 0:
        return "", ""
    end = page.find("</span>", start)
    body = page[start: end if end >= 0 else len(page)]
    body = body[body.find("</h1>") + 5:]
    source = ""
    m = SOURCE.search(body)
    if m:
        source = ", ".join(html.unescape(s).strip() for s in re.findall(r"<i>([^<]*)</i>", m.group(1)))
        body = body[m.end():]
    return source, plain(body)


def kslug(name: str) -> str:
    """Id kouzla jako v appce (SpellParser.slug)."""
    return re.sub(r"[^a-z0-9]+", "-", name.lower()).strip("-")


def spell_names():
    """Jméno kouzla (malými) → id v appce; jen kouzla s class listem, duplicitní jména se přeskočí."""
    data = json.loads(SPELL_LIST.read_text(encoding="utf-8"))
    out, seen = {}, set()
    for s in data:
        n = (s.get("Name") or "").strip()
        if not n or not ((s.get("access_ways") or {}).get("Level")):
            continue
        sid = kslug(n)
        if sid in seen:
            continue
        seen.add(sid)
        out[n.lower()] = sid
    return out


SOURCE_SUFFIX = re.compile(r"(?<=[a-z\sIVX])(?:APG|UM|UC|ACG|OA|UI|ARG|UW|ISWG|ISM|HA|MA|UE|PPC|AG|ACO|UCA|VC|BotD)$")
LEVEL_MARK = re.compile(r"(?<![\w])(\d{1,2})(?:st|nd|rd|th)?\s*[—–]\s*")
PAREN_LEVEL = re.compile(r"([A-Za-z’'][^,;:()]*?)\s*\((\d)(?:st|nd|rd|th)?(?:[- ]level)?\)")
AS_LEVEL = re.compile(r"adds? ([a-z’' ,/]+?) to (?:his|her|their|the) [\w/ ]*?(?:spell )?list as an? (\d)(?:st|nd|rd|th)-level spell", re.I)
TRIGGER = re.compile(r"add(?:s|ed)?[^.]{0,120}spells?|spell list|class list|list of spells", re.I)


def match_spell(raw: str, names):
    """Kandidát jména z textu → id kouzla (ověřené proti SpellList.json) nebo None."""
    n = raw.strip().strip(".").strip()
    n = re.sub(r"^(?:and|or)\s+", "", n).strip()
    cands = [n, SOURCE_SUFFIX.sub("", n).strip()]
    for c in list(cands):
        m = re.match(r"^(.*?)\s*\((mass|greater|lesser|communal)\)$", c, re.I)
        if m:
            cands.append(f"{m.group(1)}, {m.group(2)}")
        m = re.match(r"^(lesser|greater|mass|communal)\s+(.*)$", c, re.I)
        if m:
            cands.append(f"{m.group(2)}, {m.group(1)}")
    for c in cands:
        sid = names.get(c.lower().replace("’", "'"))
        if sid:
            return sid
    return None


def added_spells(aid: str, text: str, names):
    """Kouzla, která archetyp přidává na spell list: „1st—a, b; 2nd—c“, „a (1st), b (2nd)“, „adds x … as a 3rd-level spell“."""
    if aid in NO_ADDED_SPELLS:
        return {}
    out = {}

    def put(raw, lvl):
        lvl = int(lvl)
        sid = match_spell(raw, names) if 0 <= lvl <= 9 else None
        if sid and (sid not in out or lvl < out[sid]):
            out[sid] = lvl

    for p in text.split("\n"):
        if not TRIGGER.search(p) or "patron spell" in p.lower():
            continue
        marks = list(LEVEL_MARK.finditer(p))
        for i, m in enumerate(marks):
            chunk = p[m.end(): marks[i + 1].start() if i + 1 < len(marks) else len(p)]
            chunk = re.split(r"\.\s|\.$", chunk)[0]
            # „2nd—confusion, lesser (1st)“: před pomlčkou je úroveň classy, spell level je v závorce
            paren = re.search(r"\((\d)(?:st|nd|rd|th)\)", chunk)
            if paren:
                put(chunk[:paren.start()], paren.group(1))
                continue
            for part in re.split(r"[,;]", chunk):
                put(part, m.group(1))
        if aid not in PAREN_IS_CLASS_LEVEL:
            for m in PAREN_LEVEL.finditer(p):
                put(m.group(1).split(":")[-1], m.group(2))
        for m in AS_LEVEL.finditer(p):
            put(m.group(1), m.group(2))
    return out


def main():
    args = sys.argv[1:]
    cache = ROOT / "build/archetype-cache"
    if "--cache" in args:
        cache = Path(args[args.index("--cache") + 1])
    classes = {}  # AoN jméno classy → id v appce
    for cid, name in re.findall(r'ClassInfo\("([^"]+)",\s*"([^"]+)"', CLASS_TABLE.read_text(encoding="utf-8")):
        if cid in NPC or "(Unchained)" in name:
            continue
        classes[name] = cid

    names = spell_names()
    out, empty, failed = [], [], []
    for cname, cid in sorted(classes.items()):
        lst = class_list(cached(cache, f"list-{cname}.html", BASE + "Archetypes.aspx?Class=" + urllib.parse.quote(cname)))
        if not lst:
            empty.append(cname)
            continue
        seen = set()
        for a in lst:
            aid = cid + ":" + slug(a["name"])
            if aid in seen:
                continue
            seen.add(aid)
            try:
                page = cached(cache, urllib.parse.quote(a["fixed"], safe="") + ".html",
                              BASE + "ArchetypeDisplay.aspx?FixedName=" + urllib.parse.quote(a["fixed"]))
                source, text = archetype_page(page)
            except Exception as e:  # stránka chybí – archetyp zůstane bez textu
                failed.append(f"{a['fixed']}: {e}")
                source, text = "", ""
            entry = {
                "id": aid, "classId": cid, "name": a["name"], "replaces": a["replaces"],
                "summary": a["summary"], "pfs": a["pfs"], "source": source, "text": text,
            }
            spells = added_spells(aid, text, names)
            if spells:
                entry["spells"] = spells
            out.append(entry)
        print(f"{cname}: {len(seen)}", flush=True)

    OUT_JSON.write_text(json.dumps(out, ensure_ascii=False, indent=0, separators=(",", ":")) + "\n", encoding="utf-8")
    print(f"{OUT_JSON.relative_to(ROOT)}: {len(out)} archetypes, {sum(1 for a in out if a.get('spells'))} with added spells")
    print("classes without archetypes:", empty)
    if failed:
        print("failed pages:", *failed, sep="\n  ")


if __name__ == "__main__":
    main()
