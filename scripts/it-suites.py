#!/usr/bin/env python3
"""Register integration tests in a CI suite, and check that none are left unregistered.

CI runs integration tests only through the aggregator suites listed in .github/test-matrix.yml
(MainSuite*, Junit5Suite1, ...). A test class that is in no suite compiles fine and never runs.

  scripts/it-suites.py add <TestClass> <Suite>   insert a class into a suite (just add-it-test)
  scripts/it-suites.py check                     fail on test classes that are in no suite

`add` inserts the new entry next to the entry with the nearest simple class name above it. (The
lists are unsorted, so "first larger name from the top" would send most new classes to the same
gap.) It never appends at the end unless the name sorts last and never re-sorts. Every entry keeps
a trailing comma and the new entry is fully qualified with no import, so two PRs adding different
tests rarely touch the same lines and the merge queue stops bumping them.
"""

import argparse
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
TEST_SRC = Path("dotcms-integration/src/test/java")
MATRIX = Path(".github/test-matrix.yml")
ALLOWLIST = Path("dotcms-integration/unregistered-tests.allowlist")

BLOCK_START = re.compile(r"@(?:Suite\.)?(SuiteClasses|SelectClasses)\s*\(\s*\{")
ENTRY = re.compile(r"^(\s*)([\w.]+)\.class(,?)\s*(?://.*)?$")
IMPORT = re.compile(r"^import\s+([\w.]+)\s*;")
TEST_METHOD = re.compile(r"@(?:Test|ParameterizedTest|TestFactory|RepeatedTest)\b")


def insertion_line(items, name):
    """Line to insert `name` at, given (line, name) items in file order that need not be sorted.

    Goes directly before the item with the smallest name above `name` (the first such in the file
    on ties), or directly after the item with the largest name if none sorts after it. Nothing is
    re-sorted; two new names only share an insertion point when no existing name falls between them.
    """
    # prune: plain string order; switch to casefold only if mixed-case names start misplacing
    above = [(n, i) for i, n in items if n > name]
    if above:
        return min(above)[1]
    return max((n, -i) for i, n in items)[1] * -1 + 1


class Suite:
    """One suite source file: its lines, the @SuiteClasses/@SelectClasses block and its entries."""

    def __init__(self, path):
        self.path = path
        self.name = path.stem
        self.lines = path.read_text().split("\n")
        self.package = next(re.match(r"package\s+([\w.]+);", l).group(1)
                            for l in self.lines if l.startswith("package "))
        self.imports = {m.group(1).rsplit(".", 1)[1]: m.group(1)
                        for m in map(IMPORT.match, self.lines) if m}
        starts = [i for i, l in enumerate(self.lines)
                  if BLOCK_START.search(l) and not l.lstrip().startswith(("*", "//"))]
        if len(starts) != 1:
            raise SystemExit(f"{path}: expected one @SuiteClasses/@SelectClasses block, found {len(starts)}")
        self.start = starts[0]
        self.junit5 = BLOCK_START.search(self.lines[self.start]).group(1) == "SelectClasses"
        end = next(i for i in range(self.start, len(self.lines)) if "})" in self.lines[i])
        self.end = end
        # (line index, name as written, resolved fully qualified name)
        self.entries = []
        for i in range(self.start + 1, end):
            m = ENTRY.match(self.lines[i])
            if m:
                self.entries.append((i, m.group(2), self.resolve(m.group(2))))
            elif ".class" in self.lines[i] and not self.lines[i].lstrip().startswith("//"):
                raise SystemExit(f"{path}:{i + 1}: unrecognised suite entry: {self.lines[i].strip()}")

    def resolve(self, written):
        """Fully qualified name of an entry written as a simple name or an FQN."""
        if "." in written:
            return written
        return self.imports.get(written, f"{self.package}.{written}")

    def fqns(self):
        return {fqn for _, _, fqn in self.entries}

    def add(self, fqn):
        """Insert the fully qualified fqn at its name-derived position.

        The entry is always fully qualified, with no import: a new import would land at the end of
        the (unsorted) import block, a second hotspot where concurrent PRs conflict.
        """
        simple = fqn.rsplit(".", 1)[1]
        at = insertion_line([(i, w.rsplit(".", 1)[-1]) for i, w, _ in self.entries], simple)
        prev = ENTRY.match(self.lines[at - 1])
        if prev and not prev.group(3):  # only the old last entry can lack its comma
            self.lines[at - 1] = self.lines[at - 1].rstrip() + ","
        indent = ENTRY.match(self.lines[self.entries[0][0]]).group(1)
        self.lines.insert(at, f"{indent}{fqn}.class,")

    def write(self):
        self.path.write_text("\n".join(self.lines))


def suites(root):
    """The suites CI runs, per the test matrix (entries without a source file here are skipped)."""
    names = re.findall(r'test_class:\s*"([^"#]+)', (root / MATRIX).read_text())
    found = [next((root / TEST_SRC).rglob(f"{n}.java"), None) for n in names]
    return [Suite(p) for p in found if p]


def class_info(root, name):
    """(fqn, is_junit5) of the test class named by a simple name or FQN."""
    src = root / TEST_SRC
    if "." in name:
        path = src / (name.replace(".", "/") + ".java")
        matches = [path] if path.exists() else []
    else:
        matches = sorted(src.rglob(f"{name}.java"))
    if len(matches) != 1:
        raise SystemExit(f"{name}: " + ("not found under " + str(TEST_SRC) if not matches else
                                        "ambiguous, use the fully qualified name:\n  "
                                        + "\n  ".join(str(m.relative_to(src)) for m in matches)))
    text = matches[0].read_text()
    pkg = re.search(r"^package\s+([\w.]+);", text, re.M).group(1)
    return f"{pkg}.{matches[0].stem}", "org.junit.jupiter" in text


def add(root, name, suite_name):
    fqn, junit5 = class_info(root, name)
    all_suites = suites(root)
    for s in all_suites:
        if fqn in s.fqns():
            raise SystemExit(f"{fqn} is already registered in {s.name}")
    target = next((s for s in all_suites if s.name == suite_name), None)
    if not target:
        raise SystemExit(f"unknown suite '{suite_name}'; CI suites: " + ", ".join(s.name for s in all_suites))
    if junit5 != target.junit5:
        raise SystemExit(f"{fqn} is a JUnit {5 if junit5 else 4} test but {target.name} is a "
                         f"JUnit {5 if target.junit5 else 4} suite; it would silently never run there")
    target.add(fqn)
    target.write()
    print(f"registered {fqn} in {target.name}")


def unregistered(root):
    """Concrete test classes (with @Test methods) that no CI suite lists, minus the allow-list."""
    registered = set().union(*(s.fqns() for s in suites(root)))
    allowed = {l.split("#")[0].strip() for l in (root / ALLOWLIST).read_text().splitlines()}
    missing = []
    for path in sorted((root / TEST_SRC).rglob("*.java")):
        text = path.read_text()
        if (re.search(r"\babstract\s+class\b", text) or not TEST_METHOD.search(text)
                or BLOCK_START.search(text)):  # abstract bases and the suites themselves
            continue
        pkg = re.search(r"^package\s+([\w.]+);", text, re.M)
        fqn = f"{pkg.group(1)}.{path.stem}" if pkg else path.stem
        if fqn not in registered and fqn not in allowed:
            missing.append(fqn)
    return missing


def check(root):
    missing = unregistered(root)
    for fqn in missing:
        print(f"not in any CI suite: {fqn}")
    if missing:
        print(f"\n{len(missing)} integration test class(es) would never run in CI. Register each with\n"
              f"  just add-it-test <TestClass> <Suite>\n"
              f"or, if it is a helper or is run elsewhere, list it in {ALLOWLIST} with a reason.")
        return 1
    print("every integration test class is registered or allow-listed")
    return 0


def main(argv):
    p = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    p.add_argument("--root", type=Path, default=ROOT, help=argparse.SUPPRESS)
    sub = p.add_subparsers(dest="cmd", required=True)
    a = sub.add_parser("add", help="register a test class in a suite")
    a.add_argument("test_class", help="simple or fully qualified class name")
    a.add_argument("suite", nargs="?", help="suite name, e.g. MainSuite2a")
    sub.add_parser("check", help="fail on test classes that are in no suite")
    args = p.parse_args(argv)
    if args.cmd == "check":
        return check(args.root)
    if not args.suite:
        print("Pick a suite: add coverage to the shortest-running compatible one "
              "(docs/testing/INTEGRATION_TESTS.md, 'Registering Tests in a MainSuite').\n"
              "CI suites: " + ", ".join(s.name for s in suites(args.root)), file=sys.stderr)
        return 2
    add(args.root, args.test_class, args.suite)
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
