import { ChangeDetectionStrategy, Component, effect, inject, untracked } from '@angular/core';

import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';
import { TabsModule } from 'primeng/tabs';

import { take } from 'rxjs/operators';

import { DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';

import { DotPublishingQueueToolbarComponent } from '../components/dot-publishing-queue-toolbar/dot-publishing-queue-toolbar.component';
import { DotPublishingQueueAssetListDialogComponent } from '../dialogs/dot-publishing-queue-asset-list-dialog/dot-publishing-queue-asset-list-dialog.component';
import { DotPublishingQueueBundleDetailsDialogComponent } from '../dialogs/dot-publishing-queue-bundle-details-dialog/dot-publishing-queue-bundle-details-dialog.component';
import { DotPublishingQueueUploadDialogComponent } from '../dialogs/dot-publishing-queue-upload-dialog/dot-publishing-queue-upload-dialog.component';
import { DotPublishingQueueHistoryComponent } from '../dot-publishing-queue-history/dot-publishing-queue-history.component';
import { DotPublishingQueuePageComponent } from '../dot-publishing-queue-page/dot-publishing-queue-page.component';
import { DotPublishingQueueStore } from '../dot-publishing-queue-page/store/dot-publishing-queue.store';

@Component({
    selector: 'dot-publishing-queue-shell',
    standalone: true,
    imports: [
        TabsModule,
        DotPublishingQueueToolbarComponent,
        DotPublishingQueuePageComponent,
        DotPublishingQueueHistoryComponent,
        DotMessagePipe
    ],
    providers: [DotPublishingQueueStore, DialogService],
    templateUrl: './dot-publishing-queue-shell.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { class: 'flex flex-col h-full min-h-0 block' }
})
export class DotPublishingQueueShellComponent {
    readonly store = inject(DotPublishingQueueStore);

    private readonly dialogService = inject(DialogService);
    private readonly dotMessageService = inject(DotMessageService);

    private detailRef: DynamicDialogRef | null = null;
    private uploadRef: DynamicDialogRef | null = null;
    private assetListRef: DynamicDialogRef | null = null;

    readonly TABS = ['queue', 'history'] as const;

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

    private syncAssetList(bundleId: string | null): void {
        if (bundleId && !this.assetListRef) {
            this.assetListRef = this.dialogService.open(
                DotPublishingQueueAssetListDialogComponent,
                {
                    header: this.dotMessageService.get('publishing-queue.asset-list.title'),
                    width: '700px',
                    closable: true,
                    closeOnEscape: true,
                    draggable: false,
                    position: 'center'
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
