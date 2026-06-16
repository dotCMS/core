import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';

import { MenuItem } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { MenuModule } from 'primeng/menu';
import { PaginatorModule, PaginatorState } from 'primeng/paginator';
import { SkeletonModule } from 'primeng/skeleton';

import { PublishAuditStatus, PublishingJobView } from '@dotcms/dotcms-models';
import { DotEmptyContainerComponent, DotMessagePipe, PrincipalConfiguration } from '@dotcms/ui';

import { DotPublishingStatusChipComponent } from '../components/dot-publishing-status-chip/dot-publishing-status-chip.component';

type LoadStatus = 'init' | 'loading' | 'loaded' | 'error';
type Mode = 'ready' | 'progress';

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
    selector: 'dot-publishing-queue-list',
    standalone: true,
    imports: [
        ButtonModule,
        MenuModule,
        PaginatorModule,
        SkeletonModule,
        DotEmptyContainerComponent,
        DotMessagePipe,
        DotPublishingStatusChipComponent
    ],
    templateUrl: './dot-publishing-queue-list.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { class: 'flex flex-col min-h-0 h-full' }
})
export class DotPublishingQueueListComponent {
    readonly mode = input.required<Mode>();
    readonly rows = input.required<PublishingJobView[]>();
    readonly status = input.required<LoadStatus>();
    readonly total = input.required<number>();
    readonly page = input.required<number>();
    readonly rowsPerPage = input.required<number>();
    readonly headerKey = input.required<string>();
    readonly emptyConfig = input.required<PrincipalConfiguration>();
    /** Builder for the per-row kebab menu items. Only used in ready mode. */
    readonly kebabBuilder = input<(job: PublishingJobView) => MenuItem[] | null>(() => null);

    readonly rowClick = output<PublishingJobView>();
    readonly sendClick = output<PublishingJobView>();
    readonly retryClick = output<PublishingJobView>();
    readonly pageChange = output<number>();

    readonly first = computed(() => (this.page() - 1) * this.rowsPerPage());

    readonly skeletonRows = Array.from({ length: 5 });

    /**
     * Builds the kebab menu items once per row (keyed by bundleId) and returns
     * the same array reference across change detection cycles. PrimeNG `p-menu`
     * thrashes when `[model]` receives a brand-new array on every CD — the menu
     * re-processes the items and the first click on an item only closes the menu
     * instead of firing its `command`, forcing the user to click twice.
     */
    private readonly kebabMenus = computed(() => {
        const builder = this.kebabBuilder();
        const map = new Map<string, MenuItem[]>();
        for (const job of this.rows()) {
            const items = builder(job);
            if (items) {
                map.set(job.bundleId, items);
            }
        }
        return map;
    });

    isRetryable(status: PublishAuditStatus | null): boolean {
        return status !== null && FAILURE_STATUSES.has(status);
    }

    kebabFor(job: PublishingJobView): MenuItem[] {
        return this.kebabMenus().get(job.bundleId) ?? [];
    }

    onRowKeyDown(event: KeyboardEvent, job: PublishingJobView): void {
        if (event.key === 'Enter' || event.key === ' ') {
            event.preventDefault();
            this.rowClick.emit(job);
        }
    }

    onPaginate(event: PaginatorState): void {
        const newRows = event.rows ?? this.rowsPerPage();
        const newFirst = event.first ?? 0;
        const newPage = Math.floor(newFirst / newRows) + 1;
        this.pageChange.emit(newPage);
    }
}
