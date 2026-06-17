import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { SkeletonModule } from 'primeng/skeleton';
import { TableLazyLoadEvent, TableModule } from 'primeng/table';

import { DotMessageService, PublishingSortField } from '@dotcms/data-access';
import { PublishingJobView } from '@dotcms/dotcms-models';
import {
    DotCopyButtonComponent,
    DotEmptyContainerComponent,
    DotMessagePipe,
    PrincipalConfiguration
} from '@dotcms/ui';

import { DotPublishingStatusChipComponent } from '../components/dot-publishing-status-chip/dot-publishing-status-chip.component';
import { DotPublishingQueueStore } from '../dot-publishing-queue-page/store/dot-publishing-queue.store';

@Component({
    selector: 'dot-publishing-queue-history',
    standalone: true,
    imports: [
        DatePipe,
        ButtonModule,
        SkeletonModule,
        TableModule,
        DotCopyButtonComponent,
        DotEmptyContainerComponent,
        DotMessagePipe,
        DotPublishingStatusChipComponent
    ],
    templateUrl: './dot-publishing-queue-history.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { class: 'flex flex-col h-full min-h-0 flex-1' }
})
export class DotPublishingQueueHistoryComponent {
    readonly store = inject(DotPublishingQueueStore);
    private readonly dotMessageService = inject(DotMessageService);

    readonly first = computed(() => (this.store.historyPage() - 1) * this.store.rowsPerPage());

    /** Pass-through config so the table fills 100% height when empty/loading,
     * matching the dot-tags pattern (no rounded card, table flows edge-to-edge). */
    readonly $ptConfig = computed(() => ({
        table: {
            style: {
                'table-layout': 'fixed' as const,
                ...(this.store.historyRows().length === 0 && {
                    height: '100%',
                    width: '100%'
                })
            }
        }
    }));

    readonly historyEmpty: PrincipalConfiguration = {
        icon: 'pi-history',
        title: this.dotMessageService.get('publishing-queue.empty.history.title'),
        subtitle: this.dotMessageService.get('publishing-queue.empty.history.subtitle')
    };

    readonly selectedRows = computed(() => {
        const selectedIds = new Set(this.store.historySelectedIds());
        return this.store.historyRows().filter((row) => selectedIds.has(row.bundleId));
    });

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
}
