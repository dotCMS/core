#!/usr/bin/env node
/**
 * Builds the published @dotcms/angular with the Angular pinned in this folder's package.json
 * — the oldest version the SDK supports — instead of core-web's. Run through
 * `pnpm nx build sdk-angular`, which builds @dotcms/client, @dotcms/uve and @dotcms/types
 * first; tsconfig.build.json reads their types from dist/. See README.md for why.
 */
import { execFileSync } from 'node:child_process';
import { createHash } from 'node:crypto';
import { existsSync, readdirSync, readFileSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const here = dirname(fileURLToPath(import.meta.url));
const readJson = (file) => JSON.parse(readFileSync(file, 'utf8'));

const lockHash = createHash('sha256')
    .update(readFileSync(join(here, 'package-lock.json')))
    .digest('hex');
const installedMarker = join(here, 'node_modules', '.toolchain-lock-hash');

// Reinstall only when the lockfile changed, so repeated local builds stay fast.
// --ignore-scripts: nothing in this toolchain needs an install script, and PR builds run it.
if (!existsSync(installedMarker) || readFileSync(installedMarker, 'utf8') !== lockHash) {
    execFileSync('npm', ['ci', '--ignore-scripts', '--no-audit', '--no-fund'], {
        cwd: here,
        stdio: 'inherit'
    });
    writeFileSync(installedMarker, lockHash);
}

// The SDK sources live under core-web, so a plain import of @angular/* would resolve to
// core-web's newer Angular. Map every @angular entry point to this folder's copy instead.
// The map is built from each package's "exports", because Angular 21+ only exposes its types
// there (types/http.d.ts, not http/index.d.ts): a static wildcard silently misses them.
const angularDir = join(here, 'node_modules/@angular');
const angularPaths = {};
for (const pkg of readdirSync(angularDir)) {
    for (const [subpath, target] of Object.entries(
        readJson(join(angularDir, pkg, 'package.json')).exports ?? {}
    )) {
        if (!subpath.includes('*') && typeof target?.types === 'string') {
            angularPaths[`@angular/${pkg}${subpath.slice(1)}`] = [
                `node_modules/@angular/${pkg}/${target.types.slice(2)}`
            ];
        }
    }
}

const baseConfig = readJson(join(here, 'tsconfig.build.json'));
const generatedConfig = join(here, 'node_modules', '.tsconfig.generated.json');
writeFileSync(
    generatedConfig,
    JSON.stringify(
        {
            extends: '../tsconfig.build.json',
            compilerOptions: {
                baseUrl: '..',
                paths: { ...baseConfig.compilerOptions.paths, ...angularPaths }
            }
        },
        null,
        2
    )
);

// Read the CLI path from ng-packagr's own manifest: it moved between majors (cli/ in 19,
// src/cli/ in 21), and a hardcoded path breaks the build on the next toolchain bump.
const ngPackagrDir = join(here, 'node_modules/ng-packagr');
const ngPackagrBin = readJson(join(ngPackagrDir, 'package.json')).bin['ng-packagr'];

execFileSync(
    process.execPath,
    [
        join(ngPackagrDir, ngPackagrBin),
        '--project',
        join(here, '../ng-package.json'),
        '--config',
        generatedConfig
    ],
    { stdio: 'inherit' }
);
