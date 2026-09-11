/**
 * T012 — choosing the configuration that actually includes the changed file.
 *
 * This is the test that guards the spike's most expensive possible mistake. Two of the five real
 * violations in the acceptance case live in a `.spec.ts`, and a third is visible from BOTH the lib
 * and spec configurations. A filename-convention heuristic ("lib first") reports zero on that case
 * — the harness looks like it works while silently under-reporting, and the spike ships a false
 * number. Selection is therefore by resolved file list, never by naming.
 */
import test from 'node:test';
import assert from 'node:assert/strict';
import { makeWorkspace } from './fixtures/make-workspace.mjs';
import { selectConfigs } from './lib/config-select.mjs';

const project = {
    name: 'thing',
    root: 'libs/thing',
    shape: 'references',
    files: {
        'src/index.ts': 'export const a = 1;\n',
        'src/thing.spec.ts': 'export const s = 1;\n'
    }
};

test('a source file selects the lib configuration', async (t) => {
    const ws = await makeWorkspace({ projects: [project] });
    t.after(() => ws.cleanup());

    const selected = await selectConfigs({
        workspaceDir: ws.dir,
        project: { name: 'thing', root: 'libs/thing' },
        files: ['libs/thing/src/index.ts']
    });

    assert.equal(selected.length, 1);
    assert.match(selected[0].configPath, /tsconfig\.lib\.json$/);
});

test('a spec file selects the spec configuration', async (t) => {
    const ws = await makeWorkspace({ projects: [project] });
    t.after(() => ws.cleanup());

    const selected = await selectConfigs({
        workspaceDir: ws.dir,
        project: { name: 'thing', root: 'libs/thing' },
        files: ['libs/thing/src/thing.spec.ts']
    });

    assert.equal(selected.length, 1, 'a lib-first heuristic would return zero configs here');
    assert.match(selected[0].configPath, /tsconfig\.spec\.json$/);
});

test('a references-only configuration resolving to zero files is never selected', async (t) => {
    const ws = await makeWorkspace({ projects: [project] });
    t.after(() => ws.cleanup());

    const selected = await selectConfigs({
        workspaceDir: ws.dir,
        project: { name: 'thing', root: 'libs/thing' },
        files: ['libs/thing/src/index.ts', 'libs/thing/src/thing.spec.ts']
    });

    for (const target of selected) {
        assert.doesNotMatch(
            target.configPath,
            /libs\/thing\/tsconfig\.json$/,
            'the root config owns no files and must exclude itself with no special-casing'
        );
    }
});

test('a file claimed by two configurations produces both targets so diagnostics can be deduplicated', async (t) => {
    // Real case: src/utils/index.ts in sdk-create-app reports TS7030 under BOTH the lib and the
    // spec configuration. Selection must surface both; report assembly deduplicates by
    // file/line/code so the finding is counted once.
    const shared = {
        name: 'shared',
        root: 'libs/shared',
        shape: 'lib',
        files: { 'src/index.ts': 'export const a = 1;\n', 'src/a.spec.ts': "import './index';\n" }
    };
    const ws = await makeWorkspace({ projects: [shared] });
    t.after(() => ws.cleanup());

    const selected = await selectConfigs({
        workspaceDir: ws.dir,
        project: { name: 'shared', root: 'libs/shared' },
        files: ['libs/shared/src/index.ts', 'libs/shared/src/a.spec.ts']
    });

    assert.equal(selected.length, 2);
    assert.deepEqual(
        selected.map((s) => s.configPath.split('/').pop()).sort(),
        ['tsconfig.lib.json', 'tsconfig.spec.json']
    );
});

/* ── Template files ─────────────────────────────────────────────────────────
 * A tsconfig's resolved file list contains only TypeScript. A template is never in it, so the
 * file-list rule that works for sources finds nothing for a .html — and a pull request that
 * touches only templates resolves ZERO projects and passes silently. That is the exact failure
 * the spec's "template-only change" edge case names, and it is invisible without these tests:
 * the run reports PASS with no targets, which reads like "nothing to check".
 *
 * A template belongs to the component that references it, and Angular convention colocates the
 * two. Attaching a template to the config that owns TypeScript in its own directory is cheap and
 * correct in practice; resolving templateUrl properly would mean compiling to find out what to
 * compile.
 */

test('a template file selects the config that owns TypeScript in its directory', async (t) => {
    const ws = await makeWorkspace({
        projects: [
            {
                name: 'ngish',
                root: 'libs/ngish',
                shape: 'lib',
                files: {
                    'src/index.ts': 'export const a = 1;\n',
                    'src/thing.component.ts': 'export class Thing {}\n',
                    'src/thing.component.html': '<span></span>\n'
                }
            }
        ]
    });
    t.after(() => ws.cleanup());

    const selected = await selectConfigs({
        workspaceDir: ws.dir,
        project: { name: 'ngish', root: 'libs/ngish' },
        files: ['libs/ngish/src/thing.component.html']
    });

    assert.equal(selected.length, 1, 'a template-only change must still resolve a config');
    assert.match(selected[0].configPath, /tsconfig\.lib\.json$/);
});

test('a template with no sibling TypeScript still resolves to the project’s primary config', async (t) => {
    const ws = await makeWorkspace({
        projects: [
            {
                name: 'ngish',
                root: 'libs/ngish',
                shape: 'lib',
                files: {
                    'src/index.ts': 'export const a = 1;\n',
                    'src/templates/orphan.html': '<span></span>\n'
                }
            }
        ]
    });
    t.after(() => ws.cleanup());

    const selected = await selectConfigs({
        workspaceDir: ws.dir,
        project: { name: 'ngish', root: 'libs/ngish' },
        files: ['libs/ngish/src/templates/orphan.html']
    });

    assert.ok(selected.length >= 1, 'never drop a template silently — that reads as "nothing to check"');
});

test('a mixed diff attaches the template alongside its sources', async (t) => {
    const ws = await makeWorkspace({
        projects: [
            {
                name: 'ngish',
                root: 'libs/ngish',
                shape: 'lib',
                files: {
                    'src/index.ts': 'export const a = 1;\n',
                    'src/thing.component.ts': 'export class Thing {}\n',
                    'src/thing.component.html': '<span></span>\n'
                }
            }
        ]
    });
    t.after(() => ws.cleanup());

    const selected = await selectConfigs({
        workspaceDir: ws.dir,
        project: { name: 'ngish', root: 'libs/ngish' },
        files: ['libs/ngish/src/thing.component.ts', 'libs/ngish/src/thing.component.html']
    });

    const lib = selected.find((s) => s.configPath.endsWith('tsconfig.lib.json'));
    assert.ok(lib.files.includes('libs/ngish/src/thing.component.html'));
    assert.ok(lib.files.includes('libs/ngish/src/thing.component.ts'));
});

/* ── Entry-point configs ────────────────────────────────────────────────────
 * `apps/dotcms-ui/tsconfig.app.json` declares `"files": ["src/main.ts", "src/polyfills.ts"]`.
 * Its RESOLVED file list is therefore two entries — every component arrives through the import
 * graph, not through a glob. The file-list rule never selects it, so the app's own sources and
 * templates fall through to `tsconfig.editor.json`, an IDE-only config Nx generates that carries
 * no `angularCompilerOptions`. The file still gets checked, which is why this hid: the run looks
 * healthy while template strictness is silently unreachable for the largest application.
 *
 * Selection therefore ranks candidates rather than taking the first that matches.
 */

test('an entry-point config is preferred over an IDE-only config for its own sources', async (t) => {
    const ws = await makeWorkspace({ projects: [{ name: 'app', root: 'apps/app', files: {} }] });
    t.after(() => ws.cleanup());

    const fs = await import('node:fs/promises');
    const path = await import('node:path');
    const write = (rel, obj) =>
        fs.writeFile(path.join(ws.dir, 'apps/app', rel), JSON.stringify(obj, null, 4), 'utf8');

    // An application has an app config and an editor config — not the lib/spec pair the generic
    // fixture emits. Remove them so the layout matches apps/dotcms-ui, which is what this covers.
    for (const generated of ['tsconfig.lib.json', 'tsconfig.spec.json', 'tsconfig.json']) {
        await fs.rm(path.join(ws.dir, 'apps/app', generated), { force: true });
    }

    await fs.mkdir(path.join(ws.dir, 'apps/app/src/feature'), { recursive: true });
    await fs.writeFile(path.join(ws.dir, 'apps/app/src/main.ts'), "import './feature/x.component';\n");
    await fs.writeFile(path.join(ws.dir, 'apps/app/src/feature/x.component.ts'), 'export class X {}\n');
    await fs.writeFile(path.join(ws.dir, 'apps/app/src/feature/x.component.html'), '<span></span>\n');

    await write('tsconfig.app.json', {
        extends: '../../tsconfig.base.json',
        files: ['src/main.ts'],
        angularCompilerOptions: { strictTemplates: false }
    });
    await write('tsconfig.editor.json', {
        extends: '../../tsconfig.base.json',
        include: ['src/**/*.ts']
    });

    const selected = await selectConfigs({
        workspaceDir: ws.dir,
        project: { name: 'app', root: 'apps/app' },
        files: ['apps/app/src/feature/x.component.ts', 'apps/app/src/feature/x.component.html']
    });

    const chosen = selected.map((s) => s.configPath.split('/').pop());
    assert.ok(
        chosen.includes('tsconfig.app.json'),
        `expected the app config to be selected; got ${chosen.join(', ')}`
    );
    assert.ok(
        !chosen.includes('tsconfig.editor.json'),
        'an IDE-only config must never stand in for the build config — it carries no Angular settings'
    );
});

test('a template is never attached to a spec config just because a spec file sits beside it', async (t) => {
    // Reproduces apps/dotcms-ui exactly: the app config lists only entry points, so it can never
    // be found by "owns TypeScript in this directory" — while the spec config CAN, because Angular
    // colocates x.component.ts, x.component.html and x.component.spec.ts. The spec config carries
    // no angularCompilerOptions, so the template silently goes unchecked while the run reports a
    // target and a PASS. Alphabetical candidate order hid this in a lib-shaped fixture.
    const ws = await makeWorkspace({ projects: [{ name: 'app', root: 'apps/app', files: {} }] });
    t.after(() => ws.cleanup());

    const fs = await import('node:fs/promises');
    const path = await import('node:path');
    for (const generated of ['tsconfig.lib.json', 'tsconfig.json']) {
        await fs.rm(path.join(ws.dir, 'apps/app', generated), { force: true });
    }
    await fs.mkdir(path.join(ws.dir, 'apps/app/src/feature'), { recursive: true });
    await fs.writeFile(path.join(ws.dir, 'apps/app/src/main.ts'), "import './feature/x.component';\n");
    await fs.writeFile(path.join(ws.dir, 'apps/app/src/feature/x.component.ts'), 'export class X {}\n');
    await fs.writeFile(path.join(ws.dir, 'apps/app/src/feature/x.component.spec.ts'), "import './x.component';\n");
    await fs.writeFile(path.join(ws.dir, 'apps/app/src/feature/x.component.html'), '<span></span>\n');
    await fs.writeFile(
        path.join(ws.dir, 'apps/app/tsconfig.app.json'),
        JSON.stringify({
            extends: '../../tsconfig.base.json',
            files: ['src/main.ts'],
            angularCompilerOptions: { strictTemplates: false }
        })
    );
    await fs.writeFile(
        path.join(ws.dir, 'apps/app/tsconfig.spec.json'),
        JSON.stringify({ extends: '../../tsconfig.base.json', include: ['src/**/*.spec.ts'] })
    );

    const selected = await selectConfigs({
        workspaceDir: ws.dir,
        project: { name: 'app', root: 'apps/app' },
        files: ['apps/app/src/feature/x.component.html']
    });

    assert.equal(selected.length, 1);
    assert.match(
        selected[0].configPath,
        /tsconfig\.app\.json$/,
        `a template belongs to the build config, not the spec config; got ${selected[0].configPath}`
    );
});
