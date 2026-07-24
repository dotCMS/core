import { constants } from 'node:fs';
import { access, mkdir, readdir, readFile, stat, writeFile } from 'node:fs/promises';
import { basename, extname, isAbsolute, join, posix, relative, resolve, sep } from 'node:path';

import { createRuntime, isBinaryResponseEnvelope } from '@dotcms/ai/runtime';

import { errorMessage } from './runtime';

type DotCMSRuntime = ReturnType<typeof createRuntime>;
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
        const assets = await enumerateAssets(
            options.dotcms,
            input.path,
            options.recursive,
            options.include
        );

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

    const localFiles = await collectLocalFiles(src, options.include);
    const files: AssetManifestFile[] = [];
    const failures: AssetManifestFailure[] = [];
    const skipped: AssetManifestSkipped[] = [];
    const warnings: string[] = [];

    if (localFiles.length === 0) {
        warnings.push(
            options.include
                ? `No files under "${src}" matched the include filter "${options.include}".`
                : `No files found under "${src}".`
        );
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
            files.push(uploaded);
        } catch (error) {
            failures.push({ path: file.rel, error: errorMessage(error) });
        }
    }

    const notLive =
        options.publish && options.verify ? await verifyLive(options.dotcms, files) : [];

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

async function enumerateAssets(
    dotcms: DotCMSRuntime,
    folder: string,
    recursive: boolean,
    include?: string
): Promise<AssetContentlet[]> {
    const matches = includeMatcher(include);
    const assets: AssetContentlet[] = [];
    const seen = new Set<string>();

    for (let offset = 0; ; offset += SEARCH_LIMIT) {
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
    }

    return assets;
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

async function uploadOneAsset(
    dotcms: DotCMSRuntime,
    file: LocalFile,
    destPath: string,
    publish: boolean
): Promise<AssetManifestFile> {
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
    try {
        // Upload the real content, 0-byte included.
        response = await put(bytes);
    } catch (error) {
        // Fallback: if (and only if) dotCMS rejects a 0-byte body, retry with a single
        // newline so the file still lands instead of being dropped. The demo postloop.vtl
        // indicates 0-byte is accepted, so this path is expected to be unused.
        if (bytes.byteLength === 0) {
            response = await put(Buffer.from('\n'));
        } else {
            throw error;
        }
    }

    return {
        path: file.rel,
        bytes: file.bytes,
        identifier: response.entity?.identifier
    };
}

async function verifyLive(
    dotcms: DotCMSRuntime,
    files: AssetManifestFile[]
): Promise<AssetManifestFile[]> {
    let pending = files.filter((file) => file.identifier);

    for (let round = 0; round < 3 && pending.length > 0; round++) {
        const notLive: AssetManifestFile[] = [];

        for (const file of pending) {
            if (!(await isLive(dotcms, file.identifier as string))) {
                notLive.push(file);
            }
        }

        if (notLive.length === 0) {
            return [];
        }

        for (const file of notLive) {
            await dotcms.request({
                method: 'PUT',
                path: '/api/v1/workflow/actions/default/fire/PUBLISH',
                body: { contentlet: { identifier: file.identifier } }
            });
        }

        pending = notLive;
    }

    return pending;
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

async function collectLocalFiles(src: string, include?: string): Promise<LocalFile[]> {
    const matches = includeMatcher(include);
    const files: LocalFile[] = [];

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

            const rel = relative(src, abs).split(sep).join(posix.sep);
            if (!matches(rel)) {
                continue;
            }

            const info = await stat(abs);
            files.push({ abs, rel, bytes: info.size });
        }
    }

    await walk(src);

    return files.sort((a, b) => a.rel.localeCompare(b.rel));
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

function includeMatcher(include?: string): (rel: string) => boolean {
    const patterns = include
        ?.split(',')
        .map((pattern) => pattern.trim())
        .filter(Boolean);

    if (!patterns?.length) {
        return () => true;
    }

    // Compile each pattern ONCE here, not per-file — this matcher runs on every asset/file.
    const regexes = patterns.map(globToRegExp);

    return (rel: string) => regexes.some((re) => re.test(rel));
}

function globToRegExp(pattern: string): RegExp {
    const normalized = pattern.split(sep).join(posix.sep);
    const source = normalized.replace(/[|\\{}()[\]^$+?.]/g, '\\$&').replace(/\*/g, '[^/]*');

    return new RegExp(`${normalized.includes('/') ? '^' : '(^|/)'}${source}$`, 'i');
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
