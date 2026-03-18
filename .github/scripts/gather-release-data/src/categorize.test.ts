import { shouldSkip, isRollbackUnsafe, categorize, processChanges } from './categorize';
import { PRDetails } from './types';

describe('shouldSkip', () => {
  it('returns true for Changelog: Skip label', () => {
    expect(shouldSkip(['Changelog: Skip'])).toBe(true);
  });

  it('is case-insensitive', () => {
    expect(shouldSkip(['changelog: skip'])).toBe(true);
  });

  it('returns false for unrelated labels', () => {
    expect(shouldSkip(['bug', 'Area : Backend'])).toBe(false);
  });

  it('returns false for empty labels', () => {
    expect(shouldSkip([])).toBe(false);
  });
});

describe('isRollbackUnsafe', () => {
  it('detects AI: Not Safe To Rollback', () => {
    expect(isRollbackUnsafe(['AI: Not Safe To Rollback'])).toBe(true);
  });

  it('detects Human: Not Safe To Rollback', () => {
    expect(isRollbackUnsafe(['Human: Not Safe To Rollback'])).toBe(true);
  });

  it('returns false for safe PRs', () => {
    expect(isRollbackUnsafe(['bug', 'feature'])).toBe(false);
  });
});

describe('categorize', () => {
  const makePR = (overrides: Partial<PRDetails>): PRDetails => ({
    number: 100,
    title: 'Test PR',
    labels: [],
    body: '',
    linkedIssues: [],
    ...overrides,
  });

  it('categorizes by feature label', () => {
    expect(categorize(makePR({ labels: ['feature'] }))).toBe('feature');
    expect(categorize(makePR({ labels: ['enhancement'] }))).toBe('feature');
  });

  it('categorizes by bug label', () => {
    expect(categorize(makePR({ labels: ['bug'] }))).toBe('fix');
    expect(categorize(makePR({ labels: ['defect'] }))).toBe('fix');
  });

  it('categorizes by infrastructure label', () => {
    expect(categorize(makePR({ labels: ['infrastructure'] }))).toBe('infrastructure');
    expect(categorize(makePR({ labels: ['Area : CI/CD'] }))).toBe('infrastructure');
  });

  it('categorizes by deprecation label', () => {
    expect(categorize(makePR({ labels: ['deprecation'] }))).toBe('deprecation');
    expect(categorize(makePR({ labels: ['breaking change'] }))).toBe('deprecation');
  });

  it('falls back to title-based heuristics', () => {
    expect(categorize(makePR({ title: 'feat: add new widget' }))).toBe('feature');
    expect(categorize(makePR({ title: 'fix: resolve crash' }))).toBe('fix');
    expect(categorize(makePR({ title: 'chore: update deps' }))).toBe('infrastructure');
    expect(categorize(makePR({ title: 'build: upgrade Maven wrapper' }))).toBe('infrastructure');
  });

  it('detects release machinery as internal', () => {
    expect(categorize(makePR({ title: 'Bump version to 26.03.14' }))).toBe('internal');
    expect(categorize(makePR({ title: 'Update LICENSE Change Date' }))).toBe('internal');
  });

  it('deprecation takes priority over feature', () => {
    expect(
      categorize(makePR({ labels: ['deprecation', 'feature'] }))
    ).toBe('deprecation');
  });
});

describe('processChanges', () => {
  it('separates changes, rollback-unsafe, and skipped', () => {
    const prDetails = new Map<number, PRDetails>([
      [1, { number: 1, title: 'New feature', labels: ['feature'], body: '', linkedIssues: [] }],
      [2, { number: 2, title: 'Bug fix', labels: ['bug'], body: '', linkedIssues: [] }],
      [3, { number: 3, title: 'Dangerous migration', labels: ['feature', 'AI: Not Safe To Rollback'], body: '', linkedIssues: [] }],
      [4, { number: 4, title: 'Internal only', labels: ['Changelog: Skip'], body: '', linkedIssues: [] }],
      [5, { number: 5, title: 'Bump version to 1.0', labels: [], body: '', linkedIssues: [] }],
    ]);

    const result = processChanges(prDetails);

    expect(result.changes).toHaveLength(3); // feature, fix, dangerous migration
    expect(result.rollbackUnsafe).toHaveLength(1);
    expect(result.rollbackUnsafe[0].pr).toBe(3);
    expect(result.skipped).toEqual([4]);
    // PR #5 (bump version) is internal and excluded from changes
    expect(result.changes.find((c) => c.pr === 5)).toBeUndefined();
  });

  it('rollback-unsafe PRs still appear in changes (not skipped)', () => {
    const prDetails = new Map<number, PRDetails>([
      [1, { number: 1, title: 'Schema migration', labels: ['feature', 'Human: Not Safe To Rollback'], body: '', linkedIssues: [] }],
    ]);

    const result = processChanges(prDetails);

    expect(result.changes).toHaveLength(1);
    expect(result.rollbackUnsafe).toHaveLength(1);
    expect(result.skipped).toHaveLength(0);
  });
});
