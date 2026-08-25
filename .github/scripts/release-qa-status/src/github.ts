/**
 * GitHub API interactions: tag resolution, commit range, PR + issue fetching.
 *
 * Mirrors the patterns in .github/scripts/gather-release-data/src/github.ts so
 * the two tools stay easy to consolidate later.
 */

import { Octokit } from '@octokit/rest';
import { CommitInfo, ExternalRef, LinkedIssueInfo, PRDetails } from './types';

const STANDARD_RELEASE_PATTERN = /^v\d{2}\.\d{2}\.\d{2}-\d{1,2}$/;

export function createOctokit(): Octokit {
  const token = process.env.GITHUB_TOKEN || process.env.GH_TOKEN;
  if (!token) {
    throw new Error(
      'GitHub token required. Set GITHUB_TOKEN or GH_TOKEN (e.g. `export GH_TOKEN=$(gh auth token)`).'
    );
  }
  return new Octokit({ auth: token });
}

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
 * Kept in sync with gather-release-data/src/github.ts: both resolve the same release
 * boundary, and a divergence puts the QA status and the changelog on different ranges.
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
 * Undocumented releases are skipped: a cut whose pipeline died before writing notes
 * never shipped a QA status either, so stopping at its tag would report on one attempt
 * instead of the whole release.
 */
export function findPreviousTag(
  releases: ReleaseRef[],
  currentTag: string
): string | undefined {
  const idx = releases.findIndex((r) => r.tag === currentTag);
  if (idx === -1) return undefined;
  return releases.slice(idx + 1).find((r) => r.hasNotes)?.tag;
}

export async function fetchCommitRange(
  octokit: Octokit,
  owner: string,
  repo: string,
  fromTag: string,
  toTag: string
): Promise<{ totalCommits: number; commits: CommitInfo[] }> {
  const initial = await octokit.repos.compareCommitsWithBasehead({
    owner,
    repo,
    basehead: `${fromTag}...${toTag}`,
    per_page: 1,
  });

  const totalCommits = initial.data.total_commits;
  if (totalCommits === 0) return { totalCommits: 0, commits: [] };

  const commits: CommitInfo[] = [];
  const perPage = 250;
  let page = 1;

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

    if (response.data.commits.length < perPage) break;
    page++;
  }

  return { totalCommits, commits };
}

/** Mirrors resolvePRNumbers in gather-release-data/src/github.ts. */
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
