/**
 * Determine whether a PR should be excluded from QA evaluation because it's
 * a bot, dependency bump, release-machinery change, or carries no
 * implementation at all (spec / documentation only).
 */

import { ExclusionReason, PRDetails } from './types';

/**
 * Known bot login prefixes. Matched with startsWith (not includes) so a real
 * user named `github-actions-fan` is not accidentally excluded. The primary
 * bot signal is still authorType === 'Bot' or the `[bot]` suffix; this list
 * is a backstop for accounts that lack both.
 */
const BOT_LOGIN_PREFIXES = [
  'dependabot',
  'renovate',
  'github-actions',
  'mend-for-github',
  'snyk-bot',
];

const DEPENDENCY_LABELS = ['dependencies', 'java dependencies', 'javascript dependencies'];

/** Title patterns that indicate a version/dependency bump (case-insensitive). */
const VERSION_BUMP_PATTERNS: RegExp[] = [
  /^bump\b/i,
  /^chore\(deps\)/i,
  /^chore: bump /i,
  /^build\(deps\)/i,
  /^build: bump /i,
];

/** Title patterns that indicate release-machinery commits. */
const RELEASE_MACHINERY_PATTERNS: RegExp[] = [
  /^update license/i,
  /^release v?\d/i,
  /^\[release\]/i,
  /^merge branch/i,
  /^merge pull request/i,
];

/**
 * Agent/dev tooling that happens to ship as markdown. It reads like
 * documentation but *is* the deliverable — PR #37309 added a whole Claude
 * skill without touching a single non-markdown file — so it stays in QA scope.
 * Checked before the documentation patterns, or `.claude/skills/x/SKILL.md`
 * would match DOC_FILE_PATTERN and drop out of the report.
 */
const IMPLEMENTATION_PREFIXES = ['.claude/', '.agents/', '.cursor/', '.specify/'];
const IMPLEMENTATION_BASENAMES = ['claude.md', 'agents.md'];

/** Spec-Kit feature directories: `specs/<issue>-<slug>/{spec,plan,tasks}.md`, contracts, etc. */
const SPEC_PREFIX = 'specs/';

/** Documentation trees — every file under them is prose, whatever the extension. */
const DOC_PREFIXES = ['docs/'];

/** Loose prose anywhere else in the tree: README.md, CONTRIBUTING.md, a library's *.mdx. */
const DOC_FILE_PATTERN = /\.mdx?$/i;

/**
 * True when a path carries no implementation — nothing QA could exercise.
 * Case-insensitive: paths come straight from the GitHub API and casing varies
 * (`CLAUDE.md`, `SKILL.md`, `README.MD`).
 */
export function isDocumentationPath(path: string): boolean {
  const lower = path.toLowerCase();

  if (IMPLEMENTATION_PREFIXES.some((p) => lower.startsWith(p))) return false;
  const basename = lower.slice(lower.lastIndexOf('/') + 1);
  if (IMPLEMENTATION_BASENAMES.includes(basename)) return false;

  if (lower.startsWith(SPEC_PREFIX)) return true;
  if (DOC_PREFIXES.some((p) => lower.startsWith(p))) return true;
  return DOC_FILE_PATTERN.test(lower);
}

export interface ExclusionResult {
  excluded: boolean;
  reason?: ExclusionReason;
}

export function classifyExclusion(pr: PRDetails): ExclusionResult {
  // 1) Author-based exclusions (any GitHub App / bot account)
  if (pr.authorType === 'Bot') {
    return { excluded: true, reason: 'bot-author' };
  }
  const loginLower = pr.author.toLowerCase();
  if (loginLower.endsWith('[bot]')) {
    return { excluded: true, reason: 'bot-author' };
  }
  if (BOT_LOGIN_PREFIXES.some((s) => loginLower.startsWith(s))) {
    return { excluded: true, reason: 'bot-author' };
  }

  // 2) Dependency-bump label
  const labelsLower = pr.labels.map((l) => l.toLowerCase());
  if (DEPENDENCY_LABELS.some((d) => labelsLower.includes(d))) {
    return { excluded: true, reason: 'dependency-bump' };
  }

  // 3) Title heuristics
  if (VERSION_BUMP_PATTERNS.some((p) => p.test(pr.title))) {
    return { excluded: true, reason: 'version-bump' };
  }
  if (RELEASE_MACHINERY_PATTERNS.some((p) => p.test(pr.title))) {
    return { excluded: true, reason: 'release-machinery' };
  }

  // 4) Path heuristics — last, so a bot-authored spec PR still reads
  //    `bot-author` rather than `spec-only`.
  //
  //    Spec-Kit ships every feature as two PRs, and PR 1 carries spec.md alone.
  //    It has nothing runnable, so it never earns a QA label, and without this
  //    it lands in `missing`/`unlinked` and pages its author on every release.
  //
  //    Fail safe: only exclude on a file list we actually have in full. An
  //    absent list (fetch failed, or too many files to page) leaves the PR in
  //    QA scope — over-reporting beats silently hiding a code change.
  const files = pr.changedFiles;
  if (files && files.length > 0 && files.every(isDocumentationPath)) {
    const allSpecs = files.every((f) => f.toLowerCase().startsWith(SPEC_PREFIX));
    return { excluded: true, reason: allSpecs ? 'spec-only' : 'docs-only' };
  }

  return { excluded: false };
}
