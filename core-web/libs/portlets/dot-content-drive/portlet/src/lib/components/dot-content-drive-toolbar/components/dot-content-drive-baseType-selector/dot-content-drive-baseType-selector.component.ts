import { patchState, signalState } from '@ngrx/signals';
import { of } from 'rxjs';

import { Component, inject, linkedSignal, model, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { CheckboxModule } from 'primeng/checkbox';
import { MultiSelect, MultiSelectModule } from 'primeng/multiselect';


import { debounceTime, distinctUntilChanged, take } from 'rxjs/operators';

import { DotContentTypeService } from '@dotcms/data-access';

import { DEBOUNCE_TIME, MAP_NUMBERS_TO_BASE_TYPES } from '../../../../shared/constants';
import { DotContentDriveStore } from '../../../../store/dot-content-drive.store';

@Component({
    selector: 'dot-content-drive-base-type-selector',
    templateUrl: './dot-content-drive-baseType-selector.component.html',
    styleUrl: './dot-content-drive-baseType-selector.component.scss',
    imports: [MultiSelectModule, FormsModule, CheckboxModule],
    standalone: true
})
export class  DotContentDriveBaseTypeSelectorComponent {
    baseTypeSelector = viewChild<MultiSelect>('baseTypeSelector');

    $selectedBaseTypes = model<string[]>([]);
    #store = inject(DotContentDriveStore);
    readonly #dotContentTypeService = inject(DotContentTypeService);

    readonly $state = signalState({
        baseTypes: []
    })

    $selectedAll = linkedSignal(() => {
        const selectedBaseTypes = this.$selectedBaseTypes();

        const isAllSelected = selectedBaseTypes?.length === 0

        return isAllSelected
    })

    ngOnInit() {
        this.#dotContentTypeService
            .getAllContentTypes()
            .pipe(take(1))
            .subscribe((response) => {
                patchState(this.$state, {
                    baseTypes: response
                });
            });
    }

    onChange() {
        of(this.$selectedBaseTypes() ?? [])
        .pipe(
            debounceTime(DEBOUNCE_TIME), // Debounce to avoid spamming the server
            distinctUntilChanged()
        )
        .subscribe((value) => {
            if (value.length > 0) {
            // Get all keys from MAP_NUMBERS_TO_BASE_TYPES where the values match the array
            const keys = value.map(val => 
                Object.entries(MAP_NUMBERS_TO_BASE_TYPES).find(([_key, v]) => v === val)?.[0]
            ).filter(Boolean); // Remove any undefined values
            
            this.#store.patchFilters({
                baseType: keys
            });
            } else {
                this.#store.removeFilter('baseType');
            }
        });
    }

    toggleAll() {
        this.$selectedBaseTypes.set([]);
        this.onChange();


        // const baseTypes = this.$state.baseTypes();
        // if (this.$selectedAll()) {
        //     // If all are currently selected (selectedAll is true), select none
        //     // this.$selectedBaseTypes.set([]);

        //     return;
        // } else {
        //     // Otherwise select all base types
        //     this.$selectedBaseTypes.set(baseTypes.map(type => type.name));
        // }
        // this.onChange();
    }
}