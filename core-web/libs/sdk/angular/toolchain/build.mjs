#!/usr/bin/env node
/**
 * Builds the published @dotcms/angular with the Angular pinned in this folder's package.json
 * — the oldest version the SDK supports — instead of core-web's. Run through
 * `pnpm nx build sdk-angular`, which builds @dotcms/client, @dotcms/uve and @dotcms/types
 * first; tsconfig.build.json reads their types from dist/. See README.md for why.
 */
import { execFileSync } from 'node:child_process';
import { createHash } from 'node:crypto';
import { existsSync, readFileSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const here = dirname(fileURLToPath(import.meta.url));
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

execFileSync(
    process.execPath,
    [
        join(here, 'node_modules/ng-packagr/cli/main.js'),
        '--project',
        join(here, '../ng-package.json'),
        '--config',
        join(here, 'tsconfig.build.json')
    ],
    { stdio: 'inherit' }
);
