import { classifyExclusion, isDocumentationPath } from './exclusions';
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

describe('isDocumentationPath', () => {
  it.each([
    'specs/37376-uve-edit-pencil-permission/spec.md',
    'specs/36985-block-editor-selection-guard/contracts/report.schema.json',
    'docs/backend/JAVA_STANDARDS.md',
    'docs/images/architecture.png',
    'README.md',
    'CONTRIBUTING.md',
    'core-web/libs/sdk/react/CHANGELOG.MD',
    'examples/nextjs/docs.mdx',
  ])('treats %s as documentation', (path) => {
    expect(isDocumentationPath(path)).toBe(true);
  });

  it.each([
    'dotCMS/src/main/java/com/dotcms/rest/Foo.java',
    'core-web/libs/dotcms-ui/src/lib/foo.component.html',
    'bom/application/pom.xml',
    '.github/workflows/cicd_6-release.yml',
  ])('treats %s as implementation', (path) => {
    expect(isDocumentationPath(path)).toBe(false);
  });

  // Agent/dev tooling ships as markdown in this repo but is the deliverable,
  // not prose about one — PR #37309 added a whole Claude skill in markdown alone.
  it.each([
    '.claude/skills/dot-test-plan/SKILL.md',
    '.claude/commands/create-issue.md',
    '.agents/skills/angular-developer/references/signals.md',
    '.cursor/rules/java.mdc',
    '.specify/memory/constitution.md',
    '.specify/templates/tasks-template.md',
    'CLAUDE.md',
    'core-web/CLAUDE.md',
    'dotCMS/src/main/java/com/dotcms/rest/CLAUDE.md',
    'core-web/apps/dotcms-ui/AGENTS.md',
  ])('keeps agent tooling %s in QA scope', (path) => {
    expect(isDocumentationPath(path)).toBe(false);
  });
});

describe('classifyExclusion — path heuristics', () => {
  it('excludes a Spec-Kit PR 1 as spec-only', () => {
    expect(
      classifyExclusion(
        pr({
          title: 'Spec: UVE contentlet permission gating',
          changedFiles: [
            'specs/37376-uve-edit-pencil-permission/spec.md',
            'specs/37376-uve-edit-pencil-permission/data-model.md',
          ],
        })
      )
    ).toEqual({ excluded: true, reason: 'spec-only' });
  });

  it('excludes a pure docs PR as docs-only', () => {
    expect(
      classifyExclusion(pr({ changedFiles: ['docs/core/SPEC_KIT_QUICK_START.md', 'README.md'] }))
    ).toEqual({ excluded: true, reason: 'docs-only' });
  });

  it('reports specs mixed with other docs as docs-only, not spec-only', () => {
    expect(
      classifyExclusion(
        pr({ changedFiles: ['specs/37376-uve-edit-pencil-permission/spec.md', 'README.md'] })
      )
    ).toEqual({ excluded: true, reason: 'docs-only' });
  });

  it('keeps a PR that ships a spec alongside implementation', () => {
    expect(
      classifyExclusion(
        pr({
          changedFiles: [
            'specs/37376-uve-edit-pencil-permission/spec.md',
            'dotCMS/src/main/java/com/dotcms/rest/ContentResource.java',
          ],
        })
      )
    ).toEqual({ excluded: false });
  });

  it('keeps a markdown-only PR that ships agent tooling', () => {
    // PR #37309: `feat(skills): add dot-pr-spec-summary` — markdown, but a feature.
    expect(
      classifyExclusion(
        pr({ changedFiles: ['.claude/skills/dot-pr-spec-summary/SKILL.md', 'CLAUDE.md'] })
      )
    ).toEqual({ excluded: false });
  });

  it('keeps a PR whose file list is unknown', () => {
    // Fetch failed or the PR has more files than one page holds. Over-report
    // rather than silently hide what might be a code change.
    expect(classifyExclusion(pr({ changedFiles: undefined }))).toEqual({ excluded: false });
    expect(classifyExclusion(pr({ changedFiles: [] }))).toEqual({ excluded: false });
  });

  it('prefers an earlier reason over the path check', () => {
    // A bot that only writes specs is still excluded as a bot.
    expect(
      classifyExclusion(
        pr({ authorType: 'Bot', changedFiles: ['specs/37376-foo/spec.md'] })
      )
    ).toEqual({ excluded: true, reason: 'bot-author' });
  });
});
