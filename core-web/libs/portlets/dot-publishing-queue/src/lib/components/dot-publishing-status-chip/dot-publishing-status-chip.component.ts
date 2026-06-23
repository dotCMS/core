import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { ChipModule } from 'primeng/chip';

import { PublishAuditStatus } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

type StatusBucket = 'success' | 'danger' | 'warning' | 'info';

const BUCKETS: Record<PublishAuditStatus, StatusBucket> = {
    // success: bundle reached its target
    [PublishAuditStatus.SUCCESS]: 'success',
    [PublishAuditStatus.BUNDLE_SENT_SUCCESSFULLY]: 'success',
    [PublishAuditStatus.BUNDLE_SAVED_SUCCESSFULLY]: 'success',

    // warning: shipped but with non-fatal issues
    [PublishAuditStatus.SUCCESS_WITH_WARNINGS]: 'warning',

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

    // warning: actively being packed/sent (in-flight)
    [PublishAuditStatus.BUNDLING]: 'warning',
    [PublishAuditStatus.SENDING_TO_ENDPOINTS]: 'warning',
    [PublishAuditStatus.PUBLISHING_BUNDLE]: 'warning',
    [PublishAuditStatus.RECEIVED_BUNDLE]: 'warning'
};

/** Pure mapping function — exported for direct testing without component instantiation. */
export function publishingStatusBucket(status: PublishAuditStatus): StatusBucket {
    return BUCKETS[status] ?? 'info';
}

/**
 * Renders a coloured chip for a `PublishAuditStatus`, following the project standard
 * (same pattern as `dot-contentlet-status-chip`): `p-chip` with `bg-{c}-100! text-{c}-700!`
 * Tailwind classes. Centralises the 18-status → 4-bucket mapping so consumers don't
 * duplicate the severity logic.
 */
@Component({
    selector: 'dot-publishing-status-chip',
    standalone: true,
    imports: [ChipModule, DotMessagePipe],
    templateUrl: './dot-publishing-status-chip.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotPublishingStatusChipComponent {
    status = input<PublishAuditStatus | null>(null);

    readonly bucket = computed<StatusBucket | null>(() => {
        const s = this.status();
        return s ? publishingStatusBucket(s) : null;
    });

    /** Uses portlet-scoped keys (`publishing-queue.status.*`) so the chip labels
     * stay short for the new design without affecting the legacy JSPs that
     * still read `publisher_status_*`. */
    readonly labelKey = computed<string>(() => `publishing-queue.status.${this.status()}`);
}
