/**
 * Push-Publish audit status values.
 *
 * Mirror of `com.dotcms.publisher.business.PublishAuditStatus.Status` (Java enum
 * source of truth — see `dotCMS/src/main/java/com/dotcms/publisher/business/PublishAuditStatus.java`).
 * If the backend enum changes, update this file in lockstep.
 */
export enum PublishAuditStatus {
    BUNDLE_REQUESTED = 'BUNDLE_REQUESTED',
    BUNDLING = 'BUNDLING',
    SENDING_TO_ENDPOINTS = 'SENDING_TO_ENDPOINTS',
    FAILED_TO_SEND_TO_ALL_GROUPS = 'FAILED_TO_SEND_TO_ALL_GROUPS',
    FAILED_TO_SEND_TO_SOME_GROUPS = 'FAILED_TO_SEND_TO_SOME_GROUPS',
    FAILED_TO_BUNDLE = 'FAILED_TO_BUNDLE',
    FAILED_TO_SENT = 'FAILED_TO_SENT',
    FAILED_TO_PUBLISH = 'FAILED_TO_PUBLISH',
    SUCCESS = 'SUCCESS',
    BUNDLE_SENT_SUCCESSFULLY = 'BUNDLE_SENT_SUCCESSFULLY',
    RECEIVED_BUNDLE = 'RECEIVED_BUNDLE',
    PUBLISHING_BUNDLE = 'PUBLISHING_BUNDLE',
    WAITING_FOR_PUBLISHING = 'WAITING_FOR_PUBLISHING',
    BUNDLE_SAVED_SUCCESSFULLY = 'BUNDLE_SAVED_SUCCESSFULLY',
    INVALID_TOKEN = 'INVALID_TOKEN',
    LICENSE_REQUIRED = 'LICENSE_REQUIRED',
    SUCCESS_WITH_WARNINGS = 'SUCCESS_WITH_WARNINGS',
    FAILED_INTEGRITY_CHECK = 'FAILED_INTEGRITY_CHECK',
    /**
     * Synthetic status for bundles pushed with a future publish date but not yet
     * picked up by `PublisherQueueJob` — synthesized at read time by the v1
     * publishing API, never persisted. Mirrors the BE sentinel introduced in
     * `PublishAuditStatus.Status.SCHEDULED` (#36267).
     */
    SCHEDULED = 'SCHEDULED'
}

/** Bundles authored but not yet sent — populate the Queue tab's READY TO SEND column. */
export const READY_STATUSES: readonly PublishAuditStatus[] = [
    PublishAuditStatus.WAITING_FOR_PUBLISHING,
    PublishAuditStatus.BUNDLE_REQUESTED
] as const;

/** Bundles being packed or shipped — populate the Queue tab's IN PROGRESS column. */
export const IN_PROGRESS_STATUSES: readonly PublishAuditStatus[] = [
    PublishAuditStatus.BUNDLING,
    PublishAuditStatus.SENDING_TO_ENDPOINTS,
    PublishAuditStatus.PUBLISHING_BUNDLE,
    PublishAuditStatus.RECEIVED_BUNDLE
] as const;
