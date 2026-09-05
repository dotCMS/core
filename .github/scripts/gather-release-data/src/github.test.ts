import { extractLinkedIssues } from './github';

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
