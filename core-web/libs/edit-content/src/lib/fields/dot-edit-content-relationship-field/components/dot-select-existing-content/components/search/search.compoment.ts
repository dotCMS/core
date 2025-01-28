import { Component, inject, output, viewChild } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { DropdownModule } from 'primeng/dropdown';
import { InputGroupModule } from 'primeng/inputgroup';
import { InputTextModule } from 'primeng/inputtext';
import {OverlayPanel, OverlayPanelModule } from 'primeng/overlaypanel';

import { SearchParams } from '@dotcms/edit-content/fields/dot-edit-content-relationship-field/models/search.model';
import { DotMessagePipe } from '@dotcms/ui';

import { LanguageFieldComponent } from './components/language-field/language-field.component';

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
        LanguageFieldComponent
    ],
    templateUrl: './search.compoment.html'
})
export class SearchComponent {
    $overlayPanel = viewChild(OverlayPanel);
    /**
     * An output signal that emits when the search input is changed.
     */
    onSearch = output<SearchParams>();

    /**
     * Injects FormBuilder to create form control groups.
     */
    readonly #formBuilder = inject(FormBuilder);

    /**
     * Initializes the form group with default values for language and site.
     */
    readonly form = this.#formBuilder.nonNullable.group({
        query: [''],
        languageId: [-1],
        siteId: ['']
    });

    clearForm(event?: MouseEvent) {
        this.form.reset();
        if (event) {
            this.$overlayPanel().toggle(event);
        }
    }

    doSearch(event?: MouseEvent) {
        if (event) {
            this.$overlayPanel().toggle(event);
        }

        this.onSearch.emit(this.form.getRawValue());
    }
}
