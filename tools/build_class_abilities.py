#!/usr/bin/env python3
"""
Class abilities from the Foundry VTT PF1 system (packs/class-abilities, OGL content, GPL-3.0 project):

1. composeApp/src/commonMain/kotlin/cz/pfspells/data/ClassAbilityTable.kt – powers granted by a subclass
   (cleric/druid domains and subdomains, sorcerer bloodlines, wizard schools and elemental schools) with the level
   they are gained at (`system.links.supplements`) and the max-uses formula (`system.uses.maxFormula`).
2. composeApp/src/commonMain/composeResources/files/ClassAbilities.json – selectable abilities with text:
   witch / shaman hexes (hex, major hex, grand hex) and oracle revelations (with their mysteries).

Book names come from the system's source registry (module/registry/sources.mjs).

Usage:  pip install pyyaml  &&  python tools/build_class_abilities.py [class-abilities.zip] [sources.mjs]
        (optional local files, e.g. when Python cannot verify the GitLab certificate – download them with curl:
         https://gitlab.com/api/v4/projects/16774935/repository/archive.zip?sha=master&path=packs/class-abilities
         https://gitlab.com/api/v4/projects/16774935/repository/files/module%2Fregistry%2Fsources.mjs/raw?ref=master)
"""
import html
import io
import json
import re
import sys
import urllib.request
import zipfile
from pathlib import Path

import yaml

ROOT = Path(__file__).resolve().parent.parent
OUT_KT = ROOT / "composeApp/src/commonMain/kotlin/cz/pfspells/data/ClassAbilityTable.kt"
OUT_JSON = ROOT / "composeApp/src/commonMain/composeResources/files/ClassAbilities.json"
PROJECT = "https://gitlab.com/api/v4/projects/16774935/repository"
ZIP = PROJECT + "/archive.zip?sha=master&path=packs/class-abilities"
SOURCES = PROJECT + "/files/module%2Fregistry%2Fsources.mjs/raw?ref=master"

HEX_KINDS = {"Hex": "HEX", "Major Hex": "MAJOR_HEX", "Grand Hex": "GRAND_HEX"}
# minimální úroveň čarodějnice / šamana pro druh hexu
HEX_MIN_LEVEL = {"HEX": 1, "MAJOR_HEX": 10, "GRAND_HEX": 18}


def fetch(url: str) -> bytes:
    req = urllib.request.Request(url, headers={"User-Agent": "pf-spellbook-build"})
    with urllib.request.urlopen(req, timeout=180) as r:
        return r.read()


def slug(name: str) -> str:
    return re.sub(r"[^a-z0-9]+", "-", name.lower()).strip("-")


def kstr(s: str) -> str:
    return '"' + s.replace("\\", "\\\\").replace('"', '\\"').replace("$", "\\$") + '"'


# ---- vzorce Foundry: stejná gramatika jako FoundryFormula v Kotlinu (kontrola, že appka vzorec spočítá) ----
VARS = {"@class.unlevel": 7, "@class.level": 7, "@attributes.hd.total": 7,
        **{f"@abilities.{a}.mod": 2 for a in ("str", "dex", "con", "int", "wis", "cha")}}
FUNCS = {"floor", "ceil", "min", "max", "clamp", "if", "gt", "gte", "lt", "lte", "eq", "abs"}
TOKEN = re.compile(r"\s*(?:(\d+(?:\.\d+)?)|(@[\w.]+)|([a-z]+)\s*(?=\()|(.))")


def formula_ok(f: str) -> bool:
    toks = []
    for num, var, fn, op in TOKEN.findall(f):
        if num:
            toks.append(("n", float(num)))
        elif var:
            if var not in VARS:
                return False
            toks.append(("n", VARS[var]))
        elif fn:
            if fn not in FUNCS:
                return False
            toks.append(("f", fn))
        elif op.strip():
            toks.append(("o", op))
    pos = 0

    def peek():
        return toks[pos] if pos < len(toks) else ("e", None)

    def take(kind=None, val=None):
        nonlocal pos
        t = peek()
        if (kind and t[0] != kind) or (val is not None and t[1] != val):
            raise ValueError(f"expected {val or kind}, got {t}")
        pos += 1
        return t

    def expr():
        v = term()
        while peek() in (("o", "+"), ("o", "-")):
            v = v + term() if take()[1] == "+" else v - term()
        return v

    def term():
        v = unary()
        while peek() in (("o", "*"), ("o", "/")):
            op = take()[1]
            r = unary()
            v = v * r if op == "*" else (v / r if r else 0)
        return v

    def unary():
        if peek() == ("o", "-"):
            take()
            return -unary()
        return atom()

    def atom():
        t = peek()
        if t[0] == "n":
            take()
            return t[1]
        if t[0] == "f":
            take()
            take("o", "(")
            args = [expr()]
            while peek() == ("o", ","):
                take()
                args.append(expr())
            take("o", ")")
            return 1
        take("o", "(")
        v = expr()
        take("o", ")")
        return v

    try:
        expr()
        return pos == len(toks)
    except (ValueError, IndexError):
        return False


# ---- text popisu: HTML Foundry → prostý text ----
def plain(h: str) -> str:
    t = h or ""
    t = re.sub(r"@UUID\[[^\]]*\]\{([^}]*)\}", r"\1", t)
    t = re.sub(r"@(UUID|Embed|Compendium)\[[^\]]*\](\{[^}]*\})?", "", t)
    t = re.sub(r"\[\[/\w+\s+([^\]]*)\]\](\{([^}]*)\})?", lambda m: m.group(3) or m.group(1), t)
    t = re.sub(r"<\s*(br|/p|/li|/h\d|/tr|/div)\s*/?>", "\n", t, flags=re.I)
    t = re.sub(r"<\s*li[^>]*>", "\n- ", t, flags=re.I)
    t = re.sub(r"<[^>]+>", "", t)
    t = html.unescape(t).replace(" ", " ")
    lines = [re.sub(r"[ \t]+", " ", x).strip() for x in t.split("\n")]
    out = "\n".join(x for x in lines if x)
    return out.strip()


def unit_of(uses: dict, text: str):
    """(vzorec, jednotka) – jen denní použití; „per combat“, náboje apod. appka nepočítá."""
    f = str(uses.get("maxFormula") or "").strip()
    if not f:
        # Foundry občas vzorec nemá, i když ho text uvádí (Abyssal Claws)
        m = re.search(r"number of (?:rounds|times) per day equal to (\d+) \+ your (strength|dexterity|constitution|"
                      r"intelligence|wisdom|charisma) modifier", text, re.I)
        if m:
            f, uses = f"{m.group(1)} + @abilities.{m.group(2).lower()[:3]}.mod", {"per": "day"}
    if not f or uses.get("per") != "day":
        return None, None
    if not formula_ok(f):
        print(f"  ! vzorec nejde spočítat: {f}")
        return None, None
    unit = "ROUNDS" if re.search(r"rounds? (per|each|a) day", text, re.I) else "PER_DAY"
    return f, unit


def source_of(sys_: dict, books: dict) -> str:
    for s in sys_.get("sources") or []:
        sid = s.get("id")
        if not sid:
            continue
        name = books.get(sid, sid)
        pages = s.get("pages")
        return f"{name} pg. {pages}" if pages else name
    return ""


def subclass_key(name: str, folder: str):
    """Klíč podtřídy jako v appce: „domain:fire“, „subdomain:archon@good“, „bloodline:draconic“, „school:element:fire“."""
    if folder == "domains" and name.endswith(" Domain"):
        return "domain:" + name[: -len(" Domain")].lower()
    if folder == "domains/subdomains":
        m = re.match(r"(.+) Subdomain(?: \((.+) Domain\))?$", name)
        if m:
            return "subdomain:" + m.group(1).lower() + ("@" + m.group(2).lower() if m.group(2) else "")
    if folder == "sorcerer-bloodlines" and name.endswith(" Bloodline"):
        return "bloodline:" + name[: -len(" Bloodline")].lower()
    if folder == "wizard-schools" and name.endswith(" School"):
        return "school:" + name[: -len(" School")].lower()
    if folder == "wizard-schools/elemental-schools" and name.endswith(" Elemental School"):
        return "school:element:" + name[: -len(" Elemental School")].lower()
    return None


def main():
    data = Path(sys.argv[1]).read_bytes() if len(sys.argv) > 1 else fetch(ZIP)
    src_text = Path(sys.argv[2]).read_text(encoding="utf-8") if len(sys.argv) > 2 else fetch(SOURCES).decode("utf-8")
    books = dict(re.findall(r"\b(PZO\w+):\s*\{\s*name:\s*\"([^\"]+)\"", src_text))

    z = zipfile.ZipFile(io.BytesIO(data))
    items = []
    for n in z.namelist():
        if not n.endswith(".yaml") or n.split("/")[-1].startswith("_"):
            continue
        d = yaml.safe_load(z.read(n))
        if not isinstance(d, dict) or not d.get("system"):
            continue
        parts = n.split("/")
        d["_folder"] = "/".join(parts[parts.index("class-abilities") + 1:-1]) if "class-abilities" in parts else ""
        items.append(d)
    by_id = {d["_id"]: d for d in items if d.get("_id")}

    # 1) schopnosti podtříd
    powers = []
    for d in items:
        key = subclass_key(d["name"], d["_folder"])
        if not key:
            continue
        for sup in (d["system"].get("links") or {}).get("supplements") or []:
            p = by_id.get(str(sup.get("uuid", "")).split(".")[-1])
            if not p:
                continue
            ps = p["system"]
            f, unit = unit_of(ps.get("uses") or {}, plain((ps.get("description") or {}).get("value", "")))
            powers.append((key, int(sup.get("level") or 1), p["name"], f, unit))
    powers.sort(key=lambda r: (r[0], r[1], r[2]))

    lines = [
        "// GENERATED by tools/build_class_abilities.py from Foundry VTT PF1 (packs/class-abilities, OGL) – do not edit by hand.",
        "package cz.pfspells.data",
        "",
        "internal val SUBCLASS_POWER_TABLE: List<SubclassPower> = listOf(",
    ]
    for key, level, name, f, unit in powers:
        extra = f", uses = {kstr(f)}, unit = ResourceUnit.{unit}" if f else ""
        lines.append(f"    SubclassPower({kstr(key)}, {level}, {kstr(name)}{extra}),")
    lines.append(")")
    OUT_KT.write_text("\n".join(lines) + "\n", encoding="utf-8")
    keys = {p[0] for p in powers}
    print(f"{OUT_KT.relative_to(ROOT)}: {len(powers)} powers of {len(keys)} subclasses "
          f"({sum(1 for p in powers if p[3])} with uses)")

    # 2) hexy a revelations
    out, seen = [], set()
    for d in items:
        s = d["system"]
        tags = s.get("tags") or []
        classes = [c.lower() for c in (s.get("associations") or {}).get("classes") or []]
        text = plain((s.get("description") or {}).get("value", ""))
        kind = next((HEX_KINDS[t] for t in tags if t in HEX_KINDS), None)
        if kind and "Shaman Spirit Hex" not in tags:
            classes = [c for c in classes if c in ("witch", "shaman")]
            min_level = HEX_MIN_LEVEL[kind]
            mysteries = []
        elif "Revelation" in tags and "oracle" in classes and not d["name"].endswith("Final Revelation"):
            kind, classes = "REVELATION", ["oracle"]
            mysteries = sorted(t[: -len(" Mystery")].lower() for t in tags if t.endswith(" Mystery"))
            m = re.search(r"must be (?:at least )?(\d+)(?:st|nd|rd|th)[- ]level", text, re.I)
            min_level = int(m.group(1)) if m else 1
        else:
            continue
        if not classes:
            continue
        f, unit = unit_of(s.get("uses") or {}, text)
        aid = slug(d["name"])
        while aid in seen:
            aid += "-2"
        seen.add(aid)
        row = {"id": aid, "name": d["name"], "kind": kind, "classes": classes}
        if mysteries:
            row["mysteries"] = mysteries
        if min_level > 1:
            row["minLevel"] = min_level
        if f:
            row["uses"], row["unit"] = f, unit
        if s.get("abilityType") in ("ex", "su", "sp"):
            row["type"] = s["abilityType"].capitalize()
        row["source"] = source_of(s, books)
        row["text"] = text
        out.append(row)
    out.sort(key=lambda r: (r["kind"], r["name"]))
    OUT_JSON.write_text(json.dumps(out, ensure_ascii=False, indent=0, separators=(",", ":")) + "\n", encoding="utf-8")
    kinds = {}
    for r in out:
        kinds[r["kind"]] = kinds.get(r["kind"], 0) + 1
    print(f"{OUT_JSON.relative_to(ROOT)}: {len(out)} abilities {kinds}")


if __name__ == "__main__":
    main()
