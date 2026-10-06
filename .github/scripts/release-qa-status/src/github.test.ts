import { findPreviousTag } from './github';

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
