import { BehaviorSubject, EMPTY } from 'rxjs';

import { CommonModule } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    DestroyRef,
    inject,
    input,
    signal,
    viewChild
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ControlContainer, ReactiveFormsModule } from '@angular/forms';

import { AutoComplete, AutoCompleteCompleteEvent, AutoCompleteModule } from 'primeng/autocomplete';

import { catchError, debounceTime, switchMap } from 'rxjs/operators';

import { DotCMSContentTypeField } from '@dotcms/dotcms-models';

import { DotEditContentService } from '../../services/dot-edit-content.service';

const MIN_SEARCH_LENGTH = 1;
const DEBOUNCE_TIME = 250;

/**
 * Component that handles tag field input using PrimeNG's AutoComplete.
 * It provides tag suggestions as the user types with a minimum of 3 characters.
 */
@Component({
    selector: 'dot-edit-content-tag-field',
    standalone: true,
    imports: [CommonModule, AutoCompleteModule, ReactiveFormsModule],
    templateUrl: './dot-edit-content-tag-field.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    viewProviders: [
        {
            provide: ControlContainer,
            useFactory: () => inject(ControlContainer, { skipSelf: true })
        }
    ]
})
export class DotEditContentTagFieldComponent {
    #destroyRef = inject(DestroyRef);
    #editContentService = inject(DotEditContentService);
    #controlContainer = inject(ControlContainer);

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
     * Subject that handles the search terms stream
     * Used to debounce and process user input
     */
    #searchTerms$ = new BehaviorSubject<string>('');

    constructor() {
        this.setupSearchListener();
    }

    /**
     * Sets up the search listener that handles tag suggestions.
     */
    private setupSearchListener(): void {
        this.#searchTerms$
            .pipe(
                debounceTime(DEBOUNCE_TIME),
                switchMap((term) => this.handleSearch(term)),
                takeUntilDestroyed(this.#destroyRef)
            )
            .subscribe({
                next: (tags) => this.$suggestions.set(tags),
                error: () => this.$suggestions.set([])
            });
    }

    /**
     * Handles the search logic based on term length
     * Returns either EMPTY for short terms or the search results
     */
    private handleSearch(term: string) {
        return term.length < MIN_SEARCH_LENGTH ? this.handleShortTerm() : this.searchTags(term);
    }

    /**
     * Handles the case when the search term is too short
     * Clears suggestions and hides the overlay
     */
    private handleShortTerm() {
        this.$suggestions.set([]);
        this.hideAutocomplete();

        return EMPTY;
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
     * Safely hides the autocomplete overlay
     */
    private hideAutocomplete(): void {
        if (this.autocomplete()) {
            this.autocomplete().hide();
        }
    }

    /**
     * Handles the search event from the AutoComplete component
     * Pushes the new search term into the searchTerms$ stream
     */
    onSearch(event: AutoCompleteCompleteEvent): void {
        this.#searchTerms$.next(event.query);
    }

    /**
     * Gets the form control associated with this field
     * Used for form integration
     */
    get formControl() {
        return this.#controlContainer.control?.get(this.$field().variable);
    }
}
