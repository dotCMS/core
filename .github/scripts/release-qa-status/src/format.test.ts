import { renderMarkdown, renderSlack } from './format';
import { PRQAResult, ReleaseQAReport, SlackMapping } from './types';

function pr(
  number: number,
  title: string,
  status: PRQAResult['status'],
  extras: Partial<PRQAResult> = {}
): PRQAResult {
  return {
    pr: number,
    title,
    url: `https://github.com/dotCMS/core/pull/${number}`,
    author: 'alice',
    labels: [],
    status,
    linkedIssues: [],
    externalRefs: [],
    ...extras,
  };
}

function report(overrides: Partial<ReleaseQAReport> = {}): ReleaseQAReport {
  return {
    repo: 'dotCMS/core',
    fromTag: 'v26.05.18-01',
    toTag: 'v26.05.19-01',
    totalCommits: 5,
    totalPRs: 5,
    summary: { failed: 0, missing: 0, unlinked: 0, external: 0, passed: 0, excluded: 0 },
    failed: [],
    missing: [],
    unlinked: [],
    external: [],
    passed: [],
    excluded: [],
    ...overrides,
  };
}

describe('renderSlack', () => {
  it('returns empty string for a clean release', () => {
    expect(renderSlack(report())).toBe('');
  });

  it('uses :warning: emoji when only non-failed buckets are populated', () => {
    const r = report({
      summary: { failed: 0, missing: 1, unlinked: 0, external: 0, passed: 0, excluded: 0 },
      missing: [pr(10, 't', 'missing')],
    });
    expect(renderSlack(r)).toContain(':warning: *QA Coverage*');
  });

  it('escalates header to :rotating_light: when any PR failed QA', () => {
    const r = report({
      summary: { failed: 1, missing: 0, unlinked: 0, external: 0, passed: 0, excluded: 0 },
      failed: [pr(10, 't', 'failed')],
    });
    expect(renderSlack(r)).toContain(':rotating_light: *QA Coverage*');
  });

  it('renders the count labels exactly as requested', () => {
    const r = report({
      summary: { failed: 1, missing: 2, unlinked: 3, external: 4, passed: 0, excluded: 0 },
      failed: [pr(1, 't', 'failed')],
    });
    const out = renderSlack(r);
    expect(out).toContain('failed QA: 1');
    expect(out).toContain('missing QA: 2');
    expect(out).toContain('Orphan PRs: 3');
    expect(out).toContain('Not in the core repo: 4');
  });

  it('does NOT include a detailed PR list in the slack snippet', () => {
    const r = report({
      summary: { failed: 0, missing: 2, unlinked: 0, external: 0, passed: 0, excluded: 0 },
      missing: [
        pr(35463, 'feat(maintenance): thread dump endpoints', 'missing'),
        pr(35726, 'fix(edit-content): normalize lockedBy', 'missing'),
      ],
    });
    const out = renderSlack(r);
    // Compact: just counts + cc line, no per-PR lines.
    expect(out).not.toContain('thread dump');
    expect(out).not.toContain('normalize lockedBy');
    expect(out).not.toContain('https://github.com/dotCMS/core/pull/35463');
  });

  it('wraps each count in a Slack link with per-bucket anchor when detailUrl is provided', () => {
    const r = report({
      summary: { failed: 1, missing: 0, unlinked: 0, external: 0, passed: 0, excluded: 0 },
      failed: [pr(1, 't', 'failed')],
    });
    const out = renderSlack(r, { detailUrl: 'https://example.com/run/123' });
    expect(out).toContain('<https://example.com/run/123#user-content-qa-failed|failed QA: 1>');
    expect(out).toContain('<https://example.com/run/123#user-content-qa-missing|missing QA: 0>');
    expect(out).toContain('<https://example.com/run/123#user-content-qa-orphan|Orphan PRs: 0>');
    expect(out).toContain('<https://example.com/run/123#user-content-outside-dotcms-core|Not in the core repo: 0>');
  });

  it('omits detailUrl wrapping when none is provided', () => {
    const r = report({
      summary: { failed: 1, missing: 0, unlinked: 0, external: 0, passed: 0, excluded: 0 },
      failed: [pr(1, 't', 'failed')],
    });
    const out = renderSlack(r);
    expect(out).toContain('failed QA: 1');
    expect(out).not.toMatch(/<https:[^|]+\|failed QA/);
  });
});

describe('renderSlack cc line', () => {
  const mappings: SlackMapping[] = [
    { github_user: 'alice', slack_id: 'U0ALICE' },
    { github_user: 'dsilvam', slack_id: 'U0DSILVAM' },
  ];

  it('emits a cc line that mentions flagged-PR authors', () => {
    const r = report({
      summary: { failed: 1, missing: 0, unlinked: 0, external: 0, passed: 0, excluded: 0 },
      failed: [pr(1, 't', 'failed', { author: 'alice' })],
    });
    const out = renderSlack(r, { mappings });
    expect(out).toContain('cc <@U0ALICE>');
    expect(out).toContain('please review your PRs');
  });

  it('uses Slack ID for mapped users and plain @login for unmapped', () => {
    const r = report({
      summary: { failed: 1, missing: 1, unlinked: 0, external: 0, passed: 0, excluded: 0 },
      failed: [pr(1, 't', 'failed', { author: 'alice' })],
      missing: [pr(2, 't', 'missing', { author: 'unknownuser' })],
    });
    const out = renderSlack(r, { mappings });
    expect(out).toContain('<@U0ALICE>');
    expect(out).toContain('@unknownuser');
    expect(out).not.toContain('<@U0UNKNOWNUSER>');
  });

  it('deduplicates authors that have multiple flagged PRs', () => {
    const r = report({
      summary: { failed: 0, missing: 2, unlinked: 0, external: 0, passed: 0, excluded: 0 },
      missing: [
        pr(1, 't1', 'missing', { author: 'alice' }),
        pr(2, 't2', 'missing', { author: 'alice' }),
      ],
    });
    const out = renderSlack(r, { mappings });
    const occurrences = (out.match(/U0ALICE/g) || []).length;
    expect(occurrences).toBe(1);
  });

  it('does NOT mention authors whose only PRs are in the passed bucket', () => {
    const r = report({
      summary: { failed: 1, missing: 0, unlinked: 0, external: 0, passed: 1, excluded: 0 },
      failed: [pr(1, 't', 'failed', { author: 'alice' })],
      passed: [pr(2, 't', 'passed', { author: 'dsilvam' })],
    });
    const out = renderSlack(r, { mappings });
    expect(out).toContain('<@U0ALICE>');
    expect(out).not.toContain('<@U0DSILVAM>');
  });

  it('matches mappings case-insensitively', () => {
    const r = report({
      summary: { failed: 1, missing: 0, unlinked: 0, external: 0, passed: 0, excluded: 0 },
      failed: [pr(1, 't', 'failed', { author: 'Alice' })],
    });
    const out = renderSlack(r, { mappings });
    expect(out).toContain('<@U0ALICE>');
  });
});

describe('renderMarkdown', () => {
  it('produces a markdown report with the expected H1 and summary table', () => {
    const r = report({
      summary: { failed: 0, missing: 1, unlinked: 0, external: 0, passed: 0, excluded: 0 },
      missing: [pr(10, 'fix something', 'missing', { author: 'alice' })],
    });
    const out = renderMarkdown(r);
    expect(out).toContain('# Release QA report: `v26.05.18-01` → `v26.05.19-01`');
    expect(out).toContain('| :warning: Missing QA |');
    expect(out).toContain('## :warning: No QA label (1)');
    expect(out).toContain('[#10](https://github.com/dotCMS/core/pull/10)');
  });

  it('emits stable HTML anchors before each populated bucket heading', () => {
    const r = report({
      summary: { failed: 1, missing: 1, unlinked: 1, external: 1, passed: 0, excluded: 0 },
      failed: [pr(1, 't', 'failed')],
      missing: [pr(2, 't', 'missing')],
      unlinked: [pr(3, 't', 'unlinked')],
      external: [
        pr(4, 't', 'external', {
          externalRefs: [{ repo: 'dotCMS/private-issues', number: 9 }],
        }),
      ],
    });
    const out = renderMarkdown(r);
    expect(out).toContain('<a id="qa-failed"></a>');
    expect(out).toContain('<a id="qa-missing"></a>');
    expect(out).toContain('<a id="qa-orphan"></a>');
    expect(out).toContain('<a id="outside-dotcms-core"></a>');
  });

  it('skips anchors for empty buckets', () => {
    const r = report({
      summary: { failed: 1, missing: 0, unlinked: 0, external: 0, passed: 0, excluded: 0 },
      failed: [pr(1, 't', 'failed')],
    });
    const out = renderMarkdown(r);
    expect(out).toContain('<a id="qa-failed"></a>');
    expect(out).not.toContain('<a id="qa-missing"></a>');
    expect(out).not.toContain('<a id="qa-orphan"></a>');
    expect(out).not.toContain('<a id="outside-dotcms-core"></a>');
  });

  it('shows the all-clean message when nothing is flagged', () => {
    const out = renderMarkdown(report());
    expect(out).toContain('All non-excluded PRs have a recognized QA verdict');
  });
});