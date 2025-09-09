import { EMPTY } from 'rxjs';

import { Location } from '@angular/common';
import { ChangeDetectionStrategy, Component, effect, inject } from '@angular/core';
import { Router } from '@angular/router';

import { LazyLoadEvent, SortEvent } from 'primeng/api';

import { catchError, take } from 'rxjs/operators';

import { DotContentSearchService } from '@dotcms/data-access';
import { ESContent } from '@dotcms/dotcms-models';
import { DotFolderListViewComponent } from '@dotcms/portlets/content-drive/ui';

import { DotContentDriveToolbarComponent } from '../components/dot-content-drive-toolbar/dot-content-drive-toolbar.component';
import { SORT_ORDER, SYSTEM_HOST } from '../shared/constants';
import { DotContentDriveSortOrder, DotContentDriveStatus } from '../shared/models';
import { DotContentDriveStore } from '../store/dot-content-drive.store';
import { encodeFilters } from '../utils/functions';

@Component({
    selector: 'dot-content-drive-shell',
    imports: [DotFolderListViewComponent, DotContentDriveToolbarComponent],
    providers: [DotContentDriveStore],
    templateUrl: './dot-content-drive-shell.component.html',
    styleUrl: './dot-content-drive-shell.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotContentDriveShellComponent {
    readonly #store = inject(DotContentDriveStore);

    readonly #contentSearchService = inject(DotContentSearchService);
    readonly #router = inject(Router);
    readonly #location = inject(Location);

    readonly $items = this.#store.items;
    readonly $totalItems = this.#store.totalItems;
    readonly $status = this.#store.status;
    readonly $treeExpanded = this.#store.isTreeExpanded;

    readonly DOT_CONTENT_DRIVE_STATUS = DotContentDriveStatus;

    readonly itemsEffect = effect(() => {
        const query = this.#store.$query();
        const currentSite = this.#store.currentSite();
        const { limit, offset } = this.#store.pagination();
        const { field, order } = this.#store.sort();

        this.#store.setStatus(DotContentDriveStatus.LOADING);

        // Avoid fetching content for SYSTEM_HOST sites
        if (currentSite?.identifier === SYSTEM_HOST.identifier) {
            return;
        }

        this.#contentSearchService
            .get<ESContent>({
                query,
                limit,
                offset,
                sort: `${field} ${order}`
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
        const isTreeExpanded = this.#store.isTreeExpanded();
        const path = this.#store.path();
        const filters = this.#store.filters();

        const queryParams: Record<string, string> = {};

        queryParams['isTreeExpanded'] = isTreeExpanded.toString();

        if (path && path.length) {
            queryParams['path'] = path;
        }

        if (filters && Object.keys(filters).length) {
            queryParams['filters'] = encodeFilters(filters);
        } else {
            delete queryParams['filters'];
        }

        const urlTree = this.#router.createUrlTree([], { queryParams });
        this.#location.go(urlTree.toString());
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
