import { Component, inject, input, output, viewChild } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { DropdownModule } from 'primeng/dropdown';
import { InputGroupModule } from 'primeng/inputgroup';
import { InputTextModule } from 'primeng/inputtext';
import { OverlayPanel, OverlayPanelModule } from 'primeng/overlaypanel';

import { SearchParams } from '@dotcms/edit-content/fields/dot-edit-content-relationship-field/models/search.model';
import { DotMessagePipe } from '@dotcms/ui';

import { LanguageFieldComponent } from './components/language-field/language-field.component';
import { SiteFieldComponent } from './components/site-field/site-field.component';

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
    standalone: true,
    imports: [
        InputTextModule,
        ButtonModule,
        InputGroupModule,
        OverlayPanelModule,
        DotMessagePipe,
        DropdownModule,
        ReactiveFormsModule,
        LanguageFieldComponent,
        SiteFieldComponent
    ],
    templateUrl: './search.compoment.html'
})
export class SearchComponent {
    /**
     * Reference to the OverlayPanel component used for advanced search options.
     */
    $overlayPanel = viewChild(OverlayPanel);

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
     * FormBuilder instance for creating reactive forms.
     * @private
     */
    readonly #formBuilder = inject(FormBuilder);

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
            siteId: ['']
        })
    });

    /**
     * Resets the search form to its initial state and optionally toggles the overlay panel.
     *
     * @param event - Optional mouse event that triggered the clear action
     */
    clearForm() {
        this.form.reset();
        this.$overlayPanel().hide();
    }

    /**
     * Performs the search by emitting the current form values and hiding the overlay panel.
     * This method is triggered when the user submits the search form.
     */
    doSearch() {
        this.$overlayPanel().hide();
        const values = this.form.getRawValue();
        // Filter out values that are -1 or empty strings
        const filteredValues = Object.entries(values.systemSearchableFields).reduce(
            (acc, [key, val]) => {
                // Skip values that are -1 or empty strings
                if (val !== -1 && val !== '') {
                    acc[key] = val;
                }

                return acc;
            },
            {}
        );

        // Prepare the search parameters
        const searchParams: SearchParams = {
            query: values.query,
            systemSearchableFields: filteredValues
        };
        this.onSearch.emit(searchParams);
    }
}
