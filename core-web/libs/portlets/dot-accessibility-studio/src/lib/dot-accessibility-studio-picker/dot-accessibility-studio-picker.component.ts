import { Subject } from 'rxjs';

import {
    ChangeDetectionStrategy,
    Component,
    DestroyRef,
    inject
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';

import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { SkeletonModule } from 'primeng/skeleton';
import { TableLazyLoadEvent, TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';

import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

import { DotMessagePipe } from '@dotcms/ui';

import { AccessibilityStudioStore } from '../store/accessibility-studio.store';

/**
 * The Studio entry screen (§7): lists/searches the site's pages and selects one
 * to scan. Pages come from a real `_search`; selecting a row opens the studio.
 */
@Component({
    selector: 'dot-accessibility-studio-picker',
    standalone: true,
    imports: [
        FormsModule,
        TableModule,
        InputTextModule,
        IconFieldModule,
        InputIconModule,
        SkeletonModule,
        TagModule,
        DotMessagePipe
    ],
    templateUrl: './dot-accessibility-studio-picker.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { class: 'block h-full min-h-0 overflow-y-auto' }
})
export class DotAccessibilityStudioPickerComponent {
    readonly store = inject(AccessibilityStudioStore);

    /** Skeleton rows to render while a page of results loads. */
    readonly skeletonRows = Array.from({ length: 8 });

    /** Pass-through config: fixed table layout so column widths hold on empty state. */
    readonly $ptConfig = { table: { style: { 'table-layout': 'fixed' as const } } };

    private readonly destroyRef = inject(DestroyRef);
    private readonly searchSubject = new Subject<string>();

    constructor() {
        this.searchSubject
            .pipe(debounceTime(300), distinctUntilChanged(), takeUntilDestroyed(this.destroyRef))
            .subscribe((value) => this.store.setFilter(value));
    }

    onSearch(value: string): void {
        this.searchSubject.next(value);
    }

    onLazyLoad(event: TableLazyLoadEvent): void {
        const rows = (event.rows as number) ?? this.store.rows();
        const first = (event.first as number) ?? 0;
        const page = Math.floor(first / rows) + 1;
        this.store.setPagination(page, rows);
    }

}
