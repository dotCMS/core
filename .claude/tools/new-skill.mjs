#!/usr/bin/env node
// Scaffolds a new dot-<domain>-<action> skill with valid frontmatter,
// after warning about near-matches. Run via `just new-skill`.
// Interactive by default; accepts flags for non-interactive use:
//   node new-skill.mjs --domain issue --action triage --target prs --owner "@dotcms/team" --description "..."
import { mkdirSync, writeFileSync, existsSync } from 'node:fs';
import { join } from 'node:path';
import { execSync } from 'node:child_process';
import { createInterface } from 'node:readline/promises';
import { stdin, stdout } from 'node:process';
import { SKILLS_DIR, listSkills, loadConfig } from './skill-lib.mjs';

const cfg = loadConfig();
const argv = process.argv.slice(2);
const flag = (n) => { const i = argv.indexOf(`--${n}`); return i >= 0 ? argv[i + 1] : undefined; };

const rl = (flag('domain') && flag('action')) ? null : createInterface({ input: stdin, output: stdout });
const ask = async (q, def) => {
  if (!rl) return def;
  const a = (await rl.question(def ? `${q} [${def}]: ` : `${q}: `)).trim();
  return a || def;
};

const tokenize = (s) => new Set((s || '').toLowerCase().match(/[a-z]{4,}/g) || []);

try {
  let domain = flag('domain') || await ask(`Domain (${cfg.approvedDomains.join(' | ')})`);
  while (!cfg.approvedDomains.includes(domain)) {
    if (!rl) { console.error(`Domain must be one of: ${cfg.approvedDomains.join(', ')}`); process.exit(1); }
    domain = await ask(`Not an approved domain. Choose one of ${cfg.approvedDomains.join(' | ')}`);
  }
  const action = flag('action') || await ask('Action (verb, e.g. triage, rollback)');
  const target = flag('target') || await ask('Target (optional noun, blank to skip)', '');
  const name = `${cfg.vendorPrefix}${domain}-${action}${target ? `-${target}` : ''}`.toLowerCase().replace(/\s+/g, '-');

  // Creation-time duplicate check.
  const existing = listSkills().filter((s) => s.firstParty);
  const keywords = tokenize(`${domain} ${action} ${target}`);
  const near = existing
    .map((s) => {
      const t = tokenize(`${s.dir} ${s.fm.description}`);
      const inter = [...keywords].filter((k) => t.has(k)).length;
      return { dir: s.dir, score: inter };
    })
    .filter((x) => x.score > 0)
    .sort((a, b) => b.score - a.score)
    .slice(0, 5);

  if (existing.some((s) => s.dir === name)) { console.error(`\n❌ '${name}' already exists.`); process.exit(1); }
  if (near.length) {
    console.log(`\n⚠️  Possibly-related existing skills — extend one instead of forking?`);
    for (const n of near) console.log(`   • ${n.dir}`);
    if (rl) {
      const go = await ask('Continue creating a new skill anyway? (y/N)', 'n');
      if (!/^y/i.test(go)) { console.log('Aborted.'); process.exit(0); }
    } else {
      console.log('   (non-interactive: proceeding — confirm this is not a duplicate before committing)');
    }
  }

  const owner = flag('owner') || await ask('Owner (person or @team responsible)', '');
  const description = flag('description') || await ask('Description (what it does + when to trigger — be specific and "pushy")', '');
  const status = flag('status') || 'experimental'; // scaffolder default; author may set active in PR if confident

  const dir = join(SKILLS_DIR, name);
  if (existsSync(dir)) { console.error(`\n❌ directory ${dir} exists.`); process.exit(1); }
  mkdirSync(dir, { recursive: true });

  const body = `---
name: ${name}
description: ${description || 'TODO — what it does AND when to trigger. This drives skill matching; be specific and slightly pushy.'}
owner: ${owner || 'TODO'}
status: ${status}
# Optional governance links (dotCMS conventions, not Claude fields):
# related: [dot-other-skill]
# supersedes: dot-old-skill        # and set 'superseded-by: ${name}' on that skill
---

# ${name}

TODO: write the skill instructions. Keep SKILL.md under ~500 lines; move
long reference material into references/ and point to it from here.
`;
  writeFileSync(join(dir, 'SKILL.md'), body);
  console.log(`\n✅ Created ${dir}/SKILL.md (status: ${status}).`);

  execSync('node .claude/tools/gen-skills-catalog.mjs', { stdio: 'inherit' });
  console.log(`\nNext: fill in SKILL.md, then open a PR. skill-lint will validate naming, frontmatter, and catalog freshness.`);
} finally {
  rl?.close();
}
