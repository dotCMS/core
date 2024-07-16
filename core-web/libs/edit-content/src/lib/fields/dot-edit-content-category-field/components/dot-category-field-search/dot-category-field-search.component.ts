import { EMPTY, of } from 'rxjs';

import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, EventEmitter, input, Output } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';

import { InputTextModule } from 'primeng/inputtext';

import { debounceTime, switchMap } from 'rxjs/operators';

import { DotMessagePipe } from '@dotcms/ui';

export const DEBOUNCE_TIME = 300;

const MINIMUM_CHARACTERS = 3;

@Component({
    selector: 'dot-category-field-search',
    standalone: true,
    imports: [CommonModule, DotMessagePipe, InputTextModule, ReactiveFormsModule],
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
    @Output() changeMode = new EventEmitter<string>();

    /**
     * Represents the boolean variable isLoading.
     */
    $isLoading = input<boolean>(false, { alias: 'isLoading' });

    constructor() {
        this.searchControl.valueChanges
            .pipe(
                takeUntilDestroyed(),
                debounceTime(DEBOUNCE_TIME),
                switchMap((value: string) => {
                    if (value.length >= MINIMUM_CHARACTERS) {
                        return of(value);
                    } else if (value.length === 0) {
                        this.clearInput();

                        return of('');
                    }

                    return EMPTY;
                })
            )
            .subscribe((value: string) => {
                if (value.length >= MINIMUM_CHARACTERS) {
                    this.term.emit(value);
                }
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
