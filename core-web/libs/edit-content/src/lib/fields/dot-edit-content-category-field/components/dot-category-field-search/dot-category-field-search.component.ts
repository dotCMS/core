import { ChangeDetectionStrategy, Component, EventEmitter, input, Output } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';

import { InputGroupModule } from 'primeng/inputgroup';
import { InputGroupAddonModule } from 'primeng/inputgroupaddon';
import { InputTextModule } from 'primeng/inputtext';

import { debounceTime, distinctUntilChanged, filter, tap } from 'rxjs/operators';

import { DotMessagePipe } from '@dotcms/ui';

import { CategoryFieldViewMode } from '../../models/dot-category-field.models';

export const DEBOUNCE_TIME = 300;

const MINIMUM_CHARACTERS = 3;

@Component({
    selector: 'dot-category-field-search',
    imports: [
        DotMessagePipe,
        InputTextModule,
        ReactiveFormsModule,
        InputGroupModule,
        InputGroupAddonModule
    ],
    templateUrl: './dot-category-field-search.component.html',
    styleUrl: './dot-category-field-search.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotCategoryFieldSearchComponent {
    searchControl = new FormControl();

    /**
     * Represent a EventEmitter for the term the user want to filter
     *
     */
    @Output() term = new EventEmitter<string>();

    /**
     * Represent a EventEmitter to notify we want change the mode to `list`.
     */
    @Output() changeMode = new EventEmitter<CategoryFieldViewMode>();

    /**
     * Represents the boolean variable isLoading.
     */
    $isLoading = input<boolean>(false, { alias: 'isLoading' });

    constructor() {
        // Emit the term to search, if the input is empty hide the result.
        this.searchControl.valueChanges
            .pipe(
                takeUntilDestroyed(),
                debounceTime(DEBOUNCE_TIME),
                distinctUntilChanged(),
                tap((value: string) => {
                    if (value.length === 0) {
                        this.clearInput();
                    }
                }),
                filter((value: string) => value.length >= MINIMUM_CHARACTERS)
            )
            .subscribe((value: string) => {
                this.term.emit(value);
            });
    }

    /**
     * Clears the value of the search input field and emits a change mode event.
     *
     * @return {void}
     */
    clearInput(): void {
        this.searchControl.setValue('');
        this.changeMode.emit('list');
    }
}
