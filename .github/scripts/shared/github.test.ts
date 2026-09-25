import {
  findPreviousTag,
  listStandardReleaseTags,
  onThrottle,
  parseRepo,
  resolvePRNumbers,
} from './github';
import type { CommitInfo } from './github';
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
  /** Stand-in for octokit.graphql that serves a sha -> associated PRs map. */
  function mockGraphql(
    bySha: Record<string, Array<{ number: number; mergedAt: string | null }>>
  ) {
    return jest.fn(async (query: string) => {
      const repository: Record<string, unknown> = {};
      for (const [sha, nodes] of Object.entries(bySha)) {
        // Only answer for shas the query actually asked about, so the test
        // fails if alias construction drops or mangles a commit.
        if (query.includes(`c${sha}: object(oid: "${sha}")`)) {
          repository[`c${sha}`] = { associatedPullRequests: { nodes } };
        }
      }
      return { repository };
    });
  }

  const AAA = 'a'.repeat(40); // feature-branch commit, subject ended in "(#37132)"
  const BBB = 'b'.repeat(40); // the two-parent merge commit for the same PR
  const CCC = 'c'.repeat(40); // a direct push, unassociated with any PR

  it('dedupes merge and branch commits, and ignores direct pushes (#37201)', async () => {
    const graphql = mockGraphql({
      [AAA]: [{ number: 37196, mergedAt: '2026-08-25T00:00:00Z' }],
      [BBB]: [{ number: 37196, mergedAt: '2026-08-25T00:00:00Z' }],
      [CCC]: [],
    });
    const octokit = { graphql } as unknown as Octokit;

    const commits: CommitInfo[] = [{ sha: AAA }, { sha: BBB }, { sha: CCC }];
    expect(await resolvePRNumbers(octokit, 'dotCMS', 'core', commits)).toEqual([37196]);
    // The point of the migration: three commits, one round-trip.
    expect(graphql).toHaveBeenCalledTimes(1);
  });

  it('filters out unmerged PRs', async () => {
    const graphql = mockGraphql({
      [AAA]: [
        { number: 1, mergedAt: null },
        { number: 2, mergedAt: '2026-08-25T00:00:00Z' },
      ],
    });
    const octokit = { graphql } as unknown as Octokit;
    expect(await resolvePRNumbers(octokit, 'dotCMS', 'core', [{ sha: AAA }])).toEqual([2]);
  });

  it('batches large ranges instead of one request per commit', async () => {
    const commits: CommitInfo[] = Array.from({ length: 120 }, (_, i) => ({
      sha: i.toString(16).padStart(40, '0'),
    }));
    const graphql = jest.fn(async () => ({ repository: {} }));
    const octokit = { graphql } as unknown as Octokit;

    await resolvePRNumbers(octokit, 'dotCMS', 'core', commits);
    // 120 commits at COMMIT_BATCH=50 -> 3 queries, not 120.
    expect(graphql).toHaveBeenCalledTimes(3);
  });

  it('skips anything that is not a real oid rather than interpolating it', async () => {
    const graphql = jest.fn(async () => ({ repository: {} }));
    const octokit = { graphql } as unknown as Octokit;

    await resolvePRNumbers(octokit, 'dotCMS', 'core', [
      { sha: '") { __typename } evil: object(oid: "' },
      { sha: 'abc' },
    ]);
    expect(graphql).not.toHaveBeenCalled();
  });

  it('propagates GraphQL failures instead of silently dropping the batch', async () => {
    const graphql = jest.fn(async () => {
      throw new Error('502 Bad Gateway');
    });
    const octokit = { graphql } as unknown as Octokit;

    await expect(
      resolvePRNumbers(octokit, 'dotCMS', 'core', [{ sha: AAA }])
    ).rejects.toThrow('502 Bad Gateway');
  });
});

describe('listStandardReleaseTags', () => {
  /** Stand-in for octokit.paginate.iterator over repos.listReleases. */
  function mockOctokit(data: { tag_name: string; draft: boolean; body: string | null }[]) {
    return {
      repos: { listReleases: {} },
      paginate: { iterator: () => [{ data }][Symbol.iterator]() },
    } as unknown as Octokit;
  }

  it('skips drafts, which listReleases returns first regardless of date (#37138)', async () => {
    // Live state on dotCMS/core: 6 drafts match STANDARD_RELEASE_PATTERN and
    // one of them duplicates a published tag, so an unfiltered list both
    // mis-orders the walk-back and makes findIndex ambiguous.
    const tags = await listStandardReleaseTags(
      mockOctokit([
        { tag_name: 'v26.04.11-02', draft: true, body: null },
        { tag_name: 'v26.08.19-04', draft: false, body: 'notes' },
        { tag_name: 'v26.04.11-02', draft: false, body: 'notes' },
      ]),
      'dotCMS',
      'core'
    );

    expect(tags).toEqual([
      { tag: 'v26.08.19-04', hasNotes: true },
      { tag: 'v26.04.11-02', hasNotes: true },
    ]);
  });

  it('ignores non-standard tags and reports an empty body as undocumented', async () => {
    const tags = await listStandardReleaseTags(
      mockOctokit([
        { tag_name: 'dotcms-cli-1.2.3', draft: false, body: 'notes' },
        { tag_name: 'v26.08.19-04_lts_01', draft: false, body: 'notes' },
        { tag_name: 'v26.08.19-03', draft: false, body: '   ' },
      ]),
      'dotCMS',
      'core'
    );

    expect(tags).toEqual([{ tag: 'v26.08.19-03', hasNotes: false }]);
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
