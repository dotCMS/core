import { Observable } from 'rxjs';

import { HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { Component, EventEmitter, OnInit, Output } from '@angular/core';

import { DialogService } from 'primeng/dynamicdialog';

import { DotHttpErrorManagerService } from '@dotcms/app/api/services/dot-http-error-manager/dot-http-error-manager.service';
import { DotMessageService, DotPageRenderService } from '@dotcms/data-access';
import { HttpCode } from '@dotcms/dotcms-js';
import { DotCMSContentlet } from '@dotcms/dotcms-models';

import { DotFavoritePageComponent } from '../../dot-edit-page/components/dot-favorite-page/dot-favorite-page.component';
import { DotPagesState, DotPageStore } from '../dot-pages-store/dot-pages.store';
import { DotActionsMenuEventParams } from '../dot-pages.component';

@Component({
    selector: 'dot-pages-favorite-panel',
    templateUrl: './dot-pages-favorite-panel.component.html',
    styleUrls: ['./dot-pages-favorite-panel.component.scss']
})
export class DotPagesFavoritePanelComponent implements OnInit {
    @Output() goToUrl = new EventEmitter<string>();
    @Output() showActionsMenu = new EventEmitter<DotActionsMenuEventParams>();

    vm$: Observable<DotPagesState> = this.store.vm$;

    timeStamp = this.getTimeStamp();

    constructor(
        private store: DotPageStore,
        private dotMessageService: DotMessageService,
        private dialogService: DialogService,
        private dotPageRenderService: DotPageRenderService,
        private dotHttpErrorManagerService: DotHttpErrorManagerService
    ) {}

    ngOnInit(): void {
        this.store.getFavoritePages({ fetchAll: true });
    }

    /**
     * Event to collapse or not Favorite Page panel
     *
     * @param {Event} event
     * @memberof DotPagesComponent
     */
    toggleFavoritePagesPanel($event: Event): void {
        this.store.setLocalStorageFavoritePanelCollapsedParams($event['collapsed']);
        this.store.setFavoritePages({ collapsed: $event['collapsed'] as boolean });
    }

    /**
     * Event that opens dialog to edit/delete Favorite Page
     *
     * @param {DotCMSContentlet} favoritePage
     * @memberof DotPagesComponent
     */
    editFavoritePage(favoritePage: DotCMSContentlet) {
        const url = `${favoritePage.urlMap || favoritePage.url}?host_id=${
            favoritePage.host
        }&language_id=${favoritePage.languageId}`;

        const urlParams = { url: url.split('?')[0] };
        const searchParams = new URLSearchParams(url.split('?')[1]);

        for (const entry of searchParams) {
            urlParams[entry[0]] = entry[1];
        }

        this.dotPageRenderService.checkPermission(urlParams).subscribe(
            (hasPermission: boolean) => {
                if (hasPermission) {
                    this.displayFavoritePageDialog(favoritePage);
                } else {
                    const error = new HttpErrorResponse(
                        new HttpResponse({
                            body: null,
                            status: HttpCode.FORBIDDEN,
                            headers: null,
                            url: ''
                        })
                    );
                    this.dotHttpErrorManagerService.handle(error);
                }
            },
            () => {
                this.displayFavoritePageDialog(favoritePage);
            }
        );
    }

    private displayFavoritePageDialog(favoritePage: DotCMSContentlet) {
        this.dialogService.open(DotFavoritePageComponent, {
            header: this.dotMessageService.get('favoritePage.dialog.header'),
            width: '80rem',
            data: {
                page: {
                    favoritePageUrl: favoritePage.url,
                    favoritePage: favoritePage
                },
                onSave: () => {
                    this.timeStamp = this.getTimeStamp();
                    this.store.getFavoritePages({ fetchAll: true });
                },
                onDelete: () => {
                    this.timeStamp = this.getTimeStamp();
                    this.store.getFavoritePages({ fetchAll: true });
                }
            }
        });
    }

    private getTimeStamp() {
        return new Date().getTime().toString();
    }
}
