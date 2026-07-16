/**
 * Types for the gather-release-data script.
 */

/** A single change (PR) in the release. */
export interface Change {
  pr: number;
  title: string;
  labels: string[];
  category: 'feature' | 'fix' | 'deprecation' | 'infrastructure' | 'internal' | 'uncategorized';
  body: string;
  linkedIssues: number[];
}

/** A PR flagged as unsafe to rollback. */
export interface RollbackUnsafe {
  pr: number;
  title: string;
  labels: string[];
}

/** The full structured output consumed by the Claude prompt. */
export interface ReleaseData {
  repo: string;
  fromTag: string;
  toTag: string;
  totalCommits: number;
  rollbackUnsafe: RollbackUnsafe[];
  skipped: number[];
  changes: Change[];
}

/** Raw commit from the GitHub Compare API. */
export interface CommitInfo {
  sha: string;
  message: string;
}

/** PR details fetched from GitHub. */
export interface PRDetails {
  number: number;
  title: string;
  labels: string[];
  body: string;
  linkedIssues: number[];
}

/** CLI arguments parsed from process.argv. */
export interface CLIArgs {
  repo: string;
  fromTag?: string;
  toTag: string;
}
