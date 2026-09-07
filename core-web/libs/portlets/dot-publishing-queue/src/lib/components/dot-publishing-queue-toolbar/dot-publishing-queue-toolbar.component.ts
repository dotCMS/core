import { Subject } from 'rxjs';

import {
    ChangeDetectionStrategy,
    Component,
    DestroyRef,
    computed,
    inject,
    output
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';

import { MenuItem } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { MenuModule } from 'primeng/menu';
import { ToolbarModule } from 'primeng/toolbar';

import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

import { DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';

import { DotPublishingQueueStore } from '../../store/dot-publishing-queue.store';
import { DotPublishingQueueStatusFilterComponent } from '../dot-publishing-queue-status-filter/dot-publishing-queue-status-filter.component';

/** Identifies the menu row that renders the trailing draft-bundle count. */
export const SELECT_BUNDLE_ITEM_ID = 'select-bundle';

@Component({
    selector: 'dot-publishing-queue-toolbar',
    imports: [
        FormsModule,
        ButtonModule,
        IconFieldModule,
        InputIconModule,
        InputTextModule,
        MenuModule,
        ToolbarModule,
        DotMessagePipe,
        DotPublishingQueueStatusFilterComponent
    ],
    templateUrl: './dot-publishing-queue-toolbar.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotPublishingQueueToolbarComponent {
    protected readonly store = inject(DotPublishingQueueStore);
    readonly $uploadClick = output<void>({ alias: 'uploadClick' });
    readonly $selectBundleClick = output<void>({ alias: 'selectBundleClick' });
    readonly $deleteClick = output<void>({ alias: 'deleteClick' });

    readonly #destroyRef = inject(DestroyRef);
    readonly #dotMessageService = inject(DotMessageService);
    #searchSubject = new Subject<string>();

    /** Bulk actions appear only when the user has explicitly checked one or more rows. */
    protected readonly $hasBulkActions = computed(() => this.store.bundlesSelectedIds().length > 0);

    /** Bare "Bundles" while the count is unknown — "(0)" would read as "you
     * have no drafts". */
    protected readonly $bundlesLabel = computed(() => {
        const total = this.store.draftBundlesTotal();

        return total === null
            ? this.#dotMessageService.get('publishing-queue.bundles')
            : this.#dotMessageService.get('publishing-queue.bundles.count', String(total));
    });

    /** Built once, never recomputed: PrimeNG re-processes `[model]` on every
     * identity change and the menu then swallows the first click. */
    readonly addBundleItems: MenuItem[] = [
        {
            id: SELECT_BUNDLE_ITEM_ID,
            label: this.#dotMessageService.get('publishing-queue.add-bundle.select'),
            command: () => this.$selectBundleClick.emit()
        },
        {
            label: this.#dotMessageService.get('publishing-queue.add-bundle.upload'),
            command: () => this.$uploadClick.emit()
        }
    ];

    constructor() {
        this.#searchSubject
            .pipe(debounceTime(300), distinctUntilChanged(), takeUntilDestroyed(this.#destroyRef))
            .subscribe((value) => this.store.setSearch(value));
    }

    onSearch(value: string): void {
        this.#searchSubject.next(value);
    }

    onBulkRetry(): void {
        this.store.retryBundles({ bundleIds: this.store.bundlesSelectedIds() });
    }

    protected showsDraftCount(item: MenuItem): boolean {
        return item.id === SELECT_BUNDLE_ITEM_ID && this.store.draftBundlesTotal() !== null;
    }
}
