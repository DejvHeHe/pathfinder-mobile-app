#!/usr/bin/env python3
"""
Racial traits from Archives of Nethys (OGL / Paizo Community Use Policy):

1. composeApp/src/commonMain/kotlin/cz/pfspells/data/RacialTraitTable.kt – default racial traits of every race
   (section "<Race> Racial Traits" of the race page) for feat prerequisites ("stonecunning racial trait").
2. composeApp/src/commonMain/composeResources/files/RacialTraits.json – alternate racial traits with the traits they
   replace, source, PFS flag and text (section "<Race> Alternate Racial Traits", groups "Replaces X, Y").

Race list: https://aonprd.com/Races.aspx?Category=Core and ...=NonCore, race pages RacesDisplay.aspx (~80 requests,
1 per second, cached in build/race-cache). Race ids follow the app (RaceTable.kt from Foundry, slug of the name);
"Lashunta" belongs to both "lashunta-female" and "lashunta-male".

Usage:  python tools/build_racial_traits.py [--cache DIR]
        (when Python cannot verify the certificate, download the pages with curl into the cache directory:
         "<url-encoded race name>.html" and "list-Core.html" / "list-NonCore.html")
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
OUT_KT = ROOT / "composeApp/src/commonMain/kotlin/cz/pfspells/data/RacialTraitTable.kt"
OUT_JSON = ROOT / "composeApp/src/commonMain/composeResources/files/RacialTraits.json"
RACE_TABLE = ROOT / "composeApp/src/commonMain/kotlin/cz/pfspells/data/RaceTable.kt"
BASE = "https://aonprd.com/"
# AoN jméno → id ras v appce, kde se slug neshoduje
EXTRA_IDS = {"lashunta": ["lashunta-female", "lashunta-male"]}


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


def kstr(s: str) -> str:
    return '"' + s.replace("\\", "\\\\").replace('"', '\\"').replace("$", "\\$") + '"'


def plain(h: str) -> str:
    t = re.sub(r"<\s*br\s*/?>", "\n", h, flags=re.I)
    t = re.sub(r"<\s*/?(p|li|ul|table|tr|h\d)[^>]*>", "\n", t, flags=re.I)
    t = re.sub(r"<[^>]+>", "", t)
    t = html.unescape(t).replace(" ", " ")
    lines = [re.sub(r"[ \t]+", " ", x).strip() for x in t.split("\n")]
    return "\n".join(x for x in lines if x).strip()


def trait_name(raw: str) -> str:
    """„Keen Scent (2 RP)“ → „Keen Scent“."""
    return re.sub(r"\s*\(\d+ RP\)\s*$", "", plain(raw)).strip().rstrip(":").strip()


def section(page: str, title_re: str):
    """Text od nadpisu h1 (regex) po další h1."""
    m = re.search(r'<h1 class="title">\s*' + title_re, page)
    if not m:
        return None
    end = page.find('<h1 class="title">', m.end())
    return page[m.end(): end if end >= 0 else len(page)]


def default_traits(page: str, race: str):
    sec = section(page, re.escape(race) + r"\s+Racial Traits\s*</h1>")
    if sec is None:
        return []
    names = []
    for m in re.finditer(r"<b>(.*?)</b>\s*:", sec, re.S):
        # AoN má u některých ras neuzavřené <b> – jeden tučný blok pak obsahuje víc traitů po řádcích
        # („Inspiration (Su) A naiad can…“ / „Languages“)
        for line in plain(m.group(1)).split("\n"):
            n = trait_name(re.split(r"\s+\((?:Ex|Su|Sp)\)", line)[0])
            # řádek atributů („+2 Dexterity, …“) a tabulka „1–2: Chaos Champion …“ (ganzi) nejsou traity
            if not n or re.match(r"^[+\-–−]?\d", n) or len(n) > 40:
                continue
            if n not in names:
                names.append(n)
    return names


ENTRY = re.compile(
    r"<b>\s*(<img[^>]*>)?\s*([^<]+?)\s*</b>\s*<br\s*/?>\s*<b>\s*Source\s*</b>\s*<a[^>]*>\s*<i>([^<]*)</i>\s*</a>\s*(?:,\s*<a[^>]*>\s*<i>[^<]*</i>\s*</a>\s*)*<br\s*/?>",
    re.S,
)


def alternate_traits(page: str, race: str):
    sec = section(page, re.escape(race) + r"\s+Alternate Racial Traits?")
    if sec is None:
        return []
    out = []
    groups = list(re.finditer(r'<h2 class="title">\s*Replaces\s+([^<]+?)\s*</h2>', sec))
    for gi, g in enumerate(groups):
        body = sec[g.end(): groups[gi + 1].start() if gi + 1 < len(groups) else len(sec)]
        replaces = [r.strip().lower() for r in re.split(r"[,;]", html.unescape(g.group(1))) if r.strip()]
        entries = list(ENTRY.finditer(body))
        for ei, e in enumerate(entries):
            text = plain(body[e.end(): entries[ei + 1].start() if ei + 1 < len(entries) else len(body)])
            out.append({
                "name": trait_name(e.group(2)),
                "replaces": replaces,
                "pfs": bool(e.group(1) and "PathfinderSociety" in e.group(1)),
                "source": html.unescape(e.group(3)).strip(),
                "text": text,
            })
    return out


def main():
    args = sys.argv[1:]
    cache = ROOT / "build/race-cache"
    if "--cache" in args:
        cache = Path(args[args.index("--cache") + 1])
    app_ids = set(re.findall(r'RaceInfo\("([^"]+)"', RACE_TABLE.read_text(encoding="utf-8")))

    names = set()
    for cat in ("Core", "NonCore"):
        page = cached(cache, f"list-{cat}.html", BASE + f"Races.aspx?Category={cat}")
        names |= {html.unescape(n) for n in re.findall(r'RacesDisplay\.aspx\?ItemName=([^"&]+)"', page)}

    defaults, alternates, skipped = {}, [], []
    for name in sorted(names):
        ids = [i for i in EXTRA_IDS.get(slug(name), [slug(name)]) if i in app_ids]
        if not ids:
            skipped.append(name)
            continue
        page = cached(cache, urllib.parse.quote(name) + ".html", BASE + "RacesDisplay.aspx?ItemName=" + urllib.parse.quote(name))
        d = default_traits(page, name)
        for i in ids:
            defaults[i] = d
        seen = set()
        for a in alternate_traits(page, name):
            tid = slug(name) + ":" + slug(a["name"])
            if tid in seen:  # stejný trait ve dvou skupinách „Replaces“ (varianty) – nechá se první
                continue
            seen.add(tid)
            alternates.append({"id": tid, "races": ids, **a})

    lines = [
        "// GENERATED by tools/build_racial_traits.py from Archives of Nethys (race pages, OGL / Paizo CUP) – do not edit by hand.",
        "package cz.pfspells.data",
        "",
        "/** Výchozí rasové traity podle id rasy (jména jako na AoN). */",
        "internal val RACE_DEFAULT_TRAITS: Map<String, List<String>> = mapOf(",
    ]
    for rid in sorted(defaults):
        lines.append(f"    {kstr(rid)} to listOf({', '.join(kstr(n) for n in defaults[rid])}),")
    lines.append(")")
    OUT_KT.write_text("\n".join(lines) + "\n", encoding="utf-8")
    OUT_JSON.write_text(json.dumps(alternates, ensure_ascii=False, indent=0, separators=(",", ":")) + "\n", encoding="utf-8")
    print(f"{OUT_KT.relative_to(ROOT)}: default traits of {len(defaults)} races")
    print(f"{OUT_JSON.relative_to(ROOT)}: {len(alternates)} alternate traits")
    print("races without a page in the app:", skipped, "| app races without AoN page:", sorted(app_ids - set(defaults)))


if __name__ == "__main__":
    main()
