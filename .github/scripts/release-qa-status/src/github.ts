/**
 * PR and issue detail fetching for the QA-status report.
 *
 * Auth, throttling, tag listing, range resolution and commit→PR resolution
 * live in ../../shared/github.ts. This file used to carry a verbatim copy of
 * all of it, which is how #37138 and #37201 each had to be fixed twice.
 *
 * What stays here is what the QA report's shape dictates: PRs carry author
 * and URL for the Slack digest, and closing refs are split into same-repo vs
 * external because an external ref cannot be QA-verified in this repo.
 */

import { Octokit } from '@octokit/rest';
import { ExternalRef, LinkedIssueInfo, PRDetails } from './types';

interface ClosingRefsByPR {
  sameRepo: number[];
  external: ExternalRef[];
}

interface GraphQLClosingRefsResponse {
  repository: Record<
    string,
    {
      closingIssuesReferences: {
        nodes: Array<{
          number: number;
          repository: { nameWithOwner: string };
        }>;
        pageInfo: { hasNextPage: boolean };
      };
    } | null
  >;
}

/**
 * Fetch closing-issue references for a batch of PRs via GraphQL.
 *
 * GraphQL `closingIssuesReferences` is the source of truth — it returns links
 * from both body keywords (Closes/Fixes/Resolves) AND the Development panel
 * (manually-linked issues that don't appear in the PR body at all).
 *
 * Returns same-repo issues separately from cross-repo references so callers
 * can decide how to handle each.
 */
export async function fetchClosingIssueRefs(
  octokit: Octokit,
  owner: string,
  repo: string,
  prNumbers: number[]
): Promise<Map<number, ClosingRefsByPR>> {
  const results = new Map<number, ClosingRefsByPR>();
  const ownerRepoLower = `${owner}/${repo}`.toLowerCase();
  const BATCH = 20;

  for (let i = 0; i < prNumbers.length; i += BATCH) {
    const batch = prNumbers.slice(i, i + BATCH);
    // PR numbers come from the commits→pulls API, so they're already integers —
    // but GraphQL aliases must be static field names (no $variables), so we
    // interpolate. Belt-and-suspenders: drop anything that isn't a positive
    // integer before building the query.
    const safeBatch = batch.filter((n) => Number.isInteger(n) && n > 0);
    if (safeBatch.length === 0) continue;
    const aliases = safeBatch
      .map(
        (n) =>
          `  pr${n}: pullRequest(number: ${n}) {\n` +
          `    closingIssuesReferences(first: 100, userLinkedOnly: false) {\n` +
          `      nodes { number repository { nameWithOwner } }\n` +
          `      pageInfo { hasNextPage }\n` +
          `    }\n` +
          `  }`
      )
      .join('\n');

    const query =
      `query($owner: String!, $repo: String!) {\n` +
      `  repository(owner: $owner, name: $repo) {\n` +
      aliases +
      `\n  }\n}`;

    // Re-throw GraphQL failures: we cannot tell empty-on-purpose from
    // empty-due-to-error, so swallowing here would silently demote every PR
    // in the batch to `unlinked` and flood the Slack channel with bogus
    // orphan-PR warnings. The CI workflow has continue-on-error for the
    // whole script, so a thrown error simply drops the QA section.
    const data = await octokit.graphql<GraphQLClosingRefsResponse>(query, {
      owner,
      repo,
    });

    for (const n of safeBatch) {
      const pr = data.repository[`pr${n}`];
      if (!pr) {
        results.set(n, { sameRepo: [], external: [] });
        continue;
      }
      const sameRepo: number[] = [];
      const external: ExternalRef[] = [];
      for (const node of pr.closingIssuesReferences.nodes) {
        if (node.repository.nameWithOwner.toLowerCase() === ownerRepoLower) {
          sameRepo.push(node.number);
        } else {
          external.push({
            repo: node.repository.nameWithOwner,
            number: node.number,
          });
        }
      }
      if (pr.closingIssuesReferences.pageInfo.hasNextPage) {
        process.stderr.write(
          `Warning: PR #${n} has more than 50 closing-issue references — pagination not implemented.\n`
        );
      }
      results.set(n, { sameRepo, external });
    }

    if (i + BATCH < prNumbers.length) await sleep(500);
  }

  return results;
}

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
        return {
          number: prNumber,
          title: data.title,
          url: data.html_url,
          author: data.user?.login || '',
          authorType: data.user?.type || '',
          labels,
          // linkedIssues + externalRefs filled in by attachClosingRefs below
          linkedIssues: [],
          externalRefs: [],
        } as PRDetails;
      } catch (error: unknown) {
        const errMsg = error instanceof Error ? error.message : String(error);
        process.stderr.write(`Error: could not fetch PR #${prNumber}: ${errMsg}\n`);
        fetchErrors.push(prNumber);
        return null;
      }
    });

    const batchResults = await Promise.all(promises);
    for (const r of batchResults) if (r) results.set(r.number, r);

    if (i + BATCH_SIZE < prNumbers.length) await sleep(500);
  }

  if (fetchErrors.length > 0) {
    process.stderr.write(
      `Warning: could not fetch ${fetchErrors.length} PR(s): ${fetchErrors
        .map((n) => `#${n}`)
        .join(', ')}.\n`
    );
  }

  const refs = await fetchClosingIssueRefs(
    octokit,
    owner,
    repo,
    Array.from(results.keys())
  );
  for (const [n, pr] of results) {
    const r = refs.get(n);
    if (r) {
      pr.linkedIssues = r.sameRepo;
      pr.externalRefs = r.external;
    }
  }

  return results;
}

/**
 * Fetch labels + PR-or-issue classification for a list of issue numbers.
 * GitHub's issues endpoint returns both issues and PRs; the `pull_request`
 * field distinguishes them.
 */
export async function fetchIssueInfos(
  octokit: Octokit,
  owner: string,
  repo: string,
  issueNumbers: number[]
): Promise<Map<number, LinkedIssueInfo>> {
  const results = new Map<number, LinkedIssueInfo>();
  const BATCH_SIZE = 15;

  const unique = Array.from(new Set(issueNumbers));

  for (let i = 0; i < unique.length; i += BATCH_SIZE) {
    const batch = unique.slice(i, i + BATCH_SIZE);

    const promises = batch.map(async (issueNumber): Promise<LinkedIssueInfo> => {
      try {
        const { data } = await octokit.issues.get({
          owner,
          repo,
          issue_number: issueNumber,
        });
        const isPullRequest = !!data.pull_request;
        const labels = (data.labels || []).map((l) =>
          typeof l === 'string' ? l : l.name || ''
        );
        return {
          number: issueNumber,
          url: data.html_url,
          isPullRequest,
          notFound: false,
          labels,
          verdict: 'none', // filled in by qa.ts
        };
      } catch (error: unknown) {
        const status =
          typeof error === 'object' && error !== null && 'status' in error
            ? (error as { status: number }).status
            : 0;
        if (status === 404) {
          process.stderr.write(
            `Warning: issue #${issueNumber} not found in ${owner}/${repo}.\n`
          );
          return {
            number: issueNumber,
            url: `https://github.com/${owner}/${repo}/issues/${issueNumber}`,
            isPullRequest: false,
            notFound: true,
            labels: [],
            verdict: 'none',
          };
        }
        const errMsg = error instanceof Error ? error.message : String(error);
        process.stderr.write(
          `Error: could not fetch issue #${issueNumber}: ${errMsg}\n`
        );
        throw error;
      }
    });

    // Use allSettled so a single flaky issue lookup (e.g. a transient 5xx)
    // can't drop the entire QA section. Failed entries are treated the same
    // as notFound — `qa.ts` already ignores both when aggregating.
    const settled = await Promise.allSettled(promises);
    for (let j = 0; j < settled.length; j++) {
      const s = settled[j];
      const num = batch[j];
      if (s.status === 'fulfilled') {
        results.set(s.value.number, s.value);
      } else {
        const reason =
          s.reason instanceof Error ? s.reason.message : String(s.reason);
        process.stderr.write(
          `Warning: issue #${num} lookup failed — treating as not-found: ${reason}\n`
        );
        results.set(num, {
          number: num,
          url: `https://github.com/${owner}/${repo}/issues/${num}`,
          isPullRequest: false,
          notFound: true,
          labels: [],
          verdict: 'none',
        });
      }
    }

    if (i + BATCH_SIZE < unique.length) await sleep(500);
  }

  return results;
}

function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}
