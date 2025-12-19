import { CommonModule } from '@angular/common';
import { Component, inject, input, output, signal } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { PanelModule } from 'primeng/panel';

import { DotLocalstorageService } from '@dotcms/data-access';
import { DotCMSContentlet } from '@dotcms/dotcms-models';
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
    readonly #dotLocalstorageService = inject(DotLocalstorageService);

    readonly $favoritePages = input<DotCMSContentlet[]>([], { alias: 'favoritePages' });
    readonly navigateToPage = output<string>();
    readonly openMenu = output<DotActionsMenuEventParams>();

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
    protected getScreenshotUri(favoritePage: DotCMSContentlet): string {
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
    protected onToggleChange(collapsed: boolean): void {
        if (collapsed) {
            this.collapsePanel();
        } else {
            this.expandPanel();
        }
    }

    /**
     * Collapse the favorite pages panel
     * @memberof DotPagesFavoritePanelComponent
     */
    protected collapsePanel(): void {
        this.$isCollapsed.set(true);
        this.#dotLocalstorageService.setItem(LOCAL_STORAGE_FAVORITES_PANEL_KEY, 'true');
    }

    /**
     * Expand the favorite pages panel
     * @memberof DotPagesFavoritePanelComponent
     */
    protected expandPanel(): void {
        this.$isCollapsed.set(false);
        this.#dotLocalstorageService.setItem(LOCAL_STORAGE_FAVORITES_PANEL_KEY, 'false');
    }

    protected handleOpenMenu(originalEvent: MouseEvent, data: DotCMSContentlet): void {
        originalEvent.stopPropagation();
        this.openMenu.emit({ originalEvent, data });
    }
}
