import { patchState, signalState } from '@ngrx/signals';
import { of } from 'rxjs';

import { Component, inject, linkedSignal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { CheckboxModule } from 'primeng/checkbox';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { MultiSelect, MultiSelectModule } from 'primeng/multiselect';

import { debounceTime, distinctUntilChanged, map, take } from 'rxjs/operators';

import { DotContentTypeService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';

import {
    DEBOUNCE_TIME,
    MAP_NUMBERS_TO_BASE_TYPES,
    PANEL_SCROLL_HEIGHT
} from '../../../../shared/constants';
import { DotContentDriveStore } from '../../../../store/dot-content-drive.store';

@Component({
    selector: 'dot-content-drive-base-type-selector',
    templateUrl: './dot-content-drive-base-type-selector.component.html',
    styleUrl: './dot-content-drive-base-type-selector.component.scss',
    imports: [
        MultiSelectModule,
        FormsModule,
        CheckboxModule,
        DotMessagePipe,
        IconFieldModule,
        InputIconModule,
        InputTextModule
    ],
    standalone: true
})
export class DotContentDriveBaseTypeSelectorComponent {
    $selectedBaseTypes = linkedSignal<string[]>(() => {
        const baseTypes = this.#store.getFilterValue('baseType') as string[];
        return baseTypes?.length > 0
            ? baseTypes.map((key) => MAP_NUMBERS_TO_BASE_TYPES[key]).filter(Boolean)
            : [];
    });

    readonly #store = inject(DotContentDriveStore);
    readonly #dotContentTypeService = inject(DotContentTypeService);

    readonly $multiSelect = viewChild<MultiSelect>(MultiSelect);

    readonly $state = signalState({
        baseTypes: []
    });

    protected readonly MULTISELECT_SCROLL_HEIGHT = PANEL_SCROLL_HEIGHT;

    ngOnInit() {
        this.getCurrentBaseTypes();

        this.#dotContentTypeService
            .getAllContentTypes()
            .pipe(
                take(1),
                map((response) => response.filter((item) => item.name !== 'FORM'))
            )
            .subscribe((response) => {
                patchState(this.$state, {
                    baseTypes: response
                });
            });
    }

    getCurrentBaseTypes() {
        const baseTypes = this.#store.getFilterValue('baseType') as string[];

        if (baseTypes?.length > 0) {
            const values = baseTypes.map((key) => MAP_NUMBERS_TO_BASE_TYPES[key]).filter(Boolean);

            this.$selectedBaseTypes.set(values);
        }
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
                    const keys = value
                        .map(
                            (val) =>
                                Object.entries(MAP_NUMBERS_TO_BASE_TYPES).find(
                                    ([_key, v]) => v === val
                                )?.[0]
                        )
                        .filter(Boolean);

                    this.#store.patchFilters({
                        baseType: keys
                    });
                } else {
                    this.#store.removeFilter('baseType');
                }
            });
    }

    changedFilter() {
        // eslint-disable-next-line no-console
        console.log(this.$multiSelect());
    }
}
