import { CommonModule } from '@angular/common';
import { HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { Component, inject, input, output, signal } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { DialogService } from 'primeng/dynamicdialog';
import { PanelModule } from 'primeng/panel';

import {
    DotHttpErrorManagerService,
    DotLocalstorageService,
    DotMessageService,
    DotPageRenderService
} from '@dotcms/data-access';
import { HttpCode } from '@dotcms/dotcms-js';
import { DotCMSContentlet } from '@dotcms/dotcms-models';
import { DotFavoritePageComponent } from '@dotcms/portlets/dot-ema/ui';
import { DotMessagePipe } from '@dotcms/ui';

import { DotPagesCardComponent } from './dot-pages-card/dot-pages-card.component';

import { LOCAL_STORAGE_FAVORITES_PANEL_KEY } from '../dot-pages-store/dot-pages.store';
import { DotActionsMenuEventParams } from '../dot-pages.component';

@Component({
    selector: 'dot-page-favorites-panel',
    templateUrl: './dot-page-favorites-panel.component.html',
    styleUrls: ['./dot-page-favorites-panel.component.scss'],
    imports: [CommonModule, DotMessagePipe, DotPagesCardComponent, PanelModule, ButtonModule]
})
export class DotPageFavoritesPanelComponent {
    readonly #dotMessageService = inject(DotMessageService);
    readonly #dialogService = inject(DialogService);
    readonly #dotPageRenderService = inject(DotPageRenderService);
    readonly #dotHttpErrorManagerService = inject(DotHttpErrorManagerService);
    readonly #dotLocalstorageService = inject(DotLocalstorageService);

    readonly $favoritePages = input<DotCMSContentlet[]>([], { alias: 'favoritePages' });
    readonly goToUrl = output<string>();
    readonly showContextMenu = output<DotActionsMenuEventParams>();

    readonly $isCollapsed = signal<boolean>(true);
    readonly $timeStamp = signal<string>(new Date().getTime().toString());

    constructor() {
        const isCollapsed = this.#dotLocalstorageService.getItem<boolean>(
            LOCAL_STORAGE_FAVORITES_PANEL_KEY
        );
        this.$isCollapsed.set(isCollapsed);
    }

    /**
     * Builds the screenshot URL for a favorite page card.
     * Keeps the template clean and centralizes the query-param formatting.
     *
     * @param {DotCMSContentlet} favoritePage - The favorite page contentlet
     * @returns {string} The screenshot URL with cache-busting params, or empty string if missing.
     */
    getScreenshotUri(favoritePage: DotCMSContentlet): string {
        if (!favoritePage?.screenshot) {
            return '';
        }

        return `${favoritePage.screenshot}?language_id=${favoritePage.languageId}&${this.$timeStamp()}`;
    }

    /**
     * Event to collapse or not Favorite Page panel
     *
     * @param {Event} event
     * @memberof DotPagesComponent
     */
    onToggleChange(collapsed: boolean): void {
        if (collapsed) {
            this.collapsePanel();
        } else {
            this.expandPanel();
        }
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

        this.#dotPageRenderService.checkPermission(urlParams).subscribe(
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
                    this.#dotHttpErrorManagerService.handle(error);
                }
            },
            () => {
                this.displayFavoritePageDialog(favoritePage);
            }
        );
    }

    /**
     * Collapse the favorite pages panel
     * @memberof DotPagesFavoritePanelComponent
     */
    collapsePanel(): void {
        this.$isCollapsed.set(true);
        this.#dotLocalstorageService.setItem(LOCAL_STORAGE_FAVORITES_PANEL_KEY, 'true');
    }

    /**
     * Expand the favorite pages panel
     * @memberof DotPagesFavoritePanelComponent
     */
    expandPanel(): void {
        this.$isCollapsed.set(false);
        this.#dotLocalstorageService.setItem(LOCAL_STORAGE_FAVORITES_PANEL_KEY, 'false');
    }

    private displayFavoritePageDialog(favoritePage: DotCMSContentlet) {
        const timeStamp = new Date().getTime().toString();
        this.#dialogService.open(DotFavoritePageComponent, {
            header: this.#dotMessageService.get('favoritePage.dialog.header'),
            width: '80rem',
            data: {
                page: {
                    favoritePageUrl: favoritePage.url,
                    favoritePage: favoritePage
                },
                onSave: () => {
                    this.$timeStamp.set(timeStamp);
                    // this.#store.getFavoritePages(this.currentLimitSize);
                },
                onDelete: () => {
                    this.$timeStamp.set(timeStamp);
                    // this.#store.getFavoritePages(this.currentLimitSize);
                }
            }
        });
    }
}
