import { Observable } from 'rxjs';

import { Component, EventEmitter, Output } from '@angular/core';

import { DialogService } from 'primeng/dynamicdialog';

import { DotMessageService } from '@dotcms/data-access';
import { DotCMSContentlet } from '@dotcms/dotcms-models';

import { DotFavoritePageComponent } from '../../dot-edit-page/components/dot-favorite-page/dot-favorite-page.component';
import { DotPagesState, DotPageStore } from '../dot-pages-store/dot-pages.store';
import { DotActionsMenuEventParams, FAVORITE_PAGE_LIMIT } from '../dot-pages.component';

@Component({
    selector: 'dot-pages-favorite-panel',
    templateUrl: './dot-pages-favorite-panel.component.html',
    styleUrls: ['./dot-pages-favorite-panel.component.scss']
})
export class DotPagesFavoritePanelComponent {
    @Output() goToUrl = new EventEmitter<string>();
    @Output() showActionsMenu = new EventEmitter<DotActionsMenuEventParams>();

    vm$: Observable<DotPagesState> = this.store.vm$;

    timeStamp = this.getTimeStamp();

    private currentLimitSize = FAVORITE_PAGE_LIMIT;

    constructor(
        private store: DotPageStore,
        private dotMessageService: DotMessageService,
        private dialogService: DialogService
    ) {}

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
