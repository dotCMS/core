import { renderSlack } from './format';
import { ReleaseQAReport, PRQAResult } from './types';

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

  it('uses the requested count labels', () => {
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

  it('truncates very long titles with an ellipsis', () => {
    const longTitle = 'x'.repeat(200);
    const r = report({
      summary: { failed: 0, missing: 1, unlinked: 0, external: 0, passed: 0, excluded: 0 },
      missing: [pr(10, longTitle, 'missing')],
    });
    const out = renderSlack(r);
    expect(out).toContain('…');
    // Original 200-char title should not appear verbatim
    expect(out).not.toContain(longTitle);
  });

  it('escapes mrkdwn-meaningful characters in titles', () => {
    const r = report({
      summary: { failed: 0, missing: 1, unlinked: 0, external: 0, passed: 0, excluded: 0 },
      missing: [pr(10, 'fix <script> & co.', 'missing')],
    });
    const out = renderSlack(r);
    expect(out).toContain('&lt;script&gt;');
    expect(out).toContain('&amp;');
  });

  it('replaces backticks so paired backticks do not render as Slack monospace', () => {
    const r = report({
      summary: { failed: 0, missing: 1, unlinked: 0, external: 0, passed: 0, excluded: 0 },
      missing: [pr(10, 'bump deps to `v1.2.3-rc.1`', 'missing')],
    });
    const out = renderSlack(r);
    expect(out).not.toContain('`v1.2.3-rc.1`');
    expect(out).toContain("'v1.2.3-rc.1'");
  });
});
