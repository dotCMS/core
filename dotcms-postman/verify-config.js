#!/usr/bin/env node
/**
 * Sanity-checks dotcms-postman/config.json against what is actually on disk.
 *
 * Catches the failure modes that are otherwise silent in CI:
 *  - a collection listed in a group that has no .json file (group errors out)
 *  - a `folders` entry naming a folder that does not exist in the collection
 *    (newman runs ZERO requests and still exits green)
 *  - a collection claimed by two groups (runs twice, wastes a shard)
 *  - `folders` shards of one collection that do not cover every folder
 *
 * Run: node dotcms-postman/verify-config.js
 */
const fs = require("fs");
const path = require("path");

const DIR = path.join(__dirname, "src/main/resources/postman");
const config = JSON.parse(fs.readFileSync(path.join(__dirname, "config.json")));

const onDisk = new Set(
  fs.readdirSync(DIR)
    .filter((f) => f.endsWith(".json") && f !== "postman_environment.json")
    .map((f) => f.replace(/\.json$/, ""))
);

const errors = [];
const owner = new Map();          // collection -> [group, ...]
const folderShards = new Map();   // collection -> [[folders], ...]

for (const group of config) {
  if (!group.name) errors.push("a group is missing `name`");
  for (const coll of group.collections || []) {
    if (!onDisk.has(coll)) {
      errors.push(`[${group.name}] collection not on disk: ${coll}`);
      continue;
    }
    if (!owner.has(coll)) owner.set(coll, []);
    owner.get(coll).push(group.name);

    const folders = (group.folders || {})[coll];
    if (!folders) continue;

    const doc = JSON.parse(fs.readFileSync(path.join(DIR, `${coll}.json`)));
    const real = new Set(doc.item.filter((i) => i.item).map((i) => i.name));
    for (const f of folders) {
      if (!real.has(f)) {
        errors.push(
          `[${group.name}] ${coll}: folder "${f}" does not exist ` +
          `(newman would run zero requests). Known: ${[...real].join(" | ")}`
        );
      }
    }
    if (!folderShards.has(coll)) folderShards.set(coll, { real, shards: [] });
    folderShards.get(coll).shards.push(folders);
  }
}

// A collection may legitimately appear in >1 group ONLY when each appearance
// pins a disjoint set of folders.
for (const [coll, groups] of owner) {
  if (groups.length === 1) continue;
  const entry = folderShards.get(coll);
  if (!entry || entry.shards.length !== groups.length) {
    errors.push(`${coll} is claimed by ${groups.join(", ")} without folder pinning`);
    continue;
  }
  const counts = new Map();
  for (const s of entry.shards) {
    for (const f of s) counts.set(f, (counts.get(f) || 0) + 1);
  }
  // The shared setup folder is expected in every shard; anything else repeated
  // means real duplicated work.
  const dupes = [...counts].filter(([f, n]) => n > 1 && n !== entry.shards.length);
  if (dupes.length) {
    errors.push(`${coll}: folders in some-but-not-all shards: ${dupes.map(([f]) => f).join(", ")}`);
  }
  const missing = [...entry.real].filter((f) => !counts.has(f));
  if (missing.length) {
    errors.push(`${coll}: folders covered by NO shard: ${missing.join(", ")}`);
  }
}

const listed = new Set(owner.keys());
const fallsToDefault = [...onDisk].filter((c) => !listed.has(c));

console.log(`collections on disk : ${onDisk.size}`);
console.log(`explicitly grouped  : ${listed.size} across ${config.length} groups`);
console.log(`-> "default" shard  : ${fallsToDefault.length}`);
for (const [coll, e] of folderShards) {
  console.log(`folder-sharded      : ${coll} -> ${e.shards.length} shards covering ${e.real.size} folders`);
}

if (errors.length) {
  console.error(`\n${errors.length} PROBLEM(S):`);
  errors.forEach((e) => console.error("  - " + e));
  process.exit(1);
}
console.log("\nconfig.json OK");
