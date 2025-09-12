import { BehaviorSubject, EMPTY } from 'rxjs';

import {
    ChangeDetectionStrategy,
    Component,
    inject,
    input,
    model,
    signal,
    viewChild,
    OnInit
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { AutoComplete, AutoCompleteCompleteEvent, AutoCompleteModule } from 'primeng/autocomplete';

import { catchError, skip, switchMap } from 'rxjs/operators';

import { DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DotEditContentService } from '../../services/dot-edit-content.service';
import { DotCardFieldContentComponent } from '../dot-card-field/components/dot-card-field-content.component';
import { DotCardFieldFooterComponent } from '../dot-card-field/components/dot-card-field-footer.component';
import { DotCardFieldComponent } from '../dot-card-field/dot-card-field.component';
import { BaseFieldComponent } from '../shared/base-field.component';

export const AUTO_COMPLETE_MIN_LENGTH = 2;

export const AUTO_COMPLETE_DELAY = 300;

export const AUTO_COMPLETE_UNIQUE = true;

/**
 * Component that handles tag field input using PrimeNG's AutoComplete.
 * It provides tag suggestions as the user types with a minimum of 2 characters.
 * Implements ControlValueAccessor for seamless form integration.
 */
@Component({
    selector: 'dot-edit-content-tag-field',
    imports: [
        AutoCompleteModule,
        FormsModule,
        ReactiveFormsModule,
        DotCardFieldComponent,
        DotCardFieldContentComponent,
        DotCardFieldFooterComponent,
        DotMessagePipe
    ],
    templateUrl: './dot-edit-content-tag-field.component.html',
    styleUrl: './dot-edit-content-tag-field.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotEditContentTagFieldComponent extends BaseFieldComponent implements OnInit {
    #editContentService = inject(DotEditContentService);

    protected readonly AUTO_COMPLETE_MIN_LENGTH = AUTO_COMPLETE_MIN_LENGTH;
    protected readonly AUTO_COMPLETE_DELAY = AUTO_COMPLETE_DELAY;
    protected readonly AUTO_COMPLETE_UNIQUE = AUTO_COMPLETE_UNIQUE;

    /**
     * Required input that defines the field configuration
     */
    $field = input.required<DotCMSContentTypeField>({ alias: 'field' });

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
     * Signal that holds the disabled state
     */
    $disabled = signal<boolean>(false);

    /**
     * Subject that handles the search terms stream
     * Used to process user input
     */
    #searchTerms$ = new BehaviorSubject<string>('');

    constructor() {
        super();
        this.setupSearchListener();
    }

    ngOnInit() {
        this.statusChanges$.subscribe(() => {
            this.changeDetectorRef.detectChanges();
        });
    }

    /**
     * Sets up the search listener that handles tag suggestions.
     */
    private setupSearchListener(): void {
        this.#searchTerms$
            .pipe(
                skip(1),
                switchMap((term) => this.searchTags(term)),
                takeUntilDestroyed(this.destroyRef)
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
        if (this.$disabled()) {
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
        if (this.$disabled()) {
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
        if (this.$disabled()) {
            return;
        }

        // Get the current value from the AutoComplete component
        const autocompleteValue = this.autocomplete()?.value || [];
        this.$values.set(autocompleteValue);
        this.onChange(autocompleteValue.join(','));
    }

    /**
     * Sets the value when the form control value changes
     */
    writeValue(value: string | string[]): void {
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
    }

    /**
     * Sets the disabled state of the component
     */
    setDisabledState(isDisabled: boolean): void {
        this.$disabled.set(isDisabled);
    }
}
