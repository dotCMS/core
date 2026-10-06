/**
 * Push-Publish audit status values.
 *
 * Mirror of `com.dotcms.publisher.business.PublishAuditStatus.Status` (Java enum
 * source of truth — see `dotCMS/src/main/java/com/dotcms/publisher/business/PublishAuditStatus.java`).
 * If the backend enum changes, update this file in lockstep.
 */
export const PublishAuditStatus = {
    BUNDLE_REQUESTED: 'BUNDLE_REQUESTED',
    BUNDLING: 'BUNDLING',
    SENDING_TO_ENDPOINTS: 'SENDING_TO_ENDPOINTS',
    FAILED_TO_SEND_TO_ALL_GROUPS: 'FAILED_TO_SEND_TO_ALL_GROUPS',
    FAILED_TO_SEND_TO_SOME_GROUPS: 'FAILED_TO_SEND_TO_SOME_GROUPS',
    FAILED_TO_BUNDLE: 'FAILED_TO_BUNDLE',
    FAILED_TO_SENT: 'FAILED_TO_SENT',
    FAILED_TO_PUBLISH: 'FAILED_TO_PUBLISH',
    SUCCESS: 'SUCCESS',
    BUNDLE_SENT_SUCCESSFULLY: 'BUNDLE_SENT_SUCCESSFULLY',
    RECEIVED_BUNDLE: 'RECEIVED_BUNDLE',
    PUBLISHING_BUNDLE: 'PUBLISHING_BUNDLE',
    WAITING_FOR_PUBLISHING: 'WAITING_FOR_PUBLISHING',
    BUNDLE_SAVED_SUCCESSFULLY: 'BUNDLE_SAVED_SUCCESSFULLY',
    INVALID_TOKEN: 'INVALID_TOKEN',
    LICENSE_REQUIRED: 'LICENSE_REQUIRED',
    SUCCESS_WITH_WARNINGS: 'SUCCESS_WITH_WARNINGS',
    FAILED_INTEGRITY_CHECK: 'FAILED_INTEGRITY_CHECK',
    /**
     * Synthetic status for bundles pushed with a future publish date but not yet
     * picked up by `PublisherQueueJob` — synthesized at read time by the v1
     * publishing API, never persisted. Mirrors the BE sentinel introduced in
     * `PublishAuditStatus.Status.SCHEDULED` (#36267).
     */
    SCHEDULED: 'SCHEDULED'
} as const;

export type PublishAuditStatus = (typeof PublishAuditStatus)[keyof typeof PublishAuditStatus];

/**
 * Editable pre-flight statuses — bundles authored and queued by the publisher
 * job but not yet handed to a sender thread. Anything in this set can still be
 * cancelled without an in-flight interruption.
 */
export const READY_STATUSES: readonly PublishAuditStatus[] = [
    PublishAuditStatus.BUNDLE_REQUESTED
] as const;

/**
 * In-motion statuses — bundles being packed, sent, or applied at the receiver.
 * Once here, cancellation is best-effort and may leave a partially-shipped
 * archive on one or more endpoints.
 *
 * Defined by cancellability only. It deliberately differs from the Publishing
 * Queue status filter's "In progress" option, which owns its own list, and it is
 * unrelated to the backend's `PublishingJobsHelper.IN_PROGRESS_STATUSES` (the
 * statuses a bundle cannot be deleted in).
 */
export const IN_PROGRESS_STATUSES: readonly PublishAuditStatus[] = [
    PublishAuditStatus.WAITING_FOR_PUBLISHING,
    PublishAuditStatus.BUNDLING,
    PublishAuditStatus.SENDING_TO_ENDPOINTS,
    PublishAuditStatus.PUBLISHING_BUNDLE,
    PublishAuditStatus.RECEIVED_BUNDLE
] as const;

/**
 * Statuses that are never stored as a bundle's own status, so filtering the
 * bundle list by them always returns nothing.
 *
 * `LICENSE_REQUIRED`, `INVALID_TOKEN` and `FAILED_TO_SENT` are only recorded on
 * a single endpoint's entry in the bundle's audit history (the bundle itself
 * ends up as `FAILED_TO_SEND_TO_ALL_GROUPS` / `FAILED_TO_SEND_TO_SOME_GROUPS`).
 * `FAILED_INTEGRITY_CHECK` is not written by the backend at all. They stay in
 * `PublishAuditStatus` because the bundle detail view renders them per endpoint.
 */
export const ENDPOINT_ONLY_STATUSES: readonly PublishAuditStatus[] = [
    PublishAuditStatus.LICENSE_REQUIRED,
    PublishAuditStatus.INVALID_TOKEN,
    PublishAuditStatus.FAILED_TO_SENT,
    PublishAuditStatus.FAILED_INTEGRITY_CHECK
] as const;
