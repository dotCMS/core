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

/** 20 PRs/query — bodies dominate the response, so keep batches smaller. */
const PR_BATCH = 20;

interface PRDetailsResponse {
  repository: Record<
    string,
    {
      number: number;
      title: string;
      body: string | null;
      labels: { nodes: { name: string }[] } | null;
      closingIssuesReferences: {
        nodes: { number: number; repository: { nameWithOwner: string } }[];
      };
    } | null
  >;
}

/**
 * Fetch title, labels, body and linked issues for a list of PRs.
 *
 * `closingIssuesReferences` replaces the old body regex, which required
 * whitespace directly after the keyword and so missed `Closes: #N` — the form
 * CLAUDE.md mandates for every dotCMS PR. On v26.09.02-01 that cost 23 of 51
 * PRs their issue cross-link. GraphQL reads the same relationship GitHub
 * itself renders, so the colon form, `Closes owner/repo#N`, and issues linked
 * only through the Development sidebar all resolve.
 *
 * Cross-repo refs are dropped: the changelog links issues in this repo only.
 * (release-qa-status keeps them — it reports on them separately.)
 */
export async function fetchPRDetails(
  octokit: Octokit,
  owner: string,
  repo: string,
  prNumbers: number[]
): Promise<Map<number, PRDetails>> {
  const results = new Map<number, PRDetails>();
  const missing: number[] = [];
  const ownerRepoLower = `${owner}/${repo}`.toLowerCase();

  for (let i = 0; i < prNumbers.length; i += PR_BATCH) {
    const batch = prNumbers
      .slice(i, i + PR_BATCH)
      .filter((n) => Number.isInteger(n) && n > 0);
    if (batch.length === 0) continue;

    const aliases = batch
      .map(
        (n) =>
          `  pr${n}: pullRequest(number: ${n}) {\n` +
          `    number title body\n` +
          `    labels(first: 50) { nodes { name } }\n` +
          `    closingIssuesReferences(first: 50, userLinkedOnly: false) {\n` +
          `      nodes { number repository { nameWithOwner } }\n` +
          `    }\n` +
          `  }`
      )
      .join('\n');

    // Re-thrown for the same reason as resolvePRNumbers: a dropped batch would
    // quietly shrink the changelog rather than fail the step.
    const data = await octokit.graphql<PRDetailsResponse>(
      `query($owner: String!, $repo: String!) {\n` +
        `  repository(owner: $owner, name: $repo) {\n` +
        aliases +
        `\n  }\n}`,
      { owner, repo }
    );

    for (const n of batch) {
      const pr = data.repository[`pr${n}`];
      if (!pr) {
        missing.push(n);
        continue;
      }
      results.set(n, {
        number: n,
        title: pr.title,
        labels: (pr.labels?.nodes ?? []).map((l) => l.name),
        // See BODY_CHAR_LIMIT.
        body: (pr.body ?? '').slice(0, BODY_CHAR_LIMIT),
        linkedIssues: pr.closingIssuesReferences.nodes
          .filter((r) => r.repository.nameWithOwner.toLowerCase() === ownerRepoLower)
          .map((r) => r.number),
      });
    }
  }

  if (missing.length > 0) {
    process.stderr.write(
      `Warning: ${missing.length} PR(s) not found in ${owner}/${repo}: ` +
        `${missing.map((n) => `#${n}`).join(', ')}. These will be missing from the release notes.\n`
    );
  }

  return results;
}
