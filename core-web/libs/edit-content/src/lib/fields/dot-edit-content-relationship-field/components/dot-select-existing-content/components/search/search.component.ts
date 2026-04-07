import {
    ChangeDetectionStrategy,
    Component,
    effect,
    inject,
    input,
    output,
    untracked,
    viewChild,
    signal,
    computed,
    DestroyRef
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { ChipModule } from 'primeng/chip';
import { InputGroupModule } from 'primeng/inputgroup';
import { InputGroupAddonModule } from 'primeng/inputgroupaddon';
import { Popover, PopoverModule } from 'primeng/popover';
import { SelectModule } from 'primeng/select';

import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

import { DotMessagePipe } from '@dotcms/ui';

import { LanguageFieldComponent } from './components/language-field/language-field.component';
import { SiteFieldComponent } from './components/site-field/site-field.component';

import { ContentletFilterContext } from '../../../../models/relationship.models';
import { SearchParams } from '../../../../models/search.model';

export const DEBOUNCE_TIME = 300;
const LABEL_MAX_LENGTH = 45;

type FilterType = 'language' | 'site' | 'folder';

interface ActiveFilter {
    label: string;
    value: string | number;
    type: FilterType;
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
        ButtonModule,
        InputGroupModule,
        InputGroupAddonModule,
        PopoverModule,
        DotMessagePipe,
        SelectModule,
        ReactiveFormsModule,
        LanguageFieldComponent,
        SiteFieldComponent,
        ChipModule
    ],
    templateUrl: './search.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
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
     * Input signal for initial filter context from the contentlet being edited.
     * Used to pre-populate form fields and show chips on dialog open.
     */
    $initialFilters = input<ContentletFilterContext | null>(null, { alias: 'initialFilters' });

    /**
     * Signal that stores the currently active search parameters.
     * Updated only when a search is actually performed.
     */
    $activeSearchParams = signal<SearchParams>({});

    /**
     * Computed site context for the SiteFieldComponent.
     * Provides hostname and folder path for label display.
     */
    $siteContext = computed(() => {
        const filters = this.$initialFilters();
        if (!filters?.hostName) {
            return null;
        }

        return {
            hostName: filters.hostName,
            folderPath: filters.folderPath ?? ''
        };
    });

    /**
     * Computed property that returns active filters based on the last executed search.
     */
    $activeFilters = computed(() => {
        const searchParams = this.$activeSearchParams();
        const filters: ActiveFilter[] = [];

        if (!searchParams.systemSearchableFields) return filters;

        const { languageId, siteId, folderId } = searchParams.systemSearchableFields;

        // Language filter
        if (languageId && languageId !== -1) {
            filters.push({
                label: this.getLanguageDisplayLabel(languageId),
                value: languageId,
                type: 'language'
            });
        }

        // Site filter
        if (siteId) {
            filters.push({
                label: this.getSiteDisplayLabel(siteId),
                value: `site:${siteId}`,
                type: 'site'
            });
        }

        // Folder filter
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
     * Flag to prevent multiple pre-population runs.
     * @private
     */
    #prePopulated = false;

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

        // Pre-populate form from initial filters (runs once)
        effect(() => {
            const filters = this.$initialFilters();
            if (!filters || untracked(() => this.#prePopulated)) {
                return;
            }

            this.#prePopulated = true;

            // Pre-populate language
            if (filters.languageId && filters.languageId !== -1) {
                this.form
                    .get('systemSearchableFields.languageId')
                    ?.setValue(filters.languageId, { emitEvent: false });
            }

            // Pre-populate site or folder
            if (filters.folderId) {
                this.form
                    .get('systemSearchableFields.siteOrFolderId')
                    ?.setValue(`folder:${filters.folderId}`, { emitEvent: false });
            } else if (filters.hostId) {
                this.form
                    .get('systemSearchableFields.siteOrFolderId')
                    ?.setValue(`site:${filters.hostId}`, { emitEvent: false });
            }

            // Set active search params so chips display immediately
            this.$activeSearchParams.set(this.getValues());
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
    removeFilter(filterType: FilterType) {
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
        const field = this.$languageField();

        // Use reactive signal first (updates when languages load)
        const signalLabel = field?.$selectedLanguageLabel();
        if (signalLabel) {
            return signalLabel;
        }

        // Fallback to form control value
        return field?.languageControl?.value?.isoCode || `Language Id: ${languageId}`;
    }

    /**
     * Gets a display-friendly label for the site/folder filter.
     *
     * @param siteOrFolderId - The site or folder ID in format "type:id"
     * @returns A formatted label for display, truncated to 45 characters with ellipsis if needed
     */
    private getSiteDisplayLabel(siteOrFolderId: string): string {
        const field = this.$siteField();

        // Use reactive signal first (updates when selection changes)
        const signalLabel = field?.$selectedNodeLabel();
        if (signalLabel) {
            return this.truncateLabel(signalLabel);
        }

        // Fallback to form control value
        const siteFieldValue = field?.siteControl?.value;
        if (siteFieldValue?.label) {
            return this.truncateLabel(siteFieldValue.label);
        }

        // Fallback to initialFilters context
        const filters = this.$initialFilters();
        if (filters?.hostName) {
            const ctxLabel = filters.folderPath
                ? `${filters.hostName}${filters.folderPath}`
                : filters.hostName;

            return this.truncateLabel(ctxLabel);
        }

        return this.truncateLabel(siteOrFolderId);
    }

    private truncateLabel(label: string): string {
        return label.length > LABEL_MAX_LENGTH
            ? label.substring(0, LABEL_MAX_LENGTH) + '...'
            : label;
    }
}
