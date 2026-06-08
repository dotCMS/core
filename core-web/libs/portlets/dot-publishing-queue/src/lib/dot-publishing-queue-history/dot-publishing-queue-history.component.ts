import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';

import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { SkeletonModule } from 'primeng/skeleton';
import { TableLazyLoadEvent, TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';

import { DotMessageService, PublishingSortField } from '@dotcms/data-access';
import { PublishAuditStatus, PublishingJobView } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DotPublishingQueueStore } from '../dot-publishing-queue-page/store/dot-publishing-queue.store';

type ChipSeverity = 'success' | 'info' | 'warn' | 'danger' | 'secondary';

const SUCCESS_STATUSES = new Set<PublishAuditStatus>([
    PublishAuditStatus.SUCCESS,
    PublishAuditStatus.BUNDLE_SENT_SUCCESSFULLY,
    PublishAuditStatus.BUNDLE_SAVED_SUCCESSFULLY,
    PublishAuditStatus.SUCCESS_WITH_WARNINGS
]);

@Component({
    selector: 'dot-publishing-queue-history',
    standalone: true,
    imports: [
        ButtonModule,
        ConfirmDialogModule,
        SkeletonModule,
        TableModule,
        TagModule,
        DotMessagePipe
    ],
    providers: [ConfirmationService],
    templateUrl: './dot-publishing-queue-history.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { class: 'flex flex-col h-full min-h-0 flex-1' }
})
export class DotPublishingQueueHistoryComponent {
    readonly store = inject(DotPublishingQueueStore);
    private readonly confirmationService = inject(ConfirmationService);
    private readonly dotMessageService = inject(DotMessageService);

    readonly first = computed(() => (this.store.historyPage() - 1) * this.store.rowsPerPage());

    readonly selectedRows = computed(() => {
        const selectedIds = new Set(this.store.historySelectedIds());
        return this.store.historyRows().filter((row) => selectedIds.has(row.bundleId));
    });

    readonly hasSelection = computed(() => this.store.historySelectedIds().length > 0);

    statusSeverity(status: PublishAuditStatus): ChipSeverity {
        return SUCCESS_STATUSES.has(status) ? 'success' : 'danger';
    }

    statusLabelKey(status: PublishAuditStatus): string {
        return `publishing-queue.status.${status}`;
    }

    onLazyLoad(event: TableLazyLoadEvent): void {
        const rows = (event.rows as number) ?? this.store.rowsPerPage();
        const first = (event.first as number) ?? 0;
        const page = Math.floor(first / rows) + 1;
        if (page !== this.store.historyPage()) {
            this.store.setHistoryPage(page);
        }

        if (event.sortField) {
            const field = (
                Array.isArray(event.sortField) ? event.sortField[0] : event.sortField
            ) as PublishingSortField;
            if (
                field !== this.store.historySort() ||
                (event.sortOrder === 1 ? 'asc' : 'desc') !== this.store.historySortDirection()
            ) {
                this.store.cycleHistorySort(field);
            }
        }
    }

    onSelectionChange(rows: PublishingJobView[]): void {
        this.store.setHistorySelection(rows.map((r) => r.bundleId));
    }

    onRowClick(row: PublishingJobView): void {
        this.store.openDetail(row.bundleId);
    }

    onBulkRetry(): void {
        this.store.retryBundles({ bundleIds: this.store.historySelectedIds() });
    }

    onBulkRemove(): void {
        const count = this.store.historySelectedIds().length;
        this.confirmationService.confirm({
            header: this.dotMessageService.get('publishing-queue.history.bulk-remove.header'),
            message: this.dotMessageService.get(
                'publishing-queue.history.bulk-remove.message',
                `${count}`
            ),
            acceptLabel: this.dotMessageService.get('publishing-queue.remove'),
            rejectLabel: this.dotMessageService.get('publishing-queue.cancel'),
            acceptButtonStyleClass: 'p-button-danger',
            rejectButtonStyleClass: 'p-button-text',
            defaultFocus: 'reject',
            closable: true,
            closeOnEscape: true,
            accept: () => this.store.deleteBundlesBulk(this.store.historySelectedIds())
        });
    }
}
