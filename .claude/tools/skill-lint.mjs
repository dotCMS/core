#!/usr/bin/env node
// Validates first-party skills. Exit 0 = pass, 1 = fail.
// Runs in CI (cicd_pr_skill-lint.yml) and locally via `just skills-lint`.
import { readFileSync } from 'node:fs';
import { CATALOG_PATH, listSkills, loadConfig, isGrandfathered, buildCatalog, skillNameRegex } from './skill-lib.mjs';

const cfg = loadConfig();
const skills = listSkills().filter((s) => s.firstParty);
const errors = [];
const warnings = [];

const prefix = cfg.vendorPrefix;
const domainRe = skillNameRegex(cfg);

// --- Per-skill checks ---------------------------------------------------
// Grandfathered (legacy) skills are exempt: their issues become warnings (a
// punch-list for the rename PR), not hard failures. New dot-* skills fail hard.
for (const s of skills) {
  const fm = s.fm;
  const name = fm.name || s.dir;
  const legacy = isGrandfathered(cfg, name) || isGrandfathered(cfg, s.dir);
  const issues = [];

  // name/description are the reserved Claude fields — required.
  if (!fm.name) issues.push(`[${s.dir}] missing required 'name'`);
  if (!fm.description) issues.push(`[${s.dir}] missing required 'description'`);
  if (fm.name && fm.name !== s.dir) issues.push(`[${s.dir}] frontmatter name '${fm.name}' must match directory name`);

  if (!legacy) {
    // Naming: dot-<approved-domain>-<action>
    if (!name.startsWith(prefix)) {
      issues.push(`[${s.dir}] name must start with '${prefix}' (or be added to grandfathered list)`);
    } else if (!domainRe.test(name)) {
      issues.push(`[${s.dir}] name '${name}' must match ${prefix}<domain>-<action> where domain ∈ {${cfg.approvedDomains.join(', ')}}`);
    }
    // Required governance fields
    if (!fm.owner) issues.push(`[${s.dir}] missing required 'owner'`);
    if (!fm.status) issues.push(`[${s.dir}] missing required 'status'`);
    else if (!cfg.validStatuses.includes(fm.status)) issues.push(`[${s.dir}] status '${fm.status}' must be one of {${cfg.validStatuses.join(', ')}}`);
  }

  for (const msg of issues) (legacy ? warnings : errors).push(legacy ? `${msg} (legacy — fix in rename PR)` : msg);
}

// --- Cross-skill: supersedes <-> superseded-by consistency --------------
const byName = new Map(skills.map((s) => [s.fm.name || s.dir, s.fm]));
for (const s of skills) {
  const fm = s.fm;
  if (fm.supersedes) {
    const target = byName.get(fm.supersedes);
    if (!target) errors.push(`[${s.dir}] supersedes '${fm.supersedes}' which does not exist`);
    else if (target.status !== 'superseded') errors.push(`[${s.dir}] supersedes '${fm.supersedes}' but that skill's status is '${target.status || '—'}' (must be 'superseded')`);
    else if (target['superseded-by'] !== (fm.name || s.dir)) errors.push(`[${fm.supersedes}] must set 'superseded-by: ${fm.name || s.dir}' to match`);
  }
  // Reverse direction: a skill declaring it was superseded must point at an
  // existing replacer that supersedes it back — otherwise retirement is half-done.
  if (fm['superseded-by']) {
    const replacer = byName.get(fm['superseded-by']);
    if (!replacer) errors.push(`[${s.dir}] superseded-by '${fm['superseded-by']}' which does not exist`);
    else if (replacer.supersedes !== (fm.name || s.dir)) errors.push(`[${s.dir}] superseded-by '${fm['superseded-by']}' but that skill does not declare 'supersedes: ${fm.name || s.dir}'`);
  }
}

// --- Duplicate similarity (soft Flag: warns, does not fail) --------------
const tokenize = (s) => new Set((s || '').toLowerCase().match(/[a-z]{4,}/g) || []);
const active = skills.filter((s) => !(isGrandfathered(cfg, s.fm.name || s.dir) || isGrandfathered(cfg, s.dir)));
for (let i = 0; i < active.length; i++) {
  for (let j = i + 1; j < active.length; j++) {
    const a = tokenize(active[i].fm.description), b = tokenize(active[j].fm.description);
    const inter = [...a].filter((t) => b.has(t)).length;
    const jaccard = inter / (a.size + b.size - inter || 1);
    if (jaccard > 0.4) warnings.push(`possible overlap: '${active[i].dir}' ~ '${active[j].dir}' (${(jaccard * 100).toFixed(0)}% description similarity) — reviewer must confirm not a duplicate`);
  }
}

// --- Catalog freshness: compare committed file to freshly-built content --
// buildCatalog() is pure (no write), so linting never mutates CATALOG.md on disk.
function safeRead(p) { try { return readFileSync(p, 'utf8'); } catch { return null; } }
if (safeRead(CATALOG_PATH) !== buildCatalog().content) {
  errors.push(`CATALOG.md is stale — run 'just skills-catalog' and commit the result`);
}

// --- Report -------------------------------------------------------------
for (const w of warnings) console.log(`⚠️  ${w}`);
if (errors.length) {
  console.error(`\n❌ skill-lint failed (${errors.length} error${errors.length > 1 ? 's' : ''}):`);
  for (const e of errors) console.error(`   • ${e}`);
  process.exit(1);
}
console.log(`\n✅ skill-lint passed — ${skills.length} first-party skill(s), ${warnings.length} warning(s).`);
