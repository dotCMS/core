import { Component, computed, input } from '@angular/core';

import { TagModule } from 'primeng/tag';

import { PublishAuditStatus } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

/** `warn` (not `warning`) is PrimeNG's spelling — any other value produces no
 * `p-tag-*` class and the tag falls back to the solid primary fill. */
type StatusBucket = 'success' | 'danger' | 'warn' | 'info';

const BUCKETS: Record<PublishAuditStatus, StatusBucket> = {
    // success: bundle reached its target
    [PublishAuditStatus.SUCCESS]: 'success',
    [PublishAuditStatus.BUNDLE_SAVED_SUCCESSFULLY]: 'success',

    // warn: shipped but with non-fatal issues
    [PublishAuditStatus.SUCCESS_WITH_WARNINGS]: 'warn',

    // danger: anything that failed
    [PublishAuditStatus.FAILED_TO_SEND_TO_ALL_GROUPS]: 'danger',
    [PublishAuditStatus.FAILED_TO_SEND_TO_SOME_GROUPS]: 'danger',
    [PublishAuditStatus.FAILED_TO_BUNDLE]: 'danger',
    [PublishAuditStatus.FAILED_TO_SENT]: 'danger',
    [PublishAuditStatus.FAILED_TO_PUBLISH]: 'danger',
    [PublishAuditStatus.FAILED_INTEGRITY_CHECK]: 'danger',
    [PublishAuditStatus.INVALID_TOKEN]: 'danger',
    [PublishAuditStatus.LICENSE_REQUIRED]: 'danger',

    // info: in the queue, waiting to start
    [PublishAuditStatus.WAITING_FOR_PUBLISHING]: 'info',
    [PublishAuditStatus.BUNDLE_REQUESTED]: 'info',
    // info: future-dated bundle, not yet picked up by the publisher job
    [PublishAuditStatus.SCHEDULED]: 'info',

    // warn: actively being packed/sent (in-flight)
    [PublishAuditStatus.BUNDLING]: 'warn',
    [PublishAuditStatus.SENDING_TO_ENDPOINTS]: 'warn',
    // warn: uploaded to every endpoint, but not installed on the receivers yet
    [PublishAuditStatus.BUNDLE_SENT_SUCCESSFULLY]: 'warn',
    [PublishAuditStatus.PUBLISHING_BUNDLE]: 'warn',
    [PublishAuditStatus.RECEIVED_BUNDLE]: 'warn'
};

/** Pseudo-status of an unsent bundle. It has no `publish_audit` row, so it has
 * no `PublishAuditStatus`; the table passes this value instead. */
export const NEW_BUNDLE_STATUS = 'NEW';

export type PublishingRowStatus = PublishAuditStatus | typeof NEW_BUNDLE_STATUS;

/** Pure mapping function — exported for direct testing without component instantiation. */
export function publishingStatusBucket(status: PublishAuditStatus): StatusBucket {
    return BUCKETS[status] ?? 'info';
}

/**
 * Renders a coloured status tag for a `PublishAuditStatus`. Uses `p-tag` with a
 * `severity` (per STYLING_STANDARDS: never use `p-chip` for read-only status);
 * colour comes from the app theme's tag preset. Centralises the 18-status →
 * 4-bucket mapping so consumers don't duplicate the severity logic.
 */
@Component({
    selector: 'dot-publishing-status-chip',
    imports: [TagModule, DotMessagePipe],
    templateUrl: './dot-publishing-status-chip.component.html'
})
export class DotPublishingStatusChipComponent {
    readonly $status = input<PublishingRowStatus | null>(null, { alias: 'status' });

    /** "New" uses a primary tint (no PrimeNG severity has it), set in the template. */
    readonly $isNew = computed(() => this.$status() === NEW_BUNDLE_STATUS);

    readonly $bucket = computed<StatusBucket | null>(() => {
        const s = this.$status();
        return s && s !== NEW_BUNDLE_STATUS ? publishingStatusBucket(s) : null;
    });

    /** Uses portlet-scoped keys (`publishing-queue.status.*`) so the chip labels
     * stay short for the new design without affecting the legacy JSPs that
     * still read `publisher_status_*`. */
    readonly $labelKey = computed<string>(() => `publishing-queue.status.${this.$status()}`);
}
