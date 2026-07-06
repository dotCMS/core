import { Subject, forkJoin, of } from 'rxjs';

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
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { SkeletonModule } from 'primeng/skeleton';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';

import { catchError, debounceTime, distinctUntilChanged, map, take } from 'rxjs/operators';

import { DotContentletEditUrlService, DotMessageService } from '@dotcms/data-access';
import { BundleAssetView, DotCMSContentlet } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DotPublishingQueueStore } from '../../store/dot-publishing-queue.store';
import { groupContentletAssetsByType } from '../../util/asset-groups.util';

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
        TooltipModule,
        DotMessagePipe
    ],
    providers: [ConfirmationService],
    templateUrl: './dot-publishing-queue-asset-list-dialog.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotPublishingQueueAssetListDialogComponent {
    protected readonly store = inject(DotPublishingQueueStore);

    private readonly confirmationService = inject(ConfirmationService);
    private readonly dotMessageService = inject(DotMessageService);
    private readonly destroyRef = inject(DestroyRef);
    private readonly editUrlService = inject(DotContentletEditUrlService);
    /** Optional — present only when opened via DialogService. */
    private readonly dialogConfig = inject(DynamicDialogConfig, { optional: true });
    private readonly dialogRef = inject(DynamicDialogRef, { optional: true });

    /** When opened from the History tab the bundle is already in `publish_audit`
     * and assets can no longer be removed — the dialog renders as read-only.
     * Default true so existing call sites (Queue/Ready) keep their edit UX. */
    readonly allowRemove = (this.dialogConfig?.data?.allowRemove ?? true) as boolean;

    /** Skeleton rows for the loading state. Length chosen so the placeholder fills
     * the reserved 384px (h-96) and the dialog stays stable on load + after deletes. */
    readonly assetSkeletonRows = Array.from({ length: 8 });

    readonly assetSearch = signal('');
    private readonly searchSubject = new Subject<string>();

    /** Per-asset edit URL, resolved by `DotContentletEditUrlService` after each
     * asset list load. Non-contentlet assets (templates, languages, containers,
     * etc.) are never resolved and stay plain text — same rule as the Select
     * Bundle dialog. */
    readonly assetEditUrls = signal<Map<string, string>>(new Map());

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

        // Reset input + resolved URLs every time the dialog is reused for a
        // different bundle. The bundleId signal changes before assets stream in,
        // so this only cleans up — resolution kicks off in the assets effect below.
        effect(() => {
            this.store.selectedBundleId();
            untracked(() => {
                this.assetSearch.set('');
                this.assetEditUrls.set(new Map());
            });
        });

        // Resolve contentlet edit URLs once the store finishes loading. The
        // service caches by content type, so many contentlets of the same type
        // trigger a single metadata fetch.
        effect(() => {
            const status = this.store.assetListStatus();
            const assets = this.store.selectedAssets();
            if (status !== 'loaded') {
                return;
            }
            untracked(() => this.resolveAssetEditUrls(assets));
        });
    }

    onSearch(value: string): void {
        this.searchSubject.next(value);
    }

    /** Opens the resolved contentlet editor URL in a new tab. No-op for assets
     * without a resolved URL (non-contentlet types or still-loading). Mirrors the
     * behavior of the Select Bundle dialog. */
    onAssetRowClick(asset: BundleAssetView): void {
        const url = this.editUrlFor(asset);
        if (url) {
            window.open(url, '_blank', 'noopener');
        }
    }

    /** Template helper — used to bind `cursor-pointer` on linkable rows and to
     * short-circuit `onAssetRowClick` when the asset isn't linkable. */
    editUrlFor(asset: BundleAssetView): string | null {
        return this.assetEditUrls().get(asset.asset) ?? null;
    }

    /** Closes the dialog. Called from the footer Close button. */
    closeDialog(): void {
        this.dialogRef?.close();
    }

    /** Same fan-out avoidance as the Select Bundle dialog: group by content type,
     * fetch once per type, apply the resolved URL to every asset in that group.
     * See `groupContentletAssetsByType` and the rationale in the sister dialog. */
    private resolveAssetEditUrls(assets: BundleAssetView[]): void {
        const groups = groupContentletAssetsByType(assets);

        for (const [contentType, group] of groups) {
            const perAsset$ = group.map((asset) =>
                this.editUrlService
                    .resolveEditUrl({ inode: asset.inode, contentType } as DotCMSContentlet)
                    .pipe(
                        take(1),
                        catchError(() => of('')),
                        map((url) => [asset.asset, url] as const)
                    )
            );

            forkJoin(perAsset$).subscribe((entries) => {
                this.assetEditUrls.update((prev) => {
                    const next = new Map(prev);
                    for (const [key, url] of entries) {
                        if (url) {
                            next.set(key, url);
                        }
                    }
                    return next;
                });
            });
        }
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
