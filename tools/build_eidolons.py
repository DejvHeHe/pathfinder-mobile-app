#!/usr/bin/env python3
"""
Eidolons (summoner and unchained summoner) from d20pfsrd.com (OGL; [3PP] entries skipped):

1. composeApp/src/commonMain/kotlin/cz/pfspells/data/EidolonTable.kt – progression tables (HD, BAB, saves, skills,
   feats, armor bonus, Str/Dex bonus, evolution pool, max attacks, special) for both classes and the base forms
   (size, speed, natural armor, good saves, attack, ability scores, free evolutions).
2. composeApp/src/commonMain/composeResources/files/Eidolons.json – evolutions (id, name, cost, text) and unchained
   eidolon subtypes (id, name, text) – lazy.

Pages (2 requests, cached in build/companion-cache):
  https://www.d20pfsrd.com/classes/base-classes/summoner/eidolons/
  https://www.d20pfsrd.com/classes/unchained-classes/summoner-unchained/eidolons-unchained/

Usage:  python tools/build_eidolons.py [--cache DIR]
"""
import html
import json
import re
import sys
import time
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OUT_KT = ROOT / "composeApp/src/commonMain/kotlin/cz/pfspells/data/EidolonTable.kt"
OUT_JSON = ROOT / "composeApp/src/commonMain/composeResources/files/Eidolons.json"
PAGES = {
    "eidolons.html": "https://www.d20pfsrd.com/classes/base-classes/summoner/eidolons/",
    "eidolons-unchained.html": "https://www.d20pfsrd.com/classes/unchained-classes/summoner-unchained/eidolons-unchained/",
}
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
    t = re.sub(r"<\s*br\s*/?>", "\n", h, flags=re.I)
    t = re.sub(r"<\s*/?(p|li|ul|table|tr|h\d)[^>]*>", "\n", t, flags=re.I)
    t = re.sub(r"<[^>]+>", "", t)
    t = html.unescape(t).replace(" ", " ").replace("�", "'").replace("–", "-").replace("−", "-")
    lines = [re.sub(r"[ \t]+", " ", x).strip() for x in t.split("\n")]
    return "\n".join(x for x in lines if x).strip()


def one_line(h: str) -> str:
    return " ".join(plain(h).split())


def slug(s: str) -> str:
    return re.sub(r"[^a-z0-9]+", "_", s.lower()).strip("_")


def kstr(s: str) -> str:
    s = " ".join(s.split())
    return '"' + s.replace("\\", "\\\\").replace('"', '\\"').replace("$", "\\$") + '"'


def num(s: str) -> int:
    m = re.search(r"[+-]?\d+", s)
    return int(m.group(0)) if m else 0


def progression(page: str):
    table = re.findall(r"<table.*?</table>", page, re.S)[0]
    rows = []
    for r in re.findall(r"<tr.*?</tr>", table, re.S)[1:]:
        c = [one_line(x) for x in re.findall(r"<t[dh].*?</t[dh]>", r, re.S)]
        if len(c) < 12 or not re.match(r"\d", c[0]):
            continue
        rows.append([num(c[0]), num(c[1]), num(c[2]), num(c[3]), num(c[4]), num(c[5]), num(c[6]), num(c[7]), num(c[8]),
                     num(c[9]), num(c[10]), "" if c[11] in ("—", "-") else c[11]])
    assert len(rows) == 20, len(rows)
    return rows


def headings(page: str, level: str):
    return [(m.start(), m.end(), one_line(m.group(1))) for m in re.finditer(r"<h" + level + r"[^>]*>(.*?)</h" + level + ">", page, re.S)]


def between(page: str, start_title: str, end_title: str):
    hs = headings(page, "[23]")
    st = next(e for s, e, t in hs if t == start_title)
    en = next((s for s, e, t in hs if t == end_title and s > st), len(page))
    return page[st:en]


def blocks(sec: str):
    hs = list(re.finditer(r"<h4[^>]*>(.*?)</h4>", sec, re.S))
    for i, h in enumerate(hs):
        yield one_line(h.group(1)), sec[h.end(): hs[i + 1].start() if i + 1 < len(hs) else len(sec)]


def form(name: str, body: str, unchained: bool):
    text = one_line(body)
    st = re.search(r"Starting Statistics:?\s*(.*?)(?=Free Evolutions|$)", text)
    s = st.group(1) if st else text

    def f(label):
        m = re.search(r"\b" + label + r"\s+(.*?)(?=;\s*(?:Size|Speed|AC|Saves|Attacks?|Ability Scores)\b|$)", s)
        return m.group(1).strip().rstrip(".;") if m else ""
    size = f("Size").split()[0].lower() if f("Size") else "medium"
    na = re.search(r"\+(\d+)\s*natural armor", f("AC"))
    saves = f("Saves")
    good = [k for k in ("Fort", "Ref", "Will") if re.search(k + r"\s*\(good\)", saves)]
    ab = f("Ability Scores")
    scores = [num(re.search(a + r"\s+(\d+)", ab).group(1)) if re.search(a + r"\s+(\d+)", ab) else 10 for a in ("Str", "Dex", "Con", "Int", "Wis", "Cha")]
    free = re.search(r"Free Evolutions:?\s*(.*)$", text)
    return {
        "id": ("u_" if unchained else "") + slug(name), "name": name, "unchained": unchained,
        "size": size.upper() if size in SIZES else "MEDIUM", "speed": f("Speed"), "na": int(na.group(1)) if na else 0,
        "good": good, "attack": f("Attacks?"), "scores": scores,
        "free": free.group(1).strip().rstrip(".") if free else "",
    }


def main():
    args = sys.argv[1:]
    cache = ROOT / "build/companion-cache"
    if "--cache" in args:
        cache = Path(args[args.index("--cache") + 1])
    core = cached(cache, "eidolons.html", PAGES["eidolons.html"])
    unch = cached(cache, "eidolons-unchained.html", PAGES["eidolons-unchained.html"])

    prog_core, prog_unch = progression(core), progression(unch)
    forms = [form(n, b, False) for n, b in blocks(between(core, "Eidolon Base Forms", "Evolutions"))]
    forms += [form(n, b, True) for n, b in blocks(between(unch, "Base Forms", "Eidolon Subtypes"))]

    evolutions = []
    evo = between(core, "Evolutions", "Eidolon Models")
    groups = list(re.finditer(r"<h3[^>]*>(.*?)</h3>", evo, re.S))
    for gi, g in enumerate(groups):
        m = re.match(r"(\d)-Point Evolutions", one_line(g.group(1)))
        if not m:
            continue
        cost = int(m.group(1))
        sec = evo[g.end(): groups[gi + 1].start() if gi + 1 < len(groups) else len(evo)]
        for name, body in blocks(sec):
            if "[3PP]" in name:
                continue
            clean = re.sub(r"\s*\((?:Ex|Su|Sp|Ex or Su)\)\*?$", "", name).strip()
            body = re.sub(r'<p class="source">.*?</p>', "", body, flags=re.S)
            evolutions.append({"id": slug(clean), "name": clean, "cost": cost, "text": plain(body)})

    subtypes = [{"id": slug(n), "name": n, "text": plain(b)} for n, b in blocks(between(unch, "Eidolon Subtypes", "Eidolons and Alignment"))]

    def prog(rows):
        return "listOf(\n" + "".join(
            f"        EidolonRow({', '.join(str(x) for x in r[:11])}, {kstr(r[11])}),\n" for r in rows) + "    )"
    lines = [
        "// GENERATED by tools/build_eidolons.py from d20pfsrd.com (Summoner / Unchained Summoner – Eidolons, OGL) – do not edit by hand.",
        "package cz.pfspells.data",
        "",
        "/** Progrese eidolona klasického summonera (úroveň summonera 1–20). */",
        f"internal val EIDOLON_PROGRESSION: List<EidolonRow> = {prog(prog_core)}",
        "",
        "/** Progrese eidolona unchained summonera. */",
        f"internal val EIDOLON_PROGRESSION_UNCHAINED: List<EidolonRow> = {prog(prog_unch)}",
        "",
        "/** Základní formy (klasický summoner, unchained s prefixem „u_“). */",
        "internal val EIDOLON_FORMS: List<EidolonForm> = listOf(",
    ]
    for f in forms:
        lines.append(
            f"    EidolonForm({kstr(f['id'])}, {kstr(f['name'])}, unchained = {'true' if f['unchained'] else 'false'}, "
            f"size = CreatureSize.{f['size']}, speed = {kstr(f['speed'])}, naturalArmor = {f['na']}, "
            f"goodSaves = setOf({', '.join('BonusTarget.' + {'Fort': 'FORT', 'Ref': 'REF', 'Will': 'WILL'}[g] for g in f['good'])}), "
            f"attack = {kstr(f['attack'])}, abilities = Abilities({', '.join(map(str, f['scores']))}), freeEvolutions = {kstr(f['free'])}),"
        )
    lines.append(")")
    lines.append("")
    lines.append("/** Podtypy eidolona unchained summonera (text v Eidolons.json). */")
    lines.append(f"internal val EIDOLON_SUBTYPES: List<String> = listOf({', '.join(kstr(s['name']) for s in subtypes)})")
    OUT_KT.write_text("\n".join(lines) + "\n", encoding="utf-8")
    OUT_JSON.write_text(json.dumps({"evolutions": evolutions, "subtypes": subtypes}, ensure_ascii=False, indent=0, separators=(",", ":")) + "\n", encoding="utf-8")
    print(f"{OUT_KT.relative_to(ROOT)}: {len(forms)} base forms, {len(subtypes)} subtypes; {OUT_JSON.relative_to(ROOT)}: {len(evolutions)} evolutions")


if __name__ == "__main__":
    main()
