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
 *   plugins: [<framework>(), tsconfigPaths({ root, projects })]
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
 * Projects that opt OUT of per-file isolation — dir -> the measurement that earned it.
 *
 * research.md R-9 chose `isolate: true` for the workspace so the runner matches the
 * module-registry-per-file model the specs were written under (Jest's), rather than the
 * Angular builder's Karma-style shared context. It allows exactly this: "treat any
 * project where that is too slow as a measured exception rather than flipping the
 * default." This map is that list of exceptions, and `isolate: true` is emitted
 * explicitly everywhere else — R-9 asked for it to be explicit and it had been left
 * implicit in the Vitest default.
 *
 * The bar for an entry, all four:
 *   1. the project passes with `--no-isolate` and the SAME test count,
 *   2. three consecutive passing runs, plus one with `--sequence.shuffle`, because the
 *      risk R-9 names is turning a healthy suite into an order-dependent one,
 *   3. a real saving in `pnpm test:profile`, not noise,
 *   4. the numbers written here, so the next person can re-check rather than re-derive.
 *
 * Nothing goes in unmeasured, and a project whose suite is red cannot qualify: without
 * a green run there is nothing to compare against.
 *
 * EMPTY ON PURPOSE — this was measured and `isolate: false` earns nothing here.
 * Best-of-two runs, warm Vite cache on both sides, nothing else on the machine:
 *
 *   project                    isolate: true   --no-isolate
 *   dotcms-ui                     34.16s          33.95s
 *   edit-content                  27.32s          27.89s
 *   portlets-edit-ema-portlet     28.05s          28.36s
 *   data-access                    9.54s           9.48s
 *   portlets-content-drive        28s             35s
 *   total of the first four       99.07s          99.68s   (+0.6%)
 *
 * The `setup` phase is the tell: it does not drop. dotcms-ui reads 122.27s isolated and
 * 125.65s non-isolated. If isolation were the thing making setup run per FILE rather
 * than per worker, that number would fall, and it does not — so the per-file cost lives
 * somewhere isolation does not reach.
 *
 * Read this before re-running the experiment: an earlier attempt appeared to show
 * −42% on data-access and it was an artefact. The two variants ran back-to-back in one
 * batch, so the `isolate: true` arm paid the COLD Vite cache and the `--no-isolate` arm
 * inherited it warm. Measure each arm best-of-two, or you will rediscover the same
 * phantom. `pnpm test:profile <project> --runs=2 -- --no-isolate` does this correctly.
 */
const NO_ISOLATE = {};

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
/**
 * Packages that must resolve to ONE @angular/core instance — DERIVED, not listed.
 *
 * This started as a hand-maintained list and grew by one entry per debugging session:
 * Angular, then Spectator, then zone.js, then PrimeNG, then @ngrx, then ng-mocks,
 * then ngx-markdown... Each addition came from reading a stack trace, and each miss
 * cost a full suite run. The rule underneath all of them is simple: anything that
 * ships Angular-compiled code, or compiles Angular at test time, registers against
 * whichever @angular/core it loads. Externalised, that is not the one the specs use,
 * and Angular reports NG0203 / NG0303 or a null injector.
 *
 * So it is computed from the manifest: every dependency that itself depends on
 * @angular/*, minus build-time tooling that never runs inside a test. That way a new
 * Angular-based dependency is covered the day it is added, instead of the day someone
 * reads its stack trace.
 */
const BUILD_ONLY = [
    '@angular-devkit/build-angular',
    '@angular-eslint/schematics',
    '@angular/build',
    '@angular/compiler-cli',
    '@nx/angular',
    'ng-packagr',
    'angular-eslint'
];

function angularInstanceGuard() {
    const pkg = JSON.parse(readFileSync(join(CW, 'package.json'), 'utf8'));
    const names = Object.keys({ ...pkg.dependencies, ...pkg.devDependencies });
    const found = [];
    for (const name of names) {
        if (BUILD_ONLY.includes(name)) continue;
        const manifest = join(CW, 'node_modules', name, 'package.json');
        if (!existsSync(manifest)) continue;
        let j;
        try {
            j = JSON.parse(readFileSync(manifest, 'utf8'));
        } catch {
            continue;
        }
        const related = { ...j.peerDependencies, ...j.dependencies };
        if (Object.keys(related).some((k) => k.startsWith('@angular/'))) found.push(name);
    }
    // zone.js and the workspace's own sources are not dependencies of an Angular
    // package but belong here for the same reason: one instance, one registry. The
    // workspace entry matters because libraries here are consumed FROM SOURCE through
    // tsconfig paths, so with Nx's project-level root they would be externalised.
    return [
        '/[\\\\/](libs|apps)[\\\\/]/',
        '/zone\\.js/',
        '/@primeuix/',
        ...found.sort().map((n) => `/${n.replace(/\//g, '\\/')}/`)
    ];
}


/**
 * Read from the working tree, falling back to git history so this script stays
 * re-runnable after the jest configs are deleted.
 *
 * Finds the commit that DELETED the file and reads its parent, rather than probing a
 * fixed number of refs back. An earlier version tried HEAD, HEAD~1 and HEAD~2 — which
 * worked until a third commit shifted the refs, after which the generator silently
 * reported "0 config(s)" and regenerated nothing. It looked like success.
 */
/**
 * Jest config file names, in the order Jest itself resolves them.
 *
 * NOT just `jest.config.ts`, and that is a fix rather than completeness for its own
 * sake: `libs/portlets/dot-auth` names its config `jest.config.cts`. A `.ts`-only
 * lookup found nothing there, so no Vitest config was generated, the project's `test`
 * target simply stopped existing once the Jest plugin was removed, and its 103 tests
 * (5 spec files, in the captured baseline) went from passing to not running at all —
 * with nothing red to show for it. `nx run-many -t test` cannot report a project it
 * has no target for.
 */
const JEST_CONFIG_NAMES = ['jest.config.ts', 'jest.config.cts', 'jest.config.mts', 'jest.config.js', 'jest.config.cjs'];

const isJestConfig = (name) => JEST_CONFIG_NAMES.includes(name);

function readMaybeFromGit(relPath) {
    const abs = join(CW, relPath);
    if (existsSync(abs)) return readFileSync(abs, 'utf8');

    // Git commands run from the REPO ROOT, not from core-web: a pathspec is resolved
    // relative to cwd, so passing `core-web/...` while inside core-web matched nothing
    // and rev-list came back empty — which the caller read as "no jest config" and
    // silently generated zero files.
    const repoRoot = resolve(CW, '..');
    const gitPath = `core-web/${relPath}`;
    try {
        // The most recent commit that touched this path; for a deleted file that is
        // the deleting commit, so its parent still holds the content.
        const sha = execFileSync('git', ['log', '--diff-filter=D', '--format=%H', '-1', '--', gitPath], {
            cwd: repoRoot,
            encoding: 'utf8',
            stdio: ['pipe', 'pipe', 'ignore']
        }).trim();
        if (!sha) return null;
        for (const ref of [`${sha}^`, sha]) {
            try {
                return execFileSync('git', ['show', `${ref}:${gitPath}`], {
                    cwd: repoRoot,
                    encoding: 'utf8',
                    stdio: ['pipe', 'pipe', 'ignore']
                });
            } catch {
                /* the file was added or deleted at this ref; try the parent */
            }
        }
    } catch {
        /* not in history at all */
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
            if (isJestConfig(e.name)) found.add(rel);
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
    let src = null;
    for (const name of JEST_CONFIG_NAMES) {
        src = readMaybeFromGit(`${dir}/${name}`);
        if (src) break;
    }
    if (!src) return null;
    const str = (key) => new RegExp(`${key}:\\s*'([^']*)'`).exec(src)?.[1] ?? null;

    // transformIgnorePatterns lists packages Jest had to transform because they ship
    // ESM; under Vite the equivalent problem is externalisation, so the same list
    // becomes deps.inline.
    //
    // Strip the regex machinery and keep the package-shaped tokens, rather than trying
    // to match an alternation group. An earlier version looked for `(a|b|c)` and so
    // silently produced an EMPTY list for the two projects written as
    // `(?:\.mjs$|a|b|c)` — the `?:`, `\` and `$` are outside a package-name character
    // class, so the group never matched. dotcms-ui was one of them: y-protocols, lib0,
    // @tiptap and friends stayed externalised, Node's ESM resolver could not find
    // `y-protocols/awareness`, and ELEVEN spec files failed to load — 307 tests missing
    // against the Jest baseline, with zero reported failures. A pattern that quietly
    // matches nothing is the worst kind.
    const esm = [];
    const tip = /transformIgnorePatterns:\s*\[([\s\S]*?)\]/.exec(src);
    for (const lit of tip?.[1].matchAll(/'([^']*)'/g) ?? []) {
        const bare = lit[1]
            .replace(/^node_modules\//, '')
            .replace(/\(\?[:!]/g, '|')
            .replace(/[()]/g, '|')
            .replace(/\.\*/g, '')
            .replace(/\\?\.mjs\$?/g, '')
            .replace(/\$/g, '');
        for (const token of bare.split('|')) {
            const name = token.trim();
            // Package-shaped only: `@scope/name`, `name`, or either with a trailing
            // slash (`d3/`, `internmap/`). This drops the `-` left behind by `d3(/|-)`.
            if (/^@?[A-Za-z0-9][A-Za-z0-9._-]*(?:\/[A-Za-z0-9._-]*)?$/.test(name) && name.length > 1) {
                if (INLINE_EXCLUDE.has(name)) continue;
                esm.push(name);
            }
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

/**
 * Packages that appear in a transformIgnorePatterns list but must NOT be inlined.
 *
 * The two lists are not the same question. Jest's was "transform this, it ships ESM";
 * Vite's deps.inline is "run this through my transform instead of importing it as-is",
 * and for a package Vite already handles natively that is a downgrade. `uuid` publishes
 * an ESM wrapper over a CJS bundle (export const v1 = uuid.v1); inlined, Vite's
 * default-interop leaves the default undefined and two dot-templates spec files died on
 * "Cannot read properties of undefined (reading 'v1')".
 */
const INLINE_EXCLUDE = new Set(['uuid']);

/** Jest environment -> Vitest. Absent means the project inherited the preset's jsdom. */
function environmentFor(jestEnv) {
    if (!jestEnv) return 'jsdom';
    if (jestEnv.includes('happy-dom')) return 'happy-dom';
    if (jestEnv === 'node') return 'node';
    return 'jsdom';
}

/**
 * Pin the DOM environment's base URL to Jest's default.
 *
 * jest-jsdom served every spec from `http://localhost/`; Vitest's jsdom and
 * happy-dom both default to `http://localhost:3000/`. Specs that read
 * `window.location.host` or build a URL against it therefore changed answer on
 * migration — dot-events-socket asserted `ws://localhost/...` and got
 * `ws://localhost:3000/...`. Restoring the origin is FR-007 environment parity,
 * not a workaround, and it is one option rather than an edit per spec.
 */
function environmentOptionsFor(env) {
    if (env === 'jsdom') return "\n        environmentOptions: { jsdom: { url: 'http://localhost/' } },";
    if (env === 'happy-dom') return "\n        environmentOptions: { happyDOM: { url: 'http://localhost/' } },";

    return '';
}

/**
 * Framework detection by test-file extension rather than by dependency list: `.tsx`
 * is what the Angular compiler actually chokes on, with
 * `Cannot parse … Expected ',', got ':'`, and that took all 18 of sdk-react's files
 * down before any of them ran.
 *
 * ANGULAR IS DETECTED, NOT ASSUMED. This function used to `return tsx ? 'react' :
 * 'angular'`, making Angular the fallback for anything that was neither Vue nor React.
 * That is how apps/mcp-server and libs/sdk/{ai,client,uve,create-app} — plain
 * TypeScript, not one `@angular/*` import between them — each ended up loading
 * @analogjs/vite-plugin-angular plus the full Angular deps.inline guard: AOT/JIT
 * transform machinery over files that never mention Angular. It shows up as `transform`
 * time in `pnpm test:profile` and buys nothing.
 *
 * 'none' means no framework plugin and no Angular instance guard. Vite handles plain TS
 * natively and tsconfigPaths still resolves the workspace, so nothing is lost.
 */
function frameworkFor(dir) {
    let tsx = false;
    let vue = false;
    let angular = false;
    const walk = (rel) => {
        for (const e of readdirSync(join(CW, rel), { withFileTypes: true })) {
            if (e.name === 'node_modules' || e.name.startsWith('.')) continue;
            const child = `${rel}/${e.name}`;
            if (e.isDirectory()) {
                walk(child);
                continue;
            }
            if (/\.(spec|test)\.tsx$/.test(e.name)) tsx = true;
            else if (e.name.endsWith('.vue')) vue = true;

            // An actual import, not the mere string '@angular/'. libs/sdk/create-app
            // SCAFFOLDS Angular apps: its constants list '@angular/core',
            // '@angular/forms' and friends as packages to write into someone else's
            // package.json. A substring test reads that as an Angular project and hands
            // it back the compiler it was trying to avoid.
            if (!angular && /\.[cm]?ts$/.test(e.name)) {
                try {
                    if (/(?:from|import\s*\(?)\s*['"]@angular\//.test(readFileSync(join(CW, child), 'utf8'))) angular = true;
                } catch {
                    /* unreadable file tells us nothing */
                }
            }
        }
    };
    try {
        walk(dir);
    } catch {
        /* nothing to walk */
    }
    if (vue) return 'vue';
    if (tsx) return 'react';
    return angular ? 'angular' : 'none';
}

/**
 * Top-level directories the include glob has to cover.
 *
 * Jest's testMatch was rooted at the project, not at src, so a spec anywhere in the project
 * ran. A fixed `{src,tests}` glob silently drops the ones that live elsewhere, and the
 * project still reports green on the files it did find — sdk-ai's
 * `scripts/spec-transform.spec.ts` (10 tests) vanished exactly that way. `src` and
 * `tests` stay in unconditionally so the 45 projects that keep their specs there
 * generate byte-identical configs; anything else is added only where specs are.
 */
function includeRootsFor(dir) {
    const roots = new Set(['src', 'tests']);
    const walk = (rel, top) => {
        for (const e of readdirSync(join(CW, rel), { withFileTypes: true })) {
            if (e.name === 'node_modules' || e.name === 'dist' || e.name.startsWith('.')) continue;
            if (e.isDirectory()) walk(`${rel}/${e.name}`, top ?? e.name);
            else if (/\.(spec|test)\.[cm]?[jt]sx?$/.test(e.name) && top) roots.add(top);
        }
    };
    try {
        walk(dir, null);
    } catch {
        /* nothing to walk */
    }

    return [...roots].sort();
}

/**
 * The `isolate` line, explicit either way.
 *
 * R-9 asked for `isolate: true` to be set "explicitly on every migrated project" and
 * that was the one part of it that never shipped — it was left to Vitest's default,
 * which reads as nobody having decided. An unstated default is exactly how the Angular
 * builder's Karma-style `isolate: false` would have slipped in unnoticed if a project
 * ever moved to that builder.
 */
function isolateBlock(dir) {
    const reason = NO_ISOLATE[dir];
    if (!reason) {
        return `        // Explicit per research.md R-9: Jest gave every spec file a fresh module registry
        // and these specs were written under that. Measured exceptions live in NO_ISOLATE
        // in tools/generate-vite-configs.mjs.
        isolate: true,`;
    }
    return `        // MEASURED exception to R-9's isolate: true — ${reason}
        // Re-check: pnpm test:profile ${nameOf(dir)} (and once with -- --sequence.shuffle).
        isolate: false,`;
}

function nameOf(dir) {
    const p = join(CW, dir, 'project.json');
    return existsSync(p) ? (JSON.parse(readFileSync(p, 'utf8')).name ?? dir) : dir;
}

function generate(dir) {
    const cfg = jestSettings(dir);
    if (!cfg) return { dir, skipped: 'no jest config on disk or in git' };

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
    // The Angular instance guard is the expensive half of this list and only Angular
    // projects need it. A plain-TS project still gets the workspace-source entry — its
    // siblings are consumed from source through tsconfig paths, not from node_modules —
    // plus whatever its old transformIgnorePatterns named.
    const inline =
        framework === 'none'
            ? ['/[\\\\/](libs|apps)[\\\\/]/', ...cfg.esm.map((p) => `/${p.replace(/\//g, '\\/')}/`)]
            : [...angularInstanceGuard(), ...cfg.esm.map((p) => `/${p.replace(/\//g, '\\/')}/`)];

    const pluginImport = {
        angular: "import angular from '@analogjs/vite-plugin-angular';",
        react: "import react from '@vitejs/plugin-react';",
        vue: "import vue from '@vitejs/plugin-vue';",
        none: ''
    }[framework];
    const pluginCall = { angular: 'angular()', react: 'react()', vue: 'vue()', none: null }[framework];
    const generatorHint = {
        angular: '@nx/angular:library --unitTestRunner=vitest-analog',
        react: '@nx/react:library --unitTestRunner=vitest',
        vue: '@nx/vue:library --unitTestRunner=vitest',
        none: '@nx/js:library --unitTestRunner=vitest'
    }[framework];

    const aliasBlock = cfg.aliases.length
        ? `
    resolve: {
        alias: [
${cfg.aliases
    .map(
        (a) =>
            // Jest's moduleNameMapper keys are REGEX; Vite's `find` is a literal string
            // unless given a RegExp. Emitting the pattern as a string left `^` and `$`
            // as characters to match, so `^virtual:sdk-version$` never matched and 8 of
            // sdk-client's 15 test files failed to load — silently, with 0 reported
            // failures, because a file that cannot load reports no tests at all.
            `            { find: ${/[\^$\\[\]()|*+?]/.test(a.find) ? `/${a.find.replace(/\//g, '\\/')}/`  /* a bare / would close the literal: @primeuix/motion */ : JSON.stringify(a.find)}, replacement: resolve(__dirname, ${JSON.stringify(a.replacement.replace('<rootDir>/', './'))}) }`
    )
    .join(',\n')}
        ]
    },`
        : '';

    const body = `/// <reference types='vitest' />
${pluginImport ? `${pluginImport}\n` : ''}import tsconfigPaths from 'vite-tsconfig-paths';
import { defineConfig } from 'vite';

import { resolve } from 'path';

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
${
     framework === 'none'
         ? ` *   no framework plugin    nothing here imports @angular/*, so this project gets neither
 *                          the Angular plugin nor the Angular deps.inline guard — both
 *                          were pure transform cost. deps.inline keeps only the
 *                          workspace sources and this project's own ESM packages.`
         : ` *   deps.inline            everything importing @angular/core must resolve to one
 *                          instance; without it components fail on 'ngModule' of
 *                          null. See the generator for the measurement.`
 }
 */
export default defineConfig(() => ({
    root: __dirname,
    cacheDir: '${up}node_modules/.vite/${dir}',
    // tsconfigPaths(), with BOTH options supplied. Nx deprecated nxViteTsPaths (removal
    // in v24) and points here, and the two are interchangeable only if this plugin is
    // told where to look: a bare tsconfigPaths() resolves nothing across projects,
    // which is what made an earlier attempt report 0 tests for edit-content.
    //
    //   root      the workspace, not the project. Vite's own root is __dirname, so
    //             without this the plugin only sees this project's tsconfig and every
    //             \`@dotcms/*\` sibling stays unresolved.
    //   projects  tsconfig.base.json ONLY, and that is a fix rather than a
    //             simplification: nxViteTsPaths picked the project tsconfig by a fixed
    //             preference (app, else lib, else json) and never looked at
    //             tsconfig.spec.json, so sdk-experiments' build-only paths into dist/
    //             leaked into its test run and two spec files died on
    //             "Cannot find module '@dotcms/types'". Naming the base config takes
    //             the source paths and nothing else.
    plugins: [
${pluginCall ? `        ${pluginCall},\n` : ''}        tsconfigPaths({ root: resolve(__dirname, '${workspaceRel}'), projects: ['tsconfig.base.json'] })
    ],${aliasBlock}
    test: {
        name: '${name}',
        watch: false,
        globals: true,
        // Jest routed every .css/.scss/.sass/.less import through identity-obj-proxy
        // (@nx/jest/plugins/resolver), so a CSS-module class came back as its own name.
        // Vitest's default 'stable' strategy returns _name_hash instead, and
        // sdk-react's Column test — which asserts toHaveClass('col-start-2') on a class
        // read out of a *.module.css — failed on the hash. 'non-scoped' restores the
        // Jest reading.
        //
        // 'include: []' is load-bearing, not decoration: an object under 'css' turns CSS
        // PROCESSING on, and processing a .scss file starts sass-embedded, whose dart
        // subprocess outlives the run — content-drive finished all 30 files and then
        // hung indefinitely without printing a summary. An empty include leaves every
        // file unprocessed while the module-name strategy still applies.
        css: { include: [], modules: { classNameStrategy: 'non-scoped' } },
${isolateBlock(dir)}
        environment: '${env}',${environmentOptionsFor(env)}
        include: ['{${includeRootsFor(dir).join(',')}}/**/*.{test,spec}.{js,mjs,cjs,ts,mts,cts,jsx,tsx}'],${setupFiles.length ? `\n        setupFiles: [${setupFiles.map((f) => `'${f}'`).join(', ')}],` : ''}
        server: {
            deps: {
                inline: [${inline.join(', ')}]
            }
        },
        // 'github-actions' is GATED, not dropped: an explicit reporters array replaces
        // Vitest's environment-based auto-selection, so deleting the entry would take
        // the CI annotations with it — while leaving it in emitted ::error commands on
        // every local run, where nothing parses them. junit stays unconditional; CI
        // consumes those XML files (generates_test_results in .github/test-matrix.yml).
        reporters: process.env.GITHUB_ACTIONS
            ? ['default', 'github-actions', ['junit', { outputFile: '${up}target/core-web-reports/${name}.xml' }]]
            : ['default', ['junit', { outputFile: '${up}target/core-web-reports/${name}.xml' }]],
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
