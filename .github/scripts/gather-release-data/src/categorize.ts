/**
 * Categorization logic for PR changes.
 */

import { Change, PRDetails, RollbackUnsafe } from './types';

/** Labels that cause a PR to be omitted entirely from the changelog. */
const SKIP_LABELS = ['Changelog: Skip'];

/** Labels indicating the release is not safe to rollback. */
const ROLLBACK_UNSAFE_LABELS = [
  'AI: Not Safe To Rollback',
  'Human: Not Safe To Rollback',
];

/** Labels that map to the "feature" category. */
const FEATURE_LABELS = ['feature', 'enhancement', 'new feature'];

/** Labels that map to the "fix" category. */
const FIX_LABELS = ['bug', 'fix', 'defect', 'regression'];

/** Labels that map to the "infrastructure" category. */
const INFRA_LABELS = [
  'infrastructure',
  'security',
  'dependencies',
  'ci/cd',
  'Area : CI/CD',
];

/** Labels that map to the "deprecation" category. */
const DEPRECATION_LABELS = ['deprecation', 'breaking change', 'eol'];

/** Check if a PR has any label from the given set (case-insensitive). */
function hasLabel(prLabels: string[], targetLabels: string[]): boolean {
  const normalized = prLabels.map((l) => l.toLowerCase());
  return targetLabels.some((target) => normalized.includes(target.toLowerCase()));
}

/** Check if a PR should be skipped entirely. */
export function shouldSkip(labels: string[]): boolean {
  return hasLabel(labels, SKIP_LABELS);
}

/** Check if a PR is flagged as rollback-unsafe. */
export function isRollbackUnsafe(labels: string[]): boolean {
  return hasLabel(labels, ROLLBACK_UNSAFE_LABELS);
}

/** Determine the changelog category for a PR based on its labels and title. */
export function categorize(pr: PRDetails): Change['category'] {
  // Label-based categorization (first match wins)
  if (hasLabel(pr.labels, DEPRECATION_LABELS)) return 'deprecation';
  if (hasLabel(pr.labels, FEATURE_LABELS)) return 'feature';
  if (hasLabel(pr.labels, FIX_LABELS)) return 'fix';
  if (hasLabel(pr.labels, INFRA_LABELS)) return 'infrastructure';

  // Title-based heuristics (conventional commit prefixes)
  const title = pr.title.toLowerCase();
  if (title.startsWith('feat')) return 'feature';
  if (title.startsWith('fix')) return 'fix';
  if (title.startsWith('chore') || title.startsWith('build') || title.startsWith('ci(') || title.startsWith('ci:')) return 'infrastructure';
  if (title.startsWith('deprecat')) return 'deprecation';

  // Check for release-machinery commits that should be omitted
  if (isReleaseMachinery(pr.title)) return 'internal';

  // No label or title match — let Claude decide the category
  return 'uncategorized';
}

/** Detect release-machinery commits that have no user-facing impact. */
function isReleaseMachinery(title: string): boolean {
  const patterns = [
    /^bump version/i,
    /^update license/i,
    /^release v?\d/i,
    /^merge branch/i,
    /^merge pull request/i,
    /^\[release\]/i,
  ];
  return patterns.some((p) => p.test(title));
}

/**
 * Process all PRs into categorized changes, rollback warnings, and skipped PRs.
 */
export function processChanges(prDetails: Map<number, PRDetails>): {
  changes: Change[];
  rollbackUnsafe: RollbackUnsafe[];
  skipped: number[];
} {
  const changes: Change[] = [];
  const rollbackUnsafe: RollbackUnsafe[] = [];
  const skipped: number[] = [];

  for (const [prNumber, pr] of prDetails) {
    // Check rollback safety — rollback-unsafe PRs are never skipped
    const unsafe = isRollbackUnsafe(pr.labels);
    if (unsafe) {
      rollbackUnsafe.push({
        pr: prNumber,
        title: pr.title,
        labels: pr.labels,
      });
    }

    // Check skip (rollback-unsafe PRs are immune)
    if (!unsafe && shouldSkip(pr.labels)) {
      skipped.push(prNumber);
      continue;
    }

    const category = categorize(pr);

    // Omit internal/release-machinery
    if (category === 'internal') continue;

    changes.push({
      pr: prNumber,
      title: pr.title,
      labels: pr.labels,
      category,
      body: pr.body,
      linkedIssues: pr.linkedIssues,
    });
  }

  return { changes, rollbackUnsafe, skipped };
}
