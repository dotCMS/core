import { Component } from '@angular/core';

import { DialogService } from 'primeng/dynamicdialog';

import { Observable } from 'rxjs/internal/Observable';

import { DotRouterService } from '@dotcms/app/api/services/dot-router/dot-router.service';
import { DotMessageService } from '@dotcms/data-access';
import { DotCMSContentlet } from '@dotcms/dotcms-models';

import { DotPagesState, DotPageStore } from './dot-pages-store/dot-pages.store';

import { DotFavoritePageComponent } from '../dot-edit-page/components/dot-favorite-page/dot-favorite-page.component';

const FAVORITE_PAGE_LIMIT = 5;
@Component({
    selector: 'dot-pages',
    templateUrl: './dot-pages.component.html',
    styleUrls: ['./dot-pages.component.scss'],
    providers: [DotPageStore]
})
export class DotPagesComponent {
    vm$: Observable<DotPagesState> = this.store.vm$;

    // Needed to avoid browser to cache thumbnail img when reloaded, since it's fetched from the same URL
    timeStamp = this.getTimeStamp();

    private currentLimitSize = FAVORITE_PAGE_LIMIT;

    constructor(
        private store: DotPageStore,
        private dotRouterService: DotRouterService,
        private dialogService: DialogService,
        private dotMessageService: DotMessageService
    ) {
        this.store.setInitialStateData(FAVORITE_PAGE_LIMIT);
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
            this.store.limitFavoritePages(FAVORITE_PAGE_LIMIT);
        } else {
            this.store.getFavoritePages(favoritePagesToLoad);
        }

        this.currentLimitSize = FAVORITE_PAGE_LIMIT;
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

    /**
     * Event that opens dialog to edit/delete Favorite Page
     *
     * @param {DotCMSContentlet} favoritePage
     * @memberof DotPagesComponent
     */
    editFavoritePage(favoritePage: DotCMSContentlet) {
        this.dialogService.open(DotFavoritePageComponent, {
            header: this.dotMessageService.get('favoritePage.dialog.header.add.page'),
            width: '80rem',
            data: {
                page: {
                    favoritePageUrl: favoritePage.url,
                    favoritePage: favoritePage
                },
                onSave: () => {
                    this.timeStamp = this.getTimeStamp();
                    this.store.getFavoritePages(this.currentLimitSize);
                },
                onDelete: () => {
                    this.timeStamp = this.getTimeStamp();
                    this.store.getFavoritePages(this.currentLimitSize);
                }
            }
        });
    }

    private getTimeStamp() {
        return new Date().getTime().toString();
    }
}
