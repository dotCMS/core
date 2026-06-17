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

    /** Retry only makes sense for rows the user has explicitly checked. */
    readonly showRetry = computed(
        () => this.store.activeTab() === 'history' && this.store.historySelectedIds().length > 0
    );

    /** The Delete-Bundles button opens a scope picker (SELECTED / ALL / SUCCESS / FAILED),
     * three of which work without any row selection, so the button is visible whenever
     * the history tab has any data at all. */
    readonly showDelete = computed(
        () => this.store.activeTab() === 'history' && this.store.historyTotal() > 0
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
