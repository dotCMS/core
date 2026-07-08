// Shared helpers for skill governance tooling (catalog generator, linter, scaffolder).
// Dependency-free: Node built-ins only. Node 22+.
import { readFileSync, readdirSync, lstatSync, existsSync } from 'node:fs';
import { join, dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

export const REPO_ROOT = resolve(dirname(fileURLToPath(import.meta.url)), '..', '..');
export const SKILLS_DIR = join(REPO_ROOT, '.claude', 'skills');
export const CONFIG_PATH = join(SKILLS_DIR, 'skills.config.json');
export const CATALOG_PATH = join(SKILLS_DIR, 'CATALOG.md');

export function loadConfig() {
  return JSON.parse(readFileSync(CONFIG_PATH, 'utf8'));
}

// Minimal YAML-frontmatter parser: handles `key: value`, quoted scalars,
// folded/literal block scalars (`>` `|`), and inline arrays `[a, b]`.
// Sufficient for skill frontmatter — not a general YAML implementation.
export function parseFrontmatter(text) {
  const m = text.match(/^---\r?\n([\s\S]*?)\r?\n---/);
  if (!m) return {};
  const lines = m[1].split(/\r?\n/);
  const out = {};
  for (let i = 0; i < lines.length; i++) {
    const line = lines[i];
    const kv = line.match(/^([A-Za-z_][\w-]*):\s?(.*)$/);
    if (!kv) continue;
    const key = kv[1];
    let val = kv[2];

    if (val === '>' || val === '|' || val === '>-' || val === '|-') {
      // Block scalar: consume following more-indented lines.
      const block = [];
      const baseIndent = (lines[i + 1]?.match(/^(\s*)/)?.[1].length) ?? 0;
      while (i + 1 < lines.length && (lines[i + 1].trim() === '' || /^\s/.test(lines[i + 1]))) {
        const next = lines[++i];
        block.push(next.slice(baseIndent));
      }
      out[key] = block.join(val.startsWith('|') ? '\n' : ' ').trim();
    } else if (val.startsWith('[')) {
      out[key] = val.replace(/^\[|\]$/g, '').split(',').map((s) => s.trim().replace(/^['"]|['"]$/g, '')).filter(Boolean);
    } else {
      out[key] = val.trim().replace(/^['"]|['"]$/g, '');
    }
  }
  return out;
}

// Discover skills. Returns objects for every entry under .claude/skills/,
// flagging symlinks (external, not governed here) vs real first-party dirs.
export function listSkills() {
  const entries = readdirSync(SKILLS_DIR, { withFileTypes: true });
  const skills = [];
  for (const e of entries) {
    const full = join(SKILLS_DIR, e.name);
    const isSymlink = lstatSync(full).isSymbolicLink();
    if (!isSymlink && !lstatSync(full).isDirectory()) continue; // skip files (CATALOG.md, config, etc.)
    const skillMd = join(full, 'SKILL.md');
    if (!existsSync(skillMd)) continue;
    const fm = parseFrontmatter(readFileSync(skillMd, 'utf8'));
    skills.push({ dir: e.name, path: skillMd, isSymlink, firstParty: !isSymlink, fm });
  }
  return skills.sort((a, b) => a.dir.localeCompare(b.dir));
}

export function isGrandfathered(cfg, name) {
  return cfg.grandfathered.includes(name);
}
