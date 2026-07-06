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
    readonly uploadClick = output<void>();
    readonly selectBundleClick = output<void>();
    readonly deleteClick = output<void>();

    readonly #destroyRef = inject(DestroyRef);
    readonly #dotMessageService = inject(DotMessageService);
    #searchSubject = new Subject<string>();

    /** Bulk actions appear only when the user has explicitly checked one or more rows. */
    readonly hasBulkActions = computed(() => this.store.bundlesSelectedIds().length > 0);

    /** "Add Bundle" split-menu items. The commands emit outputs instead of
     * calling services directly so the shell owns dialog orchestration
     * (component ↔ dialog separation per libs/portlets/CLAUDE.md). */
    readonly addBundleItems: MenuItem[] = [
        {
            label: this.#dotMessageService.get('publishing-queue.add-bundle.select'),
            icon: 'pi pi-table',
            command: () => this.selectBundleClick.emit()
        },
        {
            label: this.#dotMessageService.get('publishing-queue.add-bundle.upload'),
            icon: 'pi pi-upload',
            command: () => this.uploadClick.emit()
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
}
