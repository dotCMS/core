import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';

import { ConfirmationService, MenuItem } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { MenuModule } from 'primeng/menu';
import { SkeletonModule } from 'primeng/skeleton';
import { TableLazyLoadEvent, TableModule } from 'primeng/table';
import { TooltipModule } from 'primeng/tooltip';

import {
    DotGlobalMessageService,
    DotMessageService,
    PublishingSortField
} from '@dotcms/data-access';
import { PublishingJobView } from '@dotcms/dotcms-models';
import {
    DotClipboardUtil,
    DotEmptyContainerComponent,
    DotMessagePipe,
    PrincipalConfiguration
} from '@dotcms/ui';

import { DotPublishingStatusChipComponent } from '../components/dot-publishing-status-chip/dot-publishing-status-chip.component';
import { DotPublishingQueueStore } from '../dot-publishing-queue-page/store/dot-publishing-queue.store';

/** Standard dotCMS bundle ids are 26-char ULIDs. Some are longer (custom
 * import/sync paths). Cap the visible length so the column doesn't stretch;
 * the full id stays accessible via the `title` tooltip + the copy button. */
const BUNDLE_ID_DISPLAY_MAX = 32;

@Component({
    selector: 'dot-publishing-queue-history',
    standalone: true,
    imports: [
        DatePipe,
        ButtonModule,
        ConfirmDialogModule,
        MenuModule,
        SkeletonModule,
        TableModule,
        TooltipModule,
        DotEmptyContainerComponent,
        DotMessagePipe,
        DotPublishingStatusChipComponent
    ],
    providers: [ConfirmationService, DotClipboardUtil],
    templateUrl: './dot-publishing-queue-history.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { class: 'flex flex-col h-full min-h-0 flex-1' }
})
export class DotPublishingQueueHistoryComponent {
    readonly store = inject(DotPublishingQueueStore);
    private readonly dotMessageService = inject(DotMessageService);
    private readonly confirmationService = inject(ConfirmationService);
    private readonly clipboard = inject(DotClipboardUtil);
    private readonly globalMessage = inject(DotGlobalMessageService);

    readonly first = computed(() => (this.store.historyPage() - 1) * this.store.rowsPerPage());

    /** Pass-through config:
     * - `table-layout: fixed` so each `<th style="width:…">` is honored exactly.
     * - When there are rows: `width: auto` so the table sits at the natural sum
     *   of column widths. Without this, PrimeNG's default `width: 100%` makes
     *   the browser distribute leftover container space across the fixed
     *   columns — that's what was leaving empty space on the right of Bundle Id
     *   and Status while squeezing Filter.
     * - When empty/loading: fill the container so the empty placeholder/skeleton
     *   takes the full area. */
    readonly $ptConfig = computed(() => ({
        table: {
            style: {
                'table-layout': 'fixed' as const,
                ...(this.store.historyRows().length === 0
                    ? { height: '100%', width: '100%' }
                    : { width: 'auto' })
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

    /** Per-row builder. Kept as a pure arrow so the spec can call it directly
     * to verify the items' shape. The template never calls this — it calls
     * `kebabFor(row)` which returns a memoized reference (see below). */
    readonly historyKebabFor = (row: PublishingJobView): MenuItem[] => [
        {
            label: this.dotMessageService.get('publishing-queue.history.kebab.view-details'),
            command: () => this.store.openDetail(row.bundleId)
        },
        {
            label: this.dotMessageService.get('publishing-queue.history.kebab.view-contents'),
            command: () => this.store.openAssetList(row.bundleId)
        },
        { separator: true },
        {
            label: this.dotMessageService.get('publishing-queue.history.kebab.delete'),
            styleClass: 'p-menuitem-danger',
            command: () => this.confirmRemove(row)
        }
    ];

    /** Memoizes the kebab items per row so `<p-menu [model]="…">` keeps the same
     * array reference across CD cycles. Without this, PrimeNG re-processes the
     * items on every CD and the menu thrashes — the first click only closes the
     * menu instead of firing the item's `command`, forcing the user to click
     * twice. Mirrors the fix already applied in `dot-publishing-queue-list`. */
    private readonly kebabMenus = computed(() => {
        const map = new Map<string, MenuItem[]>();
        for (const row of this.store.historyRows()) {
            map.set(row.bundleId, this.historyKebabFor(row));
        }
        return map;
    });

    kebabFor(row: PublishingJobView): MenuItem[] {
        return this.kebabMenus().get(row.bundleId) ?? [];
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

    /** Inline copy-to-clipboard for the Bundle Id column — same approach as
     * `dot-es-search-page` (an `<p-button>` + `DotClipboardUtil` + global error
     * toast). Avoids the heavier `<dot-copy-button>` wrapper which doesn't fit
     * the row hover-only + compact icon-only style we want here. */
    async copyToClipboard(value: string): Promise<void> {
        const ok = await this.clipboard.copy(value);
        if (!ok) {
            this.globalMessage.error();
        }
    }

    truncateBundleId(bundleId: string): string {
        if (!bundleId || bundleId.length <= BUNDLE_ID_DISPLAY_MAX) {
            return bundleId;
        }
        return `${bundleId.slice(0, BUNDLE_ID_DISPLAY_MAX)}…`;
    }

    private confirmRemove(row: PublishingJobView): void {
        this.confirmationService.confirm({
            header: this.dotMessageService.get('publishing-queue.delete.confirm.header'),
            message: this.dotMessageService.get(
                'publishing-queue.delete.confirm.message',
                row.bundleName || row.bundleId
            ),
            acceptLabel: this.dotMessageService.get('publishing-queue.history.kebab.delete'),
            rejectLabel: this.dotMessageService.get('publishing-queue.cancel'),
            // Delete = primary, Cancel = tertiary (text). Destructive intent
            // is communicated by the message text, not by color.
            acceptButtonStyleClass: 'p-button-primary',
            rejectButtonStyleClass: 'p-button-text',
            defaultFocus: 'reject',
            closable: true,
            closeOnEscape: true,
            accept: () => this.store.deleteBundle(row.bundleId)
        });
    }
}
