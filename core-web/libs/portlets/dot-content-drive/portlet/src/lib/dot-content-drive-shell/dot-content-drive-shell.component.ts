import { EMPTY, of } from 'rxjs';

import {
    ChangeDetectionStrategy,
    Component,
    effect,
    inject,
    OnInit,
    untracked
} from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { LazyLoadEvent, SortEvent } from 'primeng/api';

import { catchError, take } from 'rxjs/operators';

import { DotContentSearchService, DotSiteService } from '@dotcms/data-access';
import { ESContent } from '@dotcms/dotcms-models';
import { DotFolderListViewComponent } from '@dotcms/portlets/content-drive/ui';

import { SORT_ORDER, SYSTEM_HOST } from '../shared/constants';
import { DotContentDriveSortOrder, DotContentDriveStatus } from '../shared/models';
import { DotContentDriveStore } from '../store/dot-content-drive.store';
import { decodeFilters } from '../utils/functions';

@Component({
    selector: 'dot-content-drive-shell',
    imports: [DotFolderListViewComponent],
    providers: [DotContentDriveStore, DotSiteService],
    templateUrl: './dot-content-drive-shell.component.html',
    styleUrl: './dot-content-drive-shell.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotContentDriveShellComponent implements OnInit {
    readonly #store = inject(DotContentDriveStore);

    readonly #siteService = inject(DotSiteService);

    readonly #contentSearchService = inject(DotContentSearchService);

    readonly #route = inject(ActivatedRoute);

    readonly $items = this.#store.items;
    readonly $totalItems = this.#store.totalItems;
    readonly $status = this.#store.status;

    readonly DOT_CONTENT_DRIVE_STATUS = DotContentDriveStatus;

    readonly itemsEffect = effect(
        () => {
            const currentSite = untracked(() => this.#store.currentSite());
            const query = this.#store.$query();
            const { limit, offset } = this.#store.pagination();
            const { field, order } = this.#store.sort();

            // If the current site is the system host, we don't need to search for content
            // It initializes the store with the system host and the path

            if (currentSite?.identifier === SYSTEM_HOST.identifier) {
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
        }
    );

    ngOnInit(): void {
        this.#siteService
            .getCurrentSite()
            .pipe(
                take(1),
                catchError(() => {
                    return of(SYSTEM_HOST);
                })
            )
            .subscribe((currentSite) => {
                const queryParams = this.#route.snapshot.queryParams;

                const path = queryParams['path'] || '';
                const filters = decodeFilters(queryParams['filters'] || '');

                this.#store.initContentDrive({
                    currentSite,
                    path,
                    filters
                });
            });
    }

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
