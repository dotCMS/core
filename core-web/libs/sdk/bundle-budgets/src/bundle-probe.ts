import { build } from 'esbuild';

import { cpSync, existsSync, mkdirSync, rmSync, writeFileSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { gzipSync } from 'node:zlib';

const HERE = dirname(fileURLToPath(import.meta.url));

/** `core-web/` — four levels up from `libs/sdk/bundle-budgets/src`. */
export const WORKSPACE_ROOT = resolve(HERE, '../../../..');

export const SDK_DIST = join(WORKSPACE_ROOT, 'dist/libs/sdk');

/**
 * Scratch tree the probes are bundled from. It lives under `dist/` so it is disposable, and
 * deep enough inside the workspace that Node resolution still walks up to
 * `core-web/node_modules` for peer dependencies such as React.
 */
const PROBE_ROOT = join(WORKSPACE_ROOT, 'dist/.bundle-budgets');

/**
 * Peer dependencies a consumer already has. They are not the SDK's weight, so they are left
 * out of every measurement.
 */
const EXTERNALS = ['react', 'react-dom', 'react/jsx-runtime', 'next', 'next/*'];

export interface ProbeResult {
    /**
     * Modules that actually contribute bytes to the bundle.
     *
     * Deliberately not `metafile.inputs`, which lists every file esbuild *parsed* — including
     * everything a barrel export made it look at and then tree-shook away. Asserting against
     * that would report TinyMCE as present in a layout-only bundle that does not ship a byte
     * of it.
     */
    modules: string[];
    rawBytes: number;
    gzipBytes: number;
}

/**
 * Copy the built SDK packages into a node_modules tree so esbuild resolves them exactly the
 * way a consumer's bundler would — through each package's real `exports` map, including the
 * subpaths. Copying rather than symlinking keeps resolution inside this tree instead of
 * escaping to the packages' real location in `dist/libs/sdk`.
 */
export function stageSdkPackages(packages: string[]): void {
    rmSync(PROBE_ROOT, { recursive: true, force: true });

    for (const pkg of packages) {
        const source = join(SDK_DIST, pkg);

        if (!existsSync(source)) {
            throw new Error(
                `[bundle-budgets] ${source} does not exist. Build the SDKs first:\n` +
                    `  pnpm nx run-many -t build --projects='sdk-*'`
            );
        }

        cpSync(source, join(PROBE_ROOT, 'node_modules/@dotcms', pkg), { recursive: true });
    }
}

/**
 * Bundle a snippet that imports from the built packages and report what came with it.
 *
 * @param name identifier used for the probe's temporary file
 * @param source the probe's source, importing only what is being measured
 * @returns the module list and the raw/gzip size of the bundle
 */
export async function probe(name: string, source: string): Promise<ProbeResult> {
    const entry = join(PROBE_ROOT, `${name}.mjs`);
    mkdirSync(dirname(entry), { recursive: true });
    writeFileSync(entry, source);

    const result = await build({
        entryPoints: [entry],
        bundle: true,
        write: false,
        metafile: true,
        format: 'esm',
        platform: 'browser',
        target: 'es2020',
        minify: true,
        external: EXTERNALS,
        conditions: ['import', 'module', 'browser', 'default'],
        logLevel: 'silent'
    });

    const output = result.outputFiles[0].contents;
    const [outputMeta] = Object.values(result.metafile.outputs);

    const modules = Object.entries(outputMeta?.inputs ?? {})
        .filter(([, contribution]) => contribution.bytesInOutput > 0)
        .map(([id]) => id.replace(/\\/g, '/'));

    return {
        modules,
        rawBytes: output.byteLength,
        gzipBytes: gzipSync(output).byteLength
    };
}

/**
 * Modules a probe pulled in that match any of the given fragments.
 *
 * @param result a probe result
 * @param fragments case-insensitive substrings of a module path
 * @returns the offending module paths, empty when the probe is clean
 */
export function modulesMatching(result: ProbeResult, fragments: string[]): string[] {
    return result.modules.filter((id) =>
        fragments.some((fragment) => id.toLowerCase().includes(fragment.toLowerCase()))
    );
}
