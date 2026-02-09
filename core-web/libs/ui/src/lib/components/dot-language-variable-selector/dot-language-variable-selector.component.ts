import {
    ChangeDetectionStrategy,
    Component,
    computed,
    inject,
    model,
    output,
    signal,
    viewChild
} from '@angular/core';
import { FormsModule } from '@angular/forms';

import { AutoComplete, AutoCompleteModule, AutoCompleteSelectEvent } from 'primeng/autocomplete';

import { take } from 'rxjs/operators';

import { DotLanguagesService } from '@dotcms/data-access';

import { DotMessagePipe } from '../../dot-message/dot-message.pipe';
import { DotHighlightPipe } from '../../pipes/dot-highlight/dot-highlight.pipe';

/**
 * Represents a language variable with a key and value.
 */
export interface LanguageVariable {
    key: string;
    value: string;
}

/**
 * Maximum number of language variables to display in the autocomplete suggestions.
 */
const MAX_LANGUAGES_SUGGESTIONS = 20;

/**
 * Smart component for selecting and formatting language variables.
 *
 * This component is responsible for:
 * - Directly communicating with DotLanguagesService to fetch language variables
 * - Providing an autocomplete interface for searching language variables
 * - Managing its own state and data fetching
 * - Formatting the selected language variable into the proper DotCMS syntax
 *
 * @example
 * // The component emits the formatted string ready to use:
 * // If user selects "GLOBAL_SEARCH" key, it emits:
 * $text.get('GLOBAL_SEARCH')
 *
 * @usage
 * ```html
 * <dot-language-variable-selector
 *   (onSelectLanguageVariable)="handleSelection($event)">
 * </dot-language-variable-selector>
 * ```
 */
@Component({
    selector: 'dot-language-variable-selector',
    imports: [AutoCompleteModule, FormsModule, DotMessagePipe, DotHighlightPipe],
    templateUrl: './dot-language-variable-selector.component.html',
    styleUrl: './dot-language-variable-selector.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotLanguageVariableSelectorComponent {
    /**
     * Injects the DotLanguagesService to fetch language variables.
     */
    #dotLanguagesService = inject(DotLanguagesService);

    /**
     * Signal to get the autocomplete component.
     */
    $autoComplete = viewChild.required<AutoComplete>(AutoComplete);
    /**
     * Output that emits the selected language variable formatted as a DotCMS text variable.
     * The emitted string is already formatted in the proper syntax: $text.get('KEY_SELECTED')
     */
    onSelectLanguageVariable = output<string>();

    /**
     * Signal to store the selected item from the autocomplete.
     */
    $selectedItem = model<string>('');

    /**
     * Signal to store the language variables, for later use in the autocomplete
     * depending of the search query.
     */
    $languageVariables = signal<LanguageVariable[]>([]);

    /**
     * Computed property to filter the language variables based on the search query.
     */
    $filteredSuggestions = computed(() => {
        const term = this.$selectedItem()?.toLowerCase();
        const languageVariables = this.$languageVariables();

        if (!term) {
            return [];
        }

        return languageVariables
            .filter((variable) => variable.key.toLowerCase().includes(term))
            .slice(0, MAX_LANGUAGES_SUGGESTIONS);
    });

    /**
     * Clear the autocomplete when the overlay is hidden.
     */
    onHideOverlay() {
        this.#resetAutocomplete();
    }

    /**
     * Search for language variables.
     *
     * @return {void}
     */
    loadSuggestions() {
        if (this.$languageVariables().length === 0) {
            this.#getLanguageVariables();
        } else {
            /**
             * Set the autocomplete loading to false and show the overlay if there are suggestions.
             * this for handle the bug in the autocomplete and show the loading icon when there are suggestions
             */
            const autocomplete = this.$autoComplete();
            if (autocomplete) {
                autocomplete.loading = false;
                if (this.$filteredSuggestions().length > 0) {
                    autocomplete.overlayVisible = true;
                }

                autocomplete.cd.markForCheck();
            }
        }
    }

    /**
     * Handles the selection of a language variable from the autocomplete.
     * Formats the selected key into the DotCMS text variable syntax and emits it.
     *
     * @param {AutoCompleteSelectEvent} $event - The autocomplete selection event containing the selected value
     * @example
     * // If user selects {key: "GLOBAL_SEARCH", value: "Search"},
     * // this will emit: "$text.get('GLOBAL_SEARCH')"
     */
    emitSelectLanguageVariable($event: AutoCompleteSelectEvent) {
        const { value } = $event;
        this.onSelectLanguageVariable.emit(`$text.get('${value.key}')`);
        this.#resetAutocomplete();
    }

    /**
     * Fetches and formats language variables from the DotCMS Languages API.
     * The variables are stored in the $languageVariables signal for use in the autocomplete.
     *
     * The formatting process:
     * 1. Fetches all language variables
     * 2. Prioritizes English ('en-us') translations
     * 3. Falls back to other languages if English is not available
     * 4. Uses the key as value if no translations are found
     *
     * @private
     */
    #getLanguageVariables() {
        // TODO: This is a temporary solution to get the language variables from the DotCMS Languages API.
        // We need a way to get the current language from the contentlet.
        this.#dotLanguagesService
            .getLanguageVariables()
            .pipe(take(1))
            .subscribe({
                next: (variables) => {
                    const formattedVariables = Object.entries(variables)
                        .map(([key, langObj]) => {
                            // Try to get the English value first
                            let value = langObj['en-us']?.value;

                            // If there is no English value, search for it in other languages
                            if (!value) {
                                for (const lang in langObj) {
                                    if (langObj[lang]?.value) {
                                        value = langObj[lang].value;
                                        break;
                                    }
                                }
                            }

                            // If there is no value, use the key
                            if (!value) {
                                value = key;
                            }

                            return { key, value };
                        })
                        .filter(
                            (variable) => variable.value !== null && variable.value !== undefined
                        );

                    this.$languageVariables.set(formattedVariables);
                },
                error: (error) => {
                    console.error('Error fetching language variables:', error);
                }
            });
    }

    /**
     * Resets the autocomplete state.
     */
    #resetAutocomplete() {
        this.$autoComplete()?.clear();
    }
}
