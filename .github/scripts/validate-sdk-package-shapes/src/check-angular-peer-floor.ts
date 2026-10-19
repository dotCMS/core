#!/usr/bin/env node
import { readdirSync, readFileSync, statSync } from 'fs';
import { join } from 'path';

import { deriveAngularFloor, readPartialDeclarations, validateAngularPeerFloor } from './angular-peer-floor';

/**
 * CLI entry point. Usage:
 *
 *   node dist/check-angular-peer-floor.js <path-to-package.json> <fesm2022-dir-or-.mjs-file>
 *
 * Reads the Angular version floor out of a BUILT @dotcms/angular (the partial-compilation
 * stamps in its FESM output) and fails if the source package.json's @angular/* peer ranges
 * admit anything lower. The source manifest is checked rather than the dist copy because it
 * is where the range is edited; ng-packagr copies peerDependencies through unchanged and the
 * release pipeline only rewrites @dotcms/* entries.
 *
 * Unlike index.ts this needs the lib built first (`pnpm nx build sdk-angular` in core-web).
 */

function listBundles(target: string): string[] {
    if (!statSync(target).isDirectory()) {
        return [target];
    }

    return readdirSync(target)
        .filter((file) => file.endsWith('.mjs'))
        .map((file) => join(target, file));
}

function main(): void {
    const [pkgPath, artifactPath] = process.argv.slice(2);

    if (!pkgPath || !artifactPath) {
        console.error('Usage: check-angular-peer-floor.js <path-to-package.json> <fesm2022-dir-or-.mjs-file>');
        process.exit(2);
    }

    const bundles = listBundles(artifactPath);
    const declarations = bundles.flatMap((bundle) => {
        try {
            return readPartialDeclarations(readFileSync(bundle, 'utf8'));
        } catch (error) {
            throw new Error(`${bundle}: ${(error as Error).message}`);
        }
    });
    const floor = deriveAngularFloor(declarations);
    const violations = validateAngularPeerFloor(JSON.parse(readFileSync(pkgPath, 'utf8')), floor);

    console.log(
        `Read ${floor.declarationCount} partial declarations from ${bundles.length} bundle(s): compiled with Angular ${floor.compilerVersion}, highest minVersion ${floor.highestMinVersion} → floor ${floor.floor}`
    );

    if (violations.length > 0) {
        console.error('\nAngular peer range violations found:\n');
        for (const violation of violations) {
            console.error(`  - ${pkgPath}: ${violation}`);
        }
        process.exit(1);
    }

    console.log(`OK — ${pkgPath} does not admit any Angular version below ${floor.floor}.`);
}

main();
