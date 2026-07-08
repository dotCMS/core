#!/usr/bin/env node
// Staleness report (Flag layer): surfaces first-party skills that look
// neglected, so their owner can act. Never changes files or fails CI — it
// only reports. Uses git history + files only (no Project board / no extra
// token scopes). Run locally via `just skills-staleness`.
//
// Signals (honest proxies — a human owner still judges):
//   • stuck: status=experimental and no commit to the skill dir in >N days
//   • dead-ref: SKILL.md points at a skill-local references/ or scripts/
//     file that no longer exists
import { existsSync, readFileSync, appendFileSync } from 'node:fs';
import { join } from 'node:path';
import { execSync } from 'node:child_process';
import { REPO_ROOT, SKILLS_DIR, listSkills, loadConfig, isGrandfathered } from './skill-lib.mjs';

const THRESHOLD_DAYS = parseInt(process.env.STALE_DAYS || '60', 10);
const cfg = loadConfig();
const now = new Date();

function lastCommitDaysAgo(dir) {
  try {
    const iso = execSync(`git log -1 --format=%cI -- "${join('.claude/skills', dir)}"`, { cwd: REPO_ROOT, encoding: 'utf8' }).trim();
    if (!iso) return null;
    return Math.floor((now - new Date(iso)) / 86_400_000);
  } catch { return null; }
}

function deadRefs(skillDir, text) {
  const refs = new Set();
  for (const m of text.matchAll(/(?:\]\(|`)((?:references|scripts)\/[\w./-]+)/g)) refs.add(m[1]);
  return [...refs].filter((r) => !existsSync(join(SKILLS_DIR, skillDir, r)));
}

const flagged = [];
for (const s of listSkills()) {
  if (!s.firstParty || isGrandfathered(cfg, s.dir)) continue; // legacy handled by the rename PR, not nagged here
  const reasons = [];
  const age = lastCommitDaysAgo(s.dir);
  if (s.fm.status === 'experimental' && age != null && age > THRESHOLD_DAYS) {
    reasons.push(`stuck in \`experimental\` — no commit in ${age} days (threshold ${THRESHOLD_DAYS})`);
  }
  const dead = deadRefs(s.dir, readFileSync(s.path, 'utf8'));
  if (dead.length) reasons.push(`dead reference(s): ${dead.map((d) => `\`${d}\``).join(', ')}`);
  if (reasons.length) flagged.push({ dir: s.dir, owner: s.fm.owner || '—', reasons });
}

// --- Build markdown report grouped by owner -----------------------------
const lines = ['<!-- skill-staleness-report -->', '## 🧹 Skill staleness report', ''];
if (!flagged.length) {
  lines.push('No stale skills — every first-party skill is fresh. ✅');
} else {
  lines.push(`${flagged.length} skill(s) need an owner's attention. These are **proxies**, not proof — promote, retire, or reconfirm.`, '');
  const byOwner = new Map();
  for (const f of flagged) { if (!byOwner.has(f.owner)) byOwner.set(f.owner, []); byOwner.get(f.owner).push(f); }
  for (const [owner, items] of byOwner) {
    lines.push(`### Owner: ${owner}`);
    for (const it of items) { lines.push(`- **\`${it.dir}\`**`); for (const r of it.reasons) lines.push(`  - ${r}`); }
    lines.push('');
  }
}
const report = lines.join('\n');
console.log(report);

// --- CI outputs ---------------------------------------------------------
if (process.env.GITHUB_OUTPUT) {
  appendFileSync(process.env.GITHUB_OUTPUT, `has_results=${flagged.length > 0}\ncount=${flagged.length}\n`);
}
if (process.env.REPORT_FILE) appendFileSync(process.env.REPORT_FILE, report);
