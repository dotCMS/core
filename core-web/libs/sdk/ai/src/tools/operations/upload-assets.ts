import { constants } from 'node:fs';
import { access, readdir, readFile, stat } from 'node:fs/promises';
import { basename, extname, isAbsolute, join, posix, relative, resolve, sep } from 'node:path';

import {
    normalizeDotCMSPath,
    sortManifest,
    sumBytes,
    type AssetManifestFailure,
    type AssetManifestFile,
    type AssetManifestSkipped
} from './shared/asset-common';
import { includeMatcher } from './shared/glob';
import { assertInsideRoot } from './shared/local-root';
import { isContentLive } from './shared/page-common';

import { ValidationError, type DotCMSRuntime } from '../../runtime';
import { type Endpoint } from '../toolkit/endpoints';
import { errorMessage } from '../toolkit/tool-runtime';

/**
 * Every endpoint `uploadAssets` calls — the `upload_assets` tool enforces exactly this list,
 * and `upload-assets.spec.ts` checks every request against it. Add a request, add it here.
 */
export const UPLOAD_ASSETS_ENDPOINTS: readonly Endpoint[] = [
    'PUT /api/v2/assets/publish',
    'PUT /api/v2/assets/save',
    'GET /api/v1/content/{identifier}',
    'PUT /api/v1/workflow/actions/default/fire/PUBLISH'
];

export interface UploadAssetsOptions {
    dotcms: DotCMSRuntime;
    /** Absolute local directory the files are read from. */
    src: string;
    /** Host-qualified destination folder, e.g. `//demo.dotcms.com/application/themes/travel`. */
    dest: string;
    /** Comma-separated glob filter, matched against each path relative to `src`. */
    include?: string;
    /** Publish (`/api/v2/assets/publish`) rather than only save. */
    publish: boolean;
    /** After publishing, confirm each asset reports live. */
    verify: boolean;
    /**
     * Directory `src` must be inside — after symlinks resolve. Omit only when YOU chose `src`;
     * the `upload_assets` tool always sets it.
     */
    root?: string;
}

/** What `uploadAssets` returns — never the file bytes, only what landed and what did not. */
export interface UploadAssetsManifest {
    src: string;
    dest: string;
    count: number;
    bytes: number;
    files: AssetManifestFile[];
    failures: AssetManifestFailure[];
    skipped: AssetManifestSkipped[];
    /** Published assets that did not report live when verified. */
    notLive: AssetManifestFile[];
    warnings: string[];
}

interface LocalFile {
    abs: string;
    rel: string;
    bytes: number;
}

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

/**
 * Upload every file under a local directory as dotCMS file assets, preserving relative
 * paths. Files are saved or published one at a time; a failed file is recorded in the
 * manifest rather than aborting the batch.
 */
export async function uploadAssets(options: UploadAssetsOptions): Promise<UploadAssetsManifest> {
    const src = await prepareReadableDir(options.src, options.root);
    const dest = normalizeDotCMSPath(options.dest);

    if (!dest.siteQualified) {
        throw new ValidationError(
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
        files.map((file) => isContentLive(dotcms, file.identifier as string))
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

async function prepareReadableDir(src: string, root: string | undefined): Promise<string> {
    if (!isAbsolute(src)) {
        throw new ValidationError(`Source must be an absolute path: ${src}`);
    }

    const resolved = resolve(src);
    const info = await stat(resolved);
    if (!info.isDirectory()) {
        throw new ValidationError(`Source must be a directory: ${src}`);
    }

    await assertInsideRoot(root, 'src', resolved);
    await access(resolved, constants.R_OK);

    return resolved;
}

function mimeFor(path: string): string {
    return MIME_BY_EXT[extname(path).toLowerCase()] || 'application/octet-stream';
}
