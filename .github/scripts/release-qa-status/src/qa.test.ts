import { computePRQA } from './qa';
import { LinkedIssueInfo, PRDetails } from './types';

function pr(overrides: Partial<PRDetails> = {}): PRDetails {
  return {
    number: 1,
    title: 'feat: do a thing',
    url: 'https://github.com/dotCMS/core/pull/1',
    author: 'alice',
    authorType: 'User',
    labels: [],
    linkedIssues: [],
    externalRefs: [],
    ...overrides,
  };
}

function issue(
  number: number,
  labels: string[],
  extra: Partial<LinkedIssueInfo> = {}
): LinkedIssueInfo {
  return {
    number,
    url: `https://github.com/dotCMS/core/issues/${number}`,
    isPullRequest: false,
    notFound: false,
    labels,
    verdict: 'none',
    ...extra,
  };
}

describe('computePRQA', () => {
  it('returns excluded for bot-authored PRs without touching issues', () => {
    const result = computePRQA(pr({ authorType: 'Bot' }), new Map());
    expect(result.status).toBe('excluded');
    expect(result.exclusionReason).toBe('bot-author');
    expect(result.linkedIssues).toEqual([]);
  });

  it('returns unlinked when no closing refs of either kind exist', () => {
    const result = computePRQA(pr(), new Map());
    expect(result.status).toBe('unlinked');
  });

  it('returns external when only cross-repo refs exist', () => {
    const result = computePRQA(
      pr({
        externalRefs: [{ repo: 'dotCMS/private-issues', number: 9 }],
      }),
      new Map()
    );
    expect(result.status).toBe('external');
  });

  it('returns passed when every linked issue is QA : Passed or QA : Not Needed', () => {
    const infos = new Map<number, LinkedIssueInfo>([
      [10, issue(10, ['QA : Passed', 'Team : Falcon'])],
      [11, issue(11, ['QA : Not Needed'])],
    ]);
    const result = computePRQA(pr({ linkedIssues: [10, 11] }), infos);
    expect(result.status).toBe('passed');
  });

  it('returns failed when ANY linked issue has QA : Failed', () => {
    const infos = new Map<number, LinkedIssueInfo>([
      [10, issue(10, ['QA : Passed'])],
      [11, issue(11, ['QA : Failed'])],
    ]);
    const result = computePRQA(pr({ linkedIssues: [10, 11] }), infos);
    expect(result.status).toBe('failed');
  });

  it('returns missing when no linked issue carries a recognized QA label', () => {
    const infos = new Map<number, LinkedIssueInfo>([
      [10, issue(10, ['Team : Scout'])],
    ]);
    const result = computePRQA(pr({ linkedIssues: [10] }), infos);
    expect(result.status).toBe('missing');
  });

  it('label matching is case-insensitive', () => {
    const infos = new Map<number, LinkedIssueInfo>([
      [10, issue(10, ['qa : passed'])],
    ]);
    const result = computePRQA(pr({ linkedIssues: [10] }), infos);
    expect(result.status).toBe('passed');
  });

  it('ignores linked refs that resolve to a PR (not an issue)', () => {
    const infos = new Map<number, LinkedIssueInfo>([
      [
        10,
        issue(10, ['QA : Passed'], { isPullRequest: true }),
      ],
    ]);
    // Only ref is a PR-reference; treated as missing rather than passed.
    const result = computePRQA(pr({ linkedIssues: [10] }), infos);
    expect(result.status).toBe('missing');
  });

  it('ignores linked refs that 404', () => {
    const infos = new Map<number, LinkedIssueInfo>([
      [10, issue(10, [], { notFound: true })],
    ]);
    const result = computePRQA(pr({ linkedIssues: [10] }), infos);
    expect(result.status).toBe('missing');
  });

  it('still resolves passed when one ignored ref sits beside a passed one', () => {
    const infos = new Map<number, LinkedIssueInfo>([
      [10, issue(10, ['QA : Passed'])],
      [11, issue(11, [], { notFound: true })],
    ]);
    const result = computePRQA(pr({ linkedIssues: [10, 11] }), infos);
    expect(result.status).toBe('passed');
  });

  it('surfaces externalRefs alongside same-repo linkedIssues in passed result', () => {
    const infos = new Map<number, LinkedIssueInfo>([
      [10, issue(10, ['QA : Passed'])],
    ]);
    const result = computePRQA(
      pr({
        linkedIssues: [10],
        externalRefs: [{ repo: 'dotCMS/private-issues', number: 5 }],
      }),
      infos
    );
    expect(result.status).toBe('passed');
    expect(result.externalRefs).toEqual([
      { repo: 'dotCMS/private-issues', number: 5 },
    ]);
  });

  it('same-repo missing wins over coexistent externalRefs', () => {
    // When a PR has both same-repo links AND externalRefs, the same-repo
    // verdict drives the status. The external ref is only surfaced for
    // context.
    const infos = new Map<number, LinkedIssueInfo>([
      [10, issue(10, ['Team : Scout'])], // no QA label → missing
    ]);
    const result = computePRQA(
      pr({
        linkedIssues: [10],
        externalRefs: [{ repo: 'dotCMS/private-issues', number: 5 }],
      }),
      infos
    );
    expect(result.status).toBe('missing');
    expect(result.externalRefs).toEqual([
      { repo: 'dotCMS/private-issues', number: 5 },
    ]);
  });
});
