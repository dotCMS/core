/* eslint-disable no-console */
import { Component, OnInit } from '@angular/core';

import { SortEvent } from 'primeng/api';
import { LazyLoadEvent } from 'primeng/api/lazyloadevent';

import { Observable } from 'rxjs/internal/Observable';

import { DotRouterService } from '@dotcms/app/api/services/dot-router/dot-router.service';
import { DataTableColumn } from '@dotcms/app/shared/models/data-table';
import { DotMessageService } from '@dotcms/data-access';

import { DotPagesState, DotPageStore } from './dot-pages-store/dot-pages.store';

@Component({
    selector: 'dot-pages',
    templateUrl: './dot-pages.component.html',
    styleUrls: ['./dot-pages.component.scss'],
    providers: [DotPageStore]
})
export class DotPagesComponent implements OnInit {
    vm$: Observable<DotPagesState> = this.store.vm$;

    cols: DataTableColumn[];
    dotStateLabels = {
        archived: this.dotMessageService.get('Archived'),
        published: this.dotMessageService.get('Published'),
        revision: this.dotMessageService.get('Revision'),
        draft: this.dotMessageService.get('Draft')
    };

    private initialFavoritePagesLimit = 5;

    constructor(
        private store: DotPageStore,
        private dotRouterService: DotRouterService,
        private dotMessageService: DotMessageService
    ) {
        this.store.setInitialStateData(this.initialFavoritePagesLimit);
    }

    ngOnInit(): void {
        this.cols = [
            {
                fieldName: 'title',
                header: this.dotMessageService.get('title'),
                sortable: true
            },
            {
                fieldName: 'url',
                header: this.dotMessageService.get('url'),
                width: '8%'
            },
            {
                fieldName: '',
                header: this.dotMessageService.get('status')
            },
            {
                fieldName: 'contentType',
                header: this.dotMessageService.get('type')
            },
            {
                fieldName: 'languageId',
                header: this.dotMessageService.get('language')
            },
            {
                fieldName: 'modUserName',
                header: this.dotMessageService.get('Last-Editor')
            },
            {
                fieldName: 'modDate',
                format: 'date',
                header: this.dotMessageService.get('Last-Editor-Date'),
                sortable: true
            }
        ];
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
        const splittedUrl = url.split('?');
        const urlParams = { url: splittedUrl[0] };
        const searchParams = new URLSearchParams(splittedUrl[1]);

        for (const entry of searchParams) {
            urlParams[entry[0]] = entry[1];
        }

        this.dotRouterService.goToEditPage(urlParams);
    }

    customSort(event: SortEvent) {
        console.log('***sort', event);
        event.data.sort((data1, data2) => {
            const value1 = data1[event.field];
            const value2 = data2[event.field];
            let result = null;

            if (value1 == null && value2 != null) result = -1;
            else if (value1 != null && value2 == null) result = 1;
            else if (value1 == null && value2 == null) result = 0;
            else if (typeof value1 === 'string' && typeof value2 === 'string')
                result = value1.localeCompare(value2);
            else result = value1 < value2 ? -1 : value1 > value2 ? 1 : 0;

            return event.order * result;
        });
    }

    loadPagesLazy(event: LazyLoadEvent) {
        console.log('***loadPagesLazy', event);
        this.store.getPages(event.first >= 0 ? event.first : 0);
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
