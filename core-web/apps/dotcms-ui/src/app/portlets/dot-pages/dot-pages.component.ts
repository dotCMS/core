import { Component, OnInit } from '@angular/core';

import { LazyLoadEvent } from 'primeng/api/lazyloadevent';

import { Observable } from 'rxjs/internal/Observable';

import { DotRouterService } from '@dotcms/app/api/services/dot-router/dot-router.service';

import { DotPagesState, DotPageStore } from './dot-pages-store/dot-pages.store';

@Component({
    selector: 'dot-pages',
    templateUrl: './dot-pages.component.html',
    styleUrls: ['./dot-pages.component.scss'],
    providers: [DotPageStore]
})
export class DotPagesComponent implements OnInit {
    vm$: Observable<DotPagesState> = this.store.vm$;

    cols: { field: string; header: string }[];

    private initialFavoritePagesLimit = 5;

    constructor(private store: DotPageStore, private dotRouterService: DotRouterService) {
        this.store.setInitialStateData(this.initialFavoritePagesLimit);
    }

    ngOnInit(): void {
        this.cols = [
            { field: 'title', header: 'Title' },
            { field: 'url', header: 'Url' },
            { field: 'languageId', header: 'languageId' }
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

    loadPagesLazy(event: LazyLoadEvent) {
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
