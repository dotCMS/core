/**
 * PR detail fetching for the changelog.
 *
 * Auth, throttling, tag listing, range resolution and commit→PR resolution
 * live in ../../shared/github.ts. Only what the changelog's shape dictates
 * stays here — release-qa-status needs a different shape from the same PRs,
 * which is why fetchPRDetails is not shared.
 */

import { Octokit } from '@octokit/rest';
import { PRDetails } from './types';

/** Per-PR body truncation, see fetchPRDetails. */
export const BODY_CHAR_LIMIT = 1_000;

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

        // Bodies are ~90% of the payload and the changelog only needs the lede,
        // so keep the first BODY_CHAR_LIMIT chars. Uncapped, a full release
        // (50+ PRs, spec/design PRs running 10-18K each) pushes the assembled
        // prompt past Linux MAX_ARG_STRLEN (128 KiB per argv/env entry) and any
        // step that passes it through argv or GITHUB_ENV dies with E2BIG.
        const body = (data.body || '').slice(0, BODY_CHAR_LIMIT);

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
