import { ChangeDetectionStrategy, Component, effect, inject, untracked } from '@angular/core';

import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';

import { take } from 'rxjs/operators';

import { DotMessageService } from '@dotcms/data-access';

import { DotPublishingQueueStore } from './store/dot-publishing-queue.store';

import { DotPublishingQueueAssetListDialogComponent } from '../dialogs/dot-publishing-queue-asset-list-dialog/dot-publishing-queue-asset-list-dialog.component';
import { DotPublishingQueueListComponent } from '../dot-publishing-queue-list/dot-publishing-queue-list.component';

@Component({
    selector: 'dot-publishing-queue-page',
    standalone: true,
    imports: [DotPublishingQueueListComponent],
    templateUrl: './dot-publishing-queue-page.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { class: 'flex min-h-0 flex-1' }
})
export class DotPublishingQueuePageComponent {
    readonly store = inject(DotPublishingQueueStore);

    private readonly dialogService = inject(DialogService);
    private readonly dotMessageService = inject(DotMessageService);
    private dialogRef: DynamicDialogRef | null = null;

    constructor() {
        effect(() => {
            const bundleId = this.store.selectedBundleId();
            untracked(() => {
                if (bundleId && !this.dialogRef) {
                    this.openAssetListDialog();
                } else if (!bundleId && this.dialogRef) {
                    this.dialogRef.close();
                    this.dialogRef = null;
                }
            });
        });
    }

    private openAssetListDialog(): void {
        this.dialogRef = this.dialogService.open(DotPublishingQueueAssetListDialogComponent, {
            header: this.dotMessageService.get('publishing-queue.asset-list.title'),
            width: '700px',
            closable: true,
            closeOnEscape: true,
            draggable: false,
            position: 'center'
        });

        this.dialogRef.onClose.pipe(take(1)).subscribe(() => {
            this.dialogRef = null;
            this.store.closeAssetList();
        });
    }
}
