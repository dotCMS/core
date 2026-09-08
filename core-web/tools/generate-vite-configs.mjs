#!/usr/bin/env node
/**
 * Generate a per-project Vitest config — FR-007, FR-008.
 *
 * SHAPED AFTER NX'S OWN GENERATORS, NOT HAND-ROLLED.
 *
 * Nx 23 has first-class Angular+Vitest support that this migration did not consult
 * early enough: `nx g @nx/angular:library --unitTestRunner=vitest-analog` (or
 * `vitest-angular`) emits a working config, and `@nx/react` / `@nx/vue` do the same
 * for their frameworks. The first version of this script invented a parallel
 * convention instead — `root` at the workspace, `vite-tsconfig-paths`, and an
 * explicit `nx:run-commands` target. It worked, but it left the repo with a
 * migration-specific shape that nothing in the ecosystem would maintain, and that
 * the next generated project would not match.
 *
 * The base emitted here is what Nx generates, verbatim in structure:
 *
 *   root: __dirname                          // the PROJECT, not the workspace
 *   plugins: [<framework>(), nxViteTsPaths()]
 *   test: { name, watch: false, globals: true, environment, include, setupFiles,
 *           reporters, coverage: { reportsDirectory, provider: 'v8' } }
 *
 * and the `test` target is left for `@nx/vitest` to infer, as in a generated project.
 *
 * DEVIATIONS are the exception and each carries the measured failure that justifies
 * it. There are three: the DOM environment (FR-007 parity), the report artifacts
 * (FR-008 parity), and the Angular-instance guard below. Anything else that differs
 * from a generated project is a bug in this script.
 *
 * Usage:
 *   node tools/generate-vite-configs.mjs --list
 *   node tools/generate-vite-configs.mjs --all [--dry-run]
 *   node tools/generate-vite-configs.mjs <project-dir> [...]
 */

import { readFileSync, writeFileSync, existsSync, readdirSync } from 'node:fs';
import { execFileSync } from 'node:child_process';
import { join, resolve } from 'node:path';

const CW = resolve(import.meta.dirname, '..');
const OUT_OF_SCOPE = ['libs/dotcms-webcomponents', 'apps/dotcms-ui-e2e'];

/**
 * Packages that must resolve to ONE @angular/core instance.
 *
 * This is the one deviation from Nx's base that is not about parity, and it is not
 * precautionary. Nx's generated shape produces
 * `Cannot read properties of null (reading 'ngModule')` on this workspace, because
 * sibling libraries ship Angular-compiled code (PrimeNG, @ngrx) that — externalised —
 * registers its directives against a different Angular runtime than the specs use.
 * Measured on portlets-dot-tags: 39 passing with this list, 39 failing without it.
 *
 * A freshly generated Nx project has no such siblings, which is why its generator
 * does not need this, and why adopting its config alone was not sufficient here.
 */
const ANGULAR_INSTANCE_GUARD = [
    // The workspace's OWN libraries, first. They are consumed from SOURCE through
    // tsconfig paths, so with Nx's `root: __dirname` they sit outside root and Vite
    // externalises them — their `inject()` calls then bind to a different Angular
    // instance than the specs use, and Angular reports
    // `NG0203: The Injector token injection failed`.
    //
    // This is what lets the migration keep Nx's shape instead of moving `root` to the
    // workspace: same problem, solved in a supported Vitest field rather than by
    // forking the config structure. Measured on image-editor: 355 of 355 tests
    // running with this entry, 75 NG0203 errors without it.
    '/[\\\\/](libs|apps)[\\\\/]/',
    '/@angular\\//',
    '/@analogjs\\//',
    '/@openng\\/spectator/',
    '/zone\\.js/',
    '/primeng/',
    '/@primeuix/',
    '/@ngrx/'
];

/** Read from the working tree, falling back to git so this script stays re-runnable. */
function readMaybeFromGit(relPath) {
    const abs = join(CW, relPath);
    if (existsSync(abs)) return readFileSync(abs, 'utf8');
    for (const ref of ['HEAD', 'HEAD~1', 'HEAD~2']) {
        try {
            return execFileSync('git', ['show', `${ref}:core-web/${relPath}`], {
                cwd: CW,
                encoding: 'utf8',
                stdio: ['pipe', 'pipe', 'ignore']
            });
        } catch {
            /* try the next ref */
        }
    }
    return null;
}

/**
 * Discovery from four sources, because no single one is complete: the plugin's
 * include list, explicit `@nx/jest:jest` executors (five projects use those instead),
 * and the presence of a jest config or an already-generated vitest config. The last
 * two are what make this re-runnable — once the migration strips nx.json and the
 * project targets, the first two return nothing.
 */
function projects() {
    const found = new Set();

    try {
        const nx = JSON.parse(readFileSync(join(CW, 'nx.json'), 'utf8'));
        const plugin = nx.plugins?.find((p) => typeof p === 'object' && p.plugin === '@nx/jest/plugin');
        for (const g of plugin?.include ?? []) found.add(g.split('/**')[0]);
    } catch {
        /* nx.json already stripped */
    }

    const walk = (rel) => {
        for (const e of readdirSync(join(CW, rel), { withFileTypes: true })) {
            if (e.name === 'node_modules' || e.name.startsWith('.')) continue;
            const child = `${rel}/${e.name}`;
            if (e.isDirectory()) {
                walk(child);
                continue;
            }
            if (e.name === 'jest.config.ts') found.add(rel);
            else if (e.name === 'vite.config.mts' && readFileSync(join(CW, child), 'utf8').includes('GENERATED by tools/generate-vite-configs')) found.add(rel);
            else if (e.name === 'project.json') {
                try {
                    const d = JSON.parse(readFileSync(join(CW, child), 'utf8'));
                    if (d.targets?.test?.executor === '@nx/jest:jest') found.add(rel);
                } catch {
                    /* not ours to fix */
                }
            }
        }
    };
    for (const top of ['libs', 'apps']) walk(top);

    return [...found].filter((d) => !OUT_OF_SCOPE.some((o) => d === o || d.startsWith(`${o}/`))).sort();
}

/** The few values that must carry across, read by pattern — these configs are plain object literals. */
function jestSettings(dir) {
    const src = readMaybeFromGit(`${dir}/jest.config.ts`);
    if (!src) return null;
    const str = (key) => new RegExp(`${key}:\\s*'([^']*)'`).exec(src)?.[1] ?? null;

    // transformIgnorePatterns lists packages Jest had to transform because they ship
    // ESM; under Vite the equivalent problem is externalisation, so the same list
    // becomes deps.inline. Alternation groups are matched directly because the outer
    // lookahead nests and a naive [^)]* stops at the wrong paren.
    const esm = [];
    const tip = /transformIgnorePatterns:\s*\[([\s\S]*?)\]/.exec(src);
    for (const m of tip?.[1].matchAll(/\(([A-Za-z0-9@/_.-]+(?:\|[A-Za-z0-9@/_.-]+)+)\)/g) ?? []) {
        for (const name of m[1].split('|')) {
            const clean = name.trim();
            if (clean.length > 1 && !clean.includes('.mjs')) esm.push(clean);
        }
    }

    const aliases = [];
    const mnm = /moduleNameMapper:\s*\{([\s\S]*?)\n\s*\}/.exec(src);
    for (const m of mnm?.[1].matchAll(/'([^']+)':\s*'([^']+)'/g) ?? []) {
        aliases.push({ find: m[1], replacement: m[2] });
    }

    return {
        testEnvironment: str('testEnvironment'),
        coverageDirectory: str('coverageDirectory'),
        setupFiles: [...src.matchAll(/setupFilesAfter(?:Env|Each):\s*\[([^\]]*)\]/g)].flatMap((m) =>
            [...m[1].matchAll(/'([^']+)'/g)].map((x) => x[1])
        ),
        esm,
        aliases
    };
}

/** Jest environment -> Vitest. Absent means the project inherited the preset's jsdom. */
function environmentFor(jestEnv) {
    if (!jestEnv) return 'jsdom';
    if (jestEnv.includes('happy-dom')) return 'happy-dom';
    if (jestEnv === 'node') return 'node';
    return 'jsdom';
}

/**
 * Framework detection by test-file extension rather than by dependency list: `.tsx`
 * is what the Angular compiler actually chokes on, with
 * `Cannot parse … Expected ',', got ':'`, and that took all 18 of sdk-react's files
 * down before any of them ran.
 */
function frameworkFor(dir) {
    let tsx = false;
    let vue = false;
    const walk = (rel) => {
        for (const e of readdirSync(join(CW, rel), { withFileTypes: true })) {
            if (e.name === 'node_modules' || e.name.startsWith('.')) continue;
            if (e.isDirectory()) walk(`${rel}/${e.name}`);
            else if (/\.(spec|test)\.tsx$/.test(e.name)) tsx = true;
            else if (e.name.endsWith('.vue')) vue = true;
        }
    };
    try {
        walk(dir);
    } catch {
        /* nothing to walk */
    }
    if (vue) return 'vue';
    return tsx ? 'react' : 'angular';
}

function nameOf(dir) {
    const p = join(CW, dir, 'project.json');
    return existsSync(p) ? (JSON.parse(readFileSync(p, 'utf8')).name ?? dir) : dir;
}

function generate(dir) {
    const cfg = jestSettings(dir);
    if (!cfg) return { dir, skipped: 'no jest.config.ts on disk or in git' };

    const name = nameOf(dir);
    const up = '../'.repeat(dir.split('/').length);
    const workspaceRel = up.slice(0, -1) || '.';
    const env = environmentFor(cfg.testEnvironment);
    const framework = frameworkFor(dir);

    const coverageDirWs = cfg.coverageDirectory
        ? cfg.coverageDirectory.replace(/^(\.\.\/)+/, '')
        : `coverage/${dir}`;

    const setupFiles = cfg.setupFiles.map((f) => f.replace('<rootDir>/', ''));
    // Only `/` is escaped in these package regexes. Escaping `@` as well produces
    // `\@`, which eslint's no-useless-escape rejects and which broke the pre-commit
    // hook on 8 generated configs.
    const inline = [...ANGULAR_INSTANCE_GUARD, ...cfg.esm.map((p) => `/${p.replace(/\//g, '\\/')}/`)];

    const pluginImport = {
        angular: "import angular from '@analogjs/vite-plugin-angular';",
        react: "import react from '@vitejs/plugin-react';",
        vue: "import vue from '@vitejs/plugin-vue';"
    }[framework];
    const pluginCall = { angular: 'angular()', react: 'react()', vue: 'vue()' }[framework];
    const generatorHint =
        framework === 'angular'
            ? '@nx/angular:library --unitTestRunner=vitest-analog'
            : `@nx/${framework}:library --unitTestRunner=vitest`;

    const aliasBlock = cfg.aliases.length
        ? `
    resolve: {
        alias: [
${cfg.aliases
    .map(
        (a) =>
            `            { find: ${JSON.stringify(a.find)}, replacement: resolve(__dirname, ${JSON.stringify(a.replacement.replace('<rootDir>/', './'))}) }`
    )
    .join(',\n')}
        ]
    },`
        : '';

    const body = `/// <reference types='vitest' />
${pluginImport}
import { nxViteTsPaths } from '@nx/vite/plugins/nx-tsconfig-paths.plugin';
import { defineConfig } from 'vite';
${cfg.aliases.length ? "\nimport { resolve } from 'path';\n" : ''}
/**
 * GENERATED by tools/generate-vite-configs.mjs. Regenerate rather than hand-editing.
 *
 * Shaped after what nx g ${generatorHint} produces, so migrated projects match the
 * generators and the workspace keeps ONE convention instead of a migration-specific
 * one. Deviations from that base, each with its reason:
 *
 *   environment: '${env}'  carried from this project's jest.config.ts so its DOM
 *                          surface does not change (FR-007). Nx defaults to jsdom.
 *   reporters / coverage   reproduce the CI artifacts the pipeline already consumes
 *                          (FR-008). Nx emits 'default' only.
 *   deps.inline            everything importing @angular/core must resolve to one
 *                          instance; without it components fail on 'ngModule' of
 *                          null. See the generator for the measurement.
 */
export default defineConfig(() => ({
    root: __dirname,
    cacheDir: '${up}node_modules/.vite/${dir}',
    // nxViteTsPaths(), NOT tsconfigPaths(). Nx prints a deprecation notice for this
    // plugin (removal in v24) and points at vite-tsconfig-paths — but the two are not
    // interchangeable in this workspace. nxViteTsPaths knows the Nx project layout and
    // resolves sibling libraries consumed FROM SOURCE, which a bare tsconfigPaths()
    // cannot: with it, edit-content runs 1,658 tests; without it, 0, because
    // "@dotcms/utils-testing" resolves and then fails to load from outside the project
    // root. Passing tsconfigPaths the workspace base tsconfig and widening
    // server.fs.allow were both tried and neither closed the gap.
    //
    // Follow-up, not a blocker: when Nx removes it in v24 this needs revisiting, and
    // Nx will have to offer a path for exactly this case.
    plugins: [${pluginCall}, nxViteTsPaths()],${aliasBlock}
    test: {
        name: '${name}',
        watch: false,
        globals: true,
        environment: '${env}',
        include: ['{src,tests}/**/*.{test,spec}.{js,mjs,cjs,ts,mts,cts,jsx,tsx}'],${setupFiles.length ? `\n        setupFiles: [${setupFiles.map((f) => `'${f}'`).join(', ')}],` : ''}
        server: {
            deps: {
                inline: [${inline.join(', ')}]
            }
        },
        reporters: [
            'default',
            'github-actions',
            ['junit', { outputFile: '${up}target/core-web-reports/${name}.xml' }]
        ],
        coverage: {
            reportsDirectory: '${up}${coverageDirWs}',
            reporter: ['html', 'lcov', 'text'],
            provider: 'v8' as const
        }
    }
}));
`;
    return { dir, name, framework, env, inline: inline.length, body };
}

const args = process.argv.slice(2);
const dryRun = args.includes('--dry-run');

if (args.includes('--list')) {
    for (const d of projects()) {
        const c = jestSettings(d);
        console.log(
            `${d.padEnd(46)} ${frameworkFor(d).padEnd(8)} env=${environmentFor(c?.testEnvironment).padEnd(10)} esm=${c?.esm.length ?? 0} alias=${c?.aliases.length ?? 0}`
        );
    }
    process.exit(0);
}

const targets = args.includes('--all') ? projects() : args.filter((a) => !a.startsWith('--'));
if (targets.length === 0) {
    console.error('usage: generate-vite-configs.mjs --all | --list | <project-dir> [...]');
    process.exit(2);
}

let written = 0;
for (const d of targets) {
    const r = generate(d);
    if (r.skipped) {
        console.log(`  skip ${d} (${r.skipped})`);
        continue;
    }
    if (!dryRun) writeFileSync(join(CW, d, 'vite.config.mts'), r.body);
    written++;
    console.log(`  ${dryRun ? 'would write' : 'wrote'} ${d} [${r.framework}] env=${r.env} inline=${r.inline}`);
}
console.log(`\n${written} config(s)${dryRun ? ' (dry run)' : ''}`);
