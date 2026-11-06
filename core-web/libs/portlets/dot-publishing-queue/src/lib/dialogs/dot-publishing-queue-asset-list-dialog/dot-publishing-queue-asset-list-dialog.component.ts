import { Subject } from 'rxjs';

import {
    ChangeDetectionStrategy,
    Component,
    DestroyRef,
    Injector,
    computed,
    effect,
    inject,
    signal,
    untracked
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';

import { ConfirmationService, MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { SkeletonModule } from 'primeng/skeleton';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { ToastModule } from 'primeng/toast';
import { TooltipModule } from 'primeng/tooltip';

import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

import { DotContentChangeService, DotMessageService } from '@dotcms/data-access';
import { BundleAssetView } from '@dotcms/dotcms-models';
import { DotEditContentOpenerService } from '@dotcms/edit-content';
import { DotMessagePipe } from '@dotcms/ui';

import { DotPublishingQueueStore } from '../../store/dot-publishing-queue.store';
import { injectAssetRowFocusReturn } from '../../util/asset-row-focus.util';
import { isEditableContentlet } from '../../util/editable-asset.util';

/** Show the search input only when the bundle is big enough that scrolling alone is painful. */
const ASSET_SEARCH_THRESHOLD = 10;

@Component({
    selector: 'dot-publishing-queue-asset-list-dialog',
    imports: [
        FormsModule,
        ButtonModule,
        ConfirmDialogModule,
        IconFieldModule,
        InputIconModule,
        InputTextModule,
        SkeletonModule,
        TableModule,
        TagModule,
        ToastModule,
        TooltipModule,
        DotMessagePipe
    ],
    // MessageService: a row opens the side panel under this dialog's injector, and its editor needs
    // PrimeNG's MessageService. The admin root provides none (one there would hide the page
    // editor's), so the panel toasts through this one, in this template (#38303).
    providers: [ConfirmationService, MessageService],
    templateUrl: './dot-publishing-queue-asset-list-dialog.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotPublishingQueueAssetListDialogComponent {
    protected readonly store = inject(DotPublishingQueueStore);

    readonly #confirmationService = inject(ConfirmationService);
    readonly #dotMessageService = inject(DotMessageService);
    readonly #destroyRef = inject(DestroyRef);
    readonly #opener = inject(DotEditContentOpenerService);
    /** The dialog's injector: a panel a row opens closes with the dialog. */
    readonly #injector = inject(Injector);
    /** Puts focus back on a row once the list has reloaded after its panel closed (FR-039). */
    readonly #focusRowAfterReload = injectAssetRowFocusReturn(
        computed(() => this.store.assetListStatus() !== 'loading')
    );
    /** Optional — present only when opened via DialogService. */
    readonly #dialogConfig = inject(DynamicDialogConfig, { optional: true });
    readonly #dialogRef = inject(DynamicDialogRef, { optional: true });

    /** When opened from the History tab the bundle is already in `publish_audit`
     * and assets can no longer be removed — the dialog renders as read-only.
     * Default true so existing call sites (Queue/Ready) keep their edit UX. */
    readonly allowRemove = (this.#dialogConfig?.data?.allowRemove ?? true) as boolean;

    /** Skeleton rows for the loading state. Length chosen so the placeholder fills
     * the reserved 384px (h-96) and the dialog stays stable on load + after deletes. */
    readonly assetSkeletonRows = Array.from({ length: 8 });

    readonly $assetSearch = signal('');
    readonly #searchSubject = new Subject<string>();

    /** Search input only shows when the loaded asset list has more than ASSET_SEARCH_THRESHOLD items. */
    readonly $showAssetSearch = computed(
        () => this.store.selectedAssets().length > ASSET_SEARCH_THRESHOLD
    );

    /** Client-side filter over title + type. Case-insensitive. */
    readonly $filteredAssets = computed(() => {
        const query = this.$assetSearch().trim().toLowerCase();
        const assets = this.store.selectedAssets();
        if (!query) {
            return assets;
        }
        return assets.filter(
            (a) => a.title.toLowerCase().includes(query) || a.type.toLowerCase().includes(query)
        );
    });

    /** True when search is active but returns nothing — distinct UX from "bundle is empty". */
    readonly $hasNoMatches = computed(
        () =>
            this.$assetSearch().trim().length > 0 &&
            this.store.assetListStatus() === 'loaded' &&
            this.$filteredAssets().length === 0 &&
            this.store.selectedAssets().length > 0
    );

    constructor() {
        this.#searchSubject
            .pipe(debounceTime(300), distinctUntilChanged(), takeUntilDestroyed(this.#destroyRef))
            .subscribe((value) => this.$assetSearch.set(value));

        // Reset the search every time the dialog is reused for a different bundle.
        effect(() => {
            this.store.selectedBundleId();
            untracked(() => this.$assetSearch.set(''));
        });

        // Every save and delete in a side panel, at any depth, reloads the asset list: content
        // saved from a relationship inside the panel a row opened may be in it (T174).
        inject(DotContentChangeService)
            .changes$.pipe(takeUntilDestroyed(this.#destroyRef))
            .subscribe(() => this.store.loadAssets());
    }

    onSearch(value: string): void {
        this.#searchSubject.next(value);
    }

    /** Template helper: only contentlet rows open, so only they get the pointer cursor. */
    protected readonly isEditableContentlet = isEditableContentlet;

    /**
     * Opens a contentlet row in the side panel over this dialog, in whichever editor its type uses
     * (#38303, R12). The close reloads the asset list (FR-017) and puts focus back on the row once
     * the list has reloaded (FR-039); a save reloads it through the content change notification
     * (T174). Other rows do nothing.
     *
     * @param asset The clicked row.
     */
    onAssetRowClick(asset: BundleAssetView): void {
        if (!isEditableContentlet(asset)) {
            return;
        }

        this.#opener.open(
            {
                mode: 'edit',
                inode: asset.inode,
                // The variable: the opener looks the type up by it to choose the editor.
                contentType: asset.content_type_variable,
                portletId: 'publishing-queue'
            },
            {
                injector: this.#injector,
                closed: () => {
                    this.store.loadAssets();
                    this.#focusRowAfterReload(asset.asset);
                },
                // In the portlet's URL: on load the portlet reopens the panel over the list (#38303).
                url: {}
            }
        );
    }

    /** Closes the dialog. Called from the footer Close button. */
    closeDialog(): void {
        this.#dialogRef?.close();
    }

    onRemoveAsset(asset: BundleAssetView): void {
        this.#confirmationService.confirm({
            header: this.#dotMessageService.get(
                'publishing-queue.asset-list.remove-confirm.header'
            ),
            message: this.#dotMessageService.get(
                'publishing-queue.asset-list.remove-confirm.message',
                asset.title || asset.asset
            ),
            acceptLabel: this.#dotMessageService.get('publishing-queue.delete'),
            rejectLabel: this.#dotMessageService.get('publishing-queue.cancel'),
            acceptButtonStyleClass: 'p-button-danger',
            rejectButtonStyleClass: 'p-button-text',
            defaultFocus: 'reject',
            closable: true,
            closeOnEscape: true,
            accept: () => this.store.removeBundleAsset(asset.asset)
        });
    }
}
