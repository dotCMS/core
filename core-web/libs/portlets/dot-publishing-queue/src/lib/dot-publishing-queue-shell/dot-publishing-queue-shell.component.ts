import { ChangeDetectionStrategy, Component, effect, inject, untracked } from '@angular/core';

import { ConfirmationService } from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';

import { take } from 'rxjs/operators';

import { DotMessageService } from '@dotcms/data-access';
import { PublishAuditStatus } from '@dotcms/dotcms-models';

import { DotPublishingQueueToolbarComponent } from '../components/dot-publishing-queue-toolbar/dot-publishing-queue-toolbar.component';
import { DotPublishingQueueAssetListDialogHeaderComponent } from '../dialogs/dot-publishing-queue-asset-list-dialog/dot-publishing-queue-asset-list-dialog-header.component';
import { DotPublishingQueueAssetListDialogComponent } from '../dialogs/dot-publishing-queue-asset-list-dialog/dot-publishing-queue-asset-list-dialog.component';
import { DotPublishingQueueBundleDetailsDialogComponent } from '../dialogs/dot-publishing-queue-bundle-details-dialog/dot-publishing-queue-bundle-details-dialog.component';
import { DotPublishingQueueSelectBundleDialogComponent } from '../dialogs/dot-publishing-queue-select-bundle-dialog/dot-publishing-queue-select-bundle-dialog.component';
import { DotPublishingQueueUploadDialogComponent } from '../dialogs/dot-publishing-queue-upload-dialog/dot-publishing-queue-upload-dialog.component';
import { DotPublishingQueueTableComponent } from '../dot-publishing-queue-table/dot-publishing-queue-table.component';
import { DotPublishingQueueStore } from '../store/dot-publishing-queue.store';

/** Statuses for which the bundle hasn't yet been packed — assets can still be
 * edited from the asset list dialog. Anything else is read-only (already in
 * `publish_audit`). SCHEDULED bundles are queued for a future publish date but
 * haven't been picked up by the cron yet, so removing an asset is still safe —
 * the BE only rejects removal for `in progress` bundles. */
const EDITABLE_ASSET_STATUSES = new Set<PublishAuditStatus | null>([
    null,
    PublishAuditStatus.BUNDLE_REQUESTED,
    PublishAuditStatus.WAITING_FOR_PUBLISHING,
    PublishAuditStatus.SCHEDULED
]);

@Component({
    selector: 'dot-publishing-queue-shell',
    imports: [
        ConfirmDialogModule,
        DotPublishingQueueToolbarComponent,
        DotPublishingQueueTableComponent
    ],
    providers: [DotPublishingQueueStore, DialogService, ConfirmationService],
    templateUrl: './dot-publishing-queue-shell.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { class: 'flex flex-col h-full min-h-0 block' }
})
export class DotPublishingQueueShellComponent {
    readonly #store = inject(DotPublishingQueueStore);
    readonly #dialogService = inject(DialogService);
    readonly #confirmationService = inject(ConfirmationService);
    readonly #dotMessageService = inject(DotMessageService);

    #detailRef: DynamicDialogRef | null = null;
    #uploadRef: DynamicDialogRef | null = null;
    #assetListRef: DynamicDialogRef | null = null;
    #selectBundleRef: DynamicDialogRef | null = null;

    constructor() {
        effect(() => {
            const bundleId = this.#store.selectedBundleId();
            untracked(() => this.#syncAssetList(bundleId));
        });

        effect(() => {
            const bundleId = this.#store.detailBundleId();
            untracked(() => this.#syncDetail(bundleId));
        });
    }

    openSelectBundle(): void {
        if (this.#selectBundleRef) {
            return;
        }
        this.#selectBundleRef = this.#dialogService.open(
            DotPublishingQueueSelectBundleDialogComponent,
            {
                // Header is rendered inside the dialog body so its title can
                // swap between "Select Bundle" and "Configure & Send" as the
                // user moves through the wizard steps.
                showHeader: false,
                width: 'min(95vw, 1100px)',
                contentStyle: { height: '70vh', padding: '0' },
                closable: true,
                closeOnEscape: true,
                draggable: false,
                position: 'center'
            }
        );
        this.#selectBundleRef.onClose.pipe(take(1)).subscribe(() => {
            this.#selectBundleRef = null;
            // Selecting/removing bundles inside the dialog may have changed the
            // active set — refresh the unified table so the user sees the latest.
            this.#store.refresh();
        });
    }

    openUpload(): void {
        if (this.#uploadRef) {
            return;
        }
        this.#uploadRef = this.#dialogService.open(DotPublishingQueueUploadDialogComponent, {
            header: this.#dotMessageService.get('publishing-queue.upload.title'),
            width: '700px',
            contentStyle: { height: '460px' },
            closable: true,
            closeOnEscape: true,
            draggable: false,
            position: 'center'
        });
        this.#uploadRef.onClose.pipe(take(1)).subscribe(() => {
            this.#uploadRef = null;
        });
    }

    /** Confirms bulk removal of the currently-selected bundles. The toolbar only
     * surfaces the trigger when there's a selection, so we never reach here with
     * an empty list under normal use — but we still guard, since signals can
     * change between the click and the accept callback. */
    confirmDeleteBundles(): void {
        const bundleIds = this.#store.bundlesSelectedIds();
        if (bundleIds.length === 0) {
            return;
        }
        this.#confirmationService.confirm({
            header: this.#dotMessageService.get('publishing-queue.bulk-remove.header'),
            message: this.#dotMessageService.get(
                'publishing-queue.bulk-remove.message',
                `${bundleIds.length}`
            ),
            acceptLabel: this.#dotMessageService.get('publishing-queue.remove'),
            rejectLabel: this.#dotMessageService.get('publishing-queue.cancel'),
            acceptButtonStyleClass: 'p-button-primary',
            rejectButtonStyleClass: 'p-button-text',
            defaultFocus: 'reject',
            closable: true,
            closeOnEscape: true,
            accept: () => this.#store.deleteBundlesBulk(this.#store.bundlesSelectedIds())
        });
    }

    #syncAssetList(bundleId: string | null): void {
        if (bundleId && !this.#assetListRef) {
            // Editable only when the bundle hasn't started packing yet
            // (BUNDLE_REQUESTED / WAITING_FOR_PUBLISHING / drafts with null status).
            // Once the bundle is in motion or terminal, the asset list is read-only.
            const row = this.#store.bundlesRows().find((r) => r.bundleId === bundleId);
            const allowRemove = EDITABLE_ASSET_STATUSES.has(row?.status ?? null);
            const bundleName = row?.bundleName ?? null;

            this.#assetListRef = this.#dialogService.open(
                DotPublishingQueueAssetListDialogComponent,
                {
                    templates: {
                        header: DotPublishingQueueAssetListDialogHeaderComponent
                    },
                    width: '700px',
                    closable: true,
                    closeOnEscape: true,
                    draggable: false,
                    position: 'center',
                    data: { allowRemove, bundleName }
                }
            );
            this.#assetListRef.onClose.pipe(take(1)).subscribe(() => {
                this.#assetListRef = null;
                this.#store.closeAssetList();
            });
        } else if (!bundleId && this.#assetListRef) {
            this.#assetListRef.close();
            this.#assetListRef = null;
        }
    }

    #syncDetail(bundleId: string | null): void {
        if (bundleId && !this.#detailRef) {
            this.#detailRef = this.#dialogService.open(
                DotPublishingQueueBundleDetailsDialogComponent,
                {
                    header: this.#dotMessageService.get('publishing-queue.detail.title'),
                    width: '780px',
                    closable: true,
                    closeOnEscape: true,
                    draggable: false,
                    position: 'center'
                }
            );
            this.#detailRef.onClose.pipe(take(1)).subscribe(() => {
                this.#detailRef = null;
                this.#store.closeDetail();
            });
        } else if (!bundleId && this.#detailRef) {
            this.#detailRef.close();
            this.#detailRef = null;
        }
    }
}
