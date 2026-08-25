import type { Octokit } from '@octokit/rest';
import { findPreviousTag, resolvePRNumbers } from './github';

// Guards against drift from gather-release-data/src/github.ts, which resolves the
// same release boundary. If these two disagree, the QA status and the changelog
// report on different commit ranges for the same release.
describe('findPreviousTag', () => {
  it('skips undocumented releases', () => {
    // 26.08.19 shipped four attempts; -01/-02/-03 died before writing notes.
    const releases = [
      { tag: 'v26.08.19-04', hasNotes: true },
      { tag: 'v26.08.19-03', hasNotes: false },
      { tag: 'v26.08.19-02', hasNotes: false },
      { tag: 'v26.08.19-01', hasNotes: false },
      { tag: 'v26.08.14-01', hasNotes: true },
    ];
    expect(findPreviousTag(releases, 'v26.08.19-04')).toBe('v26.08.14-01');
  });

  it('returns a same-day attempt that published notes', () => {
    const releases = [
      { tag: 'v26.08.12-02', hasNotes: true },
      { tag: 'v26.08.12-01', hasNotes: true },
    ];
    expect(findPreviousTag(releases, 'v26.08.12-02')).toBe('v26.08.12-01');
  });

  it('returns undefined for an unknown tag or no documented predecessor', () => {
    const releases = [{ tag: 'v26.08.19-01', hasNotes: true }];
    expect(findPreviousTag(releases, 'v26.08.20-01')).toBeUndefined();
    expect(findPreviousTag(releases, 'v26.08.19-01')).toBeUndefined();
  });
});

// Guards against drift from gather-release-data/src/github.ts, which resolves PRs
// the same way. Regression for #37201: under merge commits, a feature-branch
// commit's subject can end in an ISSUE number, so the old "(#N)" regex resolved
// the wrong PR (or none).
describe('resolvePRNumbers', () => {
  it('resolves branch + merge commits to the same merged PR and skips direct pushes', async () => {
    const bySha: Record<string, Array<{ number: number; merged_at: string | null }>> = {
      aaa: [{ number: 37196, merged_at: '2026-08-25T00:00:00Z' }],
      bbb: [{ number: 37196, merged_at: '2026-08-25T00:00:00Z' }],
      ccc: [],
    };
    const listPRs = jest.fn(async ({ commit_sha }: { commit_sha: string }) => ({
      data: bySha[commit_sha] ?? [],
    }));
    const octokit = {
      repos: { listPullRequestsAssociatedWithCommit: listPRs },
    } as unknown as Octokit;

    const result = await resolvePRNumbers(octokit, 'dotCMS', 'core', [
      { sha: 'aaa' },
      { sha: 'bbb' },
      { sha: 'ccc' },
    ]);

    expect(result).toEqual([37196]);
    expect(listPRs).toHaveBeenCalledTimes(3);
  });
});
