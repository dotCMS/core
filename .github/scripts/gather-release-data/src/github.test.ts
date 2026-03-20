import { extractPRNumbers, findPreviousTag, extractLinkedIssues, parseRepo } from './github';
import { CommitInfo } from './types';

describe('parseRepo', () => {
  it('parses owner/repo format', () => {
    expect(parseRepo('dotCMS/core')).toEqual({ owner: 'dotCMS', repo: 'core' });
  });

  it('throws for invalid format', () => {
    expect(() => parseRepo('invalid')).toThrow('Invalid repo format');
  });
});

describe('extractPRNumbers', () => {
  it('extracts PR numbers from standard merge commits', () => {
    const commits: CommitInfo[] = [
      { sha: 'abc', message: 'fix(SiteSearch): resolve timeout (#34879)' },
      { sha: 'def', message: 'feat: add new widget (#34880)' },
      { sha: 'ghi', message: 'chore: no PR reference here' },
    ];

    const prNumbers = extractPRNumbers(commits);
    expect(prNumbers).toEqual(expect.arrayContaining([34879, 34880]));
    expect(prNumbers).toHaveLength(2);
  });

  it('deduplicates PR numbers', () => {
    const commits: CommitInfo[] = [
      { sha: 'abc', message: 'first commit (#100)' },
      { sha: 'def', message: 'second commit (#100)' },
    ];

    expect(extractPRNumbers(commits)).toEqual([100]);
  });

  it('handles empty commits', () => {
    expect(extractPRNumbers([])).toEqual([]);
  });
});

describe('findPreviousTag', () => {
  const tags = ['v26.03.13-02', 'v26.03.13-01', 'v26.03.12-01', 'v26.03.11-01'];

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
