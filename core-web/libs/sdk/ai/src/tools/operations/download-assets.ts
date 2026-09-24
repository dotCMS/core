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
import {
    RESOLVE_ENDPOINTS,
    resolveDefaultSite,
    resolveSite,
    type ResolvedSite
} from './shared/resolve';

import { HttpError, type DotCMSRuntime, ValidationError } from '../../runtime';
import { CONTEXT_ENDPOINTS, type Endpoint } from '../toolkit/endpoints';
import { errorMessage, withMessage } from '../toolkit/tool-runtime';

/**
 * Every endpoint `downloadAssets` calls — all reads. The `download_assets` tool enforces
 * exactly this list, and `download-assets.spec.ts` checks every request against it.
 */
export const DOWNLOAD_ASSETS_ENDPOINTS: readonly Endpoint[] = [
    // A folder search is scoped to one site, so the site is resolved first.
    ...CONTEXT_ENDPOINTS,
    ...RESOLVE_ENDPOINTS,
    'POST /api/content/_search',
    'GET /api/v2/assets',
    'GET /api/v2/assets/{identifier}'
];

/** What `downloadAssets` does when a destination file already exists. */
export type OverwriteMode = 'skip' | 'overwrite' | 'error';

/**
 * What `path` names. `'asset'` and `'folder'` say so outright. `'auto'` guesses from the last
 * segment (an extension means an asset) and, when that reading finds nothing, tries the other:
 * so a folder named `v1.2` and an asset named `robots` both work without the caller knowing.
 */
export type DownloadKind = 'auto' | 'asset' | 'folder';

export interface DownloadAssetsOptions {
    dotcms: DotCMSRuntime;
    /**
     * dotCMS folder or asset path, optionally host-qualified (`//host/path`). A folder is
     * searched on that one site; a path with no host means the default site.
     */
    path: string;
    /** Whether `path` is one asset or a folder. Default `'auto'`; see {@link DownloadKind}. */
    kind?: DownloadKind;
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
    const kind = options.kind ?? 'auto';
    const reading = kind === 'auto' ? (looksLikeAssetPath(input.path) ? 'asset' : 'folder') : kind;

    const record = (result: WriteResult) => {
        if (result.kind === 'written') files.push(result.file);
        else skipped.push(result.skip);
    };
    const target = (rel: string) => ({
        dest,
        rel,
        overwrite: options.overwrite,
        root: options.root
    });

    // Each download is wrapped in the same try/catch so one failure records a failure and
    // doesn't abort the batch — both the single-asset path and the folder loop go through it.
    const download = async (rel: string, source: () => AssetSource, identifier?: string) => {
        try {
            record(await saveAsset(options.dotcms, target(rel), source(), identifier));
        } catch (error) {
            failures.push({ path: rel, error: errorMessage(error) });
        }
    };

    // `path` read as ONE asset. Resolves false only when there is no asset there (404), which
    // is what lets `auto` try the folder reading instead; any other failure is recorded.
    const oneAsset = async (): Promise<boolean> => {
        const rel = basename(input.path);
        try {
            record(
                await saveAsset(options.dotcms, target(rel), {
                    path: '/api/v2/assets',
                    query: { path: assetQueryPath(input) }
                })
            );
        } catch (error) {
            if (error instanceof HttpError && error.status === 404) {
                return false;
            }
            failures.push({ path: rel, error: errorMessage(error) });
        }

        return true;
    };

    // `path` read as a FOLDER, searched on its one site. Resolves false when it holds nothing.
    const folder = async (): Promise<boolean> => {
        const site = await siteOf(options.dotcms, options.path, input);
        const { assets, truncated } = await enumerateAssets(
            options.dotcms,
            site.identifier,
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

        // After a cap, the folder is not empty: it simply was not read to the end.
        return assets.length > 0 || truncated;
    };

    let found: boolean;
    if (reading === 'asset') {
        found = (await oneAsset()) || (kind === 'auto' && (await folder()));
    } else {
        found = (await folder()) || (kind === 'auto' && (await oneAsset()));
    }
    if (!found) {
        warnings.push(zeroMatchWarning(options.path, input));
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

        // An empty body is an empty asset (a 0-byte VTL partial is real), so it is written too.
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
    siteId: string,
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
                // `+conhost` keeps the walk on one site. Without it, `/shared` matched every
                // site's `/shared`, and same-named files overwrote or skipped each other locally.
                query: `+baseType:4 +conhost:${siteId} +path:${folder}/*`,
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
    return (
        `No assets matched "${parsed.path}" — check the path. The result is empty, not a ` +
        `success.${siteReadingNote(rawInput, parsed)}`
    );
}

/** The hostname a `//host/path` input names. */
function siteHost(parsed: { siteQualified?: string; path: string }): string {
    return (parsed.siteQualified ?? '').slice(
        2,
        (parsed.siteQualified ?? '').length - parsed.path.length
    );
}

/**
 * How a `//`-prefixed input was read, for a message about it — empty for any other input.
 * `//application/themes` reads `application` as the SITE, the common mistake: saying so, and
 * what to write instead, is what lets the caller correct it rather than retry it.
 */
function siteReadingNote(
    rawInput: string,
    parsed: { siteQualified?: string; path: string }
): string {
    const trimmed = rawInput.trim();
    if (!trimmed.startsWith('//')) {
        return '';
    }

    // The plain-path form is the input with one leading slash removed — i.e. the FULL path
    // including the segment that "//" consumed as the site (e.g. "//application/themes" → "/application/themes").
    const asPlainPath = trimmed.slice(1).replace(/\/+$/, '');

    return (
        ` Note: "${rawInput}" was read as site="${siteHost(parsed)}", path="${parsed.path}" ` +
        `(a leading "//" treats the first segment as the dotCMS site). ` +
        `If you meant a path on the default site, use "${asPlainPath}"; ` +
        `if you meant a host-qualified path, keep "//<site>/<path>".`
    );
}

/**
 * The one site a folder is searched on: the `//host` the input names, or the default site.
 * An unknown host is the caller's input being wrong (VALIDATION), and says how it was read.
 */
async function siteOf(
    dotcms: DotCMSRuntime,
    rawInput: string,
    parsed: { siteQualified?: string; path: string }
): Promise<ResolvedSite> {
    if (!parsed.siteQualified) {
        return resolveDefaultSite(dotcms);
    }

    try {
        return await resolveSite(dotcms, siteHost(parsed));
    } catch (error) {
        if (error instanceof ValidationError) {
            throw withMessage(error, `${error.message}${siteReadingNote(rawInput, parsed)}`);
        }
        throw error;
    }
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
