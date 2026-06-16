import { ChangeDetectionStrategy, Component, inject } from '@angular/core';

import { ConfirmationService, MenuItem } from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { MenuModule } from 'primeng/menu';

/* eslint-disable @nx/enforce-module-boundaries */

import { DotMessageService } from '@dotcms/data-access';
import { DotPushPublishDialogService } from '@dotcms/dotcms-js';
import { PublishingJobView } from '@dotcms/dotcms-models';
import { PrincipalConfiguration } from '@dotcms/ui';
import { DotDownloadBundleDialogService } from '@services/dot-download-bundle-dialog/dot-download-bundle-dialog.service';

import { DotPublishingQueueStore } from './store/dot-publishing-queue.store';

import { DotPublishingQueueListComponent } from '../dot-publishing-queue-list/dot-publishing-queue-list.component';

// `DotDownloadBundleDialogService` lives in apps/dotcms-ui (not yet promoted to a
// shared lib). Same pattern as `app.routes.ts` uses for `@portlets/*` imports.
// TODO: promote the service+component to `@dotcms/dotcms-js` (alongside
// DotPushPublishDialogService) — tracked as a follow-up to the v1 consolidation
// work (#36048).

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
    private readonly dotPushPublishDialogService = inject(DotPushPublishDialogService);
    private readonly dotDownloadBundleDialogService = inject(DotDownloadBundleDialogService);

    readonly readyEmpty: PrincipalConfiguration = {
        icon: 'pi-folder-open',
        title: this.dotMessageService.get('publishing-queue.empty.ready.title'),
        subtitle: this.dotMessageService.get('publishing-queue.empty.ready.subtitle')
    };

    readonly progressEmpty: PrincipalConfiguration = {
        icon: 'pi-hourglass',
        title: this.dotMessageService.get('publishing-queue.empty.in-progress.title'),
        subtitle: this.dotMessageService.get('publishing-queue.empty.in-progress.subtitle')
    };

    /**
     * Arrow property (not a method) so the function reference stays stable
     * across change detection cycles. Passing `readyKebabFor.bind(this)` in the
     * template would create a fresh function on every CD, which defeats the
     * list component's `kebabMenus` memoization and triggers `<p-menu>` thrash
     * (the first click on an item only closes the menu instead of firing).
     */
    readonly readyKebabFor = (job: PublishingJobView): MenuItem[] => [
        {
            label: this.dotMessageService.get('publishing-queue.kebab.configure-send'),
            command: () => this.openPushPublish(job)
        },
        {
            label: this.dotMessageService.get('publishing-queue.kebab.generate-download'),
            command: () => this.dotDownloadBundleDialogService.open(job.bundleId)
        },
        { separator: true },
        {
            label: this.dotMessageService.get('publishing-queue.kebab.remove'),
            styleClass: 'p-menuitem-danger',
            command: () => this.confirmRemove(job)
        }
    ];

    onRowClick(row: PublishingJobView, mode: 'ready' | 'progress'): void {
        if (mode === 'ready') {
            this.store.openAssetList(row.bundleId);
        } else {
            this.store.openDetail(row.bundleId);
        }
    }

    onSend(job: PublishingJobView): void {
        this.openPushPublish(job);
    }

    onRetry(job: PublishingJobView): void {
        this.store.retryBundles({ bundleIds: [job.bundleId] });
    }

    /**
     * Opens the project-wide push publish dialog (the same one used by templates,
     * containers, content types, pages, content). Triggered via the global singleton
     * service — the dialog itself is mounted once in `main-legacy.component.html`.
     * `isBundle: true` routes the submit to the bundle endpoint instead of asset.
     */
    private openPushPublish(job: PublishingJobView): void {
        this.dotPushPublishDialogService.open({
            assetIdentifier: job.bundleId,
            title: job.bundleName || job.bundleId,
            isBundle: true
        });
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
