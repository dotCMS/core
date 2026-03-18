#!/usr/bin/env node

/**
 * gather-release-data
 *
 * Gathers structured release data from GitHub for changelog generation.
 * Outputs JSON to stdout suitable for consumption by Claude or other AI models.
 *
 * Usage:
 *   npx ts-node src/index.ts --repo dotCMS/core --to-tag v26.03.13-02
 *   npx ts-node src/index.ts --repo dotCMS/core --from-tag v26.03.13-01 --to-tag v26.03.13-02
 */

import { CLIArgs, ReleaseData } from './types';
import {
  createOctokit,
  parseRepo,
  listStandardReleaseTags,
  findPreviousTag,
  fetchCommitRange,
  extractPRNumbers,
  fetchPRDetails,
} from './github';
import { processChanges } from './categorize';

/** Parse CLI arguments. */
function parseArgs(argv: string[]): CLIArgs {
  const args: Partial<CLIArgs> = {};

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
      default:
        process.stderr.write(`Unknown argument: ${argv[i]}\n`);
        process.exit(1);
    }
  }

  if (!args.toTag) {
    process.stderr.write('Error: --to-tag is required.\n');
    process.stderr.write(
      'Usage: gather-release-data --repo owner/repo --to-tag vYY.MM.DD-NN [--from-tag vYY.MM.DD-NN]\n'
    );
    process.exit(1);
  }

  if (!args.repo) {
    process.stderr.write('Warning: --repo not specified, defaulting to dotCMS/core\n');
  }

  return {
    repo: args.repo || 'dotCMS/core',
    fromTag: args.fromTag,
    toTag: args.toTag,
  };
}

async function main(): Promise<void> {
  const args = parseArgs(process.argv);
  const { owner, repo } = parseRepo(args.repo);

  process.stderr.write(`Gathering release data for ${args.repo}...\n`);

  const octokit = createOctokit();

  // Resolve from-tag if not provided
  let fromTag = args.fromTag;
  if (!fromTag) {
    process.stderr.write(
      `No --from-tag provided, auto-detecting previous release...\n`
    );
    const tags = await listStandardReleaseTags(octokit, owner, repo);

    // Validate that toTag is visible in the release list. If it's missing,
    // the GitHub releases API hasn't indexed the newly published release yet.
    if (!tags.includes(args.toTag)) {
      process.stderr.write(
        `Error: ${args.toTag} not found in GitHub releases API. ` +
          `The release may not have been published yet, or the API has not caught up. ` +
          `Re-run this workflow to retry.\n`
      );
      process.exit(1);
    }

    fromTag = findPreviousTag(tags, args.toTag);
    if (!fromTag) {
      process.stderr.write(
        `Error: ${args.toTag} is the earliest standard release — no previous tag to compare against.\n`
      );
      process.exit(1);
    }
    process.stderr.write(`Resolved previous tag: ${fromTag}\n`);
  }

  process.stderr.write(`Comparing: ${fromTag}...${args.toTag}\n`);

  // Fetch commit range
  const { totalCommits, commits } = await fetchCommitRange(
    octokit,
    owner,
    repo,
    fromTag,
    args.toTag
  );

  process.stderr.write(`Found ${totalCommits} commits in range.\n`);

  if (commits.length < totalCommits) {
    process.stderr.write(
      `Warning: GitHub capped the commit response at ${commits.length} of ${totalCommits} total. ` +
        `Release notes will reflect only analyzed commits.\n`
    );
  }

  if (totalCommits === 0) {
    const emptyResult: ReleaseData = {
      repo: args.repo,
      fromTag,
      toTag: args.toTag,
      totalCommits: 0,
      rollbackUnsafe: [],
      skipped: [],
      changes: [],
    };
    process.stdout.write(JSON.stringify(emptyResult, null, 2) + '\n');
    return;
  }

  // Extract PR numbers from commit messages
  const prNumbers = extractPRNumbers(commits);
  process.stderr.write(
    `Extracted ${prNumbers.length} PR numbers from ${commits.length} commits.\n`
  );

  // Fetch PR details
  const prDetails = await fetchPRDetails(octokit, owner, repo, prNumbers);
  process.stderr.write(`Fetched details for ${prDetails.size} PRs.\n`);

  // Process and categorize
  const { changes, rollbackUnsafe, skipped } = processChanges(prDetails);

  // Build output — use commits.length (actual analyzed) not totalCommits (GitHub's
  // API-reported figure, which may exceed what the compare endpoint returns).
  const result: ReleaseData = {
    repo: args.repo,
    fromTag,
    toTag: args.toTag,
    totalCommits: commits.length,
    rollbackUnsafe,
    skipped,
    changes,
  };

  // Output JSON to stdout
  process.stdout.write(JSON.stringify(result, null, 2) + '\n');

  process.stderr.write(
    `Done. ${changes.length} changes, ${rollbackUnsafe.length} rollback-unsafe, ${skipped.length} skipped.\n`
  );
}

main().catch((error) => {
  process.stderr.write(`Fatal error: ${error.message}\n`);
  process.exit(1);
});
