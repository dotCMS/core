#!/usr/bin/env node

/**
 * release-qa-status
 *
 * Standalone tool: identify PRs in a release that lack QA coverage.
 *
 * Inputs:
 *   --repo        owner/repo (default: dotCMS/core)
 *   --to-tag      release tag, e.g. v26.05.19-01 (required)
 *   --from-tag    previous release tag (default: auto-detected — previous standard release)
 *   --format      "json" (default), "text", "markdown", or "slack"
 *   --mappings    path to slack-mappings.json — used by slack format to resolve
 *                 GitHub usernames to Slack member IDs for the cc line
 *   --detail-url  URL each count in the slack output links to (typically the
 *                 GitHub Actions run summary)
 *
 * Auth: set GH_TOKEN (or GITHUB_TOKEN). Quick start:
 *   export GH_TOKEN=$(gh auth token)
 *
 * Examples:
 *   npm install
 *   npm start -- --to-tag v26.05.19-01
 *   npm start -- --to-tag v26.05.19-01 --format text
 *   npm start -- --to-tag v26.05.19-01 --format markdown > $GITHUB_STEP_SUMMARY
 *   npm start -- --to-tag v26.05.19-01 --format slack \
 *     --mappings .github/data/slack-mappings.json \
 *     --detail-url https://github.com/dotCMS/core/actions/runs/12345
 *
 * QA verdict per PR (strict):
 *   - excluded:  bot / dependency-bump / release-machinery (skipped before QA check)
 *   - unlinked:  no closing-issue reference (body keyword OR Development panel)
 *   - external:  only cross-repo closing references — QA cannot be verified
 *   - failed:    any linked issue has `QA : Failed`
 *   - passed:    every linked issue has `QA : Passed` or `QA : Not Needed`
 *   - missing:   linked but no recognized QA label on any issue
 *
 * Linked-issue data comes from GitHub's GraphQL `closingIssuesReferences`,
 * which includes both body keywords (Closes/Fixes/Resolves) and issues
 * attached via the PR "Development" panel.
 */

import * as fs from 'fs';
import { CLIArgs, PRQAResult, QASummary, ReleaseQAReport, SlackMapping } from './types';
import {
  createOctokit,
  parseRepo,
  listStandardReleaseTags,
  findPreviousTag,
  fetchCommitRange,
  extractPRNumbers,
  fetchPRDetails,
  fetchIssueInfos,
} from './github';
import { computePRQA } from './qa';
import { renderMarkdown, renderSlack, renderText } from './format';

function parseArgs(argv: string[]): CLIArgs {
  const args: Partial<CLIArgs> = { format: 'json' };

  for (let i = 2; i < argv.length; i++) {
    switch (argv[i]) {
      case '--repo':
        args.repo = argv[++i];
        break;
      case '--from-tag':
        args.fromTag = argv[++i];
        break;
      case '--to-tag':
        args.toTag = argv[++i];
        break;
      case '--format': {
        const v = argv[++i];
        if (v !== 'json' && v !== 'text' && v !== 'slack' && v !== 'markdown') {
          process.stderr.write(
            `Invalid --format: ${v}. Use "json", "text", "markdown", or "slack".\n`
          );
          process.exit(1);
        }
        args.format = v;
        break;
      }
      case '--mappings':
        args.mappingsPath = argv[++i];
        break;
      case '--detail-url':
        args.detailUrl = argv[++i];
        break;
      case '-h':
      case '--help':
        process.stdout.write(
          'Usage: release-qa-status --to-tag vYY.MM.DD-NN [--repo owner/repo] ' +
            '[--from-tag vYY.MM.DD-NN] [--format json|text|markdown|slack] ' +
            '[--mappings PATH] [--detail-url URL]\n'
        );
        process.exit(0);
        break;
      default:
        process.stderr.write(`Unknown argument: ${argv[i]}\n`);
        process.exit(1);
    }
  }

  if (!args.toTag) {
    process.stderr.write('Error: --to-tag is required.\n');
    process.exit(1);
  }

  return {
    repo: args.repo || 'dotCMS/core',
    fromTag: args.fromTag,
    toTag: args.toTag,
    format: args.format || 'json',
    mappingsPath: args.mappingsPath,
    detailUrl: args.detailUrl,
  };
}

/**
 * Load slack-mappings.json. Returns [] and warns on stderr if the file is
 * missing or malformed, so the slack format still works (just without
 * @-mentions) when the mapping file isn't deployed yet.
 */
function loadMappings(path: string): SlackMapping[] {
  try {
    const raw = fs.readFileSync(path, 'utf8');
    const parsed = JSON.parse(raw);
    if (!Array.isArray(parsed)) {
      process.stderr.write(`Warning: ${path} is not a JSON array; ignoring.\n`);
      return [];
    }
    return parsed.filter(
      (m): m is SlackMapping =>
        m && typeof m.github_user === 'string' && typeof m.slack_id === 'string'
    );
  } catch (err: unknown) {
    const msg = err instanceof Error ? err.message : String(err);
    process.stderr.write(
      `Warning: could not load mappings from ${path}: ${msg}\n`
    );
    return [];
  }
}

function bucketResults(results: PRQAResult[]): Omit<ReleaseQAReport,
  'repo' | 'fromTag' | 'toTag' | 'totalCommits' | 'totalPRs'> {
  const failed: PRQAResult[] = [];
  const missing: PRQAResult[] = [];
  const unlinked: PRQAResult[] = [];
  const external: PRQAResult[] = [];
  const passed: PRQAResult[] = [];
  const excluded: PRQAResult[] = [];

  for (const r of results) {
    switch (r.status) {
      case 'failed':
        failed.push(r);
        break;
      case 'missing':
        missing.push(r);
        break;
      case 'unlinked':
        unlinked.push(r);
        break;
      case 'external':
        external.push(r);
        break;
      case 'passed':
        passed.push(r);
        break;
      case 'excluded':
        excluded.push(r);
        break;
    }
  }

  const summary: QASummary = {
    failed: failed.length,
    missing: missing.length,
    unlinked: unlinked.length,
    external: external.length,
    passed: passed.length,
    excluded: excluded.length,
  };

  return { summary, failed, missing, unlinked, external, passed, excluded };
}

async function main(): Promise<void> {
  const args = parseArgs(process.argv);
  const { owner, repo } = parseRepo(args.repo);

  process.stderr.write(`Gathering release QA status for ${args.repo}...\n`);
  const octokit = createOctokit();

  // 1) Resolve from-tag
  let fromTag = args.fromTag;
  if (!fromTag) {
    process.stderr.write(`No --from-tag provided, auto-detecting previous release...\n`);
    const tags = await listStandardReleaseTags(octokit, owner, repo);
    if (tags.length === 0) {
      process.stderr.write(
        `Error: no standard release tags found in ${args.repo}.\n`
      );
      process.exit(1);
    }
    if (tags.includes(args.toTag)) {
      fromTag = findPreviousTag(tags, args.toTag);
    } else {
      // The just-created release may not yet be indexed by the releases API
      // (eventual consistency). Fall back to the newest known tag as the
      // predecessor — listReleases returns tags newest-first, so tags[0] is
      // the previous standard release.
      process.stderr.write(
        `Note: ${args.toTag} not yet visible in releases API; ` +
          `using newest indexed tag ${tags[0]} as previous.\n`
      );
      fromTag = tags[0];
    }
    if (!fromTag) {
      process.stderr.write(
        `Error: ${args.toTag} is the earliest standard release — no previous tag to compare.\n`
      );
      process.exit(1);
    }
    process.stderr.write(`Resolved previous tag: ${fromTag}\n`);
  }

  process.stderr.write(`Comparing: ${fromTag}...${args.toTag}\n`);

  // 2) Commit range → PR numbers → PR details
  const { totalCommits, commits } = await fetchCommitRange(
    octokit,
    owner,
    repo,
    fromTag,
    args.toTag
  );
  process.stderr.write(`Found ${totalCommits} commits in range.\n`);

  if (totalCommits === 0) {
    const empty: ReleaseQAReport = {
      repo: args.repo,
      fromTag,
      toTag: args.toTag,
      totalCommits: 0,
      totalPRs: 0,
      summary: {
        failed: 0,
        missing: 0,
        unlinked: 0,
        external: 0,
        passed: 0,
        excluded: 0,
      },
      failed: [],
      missing: [],
      unlinked: [],
      external: [],
      passed: [],
      excluded: [],
    };
    emit(empty, args);
    return;
  }

  const prNumbers = extractPRNumbers(commits);
  process.stderr.write(
    `Extracted ${prNumbers.length} PR numbers from ${commits.length} commits.\n`
  );
  if (commits.length > 0 && prNumbers.length === 0) {
    // The extractPRNumbers regex only matches squash-merge commit subjects
    // (`(#N)` at end). If `main` ever switches to merge commits the QA
    // section would silently disappear from every release notification.
    // Surface the situation explicitly so the cause is obvious.
    process.stderr.write(
      `Warning: no PR numbers extracted from ${commits.length} commits. ` +
        `Has the merge strategy on the source branch changed? ` +
        `Expected squash-merge commit subjects ending in "(#N)".\n`
    );
  }

  const prDetails = await fetchPRDetails(octokit, owner, repo, prNumbers);
  process.stderr.write(`Fetched details for ${prDetails.size} PRs.\n`);

  // 3) Fetch all linked-issue infos in one pass (deduped)
  const allLinkedIssues: number[] = [];
  for (const pr of prDetails.values()) {
    allLinkedIssues.push(...pr.linkedIssues);
  }
  process.stderr.write(
    `Resolving ${new Set(allLinkedIssues).size} unique linked issues...\n`
  );
  const issueInfos = await fetchIssueInfos(octokit, owner, repo, allLinkedIssues);

  // 4) Compute QA status per PR
  const results: PRQAResult[] = [];
  for (const pr of prDetails.values()) {
    results.push(computePRQA(pr, issueInfos));
  }

  // Stable ordering: by PR number ascending within each bucket
  results.sort((a, b) => a.pr - b.pr);

  const buckets = bucketResults(results);
  const report: ReleaseQAReport = {
    repo: args.repo,
    fromTag,
    toTag: args.toTag,
    totalCommits: commits.length,
    totalPRs: results.length,
    ...buckets,
  };

  emit(report, args);

  process.stderr.write(
    `Done. failed=${report.summary.failed} missing=${report.summary.missing} ` +
      `unlinked=${report.summary.unlinked} external=${report.summary.external} ` +
      `passed=${report.summary.passed} excluded=${report.summary.excluded}\n`
  );
}

function emit(report: ReleaseQAReport, args: CLIArgs): void {
  switch (args.format) {
    case 'text':
      process.stdout.write(renderText(report) + '\n');
      return;
    case 'markdown':
      process.stdout.write(renderMarkdown(report) + '\n');
      return;
    case 'slack': {
      const mappings = args.mappingsPath ? loadMappings(args.mappingsPath) : undefined;
      const snippet = renderSlack(report, {
        detailUrl: args.detailUrl,
        mappings,
      });
      if (snippet.length > 0) process.stdout.write(snippet + '\n');
      return;
    }
    case 'json':
    default:
      process.stdout.write(JSON.stringify(report, null, 2) + '\n');
      return;
  }
}

main().catch((error) => {
  process.stderr.write(`Fatal error: ${error.message}\n`);
  process.exit(1);
});
