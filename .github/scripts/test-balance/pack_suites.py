#!/usr/bin/env python3
"""Bin-pack the 557 MainSuite integration classes into N balanced suites.

Balances on measured per-class test time (stable run-to-run: 115.2m vs 114.8m
across two runs). Job wall time is NOT used - it is dominated by runner variance.

Front-block classes (the "run FIRST on purpose" full-scan tests from #36911) are
packed normally but emitted at the head of whichever suite they land in.
"""
import os
import json, os, re, collections

HERE = os.environ.get("BALANCE_WORKDIR", os.path.dirname(os.path.abspath(__file__)))
WT = os.environ.get("REPO_ROOT", os.getcwd())
SRC = f"{WT}/dotcms-integration/src/test/java/com/dotcms"
NBINS = 7
NAMES = ["MainSuite1a", "MainSuite1b", "MainSuite2a", "MainSuite2b",
         "MainSuite3a", "MainSuite3b", "MainSuite4a"]

parsed = json.load(open(f"{HERE}/suites_parsed.json"))
times = json.load(open(f"{HERE}/post_classes.json"))          # fqn -> seconds

# ---- build the pool -------------------------------------------------------
pool, front = [], set()
for suite, v in parsed.items():
    front |= set(v["front"])
    pool += v["entries"]
assert len(pool) == len(set(pool)), "duplicate classes in pool"

by_simple = collections.defaultdict(list)
for k in times:
    by_simple[k.split(".")[-1]].append(k)


def cost(fqn):
    if fqn in times:
        return times[fqn]
    cands = by_simple.get(fqn.split(".")[-1])          # package moved / inner class
    return times[cands[0]] if cands else 0.0


missing = [c for c in pool if cost(c) == 0.0]
print(f"pool={len(pool)}  front={len(front)}  zero-cost={len(missing)}")

# ---- LPT bin-pack ---------------------------------------------------------
bins = [[] for _ in range(NBINS)]
load = [0.0] * NBINS
for c in sorted(pool, key=cost, reverse=True):
    i = load.index(min(load))
    bins[i].append(c)
    load[i] += cost(c)

# order within each bin: front-block first, then descending cost
for b in bins:
    b.sort(key=lambda c: (c not in front, -cost(c)))

print(f"\n{'suite':<14}{'classes':>9}{'test-time':>11}{'front':>7}")
for n, b, l in zip(NAMES, bins, load):
    print(f"{n:<14}{len(b):>9}{l/60:>10.1f}m{sum(1 for c in b if c in front):>7}")
print(f"\ntotal {sum(load)/60:.1f}m | perfect {sum(load)/60/NBINS:.1f}m "
      f"| max {max(load)/60:.1f}m | spread {(max(load)-min(load))/60:.1f}m")

# ---- emit -----------------------------------------------------------------
FRONT_NOTE = """
        // Data-scanning tests run FIRST on purpose.
        // Integration tests accumulate content and never clean up, so anything
        // that walks the whole dataset (executeUpgrade, findAll*) costs
        // O(all content created so far). Scheduled late these pay for every
        // preceding test's leftovers. Keep new full-scan tests in this block.
"""

TMPL = """package com.dotcms;

import com.dotcms.junit.MainBaseSuite;
import org.junit.runner.RunWith;
import org.junit.runners.Suite.SuiteClasses;

/**
 * Integration test suite shard {idx} of {n}.
 *
 * Shards are balanced on measured per-class test time so the slowest shard
 * bounds the CI critical path as tightly as possible. When adding a test,
 * put it in the shard with the lowest total time rather than appending here
 * by habit - see .github/test-matrix.yml for the shard list.
 *
 * Classes are fully qualified so that rebalancing does not churn imports.
 */
@RunWith(MainBaseSuite.class)
@SuiteClasses({{
{body}
}})
public class {cls} {{

}}
"""


def emit(cls, idx, classes):
    lines, wrote_note = [], False
    for c in classes:
        if c in front and not wrote_note:
            lines.append(FRONT_NOTE.rstrip("\n"))
            wrote_note = True
        if c not in front and wrote_note:
            lines.append("")
            wrote_note = None                     # close the block once
        lines.append(f"        {c}.class,")
    body = "\n".join(lines).rstrip(",")
    # trailing comma on the final entry is legal in Java annotations, keep it simple
    body = "\n".join(lines)
    if body.rstrip().endswith(","):
        body = body.rstrip()[:-1]
    return TMPL.format(cls=cls, idx=idx, n=NBINS, body=body)


for i, (n, b) in enumerate(zip(NAMES, bins), 1):
    open(f"{SRC}/{n}.java", "w").write(emit(n, i, b))
    print(f"wrote {n}.java")

json.dump({n: b for n, b in zip(NAMES, bins)}, open(f"{HERE}/packed.json", "w"), indent=1)
