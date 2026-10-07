import { findDeclaredIssue } from './links';

describe('findDeclaredIssue', () => {
  it('finds a non-closing "Refs #N" reference (#37714)', () => {
    const body =
      'Refs #37580 (Scout M4) and #37129 (Falcon M4), under epic #37124. ' +
      'Targets a feature branch rather than `main`, so no closing keyword.';
    expect(findDeclaredIssue(body, 'docs: gap backlog', 'scout-gap-backlog')).toBe(37580);
  });

  it.each([
    ['Part of #12', 12],
    ['related to #34', 34],
    ['References: #56', 56],
    ['contributes to #78', 78],
  ])('finds "%s"', (body, expected) => {
    expect(findDeclaredIssue(body, 'title', 'branch')).toBe(expected);
  });

  it('prefers a closing keyword over a non-closing reference, as the gate does', () => {
    expect(findDeclaredIssue('Refs #1\n\nFixes: #2', 'title', 'branch')).toBe(2);
  });

  it('falls back to a trailing "(#N)" on the title', () => {
    expect(findDeclaredIssue('', 'spec: webdav copy (#713)', 'branch')).toBe(713);
    expect(findDeclaredIssue('', 'spec: (#713) webdav copy', 'branch')).toBeUndefined();
  });

  it('falls back to the branch name', () => {
    expect(findDeclaredIssue(null, 'title', '36850-foo')).toBe(36850);
    expect(findDeclaredIssue(null, 'title', 'nicobytes/36850-foo')).toBe(36850);
    expect(findDeclaredIssue(null, 'title', 'issue-42-foo')).toBe(42);
    expect(findDeclaredIssue(null, 'title', 'fix/issue-42')).toBe(42);
  });

  it('ignores merge-queue branches and bare mentions', () => {
    expect(findDeclaredIssue('see #99', 'title', 'gh-readonly-queue/main/pr-1-abc')).toBeUndefined();
  });
});
