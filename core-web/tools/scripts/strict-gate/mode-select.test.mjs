/**
 * T051 (US4) — choosing the execution mode, out loud.
 *
 * The spec is emphatic that a fallback must never be silent, and the reason is concrete: if a
 * project quietly drops to TypeScript-only, its templates go unchecked and the run still reports
 * PASS. That is indistinguishable from "the templates are fine", which is exactly the failure a
 * gate exists to prevent.
 */
import test from 'node:test';
import assert from 'node:assert/strict';
import path from 'node:path';
import { makeWorkspace } from './fixtures/make-workspace.mjs';
import { makeNgProject } from './fixtures/make-ng-project.mjs';
import { selectMode } from './lib/mode-select.mjs';

test('an Angular project selects template-aware mode', async (t) => {
    const ng = await makeNgProject({ strictTemplates: false });
    t.after(() => ng.cleanup());

    const decision = await selectMode({
        configPath: path.join(ng.dir, ng.root, 'tsconfig.lib.json'),
        templates: true
    });

    assert.equal(decision.mode, 'template-aware');
    assert.match(decision.reason, /\S/);
});

test('a non-Angular project falls back to TypeScript-only, and says so', async (t) => {
    const ws = await makeWorkspace({
        projects: [{ name: 'plain', root: 'libs/plain', files: { 'src/index.ts': 'export const a = 1;\n' } }]
    });
    t.after(() => ws.cleanup());

    const decision = await selectMode({
        configPath: path.join(ws.dir, 'libs/plain/tsconfig.lib.json'),
        templates: true
    });

    assert.equal(decision.mode, 'typescript');
    // Asserted on the REPORTED value, not on the absence of a crash: a silent skip would pass a
    // test that only checked that nothing threw.
    assert.match(decision.reason, /angular/i, 'the fallback must state why it happened');
});

test('template-aware mode is never selected when templates are not requested', async (t) => {
    const ng = await makeNgProject({ strictTemplates: false });
    t.after(() => ng.cleanup());

    const decision = await selectMode({
        configPath: path.join(ng.dir, ng.root, 'tsconfig.lib.json'),
        templates: false
    });

    assert.equal(decision.mode, 'typescript', 'the core arm stays independent of the template arm');
});
