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

/**
 * List standard release tags (matching vYY.MM.DD-NN pattern), sorted by
 * creation date descending (newest first).
 */
export async function listStandardReleaseTags(
  octokit: Octokit,
  owner: string,
  repo: string
): Promise<string[]> {
  const tags: string[] = [];
  for await (const response of octokit.paginate.iterator(
    octokit.repos.listReleases,
    { owner, repo, per_page: 100 }
  )) {
    for (const release of response.data) {
      if (STANDARD_RELEASE_PATTERN.test(release.tag_name)) {
        tags.push(release.tag_name);
      }
    }
  }
  return tags;
}

/**
 * Find the previous standard release tag before `currentTag`.
 * Tags are ordered newest-first from the API.
 */
export function findPreviousTag(
  tags: string[],
  currentTag: string
): string | undefined {
  const idx = tags.indexOf(currentTag);
  if (idx === -1) {
    // Tag not found in release history — caller must handle
    return undefined;
  }
  // Return the next tag after the current one (i.e., the one before it chronologically)
  return tags[idx + 1];
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
      commits.push({
        sha: c.sha,
        message: c.commit.message.split('\n')[0], // first line only
      });
    }

    // If we got fewer than perPage, we've reached the end
    if (response.data.commits.length < perPage) break;
    page++;
  }

  return { totalCommits, commits };
}

/**
 * Extract PR numbers from commit messages.
 * Looks for patterns like "(#12345)" at the end of the first line.
 */
export function extractPRNumbers(commits: CommitInfo[]): number[] {
  const prNumbers = new Set<number>();
  for (const commit of commits) {
    const match = commit.message.match(/\(#(\d+)\)\s*$/);
    if (match) {
      prNumbers.add(parseInt(match[1], 10));
    }
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

        const labels = data.labels.map((l) =>
          typeof l === 'string' ? l : l.name || ''
        );

        const linkedIssues = extractLinkedIssues(data.body || '');

        // Truncate body to first ~500 chars for context (avoids oversized JSON)
        const body = (data.body || '').slice(0, 500);

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
    throw new Error(
      `Failed to fetch ${fetchErrors.length} PR(s): ${fetchErrors.map((n) => `#${n}`).join(', ')}`
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
