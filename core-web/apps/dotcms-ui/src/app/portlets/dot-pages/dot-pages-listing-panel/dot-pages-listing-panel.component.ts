import { CommonModule } from '@angular/common';
import {
    AfterViewInit,
    Component,
    HostListener,
    inject,
    input,
    OnDestroy,
    output,
    viewChild
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';

import { LazyLoadEvent } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { ContextMenu } from 'primeng/contextmenu';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { SkeletonModule } from 'primeng/skeleton';
import { Table, TableModule } from 'primeng/table';
import { TooltipModule } from 'primeng/tooltip';

import { DotMessageService } from '@dotcms/data-access';
import { DotCMSContentlet, DotLanguage } from '@dotcms/dotcms-models';
import { DotAutofocusDirective, DotMessagePipe, DotRelativeDatePipe } from '@dotcms/ui';

import { DotActionsMenuEventParams } from '../dot-pages.component';

@Component({
    selector: 'dot-pages-listing-panel',
    templateUrl: './dot-pages-listing-panel.component.html',
    styleUrls: ['./dot-pages-listing-panel.component.scss'],
    imports: [
        ButtonModule,
        CheckboxModule,
        CommonModule,
        FormsModule,
        DotAutofocusDirective,
        DotMessagePipe,
        DotRelativeDatePipe,
        SelectModule,
        InputTextModule,
        SkeletonModule,
        TableModule,
        TooltipModule,
        RouterModule,
        ContextMenu
    ]
})
export class DotPagesListingPanelComponent implements OnDestroy, AfterViewInit {
    readonly table = viewChild<Table>('table');

    readonly $pages = input.required<DotCMSContentlet[]>({ alias: 'pages' });
    readonly $languages = input.required<DotLanguage[]>({ alias: 'languages' });
    readonly $totalRecords = input.required<number>({ alias: 'totalRecords' });

    readonly goToUrl = output<string>();
    readonly showActionsMenu = output<DotActionsMenuEventParams>();
    readonly pageChange = output<void>();

    readonly #dotMessageService = inject(DotMessageService);
    #domIdMenuAttached = '';
    #scrollElement?: HTMLElement;

    dotStateLabels = {
        archived: this.#dotMessageService.get('Archived'),
        published: this.#dotMessageService.get('Published'),
        revision: this.#dotMessageService.get('Revision'),
        draft: this.#dotMessageService.get('Draft')
    };

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
    loadPagesLazy(_event: LazyLoadEvent): void {
        // this.store.getPages({
        //     offset: event.first >= 0 ? event.first : 0,
        //     sortField: event.sortField || '',
        //     sortOrder: event.sortOrder || null
        // });
    }

    /**
     * Event to show/hide actions menu when each contentlet is clicked
     *
     * @param {DotActionsMenuEventParams} params
     * @memberof DotPagesComponent
     */
    showActionsContextMenu({ event }: DotActionsMenuEventParams): void {
        event.stopPropagation();
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
    filterData(_keyword: string): void {
        // this.store.setKeyword(keyword);
        // this.store.getPages({ offset: 0 });
        // this.store.setSessionStorageFilterParams();
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
    setPagesLanguage(_languageId: string): void {
        // this.store.setLanguageId(languageId);
        // this.store.getPages({ offset: 0 });
        // this.store.setSessionStorageFilterParams();
    }

    /**
     * Event sets archived filter and loads data
     *
     * @param {string} archived
     * @memberof DotPagesListingPanelComponent
     */
    setPagesArchived(_archived: string): void {
        // this.store.setArchived(archived);
        // this.store.getPages({ offset: 0 });
        // this.store.setSessionStorageFilterParams();
    }

    /**
     * Closes the context menu when the user clicks outside of it
     *
     * @memberof DotPagesListingPanelComponent
     */
    @HostListener('window:click')
    closeContextMenu(): void {
        if (this.#domIdMenuAttached) {
            // this.cm()?.hide();
            // this.store.clearMenuActions();
            // this.#domIdMenuAttached = '';
        }
    }
}
