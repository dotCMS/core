import { classifyExclusion } from './exclusions';
import { PRDetails } from './types';

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

describe('classifyExclusion', () => {
  it('keeps a regular user-authored PR', () => {
    expect(classifyExclusion(pr())).toEqual({ excluded: false });
  });

  it('excludes a Bot authorType', () => {
    expect(classifyExclusion(pr({ authorType: 'Bot' }))).toEqual({
      excluded: true,
      reason: 'bot-author',
    });
  });

  it('excludes a [bot]-suffixed login', () => {
    expect(classifyExclusion(pr({ author: 'dependabot[bot]' }))).toEqual({
      excluded: true,
      reason: 'bot-author',
    });
  });

  it('excludes a known bot prefix even without [bot] suffix', () => {
    expect(classifyExclusion(pr({ author: 'renovate-preview' }))).toEqual({
      excluded: true,
      reason: 'bot-author',
    });
  });

  it('does NOT exclude a user whose login merely contains a bot keyword (was a bug)', () => {
    // "github-actions-fan" must not be matched — the previous .includes()
    // check was loose; we now require startsWith on the prefix list.
    expect(classifyExclusion(pr({ author: 'fan-of-github-actions' }))).toEqual({
      excluded: false,
    });
  });

  it('excludes a PR with a dependencies label', () => {
    expect(classifyExclusion(pr({ labels: ['Dependencies'] }))).toEqual({
      excluded: true,
      reason: 'dependency-bump',
    });
  });

  it('excludes a "Bump …" title', () => {
    expect(classifyExclusion(pr({ title: 'Bump lodash from 4.17.20 to 4.17.21' }))).toEqual({
      excluded: true,
      reason: 'version-bump',
    });
  });

  it('excludes "chore(deps): …" titles', () => {
    expect(classifyExclusion(pr({ title: 'chore(deps): update foo' }))).toEqual({
      excluded: true,
      reason: 'version-bump',
    });
  });

  it('excludes release-machinery titles', () => {
    expect(classifyExclusion(pr({ title: 'Release v26.05.19-01' }))).toEqual({
      excluded: true,
      reason: 'release-machinery',
    });
  });
});
