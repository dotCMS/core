import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { SkeletonModule } from 'primeng/skeleton';
import { TableModule } from 'primeng/table';

import { DotPublishingQueueService } from '@dotcms/data-access';
import { PublishAuditStatus } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DotPublishingStatusChipComponent } from '../../components/dot-publishing-status-chip/dot-publishing-status-chip.component';
import { DotPublishingQueueStore } from '../../dot-publishing-queue-page/store/dot-publishing-queue.store';

const SUCCESS_STATUSES = new Set<PublishAuditStatus>([
    PublishAuditStatus.SUCCESS,
    PublishAuditStatus.BUNDLE_SENT_SUCCESSFULLY,
    PublishAuditStatus.BUNDLE_SAVED_SUCCESSFULLY,
    PublishAuditStatus.SUCCESS_WITH_WARNINGS
]);

@Component({
    selector: 'dot-publishing-queue-bundle-details-dialog',
    standalone: true,
    imports: [
        DatePipe,
        ButtonModule,
        SkeletonModule,
        TableModule,
        DotMessagePipe,
        DotPublishingStatusChipComponent
    ],
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
}
