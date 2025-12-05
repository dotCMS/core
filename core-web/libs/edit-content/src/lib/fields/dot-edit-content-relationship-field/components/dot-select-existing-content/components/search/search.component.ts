import { Component, inject, input, output, viewChild, signal, computed, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { ChipModule } from 'primeng/chip';
import { InputGroupModule } from 'primeng/inputgroup';
import { InputGroupAddonModule } from 'primeng/inputgroupaddon';
import { InputTextModule } from 'primeng/inputtext';
import { Popover, PopoverModule } from 'primeng/popover';
import { SelectModule } from 'primeng/select';

import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

import { DotMessagePipe } from '@dotcms/ui';

import { LanguageFieldComponent } from './components/language-field/language-field.component';
import { SiteFieldComponent } from './components/site-field/site-field.component';

import { TreeNodeItem } from '../../../../../../models/dot-edit-content-host-folder-field.interface';
import { SearchParams } from '../../../../models/search.model';

export const DEBOUNCE_TIME = 300;

interface ActiveFilter {
    label: string;
    value: string | number;
    type: string;
}

/**
 * A standalone component that provides search functionality with language and site filtering.
 * This component includes a search input field and advanced filtering options through an overlay panel.
 *
 * @example
 * ```html
 * <dot-search (onSearch)="handleSearch($event)"></dot-search>
 * ```
 */
@Component({
    selector: 'dot-search',
    imports: [
        InputTextModule,
        ButtonModule,
        InputGroupModule,
        PopoverModule,
        DotMessagePipe,
        SelectModule,
        ReactiveFormsModule,
        LanguageFieldComponent,
        SiteFieldComponent,
        InputGroupAddonModule,
        ChipModule
    ],
    templateUrl: './search.component.html'
})
export class SearchComponent {
    /**
     * Reference to the Popover component used for advanced search options.
     */
    $overlayPanel = viewChild.required(Popover);

    /**
     * Reference to the language field component to access its store.
     */
    $languageField = viewChild.required(LanguageFieldComponent);

    /**
     * Reference to the site field component to access its store.
     */
    $siteField = viewChild.required(SiteFieldComponent);

    /**
     * Output signal that emits search parameters when the search is performed.
     * Emits an object containing query string, language ID, and site ID.
     */
    onSearch = output<SearchParams>();

    /**
     * Input signal that indicates if the search is loading.
     */
    $isLoading = input.required<boolean>({ alias: 'isLoading' });

    /**
     * Signal that stores the currently active search parameters.
     * Updated only when a search is actually performed.
     */
    $activeSearchParams = signal<SearchParams>({});

    /**
     * Computed property that returns active filters based on the last executed search.
     */
    $activeFilters = computed(() => {
        const searchParams = this.$activeSearchParams();
        const filters: ActiveFilter[] = [];

        if (!searchParams.systemSearchableFields) return filters;

        // Language filter
        const languageId = searchParams.systemSearchableFields.languageId as number;
        if (languageId && languageId !== -1) {
            filters.push({
                label: this.getLanguageDisplayLabel(languageId as number),
                value: languageId,
                type: 'language'
            });
        }

        // Site filter
        const siteId = searchParams.systemSearchableFields.siteId as string;
        if (siteId) {
            filters.push({
                label: this.getSiteDisplayLabel(siteId),
                value: `site:${siteId}`,
                type: 'site'
            });
        }

        // Folder filter
        const folderId = searchParams.systemSearchableFields.folderId as string;
        if (folderId) {
            filters.push({
                label: this.getSiteDisplayLabel(folderId),
                value: `folder:${folderId}`,
                type: 'folder'
            });
        }

        return filters;
    });

    /**
     * FormBuilder instance for creating reactive forms.
     * @private
     */
    readonly #formBuilder = inject(FormBuilder);

    /**
     * DestroyRef instance to track component lifecycle.
     * @private
     */
    readonly #destroyRef = inject(DestroyRef);

    /**
     * Flag to track if the component has been destroyed.
     * @private
     */
    #isDestroyed = false;

    /**
     * Reactive form group containing search parameters:
     * - query: The search text
     * - languageId: Selected language ID (-1 for all languages)
     * - siteId: Selected site ID
     */
    readonly form = this.#formBuilder.nonNullable.group({
        query: [''],
        systemSearchableFields: this.#formBuilder.nonNullable.group({
            languageId: [-1],
            siteOrFolderId: ['']
        })
    });

    constructor() {
        // Mark component as destroyed when cleanup happens
        this.#destroyRef.onDestroy(() => {
            this.#isDestroyed = true;
        });

        // debounced search.
        this.form
            .get('query')
            ?.valueChanges.pipe(
                takeUntilDestroyed(),
                debounceTime(DEBOUNCE_TIME),
                distinctUntilChanged()
            )
            .subscribe(() => {
                this.doSearch();
            });
    }

    /**
     * Resets the search form to its initial state and clears active filters.
     *
     * @param event - Optional mouse event that triggered the clear action
     */
    clearForm() {
        if (this.#isDestroyed) {
            return;
        }

        this.form.reset();
        this.$overlayPanel().hide();

        // Clear active search parameters
        this.$activeSearchParams.set({});

        this.onSearch.emit({});
    }

    /**
     * Performs the search by emitting the current form values and hiding the overlay panel.
     * Also updates the active search parameters for tag display.
     * This method is triggered when the user submits the search form.
     */
    doSearch() {
        if (this.#isDestroyed) {
            return;
        }

        this.$overlayPanel().hide();
        const values = this.getValues();

        // Update the active search parameters
        this.$activeSearchParams.set(values);

        this.onSearch.emit(values);
    }

    /**
     * Gets the search parameters from the form.
     * If the siteOrFolderId is a folder, it returns the values with the folderId.
     * If the siteOrFolderId is a site, it returns the values with the siteId.
     * Otherwise, it returns the values as is.
     *
     * @returns {SearchParams} The formatted search parameters with proper site or folder ID mapping
     */
    getValues(): SearchParams {
        const values = this.form.getRawValue();
        const systemSearchableFields = values.systemSearchableFields;
        const { siteOrFolderId, ...otherFields } = systemSearchableFields;

        // If no site or folder ID is selected, return filtered values
        if (!siteOrFolderId) {
            return {
                query: values.query || '',
                systemSearchableFields: this.filterEmptyValues(otherFields)
            };
        }

        if (!siteOrFolderId.includes(':')) {
            return {
                query: values.query || '',
                systemSearchableFields: this.filterEmptyValues(otherFields)
            };
        }

        // Parse the type and ID from the siteOrFolderId string
        const [type, id] = siteOrFolderId.split(':');

        // Create the appropriate field based on the type
        const fieldKey = type === 'folder' ? 'folderId' : 'siteId';

        return {
            query: values.query || '',
            systemSearchableFields: {
                ...this.filterEmptyValues(otherFields),
                [fieldKey]: id
            }
        };
    }

    /**
     * Filters out empty values or -1 from an object
     *
     * @param values - Object containing form values
     * @returns Object with only valid values
     */
    private filterEmptyValues(values: Record<string, unknown>): Record<string, unknown> {
        return Object.entries(values).reduce(
            (acc, [key, val]) => {
                if (val !== -1 && val !== '' && val != null) {
                    acc[key] = val;
                }

                return acc;
            },
            {} as Record<string, unknown>
        );
    }

    /**
     * Removes a specific filter and performs a new search.
     *
     * @param filterType - The type of filter to remove ('language' | 'site' | 'folder')
     */
    removeFilter(filterType: 'language' | 'site' | 'folder') {
        if (filterType === 'language') {
            this.form.get('systemSearchableFields.languageId')?.setValue(-1);
        } else {
            this.form.get('systemSearchableFields.siteOrFolderId')?.setValue('');
        }

        this.doSearch();
    }

    /**
     * Gets a display-friendly label for the language filter.
     *
     * @param languageId - The language ID
     * @returns A formatted label for display
     */
    private getLanguageDisplayLabel(languageId: number): string {
        const languageValue = this.$languageField()?.languageControl?.value;

        return languageValue?.isoCode || `Language Id: ${languageId}`;
    }

    /**
     * Gets a display-friendly label for the site/folder filter.
     *
     * @param siteOrFolderId - The site or folder ID in format "type:id"
     * @returns A formatted label for display, truncated to 45 characters with ellipsis if needed
     */
    private getSiteDisplayLabel(siteOrFolderId: string): string {
        const siteFieldValue = this.$siteField()?.siteControl?.value as TreeNodeItem;
        let label: string;

        // Try to get the site/folder name from the child component if available
        if (siteFieldValue) {
            label = siteFieldValue.label;
        } else {
            // Fallback to ID only
            label = siteOrFolderId;
        }

        // Truncate if 45 characters or longer
        return label.length >= 45 ? label.substring(0, 45) + '...' : label;
    }
}
