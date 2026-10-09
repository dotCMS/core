/**
 * GitHub transport shared by gather-release-data and release-qa-status.
 *
 * Both scripts resolve the same thing — the commit range between two release
 * tags, and the merged PRs inside it — and both run in cicd_6-release.yml.
 * This file exists because they used to do it with two verbatim copies of the
 * same code, and every fix to it (#37138, #37201) had to be applied twice.
 *
 * Only plumbing belongs here. Anything shaped by what a caller does with the
 * result — fetchPRDetails, categorization, QA verdicts — stays in that caller.
 */

import { Octokit } from '@octokit/rest';
import { throttling } from '@octokit/plugin-throttling';

/** Raw commit from the GitHub Compare API. */
export interface CommitInfo {
  sha: string;
}

/** A published standard release, and whether its notes were ever written. */
export interface ReleaseRef {
  tag: string;
  /** False when the release body is empty — a cut whose pipeline died before notes. */
  hasNotes: boolean;
}

const STANDARD_RELEASE_PATTERN = /^v\d{2}\.\d{2}\.\d{2}-\d{1,2}$/;

/**
 * Commit and PR lookups are batched, but listStandardReleaseTags still
 * paginates the full REST release list — dozens of requests on a repo with
 * this many CLI and LTS tags. Keep the retry behaviour for that.
 */
const ThrottledOctokit = Octokit.plugin(throttling);

const MAX_RATE_LIMIT_RETRIES = 3;

export function onThrottle(kind: string) {
  return (
    retryAfter: number,
    options: { method?: string; url?: string },
    _octokit: unknown,
    retryCount: number
  ): boolean => {
    const willRetry = retryCount < MAX_RATE_LIMIT_RETRIES;
    process.stderr.write(
      `${kind} rate limit on ${options.method} ${options.url}; ` +
        (willRetry
          ? `retry ${retryCount + 1}/${MAX_RATE_LIMIT_RETRIES} in ${retryAfter}s\n`
          : `giving up after ${MAX_RATE_LIMIT_RETRIES} retries\n`)
    );
    return willRetry;
  };
}

/** Create an authenticated, rate-limit-aware Octokit instance. */
export function createOctokit(): Octokit {
  const token = process.env.GITHUB_TOKEN || process.env.GH_TOKEN;
  if (!token) {
    throw new Error(
      'GitHub token required. Set GITHUB_TOKEN or GH_TOKEN environment variable.'
    );
  }
  return new ThrottledOctokit({
    auth: token,
    throttle: {
      onRateLimit: onThrottle('Primary'),
      onSecondaryRateLimit: onThrottle('Secondary'),
    },
  });
}

/** Parse "owner/repo" into { owner, repo }. */
export function parseRepo(fullRepo: string): { owner: string; repo: string } {
  const [owner, repo] = fullRepo.split('/');
  if (!owner || !repo) {
    throw new Error(`Invalid repo format: "${fullRepo}". Expected "owner/repo".`);
  }
  return { owner, repo };
}

/**
 * List standard releases (matching vYY.MM.DD-NN pattern), newest first.
 *
 * Drafts are skipped: listReleases returns them first regardless of date, so they
 * corrupt the ordering this function promises, and a draft may duplicate a real tag.
 */
export async function listStandardReleaseTags(
  octokit: Octokit,
  owner: string,
  repo: string
): Promise<ReleaseRef[]> {
  const releases: ReleaseRef[] = [];
  for await (const response of octokit.paginate.iterator(
    octokit.repos.listReleases,
    { owner, repo, per_page: 100 }
  )) {
    for (const release of response.data) {
      if (release.draft) continue;
      if (STANDARD_RELEASE_PATTERN.test(release.tag_name)) {
        releases.push({
          tag: release.tag_name,
          hasNotes: (release.body ?? '').trim().length > 0,
        });
      }
    }
  }
  return releases;
}

/**
 * Find the previous documented release before `currentTag`.
 *
 * Undocumented releases are skipped. A cut whose pipeline died before writing notes
 * describes none of its commits, so those commits belong in this range — stopping at
 * its tag would strand them. A same-day attempt that DID publish notes is a valid
 * boundary and is returned normally.
 */
export function findPreviousTag(
  releases: ReleaseRef[],
  currentTag: string
): string | undefined {
  const idx = releases.findIndex((r) => r.tag === currentTag);
  if (idx === -1) {
    // Tag not found in release history — caller must handle
    return undefined;
  }
  return releases.slice(idx + 1).find((r) => r.hasNotes)?.tag;
}

/**
 * Fetch the commit range between two tags using the Compare API.
 * Handles pagination for ranges >250 commits.
 */
export async function fetchCommitRange(
  octokit: Octokit,
  owner: string,
  repo: string,
  fromTag: string,
  toTag: string
): Promise<{ totalCommits: number; commits: CommitInfo[] }> {
  // First call to get total count
  const initial = await octokit.repos.compareCommitsWithBasehead({
    owner,
    repo,
    basehead: `${fromTag}...${toTag}`,
    per_page: 1,
  });

  const totalCommits = initial.data.total_commits;

  if (totalCommits === 0) {
    return { totalCommits: 0, commits: [] };
  }

  // Fetch all commits with pagination (250 max per page for compare)
  const commits: CommitInfo[] = [];
  let page = 1;
  const perPage = 250;

  while (commits.length < totalCommits) {
    const response = await octokit.repos.compareCommitsWithBasehead({
      owner,
      repo,
      basehead: `${fromTag}...${toTag}`,
      per_page: perPage,
      page,
    });

    for (const c of response.data.commits) {
      commits.push({ sha: c.sha });
    }

    // If we got fewer than perPage, we've reached the end
    if (response.data.commits.length < perPage) break;
    page++;
  }

  return { totalCommits, commits };
}

/**
 * Resolve merged PRs per commit via GET /repos/{owner}/{repo}/commits/{sha}/pulls.
 * Commit subjects ending in "(#N)" are often an ISSUE number under merge commits,
 * and pulls.get 404s on those; the API also maps a PR's branch commits and its
 * merge commit to the same PR (Set dedupes) and returns [] for direct pushes.
 */
export async function resolvePRNumbers(
  octokit: Octokit,
  owner: string,
  repo: string,
  commits: CommitInfo[]
): Promise<number[]> {
  const prNumbers = new Set<number>();
  const BATCH_SIZE = 15;

  for (let i = 0; i < commits.length; i += BATCH_SIZE) {
    const batch = commits.slice(i, i + BATCH_SIZE);

    const promises = batch.map(async (commit) => {
      try {
        const { data } = await octokit.repos.listPullRequestsAssociatedWithCommit({
          owner,
          repo,
          commit_sha: commit.sha,
          // A default-branch commit resolves to the one merged PR that introduced
          // it, so page 1 always suffices; this just clears the default 30.
          per_page: 100,
        });
        // Only merged PRs: for commits not reachable from the default branch the
        // endpoint also returns open PRs.
        return data.filter((pr) => pr.merged_at).map((pr) => pr.number);
      } catch (error: unknown) {
        const errMsg = error instanceof Error ? error.message : String(error);
        process.stderr.write(
          `Warning: could not resolve PR for commit ${commit.sha}: ${errMsg}\n`
        );
        return [];
      }
    });

    const batchResults = await Promise.all(promises);
    for (const numbers of batchResults) {
      for (const n of numbers) prNumbers.add(n);
    }

    if (i + BATCH_SIZE < commits.length) await sleep(500);
  }

  return Array.from(prNumbers);
}

function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}
