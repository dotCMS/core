import { readFileSync, readdirSync, existsSync, statSync } from 'node:fs';
import { join } from 'node:path';

import { SDK_DIST } from './bundle-probe.ts';

/**
 * Asserts that every `@dotcms/*` package a built package imports is declared in its own
 * `dependencies` or `peerDependencies`.
 *
 * Phantom dependencies are invisible from inside the monorepo: every SDK package resolves
 * every other one through the workspace, so an undeclared import works locally, works in the
 * examples, and works in CI — and fails only in a consumer's clean `node_modules`. That is
 * exactly how `@dotcms/types` shipped undeclared from `react`, `client`, `uve`, `angular` and
 * `vue`: all five import runtime enums (`UVE_MODE`, `UVEEventType`, `DotCMSUVEAction`,
 * `DotCMSEntityState`) out of shipped `.js`, while declaring it only as a devDependency, which
 * npm does not install for consumers. `npm i @dotcms/react` in an empty directory pulled in the
 * declared peers and then threw `Cannot find module '@dotcms/types'` on first import.
 *
 * A devDependency does not count. It is stripped from what a consumer installs, so treating it
 * as a declaration is the precise mistake this test exists to catch.
 *
 * The declaration to add is `"@dotcms/types": "0.0.0"`, which looks wrong out of context and is
 * not: cross-SDK dependencies are written with that sentinel because the release workflow
 * (`deploy-javascript-sdk/action.yml`) rewrites every `@dotcms/*` entry in `dependencies`,
 * `peerDependencies` and `devDependencies` to the exact release version at publish. It matches on
 * the dependency's name and overwrites the value unconditionally, so the value in the source tree
 * is a slot marker and nothing more. `0.0.0` is the right marker precisely because it can never
 * resolve — if the rewrite ever fails to run, the install breaks loudly instead of quietly pinning
 * consumers to a stale real version.
 *
 * `.d.ts` files are scanned alongside the JavaScript: a public declaration file that references
 * a package a consumer does not have breaks `tsc` just as surely as a missing runtime import
 * breaks `node`.
 */

/** Published libraries. Each must be built before this spec runs — see `implicitDependencies`. */
const PACKAGES = ['react', 'client', 'uve', 'types', 'analytics', 'angular', 'vue', 'experiments'];

/** `from 'x'`, `import 'x'`, `import('x')` and `require('x')` — not bare strings that merely look like specifiers. */
const SPECIFIER = /(?:from|import|require)\s*\(?\s*['"](@dotcms\/[^'"]+)['"]/g;

const SCANNED_EXTENSIONS = ['.js', '.mjs', '.cjs', '.d.ts', '.d.mts'];

/** Every scannable file under `dir`, recursively. */
function shippedFiles(dir: string): string[] {
    return readdirSync(dir, { withFileTypes: true }).flatMap((entry) => {
        const path = join(dir, entry.name);

        if (entry.isDirectory()) {
            return shippedFiles(path);
        }

        return SCANNED_EXTENSIONS.some((extension) => entry.name.endsWith(extension)) ? [path] : [];
    });
}

/** `@dotcms/types/internal` -> `@dotcms/types`. A subpath is still the same package to install. */
function packageOf(specifier: string): string {
    const [scope, name] = specifier.split('/');

    return `${scope}/${name}`;
}

describe('declared dependencies', () => {
    describe.each(PACKAGES)('@dotcms/%s', (name) => {
        test('should declare every @dotcms package it imports', () => {
            const pkgDir = join(SDK_DIST, name);

            if (!existsSync(pkgDir) || !statSync(pkgDir).isDirectory()) {
                throw new Error(
                    `[declared-deps] ${pkgDir} does not exist. Build the SDKs first:\n` +
                        `  pnpm nx run-many -t build --projects='sdk-*'`
                );
            }

            const pkg = JSON.parse(readFileSync(join(pkgDir, 'package.json'), 'utf-8'));
            const declared = new Set([
                pkg.name,
                ...Object.keys(pkg.dependencies ?? {}),
                ...Object.keys(pkg.peerDependencies ?? {})
            ]);

            const undeclared = new Map<string, string[]>();

            for (const file of shippedFiles(pkgDir)) {
                const source = readFileSync(file, 'utf-8');

                for (const [, specifier] of source.matchAll(SPECIFIER)) {
                    const imported = packageOf(specifier);

                    if (declared.has(imported)) {
                        continue;
                    }

                    const sites = undeclared.get(imported) ?? [];
                    const relative = file.slice(pkgDir.length + 1);

                    if (!sites.includes(relative)) {
                        sites.push(relative);
                    }

                    undeclared.set(imported, sites);
                }
            }

            const report = [...undeclared].map(
                ([imported, sites]) => `${imported} (imported by ${sites.slice(0, 3).join(', ')})`
            );

            expect(
                report,
                `@dotcms/${name} imports packages it does not declare. Add each to ` +
                    'peerDependencies with the "0.0.0" sentinel the release workflow rewrites ' +
                    'to the exact release version. A devDependency does not count — consumers ' +
                    'never install it.'
            ).toEqual([]);
        });
    });
});
