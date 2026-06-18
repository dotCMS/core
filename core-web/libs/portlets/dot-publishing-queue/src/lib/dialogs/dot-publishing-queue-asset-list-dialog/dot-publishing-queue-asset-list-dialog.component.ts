import { Subject } from 'rxjs';

import {
    ChangeDetectionStrategy,
    Component,
    DestroyRef,
    computed,
    effect,
    inject,
    signal,
    untracked
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';

import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DynamicDialogConfig } from 'primeng/dynamicdialog';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { SkeletonModule } from 'primeng/skeleton';
import { TableModule } from 'primeng/table';
import { TooltipModule } from 'primeng/tooltip';

import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

import { DotMessageService } from '@dotcms/data-access';
import { BundleAssetView } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DotPublishingQueueStore } from '../../dot-publishing-queue-page/store/dot-publishing-queue.store';

/** Show the search input only when the bundle is big enough that scrolling alone is painful. */
const ASSET_SEARCH_THRESHOLD = 10;

@Component({
    selector: 'dot-publishing-queue-asset-list-dialog',
    standalone: true,
    imports: [
        FormsModule,
        ButtonModule,
        ConfirmDialogModule,
        IconFieldModule,
        InputIconModule,
        InputTextModule,
        SkeletonModule,
        TableModule,
        TooltipModule,
        DotMessagePipe
    ],
    providers: [ConfirmationService],
    templateUrl: './dot-publishing-queue-asset-list-dialog.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotPublishingQueueAssetListDialogComponent {
    readonly store = inject(DotPublishingQueueStore);

    private readonly confirmationService = inject(ConfirmationService);
    private readonly dotMessageService = inject(DotMessageService);
    private readonly destroyRef = inject(DestroyRef);
    /** Optional — present only when opened via DialogService. */
    private readonly dialogConfig = inject(DynamicDialogConfig, { optional: true });

    /** When opened from the History tab the bundle is already in `publish_audit`
     * and assets can no longer be removed — the dialog renders as read-only.
     * Default true so existing call sites (Queue/Ready) keep their edit UX. */
    readonly allowRemove = (this.dialogConfig?.data?.allowRemove ?? true) as boolean;

    /** Skeleton rows for the loading state. Length chosen so the placeholder fills
     * the reserved 384px (h-96) and the dialog stays stable on load + after deletes. */
    readonly assetSkeletonRows = Array.from({ length: 8 });

    readonly assetSearch = signal('');
    private readonly searchSubject = new Subject<string>();

    /** Search input only shows when the loaded asset list has more than ASSET_SEARCH_THRESHOLD items. */
    readonly showAssetSearch = computed(
        () => this.store.selectedAssets().length > ASSET_SEARCH_THRESHOLD
    );

    /** Client-side filter over title + type. Case-insensitive. */
    readonly filteredAssets = computed(() => {
        const query = this.assetSearch().trim().toLowerCase();
        const assets = this.store.selectedAssets();
        if (!query) {
            return assets;
        }
        return assets.filter(
            (a) => a.title.toLowerCase().includes(query) || a.type.toLowerCase().includes(query)
        );
    });

    /** True when search is active but returns nothing — distinct UX from "bundle is empty". */
    readonly hasNoMatches = computed(
        () =>
            this.assetSearch().trim().length > 0 &&
            this.store.assetListStatus() === 'loaded' &&
            this.filteredAssets().length === 0 &&
            this.store.selectedAssets().length > 0
    );

    constructor() {
        this.searchSubject
            .pipe(debounceTime(300), distinctUntilChanged(), takeUntilDestroyed(this.destroyRef))
            .subscribe((value) => this.assetSearch.set(value));

        // Reset the input every time the dialog is reused for a different bundle.
        effect(() => {
            this.store.selectedBundleId();
            untracked(() => this.assetSearch.set(''));
        });
    }

    onSearch(value: string): void {
        this.searchSubject.next(value);
    }

    onRemoveAsset(asset: BundleAssetView): void {
        this.confirmationService.confirm({
            header: this.dotMessageService.get('publishing-queue.asset-list.remove-confirm.header'),
            message: this.dotMessageService.get(
                'publishing-queue.asset-list.remove-confirm.message',
                asset.title || asset.asset
            ),
            acceptLabel: this.dotMessageService.get('publishing-queue.remove'),
            rejectLabel: this.dotMessageService.get('publishing-queue.cancel'),
            acceptButtonStyleClass: 'p-button-danger',
            rejectButtonStyleClass: 'p-button-text',
            defaultFocus: 'reject',
            closable: true,
            closeOnEscape: true,
            accept: () => this.store.removeBundleAsset(asset.asset)
        });
    }
}
