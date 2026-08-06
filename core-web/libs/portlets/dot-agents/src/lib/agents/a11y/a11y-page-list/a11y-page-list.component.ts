import { Subject } from 'rxjs';

import { ChangeDetectionStrategy, Component, DestroyRef, inject } from '@angular/core';
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
import { A11yPageListStore } from '../store/a11y-page-list.store';

/**
 * The Studio entry screen (§7): lists/searches the site's pages and selects one
 * to scan. Pages come from a real `_search`; selecting a row opens the studio.
 */
@Component({
    selector: 'dot-a11y-page-list',
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
    templateUrl: './a11y-page-list.component.html',
    providers: [A11yPageListStore],
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { class: 'block h-full min-h-0 overflow-y-auto' }
})
export class DotA11yPageListComponent {
    readonly store = inject(A11yPageListStore);

    /** Skeleton rows to render while a page of results loads. */
    readonly skeletonRows = Array.from({ length: 8 });

    /** Pass-through config: fixed table layout so column widths hold on empty state. */
    readonly $ptConfig = { table: { style: { 'table-layout': 'fixed' as const } } };

    private readonly destroyRef = inject(DestroyRef);
    private readonly router = inject(Router);
    private readonly route = inject(ActivatedRoute);
    private readonly searchSubject = new Subject<string>();

    constructor() {
        this.searchSubject
            .pipe(debounceTime(300), distinctUntilChanged(), takeUntilDestroyed(this.destroyRef))
            .subscribe((value) => this.store.setFilter(value));
    }

    onSearch(value: string): void {
        this.searchSubject.next(value);
    }

    /**
     * Open a page by navigating to its run route, so the run URL carries a readable
     * path (e.g. `/agents/a11y/blog/post/hello`).
     *
     * The selected row rides along in the navigation's `state`: the run screen needs
     * the whole {@link StudioPageRow} (identifier, host, language) and the path alone
     * can't supply it. Handing it over here is what lets the run store skip a lookup
     * entirely — the trade-off is that the run route is only reachable THROUGH this
     * list, so a cold load / refresh of a run URL has no row and bounces back here.
     */
    openPage(row: StudioPageRow): void {
        // "/blog/post/hello" → ['blog','post','hello'] (drop empty leading/trailing).
        const segments = row.path.split('/').filter(Boolean);
        this.router.navigate(segments, { relativeTo: this.route, state: { row } });
    }

    onLazyLoad(event: TableLazyLoadEvent): void {
        const rows = (event.rows as number) ?? this.store.rows();
        const first = (event.first as number) ?? 0;
        const page = Math.floor(first / rows) + 1;
        this.store.setPagination(page, rows);
    }
}
