#!/usr/bin/env node
/**
 * Scenario harness for the diff-allowlist check — FR-001, FR-002a.
 * Encodes every row of the Red-gate table in
 * specs/37444-vitest-unit-test-migration/contracts/diff-allowlist.md.
 *
 * Usage:  node tools/__tests__/verify-test-only-diff.scenarios.mjs
 * Exit:   0 all scenarios pass · 1 one or more failed · 2 harness error
 *
 * WHY THIS FEEDS PATHS INSTEAD OF MUTATING GIT:
 * An earlier design had each scenario edit a real file and run `git diff`. That
 * makes the harness slow, order-dependent, and — worse — capable of leaving the
 * working tree dirty when a scenario throws. Instead the checker accepts a path
 * list on stdin (`--paths-from -`), so scenarios are pure data. The checker still
 * defaults to real `git diff` in normal use; this only changes where the list
 * comes from, not how it is judged.
 *
 * This harness deliberately has no dependencies and does not need node_modules:
 * it must be runnable before `pnpm install` succeeds, since it gates work that
 * precedes a working toolchain.
 */

import { spawnSync } from 'node:child_process';
import { existsSync } from 'node:fs';
import { resolve, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

const HERE = dirname(fileURLToPath(import.meta.url));
const CHECKER = resolve(HERE, '../verify-test-only-diff.mjs');

/**
 * Each scenario: the changed-path list a diff would produce, and the verdict the
 * checker must reach. `category` asserts which allowlist bucket admitted a path —
 * a boolean pass is not enough, because the reviewer relies on seeing the diff's
 * shape (contracts/diff-allowlist.md).
 */
const SCENARIOS = [
    {
        name: 'product source edit is rejected',
        paths: ['core-web/libs/ui/src/lib/dot-field-validation-message/dot-field-validation-message.component.ts'],
        expect: 'reject',
        because: 'FR-002 — the whole safety property. If this passes, nothing else matters.'
    },
    {
        name: 'E2E spec edit is rejected (deny beats allow)',
        paths: ['core-web/apps/dotcms-ui-e2e/src/tests/login/login.spec.ts'],
        expect: 'reject',
        because:
            'FR-002a — this path matches the `spec` allow pattern. If deny is not evaluated first, ' +
            'all 35 E2E specs silently become editable. This is the single most load-bearing scenario.'
    },
    {
        name: 'E2E runner config edit is rejected',
        paths: ['core-web/apps/dotcms-ui-e2e/playwright.config.ts'],
        expect: 'reject',
        because: 'FR-002a — matches the `test-config` allow pattern; must still be denied.'
    },
    {
        name: 'Stencil project edit is rejected',
        paths: ['core-web/libs/dotcms-webcomponents/stencil.config.ts'],
        expect: 'reject',
        because: 'Out of scope. Also matches `test-config`, so it needs an explicit deny.'
    },
    {
        name: 'unknown path shape is rejected, not ignored',
        paths: ['some/path/nobody/anticipated.xyz'],
        expect: 'reject',
        because:
            'A check that defaults to permitting the unfamiliar inverts its own purpose. ' +
            'New path shapes must fail loudly and be classified deliberately.'
    },
    {
        name: 'migration tooling is admitted under its own category',
        paths: ['core-web/tools/codemod-jest-to-vitest.mjs', 'core-web/tools/verify-test-only-diff.mjs'],
        expect: 'accept',
        category: 'migration-tooling',
        because: 'FR-001a — without this the check rejects the pull request that introduces it.'
    },
    {
        name: 'unit spec edit is admitted',
        paths: ['core-web/libs/ui/src/lib/dot-field-validation-message/dot-field-validation-message.component.spec.ts'],
        expect: 'accept',
        category: 'spec'
    },
    {
        name: 'test config changes are admitted',
        paths: [
            'core-web/libs/ui/jest.config.ts',
            'core-web/libs/ui/vitest-base.config.ts',
            'core-web/libs/ui/tsconfig.spec.json',
            'core-web/jest.preset.js'
        ],
        expect: 'accept',
        category: 'test-config'
    },
    {
        name: 'test support files are admitted',
        paths: ['core-web/libs/ui/src/test-setup.ts', 'core-web/apps/dotcms-block-editor/src/test.ts'],
        expect: 'accept',
        category: 'test-support'
    },
    {
        name: 'workspace config is admitted',
        paths: ['core-web/nx.json', 'core-web/package.json', 'core-web/pnpm-lock.yaml', 'core-web/libs/ui/project.json'],
        expect: 'accept',
        category: 'workspace-config'
    },
    {
        name: 'the Maven test invocation is admitted',
        paths: ['core-web/pom.xml'],
        expect: 'accept',
        category: 'build-invocation',
        because: 'FR-009 requires removing two Jest-only CLI flags from it.'
    },
    {
        name: 'documentation is admitted',
        paths: ['core-web/CLAUDE.md', 'docs/frontend/TESTING_FRONTEND.md', '.cursor/rules/test-context.mdc'],
        expect: 'accept',
        category: 'docs'
    },
    {
        name: 'a realistic migration commit is admitted',
        paths: [
            'core-web/libs/portlets/dot-content-drive/portlet/project.json',
            'core-web/libs/portlets/dot-content-drive/portlet/vitest-base.config.ts',
            'core-web/libs/portlets/dot-content-drive/portlet/jest.config.ts',
            'core-web/libs/portlets/dot-content-drive/portlet/src/lib/store/dot-content-drive.store.spec.ts'
        ],
        expect: 'accept'
    },
    {
        name: 'one product file among many test files still rejects',
        paths: [
            'core-web/libs/ui/src/lib/a.component.spec.ts',
            'core-web/libs/ui/jest.config.ts',
            'core-web/libs/ui/src/lib/a.component.ts',
            'core-web/libs/ui/src/lib/b.component.spec.ts'
        ],
        expect: 'reject',
        because: 'A single product file must not be lost among legitimate changes — this is the realistic accident.'
    },
    {
        name: 'a GitHub Actions workflow edit is rejected',
        paths: ['.github/workflows/cicd_comp_test-phase.yml'],
        expect: 'reject',
        because: 'FR-009 — no workflow may change. Not in any allow category, so it must fall through to rejection.'
    }
];

function runChecker(paths) {
    const res = spawnSync(process.execPath, [CHECKER, '--paths-from', '-', '--json'], {
        input: paths.join('\n') + '\n',
        encoding: 'utf8'
    });
    return res;
}

function main() {
    if (!existsSync(CHECKER)) {
        console.error('RED: verify-test-only-diff.mjs does not exist yet.');
        console.error(`Expected at: ${CHECKER}`);
        console.error(`\nAll ${SCENARIOS.length} scenarios fail for this reason, which is the`);
        console.error('intended Red state (tasks.md T013). Implement the checker (T014) only after');
        console.error('the T011 and T012 gates are signed off.');
        process.exit(1);
    }

    let failed = 0;
    for (const sc of SCENARIOS) {
        const res = runChecker(sc.paths);
        if (res.error) {
            console.log(`✗ ${sc.name}\n    harness error: ${res.error.message}`);
            failed++;
            continue;
        }
        const accepted = res.status === 0;
        const wantAccept = sc.expect === 'accept';

        if (accepted !== wantAccept) {
            console.log(`✗ ${sc.name}`);
            console.log(`    expected ${sc.expect}, got ${accepted ? 'accept' : 'reject'} (exit ${res.status})`);
            if (sc.because) console.log(`    why it matters: ${sc.because}`);
            failed++;
            continue;
        }

        if (wantAccept && sc.category) {
            let report;
            try {
                report = JSON.parse(res.stdout);
            } catch {
                console.log(`✗ ${sc.name}\n    --json output was not parseable`);
                failed++;
                continue;
            }
            const cats = new Set((report.files ?? []).map((f) => f.category));
            if (!cats.has(sc.category)) {
                console.log(`✗ ${sc.name}`);
                console.log(`    expected category "${sc.category}", saw [${[...cats].join(', ')}]`);
                failed++;
                continue;
            }
        }
        console.log(`✓ ${sc.name}`);
    }

    console.log(`\n${SCENARIOS.length - failed}/${SCENARIOS.length} scenarios passed`);
    process.exit(failed === 0 ? 0 : 1);
}

main();
