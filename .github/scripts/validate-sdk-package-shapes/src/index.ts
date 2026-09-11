#!/usr/bin/env node
import { readFileSync } from 'fs';
import { basename, dirname } from 'path';

import { validateExamplePackageJson, validateSdkLibPackageJson } from './validate';

/**
 * CLI entry point. Usage:
 *
 *   ts-node src/index.ts <path-to-package.json> [--branch <branch-name>]
 *
 * A path under `.../libs/sdk/<pkgName>/package.json` is validated as an SDK lib manifest
 * (pkgName is inferred from the parent directory name). Any other path is validated as an
 * example app manifest, using --branch (default "main") to decide whether a floating "latest"
 * is acceptable.
 *
 * Exits non-zero with one line per violation if any file fails.
 */

function parseArgs(argv: string[]): { filePaths: string[]; branch: string } {
    const branchFlagIndex = argv.indexOf('--branch');
    let branch = 'main';
    let filePaths = argv;

    if (branchFlagIndex !== -1) {
        branch = argv[branchFlagIndex + 1] ?? 'main';
        filePaths = [...argv.slice(0, branchFlagIndex), ...argv.slice(branchFlagIndex + 2)];
    }

    return { filePaths, branch };
}

function isSdkLibManifest(filePath: string): boolean {
    return dirname(filePath).replace(/\\/g, '/').includes('/libs/sdk/');
}

function main(): void {
    const { filePaths, branch } = parseArgs(process.argv.slice(2));

    if (filePaths.length === 0) {
        console.error('Usage: index.ts <path-to-package.json> [...more paths] [--branch <name>]');
        process.exit(2);
    }

    const allViolations: string[] = [];

    for (const filePath of filePaths) {
        const pkg = JSON.parse(readFileSync(filePath, 'utf8'));

        const violations = isSdkLibManifest(filePath)
            ? validateSdkLibPackageJson(pkg, basename(dirname(filePath)))
            : validateExamplePackageJson(pkg, branch);

        allViolations.push(...violations.map((v) => `${filePath}: ${v}`));
    }

    if (allViolations.length > 0) {
        console.error('SDK package.json shape violations found:\n');
        for (const violation of allViolations) {
            console.error(`  - ${violation}`);
        }
        process.exit(1);
    }

    console.log(`OK — ${filePaths.length} file(s) validated, no violations.`);
}

main();
