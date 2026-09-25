import { findPreviousTag, parseChangedFilesResponse } from './github';

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

describe('parseChangedFilesResponse', () => {
  const files = (paths: string[], hasNextPage = false) => ({
    files: { nodes: paths.map((path) => ({ path })), pageInfo: { hasNextPage } },
  });

  it('maps each alias to its paths', () => {
    const data = { repository: { pr1: files(['specs/foo/spec.md', 'README.md']) } };
    expect(parseChangedFilesResponse(data, [1])).toEqual(
      new Map([[1, ['specs/foo/spec.md', 'README.md']]])
    );
  });

  // Everything below must yield `undefined`, never `[]`. An empty array reads as
  // "changed nothing", which classifyExclusion would be free to exclude on; the
  // point of these cases is that we do not know, so the PR stays in QA scope.
  it('returns undefined when the list is truncated', () => {
    const spy = jest.spyOn(process.stderr, 'write').mockImplementation(() => true);
    const data = { repository: { pr2: files(['a.md'], true) } };
    expect(parseChangedFilesResponse(data, [2]).get(2)).toBeUndefined();
    expect(spy).toHaveBeenCalledWith(expect.stringContaining('#2'));
    spy.mockRestore();
  });

  it('returns undefined for a missing alias, a null PR, and a null files connection', () => {
    const data = { repository: { pr4: null, pr5: { files: null } } };
    const out = parseChangedFilesResponse(data, [3, 4, 5]);
    expect(out.get(3)).toBeUndefined();
    expect(out.get(4)).toBeUndefined();
    expect(out.get(5)).toBeUndefined();
    // Present-but-unknown, not absent: classifyExclusion reads every PR.
    expect(out.size).toBe(3);
  });
});
