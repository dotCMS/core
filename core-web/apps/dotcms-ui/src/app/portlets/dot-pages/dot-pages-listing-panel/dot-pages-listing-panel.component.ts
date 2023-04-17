import { Observable } from 'rxjs';

import { Component, EventEmitter, Output } from '@angular/core';

import { LazyLoadEvent } from 'primeng/api';

import { DotMessageService } from '@dotcms/data-access';

import { DotPagesState, DotPageStore } from '../dot-pages-store/dot-pages.store';
import { DotActionsMenuEventParams } from '../dot-pages.component';

@Component({
    selector: 'dot-pages-listing-panel',
    templateUrl: './dot-pages-listing-panel.component.html',
    styleUrls: ['./dot-pages-listing-panel.component.scss']
})
export class DotPagesListingPanelComponent {
    @Output() goToUrl = new EventEmitter<string>();
    @Output() showActionsMenu = new EventEmitter<DotActionsMenuEventParams>();

    vm$: Observable<DotPagesState> = this.store.vm$;

    dotStateLabels = {
        archived: this.dotMessageService.get('Archived'),
        published: this.dotMessageService.get('Published'),
        revision: this.dotMessageService.get('Revision'),
        draft: this.dotMessageService.get('Draft')
    };

    constructor(private store: DotPageStore, private dotMessageService: DotMessageService) {}

    /**
     * Event lazy loads pages data
     *
     * @param {LazyLoadEvent} event
     * @memberof DotPagesListingPanelComponent
     */
    loadPagesLazy(event: LazyLoadEvent): void {
        this.store.getPages({
            offset: event.first >= 0 ? event.first : 0,
            sortField: event.sortField || '',
            sortOrder: event.sortOrder || null
        });
    }

    /**
     * Event sets filter and loads data
     *
     * @param {string} keyword
     * @memberof DotPagesListingPanelComponent
     */
    filterData(keyword: string): void {
        this.store.setKeyword(keyword);
        this.store.getPages({ offset: 0 });
    }

    /**
     * Event sends url to redirect to EDIT mode page
     *
     * @param {Event} event
     * @memberof DotPagesListingPanelComponent
     */
    onRowSelect(event: Event): void {
        const url = `${event['data'].urlMap || event['data'].url}?language_id=${
            event['data'].languageId
        }&device_inode=`;

        this.goToUrl.emit(url);
    }

    /**
     * Event sets language filter and loads data
     *
     * @param {string} languageId
     * @memberof DotPagesListingPanelComponent
     */
    setPagesLanguage(languageId: string): void {
        this.store.setLanguageId(languageId);
        this.store.getPages({ offset: 0 });
    }

    /**
     * Event sets archived filter and loads data
     *
     * @param {string} archived
     * @memberof DotPagesListingPanelComponent
     */
    setPagesArchived(archived: string): void {
        this.store.setArchived(archived);
        this.store.getPages({ offset: 0 });
    }
}
