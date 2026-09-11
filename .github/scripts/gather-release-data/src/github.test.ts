import {
  resolvePRNumbers,
  findPreviousTag,
  extractLinkedIssues,
  parseRepo,
  onThrottle,
} from './github';
import { CommitInfo } from './types';
import type { Octokit } from '@octokit/rest';

describe('parseRepo', () => {
  it('parses owner/repo format', () => {
    expect(parseRepo('dotCMS/core')).toEqual({ owner: 'dotCMS', repo: 'core' });
  });

  it('throws for invalid format', () => {
    expect(() => parseRepo('invalid')).toThrow('Invalid repo format');
  });
});

describe('resolvePRNumbers', () => {
  it('resolves merge-commit and branch-commit dedup, and ignores direct pushes (#37201)', async () => {
    // aaa: feature-branch commit whose subject ended in the issue "(#37132)"
    // bbb: the two-parent merge commit for the same PR
    // ccc: a direct push, unassociated with any PR
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

    const commits: CommitInfo[] = [{ sha: 'aaa' }, { sha: 'bbb' }, { sha: 'ccc' }];

    const prNumbers = await resolvePRNumbers(octokit, 'dotCMS', 'core', commits);

    expect(prNumbers).toEqual([37196]);
    expect(listPRs).toHaveBeenCalledTimes(3);
  });

  it('filters out unmerged PRs', async () => {
    const bySha: Record<string, Array<{ number: number; merged_at: string | null }>> = {
      abc: [
        { number: 1, merged_at: null },
        { number: 2, merged_at: '2026-08-25T00:00:00Z' },
      ],
    };
    const listPRs = jest.fn(async ({ commit_sha }: { commit_sha: string }) => ({
      data: bySha[commit_sha] ?? [],
    }));
    const octokit = {
      repos: { listPullRequestsAssociatedWithCommit: listPRs },
    } as unknown as Octokit;

    const prNumbers = await resolvePRNumbers(octokit, 'dotCMS', 'core', [{ sha: 'abc' }]);

    expect(prNumbers).toEqual([2]);
  });

  // The per-commit catch is the "one bad sha can't abort the whole range" guarantee.
  // It degrades silently, so this is the only thing that fails if it stops working.
  it('does not abort the batch when one commit fails to resolve', async () => {
    const stderrSpy = jest.spyOn(process.stderr, 'write').mockImplementation(() => true);

    const bySha: Record<string, Array<{ number: number; merged_at: string | null }>> = {
      good: [{ number: 42, merged_at: '2026-08-25T00:00:00Z' }],
    };
    const listPRs = jest.fn(async ({ commit_sha }: { commit_sha: string }) => ({
      data: bySha[commit_sha] ?? [],
    }));
    listPRs.mockImplementationOnce(() => Promise.reject(new Error('boom')));
    const octokit = {
      repos: { listPullRequestsAssociatedWithCommit: listPRs },
    } as unknown as Octokit;

    const prNumbers = await resolvePRNumbers(octokit, 'dotCMS', 'core', [
      { sha: 'bad' },
      { sha: 'good' },
    ]);

    expect(prNumbers).toEqual([42]);
    expect(stderrSpy).toHaveBeenCalledWith(expect.stringContaining('bad'));

    stderrSpy.mockRestore();
  });
});

// Bounds the throttling plugin's retries so an exhausted quota can't park a
// release job indefinitely.
describe('onThrottle', () => {
  it('retries up to the cap, then gives up', () => {
    const stderrSpy = jest.spyOn(process.stderr, 'write').mockImplementation(() => true);
    const handler = onThrottle('Secondary');
    const opts = { method: 'GET', url: '/repos/dotCMS/core/commits/abc/pulls' };

    expect(handler(1, opts, null, 0)).toBe(true);
    expect(stderrSpy).toHaveBeenLastCalledWith(
      expect.stringContaining('Secondary rate limit on GET')
    );
    expect(stderrSpy).toHaveBeenLastCalledWith(expect.stringContaining('retry 1/3'));

    expect(handler(1, opts, null, 2)).toBe(true);
    expect(stderrSpy).toHaveBeenLastCalledWith(expect.stringContaining('retry 3/3'));

    // Never announce a retry it won't make -- this used to log "retry 4/3".
    expect(handler(1, opts, null, 3)).toBe(false);
    expect(stderrSpy).toHaveBeenLastCalledWith(
      expect.stringContaining('giving up after 3 retries')
    );
    expect(stderrSpy).not.toHaveBeenCalledWith(expect.stringContaining('retry 4/'));

    stderrSpy.mockRestore();
  });
});

describe('findPreviousTag', () => {
  const documented = (...tags: string[]) =>
    tags.map((tag) => ({ tag, hasNotes: true }));

  const tags = documented(
    'v26.03.13-02',
    'v26.03.13-01',
    'v26.03.12-01',
    'v26.03.11-01'
  );

  it('finds the tag immediately before the given tag', () => {
    expect(findPreviousTag(tags, 'v26.03.13-02')).toBe('v26.03.13-01');
    expect(findPreviousTag(tags, 'v26.03.13-01')).toBe('v26.03.12-01');
    expect(findPreviousTag(tags, 'v26.03.12-01')).toBe('v26.03.11-01');
  });

  it('returns undefined when given tag is not in the list', () => {
    expect(findPreviousTag(tags, 'v26.03.14-01')).toBeUndefined();
  });

  it('returns undefined for the oldest tag', () => {
    expect(findPreviousTag(tags, 'v26.03.11-01')).toBeUndefined();
  });

  // Regression: 26.08.19 shipped four attempts. -01/-02/-03 died before writing
  // notes, so stopping at -03 described 1 of 19 commits.
  it('skips undocumented releases', () => {
    const withGhosts = [
      { tag: 'v26.08.19-04', hasNotes: true },
      { tag: 'v26.08.19-03', hasNotes: false },
      { tag: 'v26.08.19-02', hasNotes: false },
      { tag: 'v26.08.19-01', hasNotes: false },
      { tag: 'v26.08.14-01', hasNotes: true },
    ];
    expect(findPreviousTag(withGhosts, 'v26.08.19-04')).toBe('v26.08.14-01');
  });

  it('still returns a same-day attempt that published notes', () => {
    const sameDay = [
      { tag: 'v26.08.12-02', hasNotes: true },
      { tag: 'v26.08.12-01', hasNotes: true },
    ];
    expect(findPreviousTag(sameDay, 'v26.08.12-02')).toBe('v26.08.12-01');
  });

  it('returns undefined when every predecessor is undocumented', () => {
    const allGhosts = [
      { tag: 'v26.08.19-02', hasNotes: true },
      { tag: 'v26.08.19-01', hasNotes: false },
    ];
    expect(findPreviousTag(allGhosts, 'v26.08.19-02')).toBeUndefined();
  });
});

describe('extractLinkedIssues', () => {
  it('extracts Closes #N patterns', () => {
    expect(extractLinkedIssues('Closes #123')).toEqual([123]);
    expect(extractLinkedIssues('closes #456')).toEqual([456]);
  });

  it('extracts Fixes #N patterns', () => {
    expect(extractLinkedIssues('Fixes #789')).toEqual([789]);
  });

  it('extracts Resolves #N patterns', () => {
    expect(extractLinkedIssues('Resolves #101')).toEqual([101]);
  });

  it('extracts multiple linked issues', () => {
    const issues = extractLinkedIssues('Fixes #100 and Closes #200');
    expect(issues).toEqual(expect.arrayContaining([100, 200]));
    expect(issues).toHaveLength(2);
  });

  it('extracts full URL patterns', () => {
    const issues = extractLinkedIssues(
      'Closes https://github.com/dotCMS/core/issues/12345'
    );
    expect(issues).toEqual([12345]);
  });

  it('returns empty array for no links', () => {
    expect(extractLinkedIssues('Just a normal description')).toEqual([]);
  });
});
