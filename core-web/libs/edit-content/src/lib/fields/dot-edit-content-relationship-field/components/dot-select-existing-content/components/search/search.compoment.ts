import { Component, inject, output } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { DropdownModule } from 'primeng/dropdown';
import { InputGroupModule } from 'primeng/inputgroup';
import { InputTextModule } from 'primeng/inputtext';
import { OverlayPanelModule } from 'primeng/overlaypanel';

import { DotMessagePipe } from '@dotcms/ui';

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
        ReactiveFormsModule
    ],
    templateUrl: './search.compoment.html'
})
export class SearchComponent {
    /**
     * An output signal that emits when the search input is changed.
     */
    onSearch = output<string>();

    /**
     * Injects FormBuilder to create form control groups.
     */
    readonly #formBuilder = inject(FormBuilder);

    /**
     * Initializes the form group with default values for language and site.
     */
    readonly form = this.#formBuilder.group({
        language: [''],
        site: ['']
    });
}
