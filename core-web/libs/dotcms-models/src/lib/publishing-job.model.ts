import { PublishAuditStatus } from './publishing-status.model';

/**
 * Preview of an asset inside a publishing bundle (max 3 per bundle).
 * Mirrors `com.dotcms.rest.api.v1.publishing.AbstractAssetPreviewView`.
 */
export interface AssetPreviewView {
    id: string;
    title: string;
    type: string;
}

/**
 * Publishing job combining audit status and bundle metadata.
 * Mirrors `com.dotcms.rest.api.v1.publishing.AbstractPublishingJobView`.
 *
 * Returned by `GET /api/v1/publishing` as the `entity[]` of the envelope.
 */
export interface PublishingJobView {
    bundleId: string;
    bundleName: string | null;
    status: PublishAuditStatus;
    filterName: string | null;
    filterKey: string | null;
    assetCount: number;
    assetPreview: AssetPreviewView[];
    environmentCount: number;
    createDate: string;
    statusUpdated: string | null;
    numTries: number;
}

/** Pagination envelope returned alongside `entity` on `/api/v1/publishing`. */
export interface PublishingJobsPagination {
    currentPage: number;
    perPage: number;
    totalEntries: number;
}

/** Full envelope returned by `GET /api/v1/publishing`. */
export interface PublishingJobsResponse {
    entity: PublishingJobView[];
    pagination: PublishingJobsPagination;
}
