#!/usr/bin/env python3
"""
Builds composeApp/src/commonMain/composeResources/files/Feats.json – all Pathfinder 1e feats from Archives of Nethys.

Source: https://aonprd.com/Feats.aspx (list: name, prerequisites, short description, PFS legality)
        https://aonprd.com/FeatDisplay.aspx?ItemName=<name> (types, source, full text)
Content is OGL / Paizo Community Use Policy (see NOTICE.md).

The detail pages are fetched politely (1 request / s, ~3 500 requests ≈ 1 hour) and cached in build/feat-cache,
so an interrupted run continues where it stopped. Run only when the bundled data should be refreshed.

Usage:  python tools/build_feats.py [--delay 1.0]
"""
import argparse
import hashlib
import html
import json
import re
import sys
import time
import urllib.parse
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "composeApp/src/commonMain/composeResources/files/Feats.json"
CACHE = ROOT / "build/feat-cache"
BASE = "https://aonprd.com/"


def fetch(url: str, delay: float) -> str:
    CACHE.mkdir(parents=True, exist_ok=True)
    f = CACHE / (hashlib.sha1(url.encode()).hexdigest() + ".html")
    if f.exists():
        return f.read_text(encoding="utf-8")
    for attempt in range(4):
        try:
            req = urllib.request.Request(url, headers={"User-Agent": "pf-spellbook-build"})
            with urllib.request.urlopen(req, timeout=60) as r:
                text = r.read().decode("utf-8")
            break
        except Exception as e:  # síť / 5xx – počkat a zkusit znovu
            if attempt == 3:
                raise
            print(f"  retry {url}: {e}", file=sys.stderr)
            time.sleep(10 * (attempt + 1))
    f.write_text(text, encoding="utf-8")
    time.sleep(delay)
    return text


def clean(fragment: str) -> str:
    """HTML → text: <br> = nový řádek, tagy pryč, entity, sjednocené mezery."""
    t = re.sub(r"<br\s*/?>", "\n", fragment, flags=re.I)
    t = re.sub(r"</(p|li|tr|h\d)>", "\n", t, flags=re.I)
    t = re.sub(r"<[^>]+>", "", t)
    t = html.unescape(t).replace("\xa0", " ")
    t = re.sub(r"[ \t]+", " ", t)
    t = re.sub(r" *\n *", "\n", t)
    t = re.sub(r"\n{3,}", "\n\n", t)
    return t.strip()


def strip_marks(name: str) -> str:
    """AoN připojuje ke jménům značky (* mythic verze, ⊤ combat trick, † …) – do jména nepatří."""
    return re.sub(r"[*⊤†]", "", name).strip()


def slug(name: str) -> str:
    return re.sub(r"[^a-z0-9]+", "-", name.lower()).strip("-")


def parse_list(page: str):
    """Řádky tabulky Feats.aspx: (ItemName, jméno, prerekvizity, popis, pfs)."""
    rows = []
    for tr in re.findall(r"<tr[^>]*>(.*?)</tr>", page, re.S):
        tds = re.findall(r"<td[^>]*>(.*?)</td>", tr, re.S)
        if len(tds) < 3:
            continue
        m = re.search(r'href="FeatDisplay\.aspx\?ItemName=([^"]+)"', tds[0])
        if not m:
            continue
        rows.append({
            "item": html.unescape(m.group(1)),
            "name": strip_marks(clean(tds[0])),
            "prereq": clean(tds[1]),
            "summary": clean(tds[2]),
            "pfs": 'title="PFS Legal"' in tds[0],
        })
    return rows


def parse_detail(page: str):
    """Všechny bloky <h1 class="title"> stránky (stejné jméno může mít víc featů). Mythic / combat trick (h2) se přeskočí."""
    feats = []
    for m in re.finditer(r'<h1 class="title">(.*?)</h1>(.*?)(?=<h1 class="title">|<h2 class="title">|</span>\s*</td>)', page, re.S):
        title = strip_marks(clean(m.group(1)))
        body = m.group(2)
        tm = re.match(r"(.*?)\s*\(([^()]*)\)\s*$", title)
        name, types = (tm.group(1), [t.strip() for t in tm.group(2).split(",") if t.strip()]) if tm else (title, [])
        source = None
        sm = re.search(r"<b>Source</b>\s*(.*?)<br", body, re.S)
        if sm:
            source = clean(sm.group(1))
            body = body[sm.end() - 3:]
        # sekce „<b>Label</b>: text“; text před první sekcí = flavor
        parts = re.split(r"<b>([^<]{2,40})</b>\s*:?", body)
        flavor = clean(parts[0])
        sections = []
        for i in range(1, len(parts) - 1, 2):
            label = clean(parts[i]).rstrip(":")
            text = clean(parts[i + 1]).lstrip(": ").strip()
            if text:
                sections.append([label, text])
        feats.append({"name": name, "types": types, "source": source, "flavor": flavor, "sections": sections})
    return feats


def main():
    # Windows konzole (cp1250) neumí všechny znaky ze jmen featů
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    sys.stderr.reconfigure(encoding="utf-8", errors="replace")
    ap = argparse.ArgumentParser()
    ap.add_argument("--delay", type=float, default=1.0)
    args = ap.parse_args()

    rows = parse_list(fetch(BASE + "Feats.aspx", args.delay))
    seen, unique = set(), []
    for r in rows:
        if r["item"] not in seen:
            seen.add(r["item"])
            unique.append(r)
    print(f"list: {len(rows)} rows, {len(unique)} unique feats")

    out, ids = [], set()
    for n, r in enumerate(unique, 1):
        url = BASE + "FeatDisplay.aspx?ItemName=" + urllib.parse.quote(r["item"])
        try:
            details = parse_detail(fetch(url, args.delay))
        except Exception as e:
            print(f"  FAILED {r['item']}: {e}", file=sys.stderr)
            details = []
        # stejné jméno → víc featů (různé zdroje); jinak aspoň data ze seznamu
        details = [d for d in details if d["name"].lower() == r["name"].lower()] or details[:1] or [None]
        for d in details:
            base = slug(r["name"])
            fid, k = base, 2
            while fid in ids:
                fid, k = f"{base}-{k}", k + 1
            ids.add(fid)
            sections = (d or {}).get("sections", [])
            prereq = next((t for lbl, t in sections if lbl.lower().startswith("prerequisite")), None) or r["prereq"]
            out.append({
                "id": fid,
                "name": r["name"],
                "types": (d or {}).get("types", []),
                "prereq": prereq.rstrip("."),
                "summary": r["summary"],
                "pfs": r["pfs"],
                "source": (d or {}).get("source"),
                "flavor": (d or {}).get("flavor") or None,
                "sections": [s for s in sections if not s[0].lower().startswith("prerequisite")],
            })
        if n % 100 == 0:
            print(f"  {n}/{len(unique)}")

    out.sort(key=lambda f: f["name"].lower())
    OUT.write_text(json.dumps(out, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
    print(f"{OUT.relative_to(ROOT)}: {len(out)} feats, {OUT.stat().st_size // 1024} KB")


if __name__ == "__main__":
    main()
