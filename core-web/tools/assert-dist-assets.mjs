#!/usr/bin/env node
/**
 * Assert that the shipped third-party editor bundles actually landed in dist.
 *
 * `dist/apps/dotcms-ui/tinymce` and `dist/apps/dotcms-ui/assets/monaco-editor` are recursive
 * `glob: **\/*` copies whose inputs (node_modules/tinymce, node_modules/monaco-editor) become
 * symlinks pointing outside the workspace root once the virtual store moves out of the project.
 *
 * The failure mode this guards against is not a build that errors — it is a copy that matches
 * nothing and still exits 0. The build goes green and ships an editor with no assets. So this
 * asserts by FILE COUNT and never by the build's exit status.
 *
 * See specs/37573-pnpm-global-virtual-store/ (task T008, FR-008).
 *
 *   node tools/assert-dist-assets.mjs [--dist <path>] [--update-baseline]
 *
 *   --update-baseline   rewrite the baselines below from the current dist (task T004 only — run it
 *                       against a known-good build made WITHOUT the global store)
 *
 * Exit codes: 0 all bundles at or above baseline, 1 a bundle is short or missing, 2 could not run.
 */

import { readdirSync, statSync, existsSync, readFileSync, writeFileSync } from 'node:fs';
import { join, dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const HERE = dirname(fileURLToPath(import.meta.url));
const CORE_WEB = resolve(HERE, '..');
const DEFAULT_DIST = resolve(CORE_WEB, 'dist', 'apps', 'dotcms-ui');

/**
 * Baselines from a known-good production build, re-taken on this branch (task T004, 2026-09-22):
 * a cache-skipped `dotcms-ui` production build with the default LOCAL virtual store.
 *
 * File counts reproduce the issue's numbers exactly (217 and 1068). The byte totals do NOT, and
 * that is a units difference rather than a discrepancy: #37573 quotes "10 MB" and "84 MB" from
 * `du`, which reports allocated disk blocks, while this counts logical file sizes — 8,180,708 and
 * 77,295,025 bytes for the same trees. Comparing a `du` figure against a sum of `stat` sizes fails
 * a perfectly good build, which is exactly what happened the first time this ran.
 */
const BASELINES = [
    { name: 'tinymce', path: 'tinymce', minFiles: 217, minBytes: 8_180_708 },
    { name: 'monaco-editor', path: 'assets/monaco-editor', minFiles: 1068, minBytes: 77_295_025 }
];

const argv = process.argv.slice(2);
const distRoot = argv.includes('--dist') ? resolve(argv[argv.indexOf('--dist') + 1]) : DEFAULT_DIST;
const updateBaseline = argv.includes('--update-baseline');

/** Count files and bytes under a directory, following symlinks (the point is what shipped). */
function measure(dir) {
    let files = 0;
    let bytes = 0;

    const walk = (current) => {
        for (const entry of readdirSync(current, { withFileTypes: true })) {
            const full = join(current, entry.name);
            let info;
            try {
                info = statSync(full); // statSync, not lstatSync: resolve links deliberately
            } catch {
                continue; // a dangling link is a missing file, which the count will reflect
            }

            if (info.isDirectory()) walk(full);
            else if (info.isFile()) {
                files += 1;
                bytes += info.size;
            }
        }
    };

    walk(dir);

    return { files, bytes };
}

const mb = (bytes) => `${(bytes / 1024 / 1024).toFixed(1)} MB`;

function main() {
    if (!existsSync(distRoot)) {
        console.error(
            `No build output at ${distRoot}\n` +
                `Build first, and build with the cache skipped:\n` +
                `  pnpm nx build dotcms-ui --configuration=production --skip-nx-cache\n` +
                `A cached run replays recorded output and reports success without executing anything.`
        );
        process.exit(2);
    }

    const results = [];
    let failed = false;

    for (const baseline of BASELINES) {
        const dir = join(distRoot, baseline.path);

        if (!existsSync(dir)) {
            console.error(`FAIL  ${baseline.name}: ${dir} does not exist`);
            failed = true;
            results.push({ ...baseline, files: 0, bytes: 0 });
            continue;
        }

        const { files, bytes } = measure(dir);
        results.push({ ...baseline, files, bytes });

        if (updateBaseline) {
            console.log(`baseline ${baseline.name}: ${files} files, ${mb(bytes)}`);
            continue;
        }

        if (files < baseline.minFiles || bytes < baseline.minBytes) {
            console.error(
                `FAIL  ${baseline.name}: ${files} files / ${mb(bytes)} ` +
                    `(expected at least ${baseline.minFiles} files / ${mb(baseline.minBytes)})`
            );
            if (files === 0) {
                console.error(
                    '      Zero files is the signature failure of this feature: a recursive copy\n' +
                        '      out of a symlinked input matched nothing, and the build still exited 0.'
                );
            }
            failed = true;
        } else {
            console.log(`ok    ${baseline.name}: ${files} files / ${mb(bytes)}`);
        }
    }

    if (updateBaseline) {
        const source = readFileSync(__filenameShim(), 'utf8');
        const updated = results.reduce(
            (acc, r) =>
                acc.replace(
                    new RegExp(`(name: '${r.name}'[^}]*minFiles: )\\d+(, minBytes: )[^ }]+`),
                    `$1${r.files}$2${r.bytes}`
                ),
            source
        );
        writeFileSync(__filenameShim(), updated);
        console.log('\nBaselines written. Commit this file with the numbers you just measured.');
        process.exit(0);
    }

    process.exit(failed ? 1 : 0);
}

function __filenameShim() {
    return fileURLToPath(import.meta.url);
}

main();
