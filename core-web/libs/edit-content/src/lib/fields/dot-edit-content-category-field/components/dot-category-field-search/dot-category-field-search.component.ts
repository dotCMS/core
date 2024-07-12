import { CommonModule } from '@angular/common';
import {
    AfterViewInit,
    ChangeDetectionStrategy,
    Component,
    DestroyRef,
    EventEmitter,
    inject,
    input,
    Output
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';

import { InputTextModule } from 'primeng/inputtext';

import { debounceTime, filter } from 'rxjs/operators';

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
export class DotCategoryFieldSearchComponent implements AfterViewInit {
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
    isLoading = input.required<boolean>();
    #destroyRef = inject(DestroyRef);

    ngAfterViewInit(): void {
        this.listenInputChanges();
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

    /**
     * Sets up the search input observable to listen to input events,
     * debounce the input, filter by minimum character length,
     * and emit the search query value.
     *
     * @private
     */
    private listenInputChanges(): void {
        this.searchControl.valueChanges
            .pipe(
                takeUntilDestroyed(this.#destroyRef),
                debounceTime(DEBOUNCE_TIME),
                filter((value: string) => value.length >= MINIMUM_CHARACTERS)
            )
            .subscribe((value: string) => {
                this.term.emit(value);
            });
    }
}
