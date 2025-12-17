import { CommonModule } from '@angular/common';
import { Component, computed, inject, input, output } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';

import { LazyLoadEvent } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { SkeletonModule } from 'primeng/skeleton';
import { TableModule } from 'primeng/table';
import { TooltipModule } from 'primeng/tooltip';

import { debounceTime, distinctUntilChanged, startWith } from 'rxjs/operators';

import { DotMessageService } from '@dotcms/data-access';
import { DotCMSContentlet, DotSystemLanguage } from '@dotcms/dotcms-models';
import { DotAutofocusDirective, DotMessagePipe, DotRelativeDatePipe } from '@dotcms/ui';

import { DotActionsMenuEventParams } from '../dot-pages.component';

type LanguageOption = {
    label: string;
    value: string | number;
};

type TableRowSelectEvent<T> = {
    data: T;
};

@Component({
    selector: 'dot-pages-table',
    templateUrl: './dot-pages-table.component.html',
    styleUrls: ['./dot-pages-table.component.scss'],
    imports: [
        ButtonModule,
        CheckboxModule,
        CommonModule,
        DotAutofocusDirective,
        DotMessagePipe,
        DotRelativeDatePipe,
        SelectModule,
        InputTextModule,
        SkeletonModule,
        TableModule,
        TooltipModule,
        RouterModule,
        ReactiveFormsModule
    ]
})
export class DotPagesTableComponent {
    readonly #dotMessageService = inject(DotMessageService);

    /** The pages to display. */
    readonly $pages = input.required<DotCMSContentlet[]>({ alias: 'pages' });
    /** The languages to display. */
    readonly $languages = input.required<DotSystemLanguage[]>({ alias: 'languages' });
    /** The total number of records. */
    readonly $totalRecords = input.required<number>({ alias: 'totalRecords' });

    /** Emits a navigation URL when the user selects a row. */
    readonly goToUrl = output<string>();
    /** Emits when the actions menu should be opened for a row. */
    readonly showContextMenu = output<DotActionsMenuEventParams>();
    /** Emits when the paginator changes page. */
    readonly pageChange = output<void>();
    /** Emits when the user clicks the create-page button. */
    readonly createPage = output<void>();

    /** Emits the current search term as the user types */
    readonly search = output<string>();
    /** Emits the selected language id (or 'all') */
    readonly languageChange = output<string | number>();
    /** Emits whether archived pages should be shown */
    readonly archivedChange = output<boolean>();
    /** Emits PrimeNG lazy load event (pagination + sort changes) */
    readonly lazyLoad = output<LazyLoadEvent>();

    // Reactive "dynamic" filters form
    /** Search keyword control (debounced before emitting). */
    readonly searchControl = new FormControl<string>('', { nonNullable: true });
    /** Selected language id control. */
    readonly languageControl = new FormControl<string | number>('all', { nonNullable: true });
    /** Archived toggle control. */
    readonly archivedControl = new FormControl<boolean>(false, { nonNullable: true });

    /**
     * Computed property for the language options
     * @returns The language options
     */
    readonly $languageOptions = computed<LanguageOption[]>(() => {
        const availableLanguages: LanguageOption[] = this.$languages().map((language) => ({
            label: `${language.language}${language.countryCode ? ` (${language.countryCode})` : ''}`,
            value: language.id
        }));

        return [{ label: 'All', value: 'all' }, ...availableLanguages];
    });

    /**
     * Map of languageId -> ISO code
     */
    readonly $languageIsoCodeById = computed<Record<number, string>>(() => {
        return this.$languages().reduce<Record<number, string>>((acc, language) => {
            // DotSystemLanguage.isoCode is the server-provided ISO code
            acc[language.id] = language.isoCode ?? '';
            return acc;
        }, {});
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

    /**
     * Wires up reactive filters:
     * - Search is debounced (300ms) before emitting `search`
     * - Language emits immediately via `languageChange`
     * - Archived emits immediately via `archivedChange`
     */
    constructor() {
        this.searchControl.valueChanges
            .pipe(
                startWith(this.searchControl.value),
                debounceTime(300),
                distinctUntilChanged(),
                takeUntilDestroyed()
            )
            .subscribe((keyword) => this.search.emit(keyword));

        this.languageControl.valueChanges
            .pipe(distinctUntilChanged(), takeUntilDestroyed())
            .subscribe((languageId) => this.languageChange.emit(languageId));

        this.archivedControl.valueChanges
            .pipe(distinctUntilChanged(), takeUntilDestroyed())
            .subscribe((archived) => this.archivedChange.emit(archived));
    }

    /**
     * Emits PrimeNG's lazy-load event (pagination + sort) so the parent can fetch new data.
     *
     * @param {LazyLoadEvent} event - PrimeNG table lazy-load event
     */
    loadPagesLazy(event: LazyLoadEvent): void {
        this.lazyLoad.emit(event);
    }

    /**
     * Stops row click propagation when opening the actions context menu.
     *
     * @param {DotActionsMenuEventParams} params - click event + row metadata
     */
    showActionsContextMenu({ event }: DotActionsMenuEventParams): void {
        event.stopPropagation();
    }

    /**
     * Emits a constructed edit URL when the user selects a row.
     *
     * @param {TableRowSelectEvent<DotCMSContentlet>} event - PrimeNG row select event
     */
    onRowSelect(event: TableRowSelectEvent<DotCMSContentlet>): void {
        const data = event?.data as DotCMSContentlet & {
            url?: string;
            urlMap?: string;
            languageId?: string | number;
        };
        const urlValue = data.urlMap || data.url || '';
        const languageId = data.languageId ?? '';
        const url = `${urlValue}?language_id=${languageId}&device_inode=`;

        this.goToUrl.emit(url);
    }

    // Note: output emission is handled via the reactive form control subscriptions above.
}
