/**
 * T011 — mapping changed files to the project that OWNS them.
 *
 * Ownership is a property of where a file lives, not of the dependency graph. `nx affected`
 * answers a different question — it returns dependents — and for a shared config that is every
 * project in the workspace. FR-010 exists because that difference is the whole cost model.
 */
import test from 'node:test';
import assert from 'node:assert/strict';
import { mapFilesToProjects } from './lib/project-map.mjs';

const PROJECTS = [
    { name: 'ui', root: 'core-web/libs/ui' },
    { name: 'portlet', root: 'core-web/libs/portlets/thing' },
    { name: 'portlet-ui', root: 'core-web/libs/portlets/thing/ui' },
    { name: 'core-web', root: 'core-web' }
];

const asChanged = (paths) => paths.map((p) => ({ path: p, status: 'M', kind: 'source', changedLines: [] }));

test('assigns each file to the longest matching project root', () => {
    const { targets } = mapFilesToProjects({
        projects: PROJECTS,
        files: asChanged([
            'core-web/libs/ui/src/a.ts',
            'core-web/libs/portlets/thing/src/b.ts',
            'core-web/libs/portlets/thing/ui/src/c.ts'
        ])
    });

    const owner = (p) => targets.find((t) => t.files.includes(p))?.project;
    assert.equal(owner('core-web/libs/ui/src/a.ts'), 'ui');
    assert.equal(owner('core-web/libs/portlets/thing/src/b.ts'), 'portlet');
    // The nested project wins over its parent — otherwise every nested lib's files would be
    // checked under the wrong configuration.
    assert.equal(owner('core-web/libs/portlets/thing/ui/src/c.ts'), 'portlet-ui');
});

test('reports a file no project claims instead of dropping it', () => {
    const { targets, unmapped } = mapFilesToProjects({
        projects: PROJECTS.filter((p) => p.name !== 'core-web'),
        files: asChanged(['docs/readme.ts', 'core-web/libs/ui/src/a.ts'])
    });

    assert.equal(targets.length, 1);
    assert.equal(unmapped.length, 1);
    assert.equal(unmapped[0].path, 'docs/readme.ts');
    assert.match(unmapped[0].reason, /\S/, 'an unmapped file must say why');
});

test('a shared-config change does not fan out to every project', () => {
    // tsconfig.base.json and nx.json are declared under nx.json's sharedGlobals, so `nx affected`
    // returns all 56 projects for this diff. The gate must stay on the owning project.
    const { targets } = mapFilesToProjects({
        projects: PROJECTS,
        files: asChanged(['core-web/tsconfig.base.json', 'core-web/nx.json'])
    });

    assert.ok(targets.length <= 1, `expected no fan-out, got ${targets.length} targets`);
    for (const target of targets) {
        assert.notEqual(target.project, 'ui');
        assert.notEqual(target.project, 'portlet');
    }
});

test('groups multiple files of one project into a single target', () => {
    const { targets } = mapFilesToProjects({
        projects: PROJECTS,
        files: asChanged(['core-web/libs/ui/src/a.ts', 'core-web/libs/ui/src/b.ts'])
    });

    assert.equal(targets.length, 1);
    assert.equal(targets[0].files.length, 2);
});
