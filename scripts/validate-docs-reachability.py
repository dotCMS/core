#!/usr/bin/env python3
"""Documentation reachability check.

A doc under docs/ that is not linked from root CLAUDE.md or docs/README.md is
invisible in practice: nobody browsing finds it, and on-demand context loading
never reaches it. This walks the link graph from those two roots and reports
anything it cannot reach.

Reachability is transitive — a doc linked from another reachable doc counts.

Usage:
    python3 scripts/validate-docs-reachability.py           # report
    python3 scripts/validate-docs-reachability.py --strict  # exit 1 on any orphan
    python3 scripts/validate-docs-reachability.py --links   # also report broken links
"""
import os
import re
import sys
from collections import deque

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ROOTS = ["CLAUDE.md", "docs/README.md"]
DOCS_DIR = "docs"

# [text](path) and @docs/path.md imports, which Cursor rules use.
MD_LINK = re.compile(r"\[[^\]]*\]\(([^)\s]+)")
AT_IMPORT = re.compile(r"@(docs/[\w./-]+\.md)")


def resolve(source, target, root_relative=False):
    """Return the repo-relative path a link points at, or None if not a local .md."""
    target = target.split("#")[0].strip()
    if not target or target.startswith(("http://", "https://", "mailto:")):
        return None
    if not target.endswith(".md"):
        return None
    if target.startswith("/"):
        path = target.lstrip("/")
    elif root_relative and target.startswith("docs/"):
        path = target
    else:
        path = os.path.normpath(os.path.join(os.path.dirname(source), target))
    return path


def main():
    strict = "--strict" in sys.argv
    show_links = "--links" in sys.argv
    os.chdir(REPO)

    all_docs = set()
    for dirpath, dirnames, filenames in os.walk(DOCS_DIR):
        dirnames[:] = [d for d in dirnames if d not in (".git", "node_modules")]
        for name in filenames:
            if name.endswith(".md"):
                all_docs.add(os.path.relpath(os.path.join(dirpath, name)))

    reached = {}
    broken = []
    queue = deque()
    for root in ROOTS:
        if not os.path.isfile(root):
            print(f"Root index missing: {root}")
            return 1
        reached[root] = "<root>"
        queue.append(root)

    while queue:
        current = queue.popleft()
        try:
            text = open(current, encoding="utf-8", errors="replace").read()
        except OSError:
            continue
        found = [(m, False) for m in MD_LINK.findall(text)]
        found += [(m, True) for m in AT_IMPORT.findall(text)]
        for target, root_relative in found:
            path = resolve(current, target, root_relative)
            if path is None:
                continue
            if not os.path.isfile(path):
                # @docs/... imports are a prose convention and appear in syntax
                # examples ("use @docs/path/file.md"); only real markdown links
                # are reported as broken.
                if not root_relative:
                    broken.append((current, target))
                continue
            if path not in reached:
                reached[path] = current
                queue.append(path)

    orphans = sorted(all_docs - set(reached))

    print(f"Docs under {DOCS_DIR}/: {len(all_docs)}")
    print(f"Reachable:              {len(all_docs) - len(orphans)}")
    print(f"Unreachable:            {len(orphans)}")

    if orphans:
        print("\nUnreachable from CLAUDE.md or docs/README.md:")
        for path in orphans:
            print(f"  {path}")
        print("\nAdd each to docs/README.md, or delete/merge it — an unlinked doc")
        print("is never loaded and silently goes stale.")

    if show_links and broken:
        print(f"\nBroken links ({len(broken)}):")
        for source, target in sorted(set(broken)):
            print(f"  {source} -> {target}")

    if strict and orphans:
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
