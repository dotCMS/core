import { of } from 'rxjs';

import { Component, effect, inject, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { catchError } from 'rxjs/operators';

import { DotContentSearchService, DotSiteService } from '@dotcms/data-access';
import { ESContent } from '@dotcms/dotcms-models';
import { DotFolderListViewComponent } from '@dotcms/portlets/content-drive/ui';

import { DotContentDriveStore } from '../store/dot-content-drive.store';
import { DotContentDriveStatus, SYSTEM_HOST } from '../store/models';
import { decodeFilters } from '../utils/functions';

@Component({
    selector: 'dot-content-drive-shell',
    standalone: true,
    imports: [DotFolderListViewComponent],
    providers: [DotContentDriveStore, DotSiteService],
    templateUrl: './dot-content-drive-shell.component.html',
    styleUrl: './dot-content-drive-shell.component.scss'
})
export class DotContentDriveShellComponent implements OnInit {
    readonly #store = inject(DotContentDriveStore);

    readonly #siteService = inject(DotSiteService);

    readonly #contentSearchService = inject(DotContentSearchService);

    readonly #route = inject(ActivatedRoute);

    readonly $items = this.#store.items;

    readonly itemsEffect = effect(() => {
        this.#contentSearchService
            .get<ESContent>({
                query: this.#store.$query(),
                limit: 40,
                offset: 0
            })
            .pipe(
                catchError(() => {
                    this.#store.setStatus(DotContentDriveStatus.ERROR);

                    return of({
                        jsonObjectView: {
                            contentlets: []
                        }
                    });
                })
            )
            .subscribe((response) => {
                this.#store.setItems(response.jsonObjectView.contentlets);
            });
    });

    ngOnInit(): void {
        this.#siteService
            .getCurrentSite()
            .pipe(
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
}
