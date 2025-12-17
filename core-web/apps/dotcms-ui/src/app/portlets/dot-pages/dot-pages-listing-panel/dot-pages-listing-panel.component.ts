import { Subject } from 'rxjs';

import { CommonModule } from '@angular/common';
import { Component, computed, DestroyRef, inject, input, output } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';

import { LazyLoadEvent } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { SkeletonModule } from 'primeng/skeleton';
import { TableModule } from 'primeng/table';
import { TooltipModule } from 'primeng/tooltip';

import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

import { DotMessageService } from '@dotcms/data-access';
import { DotCMSContentlet, DotSystemLanguage } from '@dotcms/dotcms-models';
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
        RouterModule
    ]
})
export class DotPagesListingPanelComponent {
    readonly #dotMessageService = inject(DotMessageService);
    readonly #destroyRef = inject(DestroyRef);
    readonly #searchTerm$ = new Subject<string>();

    readonly $pages = input.required<DotCMSContentlet[]>({ alias: 'pages' });
    readonly $languages = input.required<DotSystemLanguage[]>({ alias: 'languages' });
    readonly $totalRecords = input.required<number>({ alias: 'totalRecords' });

    readonly goToUrl = output<string>();
    readonly showActionsMenu = output<DotActionsMenuEventParams>();
    readonly pageChange = output<void>();

    /** Emits the current search term as the user types */
    readonly search = output<string>();
    /** Emits the selected language id (or 'all') */
    readonly languageChange = output<string | number>();
    /** Emits whether archived pages should be shown */
    readonly archivedChange = output<boolean>();
    /** Emits PrimeNG lazy load event (pagination + sort changes) */
    readonly lazyLoad = output<LazyLoadEvent>();

    /**
     * Computed property for the language options
     * @returns The language options
     */
    readonly $languageOptions = computed(() => {
        const availableLanguages = this.$languages().map((language) => ({
            label: `${language.language} (${language.countryCode})`,
            value: language.id
        }));
        return [{ label: 'All', value: 'all' }, ...availableLanguages];
    });

    /**
     * Computed property for the dot state labels
     * @returns The dot state labels
     */
    readonly dotStateLabels = {
        archived: this.#dotMessageService.get('Archived'),
        published: this.#dotMessageService.get('Published'),
        revision: this.#dotMessageService.get('Revision'),
        draft: this.#dotMessageService.get('Draft')
    };

    constructor() {
        this.#searchTerm$
            .pipe(debounceTime(300), distinctUntilChanged(), takeUntilDestroyed(this.#destroyRef))
            .subscribe((keyword) => this.search.emit(keyword));
    }

    /**
     * Event lazy loads pages data
     *
     * @param {LazyLoadEvent} event
     * @memberof DotPagesListingPanelComponent
     */
    loadPagesLazy(event: LazyLoadEvent): void {
        this.lazyLoad.emit(event);
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
     * Event sets filter and loads data
     *
     * @param {string} keyword
     * @memberof DotPagesListingPanelComponent
     */
    onSearch(keyword: string): void {
        this.#searchTerm$.next(keyword);
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
    setPagesLanguage(languageId: string | number): void {
        this.languageChange.emit(languageId);
    }

    /**
     * Event sets archived filter and loads data
     *
     * @param {string} archived
     * @memberof DotPagesListingPanelComponent
     */
    setPagesArchived(archived: boolean): void {
        this.archivedChange.emit(archived);
    }
}
