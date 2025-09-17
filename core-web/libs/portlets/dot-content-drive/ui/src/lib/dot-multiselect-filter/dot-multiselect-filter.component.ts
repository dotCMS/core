import { Component, inject, input, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { CheckboxChangeEvent, CheckboxModule } from 'primeng/checkbox';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputText, InputTextModule } from 'primeng/inputtext';
import { MultiSelect } from 'primeng/multiselect';

/**
 * DotMultiSelectFilterComponent provides a reusable custom filter template
 * for PrimeNG MultiSelect components. It includes a "Select All" checkbox
 * and a search input field.
 *
 * @example
 * ```html
 * <p-multiSelect [options]="options" ...>
 *   <ng-template pTemplate="filter">
 *     <dot-multiselect-filter />
 *   </ng-template>
 * </p-multiSelect>
 * ```
 */
@Component({
    selector: 'dot-multiselect-filter',
    templateUrl: './dot-multiselect-filter.component.html',
    styleUrl: './dot-multiselect-filter.component.scss',
    imports: [FormsModule, CheckboxModule, IconFieldModule, InputIconModule, InputTextModule]
})
export class DotMultiSelectFilterComponent {
    /**
     * Automatically inject the parent MultiSelect component
     */
    readonly #multiSelect = inject(MultiSelect, { optional: true, host: true });

    /**
     * Placeholder text for the search input
     */
    filterPlaceholder = input<string>('Search');

    protected readonly input = viewChild<InputText>(InputText);

    constructor() {
        if (!this.#multiSelect) {
            throw new Error(
                '[DotMultiSelectFilterComponent] must be used within a PrimeNG MultiSelect component'
            );
        }
    }

    ngAfterViewInit(): void {
        this.input()?.el.nativeElement.focus();
    }

    /**
     * Whether the "Select All" checkbox is checked
     */
    get allSelected(): boolean {
        return this.#multiSelect?.allSelected() || false;
    }

    /**
     * Handle filter input changes
     */
    protected onFilter(event: Event): void {
        this.#multiSelect?.onFilterInputChange(event as KeyboardEvent);
    }

    /**
     * Toggle all items selection
     */
    protected toggleCheckAll(event: CheckboxChangeEvent): void {
        this.#multiSelect?.onToggleAll(event.originalEvent);
    }
}
