import { Subject } from 'rxjs';

import { DatePipe } from '@angular/common';
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

import { ButtonModule } from 'primeng/button';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { SkeletonModule } from 'primeng/skeleton';
import { TableModule } from 'primeng/table';

import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

import { DotPublishingQueueService } from '@dotcms/data-access';
import { PublishAuditStatus } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DotPublishingStatusChipComponent } from '../../components/dot-publishing-status-chip/dot-publishing-status-chip.component';
import { DotPublishingQueueStore } from '../../dot-publishing-queue-page/store/dot-publishing-queue.store';

/** Show the search input only when the bundle is big enough that scrolling alone is painful. */
const ASSET_SEARCH_THRESHOLD = 10;

const SUCCESS_STATUSES = new Set<PublishAuditStatus>([
    PublishAuditStatus.SUCCESS,
    PublishAuditStatus.BUNDLE_SENT_SUCCESSFULLY,
    PublishAuditStatus.BUNDLE_SAVED_SUCCESSFULLY,
    PublishAuditStatus.SUCCESS_WITH_WARNINGS
]);

@Component({
    selector: 'dot-publishing-queue-bundle-details-dialog',
    standalone: true,
    imports: [
        DatePipe,
        FormsModule,
        ButtonModule,
        IconFieldModule,
        InputIconModule,
        InputTextModule,
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
    private readonly destroyRef = inject(DestroyRef);

    /** Placeholder rows for the assets table's loading state. Length chosen so
     * the skeleton fills the reserved 192px (h-48) and the dialog opens at its
     * final size — no jump when the assets endpoint resolves. */
    readonly assetSkeletonRows = Array.from({ length: 5 });

    readonly assetSearch = signal('');
    private readonly searchSubject = new Subject<string>();

    /** Search input only shows when bundle has > ASSET_SEARCH_THRESHOLD assets. */
    readonly showAssetSearch = computed(
        () => (this.store.detail()?.assetCount ?? 0) > ASSET_SEARCH_THRESHOLD
    );

    /** Client-side filter over title + type. Case-insensitive. */
    readonly filteredAssets = computed(() => {
        const query = this.assetSearch().trim().toLowerCase();
        const assets = this.store.detailAssets();
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
            this.store.detailAssetsStatus() === 'loaded' &&
            this.filteredAssets().length === 0 &&
            this.store.detailAssets().length > 0
    );

    readonly canDownload = computed(() => {
        const status = this.store.detail()?.status;
        return status ? SUCCESS_STATUSES.has(status) : false;
    });

    constructor() {
        this.searchSubject
            .pipe(debounceTime(300), distinctUntilChanged(), takeUntilDestroyed(this.destroyRef))
            .subscribe((value) => this.assetSearch.set(value));

        // Reset the input every time the dialog is reused for a different bundle.
        effect(() => {
            this.store.detailBundleId();
            untracked(() => this.assetSearch.set(''));
        });
    }

    onSearch(value: string): void {
        this.searchSubject.next(value);
    }

    downloadHref(bundleId: string): string {
        return this.publishingService.getBundleDownloadUrl(bundleId);
    }
}
