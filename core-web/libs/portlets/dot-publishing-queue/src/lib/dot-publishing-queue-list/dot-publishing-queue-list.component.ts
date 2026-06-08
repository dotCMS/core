import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { PaginatorModule, PaginatorState } from 'primeng/paginator';
import { SkeletonModule } from 'primeng/skeleton';
import { TagModule } from 'primeng/tag';

import { PublishAuditStatus, PublishingJobView } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

type LoadStatus = 'init' | 'loading' | 'loaded' | 'error';
type Mode = 'ready' | 'progress';
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

const READY_STATUSES_SET = new Set<PublishAuditStatus>([
    PublishAuditStatus.BUNDLE_REQUESTED,
    PublishAuditStatus.WAITING_FOR_PUBLISHING
]);

@Component({
    selector: 'dot-publishing-queue-list',
    standalone: true,
    imports: [ButtonModule, PaginatorModule, SkeletonModule, TagModule, DotMessagePipe],
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
    readonly emptyKey = input.required<string>();

    readonly rowClick = output<PublishingJobView>();
    readonly pageChange = output<number>();

    readonly first = computed(() => (this.page() - 1) * this.rowsPerPage());

    readonly skeletonRows = Array.from({ length: 5 });

    statusSeverity(status: PublishAuditStatus): ChipSeverity {
        if (SUCCESS_STATUSES.has(status)) {
            return 'success';
        }
        if (FAILURE_STATUSES.has(status)) {
            return 'danger';
        }
        if (READY_STATUSES_SET.has(status)) {
            return 'info';
        }
        return 'warn';
    }

    statusLabelKey(status: PublishAuditStatus): string {
        return `publishing-queue.status.${status}`;
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
