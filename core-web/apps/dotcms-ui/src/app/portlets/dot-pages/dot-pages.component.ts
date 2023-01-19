/* eslint-disable no-console */
import { Component } from '@angular/core';

import { LazyLoadEvent } from 'primeng/api/lazyloadevent';

import { Observable } from 'rxjs/internal/Observable';

import { DotRouterService } from '@dotcms/app/api/services/dot-router/dot-router.service';
import { DataTableColumn } from '@dotcms/app/shared/models/data-table';
import { DotActionMenuItem } from '@dotcms/app/shared/models/dot-action-menu/dot-action-menu-item.model';
import { DotMessageService } from '@dotcms/data-access';

import { DotPagesState, DotPageStore } from './dot-pages-store/dot-pages.store';

@Component({
    selector: 'dot-pages',
    templateUrl: './dot-pages.component.html',
    styleUrls: ['./dot-pages.component.scss'],
    providers: [DotPageStore]
})
export class DotPagesComponent {
    vm$: Observable<DotPagesState> = this.store.vm$;

    cols: DataTableColumn[];
    dotStateLabels = {
        archived: this.dotMessageService.get('Archived'),
        published: this.dotMessageService.get('Published'),
        revision: this.dotMessageService.get('Revision'),
        draft: this.dotMessageService.get('Draft')
    };

    actions: DotActionMenuItem[] = [
        {
            menuItem: {
                label: this.dotMessageService.get('contenttypes.action.delete'),
                command: (item) => {
                    console.log(item);
                },
                icon: 'delete'
            },
            shouldShow: (item) => !item.fixed && !item.defaultType
        },
        {
            menuItem: {
                label: this.dotMessageService.get('contenttypes.action.create'),
                command: (item) => {
                    console.log(item);
                },
                icon: 'list'
            },
            shouldShow: (item) => !item.fixed && !item.defaultType
        }
    ];

    private initialFavoritePagesLimit = 5;

    constructor(
        private store: DotPageStore,
        private dotRouterService: DotRouterService,
        private dotMessageService: DotMessageService
    ) {
        this.store.setInitialStateData(this.initialFavoritePagesLimit);
    }

    /**
     * Event to load more/less Favorite page data
     *
     * @param {boolean} areAllFavoritePagesLoaded
     * @param {number} [favoritePagesToLoad]
     * @memberof DotPagesComponent
     */
    toggleFavoritePagesData(
        areAllFavoritePagesLoaded: boolean,
        favoritePagesToLoad?: number
    ): void {
        if (areAllFavoritePagesLoaded) {
            this.store.limitFavoritePages(this.initialFavoritePagesLimit);
        } else {
            this.store.getFavoritePages(favoritePagesToLoad);
        }
    }

    /**
     * Event to redirect to Edit Page with selected Favorite Page
     *
     * @param {string} url
     * @memberof DotPagesComponent
     */
    goToUrl(url: string) {
        console.log('***url', url);
        const splittedUrl = url.split('?');
        const urlParams = { url: splittedUrl[0] };
        const searchParams = new URLSearchParams(splittedUrl[1]);

        for (const entry of searchParams) {
            urlParams[entry[0]] = entry[1];
        }

        this.dotRouterService.goToEditPage(urlParams);
    }

    filterData(keyword: string) {
        console.log('***filterData', keyword);
        this.store.setFilter(keyword);
        this.store.getPages({ offset: 0 });
    }

    onRowSelect(event: Event) {
        this.goToUrl(event['data'].urlMap || event['data'].url);
    }

    // customSort(event: SortEvent) {
    //     // TODO: IMPLEMENTATION
    //     console.log('***sort', event);
    //     event.data.sort((data1, data2) => {
    //         const value1 = data1[event.field];
    //         const value2 = data2[event.field];
    //         let result = null;

    //         if (value1 == null && value2 != null) result = -1;
    //         else if (value1 != null && value2 == null) result = 1;
    //         else if (value1 == null && value2 == null) result = 0;
    //         else if (typeof value1 === 'string' && typeof value2 === 'string')
    //             result = value1.localeCompare(value2);
    //         else result = value1 < value2 ? -1 : value1 > value2 ? 1 : 0;

    //         return event.order * result;
    //     });
    // }

    loadPagesLazy(event: LazyLoadEvent) {
        this.store.getPages({
            offset: event.first >= 0 ? event.first : 0,
            sortField: event.sortField || '',
            sortOrder: event.sortOrder || null
        });
        //simulate remote connection with a timeout
        // setTimeout(() => {
        //   //load data of required page
        //   let loadedCars = this.cars.slice(event.first, event.first + event.rows);

        //   //populate page of virtual cars
        //   Array.prototype.splice.apply(this.virtualCars, [
        //     ...[event.first, event.rows],
        //     ...loadedCars,
        //   ]);
        // console.log('**** this.virtualCars', this.virtualCars)

        //   //trigger change detection
        //   event.forceUpdate();
        // }, Math.random() * 1000 + 250);
    }
}
