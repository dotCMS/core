/**
 * GitHub API interactions for gathering release data.
 */

import { Octokit } from '@octokit/rest';
import { CommitInfo, PRDetails } from './types';

const STANDARD_RELEASE_PATTERN = /^v\d{2}\.\d{2}\.\d{2}-\d{1,2}$/;

/** Create an authenticated Octokit instance. */
export function createOctokit(): Octokit {
  const token = process.env.GITHUB_TOKEN || process.env.GH_TOKEN;
  if (!token) {
    throw new Error(
      'GitHub token required. Set GITHUB_TOKEN or GH_TOKEN environment variable.'
    );
  }
  return new Octokit({ auth: token });
}

/** Parse "owner/repo" into { owner, repo }. */
export function parseRepo(fullRepo: string): { owner: string; repo: string } {
  const [owner, repo] = fullRepo.split('/');
  if (!owner || !repo) {
    throw new Error(`Invalid repo format: "${fullRepo}". Expected "owner/repo".`);
  }
  return { owner, repo };
}

/** A published standard release, and whether its notes were ever written. */
export interface ReleaseRef {
  tag: string;
  /** False when the release body is empty — a cut whose pipeline died before notes. */
  hasNotes: boolean;
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
 * Resolve the merged PRs that introduced each commit in the range.
 *
 * Uses GET /repos/{owner}/{repo}/commits/{sha}/pulls rather than parsing "(#N)"
 * out of commit subjects. Under merge commits, feature-branch commits land on
 * main verbatim, and a subject ending in "(#N)" is often an ISSUE the author
 * typed — feeding that to pulls.get 404s and drops a real PR. The API resolves
 * a merged PR's branch commits AND its merge commit to the same PR, so the Set
 * dedupes them for free, and returns [] for direct pushes.
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

    // Promise.all preserves input order, so the returned array is deterministic.
    const batchResults = await Promise.all(promises);
    for (const numbers of batchResults) {
      for (const n of numbers) prNumbers.add(n);
    }

    if (i + BATCH_SIZE < commits.length) await sleep(500);
  }

  return Array.from(prNumbers);
}

/**
 * Fetch PR details with rate-limit awareness.
 * Processes PRs in batches to avoid hitting secondary rate limits.
 * Throws if any PR fetch fails so the workflow step fails visibly.
 */
export async function fetchPRDetails(
  octokit: Octokit,
  owner: string,
  repo: string,
  prNumbers: number[]
): Promise<Map<number, PRDetails>> {
  const results = new Map<number, PRDetails>();
  const fetchErrors: number[] = [];
  const BATCH_SIZE = 15;

  for (let i = 0; i < prNumbers.length; i += BATCH_SIZE) {
    const batch = prNumbers.slice(i, i + BATCH_SIZE);

    const promises = batch.map(async (prNumber) => {
      try {
        const { data } = await octokit.pulls.get({
          owner,
          repo,
          pull_number: prNumber,
        });

        const labels = data.labels.map((l) => l.name || '');

        const linkedIssues = extractLinkedIssues(data.body || '');

        // Safety cap at 50K chars to guard against extreme outliers
        const body = (data.body || '').slice(0, 50_000);

        return {
          number: prNumber,
          title: data.title,
          labels,
          body,
          linkedIssues,
        } as PRDetails;
      } catch (error: unknown) {
        const errMsg =
          error instanceof Error ? error.message : String(error);
        process.stderr.write(
          `Error: Could not fetch PR #${prNumber}: ${errMsg}\n`
        );
        fetchErrors.push(prNumber);
        return null;
      }
    });

    const batchResults = await Promise.all(promises);
    for (const result of batchResults) {
      if (result) {
        results.set(result.number, result);
      }
    }

    // Brief pause between batches to avoid secondary rate limits
    if (i + BATCH_SIZE < prNumbers.length) {
      await sleep(500);
    }
  }

  if (fetchErrors.length > 0) {
    process.stderr.write(
      `Warning: Could not fetch ${fetchErrors.length} PR(s): ${fetchErrors.map((n) => `#${n}`).join(', ')}. ` +
        `These PRs will be missing from the release notes.\n`
    );
  }

  return results;
}

/**
 * Extract linked issue numbers from PR body text.
 * Looks for "Closes #N", "Fixes #N", "Resolves #N" patterns.
 */
export function extractLinkedIssues(body: string): number[] {
  const issues = new Set<number>();
  const patterns = [
    /(?:close[sd]?|fix(?:e[sd])?|resolve[sd]?)\s+#(\d+)/gi,
    /(?:close[sd]?|fix(?:e[sd])?|resolve[sd]?)\s+https:\/\/github\.com\/[^/]+\/[^/]+\/issues\/(\d+)/gi,
  ];

  for (const pattern of patterns) {
    let match;
    while ((match = pattern.exec(body)) !== null) {
      issues.add(parseInt(match[1], 10));
    }
  }

  return Array.from(issues);
}

function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}
