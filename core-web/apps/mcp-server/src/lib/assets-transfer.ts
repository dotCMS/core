import { constants } from 'node:fs';
import { access, mkdir, readdir, readFile, stat, writeFile } from 'node:fs/promises';
import { basename, extname, isAbsolute, join, posix, relative, resolve, sep } from 'node:path';

import { type DotCMSRuntime, isBinaryResponseEnvelope } from '@dotcms/ai/runtime';

import { errorMessage } from './runtime';
type OverwriteMode = 'skip' | 'overwrite' | 'error';

export interface AssetManifestFile {
    path: string;
    bytes: number;
    identifier?: string;
}

export interface AssetManifestFailure {
    path: string;
    error: string;
}

export interface AssetManifestSkipped {
    path: string;
    reason: string;
}

interface AssetContentlet {
    identifier?: string;
    path?: string;
}

interface LocalFile {
    abs: string;
    rel: string;
    bytes: number;
}

const SEARCH_LIMIT = 500;

/**
 * Bounds on a single enumeration. `/application` on a large site can hold tens of
 * thousands of assets, and every one enumerated is then downloaded one at a time with an
 * open file handle — so an unbounded walk is both an unbounded MCP call and unbounded load
 * on the instance. A realistic theme is 100–500 files, so these are far above any genuine
 * use while still being a ceiling. Hitting either is reported, never silent.
 */
const MAX_ENUMERATED_ASSETS = 5_000;
const MAX_SEARCH_PAGES = Math.ceil(MAX_ENUMERATED_ASSETS / SEARCH_LIMIT);
const MIME_BY_EXT: Record<string, string> = {
    '.css': 'text/css',
    '.eot': 'application/vnd.ms-fontobject',
    '.gif': 'image/gif',
    '.html': 'text/html',
    '.ico': 'image/x-icon',
    '.jpeg': 'image/jpeg',
    '.jpg': 'image/jpeg',
    '.js': 'application/javascript',
    '.json': 'application/json',
    '.png': 'image/png',
    '.scss': 'text/x-scss',
    '.svg': 'image/svg+xml',
    '.ttf': 'font/ttf',
    '.vtl': 'text/x-velocity',
    '.woff': 'font/woff',
    '.woff2': 'font/woff2'
};

export async function downloadAssets(options: {
    dotcms: DotCMSRuntime;
    path: string;
    dest: string;
    recursive: boolean;
    overwrite: OverwriteMode;
    include?: string;
}) {
    const input = normalizeDotCMSPath(options.path);
    const dest = await prepareWritableDir(options.dest);
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
                { dest, rel, overwrite: options.overwrite },
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
            warnings.push(
                `Enumeration stopped at the ${MAX_ENUMERATED_ASSETS}-asset cap — this folder ` +
                    `holds more than that, so the download is INCOMPLETE. Narrow it with a ` +
                    `subfolder path or an \`include\` pattern and run again.`
            );
        }

        if (assets.length === 0) {
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
    options: { rel: string; dest: string; overwrite: OverwriteMode },
    bytes: Buffer,
    identifier?: string
): Promise<WriteResult> {
    const outputPath = safeJoin(options.dest, options.rel);
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

export async function uploadAssets(options: {
    dotcms: DotCMSRuntime;
    src: string;
    dest: string;
    include?: string;
    publish: boolean;
    verify: boolean;
}) {
    const src = await prepareReadableDir(options.src);
    const dest = normalizeDotCMSPath(options.dest);

    if (!dest.siteQualified) {
        throw new Error(
            'Upload destination must be host-qualified, e.g. //demo.dotcms.com/application/themes/travel'
        );
    }

    const { files: localFiles, totalSeen } = await collectLocalFiles(src, options.include);
    const files: AssetManifestFile[] = [];
    const failures: AssetManifestFailure[] = [];
    const skipped: AssetManifestSkipped[] = [];
    const warnings: string[] = [];

    if (localFiles.length === 0) {
        if (options.include && totalSeen > 0) {
            // The source dir is NOT empty — the include pattern is the problem. Say so distinctly so
            // this never reads as "nothing to upload" in an unattended run. The matcher supports
            // *, ? , ** globstar, and {a,b,c} brace expansion, all relative to `src`.
            warnings.push(
                `Include pattern "${options.include}" matched 0 of ${totalSeen} file(s) under ` +
                    `"${src}" — check the glob syntax. Patterns are relative to the source dir and ` +
                    `support *, ?, ** (globstar), and {png,webp,jpg} brace expansion (e.g. ` +
                    `"*.{png,webp,jpg}" or "**/*.png"). Nothing was uploaded.`
            );
        } else {
            warnings.push(`No files found under "${src}".`);
        }
    }

    for (const file of localFiles) {
        try {
            // Every file in src lands in dotCMS as-is, 0-byte content included. We do not
            // skip on empty content: an empty file that exists locally must exist remotely,
            // otherwise the container can't assemble CONTENT bodies (the empty-skip was the
            // root cause of a missing postloop.vtl). `skipped[]` is reserved for real skips
            // (e.g. a glob matching nothing), never for empty content.
            const uploaded = await uploadOneAsset(
                options.dotcms,
                file,
                `${dest.siteQualified}/${file.rel}`,
                options.publish
            );
            files.push(uploaded.file);
            if (uploaded.warning) {
                warnings.push(uploaded.warning);
            }
        } catch (error) {
            failures.push({ path: file.rel, error: errorMessage(error) });
        }
    }

    // Belt AND braces: `verifyLive` guards every await internally, but this call is the last
    // thing standing between a completed set of writes and the manifest that reports them.
    // If verification ever fails in a way it did not anticipate, the uploads still happened
    // and the model still needs to be told exactly what landed — so the worst case here is a
    // manifest with a warning, never a thrown error that erases the whole report.
    let notLive: AssetManifestFile[] = [];
    if (options.publish && options.verify) {
        try {
            const verified = await verifyLive(options.dotcms, files);
            notLive = verified.notLive;
            warnings.push(...verified.warnings);
        } catch (error) {
            warnings.push(
                `Upload succeeded but live-verification could not complete: ` +
                    `${errorMessage(error)}. The ${files.length} file(s) listed below WERE ` +
                    `uploaded — do not re-upload them; check their published state directly.`
            );
        }
    }

    return sortManifest({
        src,
        dest: dest.siteQualified,
        count: files.length,
        bytes: sumBytes(files),
        files,
        failures,
        skipped,
        notLive,
        warnings
    });
}

/** Enumerated assets plus whether a cap stopped the walk early (see MAX_ENUMERATED_ASSETS). */
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
    const seen = new Set<string>();

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
        const seenBefore = seen.size;

        for (const asset of page) {
            if (!asset.identifier || !asset.path || seen.has(asset.identifier)) {
                continue;
            }

            const rel = relativeAssetPath(folder, normalizeDotCMSPath(asset.path).path);
            if (!rel || (!recursive && rel.includes('/')) || !matches(rel)) {
                continue;
            }

            seen.add(asset.identifier);
            assets.push(asset);
        }

        if (page.length < SEARCH_LIMIT) {
            break;
        }

        // Termination guard, NOT an optimisation. If the backend ignores or clamps `offset`,
        // every page comes back full of the same identifiers: `page.length < SEARCH_LIMIT`
        // never fires, `seen` de-dupes so `assets` stops growing, and the loop spins forever
        // issuing identical POSTs — an MCP call that never returns while the instance takes
        // sustained load. A page that adds nothing new means we are not advancing, whatever
        // the backend thinks it is doing.
        if (seen.size === seenBefore) {
            break;
        }

        if (assets.length >= MAX_ENUMERATED_ASSETS) {
            return { assets, truncated: true };
        }
    }

    return { assets, truncated: false };
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

/** An uploaded file, plus any caveat the caller must surface (see the 0-byte fallback). */
interface UploadOneResult {
    file: AssetManifestFile;
    warning?: string;
}

async function uploadOneAsset(
    dotcms: DotCMSRuntime,
    file: LocalFile,
    destPath: string,
    publish: boolean
): Promise<UploadOneResult> {
    const bytes = await readFile(file.abs);

    const put = (data: Buffer) =>
        dotcms.request({
            method: 'PUT',
            path: publish ? '/api/v2/assets/publish' : '/api/v2/assets/save',
            formData: {
                path: destPath,
                file: {
                    name: basename(file.rel),
                    type: mimeFor(file.rel),
                    data: data.toString('base64')
                }
            }
        }) as Promise<{ entity?: { identifier?: string } }>;

    let response: { entity?: { identifier?: string } };
    let warning: string | undefined;
    try {
        // Upload the real content, 0-byte included.
        response = await put(bytes);
    } catch (error) {
        // Fallback: if (and only if) dotCMS rejects a 0-byte body, retry with a single
        // newline so the file still lands instead of being dropped. The demo postloop.vtl
        // indicates 0-byte is accepted, so this path is expected to be unused.
        if (bytes.byteLength === 0) {
            response = await put(Buffer.from('\n'));
            // The remote asset now DIFFERS from the source: 1 byte where the source has 0.
            // Reporting a clean success would leave the caller unable to see that, and for
            // an empty VTL or CSS partial the difference is invisible until something
            // downstream behaves oddly. Say it plainly and report the bytes actually sent.
            warning =
                `"${file.rel}" is 0 bytes and dotCMS rejected an empty body, so it was ` +
                `uploaded as a single newline (1 byte) instead. The remote file does NOT ` +
                `match the source exactly.`;
        } else {
            throw error;
        }
    }

    return {
        file: {
            path: file.rel,
            bytes: warning ? 1 : file.bytes,
            identifier: response.entity?.identifier
        },
        warning
    };
}

/** What a verification pass learned. It can only ever ADD to a manifest, never replace it. */
interface VerifyLiveResult {
    notLive: AssetManifestFile[];
    warnings: string[];
}

/**
 * Re-check that every uploaded asset is actually live, re-firing PUBLISH for any that
 * aren't (up to 3 rounds), then confirming the last round's fires.
 *
 * Every await in here is individually guarded, for one reason: this is a READ-ONLY
 * verification of writes that have ALREADY COMMITTED. A throw escaping this function would
 * propagate out of `uploadAssets` and discard `files[]`, `failures[]` and `warnings[]` — so
 * a 120-file theme that uploaded and published perfectly, then hit one flaky liveness GET,
 * would be reported to the model as a failure. Its next move is to re-upload all 120.
 *
 * Verification can therefore only ever downgrade the manifest (add to `notLive`/`warnings`),
 * never replace it with an exception.
 */
async function verifyLive(
    dotcms: DotCMSRuntime,
    files: AssetManifestFile[]
): Promise<VerifyLiveResult> {
    const warnings: string[] = [];

    // A file with no identifier CANNOT be checked, which is not the same as it being fine.
    // Silently filtering these out meant that if the publish envelope ever stopped matching
    // the expected shape, every identifier would be undefined, every file would drop out
    // here, the round loop would never run, and the manifest would report
    // `count: 120, notLive: [], warnings: []` — indistinguishable from a fully verified
    // publish when in fact nothing at all was verified.
    const unverifiable = files.filter((file) => !file.identifier);
    if (unverifiable.length > 0) {
        warnings.push(
            `${unverifiable.length} of ${files.length} uploaded file(s) returned no identifier, ` +
                `so their live status could NOT be verified: ` +
                `${unverifiable.map((file) => file.path).join(', ')}. ` +
                `They may or may not be published — check them directly.`
        );
    }

    let pending = files.filter((file) => file.identifier);

    for (let round = 0; round < 3 && pending.length > 0; round++) {
        const notLive = await collectNotLive(dotcms, pending, warnings);

        if (notLive.length === 0) {
            return { notLive: [], warnings };
        }

        // Sequential, not concurrent: these fire workflow actions against content dotCMS is
        // concurrently versioning and indexing. The per-item catch is the fix that matters —
        // previously the first bad fire (locked by another workflow, or a token without
        // PUBLISH on that folder) threw, so every remaining asset was never even attempted
        // and nothing recorded which ones those were.
        for (const file of notLive) {
            try {
                await dotcms.request({
                    method: 'PUT',
                    path: '/api/v1/workflow/actions/default/fire/PUBLISH',
                    body: { contentlet: { identifier: file.identifier } }
                });
            } catch (error) {
                warnings.push(
                    `Re-publish failed for "${file.path}" (${file.identifier}): ` +
                        `${errorMessage(error)}. Remaining files were still attempted.`
                );
            }
        }

        pending = notLive;
    }

    // The PUBLISH fired in the final round has not been verified yet — without this pass an
    // asset that only goes live on its last re-fire would be reported as notLive despite
    // having published successfully (a false negative in the transfer manifest).
    return { notLive: await collectNotLive(dotcms, pending, warnings), warnings };
}

/**
 * Which of `files` are not live yet.
 *
 * Pure GETs on distinct identifiers with no interdependence, so they run concurrently via
 * `allSettled` — a 120-file theme was previously up to 3 rounds of 120 sequential round
 * trips plus a final 120, and only the last round's results mattered.
 *
 * `allSettled` (not `all`) for the same reason the whole function is guarded: `all` fails
 * fast and discards its settled siblings, and here those siblings ARE the answer. A single
 * rejected read must not decide the fate of the other 119. A file whose check failed is
 * treated as NOT-not-live — it is left out of `notLive` and reported as a warning, so an
 * unreadable status never masquerades as a confirmed failure.
 */
async function collectNotLive(
    dotcms: DotCMSRuntime,
    files: AssetManifestFile[],
    warnings: string[]
): Promise<AssetManifestFile[]> {
    const results = await Promise.allSettled(
        files.map((file) => isLive(dotcms, file.identifier as string))
    );

    const notLive: AssetManifestFile[] = [];
    results.forEach((result, index) => {
        const file = files[index];
        if (result.status === 'rejected') {
            warnings.push(
                `Could not check whether "${file.path}" (${file.identifier}) is live: ` +
                    `${errorMessage(result.reason)}.`
            );

            return;
        }
        if (!result.value) {
            notLive.push(file);
        }
    });

    return notLive;
}

async function isLive(dotcms: DotCMSRuntime, identifier: string): Promise<boolean> {
    const response = (await dotcms.request({
        path: `/api/v1/content/${encodeURIComponent(identifier)}`,
        query: { depth: 0 }
    })) as { entity?: { live?: boolean; contentlets?: Array<{ live?: boolean }> } };
    const entity = response.entity;
    const contentlet = entity?.contentlets?.[0] || entity;

    return contentlet?.live === true;
}

/**
 * Walk `src` and return the files matching `include` (all files when no `include`), plus
 * `totalSeen` — the count of files present regardless of the filter. `totalSeen` lets the caller
 * distinguish "the source dir is empty" from "your include pattern matched none of N real files",
 * so a mistyped glob is reported as a syntax problem instead of silent success.
 */
async function collectLocalFiles(
    src: string,
    include?: string
): Promise<{ files: LocalFile[]; totalSeen: number }> {
    const matches = includeMatcher(include);
    const files: LocalFile[] = [];
    let totalSeen = 0;

    async function walk(dir: string) {
        for (const entry of await readdir(dir, { withFileTypes: true })) {
            const abs = join(dir, entry.name);

            if (entry.isDirectory()) {
                await walk(abs);
                continue;
            }

            if (!entry.isFile()) {
                continue;
            }

            totalSeen++;

            const rel = relative(src, abs).split(sep).join(posix.sep);
            if (!matches(rel)) {
                continue;
            }

            const info = await stat(abs);
            files.push({ abs, rel, bytes: info.size });
        }
    }

    await walk(src);

    return { files: files.sort((a, b) => a.rel.localeCompare(b.rel)), totalSeen };
}

function normalizeDotCMSPath(input: string): { siteQualified?: string; path: string } {
    const value = input.trim().replace(/\/+$/, '');

    if (value.startsWith('//')) {
        const firstSlash = value.slice(2).indexOf('/');
        if (firstSlash < 0) {
            throw new Error(`Site-qualified path "${input}" must include a path`);
        }

        return { siteQualified: value, path: value.slice(firstSlash + 2) };
    }

    if (!value.startsWith('/')) {
        throw new Error(`dotCMS path "${input}" must start with "/" or "//host/"`);
    }

    return { path: value };
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

async function prepareWritableDir(dest: string): Promise<string> {
    if (!isAbsolute(dest)) {
        throw new Error(`Destination must be an absolute path: ${dest}`);
    }

    const resolved = resolve(dest);
    await mkdir(resolved, { recursive: true });
    await access(resolved, constants.W_OK);

    return resolved;
}

async function prepareReadableDir(src: string): Promise<string> {
    if (!isAbsolute(src)) {
        throw new Error(`Source must be an absolute path: ${src}`);
    }

    const resolved = resolve(src);
    const info = await stat(resolved);
    if (!info.isDirectory()) {
        throw new Error(`Source must be a directory: ${src}`);
    }

    await access(resolved, constants.R_OK);

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

/**
 * Split an `include` string into its comma-separated patterns — but NOT on commas inside a brace
 * group, so `*.{png,webp,jpg}` stays one pattern while `*.vtl,*.scss` is two. Exported for tests.
 */
export function splitIncludePatterns(include?: string): string[] {
    if (!include) {
        return [];
    }
    const patterns: string[] = [];
    let current = '';
    let braceDepth = 0;
    for (const ch of include) {
        if (ch === '{') {
            braceDepth++;
            current += ch;
        } else if (ch === '}') {
            braceDepth = Math.max(0, braceDepth - 1);
            current += ch;
        } else if (ch === ',' && braceDepth === 0) {
            patterns.push(current);
            current = '';
        } else {
            current += ch;
        }
    }
    patterns.push(current);
    return patterns.map((p) => p.trim()).filter(Boolean);
}

/**
 * Build a matcher over relative POSIX paths from a comma-separated `include` string. A file matches
 * if it matches ANY pattern. No `include` → matches everything. Exported for tests.
 *
 * Supports the glob features callers reasonably assume from a standard glob:
 *   - `*`  matches any run of chars WITHIN a path segment (does not cross `/`)
 *   - `**` matches across segments, including zero (so a leading globstar also matches a top-level file)
 *   - `?`  matches a single non-`/` char
 *   - `{png,webp,jpg}` brace expansion (alternation)
 * A pattern with no `/` matches the file's basename anywhere in the tree; a pattern with a `/` is
 * anchored at the root of `src`.
 */
export function includeMatcher(include?: string): (rel: string) => boolean {
    const patterns = splitIncludePatterns(include);

    if (!patterns.length) {
        return () => true;
    }

    // Compile each pattern ONCE here, not per-file — this matcher runs on every asset/file.
    const regexes = patterns.map(globToRegExp);

    return (rel: string) => regexes.some((re) => re.test(rel));
}

/**
 * Compile a single glob pattern to a RegExp with a single left-to-right character scan.
 *
 * A scanner (rather than chained `.replace()` passes) is used deliberately: it has no ordering
 * hazard between `**` and `*`, needs no placeholder sentinels, and each glob token emits its regex
 * exactly once. The old chained-replace version turned every `*` into `[^/]*`, so a `**` + `/*.png`
 * pattern compiled to "exactly one subdirectory" and silently matched nothing for top-level files.
 *
 * Tokens:
 *   - `**` (with an optional adjacent `/`) crosses directory boundaries, matching zero or more
 *     segments, so a leading `**` also matches a top-level file.
 *   - `*` matches any run of chars within one segment (never crosses `/`).
 *   - `?` matches a single non-`/` char.
 *   - `{png,webp,jpg}` expands to alternation `(?:png|webp|jpg)` (nested wildcards are honored).
 * A pattern containing `/` is anchored at the root of `src`; otherwise it matches a basename
 * anywhere in the tree.
 */
function globToRegExp(pattern: string): RegExp {
    const normalized = pattern.split(sep).join(posix.sep);
    const anchored = normalized.includes('/');
    const source = compileGlob(normalized, 0, normalized.length);
    return new RegExp(`${anchored ? '^' : '(^|/)'}${source}$`, 'i');
}

/** Regex-escape a single literal character. */
function escapeRegexChar(ch: string): string {
    return /[|\\{}()[\]^$+.*?]/.test(ch) ? `\\${ch}` : ch;
}

/**
 * Translate the glob in `input[start..end)` to a regex source string. Recurses into brace groups so
 * `{a*,b}` honors the wildcard inside each alternative.
 */
function compileGlob(input: string, start: number, end: number): string {
    let out = '';
    let i = start;

    while (i < end) {
        const ch = input[i];

        if (ch === '*') {
            if (input[i + 1] === '*') {
                // `**` crosses directory boundaries. Consume it plus one adjacent `/` (leading or
                // trailing) and emit an optional "any number of full segments" fragment.
                i += 2;
                const trailing = i >= end;
                if (input[i] === '/') {
                    i++;
                } else if (!trailing && out.endsWith('/')) {
                    out = out.slice(0, -1);
                }

                if (trailing) {
                    // A globstar with nothing after it — `themes/**` — means "everything
                    // below here", so it has to be able to match a final FILENAME segment.
                    // The general fragment below cannot: it only ever ends at a `/`, so
                    // `themes/**` compiled to `^themes(?:.*/)?$` and matched nothing but the
                    // bare string `themes`. Since `dir/**` is the common idiom, users writing
                    // it hit the "matched 0 of N files, check the glob syntax" warning while
                    // their syntax was perfectly reasonable.
                    out += '.*';
                } else {
                    out += '(?:.*/)?';
                }
            } else {
                out += '[^/]*';
                i++;
            }
        } else if (ch === '?') {
            out += '[^/]';
            i++;
        } else if (ch === '{') {
            const close = input.indexOf('}', i);
            if (close === -1 || close >= end) {
                // Unbalanced brace: treat the `{` literally rather than throwing.
                out += '\\{';
                i++;
            } else {
                const alternatives = splitTopLevelCommas(input.slice(i + 1, close)).map((alt) =>
                    compileGlob(alt, 0, alt.length)
                );
                out += `(?:${alternatives.join('|')})`;
                i = close + 1;
            }
        } else {
            out += escapeRegexChar(ch);
            i++;
        }
    }

    return out;
}

/** Split on commas that are NOT inside a nested brace group (for brace-group alternatives). */
function splitTopLevelCommas(group: string): string[] {
    const parts: string[] = [];
    let current = '';
    let depth = 0;
    for (const ch of group) {
        if (ch === '{') {
            depth++;
            current += ch;
        } else if (ch === '}') {
            depth = Math.max(0, depth - 1);
            current += ch;
        } else if (ch === ',' && depth === 0) {
            parts.push(current);
            current = '';
        } else {
            current += ch;
        }
    }
    parts.push(current);
    return parts;
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

function mimeFor(path: string): string {
    return MIME_BY_EXT[extname(path).toLowerCase()] || 'application/octet-stream';
}

function sumBytes(files: AssetManifestFile[]): number {
    return files.reduce((sum, file) => sum + file.bytes, 0);
}

function sortManifest<
    T extends {
        files: AssetManifestFile[];
        failures: AssetManifestFailure[];
        skipped?: AssetManifestSkipped[];
        notLive?: AssetManifestFile[];
    }
>(manifest: T): T {
    const byPath = (a: { path: string }, b: { path: string }) => a.path.localeCompare(b.path);
    manifest.files.sort(byPath);
    manifest.failures.sort(byPath);
    manifest.skipped?.sort(byPath);
    manifest.notLive?.sort(byPath);
    return manifest;
}
