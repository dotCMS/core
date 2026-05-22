/**
 * Output rendering for the QA report.
 *
 * - renderText:     terminal-friendly summary with full PR lists per bucket
 * - renderMarkdown: GitHub-flavored markdown report for $GITHUB_STEP_SUMMARY
 * - renderSlack:    compact Slack-mrkdwn snippet — counts as links + cc line
 *                   with resolved @-mentions of authors who need to act
 */

import { PRQAResult, ReleaseQAReport, SlackMapping } from './types';

type BucketKey = 'failed' | 'missing' | 'unlinked' | 'external';

const STATUS_HEADERS: Record<
  BucketKey,
  { emoji: string; title: string; blurb: string; anchor: string }
> = {
  failed: {
    emoji: ':rotating_light:',
    title: 'QA Failed',
    blurb: 'Linked issue carries `QA : Failed` — change may break things.',
    anchor: 'qa-failed',
  },
  missing: {
    emoji: ':warning:',
    title: 'No QA label',
    blurb:
      'Linked issue exists but has no `QA : Passed` / `QA : Not Needed` / `QA : Failed` label.',
    anchor: 'qa-missing',
  },
  unlinked: {
    emoji: ':grey_question:',
    title: 'No linked issue',
    blurb:
      'PR has no closing-issue reference (body keyword or Development panel) — cannot verify QA.',
    anchor: 'qa-orphan',
  },
  external: {
    emoji: ':link:',
    title: 'Linked to a different repo',
    blurb:
      'PR closes an issue in another repository — QA labels cannot be read from here.',
    anchor: 'qa-external',
  },
};

// =========================================================================
// renderText — full detail, terminal-friendly
// =========================================================================

function renderTextPR(pr: PRQAResult): string {
  const issueRefs: string[] = [];
  for (const i of pr.linkedIssues) issueRefs.push(`#${i.number}`);
  for (const x of pr.externalRefs) issueRefs.push(`${x.repo}#${x.number}`);
  const refsSuffix = issueRefs.length > 0 ? ` (issues: ${issueRefs.join(', ')})` : '';
  return `  - #${pr.pr} ${pr.title} — @${pr.author}${refsSuffix}\n    ${pr.url}`;
}

function renderExcludedTextLine(pr: PRQAResult): string {
  const reason = pr.exclusionReason ? ` [${pr.exclusionReason}]` : '';
  return `  - #${pr.pr} ${pr.title} — @${pr.author}${reason}`;
}

export function renderText(report: ReleaseQAReport): string {
  const lines: string[] = [];
  lines.push(`Release QA report: ${report.fromTag} → ${report.toTag}`);
  lines.push(`Repo: ${report.repo}`);
  lines.push(`Commits: ${report.totalCommits} | PRs analyzed: ${report.totalPRs}`);
  lines.push('');
  lines.push('Summary:');
  lines.push(`  failed:   ${report.summary.failed}`);
  lines.push(`  missing:  ${report.summary.missing}`);
  lines.push(`  unlinked: ${report.summary.unlinked}`);
  lines.push(`  external: ${report.summary.external}`);
  lines.push(`  passed:   ${report.summary.passed}`);
  lines.push(`  excluded: ${report.summary.excluded}`);
  lines.push('');

  const totalFlagged =
    report.summary.failed +
    report.summary.missing +
    report.summary.unlinked +
    report.summary.external;
  if (totalFlagged === 0) {
    lines.push(':white_check_mark: All non-excluded PRs have a recognized QA verdict.');
    return lines.join('\n');
  }

  for (const key of ['failed', 'missing', 'unlinked', 'external'] as const) {
    const bucket = report[key];
    if (bucket.length === 0) continue;
    const h = STATUS_HEADERS[key];
    lines.push(`${h.emoji} ${h.title} (${bucket.length})`);
    lines.push(`  ${h.blurb}`);
    for (const pr of bucket) lines.push(renderTextPR(pr));
    lines.push('');
  }

  if (report.excluded.length > 0) {
    lines.push(`Excluded (${report.excluded.length})`);
    lines.push(
      '  Bot / dependency-bump / version-bump / release-machinery PRs (skipped before QA check).'
    );
    for (const pr of report.excluded) lines.push(renderExcludedTextLine(pr));
    lines.push('');
  }

  return lines.join('\n');
}

// =========================================================================
// renderMarkdown — GitHub-flavored markdown for $GITHUB_STEP_SUMMARY
// =========================================================================

function mdEscape(s: string): string {
  return s.replace(/\|/g, '\\|');
}

function renderMarkdownTable(bucket: PRQAResult[], showIssueLabels: boolean): string[] {
  const out: string[] = [];
  out.push(showIssueLabels
    ? '| PR | Title | Author | Linked issue | Issue labels |'
    : '| PR | Title | Author | Details |');
  out.push(showIssueLabels
    ? '|---|---|---|---|---|'
    : '|---|---|---|---|');
  for (const pr of bucket) {
    const title = mdEscape(pr.title);
    if (showIssueLabels) {
      const issue = pr.linkedIssues[0];
      const issueCell = issue
        ? `[#${issue.number}](${issue.url})`
        : pr.externalRefs.map((x) => `\`${x.repo}#${x.number}\``).join(', ') || '—';
      const labels = issue ? issue.labels.map((l) => `\`${l}\``).join(', ') : '';
      out.push(`| [#${pr.pr}](${pr.url}) | ${title} | @${pr.author} | ${issueCell} | ${labels} |`);
    } else {
      const extras = pr.externalRefs.map((x) => `\`${x.repo}#${x.number}\``).join(', ');
      const labels = pr.labels.map((l) => `\`${l}\``).join(', ');
      out.push(`| [#${pr.pr}](${pr.url}) | ${title} | @${pr.author} | ${extras || labels} |`);
    }
  }
  return out;
}

export function renderMarkdown(report: ReleaseQAReport): string {
  const s = report.summary;
  const out: string[] = [];
  out.push(`# Release QA report: \`${report.fromTag}\` → \`${report.toTag}\``);
  out.push('');
  out.push(`**Repo:** \`${report.repo}\`  |  **Commits:** ${report.totalCommits}  |  **PRs analyzed:** ${report.totalPRs}`);
  out.push('');
  out.push('## Summary');
  out.push('');
  out.push('| Bucket | Count |');
  out.push('|---|---:|');
  out.push(`| :rotating_light: Failed QA | ${s.failed} |`);
  out.push(`| :warning: Missing QA | ${s.missing} |`);
  out.push(`| :grey_question: Orphan PRs | ${s.unlinked} |`);
  out.push(`| :link: Not in the core repo | ${s.external} |`);
  out.push(`| :white_check_mark: Passed | ${s.passed} |`);
  out.push(`| Excluded | ${s.excluded} |`);
  out.push('');

  const flagged = s.failed + s.missing + s.unlinked + s.external;
  if (flagged === 0) {
    out.push(':white_check_mark: All non-excluded PRs have a recognized QA verdict.');
    return out.join('\n');
  }

  const sections: Array<{ bucket: PRQAResult[]; key: BucketKey }> = [
    { bucket: report.failed, key: 'failed' },
    { bucket: report.missing, key: 'missing' },
    { bucket: report.unlinked, key: 'unlinked' },
    { bucket: report.external, key: 'external' },
  ];

  for (const sec of sections) {
    if (sec.bucket.length === 0) continue;
    const h = STATUS_HEADERS[sec.key];
    // Explicit HTML anchor: the auto-generated heading slug includes the
    // count (e.g. "qa-failed-1") and would break each time it changes.
    // Stable anchors let the Slack counts deep-link reliably.
    out.push(`<a id="${h.anchor}"></a>`);
    out.push(`## ${h.emoji} ${h.title} (${sec.bucket.length})`);
    out.push('');
    out.push(h.blurb);
    out.push('');
    const showIssueLabels = sec.key === 'failed' || sec.key === 'missing';
    out.push(...renderMarkdownTable(sec.bucket, showIssueLabels));
    out.push('');
  }

  if (report.passed.length > 0) {
    out.push(`## :white_check_mark: QA Passed (${report.passed.length})`);
    out.push('');
    out.push(...renderMarkdownTable(report.passed, true));
    out.push('');
  }

  if (report.excluded.length > 0) {
    out.push(`## Excluded (${report.excluded.length})`);
    out.push('');
    out.push('Bot / dependency-bump / version-bump / release-machinery PRs.');
    out.push('');
    out.push('| PR | Title | Author | Reason |');
    out.push('|---|---|---|---|');
    for (const pr of report.excluded) {
      out.push(
        `| [#${pr.pr}](${pr.url}) | ${mdEscape(pr.title)} | @${pr.author} | \`${pr.exclusionReason ?? ''}\` |`
      );
    }
    out.push('');
  }

  return out.join('\n');
}

// =========================================================================
// renderSlack — compact: counts as links + cc line with resolved mentions
// =========================================================================

/**
 * Slack-mrkdwn snippet for the release notification. Compact by design: just
 * counts (each linked to the detail URL if provided) plus a cc line that
 * @-mentions the authors of flagged PRs. Returns the empty string when no PR
 * needs review.
 */
export function renderSlack(
  report: ReleaseQAReport,
  options: { detailUrl?: string; mappings?: SlackMapping[] } = {}
): string {
  const s = report.summary;
  const flagged = s.failed + s.missing + s.unlinked + s.external;
  if (flagged === 0) return '';

  const out: string[] = [];
  const plural = flagged === 1 ? '' : 's';
  // Escalate header emoji when any PR actually failed QA.
  const headerEmoji = s.failed > 0 ? ':rotating_light:' : ':warning:';

  out.push('');
  out.push(`${headerEmoji} *QA Coverage* — ${flagged} PR${plural} need review`);
  out.push(`  ${slackCounts(s, options.detailUrl)}`);

  const ccLine = renderCcLine(report, options.mappings);
  if (ccLine) {
    out.push('');
    out.push(ccLine);
  }

  return out.join('\n');
}

/** Build the four-bucket count line. Each cell deep-links to its bucket
 *  anchor on the detail page when detailUrl is set. */
function slackCounts(s: ReleaseQAReport['summary'], detailUrl?: string): string {
  const cell = (label: string, n: number, key: BucketKey): string => {
    const text = `${label}: ${n}`;
    if (!detailUrl) return text;
    return `<${detailUrl}#${STATUS_HEADERS[key].anchor}|${text}>`;
  };
  return [
    cell('failed QA', s.failed, 'failed'),
    cell('missing QA', s.missing, 'missing'),
    cell('Orphan PRs', s.unlinked, 'unlinked'),
    cell('Not in the core repo', s.external, 'external'),
  ].join('  |  ');
}

/**
 * Collect unique authors across all flagged buckets and render the cc line
 * with Slack mentions where the GH user is in the mappings, plain text
 * otherwise. Returns empty string when no authors to mention.
 */
function renderCcLine(
  report: ReleaseQAReport,
  mappings: SlackMapping[] | undefined
): string {
  const authors = new Set<string>();
  for (const pr of report.failed) authors.add(pr.author);
  for (const pr of report.missing) authors.add(pr.author);
  for (const pr of report.unlinked) authors.add(pr.author);
  for (const pr of report.external) authors.add(pr.author);

  if (authors.size === 0) return '';

  const sorted = Array.from(authors).sort();
  const map = new Map<string, string>();
  if (mappings) {
    for (const m of mappings) {
      map.set(m.github_user.toLowerCase(), m.slack_id);
    }
  }

  const mentions = sorted.map((login) => {
    const id = map.get(login.toLowerCase());
    return id ? `<@${id}>` : `@${login}`;
  });

  return `cc ${mentions.join(' ')} — please review your PRs in this release.`;
}