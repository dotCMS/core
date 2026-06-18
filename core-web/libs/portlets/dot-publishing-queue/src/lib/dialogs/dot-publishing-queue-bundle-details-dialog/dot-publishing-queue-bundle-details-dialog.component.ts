import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { SkeletonModule } from 'primeng/skeleton';
import { TableModule } from 'primeng/table';

import { DotPublishingQueueService } from '@dotcms/data-access';
import { EndpointDetailView, PublishAuditStatus } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DotPublishingStatusChipComponent } from '../../components/dot-publishing-status-chip/dot-publishing-status-chip.component';
import { DotPublishingQueueStore } from '../../dot-publishing-queue-page/store/dot-publishing-queue.store';

const SUCCESS_STATUSES = new Set<PublishAuditStatus>([
    PublishAuditStatus.SUCCESS,
    PublishAuditStatus.BUNDLE_SENT_SUCCESSFULLY,
    PublishAuditStatus.BUNDLE_SAVED_SUCCESSFULLY,
    PublishAuditStatus.SUCCESS_WITH_WARNINGS
]);

/** Flattened row used by the endpoints table — each endpoint carries its
 * environment name as a column, so all groups share one uniform grid. */
export interface EndpointTableRow {
    key: string;
    envName: string;
    endpoint: EndpointDetailView;
}

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

    /** Flattens environments → one row per endpoint, with the env name carried
     * as a column. Single table, no subheader rows. */
    readonly endpointRows = computed<EndpointTableRow[]>(() => {
        const detail = this.store.detail();
        if (!detail) {
            return [];
        }
        const rows: EndpointTableRow[] = [];
        for (const env of detail.environments) {
            for (const endpoint of env.endpoints) {
                rows.push({
                    key: `${env.id}-${endpoint.id}`,
                    envName: env.name,
                    endpoint
                });
            }
        }
        return rows;
    });

    /** Builds the endpoint URL, omitting protocol/port when they're blank.
     * Returns null when the address itself is empty — the cell renders "—"
     * rather than the malformed `://:` the JSP would show. */
    endpointAddress(endpoint: EndpointDetailView): string | null {
        const address = (endpoint.address ?? '').trim();
        if (!address) {
            return null;
        }
        const protocol = (endpoint.protocol ?? '').trim();
        const port = (endpoint.port ?? '').trim();
        const prefix = protocol ? `${protocol}://` : '';
        const suffix = port ? `:${port}` : '';
        return `${prefix}${address}${suffix}`;
    }

    downloadHref(bundleId: string): string {
        return this.publishingService.getBundleDownloadUrl(bundleId);
    }
}
