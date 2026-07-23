import { Subject } from 'rxjs';

import {
    ChangeDetectionStrategy,
    Component,
    DestroyRef,
    inject
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';

import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { SkeletonModule } from 'primeng/skeleton';
import { TableLazyLoadEvent, TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';

import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

import { DotMessagePipe } from '@dotcms/ui';

import { StudioPageRow } from '../models/accessibility-studio.models';
import { AccessibilityStudioStore } from '../store/accessibility-studio.store';

/**
 * The Studio entry screen (§7): lists/searches the site's pages and selects one
 * to scan. Pages come from a real `_search`; selecting a row opens the studio.
 */
@Component({
    selector: 'dot-a11y-picker',
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
    templateUrl: './a11y-picker.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { class: 'block h-full min-h-0 overflow-y-auto' }
})
export class DotA11yPickerComponent {
    readonly store = inject(AccessibilityStudioStore);

    /** Skeleton rows to render while a page of results loads. */
    readonly skeletonRows = Array.from({ length: 8 });

    /** Pass-through config: fixed table layout so column widths hold on empty state. */
    readonly $ptConfig = { table: { style: { 'table-layout': 'fixed' as const } } };

    private readonly destroyRef = inject(DestroyRef);
    private readonly router = inject(Router);
    private readonly route = inject(ActivatedRoute);
    private readonly searchSubject = new Subject<string>();

    constructor() {
        // Landing on the picker route (fresh, our Back button, or the browser back
        // button) must reset the studio to the picker phase — otherwise a run left
        // it in a non-picker phase and the page-load effect (gated on `picker`)
        // wouldn't refetch. Idempotent: a no-op when already in the picker.
        if (!this.store.inPicker()) {
            this.store.backToPicker();
        }

        this.searchSubject
            .pipe(debounceTime(300), distinctUntilChanged(), takeUntilDestroyed(this.destroyRef))
            .subscribe((value) => this.store.setFilter(value));
    }

    onSearch(value: string): void {
        this.searchSubject.next(value);
    }

    /**
     * Open a page by navigating to its run route, so the selected page lands in
     * the URL and the run is deep-linkable / shareable with a readable path (e.g.
     * `/agents/a11y/blog/post/hello`). The page path becomes real route segments;
     * the run screen reads them back and drives the store — selection is never set
     * directly here, keeping the URL the single source of truth for what's open.
     */
    openPage(row: StudioPageRow): void {
        // "/blog/post/hello" → ['blog','post','hello'] (drop empty leading/trailing).
        const segments = row.path.split('/').filter(Boolean);
        this.router.navigate(segments, { relativeTo: this.route });
    }

    onLazyLoad(event: TableLazyLoadEvent): void {
        const rows = (event.rows as number) ?? this.store.rows();
        const first = (event.first as number) ?? 0;
        const page = Math.floor(first / rows) + 1;
        this.store.setPagination(page, rows);
    }

}
