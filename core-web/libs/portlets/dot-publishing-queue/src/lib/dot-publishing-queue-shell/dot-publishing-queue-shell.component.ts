import { ChangeDetectionStrategy, Component, effect, inject, untracked } from '@angular/core';

import { ConfirmationService } from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';
import { TabsModule } from 'primeng/tabs';

import { take } from 'rxjs/operators';

import { DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';

import { DotPublishingQueueToolbarComponent } from '../components/dot-publishing-queue-toolbar/dot-publishing-queue-toolbar.component';
import { DotPublishingQueueAssetListDialogComponent } from '../dialogs/dot-publishing-queue-asset-list-dialog/dot-publishing-queue-asset-list-dialog.component';
import { DotPublishingQueueBundleDetailsDialogComponent } from '../dialogs/dot-publishing-queue-bundle-details-dialog/dot-publishing-queue-bundle-details-dialog.component';
import {
    DeleteBundlesScope,
    DotPublishingQueueDeleteDialogComponent
} from '../dialogs/dot-publishing-queue-delete-dialog/dot-publishing-queue-delete-dialog.component';
import { DotPublishingQueueUploadDialogComponent } from '../dialogs/dot-publishing-queue-upload-dialog/dot-publishing-queue-upload-dialog.component';
import { DotPublishingQueueHistoryComponent } from '../dot-publishing-queue-history/dot-publishing-queue-history.component';
import { DotPublishingQueuePageComponent } from '../dot-publishing-queue-page/dot-publishing-queue-page.component';
import {
    DotPublishingQueueStore,
    PURGE_FAILED_STATUSES,
    PURGE_SUCCESS_STATUSES
} from '../dot-publishing-queue-page/store/dot-publishing-queue.store';

@Component({
    selector: 'dot-publishing-queue-shell',
    standalone: true,
    imports: [
        ConfirmDialogModule,
        TabsModule,
        DotPublishingQueueToolbarComponent,
        DotPublishingQueuePageComponent,
        DotPublishingQueueHistoryComponent,
        DotMessagePipe
    ],
    providers: [DotPublishingQueueStore, DialogService, ConfirmationService],
    templateUrl: './dot-publishing-queue-shell.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { class: 'flex flex-col h-full min-h-0 block' }
})
export class DotPublishingQueueShellComponent {
    readonly store = inject(DotPublishingQueueStore);

    private readonly dialogService = inject(DialogService);
    private readonly confirmationService = inject(ConfirmationService);
    private readonly dotMessageService = inject(DotMessageService);

    private detailRef: DynamicDialogRef | null = null;
    private uploadRef: DynamicDialogRef | null = null;
    private assetListRef: DynamicDialogRef | null = null;
    private deleteRef: DynamicDialogRef | null = null;

    readonly TABS = ['queue', 'history'] as const;

    /** Zero-out PrimeNG's default tabpanel padding so portlet content goes flush
     * edge-to-edge, matching the dot-tags / dot-query-tool layout. */
    readonly tabPanelsPt = { root: { class: 'flex-1 min-h-0 p-0!' } };
    readonly tabPanelPt = { root: { class: 'h-full p-0! flex flex-col min-h-0' } };

    constructor() {
        effect(() => {
            const bundleId = this.store.selectedBundleId();
            untracked(() => this.syncAssetList(bundleId));
        });

        effect(() => {
            const bundleId = this.store.detailBundleId();
            untracked(() => this.syncDetail(bundleId));
        });
    }

    onTabChange(value: string | number): void {
        this.store.setActiveTab(value === 'history' ? 'history' : 'queue');
    }

    openUpload(): void {
        if (this.uploadRef) {
            return;
        }
        this.uploadRef = this.dialogService.open(DotPublishingQueueUploadDialogComponent, {
            header: this.dotMessageService.get('publishing-queue.upload.title'),
            width: '700px',
            contentStyle: { height: '460px' },
            closable: true,
            closeOnEscape: true,
            draggable: false,
            position: 'center'
        });
        this.uploadRef.onClose.pipe(take(1)).subscribe(() => {
            this.uploadRef = null;
        });
    }

    /** Opens the "Select Bundles to Delete" dialog; on close, dispatches the
     * chosen scope to the store. The ALL scope is gated by a destructive-confirm
     * step to match the legacy JSP's pre-call `confirm("This cannot be undone")`. */
    openDeleteBundles(): void {
        if (this.deleteRef) {
            return;
        }
        this.deleteRef = this.dialogService.open(DotPublishingQueueDeleteDialogComponent, {
            header: this.dotMessageService.get('bundle.delete.title'),
            width: '500px',
            closable: true,
            closeOnEscape: true,
            draggable: false,
            position: 'center'
        });
        this.deleteRef.onClose
            .pipe(take(1))
            .subscribe((scope: DeleteBundlesScope | undefined) => {
                this.deleteRef = null;
                if (scope) {
                    this.dispatchDelete(scope);
                }
            });
    }

    private dispatchDelete(scope: DeleteBundlesScope): void {
        switch (scope) {
            case 'selected':
                this.store.deleteBundlesBulk(this.store.historySelectedIds());
                break;
            case 'all':
                this.confirmationService.confirm({
                    header: this.dotMessageService.get('bundle.delete.title'),
                    message: this.dotMessageService.get('bundle.delete.all.confirmation'),
                    acceptLabel: this.dotMessageService.get('publishing-queue.remove'),
                    rejectLabel: this.dotMessageService.get('publishing-queue.cancel'),
                    acceptButtonStyleClass: 'p-button-danger',
                    rejectButtonStyleClass: 'p-button-text',
                    defaultFocus: 'reject',
                    closable: true,
                    closeOnEscape: true,
                    accept: () => this.store.purgeBundles()
                });
                break;
            case 'success':
                this.store.purgeBundles(PURGE_SUCCESS_STATUSES);
                break;
            case 'failed':
                this.store.purgeBundles(PURGE_FAILED_STATUSES);
                break;
        }
    }

    private syncAssetList(bundleId: string | null): void {
        if (bundleId && !this.assetListRef) {
            // History bundles are already in `publish_audit` — assets are
            // read-only there. Only the Queue tab (drafts/in-progress) allows
            // removal.
            const allowRemove = this.store.activeTab() === 'queue';
            this.assetListRef = this.dialogService.open(
                DotPublishingQueueAssetListDialogComponent,
                {
                    header: this.dotMessageService.get('publishing-queue.asset-list.title'),
                    width: '700px',
                    closable: true,
                    closeOnEscape: true,
                    draggable: false,
                    position: 'center',
                    data: { allowRemove }
                }
            );
            this.assetListRef.onClose.pipe(take(1)).subscribe(() => {
                this.assetListRef = null;
                this.store.closeAssetList();
            });
        } else if (!bundleId && this.assetListRef) {
            this.assetListRef.close();
            this.assetListRef = null;
        }
    }

    private syncDetail(bundleId: string | null): void {
        if (bundleId && !this.detailRef) {
            this.detailRef = this.dialogService.open(
                DotPublishingQueueBundleDetailsDialogComponent,
                {
                    header: this.dotMessageService.get('publishing-queue.detail.title'),
                    width: '780px',
                    closable: true,
                    closeOnEscape: true,
                    draggable: false,
                    position: 'center'
                }
            );
            this.detailRef.onClose.pipe(take(1)).subscribe(() => {
                this.detailRef = null;
                this.store.closeDetail();
            });
        } else if (!bundleId && this.detailRef) {
            this.detailRef.close();
            this.detailRef = null;
        }
    }
}
