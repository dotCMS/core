/**
 * Determine whether a PR should be excluded from QA evaluation because it's
 * a bot, dependency bump, or release-machinery change.
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

  return { excluded: false };
}
