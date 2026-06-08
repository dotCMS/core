import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { SkeletonModule } from 'primeng/skeleton';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';

import { DotPublishingQueueService } from '@dotcms/data-access';
import { PublishAuditStatus } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DotPublishingQueueStore } from '../../dot-publishing-queue-page/store/dot-publishing-queue.store';

type ChipSeverity = 'success' | 'info' | 'warn' | 'danger' | 'secondary';

const SUCCESS_STATUSES = new Set<PublishAuditStatus>([
    PublishAuditStatus.SUCCESS,
    PublishAuditStatus.BUNDLE_SENT_SUCCESSFULLY,
    PublishAuditStatus.BUNDLE_SAVED_SUCCESSFULLY,
    PublishAuditStatus.SUCCESS_WITH_WARNINGS
]);

const FAILURE_STATUSES = new Set<PublishAuditStatus>([
    PublishAuditStatus.FAILED_TO_SEND_TO_ALL_GROUPS,
    PublishAuditStatus.FAILED_TO_SEND_TO_SOME_GROUPS,
    PublishAuditStatus.FAILED_TO_BUNDLE,
    PublishAuditStatus.FAILED_TO_SENT,
    PublishAuditStatus.FAILED_TO_PUBLISH,
    PublishAuditStatus.FAILED_INTEGRITY_CHECK,
    PublishAuditStatus.INVALID_TOKEN,
    PublishAuditStatus.LICENSE_REQUIRED
]);

@Component({
    selector: 'dot-publishing-queue-bundle-details-dialog',
    standalone: true,
    imports: [DatePipe, ButtonModule, SkeletonModule, TableModule, TagModule, DotMessagePipe],
    templateUrl: './dot-publishing-queue-bundle-details-dialog.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotPublishingQueueBundleDetailsDialogComponent {
    readonly store = inject(DotPublishingQueueStore);

    private readonly publishingService = inject(DotPublishingQueueService);

    readonly canDownload = computed(() => {
        const status = this.store.detail()?.status;
        return status ? SUCCESS_STATUSES.has(status) : false;
    });

    downloadHref(bundleId: string): string {
        return this.publishingService.getBundleDownloadUrl(bundleId);
    }

    statusLabelKey(status: PublishAuditStatus): string {
        return `publishing-queue.status.${status}`;
    }

    statusSeverity(status: PublishAuditStatus | null): ChipSeverity {
        if (!status) {
            return 'secondary';
        }
        if (SUCCESS_STATUSES.has(status)) {
            return 'success';
        }
        if (FAILURE_STATUSES.has(status)) {
            return 'danger';
        }
        return 'info';
    }
}
