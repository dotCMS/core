import { EMPTY } from 'rxjs';

import { NgClass, Location } from '@angular/common';
import { ChangeDetectionStrategy, Component, effect, inject, untracked } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { LazyLoadEvent, SortEvent } from 'primeng/api';

import { catchError, take } from 'rxjs/operators';

import { DotContentSearchService } from '@dotcms/data-access';
import { ESContent } from '@dotcms/dotcms-models';
import { DotFolderListViewComponent } from '@dotcms/portlets/content-drive/ui';
import { GlobalStore } from '@dotcms/store';

import { DotContentDriveToolbarComponent } from '../components/dot-content-drive-toolbar/dot-content-drive-toolbar.component';
import { DEFAULT_PATH, DEFAULT_TREE_EXPANDED, SORT_ORDER, SYSTEM_HOST } from '../shared/constants';
import { DotContentDriveSortOrder, DotContentDriveStatus } from '../shared/models';
import { DotContentDriveStore } from '../store/dot-content-drive.store';
import { decodeFilters, encodeFilters } from '../utils/functions';

@Component({
    selector: 'dot-content-drive-shell',
    imports: [DotFolderListViewComponent, DotContentDriveToolbarComponent, NgClass],
    providers: [DotContentDriveStore],
    templateUrl: './dot-content-drive-shell.component.html',
    styleUrl: './dot-content-drive-shell.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotContentDriveShellComponent {
    readonly #store = inject(DotContentDriveStore);

    readonly #contentSearchService = inject(DotContentSearchService);
    readonly #route = inject(ActivatedRoute);
    readonly #globalStore = inject(GlobalStore);
    readonly #router = inject(Router);
    readonly #location = inject(Location);

    readonly $items = this.#store.items;
    readonly $totalItems = this.#store.totalItems;
    readonly $status = this.#store.status;
    readonly $treeExpanded = this.#store.treeExpanded;

    readonly DOT_CONTENT_DRIVE_STATUS = DotContentDriveStatus;

    readonly itemsEffect = effect(() => {
        const currentSite = untracked(() => this.#store.currentSite());
        const query = this.#store.$query();
        const { limit, offset } = this.#store.pagination();
        const { field, order } = this.#store.sort();

        // If the current site is the system host, we don't need to search for content
        // It initializes the store with the system host and the path
        if (currentSite?.identifier === SYSTEM_HOST.identifier || !currentSite) {
            return;
        }

        this.#store.setStatus(DotContentDriveStatus.LOADING);

        this.#contentSearchService
            .get<ESContent>({
                query,
                limit,
                offset,
                sort: `score,${field} ${order}`
            })
            .pipe(
                take(1),
                catchError(() => {
                    this.#store.setStatus(DotContentDriveStatus.ERROR);

                    return EMPTY;
                })
            )
            .subscribe((response) => {
                this.#store.setItems(response.jsonObjectView.contentlets, response.resultsSize);
            });
    });

    readonly updateQueryParamsEffect = effect(() => {
        const treeExpanded = this.#store.treeExpanded();
        const path = this.#store.path();
        const filters = this.#store.filters();

        const queryParams: Record<string, string> = {};

        queryParams['treeExpanded'] = treeExpanded.toString();

        if (path && path.length) {
            queryParams['path'] = path;
        }
        if (filters && Object.keys(filters).length) {
            queryParams['filters'] = encodeFilters(filters);
        }

        const urlTree = this.#router.createUrlTree([], { queryParams });
        this.#location.go(urlTree.toString());
    });

    readonly initEffect = effect(() => {
        const currentSite = this.#globalStore.siteDetails();
        const path = this.#route.snapshot.queryParams['path'] ?? DEFAULT_PATH;
        const filters = decodeFilters(this.#route.snapshot.queryParams['filters']);
        const queryTreeExpanded =
            this.#route.snapshot.queryParams['treeExpanded'] ?? DEFAULT_TREE_EXPANDED.toString();
        const treeExpanded = queryTreeExpanded == 'true';

        this.#store.initContentDrive({
            currentSite,
            path,
            filters,
            treeExpanded
        });
    });

    onPaginate(event: LazyLoadEvent) {
        // Explicit check because it can potentially be 0
        if (event.rows === undefined || event.first === undefined) {
            return;
        }

        this.#store.setPagination({
            limit: event.rows,
            offset: event.first
        });
    }

    onSort(event: SortEvent) {
        // Explicit check because it can potentially be 0
        if (event.order === undefined || !event.field) {
            return;
        }

        this.#store.setSort({
            field: event.field,
            order: SORT_ORDER[event.order] ?? DotContentDriveSortOrder.ASC
        });
    }
}
