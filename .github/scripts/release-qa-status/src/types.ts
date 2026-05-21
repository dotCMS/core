/**
 * Types for the release-qa-status script.
 */

/** Possible QA verdicts for a PR's linked-issue set. */
export type QAStatus =
  | 'passed' // every same-repo linked issue is Passed or Not Needed
  | 'failed' // at least one same-repo linked issue is Failed
  | 'missing' // linked issues exist but none of them carry a recognized QA label
  | 'unlinked' // no closing-issue references at all (body keywords or Development panel)
  | 'external' // only cross-repo closing references — QA cannot be verified from here
  | 'excluded'; // bot / version-bump / release machinery — outside QA scope

/** Reason a PR was excluded from QA evaluation. */
export type ExclusionReason =
  | 'bot-author'
  | 'dependency-bump'
  | 'version-bump'
  | 'release-machinery';

/** Label resolution for a single linked issue. */
export interface LinkedIssueInfo {
  number: number;
  url: string;
  /** True when `#N` actually points to a PR, not an issue. We ignore those. */
  isPullRequest: boolean;
  /** True when the API returned 404 — issue may be in another repo or deleted. */
  notFound: boolean;
  labels: string[];
  /** Per-issue verdict (helps callers explain the aggregate). */
  verdict: 'passed' | 'not-needed' | 'failed' | 'none' | 'ignored';
}

/** A cross-repo closing reference (e.g. dotCMS/private-issues#589). */
export interface ExternalRef {
  repo: string; // "owner/name"
  number: number;
}

/** Raw PR details fetched from GitHub. */
export interface PRDetails {
  number: number;
  title: string;
  url: string;
  author: string;
  authorType: string;
  labels: string[];
  /** Same-repo closing-issue references (from body keywords + Development panel). */
  linkedIssues: number[];
  /** Cross-repo closing-issue references. */
  externalRefs: ExternalRef[];
}

/** A single PR with its computed QA result. */
export interface PRQAResult {
  pr: number;
  title: string;
  url: string;
  author: string;
  labels: string[];
  status: QAStatus;
  /** Present when status === 'excluded'. */
  exclusionReason?: ExclusionReason;
  /** Resolved info for each same-repo linked issue (empty if unlinked or excluded). */
  linkedIssues: LinkedIssueInfo[];
  /** Cross-repo closing references — surfaced so reviewers can chase them manually. */
  externalRefs: ExternalRef[];
}

/** Summary counts per QA bucket. */
export interface QASummary {
  failed: number;
  missing: number;
  unlinked: number;
  external: number;
  passed: number;
  excluded: number;
}

/** Full structured output. */
export interface ReleaseQAReport {
  repo: string;
  fromTag: string;
  toTag: string;
  totalCommits: number;
  totalPRs: number;
  summary: QASummary;
  failed: PRQAResult[];
  missing: PRQAResult[];
  unlinked: PRQAResult[];
  external: PRQAResult[];
  passed: PRQAResult[];
  excluded: PRQAResult[];
}

/** Raw commit from the GitHub Compare API. */
export interface CommitInfo {
  sha: string;
  message: string;
}

/** CLI arguments parsed from process.argv. */
export interface CLIArgs {
  repo: string;
  fromTag?: string;
  toTag: string;
  format: 'json' | 'text' | 'slack';
}
