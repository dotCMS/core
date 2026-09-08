#!/usr/bin/env node
/**
 * Apply the per-project migration steps that are not the vite config — FR-005, FR-007.
 *
 * Run AFTER tools/generate-vite-configs.mjs. For each project:
 *
 *   1. tsconfig.spec.json  — types jest -> vitest/globals; include the component
 *      sources, not just the specs (Angular's compiler needs them in the compilation
 *      unit; ts-jest did not, because it transpiles per file — research R-16).
 *   2. src/test-setup.ts   — replace jest-preset-angular's setupZoneTestEnv with an
 *      explicit TestBed init, PRESERVING any strictness flags it carried.
 *   3. project.json        — drop the `test` target so @nx/vitest infers it from the
 *      vite config, and drop `passWithNoTests` (FR-004, research R-11).
 *   4. jest.config.ts      — delete.
 *
 * Idempotent: re-running a migrated project is a no-op.
 *
 * Usage: node tools/migrate-project.mjs <project-dir> [...] | --all [--dry-run]
 */

import { readFileSync, writeFileSync, existsSync, rmSync, readdirSync } from 'node:fs';
import { join, resolve } from 'node:path';

const CW = resolve(import.meta.dirname, '..');

function jestProjects() {
    const nx = JSON.parse(readFileSync(join(CW, 'nx.json'), 'utf8'));
    const plugin = nx.plugins.find((p) => typeof p === 'object' && p.plugin === '@nx/jest/plugin');
    const fromPlugin = plugin ? plugin.include.map((g) => g.split('/**')[0]) : [];

    // The plugin's include list is NOT the whole story: five projects declare an
    // explicit `@nx/jest:jest` executor in their own project.json instead
    // (ai-ui, dot-users, dot-roles, dot-agents, dot-publishing-queue). Scoping the
    // migration to the plugin list alone would have left them on Jest while every
    // completeness assertion reported success.
    const fromExecutor = [];
    const walk = (rel) => {
        for (const entry of readdirSync(join(CW, rel), { withFileTypes: true })) {
            if (entry.name === 'node_modules' || entry.name.startsWith('.')) continue;
            const child = `${rel}/${entry.name}`;
            if (entry.isDirectory()) walk(child);
            else if (entry.name === 'project.json') {
                try {
                    const d = JSON.parse(readFileSync(join(CW, child), 'utf8'));
                    if (d.targets?.test?.executor === '@nx/jest:jest') fromExecutor.push(rel);
                } catch { /* unparseable project.json is not ours to fix */ }
            }
        }
    };
    for (const top of ['libs', 'apps']) walk(top);

    // A THIRD source: any directory that still has a jest.config.ts. Without this the
    // pipeline is not re-runnable — once nx.json's plugin entry and the project.json
    // test targets are stripped, discovery returns nothing and the generators print
    // usage while leaving 48 jest configs on disk. Re-runnability matters: this ran
    // several times over the course of the migration.
    const fromJestConfig = [];
    const walkJest = (rel) => {
        for (const entry of readdirSync(join(CW, rel), { withFileTypes: true })) {
            if (entry.name === 'node_modules' || entry.name.startsWith('.')) continue;
            const child = `${rel}/${entry.name}`;
            if (entry.isDirectory()) walkJest(child);
            else if (entry.name === 'jest.config.ts') fromJestConfig.push(rel);
        }
    };
    for (const top of ['libs', 'apps']) walkJest(top);

    const OUT_OF_SCOPE = ['libs/dotcms-webcomponents'];
    return [...new Set([...fromPlugin, ...fromExecutor, ...fromJestConfig])]
        .filter((d) => !OUT_OF_SCOPE.some((o) => d === o || d.startsWith(o + '/')))
        .sort();
}

function migrateTsconfigSpec(dir, dry) {
    const p = join(CW, dir, 'tsconfig.spec.json');
    if (!existsSync(p)) return 'absent';
    const d = JSON.parse(readFileSync(p, 'utf8'));
    const before = JSON.stringify(d);

    const types = d.compilerOptions?.types ?? [];
    if (types.includes('jest')) {
        d.compilerOptions.types = types.map((t) => (t === 'jest' ? 'vitest/globals' : t));
    }
    // Angular's compiler needs the sources, not only the specs.
    d.include = ['src/**/*.ts', 'src/**/*.tsx', 'src/**/*.d.ts', 'vite.config.mts'];

    // `module: commonjs` is what ts-jest wanted and what an ESM pipeline cannot use:
    // extensionless relative imports stop resolving and Node reports
    // `Cannot find module './define-adapter'` with no hint that the module *system*
    // is the problem. Measured on sdk-ai: 0 tests running before, 58/58 after.
    if (d.compilerOptions?.module === 'commonjs') {
        d.compilerOptions.module = 'esnext';
        d.compilerOptions.moduleResolution = 'bundler';
    }

    if (JSON.stringify(d) === before) return 'unchanged';
    if (!dry) writeFileSync(p, JSON.stringify(d, null, 4) + '\n');
    return 'updated';
}

const SETUP_ZONE = /import\s*\{\s*setupZoneTestEnv\s*\}\s*from\s*'jest-preset-angular\/setup-env\/zone';\s*/;

function migrateTestSetup(dir, dry) {
    const p = join(CW, dir, 'src', 'test-setup.ts');
    if (!existsSync(p)) return 'absent';
    let s = readFileSync(p, 'utf8');
    if (!SETUP_ZONE.test(s)) return 'unchanged';

    // Preserve whatever options setupZoneTestEnv was given. They are not decoration:
    // errorOnUnknownElements/Properties turn a silent no-op into a failure, so losing
    // them would quietly weaken every spec in the project.
    const call = /setupZoneTestEnv\(\s*(\{[\s\S]*?\})?\s*\);?/.exec(s);
    const opts = call?.[1] ?? '';

    // Analog's setup-zone, not a hand-rolled `import 'zone.js/testing'`. zone.js
    // patches jasmine/mocha/jest to install the ProxyZone that fakeAsync needs and
    // knows nothing about Vitest, so hand-rolling it leaves every fakeAsync test
    // failing with "Expected to be running in 'ProxyZone'" — 98 in data-access alone
    // (research R-18).
    s = s.replace(SETUP_ZONE, "import '@analogjs/vitest-angular/setup-zone';\n\nimport { getTestBed } from '@angular/core/testing';\nimport { BrowserTestingModule, platformBrowserTesting } from '@angular/platform-browser/testing';\n\n");
    s = s.replace(
        call[0],
        `// Replaces jest-preset-angular's setupZoneTestEnv: nothing initialises the TestBed\n` +
        `// for us under Vitest, so this file does it. The options below are carried across\n` +
        `// verbatim — they make unknown elements and properties fail rather than pass.\n` +
        `getTestBed().initTestEnvironment(BrowserTestingModule, platformBrowserTesting()${opts ? `, ${opts}` : ''});`
    );
    if (!dry) writeFileSync(p, s);
    return 'updated';
}

function migrateProjectJson(dir, dry) {
    const p = join(CW, dir, 'project.json');
    if (!existsSync(p)) return 'absent';
    const d = JSON.parse(readFileSync(p, 'utf8'));
    if (!d.targets?.test) return 'unchanged';
    delete d.targets.test; // @nx/vitest infers it from vite.config.mts
    if (!dry) writeFileSync(p, JSON.stringify(d, null, 4) + '\n');
    return 'updated';
}

function removeJestConfig(dir, dry) {
    const p = join(CW, dir, 'jest.config.ts');
    if (!existsSync(p)) return 'absent';
    if (!dry) rmSync(p);
    return 'deleted';
}

const args = process.argv.slice(2);
const dry = args.includes('--dry-run');
const dirs = args.includes('--all') ? jestProjects() : args.filter((a) => !a.startsWith('--'));

if (dirs.length === 0) {
    console.error('usage: migrate-project.mjs <project-dir> [...] | --all [--dry-run]');
    process.exit(2);
}

for (const dir of dirs) {
    const r = {
        tsconfig: migrateTsconfigSpec(dir, dry),
        setup: migrateTestSetup(dir, dry),
        project: migrateProjectJson(dir, dry),
        jest: removeJestConfig(dir, dry)
    };
    console.log(`${dir.padEnd(46)} tsconfig=${r.tsconfig} setup=${r.setup} project=${r.project} jest=${r.jest}`);
}
console.log(`\n${dirs.length} project(s)${dry ? ' (dry run)' : ''}`);
