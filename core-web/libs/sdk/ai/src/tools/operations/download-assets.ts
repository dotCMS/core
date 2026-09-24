import { constants } from 'node:fs';
import { access, mkdir, writeFile } from 'node:fs/promises';
import { basename, extname, isAbsolute, posix, relative, resolve, sep } from 'node:path';

import {
    normalizeDotCMSPath,
    sortManifest,
    sumBytes,
    type AssetManifestFailure,
    type AssetManifestFile,
    type AssetManifestSkipped
} from './shared/asset-common';
import { includeMatcher } from './shared/glob';
import { assertInsideRoot, assertWritableInsideRoot } from './shared/local-root';

import { type DotCMSRuntime, isBinaryResponseEnvelope } from '../../runtime';
import { type Endpoint } from '../toolkit/endpoints';
import { errorMessage } from '../toolkit/tool-runtime';

/**
 * Every endpoint `downloadAssets` calls — all reads. The `download_assets` tool enforces
 * exactly this list, and `download-assets.spec.ts` checks every request against it.
 */
export const DOWNLOAD_ASSETS_ENDPOINTS: readonly Endpoint[] = [
    'POST /api/content/_search',
    'GET /api/v2/assets',
    'GET /api/v2/assets/{identifier}'
];

/** What `downloadAssets` does when a destination file already exists. */
export type OverwriteMode = 'skip' | 'overwrite' | 'error';

export interface DownloadAssetsOptions {
    dotcms: DotCMSRuntime;
    /** dotCMS folder or asset path, optionally host-qualified (`//host/path`). */
    path: string;
    /** Absolute local directory the files are written into. */
    dest: string;
    /** Include files in nested folders. */
    recursive: boolean;
    overwrite: OverwriteMode;
    /** Comma-separated glob filter, e.g. `*.vtl,*.scss`. */
    include?: string;
    /**
     * Directory `dest`, and every file written, must stay inside — after symlinks resolve.
     * Omit only when YOU chose `dest`; the `download_assets` tool always sets it.
     */
    root?: string;
}

/** What `downloadAssets` returns — never the file bytes, only where they landed. */
export interface DownloadAssetsManifest {
    path: string;
    dest: string;
    count: number;
    bytes: number;
    files: AssetManifestFile[];
    failures: AssetManifestFailure[];
    skipped: AssetManifestSkipped[];
    warnings: string[];
}

interface AssetContentlet {
    identifier?: string;
    path?: string;
}

const SEARCH_LIMIT = 500;

/**
 * Bounds on a single enumeration: how many search results one walk reads, whether or not the
 * filters select them. `/application` on a large site can hold tens of thousands of assets,
 * and every one selected is then downloaded one at a time with an open file handle — so an
 * unbounded walk is both an unbounded tool call and unbounded load on the instance. A
 * realistic theme is 100–500 files, so this is far above any genuine use while still being a
 * ceiling. Hitting it is reported, never silent.
 */
const MAX_ENUMERATED_ASSETS = 5_000;
const MAX_SEARCH_PAGES = Math.ceil(MAX_ENUMERATED_ASSETS / SEARCH_LIMIT);

/**
 * Download a dotCMS asset, or every asset under a folder, to a local directory. Relative
 * paths are preserved, and one failed file is recorded in the manifest rather than aborting
 * the batch.
 */
export async function downloadAssets(
    options: DownloadAssetsOptions
): Promise<DownloadAssetsManifest> {
    const input = normalizeDotCMSPath(options.path);
    const dest = await prepareWritableDir(options.dest, options.root);
    const files: AssetManifestFile[] = [];
    const failures: AssetManifestFailure[] = [];
    const skipped: AssetManifestSkipped[] = [];
    const warnings: string[] = [];
    const directAssetPath = looksLikeAssetPath(input.path);

    // Each download is wrapped in the same try/catch so one failure records a failure and
    // doesn't abort the batch — both the single-asset path and the folder loop go through it.
    const download = async (
        rel: string,
        fetchBytes: () => Promise<Buffer>,
        identifier?: string
    ) => {
        try {
            const result = await writeDownloadedFile(
                { dest, rel, overwrite: options.overwrite, root: options.root },
                await fetchBytes(),
                identifier
            );
            if (result.kind === 'written') files.push(result.file);
            else skipped.push(result.skip);
        } catch (error) {
            failures.push({ path: rel, error: errorMessage(error) });
        }
    };

    if (directAssetPath) {
        await download(basename(input.path), () =>
            downloadAssetBytes(options.dotcms, {
                path: '/api/v2/assets',
                query: { path: assetQueryPath(input) }
            })
        );
    } else {
        const { assets, truncated } = await enumerateAssets(
            options.dotcms,
            input.path,
            options.recursive,
            options.include
        );

        if (truncated) {
            // `include` and `recursive` filter what the search returned, so they cannot get a
            // walk past the cap; only a narrower folder can.
            warnings.push(
                `Enumeration stopped before reaching the end of this folder (it reads at most ` +
                    `${MAX_ENUMERATED_ASSETS} assets), so the download is INCOMPLETE. Narrow it ` +
                    `with a subfolder path and run again.`
            );
        } else if (assets.length === 0) {
            // Only when the walk reached the end: after a cap, zero matches says nothing
            // about whether the path is right.
            warnings.push(zeroMatchWarning(options.path, input));
        }

        for (const asset of assets) {
            const assetPath = asset.path ? normalizeDotCMSPath(asset.path).path : '';
            const rel = relativeAssetPath(input.path, assetPath) || assetPath || '(unknown)';
            const identifier = asset.identifier;

            await download(
                rel,
                () => {
                    if (!identifier || !relativeAssetPath(input.path, assetPath)) {
                        throw new Error('Asset is missing identifier or path');
                    }
                    return downloadAssetBytes(options.dotcms, {
                        path: `/api/v2/assets/${encodeURIComponent(identifier)}`
                    });
                },
                identifier
            );
        }
    }

    return sortManifest({
        path: input.path,
        dest,
        count: files.length,
        bytes: sumBytes(files),
        files,
        failures,
        skipped,
        warnings
    });
}

type WriteResult =
    | { kind: 'written'; file: AssetManifestFile }
    | { kind: 'skipped'; skip: AssetManifestSkipped };

async function writeDownloadedFile(
    options: { rel: string; dest: string; overwrite: OverwriteMode; root?: string },
    bytes: Buffer,
    identifier?: string
): Promise<WriteResult> {
    const outputPath = safeJoin(options.dest, options.rel);
    // `safeJoin` keeps the path inside `dest` as a string; this keeps the WRITE inside the root
    // — a symlinked directory or file already inside `dest` would otherwise redirect it.
    await assertWritableInsideRoot(options.root, outputPath);
    if (await exists(outputPath)) {
        if (options.overwrite === 'skip') {
            return { kind: 'skipped', skip: { path: options.rel, reason: 'exists' } };
        }
        if (options.overwrite === 'error') {
            throw new Error('Destination file already exists');
        }
    }

    await mkdir(resolve(outputPath, '..'), { recursive: true });
    await writeFile(outputPath, bytes);
    return { kind: 'written', file: { path: options.rel, bytes: bytes.byteLength, identifier } };
}

/**
 * The selected assets, plus whether the walk stopped before the search reached its end — the
 * cap (see MAX_ENUMERATED_ASSETS) or a backend that stopped advancing.
 */
interface EnumerateResult {
    assets: AssetContentlet[];
    truncated: boolean;
}

async function enumerateAssets(
    dotcms: DotCMSRuntime,
    folder: string,
    recursive: boolean,
    include?: string
): Promise<EnumerateResult> {
    const matches = includeMatcher(include);
    const assets: AssetContentlet[] = [];
    // Every identifier the search returned, selected or not. Progress is measured on this, not
    // on `assets`: a full page the filters reject entirely still moved the walk forward.
    const enumerated = new Set<string>();

    for (let page_ = 0; page_ < MAX_SEARCH_PAGES; page_++) {
        const offset = page_ * SEARCH_LIMIT;
        const response = await dotcms.request({
            method: 'POST',
            path: '/api/content/_search',
            body: {
                query: `+baseType:4 +path:${folder}/*`,
                sort: 'path asc',
                limit: SEARCH_LIMIT,
                offset
            }
        });
        const page = extractContentlets(response);
        const enumeratedBefore = enumerated.size;

        for (const asset of page) {
            if (!asset.identifier || !asset.path || enumerated.has(asset.identifier)) {
                continue;
            }
            enumerated.add(asset.identifier);

            const rel = relativeAssetPath(folder, normalizeDotCMSPath(asset.path).path);
            if (!rel || (!recursive && rel.includes('/')) || !matches(rel)) {
                continue;
            }

            assets.push(asset);
        }

        // A short page is the end of the results: the only way the walk is complete.
        if (page.length < SEARCH_LIMIT) {
            return { assets, truncated: false };
        }

        // Termination guard, NOT an optimisation. If the backend ignores or clamps `offset`,
        // every page comes back full of the same identifiers: the short-page exit never
        // fires, and the loop spins forever issuing identical POSTs — a tool call that never
        // returns while the instance takes sustained load. A page that returns nothing new
        // means we are not advancing, whatever the backend thinks it is doing — and whatever
        // lies past it was never read.
        if (enumerated.size === enumeratedBefore) {
            return { assets, truncated: true };
        }
    }

    // Out of pages on a full one: there are results left that this walk did not read.
    return { assets, truncated: true };
}

/** Fetch an asset's raw bytes — by identifier (`/api/v2/assets/{id}`) or by path query. */
async function downloadAssetBytes(
    dotcms: DotCMSRuntime,
    request: { path: string; query?: Record<string, string> }
): Promise<Buffer> {
    const response = await dotcms.request({ ...request, responseType: 'base64' });

    if (!isBinaryResponseEnvelope(response)) {
        throw new Error('Expected a binary asset response');
    }

    const bytes = Buffer.from(response.base64, 'base64');
    if (bytes.byteLength === 0) {
        throw new Error('Downloaded asset was empty');
    }

    return bytes;
}

/** The path to send to the `/api/v2/assets?path=` query — host-qualified when available. */
function assetQueryPath(normalized: { siteQualified?: string; path: string }): string {
    return normalized.siteQualified || normalized.path;
}

/**
 * Message for a folder enumeration that matched 0 assets. The common cause is the `//host/path`
 * ambiguity: a `//`-prefixed input has its FIRST segment consumed as the site, so `//application/themes`
 * searches the path `/themes` on site `application` — which usually doesn't exist. Surface exactly
 * that so the agent can correct it instead of treating an empty result as success.
 */
function zeroMatchWarning(
    rawInput: string,
    parsed: { siteQualified?: string; path: string }
): string {
    const base = `No assets matched "${parsed.path}" — check the path. The result is empty, not a success.`;
    const trimmed = rawInput.trim();
    if (trimmed.startsWith('//')) {
        const site = parsed.siteQualified?.slice(
            2,
            parsed.siteQualified.length - parsed.path.length
        );
        // The plain-path form is the input with one leading slash removed — i.e. the FULL path
        // including the segment that "//" consumed as the site (e.g. "//application/themes" → "/application/themes").
        const asPlainPath = trimmed.slice(1).replace(/\/+$/, '');
        return (
            `${base} Note: "${rawInput}" was read as site="${site}", path="${parsed.path}" ` +
            `(a leading "//" treats the first segment as the dotCMS site). ` +
            `If you meant a path on the default site, use "${asPlainPath}"; ` +
            `if you meant a host-qualified path, keep "//<site>/<path>".`
        );
    }
    return base;
}

function relativeAssetPath(folder: string, assetPath: string): string {
    const prefix = `${folder.replace(/\/+$/, '')}/`;
    return assetPath.startsWith(prefix) ? assetPath.slice(prefix.length) : '';
}

function looksLikeAssetPath(path: string): boolean {
    return extname(path) !== '';
}

async function prepareWritableDir(dest: string, root: string | undefined): Promise<string> {
    if (!isAbsolute(dest)) {
        throw new Error(`Destination must be an absolute path: ${dest}`);
    }

    const resolved = resolve(dest);
    // Checked BEFORE mkdir, so a refused `dest` creates nothing — not even its parents.
    await assertInsideRoot(root, 'dest', resolved);
    await mkdir(resolved, { recursive: true });
    await access(resolved, constants.W_OK);

    return resolved;
}

function safeJoin(root: string, rel: string): string {
    if (posix.isAbsolute(rel) || rel.split('/').includes('..')) {
        throw new Error(`Unsafe relative path: ${rel}`);
    }

    const output = resolve(root, rel);
    const back = relative(root, output);

    if (back === '..' || back.startsWith(`..${sep}`) || isAbsolute(back)) {
        throw new Error(`Resolved path escapes destination: ${rel}`);
    }

    return output;
}

function extractContentlets(response: unknown): AssetContentlet[] {
    const root = response as {
        entity?: {
            jsonObjectView?: { contentlets?: unknown };
            contentlets?: unknown;
            results?: unknown;
        };
        contentlets?: unknown;
    };
    const candidates = [
        root.entity?.jsonObjectView?.contentlets,
        root.entity?.contentlets,
        root.entity?.results,
        root.contentlets
    ];

    for (const candidate of candidates) {
        if (Array.isArray(candidate)) {
            return candidate as AssetContentlet[];
        }
    }

    return [];
}

async function exists(path: string): Promise<boolean> {
    try {
        await access(path, constants.F_OK);
        return true;
    } catch {
        return false;
    }
}
