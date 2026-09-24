import { randomUUID } from 'node:crypto';
import { constants } from 'node:fs';
import { access, mkdir, open, rename, rm } from 'node:fs/promises';
import { basename, dirname, extname, isAbsolute, posix, relative, resolve, sep } from 'node:path';

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

import { type DotCMSRuntime, ValidationError } from '../../runtime';
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
    const download = async (rel: string, source: () => AssetSource, identifier?: string) => {
        try {
            const result = await saveAsset(
                options.dotcms,
                { dest, rel, overwrite: options.overwrite, root: options.root },
                source(),
                identifier
            );
            if (result.kind === 'written') files.push(result.file);
            else skipped.push(result.skip);
        } catch (error) {
            failures.push({ path: rel, error: errorMessage(error) });
        }
    };

    if (directAssetPath) {
        await download(basename(input.path), () => ({
            path: '/api/v2/assets',
            query: { path: assetQueryPath(input) }
        }));
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
                    return { path: `/api/v2/assets/${encodeURIComponent(identifier)}` };
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

/** The read that returns an asset's bytes: by identifier, or by path query. */
interface AssetSource {
    path: string;
    query?: Record<string, string>;
}

/**
 * Save one asset to disk, streaming its bytes straight from the response into the file.
 *
 * Everything that can refuse the write is decided BEFORE the request: the path, the root, and
 * the overwrite mode — so `skip` never transfers bytes it would throw away.
 */
async function saveAsset(
    dotcms: DotCMSRuntime,
    target: { rel: string; dest: string; overwrite: OverwriteMode; root?: string },
    source: AssetSource,
    identifier?: string
): Promise<WriteResult> {
    const outputPath = safeJoin(target.dest, target.rel);
    // `safeJoin` keeps the path inside `dest` as a string; this keeps the WRITE inside the root
    // — a symlinked directory or file already inside `dest` would otherwise redirect it.
    await assertWritableInsideRoot(target.root, outputPath);
    if (await exists(outputPath)) {
        if (target.overwrite === 'skip') {
            return { kind: 'skipped', skip: { path: target.rel, reason: 'exists' } };
        }
        if (target.overwrite === 'error') {
            throw new Error('Destination file already exists');
        }
    }

    const bytes = (await dotcms.request({
        ...source,
        onBody: (body) => writeBodyToFile(outputPath, body)
    })) as number;

    return { kind: 'written', file: { path: target.rel, bytes, identifier } };
}

/**
 * Write a response body to `outputPath` and return how many bytes it held.
 *
 * The bytes go to a temporary file beside the target, renamed over it only once the body is
 * complete. Written in place, a transfer that broke halfway would leave the target truncated —
 * and, with `overwrite`, would have already destroyed the good copy it was replacing. The
 * temporary file is opened exclusively (`wx`), so it is never an existing file or a planted
 * symlink; the rename replaces the target's directory entry rather than writing through it.
 */
async function writeBodyToFile(
    outputPath: string,
    body: ReadableStream<Uint8Array>
): Promise<number> {
    await mkdir(dirname(outputPath), { recursive: true });
    const partial = `${outputPath}.${randomUUID()}.part`;
    const reader = body.getReader();
    let bytes = 0;

    try {
        const file = await open(partial, 'wx');
        try {
            for (;;) {
                const { done, value } = await reader.read();
                if (done) break;
                // Awaiting each write is the backpressure: the next chunk is not read until
                // this one is on disk, so memory holds one chunk, not the file.
                await file.write(value);
                bytes += value.byteLength;
            }
        } finally {
            await file.close();
        }

        if (bytes === 0) {
            throw new Error('Downloaded asset was empty');
        }
        await rename(partial, outputPath);

        return bytes;
    } catch (error) {
        await reader.cancel().catch(() => undefined);
        await rm(partial, { force: true });
        throw error;
    }
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
        throw new ValidationError(`Destination must be an absolute path: ${dest}`);
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
