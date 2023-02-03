/* eslint-disable no-console */

import { Subject } from 'rxjs';

import { Component, ElementRef, OnDestroy, OnInit, ViewChild } from '@angular/core';

import { MenuItem, SelectItem } from 'primeng/api';
import { LazyLoadEvent } from 'primeng/api/lazyloadevent';
import { Menu } from 'primeng/menu';

import { Observable } from 'rxjs/internal/Observable';
import { takeUntil } from 'rxjs/operators';

import { DotMessageSeverity, DotMessageType } from '@components/dot-message-display/model';
import { DotMessageDisplayService } from '@components/dot-message-display/services';
import { DotRouterService } from '@dotcms/app/api/services/dot-router/dot-router.service';
import { DataTableColumn } from '@dotcms/app/shared/models/data-table';
import { DotEventsService, DotMessageService } from '@dotcms/data-access';
import { DotCMSContentlet } from '@dotcms/dotcms-models';

import { DotPagesState, DotPageStore } from './dot-pages-store/dot-pages.store';

@Component({
    selector: 'dot-pages',
    templateUrl: './dot-pages.component.html',
    styleUrls: ['./dot-pages.component.scss'],
    providers: [DotPageStore]
})
export class DotPagesComponent implements OnInit, OnDestroy {
    @ViewChild('menu') menu: Menu;
    vm$: Observable<DotPagesState> = this.store.vm$;

    langOptions: SelectItem[];

    cols: DataTableColumn[];
    dotStateLabels = {
        archived: this.dotMessageService.get('Archived'),
        published: this.dotMessageService.get('Published'),
        revision: this.dotMessageService.get('Revision'),
        draft: this.dotMessageService.get('Draft')
    };

    actions: { [key: string]: MenuItem[] };

    private initialFavoritePagesLimit = 5;
    private domIdMenuAttached = '';
    private destroy$: Subject<boolean> = new Subject<boolean>();

    constructor(
        private store: DotPageStore,
        private dotRouterService: DotRouterService,
        private dotMessageService: DotMessageService,
        private dotMessageDisplayService: DotMessageDisplayService,
        private dotEventsService: DotEventsService,
        private element: ElementRef
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
        this.store.setKeyword(keyword);
        this.store.getPages({ offset: 0 });
    }

    onRowSelect(event: Event) {
        this.goToUrl(event['data'].urlMap || event['data'].url);
    }

    setPagesLanguage(languageId: string) {
        console.log('===langid', languageId);
        this.store.setLanguageId(languageId);
        this.store.getPages({ offset: 0 });
    }

    setPagesArchived(archived: string) {
        console.log('===archived', archived);
        this.store.setArchived(archived);
        this.store.getPages({ offset: 0 });
    }

    showActionsMenu(event: MouseEvent, actionMenuDomId: string, item: DotCMSContentlet) {
        event.stopPropagation();

        if (event.currentTarget['id'] === this.domIdMenuAttached) {
            this.menu.hide();
        }

        if (event.currentTarget['id'] !== this.domIdMenuAttached) {
            this.menu.hide();
            this.store.showActionsMenu({ item, actionMenuDomId });
        }
    }

    closedActionsMenu(event: Event) {
        console.log(event);
        this.store.clearMenuActions();
        this.domIdMenuAttached = '';
    }

    loadPagesLazy(event: LazyLoadEvent) {
        this.store.getPages({
            offset: event.first >= 0 ? event.first : 0,
            sortField: event.sortField || '',
            sortOrder: event.sortOrder || null
        });
    }

    ngOnInit(): void {
        this.store.actionMenuDomId$
            .pipe(takeUntil(this.destroy$))
            .subscribe((actionMenuDomId: string) => {
                if (actionMenuDomId !== undefined) {
                    const target = this.element.nativeElement.querySelector(`#${actionMenuDomId}`);
                    console.log('***rowIndex', actionMenuDomId, target);
                    this.domIdMenuAttached = actionMenuDomId;
                    this.menu.show({ currentTarget: target });
                }
            });

        this.dotEventsService
            .listen('dot-global-message')
            .pipe(takeUntil(this.destroy$))
            .subscribe((evt) => {
                console.log('---- dot-global-message', evt);

                this.store.getPages({ offset: 0 });

                this.dotMessageDisplayService.push({
                    life: 3000,
                    message: evt.data['value'],
                    severity: DotMessageSeverity.SUCCESS,
                    type: DotMessageType.SIMPLE_MESSAGE
                });
            });
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }
}
