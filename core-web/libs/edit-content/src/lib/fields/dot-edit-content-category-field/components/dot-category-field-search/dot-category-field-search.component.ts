import { fromEvent } from 'rxjs';

import { CommonModule } from '@angular/common';
import {
    AfterViewInit,
    ChangeDetectionStrategy,
    Component,
    DestroyRef,
    ElementRef,
    EventEmitter,
    inject,
    input,
    Output,
    ViewChild
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { InputTextModule } from 'primeng/inputtext';

import { debounceTime, filter, map } from 'rxjs/operators';

import { DotMessagePipe } from '@dotcms/ui';

const DEBOUNCE_TIME = 300;
const MINIMUM_CHARACTERS = 3;

@Component({
    selector: 'dot-category-field-search',
    standalone: true,
    imports: [CommonModule, DotMessagePipe, InputTextModule],
    templateUrl: './dot-category-field-search.component.html',
    styleUrl: './dot-category-field-search.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotCategoryFieldSearchComponent implements AfterViewInit {
    @ViewChild('searchInput') searchInput!: ElementRef;

    /**
     * Represent a EventEmitter for the term the user want to filter
     *
     */
    @Output() term = new EventEmitter<string>();

    /**
     * Represent a EventEmitter to notify we want change the mode to `list`.
     */
    @Output() changeMode = new EventEmitter<string>();
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
        this.searchInput.nativeElement.value = '';
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
        fromEvent(this.searchInput.nativeElement, 'input')
            .pipe(
                takeUntilDestroyed(this.#destroyRef),
                map((event: Event) => (event.target as HTMLInputElement).value),
                debounceTime(DEBOUNCE_TIME),
                filter((value: string) => value.length >= MINIMUM_CHARACTERS)
            )
            .subscribe((value: string) => {
                this.term.emit(value);
            });
    }
}
