import { Observable } from 'rxjs';

import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { BundleAssetView, PublishAuditStatus, PublishingJobsResponse } from '@dotcms/dotcms-models';

export interface ListPublishingJobsParams {
    statuses: readonly PublishAuditStatus[];
    page?: number;
    perPage?: number;
    filter?: string;
}

/**
 * Backs the Publishing Queue Angular portlet. Wraps the v1 publishing endpoints
 * the new UI needs in this slice.
 *
 * Endpoints covered:
 * - `GET /api/v1/publishing` (`PublishingResource#listPublishingJobs`)
 * - `GET /api/bundle/{bundleId}/assets` (`BundleResource#getPublishQueueElements`)
 *
 * Future slices add `getPublishingJobDetails`, `pushBundle`, `retryBundles`, etc.
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

        return this.http.get<PublishingJobsResponse>('/api/v1/publishing', {
            params: httpParams
        });
    }

    getBundleAssets(bundleId: string): Observable<BundleAssetView[]> {
        const params = new HttpParams().set('limit', -1);

        return this.http.get<BundleAssetView[]>(`/api/bundle/${bundleId}/assets`, {
            params
        });
    }
}
