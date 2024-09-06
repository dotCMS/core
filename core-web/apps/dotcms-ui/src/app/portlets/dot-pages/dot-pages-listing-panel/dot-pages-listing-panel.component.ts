import { Observable } from 'rxjs';

import {
    AfterViewInit,
    Component,
    EventEmitter,
    HostListener,
    inject,
    OnDestroy,
    Output,
    ViewChild
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { LazyLoadEvent } from 'primeng/api';
import { ContextMenu } from 'primeng/contextmenu';
import { Table } from 'primeng/table';

import { filter } from 'rxjs/operators';

import { DotMessageService } from '@dotcms/data-access';

import { DotPagesState, DotPageStore } from '../dot-pages-store/dot-pages.store';
import { DotActionsMenuEventParams } from '../dot-pages.component';

@Component({
    selector: 'dot-pages-listing-panel',
    templateUrl: './dot-pages-listing-panel.component.html',
    styleUrls: ['./dot-pages-listing-panel.component.scss']
})
export class DotPagesListingPanelComponent implements OnDestroy, AfterViewInit {
    readonly store = inject(DotPageStore);
    readonly #dotMessageService = inject(DotMessageService);

    @ViewChild('cm') cm: ContextMenu;
    @ViewChild('table') table: Table;
    @Output() goToUrl = new EventEmitter<string>();
    @Output() showActionsMenu = new EventEmitter<DotActionsMenuEventParams>();
    @Output() pageChange = new EventEmitter<void>();
    vm$: Observable<DotPagesState> = this.store.vm$;
    dotStateLabels = {
        archived: this.#dotMessageService.get('Archived'),
        published: this.#dotMessageService.get('Published'),
        revision: this.#dotMessageService.get('Revision'),
        draft: this.#dotMessageService.get('Draft')
    };
    #domIdMenuAttached = '';
    #scrollElement?: HTMLElement;

    constructor() {
        this.store.actionMenuDomId$
            .pipe(
                takeUntilDestroyed(),
                filter((actionMenuDomId) => !!actionMenuDomId)
            )
            .subscribe((actionMenuDomId: string) => {
                if (actionMenuDomId.includes('tableRow')) {
                    this.cm.show(new Event('click'));
                    this.#domIdMenuAttached = actionMenuDomId;
                    // To hide when the menu is opened
                } else this.cm.hide();
            });
    }

    ngAfterViewInit(): void {
        this.#scrollElement = document.querySelector('dot-pages');

        this.#scrollElement?.addEventListener('scroll', () => {
            this.closeContextMenu();
        });
    }

    ngOnDestroy(): void {
        this.#scrollElement?.removeAllListeners('scroll');
    }

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
     * Event to show/hide actions menu when each contentlet is clicked
     *
     * @param {DotActionsMenuEventParams} params
     * @memberof DotPagesComponent
     */
    showActionsContextMenu({ event, actionMenuDomId, item }: DotActionsMenuEventParams): void {
        event.stopPropagation();
        this.store.clearMenuActions();
        this.cm.hide();

        this.store.showActionsMenu({ item, actionMenuDomId });
    }

    /**
     * Event to reset status of menu actions when closed
     *
     * @memberof DotPagesComponent
     */
    closedActionsContextMenu() {
        this.#domIdMenuAttached = '';
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
        this.store.setSessionStorageFilterParams();
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
        this.store.setSessionStorageFilterParams();
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
        this.store.setSessionStorageFilterParams();
    }

    /**
     * Closes the context menu when the user clicks outside of it
     *
     * @memberof DotPagesListingPanelComponent
     */
    @HostListener('window:click')
    private closeContextMenu(): void {
        if (this.#domIdMenuAttached.includes('tableRow')) {
            this.cm.hide();
            this.store.clearMenuActions();
        }
    }
}
