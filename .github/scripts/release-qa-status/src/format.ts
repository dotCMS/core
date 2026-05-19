/**
 * Human-readable text rendering of the QA report.
 *
 * - renderText:  terminal-friendly summary
 * - renderSlack: Slack-mrkdwn snippet for the release notification
 */

import { PRQAResult, ReleaseQAReport } from './types';

const STATUS_HEADERS: Record<
  'failed' | 'missing' | 'unlinked' | 'external',
  { emoji: string; title: string; blurb: string }
> = {
  failed: {
    emoji: ':rotating_light:',
    title: 'QA Failed',
    blurb: 'Linked issue carries `QA : Failed` — change may break things.',
  },
  missing: {
    emoji: ':warning:',
    title: 'No QA label',
    blurb:
      'Linked issue exists but has no `QA : Passed` / `QA : Not Needed` / `QA : Failed` label.',
  },
  unlinked: {
    emoji: ':grey_question:',
    title: 'No linked issue',
    blurb:
      'PR has no closing-issue reference (body keyword or Development panel) — cannot verify QA.',
  },
  external: {
    emoji: ':link:',
    title: 'Linked to a different repo',
    blurb:
      'PR closes an issue in another repository — QA labels cannot be read from here.',
  },
};

function renderPRLine(pr: PRQAResult): string {
  const issueRefs: string[] = [];
  for (const i of pr.linkedIssues) issueRefs.push(`#${i.number}`);
  for (const x of pr.externalRefs) issueRefs.push(`${x.repo}#${x.number}`);
  const refsSuffix = issueRefs.length > 0 ? ` (issues: ${issueRefs.join(', ')})` : '';
  return `  - #${pr.pr} ${pr.title} — @${pr.author}${refsSuffix}\n    ${pr.url}`;
}

export function renderText(report: ReleaseQAReport): string {
  const lines: string[] = [];
  lines.push(`Release QA report: ${report.fromTag} → ${report.toTag}`);
  lines.push(`Repo: ${report.repo}`);
  lines.push(
    `Commits: ${report.totalCommits} | PRs analyzed: ${report.totalPRs}`
  );
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
    for (const pr of bucket) lines.push(renderPRLine(pr));
    lines.push('');
  }

  return lines.join('\n');
}

/**
 * Slack-mrkdwn snippet for the release notification. Returns the empty string
 * when no PR needs review, so the workflow can render the announcement without
 * a trailing QA section on healthy releases.
 */
export function renderSlack(report: ReleaseQAReport): string {
  const s = report.summary;
  const flagged = s.failed + s.missing + s.unlinked + s.external;
  if (flagged === 0) return '';

  const out: string[] = [];
  const plural = flagged === 1 ? '' : 's';
  out.push('');
  out.push(`:warning: *QA Coverage* — ${flagged} PR${plural} need review`);
  out.push(
    `  failed QA: ${s.failed}  |  missing QA: ${s.missing}  |  ` +
      `Orphan PRs: ${s.unlinked}  |  Not in the core repo: ${s.external}`
  );

  const sections: Array<{
    bucket: PRQAResult[];
    emoji: string;
    label: string;
    blurb: string;
  }> = [
    {
      bucket: report.failed,
      emoji: ':rotating_light:',
      label: 'Failed QA',
      blurb: 'Linked issue carries `QA : Failed` — change may break things.',
    },
    {
      bucket: report.missing,
      emoji: ':warning:',
      label: 'Missing QA',
      blurb:
        'Linked issue exists but has no `QA : Passed` / `QA : Not Needed` / `QA : Failed`.',
    },
    {
      bucket: report.unlinked,
      emoji: ':grey_question:',
      label: 'Orphan PRs',
      blurb: 'PR has no closing-issue reference — QA cannot be verified.',
    },
    {
      bucket: report.external,
      emoji: ':link:',
      label: 'Not in the core repo',
      blurb:
        'PR closes an issue in another repository — QA labels not visible from here.',
    },
  ];

  for (const sec of sections) {
    if (sec.bucket.length === 0) continue;
    out.push('');
    out.push(`${sec.emoji} *${sec.label} (${sec.bucket.length})*`);
    out.push(`  ${sec.blurb}`);
    for (const pr of sec.bucket) out.push(renderSlackPR(pr));
  }

  return out.join('\n');
}

function renderSlackPR(pr: PRQAResult): string {
  const refs: string[] = [];
  for (const i of pr.linkedIssues) {
    refs.push(i.url ? `<${i.url}|#${i.number}>` : `#${i.number}`);
  }
  for (const x of pr.externalRefs) {
    refs.push(`${x.repo}#${x.number}`);
  }
  const refsSuffix = refs.length > 0 ? ` → ${refs.join(', ')}` : '';
  const title = pr.title.replace(/[\n\r]+/g, ' ').slice(0, 140);
  return `  • <${pr.url}|#${pr.pr}> ${escapeSlack(title)} — @${pr.author}${refsSuffix}`;
}

/** Escape characters Slack interprets as mrkdwn. */
function escapeSlack(s: string): string {
  return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
}
