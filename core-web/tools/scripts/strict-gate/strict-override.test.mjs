/**
 * T013 — strictness is genuinely in force despite an inherited `strict: false`.
 *
 * This is the premise the whole spike rests on (FR-003). If it ever stops holding — a TypeScript
 * upgrade changing option precedence, say — everything downstream reports zero findings and looks
 * healthy. That is why it is asserted directly rather than inferred from the end-to-end result.
 */
import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs/promises';
import path from 'node:path';
import { makeWorkspace } from './fixtures/make-workspace.mjs';
import { checkTypeScript } from './lib/check-ts.mjs';
import { checkAngularTemplates } from './lib/check-ng.mjs';
import { makeNgProject } from './fixtures/make-ng-project.mjs';

const violations = {
    name: 'loose',
    root: 'libs/loose',
    files: {
        'src/index.ts': [
            'export function implicitAny(value) {',       // TS7006 under noImplicitAny
            '    return value;',
            '}',
            '',
            'export function possiblyNull(input: string | null) {',
            '    return input.length;',                    // TS18047 under strictNullChecks
            '}',
            '',
            'export function fromIndexSignature(env: Record<string, string>) {',
            '    return env.CI;',                          // TS4111 — NOT part of --strict
            '}',
            ''
        ].join('\n')
    }
};

test('forces strict on a project whose base config sets strict: false', async (t) => {
    const ws = await makeWorkspace({ projects: [violations], strict: false });
    t.after(() => ws.cleanup());

    const { diagnostics } = await checkTypeScript({
        workspaceDir: ws.dir,
        configPath: path.join(ws.dir, 'libs/loose/tsconfig.lib.json'),
        flagSet: 'strict'
    });

    const codes = diagnostics.map((d) => d.code);
    assert.ok(codes.includes('TS7006'), `expected an implicit-any error, got ${codes.join(', ')}`);
    assert.ok(codes.includes('TS18047'), `expected a possibly-null error, got ${codes.join(', ')}`);
});

test("the repo's strict convention includes noPropertyAccessFromIndexSignature", async (t) => {
    // Measured, not assumed: TS4111 is NOT one of the flags `--strict` turns on. The 22 projects
    // that opted into strict all declare noPropertyAccessFromIndexSignature alongside it, so the
    // gate's "strict" must mean the repo's convention or it under-reports real debt.
    const ws = await makeWorkspace({ projects: [violations], strict: false });
    t.after(() => ws.cleanup());

    const { diagnostics } = await checkTypeScript({
        workspaceDir: ws.dir,
        configPath: path.join(ws.dir, 'libs/loose/tsconfig.lib.json'),
        flagSet: 'strict'
    });

    assert.ok(
        diagnostics.some((d) => d.code === 'TS4111'),
        'the repo convention must catch index-signature property access'
    );
});

test('the narrow flag set reports strictly fewer codes than the full one', async (t) => {
    const ws = await makeWorkspace({ projects: [violations], strict: false });
    t.after(() => ws.cleanup());

    const configPath = path.join(ws.dir, 'libs/loose/tsconfig.lib.json');
    const full = await checkTypeScript({ workspaceDir: ws.dir, configPath, flagSet: 'strict' });
    const narrow = await checkTypeScript({ workspaceDir: ws.dir, configPath, flagSet: 'null-checks' });

    const fullCodes = new Set(full.diagnostics.map((d) => d.code));
    const narrowCodes = new Set(narrow.diagnostics.map((d) => d.code));

    assert.ok(narrowCodes.has('TS18047'), 'null-checks must still catch possibly-null');
    assert.ok(!narrowCodes.has('TS4111'), 'null-checks must not include the index-signature rule');
    for (const code of narrowCodes) {
        assert.ok(fullCodes.has(code), `${code} appeared under the narrow set but not the full one`);
    }
});

test('leaves every configuration file byte-identical', async (t) => {
    const ws = await makeWorkspace({ projects: [violations], strict: false });
    t.after(() => ws.cleanup());

    const configPath = path.join(ws.dir, 'libs/loose/tsconfig.lib.json');
    const read = async (p) => fs.readFile(p, 'utf8');
    const before = {
        base: await read(path.join(ws.dir, 'tsconfig.base.json')),
        lib: await read(configPath),
        root: await read(path.join(ws.dir, 'libs/loose/tsconfig.json'))
    };

    await checkTypeScript({ workspaceDir: ws.dir, configPath, flagSet: 'strict' });

    assert.equal(await read(path.join(ws.dir, 'tsconfig.base.json')), before.base);
    assert.equal(await read(configPath), before.lib);
    assert.equal(await read(path.join(ws.dir, 'libs/loose/tsconfig.json')), before.root);

    const stray = (await fs.readdir(path.join(ws.dir, 'libs/loose'))).filter((f) =>
        f.startsWith('tsconfig.') && !['tsconfig.json', 'tsconfig.lib.json', 'tsconfig.spec.json'].includes(f)
    );
    assert.deepEqual(stray, [], 'no overlay config may be left behind — SC-010');
});

/* ── T050 (US4) — Angular template strictness ───────────────────────────────
 * The premise of the template arm, and it is a DIFFERENT mechanism from the TypeScript one:
 * Angular's settings are not TypeScript compiler options, and its command-line parser rejects
 * them outright (verified against the pinned compiler: only i18nFile, i18nFormat, locale,
 * missingTranslation and watch are tolerated). They can only be supplied through configuration
 * the compiler reads — and this harness does that in memory, so nothing is written anywhere.
 */

test('forces template strictness on a project whose config sets strictTemplates: false', async (t) => {
    const ng = await makeNgProject({ strictTemplates: false, withViolations: true });
    t.after(() => ng.cleanup());

    const { diagnostics } = await checkAngularTemplates({
        configPath: path.join(ng.dir, ng.root, 'tsconfig.lib.json'),
        flagSet: 'strict'
    });

    assert.ok(diagnostics.length > 0, 'a number bound to a string input must fail under strictTemplates');
    assert.ok(
        diagnostics.some((d) => d.layer === 'template'),
        `expected a template-layer diagnostic; got ${diagnostics.map((d) => `${d.code}/${d.layer}`).join(', ')}`
    );
});

test('the same project reports nothing when template strictness is left off', async (t) => {
    // Establishes that the findings above are CAUSED by forcing the setting, rather than being
    // pre-existing breakage the fixture happened to contain.
    const ng = await makeNgProject({ strictTemplates: false, withViolations: true });
    t.after(() => ng.cleanup());

    const { diagnostics } = await checkAngularTemplates({
        configPath: path.join(ng.dir, ng.root, 'tsconfig.lib.json'),
        flagSet: 'strict',
        forceTemplates: false
    });

    assert.equal(
        diagnostics.filter((d) => d.layer === 'template').length,
        0,
        'without forcing, the project compiles as it does today'
    );
});

test('a separate-file template diagnostic is attributed to the .html file', async (t) => {
    const ng = await makeNgProject({ strictTemplates: false, withViolations: true });
    t.after(() => ng.cleanup());

    const { diagnostics } = await checkAngularTemplates({
        configPath: path.join(ng.dir, ng.root, 'tsconfig.lib.json'),
        flagSet: 'strict'
    });

    assert.ok(
        diagnostics.some((d) => d.file.endsWith('separate.component.html')),
        'a violation in an external template belongs to the template file, not the component'
    );
});

test('an inline template diagnostic is attributed to the component source', async (t) => {
    // There is no template file to blame, so the diagnostic must land on the .ts — otherwise the
    // diff filter compares against a path that does not exist and silently drops a real finding.
    const ng = await makeNgProject({ strictTemplates: false, withViolations: true });
    t.after(() => ng.cleanup());

    const { diagnostics } = await checkAngularTemplates({
        configPath: path.join(ng.dir, ng.root, 'tsconfig.lib.json'),
        flagSet: 'strict'
    });

    assert.ok(
        diagnostics.some((d) => d.file.endsWith('inline.component.ts')),
        'an inline template violation belongs to the component source file'
    );
});

test('template-aware checking leaves every configuration file byte-identical', async (t) => {
    const ng = await makeNgProject({ strictTemplates: false });
    t.after(() => ng.cleanup());

    const configPath = path.join(ng.dir, ng.root, 'tsconfig.lib.json');
    const rootConfig = path.join(ng.dir, ng.root, 'tsconfig.json');
    const before = { lib: await fs.readFile(configPath, 'utf8'), root: await fs.readFile(rootConfig, 'utf8') };

    await checkAngularTemplates({ configPath, flagSet: 'strict' });

    assert.equal(await fs.readFile(configPath, 'utf8'), before.lib);
    assert.equal(await fs.readFile(rootConfig, 'utf8'), before.root, 'strictTemplates:false must still say false');
});
