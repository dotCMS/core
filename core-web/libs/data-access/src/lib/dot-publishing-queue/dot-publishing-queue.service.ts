import { Observable } from 'rxjs';

import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { map } from 'rxjs/operators';

import {
    BundleAssetView,
    DotCMSResponse,
    PublishAuditStatus,
    PublishingJobDetailView,
    PublishingJobsResponse,
    RemoveAssetResultView,
    RetryBundleResultView,
    UnsentBundlesResponse
} from '@dotcms/dotcms-models';

export type PublishingSortField = 'bundle_name' | 'status' | 'created' | 'modified';
export type PublishingSortDirection = 'asc' | 'desc';

export interface ListPublishingJobsParams {
    statuses: readonly PublishAuditStatus[];
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
 * - `DELETE /api/v1/publishing/{bundleId}` — delete single bundle
 * - `DELETE /api/v1/publishing` — bulk delete by id (added by #36046)
 *
 * Legacy endpoints (`com.dotcms.rest.BundleResource`) still used until #36048 lands:
 * - `GET /api/bundle/{bundleId}/assets` — asset list inside a bundle
 * - `POST /api/bundle/sync` — synchronous .tar.gz upload (licensed)
 * - `GET /api/bundle/_download/{bundleId}` — bundle download (URL only)
 *
 * Push-to-environment flow is delegated to the project-wide push publish dialog
 * (`DotPushPublishDialogService.open(...)` from `@dotcms/dotcms-js`), which hits
 * the legacy `/DotAjaxDirector/.../cmd/pushBundle` endpoint via `PushPublishService`.
 * No bespoke push endpoint lives here.
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
    private http = inject(HttpClient);

    listPublishingJobs(params: ListPublishingJobsParams): Observable<PublishingJobsResponse> {
        let httpParams = new HttpParams().set('status', params.statuses.join(','));

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

        return this.http.get<PublishingJobsResponse>('/api/v1/publishing', {
            params: httpParams
        });
    }

    getPublishingJobDetails(bundleId: string): Observable<PublishingJobDetailView> {
        return this.http
            .get<DotCMSResponse<PublishingJobDetailView>>(`/api/v1/publishing/${bundleId}`)
            .pipe(map((response) => response.entity));
    }

    retryBundles(payload: RetryBundlesPayload): Observable<RetryBundleResultView[]> {
        return this.http
            .post<DotCMSResponse<RetryBundleResultView[]>>('/api/v1/publishing/retry', payload)
            .pipe(map((response) => response.entity));
    }

    deleteBundle(bundleId: string): Observable<{ message: string }> {
        return this.http.delete<{ message: string }>(`/api/v1/publishing/${bundleId}`);
    }

    /**
     * Bulk delete bundles by id (BE endpoint added in #36046). Falls back to per-id loops
     * on the consumer side if the endpoint returns 404.
     */
    deleteBundles(bundleIds: string[]): Observable<{ message: string; deleted: string[] }> {
        return this.http.request<{ message: string; deleted: string[] }>(
            'DELETE',
            '/api/v1/publishing',
            { body: { bundleIds } }
        );
    }

    uploadBundle(file: File): Observable<{ bundleName: string; status: string }> {
        const formData = new FormData();
        formData.append('file', file, file.name);
        return this.http.post<{ bundleName: string; status: string }>('/api/bundle/sync', formData);
    }

    /** Builds the absolute download URL for a bundle's `.tar.gz`. */
    getBundleDownloadUrl(bundleId: string): string {
        return `/api/bundle/_download/${bundleId}`;
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

        return this.http.get<UnsentBundlesResponse>(
            `/api/bundle/getunsendbundles/userid/${userId}`,
            { params: query }
        );
    }

    getBundleAssets(bundleId: string): Observable<BundleAssetView[]> {
        const params = new HttpParams().set('limit', -1);

        return this.http.get<BundleAssetView[]>(`/api/bundle/${bundleId}/assets`, {
            params
        });
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
        return this.http
            .request<
                DotCMSResponse<RemoveAssetResultView[]>
            >('DELETE', `/api/v1/bundles/${bundleId}/assets`, { body: { assetIds } })
            .pipe(map((response) => response.entity));
    }
}
