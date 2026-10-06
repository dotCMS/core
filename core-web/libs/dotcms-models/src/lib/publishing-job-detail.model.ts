import { PublishAuditStatus } from './publishing-status.model';

/**
 * One endpoint inside an environment, with its publish status.
 * Mirrors `com.dotcms.rest.api.v1.publishing.AbstractEndpointDetailView`.
 */
export interface EndpointDetailView {
    id: string;
    serverName: string;
    address: string;
    port: string;
    protocol: string;
    status: PublishAuditStatus | null;
    statusMessage: string | null;
    stackTrace: string | null;
}

/**
 * One environment with its endpoints.
 * Mirrors `com.dotcms.rest.api.v1.publishing.AbstractEnvironmentDetailView`.
 */
export interface EnvironmentDetailView {
    id: string;
    name: string;
    endpoints: EndpointDetailView[];
}

/**
 * Bundle/publish phase timestamps.
 * Mirrors `com.dotcms.rest.api.v1.publishing.AbstractTimestampsView`.
 */
export interface TimestampsView {
    bundleStart: string | null;
    bundleEnd: string | null;
    publishStart: string | null;
    publishEnd: string | null;
    createDate: string;
    statusUpdated: string | null;
}

/**
 * Full bundle detail returned by `GET /api/v1/publishing/{bundleId}`.
 * Mirrors `com.dotcms.rest.api.v1.publishing.AbstractPublishingJobDetailView`.
 */
export interface PublishingJobDetailView {
    bundleId: string;
    bundleName: string | null;
    status: PublishAuditStatus;
    filterName: string | null;
    filterKey: string | null;
    assetCount: number;
    environments: EnvironmentDetailView[];
    timestamps: TimestampsView;
    numTries: number;
    /** Future `publishDate` the bundle was pushed with — set only when status is SCHEDULED. */
    scheduledPublishDate: string | null;
}

/** Per-asset result of `DELETE /v1/bundles/{bundleId}/assets`. */
export interface RemoveAssetResultView {
    assetId: string;
    success: boolean;
    message: string;
}

/** Result of a single bundle retry. */
export interface RetryBundleResultView {
    bundleId: string;
    success: boolean;
    message: string;
    forcePush: boolean | null;
    operation: 'PUBLISH' | 'UNPUBLISH' | null;
    deliveryStrategy: 'ALL_ENDPOINTS' | 'FAILED_ENDPOINTS';
    assetCount: number | null;
}
