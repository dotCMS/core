import fs from 'fs-extra';

import path from 'path';

/**
 * Environment variable that overrides the packaged compose file with a remote one.
 *
 * Documented escape hatch (D4a): it is intentionally a truthiness check, so exporting the
 * variable as an empty string does NOT disable the bundled default.
 */
export const COMPOSE_URL_ENV_VAR = 'DOTCMS_COMPOSE_URL';

/** Relative location of the packaged asset inside the published npm package. */
const ASSET_RELATIVE_PATH = path.join('assets', 'docker-compose.yml');

/** Timeout applied to the remote read, in milliseconds. */
const REMOTE_READ_TIMEOUT_MS = 15000;

interface ComposeSourceBase {
    /** Human-readable origin, shown in diagnostics (D4a). */
    readonly describe: string;
    /** Resolves with the compose file CONTENTS — this never writes to disk. */
    read(): Promise<string>;
}

/** The compose file shipped inside the npm package. Carries no URL: it cannot hit the network. */
export interface BundledComposeSource extends ComposeSourceBase {
    readonly kind: 'bundled';
    /** Absolute path to the packaged asset. */
    readonly path: string;
}

/** A compose file fetched from `DOTCMS_COMPOSE_URL`. */
export interface RemoteComposeSource extends ComposeSourceBase {
    readonly kind: 'remote';
    readonly url: string;
}

export type ComposeSource = BundledComposeSource | RemoteComposeSource;

/**
 * Best starting directory for locating the packaged asset, under BOTH module systems.
 *
 * `src/` ships as ESM (`"type": "module"`) but Jest compiles the specs to CommonJS
 * (`tsconfig.spec.json` sets `"module": "commonjs"`), where a bare `import.meta.url` is a
 * compile-time syntax error. Rather than reach for `eval` to smuggle `import.meta` past the
 * CommonJS emit — which works, but trips esbuild's `direct-eval` warning and defeats bundler
 * analysis — this only needs to be *approximately* right: `resolveBundledAssetPath()` below
 * walks up from here until it finds the asset.
 *
 *   - CommonJS (Jest):   `__dirname` is defined.
 *   - ESM (the shipped bundle): `__dirname` is not, but this is a `bin` entry point, so
 *     `process.argv[1]` is the bundle itself and its directory is the package root.
 */
function currentModuleDir(): string {
    if (typeof __dirname === 'string') {
        return __dirname;
    }

    const entryPoint = process.argv[1];

    if (typeof entryPoint === 'string' && entryPoint.length > 0) {
        return path.dirname(entryPoint);
    }

    return process.cwd();
}

/**
 * Walks up from this module looking for the packaged `assets/docker-compose.yml`.
 *
 * The layout differs between the checked-out source (`src/compose/…` -> `<pkg>/assets`) and the
 * published bundle (`<pkg>/index.js` -> `<pkg>/assets`), so the asset is located by search
 * rather than by a hardcoded number of `..` segments.
 */
function resolveBundledAssetPath(): string {
    let dir = currentModuleDir();
    let fallback = path.resolve(dir, ASSET_RELATIVE_PATH);

    for (;;) {
        const candidate = path.join(dir, ASSET_RELATIVE_PATH);

        if (fs.existsSync(candidate)) {
            return candidate;
        }

        // Remember the package root as the best guess if the asset is missing entirely.
        if (fs.existsSync(path.join(dir, 'package.json'))) {
            fallback = candidate;
        }

        const parent = path.dirname(dir);

        if (parent === dir) {
            return fallback;
        }

        dir = parent;
    }
}

function createBundledSource(): BundledComposeSource {
    const assetPath = resolveBundledAssetPath();

    return {
        kind: 'bundled',
        path: assetPath,
        describe: `bundled compose file (${assetPath})`,
        read: () => fs.readFile(assetPath, 'utf8')
    };
}

function createRemoteSource(url: string): RemoteComposeSource {
    return {
        kind: 'remote',
        url,
        describe: `remote compose file from ${COMPOSE_URL_ENV_VAR}`,
        read: async () => {
            const controller = new AbortController();
            const timer = setTimeout(() => controller.abort(), REMOTE_READ_TIMEOUT_MS);

            try {
                const response = await fetch(url, {
                    signal: controller.signal,
                    redirect: 'follow'
                });

                if (!response.ok) {
                    throw new Error(
                        `Failed to download compose file from ${url}: ${response.status} ${response.statusText}`
                    );
                }

                return await response.text();
            } catch (error) {
                if (error instanceof Error && error.name === 'AbortError') {
                    throw new Error(
                        `Timed out after ${REMOTE_READ_TIMEOUT_MS}ms downloading compose file from ${url}`
                    );
                }

                throw error;
            } finally {
                clearTimeout(timer);
            }
        }
    };
}

/**
 * Resolves where the docker compose file comes from.
 *
 * Returns the asset bundled in the package unless `DOTCMS_COMPOSE_URL` is set to a non-empty
 * value, in which case the URL is used verbatim. The environment is read on every call, so the
 * escape hatch takes effect without re-importing this module.
 */
export function resolveComposeSource(): ComposeSource {
    const override = process.env[COMPOSE_URL_ENV_VAR];

    if (override) {
        return createRemoteSource(override);
    }

    return createBundledSource();
}
