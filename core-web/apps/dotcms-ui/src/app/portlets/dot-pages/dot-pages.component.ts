import { Component } from '@angular/core';
import { Observable } from 'rxjs/internal/Observable';
import { DotPagesState, DotPageStore } from './dot-pages-store/dot-pages.store';

@Component({
    selector: 'dot-pages',
    templateUrl: './dot-pages.component.html',
    styleUrls: ['./dot-pages.component.scss'],
    providers: [DotPageStore]
})
export class DotPagesComponent {
    vm$: Observable<DotPagesState> = this.store.vm$;

    private initialFavoritePagesLimit = 5;

    constructor(private store: DotPageStore) {
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
}
