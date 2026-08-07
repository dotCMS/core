#!/usr/bin/env python3
"""Rebalance dotcms-postman collection groups.

Groups are the CI shard unit. Balances on measured newman time, keeping the
total group count at 9 (was 11): four micro-groups merge, graphql splits in two.
GraphQL is folder-sharded via the new `folders` key rather than by splitting the
518KB collection file.
"""
import os
import json, os, collections

HERE = os.environ.get("BALANCE_WORKDIR", os.path.dirname(os.path.abspath(__file__)))
NGROUPS = 9
OVERHEAD = 9.4 * 60          # measured fixed cost per postman shard (seconds)

raw = json.load(open(f"{HERE}/pm/pm_colls.json"))        # group -> {TEST-<coll>: secs}
gsplit = json.load(open(f"{HERE}/graphql_split.json"))

# ---- flatten to collection -> seconds --------------------------------------
COLL_DIR = os.path.join(os.environ.get("REPO_ROOT", os.getcwd()), "dotcms-postman/src/main/resources/postman")
on_disk = {f[:-5] for f in os.listdir(COLL_DIR)
           if f.endswith(".json") and f != "postman_environment.json"}

cost = {}
skipped = []
for g, d in raw.items():
    for k, v in d.items():
        name = k[5:] if k.startswith("TEST-") else k         # strip TEST- prefix
        if name not in on_disk:      # e.g. failsafe-summary.xml, a maven artifact
            skipped.append(name)
            continue
        cost[name] = v
print(f"collections on disk: {len(on_disk)}, with timings: {len(cost)}")
print(f"skipped non-collection report files: {sorted(set(skipped))}")
untimed = sorted(on_disk - set(cost))
print(f"on disk but never timed ({len(untimed)}): {untimed}")
for u in untimed:                    # unknown cost -> keep them in `default`
    cost[u] = 0.0

# #36915 (already on this branch) moved ContentTypeResourceTests into `template`.
# Keep that; it is reflected in config.json on disk, not in the measured grouping.

# GraphQLTests becomes two folder-sharded units.
gq = cost.pop("GraphQLTests")
setup_share = 0.16 * 60
units = {k: v for k, v in cost.items()}
units["GraphQLTests@a"] = 14.60 * 60 + setup_share
units["GraphQLTests@b"] = 10.82 * 60 + setup_share

print(f"{len(units)} schedulable units, total {sum(units.values())/60:.1f}m")
big = sorted(units.items(), key=lambda kv: -kv[1])[:6]
print("largest units (these bound the shard floor):")
for k, v in big:
    print(f"   {v/60:6.2f}m  {k}")
print(f"\nfloor = largest unit + overhead = {(big[0][1]+OVERHEAD)/60:.1f}m")

# ---- LPT pack into NGROUPS --------------------------------------------------
bins = [[] for _ in range(NGROUPS)]
load = [0.0] * NGROUPS
for k, v in sorted(units.items(), key=lambda kv: -kv[1]):
    i = load.index(min(load))
    bins[i].append(k)
    load[i] += v

# name each group after its most expensive member, lowercased/short
def gname(members):
    head = max(members, key=lambda m: units[m])
    base = head.split("@")[0].replace(".postman_collection", "")
    base = base.replace("_Resource_Tests", "").replace("_Resource", "").replace("Tests", "")
    base = base.replace("Resource", "").strip("_") or head
    suffix = "-a" if head.endswith("@a") else ("-b" if head.endswith("@b") else "")
    return base.lower().replace("_", "-") + suffix

names = []
for b in bins:
    n = gname(b)
    while n in names:
        n += "2"
    names.append(n)

print(f"\n{'group':<18}{'units':>7}{'test':>9}{'est wall':>10}")
for n, b, l in sorted(zip(names, bins, load), key=lambda x: -x[2]):
    print(f"{n:<18}{len(b):>7}{l/60:>8.1f}m{(l+OVERHEAD)/60:>9.1f}m")
print(f"\nmax est wall {(max(load)+OVERHEAD)/60:.1f}m  (was 37.8m)")

# `default` is computed by index.js as "every collection on disk not listed in
# config.json". It is the safety net that picks up newly added collections, so
# it must stay a real shard. Rather than listing all 95 collections and leaving
# `default` empty, leave ONE balanced bin unlisted - it becomes `default`, stays
# balanced, and still absorbs anything new.
# Prefer the bin that already holds the most previously-unnamed collections, so
# `default` keeps meaning "the misc tail" rather than silently owning a headline
# collection. Never pick a bin holding a folder-sharded unit.
was_default = {
    k[5:] if k.startswith("TEST-") else k
    for g in ("default", "default-split") for k in raw.get(g, {})
}
# Units that must stay explicitly listed: folder-sharded ones (config drives the
# split) and ContentTypeResourceTests (#36915 deliberately placed it by name).
PINNED = ("GraphQLTests@", "ContentTypeResourceTests")
default_idx = max(
    (i for i, b in enumerate(bins)
     if not any(m.startswith(PINNED) for m in b)),
    key=lambda i: sum(1 for m in bins[i] if m in was_default),
)
print(f"\nbin {default_idx} ({names[default_idx]}) left UNLISTED -> becomes the `default` shard")

out = []
for i, (n, b) in enumerate(zip(names, bins)):
    if i == default_idx:
        continue
    entry = {"name": n}
    colls, folders = [], None
    for m in sorted(b, key=lambda m: -units[m]):
        if m.startswith("GraphQLTests@"):
            colls.append("GraphQLTests")
            folders = {"GraphQLTests": [gsplit["setup"]] +
                       (gsplit["a"] if m.endswith("@a") else gsplit["b"])}
        else:
            colls.append(m)
    entry["collections"] = colls
    if folders:
        entry["folders"] = folders
    out.append(entry)

shard_names = [e["name"] for e in out] + ["default"]
json.dump({"groups": out, "shards": shard_names,
           "default_members": sorted(bins[default_idx])},
          open(f"{HERE}/postman_groups.json", "w"), indent=2)
print(f"wrote postman_groups.json: {len(out)} listed groups + default = {len(shard_names)} shards")
