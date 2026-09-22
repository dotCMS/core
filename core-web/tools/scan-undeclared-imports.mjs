#!/usr/bin/env node
/**
 * Find third-party packages that import modules they never declare.
 *
 * Under the default (local) virtual store these resolve by accident: Node's walk-up escapes the
 * isolated package directory, passes through the hoisted fallback, and reaches core-web's own
 * node_modules, where the undeclared module happens to exist as a direct dependency. Move the
 * virtual store outside the project (virtualStoreType: global) and that last step disappears.
 *
 * So this scan exists to enumerate the accidental-rescue set BEFORE anyone switches, rather than
 * discovering it one failed bundle at a time. A bundler only reports what the code path it walked
 * happens to reach; a package behind a lazily loaded route stays invisible until an adopter hits
 * it months later.
 *
 * Run it against a LOCAL-store install. See specs/37573-pnpm-global-virtual-store/ (task T007).
 *
 *   node tools/scan-undeclared-imports.mjs [--json] [--all]
 *
 *   --json  machine-readable output
 *   --all   also report findings that nothing currently resolves (not accidental rescues, so not
 *           the cause of the global-store failures — usually genuinely optional guarded imports)
 *
 * Exit codes: 0 no rescued findings, 1 rescued findings present, 2 could not run.
 */

import { readFileSync, readdirSync, statSync, existsSync } from 'node:fs';
import { join, dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { builtinModules } from 'node:module';

const CORE_WEB = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const VIRTUAL_STORE = join(CORE_WEB, 'node_modules', '.pnpm');
const TOP_LEVEL = join(CORE_WEB, 'node_modules');

const BUILTINS = new Set([...builtinModules, ...builtinModules.map((m) => `node:${m}`)]);
const CODE_EXT = /\.(js|mjs|cjs)$/;

// Skip directories that ship inside a package but are never part of what a consumer imports, and
// bundles large enough that scanning them costs more than the findings are worth. A minified vendor
// bundle has already had its specifiers rewritten, so it tells us nothing about the manifest.
//
// This list is not fussiness. Without it the scan reports ~600 findings across ~100 packages —
// a package's own eslint config importing `@eslint/js`, an `examples/` folder importing `webpack`,
// a `testHelper.mjs` importing a sibling — none of which a bundler will ever resolve. The four
// defects that actually break the global-store build drown in it.
const SKIP_DIRS = new Set([
    'node_modules',
    'test',
    'tests',
    '__tests__',
    '__mocks__',
    'fixtures',
    'coverage',
    'example',
    'examples',
    'demo',
    'demos',
    'docs',
    'doc',
    'website',
    'scripts',
    'benchmark',
    'benchmarks',
    'bench',
    '.github'
]);

// A package's own tooling config is shipped but never imported by a consumer.
const SKIP_FILES = /^(eslint|rollup|vite|vitest|webpack|babel|jest|karma|prettier|tsup|tailwind|postcss)\.config\./;

const MAX_FILE_BYTES = 2 * 1024 * 1024;

const args = new Set(process.argv.slice(2));
const asJson = args.has('--json');
const showAll = args.has('--all');

/** Bare specifier -> the package it resolves from ('@scope/name' or 'name'). */
function packageOf(specifier) {
    const parts = specifier.split('/');
    return specifier.startsWith('@') ? parts.slice(0, 2).join('/') : parts[0];
}

function isBare(specifier) {
    return !specifier.startsWith('.') && !specifier.startsWith('/') && !specifier.startsWith('#');
}

/**
 * Pull bare import specifiers out of a source file.
 *
 * Deliberately regex-based rather than AST-based: this runs over ~1200 packages of shipped
 * JavaScript in a mix of module formats, and the cost of a parser that chokes on one exotic file
 * is a silent gap in the scan. The trade is that a computed `require(someVar)` is invisible here —
 * which is exactly why the cache-skipped build stays a hard requirement and this scan is only the
 * first pass.
 */
function specifiersIn(source) {
    const found = new Set();
    const patterns = [
        /\bfrom\s*['"]([^'"]+)['"]/g, // import x from 'y' / export x from 'y'
        /\bimport\s*['"]([^'"]+)['"]/g, // import 'y'
        /\bimport\s*\(\s*['"]([^'"]+)['"]\s*\)/g, // import('y')
        /\brequire\s*\(\s*['"]([^'"]+)['"]\s*\)/g // require('y')
    ];

    for (const pattern of patterns) {
        for (const [, specifier] of source.matchAll(pattern)) {
            if (isBare(specifier)) found.add(specifier);
        }
    }

    return found;
}

function* walkCode(dir) {
    let entries;
    try {
        entries = readdirSync(dir, { withFileTypes: true });
    } catch {
        return;
    }

    for (const entry of entries) {
        const full = join(dir, entry.name);
        if (entry.isDirectory()) {
            if (!SKIP_DIRS.has(entry.name) && !entry.name.startsWith('.')) yield* walkCode(full);
        } else if (CODE_EXT.test(entry.name) && !SKIP_FILES.test(entry.name)) {
            try {
                if (statSync(full).size <= MAX_FILE_BYTES) yield full;
            } catch {
                /* vanished mid-walk */
            }
        }
    }
}

/** Every package directory inside the virtual store, as { name, version, dir }. */
function installedPackages() {
    const packages = [];

    for (const entry of readdirSync(VIRTUAL_STORE, { withFileTypes: true })) {
        if (!entry.isDirectory() || entry.name === 'node_modules' || entry.name.startsWith('.')) {
            continue;
        }

        // node_modules/.pnpm/<key>/node_modules/<name>  — <name> may be scoped, so walk one level.
        const inner = join(VIRTUAL_STORE, entry.name, 'node_modules');
        let scopes;
        try {
            scopes = readdirSync(inner, { withFileTypes: true });
        } catch {
            continue;
        }

        for (const scope of scopes) {
            if (!scope.isDirectory()) continue;
            const candidates = scope.name.startsWith('@')
                ? readdirSync(join(inner, scope.name), { withFileTypes: true })
                      .filter((d) => d.isDirectory())
                      .map((d) => `${scope.name}/${d.name}`)
                : [scope.name];

            for (const name of candidates) {
                const dir = join(inner, name);
                const manifestPath = join(dir, 'package.json');
                if (!existsSync(manifestPath)) continue;

                try {
                    const manifest = JSON.parse(readFileSync(manifestPath, 'utf8'));
                    // The store key encodes the package this directory belongs to; anything else
                    // inside it is an injected dependency that has its own entry elsewhere.
                    if (manifest.name !== name) continue;
                    packages.push({ name, version: manifest.version ?? 'unknown', dir, manifest });
                } catch {
                    /* unreadable manifest — nothing to compare against */
                }
            }
        }
    }

    return packages;
}

/**
 * Root directories a consumer can actually reach, derived from the package's declared entry points
 * (`main`, `module`, `browser`, `exports`, `bin`).
 *
 * A finding inside one of these is a real hazard: a bundler following an import into the package
 * will walk that code. A finding outside them is shipped-but-unreachable — still a manifest defect,
 * but not one that can break this build, so it is reported separately instead of drowning the
 * signal.
 */
function entryRoots(manifest) {
    const roots = new Set();
    const add = (value) => {
        if (typeof value === 'string') {
            const clean = value.replace(/^\.\//, '');
            roots.add(clean.includes('/') ? clean.slice(0, clean.indexOf('/')) : '');
        } else if (Array.isArray(value)) value.forEach(add);
        else if (value && typeof value === 'object') Object.values(value).forEach(add);
    };

    add(manifest.main);
    add(manifest.module);
    add(manifest.browser);
    add(manifest.exports);
    add(manifest.bin);
    add(manifest.types);

    // No declared entry point at all: treat the package root as reachable rather than guess.
    if (roots.size === 0) roots.add('');

    return roots;
}

function isReachable(relativePath, roots) {
    const top = relativePath.includes('/') ? relativePath.slice(0, relativePath.indexOf('/')) : '';

    return roots.has(top);
}

function declaredNames(manifest) {
    return new Set([
        ...Object.keys(manifest.dependencies ?? {}),
        ...Object.keys(manifest.peerDependencies ?? {}),
        ...Object.keys(manifest.optionalDependencies ?? {})
    ]);
}

function main() {
    if (!existsSync(VIRTUAL_STORE)) {
        console.error(
            `No virtual store at ${VIRTUAL_STORE}\n` +
                `Run \`pnpm install\` in core-web first. This scan needs a LOCAL-store install:\n` +
                `it reports which undeclared imports are currently rescued from core-web's own\n` +
                `node_modules, which is precisely what the global store takes away.`
        );
        process.exit(2);
    }

    const findings = [];

    for (const { name, version, dir, manifest } of installedPackages()) {
        const declared = declaredNames(manifest);
        const roots = entryRoots(manifest);
        const seen = new Set();

        for (const file of walkCode(dir)) {
            const withinPackage = file.slice(dir.length + 1);
            let source;
            try {
                source = readFileSync(file, 'utf8');
            } catch {
                continue;
            }

            for (const specifier of specifiersIn(source)) {
                const target = packageOf(specifier);
                if (BUILTINS.has(specifier) || BUILTINS.has(target)) continue;
                if (target === name || declared.has(target)) continue;
                if (seen.has(specifier)) continue;
                seen.add(specifier);

                findings.push({
                    package: name,
                    version,
                    specifier,
                    sourceFile: file.slice(CORE_WEB.length + 1),
                    declared: false,
                    // Is this file reachable from the package's declared entry points? Unreachable
                    // findings are real manifest defects but cannot break this build.
                    entryReachable: isReachable(withinPackage, roots),
                    // The whole point: does this currently resolve from core-web's own top level?
                    // If yes, it is an accidental rescue and WILL break under the global store.
                    rescuedFrom: existsSync(join(TOP_LEVEL, target)) ? 'core-web/node_modules' : null
                });
            }
        }
    }

    const rescued = findings.filter((f) => f.rescuedFrom);
    // The work list: rescued AND reachable from a declared entry point.
    const actionable = rescued.filter((f) => f.entryReachable);
    const shipped = rescued.filter((f) => !f.entryReachable);

    if (asJson) {
        console.log(
            JSON.stringify(
                { actionable: actionable.length, unreachable: shipped.length, total: findings.length, findings: showAll ? findings : rescued },
                null,
                2
            )
        );
        process.exit(actionable.length > 0 ? 1 : 0);
    }

    if (actionable.length === 0) {
        console.log('No actionable undeclared imports found.');
        console.log(
            'Treat that as suspicious rather than reassuring: at the time of #37573 at least four\n' +
                'packages had this defect. A scan that finds nothing is usually broken.'
        );
    }

    const byPackage = new Map();
    for (const f of actionable) {
        if (!byPackage.has(f.package)) byPackage.set(f.package, []);
        byPackage.get(f.package).push(f);
    }

    for (const [pkg, group] of [...byPackage].sort()) {
        console.log(`${pkg}@${group[0].version}`);
        for (const f of group) console.log(`    imports '${f.specifier}'  -> ${f.sourceFile}`);
    }

    console.log(
        `\n${actionable.length} actionable finding(s) across ${byPackage.size} package(s):` +
            ` undeclared, currently rescued from core-web/node_modules, and reachable from the` +
            ` package's declared entry points.`
    );
    console.log(
        `${shipped.length} further rescue(s) sit in shipped-but-unreachable files (a package's own` +
            ` tooling, examples, test helpers). Real manifest defects, but a bundler never walks them,` +
            ` so they cannot break this build. Pass --all to see everything.`
    );
    console.log(
        '\nThe build remains the authority: a computed or dynamic specifier is invisible to a static' +
            '\nscan, so treat this as the starting work list, not the complete one.'
    );

    process.exit(actionable.length > 0 ? 1 : 0);
}

main();
