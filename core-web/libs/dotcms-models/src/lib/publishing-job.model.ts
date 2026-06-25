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
 *
 * Note: `status` is null when the row represents an unsent draft bundle
 * (sourced from `GET /api/bundle/getunsendbundles/userid/{userId}`) — drafts
 * live in `publishing_bundle` only and don't have a `publish_audit` entry yet.
 * Backend `AbstractPublishingJobView` always sets it; FE-side this is widened
 * to `| null` to reuse the same row type for both sources.
 */
export interface PublishingJobView {
    bundleId: string;
    bundleName: string | null;
    status: PublishAuditStatus | null;
    filterName: string | null;
    filterKey: string | null;
    assetCount: number;
    assetPreview: AssetPreviewView[];
    environmentCount: number;
    createDate: string;
    statusUpdated: string | null;
    numTries: number;
}

/** Raw legacy `getunsendbundles` response payload. */
export interface UnsentBundlesResponse {
    identifier: string;
    label: string;
    items: { id: string; name: string }[];
    numRows: number;
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

/** Operation values accepted by the push-bundle endpoint. Same vocabulary as
 * the legacy push-publish form's `pushActionSelected`. */
export type PushBundleOperation = 'publish' | 'expire' | 'publishexpire';

/**
 * Request body for `POST /api/v1/publishing/push/{bundleId}`.
 * Mirrors `com.dotcms.rest.api.v1.publishing.PushBundleForm`.
 *
 * Dates must be ISO 8601 with timezone offset (e.g. `2025-03-15T14:30:00-05:00`).
 * `publishDate` is required for `publish` / `publishexpire`; `expireDate` is
 * required for `expire` / `publishexpire` — the BE enforces this.
 */
export interface PushBundleForm {
    operation: PushBundleOperation;
    publishDate?: string;
    expireDate?: string;
    environments: string[];
    filterKey: string;
}

/**
 * Response payload from `POST /api/v1/publishing/push/{bundleId}`.
 * Mirrors `com.dotcms.rest.api.v1.publishing.AbstractPushBundleResultView`.
 */
export interface PushBundleResultView {
    bundleId: string;
    operation: PushBundleOperation;
    publishDate: string | null;
    expireDate: string | null;
    environments: string[];
    filterKey: string;
}
