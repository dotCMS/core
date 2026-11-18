import fs from 'fs';
import path from 'path';

/**
 * Packaging guard for the bundled Docker compose file (dotCMS issue #37262, AC-013).
 *
 * The CLI ships its own `assets/docker-compose.yml`. For that file to actually reach
 * users it must be declared in BOTH manifests:
 *
 *   1. package.json -> "files"                        (what npm publishes)
 *   2. project.json -> targets.build.options.assets   (what esbuild copies into dist)
 *
 * Miss either one and the published package has no compose file, so every
 * local-Docker run of the CLI fails at its very first step. That is the most
 * likely way to break this release, hence a test instead of a code review.
 */

const PROJECT_ROOT = path.resolve(__dirname, '..');
const PACKAGE_JSON_PATH = path.join(PROJECT_ROOT, 'package.json');
const PROJECT_JSON_PATH = path.join(PROJECT_ROOT, 'project.json');

/** Path of the compose asset, relative to the package/project root. */
const COMPOSE_ASSET_PATH = 'assets/docker-compose.yml';

/** Project root as spelled inside project.json asset entries (workspace-relative). */
const WORKSPACE_PROJECT_ROOT = 'libs/sdk/create-app';

type ProjectJsonAsset = string | { input?: string; glob?: string; output?: string };

function readJson(filePath: string): Record<string, unknown> {
    return JSON.parse(fs.readFileSync(filePath, 'utf-8'));
}

/** Normalize a manifest entry: drop `./` prefixes and trailing slashes. */
function normalize(entry: string): string {
    return entry.trim().replace(/^\.\//, '').replace(/\/+$/, '');
}

/** Turn a (possibly globbed) path into a matcher, honoring `*` and `**`. */
function globToRegExp(pattern: string): RegExp {
    const GLOBSTAR = '<<GLOBSTAR>>';
    const source = pattern
        .split('/')
        .map((segment) =>
            segment === '**'
                ? GLOBSTAR
                : segment.replace(/[.+^${}()|[\]\\?]/g, '\\$&').replace(/\*/g, '[^/]*')
        )
        .join('/')
        .split(`${GLOBSTAR}/`)
        .join('(?:.*/)?')
        .split(GLOBSTAR)
        .join('.*');

    return new RegExp(`^${source}$`);
}

/**
 * Would this entry ship `assets/docker-compose.yml`?
 *
 * Deliberately tolerant of any reasonable spelling: a bare directory (`assets`),
 * a trailing slash (`assets/`), a glob (`assets/**`, `assets/*`, `assets/*.yml`)
 * or the explicit file path all count.
 */
function shipsComposeAsset(entry: string): boolean {
    const normalized = normalize(entry);

    if (!normalized) {
        return false;
    }

    // A directory entry ships everything under it.
    if (COMPOSE_ASSET_PATH === normalized || COMPOSE_ASSET_PATH.startsWith(`${normalized}/`)) {
        return true;
    }

    return globToRegExp(normalized).test(COMPOSE_ASSET_PATH);
}

/** Flatten a project.json asset entry (string or `{ input, glob, output }`) to a path. */
function toAssetPath(asset: ProjectJsonAsset): string {
    if (typeof asset === 'string') {
        return normalize(asset);
    }

    const input = normalize(asset?.input ?? '');
    const glob = normalize(asset?.glob ?? '');

    return [input, glob].filter(Boolean).join('/');
}

/** project.json paths are workspace-relative; make them package-relative. */
function toProjectRelative(assetPath: string): string {
    const normalized = normalize(assetPath);

    return normalized.startsWith(`${WORKSPACE_PROJECT_ROOT}/`)
        ? normalized.slice(WORKSPACE_PROJECT_ROOT.length + 1)
        : normalized;
}

function fail(lines: string[]): never {
    throw new Error(`\n${lines.join('\n')}\n`);
}

describe('@dotcms/create-app packaging', () => {
    describe('package.json "files"', () => {
        it('ships assets/docker-compose.yml to npm', () => {
            const pkg = readJson(PACKAGE_JSON_PATH);
            const files = pkg['files'];

            if (!Array.isArray(files)) {
                fail([
                    `${PACKAGE_JSON_PATH} has no "files" array.`,
                    `Add one that includes "${COMPOSE_ASSET_PATH}" or npm will publish the CLI without the compose file.`
                ]);
            }

            const entries = files as string[];
            const matches = entries.filter(
                (entry) => typeof entry === 'string' && shipsComposeAsset(entry)
            );

            if (matches.length === 0) {
                fail([
                    'package.json will NOT publish the bundled Docker compose file.',
                    `  manifest : ${PACKAGE_JSON_PATH}`,
                    `  "files"  : ${JSON.stringify(entries)}`,
                    `  missing  : an entry covering "${COMPOSE_ASSET_PATH}"`,
                    '',
                    '  Fix: add "assets/**" (or "assets", or "assets/docker-compose.yml") to the "files" array.',
                    '  Without it the published package has no compose file and every local-Docker',
                    '  run of the CLI fails at its first step. (issue #37262, AC-013)'
                ]);
            }

            expect(matches.length).toBeGreaterThan(0);
        });
    });

    describe('project.json build assets', () => {
        it('copies assets/docker-compose.yml into the build output', () => {
            const project = readJson(PROJECT_JSON_PATH);
            const assets = (
                project as {
                    targets?: { build?: { options?: { assets?: ProjectJsonAsset[] } } };
                }
            ).targets?.build?.options?.assets;

            if (!Array.isArray(assets)) {
                fail([
                    `${PROJECT_JSON_PATH} has no targets.build.options.assets array.`,
                    `Add one that copies "${COMPOSE_ASSET_PATH}" into the build output.`
                ]);
            }

            const resolved = assets.map((asset) => toProjectRelative(toAssetPath(asset)));
            const matches = resolved.filter(shipsComposeAsset);

            if (matches.length === 0) {
                fail([
                    'project.json will NOT copy the bundled Docker compose file into dist.',
                    `  manifest : ${PROJECT_JSON_PATH}`,
                    `  assets   : ${JSON.stringify(assets)}`,
                    `  resolved : ${JSON.stringify(resolved)}`,
                    `  missing  : an entry covering "${COMPOSE_ASSET_PATH}"`,
                    '',
                    '  Fix: add to targets.build.options.assets either the string',
                    `       "${WORKSPACE_PROJECT_ROOT}/${COMPOSE_ASSET_PATH}" or the object`,
                    `       { "input": "${WORKSPACE_PROJECT_ROOT}/assets", "glob": "**/*", "output": "assets" }.`,
                    '  Without it the compose file never lands in dist, so the published package',
                    '  ships without it and every local-Docker run fails. (issue #37262, AC-013)'
                ]);
            }

            expect(matches.length).toBeGreaterThan(0);
        });
    });
});
