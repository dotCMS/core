#!/usr/bin/env node
/**
 * Diff allowlist enforcement — FR-001, FR-001a, FR-002, FR-002a.
 *
 * Fails if a pull request changes anything outside an allowlist of test files,
 * test configuration and migration tooling. This is what replaces manual QA on a
 * ~1,000-file test-runner migration: it turns "this only touches tests" from a
 * promise in a PR description into a required check.
 *
 * Contract: specs/37444-vitest-unit-test-migration/contracts/diff-allowlist.md
 *
 * Usage:
 *   node tools/verify-test-only-diff.mjs [--base <ref>] [--json]
 *   node tools/verify-test-only-diff.mjs --paths-from <file|-> [--json]
 *
 * Exit codes:
 *   0  every changed path matched an allowed category
 *   1  at least one path matched none, or matched a denied category
 *   2  invocation error (no merge base, not a git repo, unreadable input)
 *
 * TWO DESIGN RULES THAT CARRY THE WHOLE CHECK:
 *
 *   1. DENY IS EVALUATED BEFORE ALLOW. E2E specs are also named `*.spec.ts`, and
 *      the Stencil project's config also matches `test-config`. An allow-first
 *      check would silently admit all 35 E2E specs while appearing to work.
 *
 *   2. UNMATCHED IS REJECTED, NEVER IGNORED. A path shape nobody anticipated
 *      fails the build and has to be classified deliberately. A check that
 *      defaults to permitting the unfamiliar inverts its own purpose.
 */

import { spawnSync } from 'node:child_process';
import { readFileSync } from 'node:fs';

/**
 * Checked FIRST. These paths would otherwise be admitted by an allow category,
 * so leaving them implicit is not an option.
 */
const DENY = [
    { category: 'e2e', patterns: ['core-web/apps/dotcms-ui-e2e/**', '**/playwright.config.*'],
      why: 'E2E is out of scope (FR-002a) and its specs share the *.spec.ts naming' },
    { category: 'stencil', patterns: ['core-web/libs/dotcms-webcomponents/**'],
      why: 'Out of scope; its Stencil config would match test-config' }
];

/**
 * Named product-source exceptions — each one enumerated, never a widened pattern.
 *
 * The whole point of this check is that no product file changes, so an exception has
 * to name the exact path and say why the migration could not proceed without it. They
 * are counted and printed SEPARATELY from the allowed categories: a reviewer must see
 * "1 product exception" rather than have it disappear into a bucket.
 */
const PRODUCT_EXCEPTIONS = [
    {
        path: 'core-web/libs/portlets/dot-content-drive/portlet/src/lib/components/dialogs/dot-content-drive-action-center/dot-content-drive-action-center.component.ts',
        why:
            'escaped backticks inside a CSS comment in the inline `styles` array break ' +
            "@analogjs/vite-plugin-angular's JIT virtual style module — the CSS is re-emitted " +
            'into a template literal, the backtick closes it early and Rollup then parses the ' +
            'CSS as JavaScript ("Expected \';\', got \':\'" on `:host`). It took two spec ' +
            'files down entirely, 214 tests. The edit replaces the backticks with quotes ' +
            'INSIDE a /* */ comment: no selector, declaration or TypeScript changes.'
    }
];

const ALLOW = [
    { category: 'spec', patterns: ['**/*.spec.ts', '**/*.spec.tsx', '**/*.test.ts', '**/*.test.tsx'] },
    { category: 'test-support', patterns: ['**/test-setup.ts', '**/src/test.ts', 'core-web/tools/__tests__/**'] },
    { category: 'test-config', patterns: [
        '**/jest.config.*', '**/vite.config.*', '**/vitest.config.*', '**/vitest-base.config.*',
        '**/tsconfig.spec.json', '**/karma.conf.js', 'core-web/jest.preset.js'
    ] },
    { category: 'workspace-config', patterns: [
        'core-web/nx.json', 'core-web/package.json', 'core-web/pnpm-lock.yaml', '**/project.json'
    ] },
    { category: 'build-invocation', patterns: ['core-web/pom.xml'] },
    // Its OWN category rather than folded into workspace-config, deliberately: a
    // tsconfig.json also configures the product build, so it is the one allowed
    // category a reviewer should actually open. Keeping it separate makes the
    // category report say "ts-project-refs: 2" instead of burying those two files
    // in a count of thirty. Only expected here because deleting a project's
    // tsconfig.spec.json leaves a dangling `references` entry behind.
    { category: 'ts-project-refs', patterns: ['**/tsconfig.json'] },
    // Shared test utilities that are NOT spec files but exist only to be imported by
    // them. `libs/utils-testing` holds 27 `jest.fn()` calls and is imported across the
    // workspace; the first spike run failed on `TS2708: Cannot use namespace 'jest' as
    // a value` from it. It qualifies as test infrastructure on evidence, not naming:
    // tagged skip:test/skip:lint, no build target, no production consumer.
    // Kept as its own category so a reviewer sees it counted separately.
    { category: 'test-utilities', patterns: ['core-web/libs/utils-testing/**', '**/__mocks__/**'] },
    // Adding a Vitest config to a library makes @nx/dependency-checks flag its
    // dev-only imports as missing runtime dependencies, so each project's eslint
    // config needs those paths added to the rule's ignore list. Test-adjacent by
    // necessity, but its own category: an eslint config can change lint behaviour
    // for product code too, so a reviewer should see the count and open the files.
    { category: 'lint-dependency-checks', patterns: ['**/eslint.config.mjs'] },
    { category: 'migration-tooling', patterns: ['core-web/tools/**'] },
    { category: 'docs', patterns: ['**/*.md', '.cursor/rules/*.mdc', 'specs/**'] }
];

/**
 * Glob matcher. Deliberately hand-rolled rather than pulling in picomatch: this
 * check must run before `pnpm install` succeeds, because it gates work that
 * precedes a working toolchain. The supported syntax is exactly what the
 * patterns above use — `**`, `*`, and `?` — and nothing more.
 */
function globToRegExp(pattern) {
    let re = '';
    for (let i = 0; i < pattern.length; i++) {
        const c = pattern[i];
        if (c === '*') {
            if (pattern[i + 1] === '*') {
                // `**/` matches zero or more path segments; a trailing `**` matches the rest.
                if (pattern[i + 2] === '/') {
                    re += '(?:[^/]+/)*';
                    i += 2;
                } else {
                    re += '.*';
                    i += 1;
                }
            } else {
                re += '[^/]*';
            }
        } else if (c === '?') re += '[^/]';
        else if ('.+^${}()|[]\\/'.includes(c)) re += '\\' + c;
        else re += c;
    }
    return new RegExp(`^${re}$`);
}

const compiled = new Map();
function matches(path, pattern) {
    if (!compiled.has(pattern)) compiled.set(pattern, globToRegExp(pattern));
    return compiled.get(pattern).test(path);
}

function classify(path) {
    const exception = PRODUCT_EXCEPTIONS.find((e) => e.path === path);
    if (exception) {
        return { path, verdict: 'allowed', category: 'product-exception', why: exception.why };
    }
    for (const rule of DENY) {
        if (rule.patterns.some((p) => matches(path, p))) {
            return { path, verdict: 'denied', category: rule.category, why: rule.why };
        }
    }
    for (const rule of ALLOW) {
        if (rule.patterns.some((p) => matches(path, p))) {
            return { path, verdict: 'allowed', category: rule.category };
        }
    }
    return { path, verdict: 'unmatched', category: null };
}

function fail(code, message) {
    console.error(`verify-test-only-diff: ${message}`);
    process.exit(code);
}

function parseArgs(argv) {
    const args = {};
    for (let i = 2; i < argv.length; i++) {
        const a = argv[i];
        if (a === '--base') args.base = argv[++i];
        else if (a === '--paths-from') args.pathsFrom = argv[++i];
        else if (a === '--json') args.json = true;
        else fail(2, `unknown argument: ${a}`);
    }
    return args;
}

function readPaths(args) {
    if (args.pathsFrom) {
        const raw = args.pathsFrom === '-'
            ? readFileSync(0, 'utf8')
            : readFileSync(args.pathsFrom, 'utf8');
        return raw.split('\n').map((l) => l.trim()).filter(Boolean);
    }

    const base = args.base ?? 'origin/main';
    const mb = spawnSync('git', ['merge-base', 'HEAD', base], { encoding: 'utf8' });
    if (mb.status !== 0) {
        fail(2, `cannot find merge base with ${base}. Is it fetched? A shallow clone has no merge base.\n` +
                `  ${(mb.stderr || '').trim()}`);
    }
    const mergeBase = mb.stdout.trim();
    const diff = spawnSync('git', ['diff', '--name-only', `${mergeBase}..HEAD`], { encoding: 'utf8' });
    if (diff.status !== 0) fail(2, `git diff failed: ${(diff.stderr || '').trim()}`);
    return diff.stdout.split('\n').map((l) => l.trim()).filter(Boolean);
}

function main() {
    const args = parseArgs(process.argv);
    const paths = readPaths(args);

    if (paths.length === 0) {
        // An empty diff is vacuously clean, but say so rather than printing a
        // bare success that looks like the check ran against real changes.
        if (args.json) console.log(JSON.stringify({ ok: true, empty: true, files: [] }, null, 2));
        else console.log('No changed paths. Nothing to verify.');
        process.exit(0);
    }

    const results = paths.map(classify);
    const rejected = results.filter((r) => r.verdict !== 'allowed');

    if (args.json) {
        console.log(JSON.stringify({ ok: rejected.length === 0, files: results }, null, 2));
    } else {
        const byCategory = new Map();
        for (const r of results.filter((x) => x.verdict === 'allowed')) {
            byCategory.set(r.category, (byCategory.get(r.category) ?? 0) + 1);
        }
        console.log(`Inspected ${results.length} changed path(s).\n`);
        if (byCategory.size) {
            console.log('Allowed:');
            for (const [cat, n] of [...byCategory].sort()) {
                console.log(`  ${String(n).padStart(5)}  ${cat}`);
            }
        }
        const exceptions = results.filter((r) => r.category === 'product-exception');
        if (exceptions.length) {
            console.log(`\nPRODUCT EXCEPTIONS (${exceptions.length}) — read these:`);
            for (const e of exceptions) {
                console.log(`  ${e.path}\n      ${e.why}`);
            }
        }
        if (rejected.length) {
            console.log('\nREJECTED:');
            for (const r of rejected) {
                const reason = r.verdict === 'denied'
                    ? `denied (${r.category}) — ${r.why}`
                    : 'no allowed category matched';
                console.log(`  ${r.path}\n      ${reason}`);
            }
        }
    }

    // Guidance goes to stderr, and the human summary is suppressed under --json,
    // so stdout stays a single parseable document for callers that consume it.
    if (rejected.length) {
        console.error(
            `\n${rejected.length} path(s) are outside the allowlist. This change claims to touch only\n` +
            'tests and test configuration; each path above contradicts that claim. Either revert it,\n' +
            'or — if it is legitimately test infrastructure — classify it explicitly in\n' +
            'contracts/diff-allowlist.md and here. Do not widen a category to make a product file fit.'
        );
        process.exit(1);
    }
    if (!args.json) {
        console.log('\nOK — every changed path is test code, test configuration, or migration tooling.');
    }
    process.exit(0);
}

main();
