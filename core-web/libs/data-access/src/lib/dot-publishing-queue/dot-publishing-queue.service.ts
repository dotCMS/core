import { Observable } from 'rxjs';

import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { map } from 'rxjs/operators';

import {
    BundleAssetView,
    DotCMSResponse,
    DotEnvironment,
    PublishAuditStatus,
    PublishingJobDetailView,
    PublishingJobsResponse,
    PushBundleResultView,
    RetryBundleResultView
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

export type PushOperation = 'publish' | 'expire' | 'publishexpire';
export type RetryDeliveryStrategy = 'ALL_ENDPOINTS' | 'FAILED_ENDPOINTS';

export interface PushBundlePayload {
    operation: PushOperation;
    publishDate?: string;
    expireDate?: string;
    environments: string[];
    filterKey: string;
}

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
 * - `POST /api/v1/publishing/push/{bundleId}` — push bundle to environments
 * - `POST /api/v1/publishing/retry` — retry bundles (bulk)
 * - `DELETE /api/v1/publishing/{bundleId}` — delete single bundle
 * - `DELETE /api/v1/publishing` — bulk delete by id (added by #36046)
 *
 * Legacy endpoints (`com.dotcms.rest.BundleResource`) still used until #36048 lands:
 * - `GET /api/bundle/{bundleId}/assets` — asset list inside a bundle
 * - `POST /api/bundle/_generate` — async tar.gz generation
 * - `POST /api/bundle/sync` — synchronous .tar.gz upload (licensed)
 * - `GET /api/bundle/_download/{bundleId}` — bundle download (URL only)
 *
 * Environment list comes from `EnvironmentResource` (`GET /api/environment`).
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

    pushBundle(bundleId: string, payload: PushBundlePayload): Observable<PushBundleResultView> {
        return this.http
            .post<DotCMSResponse<PushBundleResultView>>(
                `/api/v1/publishing/push/${bundleId}`,
                payload
            )
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

    generateBundle(
        bundleId: string,
        filterKey: string,
        operation: PushOperation = 'publish'
    ): Observable<unknown> {
        return this.http.post('/api/bundle/_generate', { bundleId, filterKey, operation });
    }

    uploadBundle(file: File): Observable<{ bundleName: string; status: string }> {
        const formData = new FormData();
        formData.append('file', file, file.name);
        return this.http.post<{ bundleName: string; status: string }>(
            '/api/bundle/sync',
            formData
        );
    }

    /** Builds the absolute download URL for a bundle's `.tar.gz`. */
    getBundleDownloadUrl(bundleId: string): string {
        return `/api/bundle/_download/${bundleId}`;
    }

    getBundleAssets(bundleId: string): Observable<BundleAssetView[]> {
        const params = new HttpParams().set('limit', -1);

        return this.http.get<BundleAssetView[]>(`/api/bundle/${bundleId}/assets`, {
            params
        });
    }

    /**
     * Lists Push-Publish environments visible to the current user.
     * Backed by `EnvironmentResource#loadAllEnvironments` (role-filtered for non-admins).
     */
    getEnvironments(): Observable<DotEnvironment[]> {
        return this.http
            .get<DotCMSResponse<DotEnvironment[]>>('/api/environment')
            .pipe(map((response) => response.entity));
    }
}
