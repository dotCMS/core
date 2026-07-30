import { forkJoin, Observable, of } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { catchError, map, switchMap } from 'rxjs/operators';

import { DotCMSAPIResponse, DotCMSContentlet } from '@dotcms/dotcms-models';

import {
    ContainerSourceView,
    PageDiffFile,
    PageRenderSourcesView,
    PageSourceFile,
    ThemeSourceView,
    WidgetSourceView
} from '../models/page-render-sources.models';

/**
 * Resolves the "working vs live" file diff for a page.
 *
 * The flow is two-staged and entirely read-only:
 *   1. {@link getPageSources} → `_render-sources` for the page, flattened + de-duped
 *      into the file assets that compose it (theme VTL/CSS, container VTLs, widget VTLs).
 *   2. {@link getDiffFiles} → for each file, look up its versions, fetch the working
 *      and live text, and keep only the files whose two versions actually differ.
 *
 * All calls go same-origin to the dotCMS backend (the dev server proxies `/api`
 * and `/dA` to :8080), so no dev-only path prefix is needed here — unlike the
 * run screen's iframes, which render pages via the `/dot-page` proxy sentinel.
 */
@Injectable()
export class DotPageSourcesService {
    readonly #http = inject(HttpClient);

    /**
     * Fetch the page's source files via `_render-sources` and flatten them into
     * a de-duplicated list of file assets (by identifier). Only files backed by a
     * real file asset (with an identifier) survive — inline CODE widgets and DB
     * containers have no file to diff and are dropped.
     *
     * @param uri     Plain page path, e.g. `/index` or `/about/team` (no host).
     * @param hostId  Host identifier to disambiguate the site (`host_id` query param).
     * @param languageId Language id for the render-sources lookup.
     */
    getPageSources(uri: string, hostId: string, languageId: number): Observable<PageSourceFile[]> {
        // JAX-RS binds `{uri: .*}` with the leading slash stripped; the backend
        // re-adds it. Send the path without a leading slash so it slots cleanly
        // after `_render-sources/`.
        const path = uri.startsWith('/') ? uri.slice(1) : uri;
        const params = new URLSearchParams({
            host_id: hostId,
            language_id: String(languageId)
        });

        return this.#http
            .get<
                DotCMSAPIResponse<PageRenderSourcesView>
            >(`/api/v1/page/_render-sources/${path}?${params.toString()}`)
            .pipe(map((response) => flattenSources(response?.entity)));
    }

    /**
     * Resolve the working-vs-live diff for every page source file: fetch each
     * file's versions, pull the working + live text, and return only the files
     * whose two versions differ (with per-file added/removed line counts).
     *
     * Files that fail to resolve (deleted, no versions, fetch error) are skipped
     * rather than aborting the whole diff — a single bad asset shouldn't blank the
     * screen. The result is ordered theme → container → widget, then by name.
     *
     * @param files      The flattened source files from {@link getPageSources}.
     * @param languageId Numeric language id — versions are filtered to this language
     *                   by each version's own `languageId`, so no id→iso mapping is needed.
     */
    getDiffFiles(files: PageSourceFile[], languageId: number): Observable<PageDiffFile[]> {
        if (!files.length) {
            return of([]);
        }

        return forkJoin(files.map((file) => this.resolveDiffFile(file, languageId))).pipe(
            map((results) => results.filter((f): f is PageDiffFile => f !== null).sort(byOriginThenName))
        );
    }

    /**
     * Resolve a single file's working-vs-live diff, or `null` when it can't be
     * diffed (no versions, missing working/live, fetch error, or identical text).
     */
    private resolveDiffFile(
        file: PageSourceFile,
        languageId: number
    ): Observable<PageDiffFile | null> {
        return this.getVersions(file.identifier, languageId).pipe(
            switchMap((versions) => {
                const working = versions.find((v) => v.working);
                const live = versions.find((v) => v.live);

                // No working version → nothing the agent could have changed. No live
                // version → a brand-new working-only file; still worth showing as an
                // all-added diff against an empty "before".
                if (!working) {
                    return of(null);
                }

                const workingUrl = versionUrl(working);
                const liveUrl = live ? versionUrl(live) : null;
                if (!workingUrl) {
                    return of(null);
                }

                return forkJoin({
                    working: this.fetchText(workingUrl),
                    live: liveUrl ? this.fetchText(liveUrl) : of('')
                }).pipe(
                    map(({ working: workingText, live: liveText }) => {
                        // Identical → not a change; drop it (the whole point is to
                        // surface only what the agent touched).
                        if (workingText === liveText) {
                            return null;
                        }
                        const { added, removed } = countLineChanges(liveText, workingText);

                        return { ...file, working: workingText, live: liveText, added, removed };
                    })
                );
            }),
            // A single asset failing (deleted, 404, etc.) must not blank the diff.
            catchError(() => of(null))
        );
    }

    /**
     * All versions of a file asset for the given language, via `/api/v1/content/versions`.
     *
     * The response groups versions by language ISO code (e.g. `en-us`), which we
     * don't have from the numeric page `languageId`. Rather than resolve id→iso,
     * flatten every group and filter by each version's own `languageId` — the
     * numeric id we already hold.
     */
    private getVersions(identifier: string, languageId: number): Observable<DotCMSContentlet[]> {
        return this.#http
            .get<
                DotCMSAPIResponse<{ versions: Record<string, DotCMSContentlet[]> }>
            >(`/api/v1/content/versions?identifier=${identifier}&groupByLang=1`)
            .pipe(
                map((response) => {
                    const groups = response?.entity?.versions ?? {};

                    return Object.values(groups)
                        .flat()
                        .filter((version) => version.languageId === languageId);
                })
            );
    }

    /**
     * Fetch the raw text of a specific file version. The `fileAssetVersion` path
     * (`/dA/<inode>/fileAsset/<name>`) is version-specific (the inode identifies
     * the version), so GETting it returns that exact version's bytes.
     */
    private fetchText(url: string): Observable<string> {
        return this.#http.get(url, { responseType: 'text' }).pipe(catchError(() => of('')));
    }
}

/**
 * The versioned asset URL of a file-asset contentlet — `fileAssetVersion` is the
 * version-specific `/dA/<inode>/...` path; `fileAsset` is the identifier-level
 * fallback. Mirrors `getFileVersion` in `@dotcms/utils`.
 */
function versionUrl(contentlet: DotCMSContentlet): string | null {
    return (
        (contentlet['fileAssetVersion'] as string) ||
        (contentlet['assetVersion'] as string) ||
        (contentlet['fileAsset'] as string) ||
        null
    );
}

/**
 * Flatten the `_render-sources` tree into a de-duplicated (by identifier) list of
 * file-asset source files. Theme files, FILE-container content-type VTLs, and
 * FILE widgets each contribute; DB containers and CODE widgets have no file.
 */
function flattenSources(view: PageRenderSourcesView | undefined): PageSourceFile[] {
    if (!view) {
        return [];
    }

    const byId = new Map<string, PageSourceFile>();
    const add = (
        identifier: string | undefined,
        path: string | undefined,
        origin: PageSourceFile['origin'],
        extension?: string
    ) => {
        if (!identifier || !path || byId.has(identifier)) {
            return;
        }
        byId.set(identifier, {
            identifier,
            path,
            name: basename(path),
            folder: dirname(path),
            extension: (extension ?? extensionOf(path)).toLowerCase(),
            origin
        });
    };

    // Theme files (VTL, CSS, SCSS, …) — each carries its own extension.
    (view.theme as ThemeSourceView | undefined)?.files?.forEach((f) =>
        add(f.identifier, f.path, 'theme', f.extension)
    );

    // Container VTLs — only FILE containers reference a file per content type.
    Object.values(view.containers ?? {}).forEach((container: ContainerSourceView) =>
        container.contentTypes?.forEach((ct) => add(ct.identifier, ct.path, 'container'))
    );

    // Widget VTLs — only FILE widgets reference a file.
    (view.widgets ?? []).forEach((w: WidgetSourceView) => add(w.identifier, w.path, 'widget'));

    return [...byId.values()];
}

/** Last path segment, e.g. `//host/a/b/header.vtl` → `header.vtl`. */
function basename(path: string): string {
    const parts = path.split('/').filter(Boolean);

    return parts.length ? parts[parts.length - 1] : path;
}

/**
 * Host-qualified folder that contains the file — the path minus its filename,
 * with the leading `//host` prefix and a trailing slash preserved, e.g.
 * `//host/a/b/header.vtl` → `//host/a/b/`. Returns the path unchanged when it has
 * no `/` to strip.
 */
function dirname(path: string): string {
    const lastSlash = path.lastIndexOf('/');

    return lastSlash > -1 ? path.slice(0, lastSlash + 1) : path;
}

/** Lowercased extension without the dot, e.g. `header.vtl` → `vtl`; `''` when none. */
function extensionOf(path: string): string {
    const name = basename(path);
    const dot = name.lastIndexOf('.');

    return dot > -1 ? name.slice(dot + 1) : '';
}

/**
 * Count added / removed lines between two texts. A line present in `next` but not
 * `prev` counts as added and vice-versa. This is a coarse multiset delta — enough
 * for the +N / −M badges beside each file; Monaco owns the precise line-by-line
 * rendering.
 */
function countLineChanges(prev: string, next: string): { added: number; removed: number } {
    const prevLines = prev.length ? prev.split('\n') : [];
    const nextLines = next.length ? next.split('\n') : [];

    const counts = new Map<string, number>();
    for (const line of prevLines) {
        counts.set(line, (counts.get(line) ?? 0) + 1);
    }
    let added = 0;
    for (const line of nextLines) {
        const remaining = counts.get(line) ?? 0;
        if (remaining > 0) {
            counts.set(line, remaining - 1);
        } else {
            added++;
        }
    }
    // Whatever prev lines were never matched by a next line were removed.
    let removed = 0;
    for (const remaining of counts.values()) {
        removed += remaining;
    }

    return { added, removed };
}

/** Sort diff files theme → container → widget, then alphabetically by name. */
const ORIGIN_RANK: Record<PageSourceFile['origin'], number> = {
    theme: 0,
    container: 1,
    widget: 2
};
function byOriginThenName(a: PageSourceFile, b: PageSourceFile): number {
    return ORIGIN_RANK[a.origin] - ORIGIN_RANK[b.origin] || a.name.localeCompare(b.name);
}
