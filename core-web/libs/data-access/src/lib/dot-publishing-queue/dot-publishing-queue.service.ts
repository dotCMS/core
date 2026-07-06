import { Observable, of } from 'rxjs';

import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { catchError, map } from 'rxjs/operators';

import {
    BundleAssetView,
    DotCMSResponse,
    PublishAuditStatus,
    PublishingJobDetailView,
    PublishingJobsResponse,
    PushBundleForm,
    PushBundleResultView,
    RemoveAssetResultView,
    RetryBundleResultView,
    UnsentBundlesResponse
} from '@dotcms/dotcms-models';

export type PublishingSortField = 'bundle_name' | 'status' | 'created' | 'modified';
export type PublishingSortDirection = 'asc' | 'desc';

export interface ListPublishingJobsParams {
    /** Empty/omitted = all statuses (BE returns every row in publish_audit). */
    statuses?: readonly PublishAuditStatus[];
    page?: number;
    perPage?: number;
    filter?: string;
    sort?: PublishingSortField;
    sortDirection?: PublishingSortDirection;
}

export type RetryDeliveryStrategy = 'ALL_ENDPOINTS' | 'FAILED_ENDPOINTS';

export interface RetryBundlesPayload {
    bundleIds: string[];
    forcePush?: boolean;
    deliveryStrategy?: RetryDeliveryStrategy;
}

/**
 * Backs the Publishing Queue Angular portlet.
 *
 * v1 endpoints (`com.dotcms.rest.api.v1.publishing.PublishingResource`):
 * - `GET /api/v1/publishing` — list bundles by status (sort/filter via params)
 * - `GET /api/v1/publishing/{bundleId}` — full bundle detail with endpoints
 * - `POST /api/v1/publishing/retry` — retry bundles (bulk)
 * - `DELETE /api/v1/publishing/{bundleId}` — delete single bundle (synchronous)
 * - `DELETE /api/v1/publishing/purge?status=...` — bulk delete by status (async,
 *    WebSocket-notified; omit `status` to use safe defaults = ALL terminal/queued)
 *
 * Legacy endpoints (`com.dotcms.rest.BundleResource`) still used until #36048 lands:
 * - `DELETE /api/bundle/ids` — bulk delete by bundle id (async, WebSocket-notified;
 *    same endpoint the legacy JSP uses for "SELECTED" in the delete-bundles dialog)
 * - `GET /api/bundle/{bundleId}/assets` — asset list inside a bundle
 * - `POST /api/bundle/sync` — synchronous .tar.gz upload (licensed)
 * - `GET /api/bundle/_download/{bundleId}` — bundle download (URL only)
 *
 * Two push paths exist:
 * - Row-level "Configure & Send" delegates to the project-wide push publish
 *   dialog (`DotPushPublishDialogService.open(...)` from `@dotcms/dotcms-js`),
 *   which hits the legacy `/DotAjaxDirector/.../cmd/pushBundle` endpoint via
 *   `PushPublishService`.
 * - The Select Bundle dialog calls `pushBundle` (see below), which posts to
 *   `POST /api/v1/publishing/push/{bundleId}` — the modern v1 REST endpoint.
 *
 * Bundle generate-and-download is delegated to the project-wide download dialog
 * (`DotDownloadBundleDialogService.open(bundleId)` from
 * `@services/dot-download-bundle-dialog/...`), which posts to `/api/bundle/_generate`
 * with the user-picked filter + operation and triggers a browser download.
 * No bespoke generate endpoint lives here either.
 */
@Injectable({
    providedIn: 'root'
})
export class DotPublishingQueueService {
    readonly #http = inject(HttpClient);

    listPublishingJobs(params: ListPublishingJobsParams): Observable<PublishingJobsResponse> {
        let httpParams = new HttpParams();

        // Only send status when the caller selected one or more — omitting the
        // param tells the BE "all statuses" and makes the FE forward-compatible
        // with new server-side statuses (e.g. SCHEDULED, see #36267).
        if (params.statuses && params.statuses.length > 0) {
            httpParams = httpParams.set('status', params.statuses.join(','));
        }

        if (params.page !== undefined) {
            httpParams = httpParams.set('page', params.page);
        }

        if (params.perPage !== undefined) {
            httpParams = httpParams.set('per_page', params.perPage);
        }

        if (params.filter && params.filter.length > 0) {
            httpParams = httpParams.set('filter', params.filter);
        }

        if (params.sort) {
            const order = params.sortDirection === 'desc' ? '-' : '';
            httpParams = httpParams.set('sort', `${order}${params.sort}`);
        }

        return this.#http.get<PublishingJobsResponse>('/api/v1/publishing', {
            params: httpParams
        });
    }

    getPublishingJobDetails(bundleId: string): Observable<PublishingJobDetailView> {
        return this.#http
            .get<
                DotCMSResponse<PublishingJobDetailView>
            >(`/api/v1/publishing/${encodeURIComponent(bundleId)}`)
            .pipe(map((response) => response.entity));
    }

    retryBundles(payload: RetryBundlesPayload): Observable<RetryBundleResultView[]> {
        return this.#http
            .post<DotCMSResponse<RetryBundleResultView[]>>('/api/v1/publishing/retry', payload)
            .pipe(map((response) => response.entity));
    }

    /**
     * Queues a bundle for publishing via the modern v1 REST endpoint.
     * Replaces the legacy `/DotAjaxDirector/.../cmd/pushBundle` AJAX action — same
     * effect, proper JSON in/out + proper HTTP status codes.
     *
     * BE endpoint: `POST /api/v1/publishing/push/{bundleId}` (see
     * `com.dotcms.rest.api.v1.publishing.PublishingResource.pushBundle`).
     * Single-bundle per call; callers needing to push N bundles must fan out
     * (e.g. via `forkJoin`).
     */
    pushBundle(bundleId: string, form: PushBundleForm): Observable<PushBundleResultView> {
        return this.#http
            .post<
                DotCMSResponse<PushBundleResultView>
            >(`/api/v1/publishing/push/${encodeURIComponent(bundleId)}`, form)
            .pipe(map((response) => response.entity));
    }

    deleteBundle(bundleId: string): Observable<{ message: string }> {
        return this.#http.delete<{
            message: string;
        }>(`/api/v1/publishing/${encodeURIComponent(bundleId)}`);
    }

    /**
     * Bulk delete bundles by id — fire-and-forget. The BE acks immediately with a
     * `ResponseEntityView<string>` message; the actual deletion runs on a background
     * thread and notifies the user via WebSocket system message when finished.
     *
     * Uses the legacy `DELETE /api/bundle/ids` (same endpoint as the JSP) until the
     * v1 consolidation work (#36048) ships an equivalent under `/api/v1/publishing`.
     */
    deleteBundles(bundleIds: string[]): Observable<unknown> {
        return this.#http.request<unknown>('DELETE', '/api/bundle/ids', {
            body: { identifiers: bundleIds }
        });
    }

    /**
     * Bulk-purges bundles by status — fire-and-forget. Mirrors the legacy
     * `/api/bundle/all{,/success,/fail}` endpoints behind a single v1 path. Omit
     * `statuses` to use the BE's safe defaults (all terminal + queued; in-progress
     * statuses are rejected with 400). BE acks immediately; result is delivered via
     * WebSocket system message.
     */
    purgeBundles(statuses?: readonly PublishAuditStatus[]): Observable<unknown> {
        const params =
            statuses && statuses.length > 0
                ? new HttpParams().set('status', statuses.join(','))
                : new HttpParams();
        return this.#http.delete<unknown>('/api/v1/publishing/purge', { params });
    }

    uploadBundle(file: File): Observable<{ bundleName: string; status: string }> {
        const formData = new FormData();
        formData.append('file', file, file.name);
        return this.#http.post<{ bundleName: string; status: string }>('/api/bundle/sync', formData);
    }

    /** Builds the absolute download URL for a bundle's `.tar.gz`. */
    getBundleDownloadUrl(bundleId: string): string {
        return `/api/bundle/_download/${encodeURIComponent(bundleId)}`;
    }

    /**
     * Mirrors the legacy `DotDownloadBundleDialogComponent` submit: POSTs to
     * `/api/bundle/_generate` and returns the resulting `.tar.gz` blob plus the
     * filename parsed from the `content-disposition` header.
     *
     * Used by the inline Download menu in the Select Bundle dialog — picking a
     * filter from the menu must produce the exact same file a customer gets
     * from the legacy modal, so the wire payload here is byte-identical:
     *   `{ bundleId, operation: '0' | '1', filterKey }`
     * where `'0'` = publish, `'1'` = unpublish (BE vocabulary, not the v1 REST
     * `'publish'` / `'expire'` strings — the `_generate` endpoint predates
     * v1).
     *
     * Uses `HttpClient.post` with `{ observe: 'response', responseType: 'blob' }`
     * so the project's auth + error interceptors apply and the headers are
     * available for the filename parse — the legacy dialog used raw `fetch`
     * only because of how it bootstrapped pre-v1.
     */
    generateBundle(
        bundleId: string,
        operation: '0' | '1',
        filterKey: string
    ): Observable<{ blob: Blob; filename: string }> {
        return this.#http
            .post(
                '/api/bundle/_generate',
                { bundleId, operation, filterKey },
                {
                    observe: 'response',
                    responseType: 'blob'
                }
            )
            .pipe(
                map((response) => {
                    if (!response.body) {
                        throw new Error('Empty bundle-generate response body');
                    }
                    return {
                        blob: response.body,
                        filename: parseFilenameFromContentDisposition(
                            response.headers.get('content-disposition')
                        )
                    };
                })
            );
    }

    /** Builds the absolute download URL for a bundle's manifest CSV. */
    getBundleManifestUrl(bundleId: string): string {
        return `/api/bundle/${encodeURIComponent(bundleId)}/manifest`;
    }

    /**
     * Returns `true` when the bundle's `.tar.gz` is downloadable right now.
     *
     * Why this probe exists:
     * The legacy JSP gates the "Download Bundle" button on two server-side
     * conditions:
     *   1. the `.tar.gz` file existing on disk (it may have been purged via
     *      `DELETE /api/bundle/olderthan/{N}` — there is no scheduled cleanup
     *      built into the core, so this happens only on manual admin action);
     *   2. the targeted environments not being static-publish / S3 (those
     *      protocols don't produce a downloadable archive).
     *
     * Neither piece of state is exposed on the current detail response
     * (`GET /api/v1/publishing/{bundleId}`). Until the BE adds a `hasBundle`
     * (or `endpointProtocols`) flag we can read directly, the FE has to ask
     * the actual download endpoint. HEAD is the lightweight option — JAX-RS
     * auto-handles HEAD by invoking the `@GET` handler and discarding the
     * body, so we get the file-existence answer without paying for the
     * payload. A 404 (file purged) becomes `false` via `catchError`.
     *
     * @see `RemotePublishAjaxAction.downloadBundle` and `BundleResource._download`
     *      for the server-side behavior.
     */
    probeBundleDownload(bundleId: string): Observable<boolean> {
        return this.#http.head(this.getBundleDownloadUrl(bundleId), { observe: 'response' }).pipe(
            map((response) => response.status === 200),
            catchError(() => of(false))
        );
    }

    /**
     * Returns `true` when the bundle's manifest CSV is downloadable right now.
     *
     * Same rationale as {@link probeBundleDownload}: the legacy JSP calls
     * `ManifestUtil.manifestExists(bundleId)` server-side, which (a) checks
     * that the `.tar.gz` is on disk and (b) scans the archive for a manifest
     * entry — bundles built before the manifest feature was introduced have
     * no entry. Neither check is exposed on `GET /api/v1/publishing/{bundleId}`,
     * so the FE probes the actual download endpoint with HEAD.
     *
     * @see `BundleResource.downloadManifest` and `ManifestUtil.manifestExists`
     *      for the server-side behavior.
     */
    probeBundleManifest(bundleId: string): Observable<boolean> {
        return this.#http.head(this.getBundleManifestUrl(bundleId), { observe: 'response' }).pipe(
            map((response) => response.status === 200),
            catchError(() => of(false))
        );
    }

    /**
     * Lists unsent (draft) bundles owned by the given user.
     *
     * Backed by the legacy endpoint `GET /api/bundle/getunsendbundles/userid/{userId}`
     * (`BundleResource#getUnsendBundles`). The newer v1 `/api/v1/publishing` reads
     * from `publish_audit` and does NOT include drafts — drafts live in
     * `publishing_bundle` only. This is the only endpoint that surfaces them
     * until #36048 (legacy → v1 consolidation) lands.
     *
     * Response shape: `{ identifier, label, items: [{ id, name }, ...], numRows }`.
     * Caller is responsible for mapping `items` to whatever row shape the UI needs.
     */
    getUnsendBundles(
        userId: string,
        filter = '*',
        start = 0,
        count = 50
    ): Observable<UnsentBundlesResponse> {
        const query = new HttpParams()
            .set('name', filter || '*')
            .set('start', start)
            .set('count', count);

        return this.#http.get<UnsentBundlesResponse>(
            `/api/bundle/getunsendbundles/userid/${encodeURIComponent(userId)}`,
            { params: query }
        );
    }

    getBundleAssets(bundleId: string): Observable<BundleAssetView[]> {
        const params = new HttpParams().set('limit', -1);

        return this.#http.get<BundleAssetView[]>(
            `/api/bundle/${encodeURIComponent(bundleId)}/assets`,
            { params }
        );
    }

    /**
     * Removes one or more assets from an unsent bundle.
     * Backed by `BundleManagementResource#removeAssetsFromBundle`
     * (`DELETE /api/v1/bundles/{bundleId}/assets`).
     *
     * Backend returns 409 if the bundle is already in progress.
     * Per-asset results live inside `entity[]` — some may fail while others succeed.
     */
    removeAssetsFromBundle(
        bundleId: string,
        assetIds: string[]
    ): Observable<RemoveAssetResultView[]> {
        return this.#http
            .request<
                DotCMSResponse<RemoveAssetResultView[]>
            >('DELETE', `/api/v1/bundles/${encodeURIComponent(bundleId)}/assets`, { body: { assetIds } })
            .pipe(map((response) => response.entity));
    }
}

/** Pulls `filename` out of a `content-disposition` header. Mirrors the parse
 * the legacy download dialog does (`contentDisposition.slice(idx + 'filename='.length)`)
 * so generated filenames match exactly. Returns an empty string when the
 * header is missing or malformed — `getDownloadLink` will still produce a
 * usable anchor in that case. */
function parseFilenameFromContentDisposition(header: string | null): string {
    if (!header) {
        return '';
    }
    const key = 'filename=';
    const idx = header.indexOf(key);
    if (idx < 0) {
        return '';
    }
    return header.slice(idx + key.length).replace(/^"|"$/g, '');
}
