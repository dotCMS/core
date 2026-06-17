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

import { ButtonModule } from 'primeng/button';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { ToolbarModule } from 'primeng/toolbar';

import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

import { DotMessagePipe } from '@dotcms/ui';

import { DotPublishingQueueStore } from '../../dot-publishing-queue-page/store/dot-publishing-queue.store';

@Component({
    selector: 'dot-publishing-queue-toolbar',
    standalone: true,
    imports: [
        FormsModule,
        ButtonModule,
        IconFieldModule,
        InputIconModule,
        InputTextModule,
        ToolbarModule,
        DotMessagePipe
    ],
    templateUrl: './dot-publishing-queue-toolbar.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotPublishingQueueToolbarComponent {
    readonly store = inject(DotPublishingQueueStore);
    readonly uploadClick = output<void>();
    readonly deleteClick = output<void>();

    private readonly destroyRef = inject(DestroyRef);
    private searchSubject = new Subject<string>();

    /** Bulk actions appear only when the user has explicitly checked one or more rows.
     * The Delete-Bundles dialog still offers ALL/SUCCESS/FAILED scopes (which don't
     * strictly need a selection), but exposing them only after a selection keeps the
     * top bar quiet and matches the rest of the bulk-action UI. */
    readonly hasBulkActions = computed(
        () => this.store.activeTab() === 'history' && this.store.historySelectedIds().length > 0
    );

    constructor() {
        this.searchSubject
            .pipe(debounceTime(300), distinctUntilChanged(), takeUntilDestroyed(this.destroyRef))
            .subscribe((value) => this.store.setSearch(value));
    }

    onSearch(value: string): void {
        this.searchSubject.next(value);
    }

    onBulkRetry(): void {
        this.store.retryBundles({ bundleIds: this.store.historySelectedIds() });
    }
}
