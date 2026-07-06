import { DatePipe } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    computed,
    inject,
    signal,
    viewChild
} from '@angular/core';

import { ConfirmationService, MenuItem } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ContextMenu, ContextMenuModule } from 'primeng/contextmenu';
import { MenuModule } from 'primeng/menu';
import { SkeletonModule } from 'primeng/skeleton';
import { TableLazyLoadEvent, TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';

/* eslint-disable @nx/enforce-module-boundaries */
// `DotDownloadBundleDialogService` lives in apps/dotcms-ui (not yet promoted to a
// shared lib). Tracked alongside the v1 consolidation work (#36048).

import { DotGlobalMessageService, DotMessageService } from '@dotcms/data-access';
import { DotPushPublishDialogService } from '@dotcms/dotcms-js';
import { PublishAuditStatus, PublishingJobView } from '@dotcms/dotcms-models';
import {
    DotClipboardUtil,
    DotEmptyContainerComponent,
    DotMessagePipe,
    DotRelativeDatePipe,
    PrincipalConfiguration
} from '@dotcms/ui';
import { DotDownloadBundleDialogService } from '@services/dot-download-bundle-dialog/dot-download-bundle-dialog.service';

import { DotPublishingStatusChipComponent } from '../components/dot-publishing-status-chip/dot-publishing-status-chip.component';
import { DotPublishingQueueStore } from '../store/dot-publishing-queue.store';

/** Standard dotCMS bundle ids are 26-char ULIDs. Some are longer (custom
 * import/sync paths). Cap the visible length so the column doesn't stretch;
 * the full id stays accessible via the `title` tooltip + the copy button. */
const BUNDLE_ID_DISPLAY_MAX = 32;

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

const ACTIVE_STATUSES = new Set<PublishAuditStatus>([
    PublishAuditStatus.BUNDLE_REQUESTED,
    PublishAuditStatus.WAITING_FOR_PUBLISHING,
    PublishAuditStatus.BUNDLING,
    PublishAuditStatus.SENDING_TO_ENDPOINTS,
    PublishAuditStatus.PUBLISHING_BUNDLE,
    PublishAuditStatus.RECEIVED_BUNDLE
]);

@Component({
    selector: 'dot-publishing-queue-table',
    imports: [
        DatePipe,
        ButtonModule,
        ConfirmDialogModule,
        ContextMenuModule,
        MenuModule,
        SkeletonModule,
        TableModule,
        TagModule,
        TooltipModule,
        DotEmptyContainerComponent,
        DotMessagePipe,
        DotPublishingStatusChipComponent,
        DotRelativeDatePipe
    ],
    providers: [ConfirmationService, DotClipboardUtil],
    templateUrl: './dot-publishing-queue-table.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { class: 'flex flex-col h-full min-h-0 flex-1' }
})
export class DotPublishingQueueTableComponent {
    protected readonly store = inject(DotPublishingQueueStore);
    private readonly dotMessageService = inject(DotMessageService);
    private readonly confirmationService = inject(ConfirmationService);
    private readonly clipboard = inject(DotClipboardUtil);
    private readonly globalMessage = inject(DotGlobalMessageService);
    private readonly dotPushPublishDialogService = inject(DotPushPublishDialogService);
    private readonly dotDownloadBundleDialogService = inject(DotDownloadBundleDialogService);

    readonly first = computed(() => (this.store.bundlesPage() - 1) * this.store.rowsPerPage());

    /** Page-size dropdown options. Mirrors `dot-folder-list-view` (content-drive)
     * for visual consistency across the admin UI. The store seeds at 20 too. */
    readonly rowsPerPageOptions = [20, 40, 60];

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
                ...(this.store.bundlesRows().length === 0
                    ? { height: '100%', width: '100%' }
                    : { width: 'auto' })
            }
        }
    }));

    readonly bundlesEmpty: PrincipalConfiguration = {
        icon: 'pi-inbox',
        title: this.dotMessageService.get('publishing-queue.empty.bundles.title'),
        subtitle: this.dotMessageService.get('publishing-queue.empty.bundles.subtitle')
    };

    readonly selectedRows = computed(() => {
        const selectedIds = new Set(this.store.bundlesSelectedIds());
        return this.store.bundlesRows().filter((row) => selectedIds.has(row.bundleId));
    });

    /** Per-row builder. Kept as a pure arrow so the spec can call it directly
     * to verify the items' shape. The template never calls this — it calls
     * `kebabFor(row)` which returns a memoized reference (see below). */
    readonly bundlesKebabFor = (row: PublishingJobView): MenuItem[] => {
        const items: MenuItem[] = [
            {
                label: this.dotMessageService.get('publishing-queue.history.kebab.view-details'),
                command: () => this.store.openDetail(row.bundleId)
            },
            {
                label: this.dotMessageService.get('publishing-queue.history.kebab.view-contents'),
                command: () => this.store.openAssetList(row.bundleId)
            }
        ];

        if (row.status && ACTIVE_STATUSES.has(row.status)) {
            items.push({
                label: this.dotMessageService.get('publishing-queue.kebab.configure-send'),
                command: () => this.openPushPublish(row)
            });
        }

        if (row.status && FAILURE_STATUSES.has(row.status)) {
            items.push({
                label: this.dotMessageService.get('publishing-queue.retry-send'),
                command: () => this.store.retryBundles({ bundleIds: [row.bundleId] })
            });
        }

        items.push({
            label: this.dotMessageService.get('publishing-queue.kebab.generate-download'),
            command: () => this.dotDownloadBundleDialogService.open(row.bundleId)
        });

        items.push({ separator: true });
        items.push({
            label: this.dotMessageService.get('publishing-queue.history.kebab.delete'),
            styleClass: 'p-menuitem-danger',
            command: () => this.confirmRemove(row)
        });

        return items;
    };

    /** Memoizes the kebab items per row so `<p-menu [model]="…">` keeps the same
     * array reference across CD cycles. Without this, PrimeNG re-processes the
     * items on every CD and the menu thrashes — the first click only closes the
     * menu instead of firing the item's `command`, forcing the user to click
     * twice. */
    private readonly kebabMenus = computed(() => {
        const map = new Map<string, MenuItem[]>();
        for (const row of this.store.bundlesRows()) {
            map.set(row.bundleId, this.bundlesKebabFor(row));
        }
        return map;
    });

    kebabFor(row: PublishingJobView): MenuItem[] {
        return this.kebabMenus().get(row.bundleId) ?? [];
    }

    /** Right-click context menu reuses the same kebab items, scoped to whichever
     * row was right-clicked. One shared `<p-contextMenu>` instead of one per row
     * keeps the DOM small even with hundreds of rows. */
    readonly contextMenu = viewChild<ContextMenu>('rowContextMenu');
    readonly contextMenuRow = signal<PublishingJobView | null>(null);
    readonly contextMenuItems = computed<MenuItem[]>(() => {
        const row = this.contextMenuRow();
        return row ? this.kebabFor(row) : [];
    });

    onRowContextMenu(event: MouseEvent, row: PublishingJobView): void {
        event.preventDefault();
        this.contextMenuRow.set(row);
        this.contextMenu()?.show(event);
    }

    /** Rows in any failure bucket render their bundle id in danger-red — same
     * `text-red-700` the status chip uses for the danger bucket. Gives an
     * at-a-glance signal even when the Status column is off-screen on narrow
     * viewports. */
    isFailedRow(row: PublishingJobView): boolean {
        return row.status ? FAILURE_STATUSES.has(row.status) : false;
    }

    onLazyLoad(event: TableLazyLoadEvent): void {
        const rows = (event.rows as number) ?? this.store.rowsPerPage();
        const first = (event.first as number) ?? 0;
        const page = Math.floor(first / rows) + 1;

        // Persist a rows-per-page change BEFORE the page change so the next
        // fetch goes out with the new size. The store's effect debounces both
        // patches into a single `loadBundles` call.
        //
        // Sort handling is intentionally NOT translated from this lazy-load
        // event: the sortable header buttons dispatch `cycleBundlesSort` on the
        // store directly (see the template's header cells), which drives the
        // `bundlesSort` / `bundlesSortDirection` state and its refetch effect.
        // The p-table lazy-load event doesn't carry that state, so relying on
        // its `sortField` here would double-fire the load.
        if (rows !== this.store.rowsPerPage()) {
            this.store.setRowsPerPage(rows);
        } else if (page !== this.store.bundlesPage()) {
            this.store.setBundlesPage(page);
        }
    }

    onSelectionChange(rows: PublishingJobView[]): void {
        this.store.setBundlesSelection(rows.map((r) => r.bundleId));
    }

    onRowClick(row: PublishingJobView): void {
        this.store.openDetail(row.bundleId);
    }

    /** Inline copy-to-clipboard for the Bundle Id column. Same approach used in
     * `dot-es-search-page`: a `<p-button>` + `DotClipboardUtil` + global error
     * toast. Lighter than wrapping `<dot-copy-button>` for the row hover-only,
     * compact icon-only style we want here. */
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

    /**
     * Opens the project-wide push publish dialog (the same one used by templates,
     * containers, content types, pages, content). Triggered via the global singleton
     * service — the dialog itself is mounted once in `main-legacy.component.html`.
     * `isBundle: true` routes the submit to the bundle endpoint instead of asset.
     */
    private openPushPublish(row: PublishingJobView): void {
        this.dotPushPublishDialogService.open({
            assetIdentifier: row.bundleId,
            title: row.bundleName || row.bundleId,
            isBundle: true
        });
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
            acceptButtonStyleClass: 'p-button-primary',
            rejectButtonStyleClass: 'p-button-text',
            defaultFocus: 'reject',
            closable: true,
            closeOnEscape: true,
            accept: () => this.store.deleteBundle(row.bundleId)
        });
    }
}
