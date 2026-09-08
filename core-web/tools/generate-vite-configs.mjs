#!/usr/bin/env node
/**
 * Generate a per-project `vite.config.mts` by translating that project's
 * `jest.config.ts` — FR-007, FR-008, and the R-16 recipe.
 *
 * Each project's Jest config already records what its tests need. The translation
 * is one-to-one:
 *
 *   testEnvironment          -> test.environment          (FR-007 parity)
 *   coverageDirectory        -> test.coverage.reportsDirectory
 *   setupFilesAfterEach      -> test.setupFiles
 *   moduleNameMapper         -> resolve.alias
 *   transformIgnorePatterns  -> test.server.deps.inline    <- the load-bearing one
 *
 * That last mapping is the point of generating rather than hand-writing. A package
 * listed in transformIgnorePatterns is one Jest had to transform because it ships
 * ESM; under Vite the equivalent problem is externalisation, and the same package
 * list is the answer. Hand-copying it 41 times would go wrong 41 different ways.
 *
 * Usage:
 *   node tools/generate-vite-configs.mjs --list
 *   node tools/generate-vite-configs.mjs <project-dir> [...]
 *   node tools/generate-vite-configs.mjs --all [--dry-run]
 */

import { readFileSync, writeFileSync, existsSync, readdirSync } from 'node:fs';
import { join, resolve } from 'node:path';

const CW = resolve(import.meta.dirname, '..');

/** Packages that must be inlined for EVERY Angular project, regardless of config. */
const ALWAYS_INLINE = [
    '/@angular\\//',
    '/@openng\\/spectator/',
    '/zone\\.js/',
    // Anything shipping Angular-compiled code registers directives against whichever
    // @angular/core instance it loads. Externalised, that is not the one the specs
    // use, and components die on `firstCreatePass` of null (research R-16).
    '/primeng/',
    '/@primeuix/',
    '/@ngrx/'
];

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

/**
 * Read the values we need out of a jest.config.ts by pattern rather than by
 * evaluating it. Evaluating would need the whole TS pipeline for a handful of
 * string literals, and these configs are uniformly simple object literals.
 */
function readJestConfig(dir) {
    const p = join(CW, dir, 'jest.config.ts');
    if (!existsSync(p)) return null;
    const src = readFileSync(p, 'utf8');

    const str = (key) => {
        const m = new RegExp(`${key}:\\s*'([^']*)'`).exec(src);
        return m ? m[1] : null;
    };

    // transformIgnorePatterns carries a negative-lookahead list of package names.
    // Extracted by finding pipe-separated alternation groups rather than parsing the
    // whole lookahead: the outer group nests, so a naive [^)]* stops at the wrong
    // paren and silently yields nothing — which is how an earlier version reported
    // esm=0 for every project while eight of them had real lists.
    const tip = /transformIgnorePatterns:\s*\[([\s\S]*?)\]/.exec(src);
    const esmPackages = [];
    if (tip) {
        for (const m of tip[1].matchAll(/\(([A-Za-z0-9@/_.-]+(?:\|[A-Za-z0-9@/_.-]+)+)\)/g)) {
            for (const name of m[1].split('|')) {
                const clean = name.trim();
                // Skip the `(/|-)` style separator groups some configs use.
                if (clean.length > 1 && !clean.includes('.mjs')) esmPackages.push(clean);
            }
        }
    }

    const mnm = /moduleNameMapper:\s*\{([\s\S]*?)\n\s*\}/.exec(src);
    const aliases = [];
    if (mnm) {
        for (const m of mnm[1].matchAll(/'([^']+)':\s*'([^']+)'/g)) {
            aliases.push({ find: m[1], replacement: m[2] });
        }
    }

    return {
        displayName: str('displayName'),
        testEnvironment: str('testEnvironment'),
        coverageDirectory: str('coverageDirectory'),
        setupFiles: [...src.matchAll(/setupFilesAfter(?:Env|Each):\s*\[([^\]]*)\]/g)]
            .flatMap((m) => [...m[1].matchAll(/'([^']+)'/g)].map((x) => x[1])),
        esmPackages,
        aliases
    };
}

/** Jest environment names -> Vitest environment names. */
function mapEnvironment(jestEnv) {
    if (!jestEnv) return 'jsdom'; // the jest-preset-angular default the preset applied
    if (jestEnv.includes('happy-dom')) return 'happy-dom';
    if (jestEnv === 'node') return 'node';
    if (jestEnv === 'jsdom') return 'jsdom';
    return 'jsdom';
}

function projectName(dir) {
    const pj = join(CW, dir, 'project.json');
    return existsSync(pj) ? JSON.parse(readFileSync(pj, 'utf8')).name : null;
}

function generate(dir) {
    const cfg = readJestConfig(dir);
    if (!cfg) return { dir, skipped: 'no jest.config.ts' };

    const name = projectName(dir);
    const depth = dir.split('/').length;
    const up = '../'.repeat(depth);
    const env = mapEnvironment(cfg.testEnvironment);

    const setupFiles = cfg.setupFiles.map((f) => `${dir}/${f.replace('<rootDir>/', '')}`);
    const coverageDir = cfg.coverageDirectory
        ? cfg.coverageDirectory.replace(/^(\.\.\/)+/, '')
        : `coverage/${dir}`;

    const inline = [...ALWAYS_INLINE, ...cfg.esmPackages.map((p) => `/${p.replace(/[/@]/g, (c) => '\\' + c)}/`)];
    const aliasLines = cfg.aliases.map(
        (a) => `            { find: ${JSON.stringify(a.find)}, replacement: resolve(WORKSPACE, ${JSON.stringify(
            a.replacement.replace('<rootDir>', dir)
        )}) }`
    );

    const body = `/// <reference types="vitest" />
import angular from '@analogjs/vite-plugin-angular';
import { defineConfig } from 'vite';
import tsconfigPaths from 'vite-tsconfig-paths';

import { resolve } from 'path';

/**
 * GENERATED by tools/generate-vite-configs.mjs from this project's jest.config.ts.
 * Regenerate rather than hand-editing where possible; see research.md R-16 for why
 * each block is here and which failure it prevents.
 */
const WORKSPACE = resolve(import.meta.dirname, '${up.slice(0, -1)}');
const PROJECT = '${dir}';

export default defineConfig({
    // The workspace, not the project: sibling libs resolve through tsconfig paths to
    // their TypeScript source, and the Angular plugin only compiles under root.
    root: WORKSPACE,
    plugins: [
        angular({
            tsconfig: resolve(import.meta.dirname, 'tsconfig.spec.json'),
            workspaceRoot: WORKSPACE
        }),
        // Pinned to the base tsconfig AND this project's spec tsconfig. Pinning only
        // the base broke sibling-alias resolution: @dotcms/types resolved to a
        // root-relative '/libs/sdk/types/src/index.ts' (no core-web prefix) and cost
        // sdk-uve 94 of its 103 tests. Pinning nothing fixes that too, but then the
        // plugin crawls every tsconfig in the monorepo, which segfaults the native
        // resolver on CI — the failure the original pin existed to prevent. Two
        // entries keep both properties.
        tsconfigPaths({
            root: WORKSPACE,
            projects: ['tsconfig.base.json', \`\${PROJECT}/tsconfig.spec.json\`]
        })
    ],
    // No mainFields here, deliberately. Setting it to ['module'] — copied from the
    // SDK *build* configs, where it is correct — makes Vite ignore packages that
    // declare only a main field, including @analogjs/vitest-angular. Resolution falls
    // through to a bogus root-relative path and takes the whole project down with an
    // error that names the package but not the cause (research R-20).
    resolve: {${aliasLines.length ? `

        alias: [
${aliasLines.join(',\n')}
        ]` : ''}
    },
    test: {
        root: WORKSPACE,
        globals: true,
        // Carried across from jest.config.ts (FR-007). Without pinning it the project
        // would silently adopt Vitest's default instead.
        environment: '${env}',${setupFiles.length ? `
        setupFiles: [${setupFiles.map((f) => `'${f}'`).join(', ')}],` : ''}
        include: [\`\${PROJECT}/src/**/*.spec.ts\`, \`\${PROJECT}/src/**/*.spec.tsx\`, \`\${PROJECT}/src/**/*.test.ts\`, \`\${PROJECT}/src/**/*.test.tsx\`],
        // Jest gave each spec file a fresh module registry; state this rather than
        // inherit it, so the semantics the specs were written under stay explicit.
        isolate: true,
        server: {
            // Deliberately true, not a package list. The list started as Angular + Spectator +
            // zone.js, then needed PrimeNG, then @ngrx, then each project's own
            // transformIgnorePatterns chain — and dotcms-ui still produced 420 NG0203
            // errors from something not on it. Every package that touches
            // @angular/core must resolve to the same instance, and enumerating them
            // is a losing game: one missed package fails ~200 tests with an error
            // that names Angular, not the package. Inlining everything is slower and
            // correct; FR-017 sets no performance threshold.
            //
            // This project's jest.config.ts listed: ${cfg.esmPackages.length ? cfg.esmPackages.join(', ') : '(no transformIgnorePatterns)'}
            deps: { inline: true }
        },
        reporters: ['default', 'github-actions', ['junit', { outputFile: 'target/core-web-reports/${name}.xml' }]],
        coverage: {
            provider: 'v8',
            reporter: ['html', 'lcov', 'text'],
            reportsDirectory: '${coverageDir}'
        }
    }
});
`;
    return { dir, name, env, inline: inline.length, setupFiles: setupFiles.length, body };
}

const args = process.argv.slice(2);
const dryRun = args.includes('--dry-run');
const all = args.includes('--all');
const list = args.includes('--list');
const targets = args.filter((a) => !a.startsWith('--'));

if (list) {
    for (const d of jestProjects()) {
        const c = readJestConfig(d);
        console.log(
            `${d.padEnd(46)} env=${String(mapEnvironment(c?.testEnvironment)).padEnd(10)} esm=${c?.esmPackages.length ?? 0} alias=${c?.aliases.length ?? 0}`
        );
    }
    process.exit(0);
}

const dirs = all ? jestProjects() : targets;
if (dirs.length === 0) {
    console.error('usage: generate-vite-configs.mjs --all | --list | <project-dir> [...]');
    process.exit(2);
}

let written = 0;
for (const d of dirs) {
    const r = generate(d);
    if (r.skipped) {
        console.log(`  skip ${d} (${r.skipped})`);
        continue;
    }
    const out = join(CW, d, 'vite.config.mts');
    if (!dryRun) writeFileSync(out, r.body);
    written++;
    console.log(`  ${dryRun ? 'would write' : 'wrote'} ${d}/vite.config.mts  env=${r.env} inline=${r.inline} setup=${r.setupFiles}`);
}
console.log(`\n${written} config(s)${dryRun ? ' (dry run)' : ''}`);
