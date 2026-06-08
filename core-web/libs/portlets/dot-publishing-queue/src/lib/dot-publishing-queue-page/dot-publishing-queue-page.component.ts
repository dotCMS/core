import { ChangeDetectionStrategy, Component, inject } from '@angular/core';

import { ConfirmationService, MenuItem } from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { MenuModule } from 'primeng/menu';

import { DotMessageService } from '@dotcms/data-access';
import { PublishingJobView } from '@dotcms/dotcms-models';

import { DotPublishingQueueStore } from './store/dot-publishing-queue.store';

import { DotPublishingQueueListComponent } from '../dot-publishing-queue-list/dot-publishing-queue-list.component';

@Component({
    selector: 'dot-publishing-queue-page',
    standalone: true,
    imports: [ConfirmDialogModule, MenuModule, DotPublishingQueueListComponent],
    providers: [ConfirmationService],
    templateUrl: './dot-publishing-queue-page.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { class: 'flex min-h-0 flex-1' }
})
export class DotPublishingQueuePageComponent {
    readonly store = inject(DotPublishingQueueStore);

    private readonly confirmationService = inject(ConfirmationService);
    private readonly dotMessageService = inject(DotMessageService);

    readyKebabFor(job: PublishingJobView): MenuItem[] {
        return [
            {
                label: this.dotMessageService.get('publishing-queue.kebab.configure-send'),
                icon: 'pi pi-send',
                command: () => this.store.openConfigureSend(job)
            },
            {
                label: this.dotMessageService.get('publishing-queue.kebab.generate-download'),
                icon: 'pi pi-download',
                command: () =>
                    this.store.generateBundle(job.bundleId, job.filterKey || 'ForcePush.yml')
            },
            { separator: true },
            {
                label: this.dotMessageService.get('publishing-queue.kebab.remove'),
                icon: 'pi pi-trash',
                styleClass: 'p-menuitem-danger',
                command: () => this.confirmRemove(job)
            }
        ];
    }

    onRowClick(row: PublishingJobView, mode: 'ready' | 'progress'): void {
        if (mode === 'ready') {
            this.store.openAssetList(row.bundleId);
        } else {
            this.store.openDetail(row.bundleId);
        }
    }

    onSend(job: PublishingJobView): void {
        this.store.openConfigureSend(job);
    }

    onRetry(job: PublishingJobView): void {
        this.store.retryBundles({ bundleIds: [job.bundleId] });
    }

    private confirmRemove(job: PublishingJobView): void {
        this.confirmationService.confirm({
            header: this.dotMessageService.get('publishing-queue.confirm-remove.header'),
            message: this.dotMessageService.get(
                'publishing-queue.confirm-remove.message',
                job.bundleName || job.bundleId
            ),
            acceptLabel: this.dotMessageService.get('publishing-queue.remove'),
            rejectLabel: this.dotMessageService.get('publishing-queue.cancel'),
            acceptButtonStyleClass: 'p-button-danger',
            rejectButtonStyleClass: 'p-button-text',
            defaultFocus: 'reject',
            closable: true,
            closeOnEscape: true,
            accept: () => this.store.deleteBundle(job.bundleId)
        });
    }
}
