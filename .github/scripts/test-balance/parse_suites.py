#!/usr/bin/env python3
"""Parse MainSuite*.java @SuiteClasses lists -> {suite: {'entries': [fqn], 'front': [fqn]}}

Front block = the "run FIRST on purpose" cluster added by #36911; those classes do
full-DB scans and must stay at the head of whatever suite they end up in.
"""
import os, re, os, json, sys

WT = os.environ.get("REPO_ROOT", os.getcwd())
SRC = f"{WT}/dotcms-integration/src/test/java/com/dotcms"
SUITES = ["MainSuite1a", "MainSuite1b", "MainSuite2a", "MainSuite2b", "MainSuite3a"]
FRONT_MARK = re.compile(r"run FIRST on purpose")


def parse(suite):
    s = open(f"{SRC}/{suite}.java").read()
    imports = dict(
        (m.split(".")[-1], m)
        for m in re.findall(r"^import\s+([\w.]+);", s, re.M)
        if not m.startswith("org.junit")
    )
    m = re.search(r"@(?:Suite\.)?SuiteClasses\(\{", s)
    body = s[m.end(): s.index("})", m.end())]

    entries, front = [], []
    in_front = False        # inside the "run FIRST" cluster
    front_done = False
    for raw in body.split("\n"):
        line = raw.strip()
        if not line:
            if in_front:            # blank line closes the front cluster
                in_front, front_done = False, True
            continue
        if line.startswith(("//", "/*", "*")):
            if not front_done and FRONT_MARK.search(line):
                in_front = True
            continue
        mm = re.match(r"([\w.]+)\.class\s*,?", line)
        if not mm:
            continue
        name = mm.group(1)
        fqn = name if "." in name else imports.get(name)
        if fqn is None:
            print(f"  !! UNRESOLVED {suite}: {name}", file=sys.stderr)
            sys.exit(1)
        entries.append(fqn)
        if in_front:
            front.append(fqn)
    return {"entries": entries, "front": front}


if __name__ == "__main__":
    out = {}
    for s in SUITES:
        out[s] = parse(s)
        print(f"{s:<14} {len(out[s]['entries']):>4} classes, front block = {len(out[s]['front'])}")
        for f in out[s]["front"]:
            print(f"                 FRONT {f.split('.')[-1]}")
    allc = [c for v in out.values() for c in v["entries"]]
    print(f"\ntotal {len(allc)}, unique {len(set(allc))}")
    dupes = sorted(c for c in set(allc) if allc.count(c) > 1)
    print("DUPLICATES:", dupes if dupes else "none")
    json.dump(out, open(os.path.dirname(__file__) + "/suites_parsed.json", "w"), indent=1)
