import { signalMethod } from '@ngrx/signals';
import { BehaviorSubject, EMPTY } from 'rxjs';

import {
    ChangeDetectionStrategy,
    Component,
    DestroyRef,
    forwardRef,
    inject,
    input,
    model,
    signal,
    viewChild
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule, NG_VALUE_ACCESSOR, ReactiveFormsModule } from '@angular/forms';

import { AutoComplete, AutoCompleteCompleteEvent, AutoCompleteModule } from 'primeng/autocomplete';

import { catchError, skip, switchMap } from 'rxjs/operators';

import { DotEditContentService } from '../../../../services/dot-edit-content.service';
import { BaseControlValueAccessor } from '../../../shared/base-control-value-accesor';

export const AUTO_COMPLETE_MIN_LENGTH = 2;

export const AUTO_COMPLETE_DELAY = 300;

export const AUTO_COMPLETE_UNIQUE = true;

/**
 * Component that handles tag field input using PrimeNG's AutoComplete.
 * It provides tag suggestions as the user types with a minimum of 2 characters.
 * Implements ControlValueAccessor for seamless form integration.
 */
@Component({
    selector: 'dot-tag-field',
    imports: [AutoCompleteModule, FormsModule, ReactiveFormsModule],
    templateUrl: './tag-field.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [
        {
            multi: true,
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => DotTagFieldComponent)
        }
    ]
})
export class DotTagFieldComponent extends BaseControlValueAccessor<string | string[]> {
    #editContentService = inject(DotEditContentService);

    /**
     * A readonly private field that holds a reference to the `DestroyRef` service.
     * This service is injected into the component to manage the destruction lifecycle.
     */
    #destroyRef = inject(DestroyRef);

    protected readonly AUTO_COMPLETE_MIN_LENGTH = AUTO_COMPLETE_MIN_LENGTH;
    protected readonly AUTO_COMPLETE_DELAY = AUTO_COMPLETE_DELAY;
    protected readonly AUTO_COMPLETE_UNIQUE = AUTO_COMPLETE_UNIQUE;

    /**
     * Required input that defines the field configuration
     */
    $variableName = input.required<string>({ alias: 'variableName' });

    /**
     * Required input that defines the error state
     */
    $hasError = input.required<boolean>({ alias: 'hasError' });

    /**
     * Reference to the PrimeNG AutoComplete component
     * Used to programmatically control the component's behavior
     */
    readonly autocomplete = viewChild<AutoComplete>('autocomplete');

    /**
     * Signal that holds the current suggestions for the autocomplete
     */
    $suggestions = signal<string[]>([]);

    /**
     * Signal that holds the current value (array of tags)
     */
    $values = model<string[]>([]);

    /**
     * Subject that handles the search terms stream
     * Used to process user input
     */
    #searchTerms$ = new BehaviorSubject<string>('');

    constructor() {
        super();
        this.setupSearchListener();
        this.handleChangeValue(this.$value);
    }

    /**
     * Sets up the search listener that handles tag suggestions.
     */
    private setupSearchListener(): void {
        this.#searchTerms$
            .pipe(
                skip(1),
                switchMap((term) => this.searchTags(term)),
                takeUntilDestroyed(this.#destroyRef)
            )
            .subscribe({
                next: (tags) => this.$suggestions.set(tags),
                error: () => this.$suggestions.set([])
            });
    }

    /**
     * Performs the actual tag search with error handling
     */
    private searchTags(term: string) {
        return this.#editContentService.getTags(term).pipe(
            catchError((error) => {
                console.error('Error fetching tags:', error);

                return EMPTY;
            })
        );
    }

    /**
     * Handles the search event from the AutoComplete component
     * Pushes the new search term into the searchTerms$ stream
     */
    onSearch(event: AutoCompleteCompleteEvent): void {
        this.#searchTerms$.next(event.query);
    }

    /**
     * Handles the Enter key press event in the tag input field.
     * Prevents form submission and allows custom values while maintaining uniqueness.
     *
     * @param {Event} event - The keyboard event object
     */
    onEnterKey(event: Event): void {
        if (this.$isDisabled()) {
            return;
        }

        event.preventDefault();

        const input = event.target as HTMLInputElement;
        const value = input.value.trim();

        if (value) {
            const currentValues = this.$values();
            const isDuplicate = currentValues.includes(value);

            if (!isDuplicate) {
                const newValue = [...currentValues, value];
                this.$values.set(newValue);
                this.onChange(newValue.join(','));
                input.value = '';
            }
        }
    }

    /**
     * Handles when tags are selected/changed in the AutoComplete
     */
    onTagsChange(tags: string[]): void {
        if (this.$isDisabled()) {
            return;
        }

        this.$values.set(tags);
        this.onChange(tags.join(','));
        this.onTouched();
    }

    /**
     * Handles when a tag is unselected in the AutoComplete
     */
    onTagUnselected(): void {
        if (this.$isDisabled()) {
            return;
        }

        // Get the current value from the AutoComplete component
        const autocompleteValue = this.autocomplete()?.value || [];
        this.$values.set(autocompleteValue);
        this.onChange(autocompleteValue.join(','));
    }

    readonly handleChangeValue = signalMethod<string | string[]>((value) => {
        let tagsArray: string[] = [];

        if (typeof value === 'string') {
            // Handle string format (comma-separated)
            tagsArray = value
                ? value
                      .split(',')
                      .map((tag) => tag.trim())
                      .filter((tag) => tag)
                : [];
        } else if (Array.isArray(value)) {
            // Handle array format
            tagsArray = value;
        }

        this.$values.set(tagsArray);
    });
}
