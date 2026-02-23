import { signalState, patchState } from '@ngrx/signals';

import { CommonModule } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    computed,
    effect,
    forwardRef,
    HostListener,
    inject,
    input,
    model,
    OnInit,
    output,
    signal,
    viewChild
} from '@angular/core';
import { ControlValueAccessor, FormsModule, NG_VALUE_ACCESSOR } from '@angular/forms';

import { Select, SelectModule } from 'primeng/select';

import { DotLanguagesService } from '@dotcms/data-access';
import { DotLanguage } from '@dotcms/dotcms-models';

interface DotLanguageWithLabel extends DotLanguage {
    label: string;
}

/**
 * Represents the state for the DotLanguageSelectorComponent.
 */
interface DotLanguageSelectorState {
    /**
     * The list of loaded languages.
     */
    languages: DotLanguageWithLabel[];

    /**
     * The currently pinned option (shown at top of list).
     * This is the selected value that may not exist in the loaded list yet.
     */
    pinnedOption: DotLanguageWithLabel | null;

    /**
     * Indicates whether the languages are currently being loaded.
     */
    loading: boolean;
}

@Component({
    selector: 'dot-language-selector',
    imports: [CommonModule, FormsModule, SelectModule],
    templateUrl: './dot-language-selector.component.html',
    styles: [
        `
            :host {
                display: contents;
            }
        `
    ],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => DotLanguageSelectorComponent),
            multi: true
        }
    ]
})
export class DotLanguageSelectorComponent implements ControlValueAccessor, OnInit {
    private languagesService = inject(DotLanguagesService);

    /**
     * For UX parity with `DotSiteComponent`: focusing the host focuses the select input.
     */
    @HostListener('focus')
    onHostFocus(): void {
        this.select()?.focusInputViewChild?.nativeElement?.focus();
    }

    /**
     * Signal-based ViewChild for the underlying PrimeNG Select.
     * Uses the template ref `#select` from the component template.
     */
    select = viewChild<Select>('select');

    /**
     * Placeholder text to be shown in the select input when empty.
     */
    placeholder = input<string>('');

    /**
     * Whether the select is disabled.
     * Settable via component input.
     */
    disabled = input<boolean>(false);

    /**
     * Two-way model binding for the selected language.
     * Accepts a number (language id), a DotLanguage object, or null.
     *
     * Note: This component enforces a selected language; when null is written it will fall back
     * to the default language (or first language) once languages are loaded.
     */
    value = model<number | DotLanguage | null>(null);

    /**
     * Disabled state from the ControlValueAccessor interface.
     * True when the form control disables this component.
     * @internal
     */
    $isDisabled = signal<boolean>(false);

    /**
     * Combined disabled state: true if either the input or ControlValueAccessor disabled state is true.
     * Used to control the actual disabled property of the select.
     */
    $disabled = computed(() => this.disabled() || this.$isDisabled());

    /**
     * Output event emitted whenever the selected language changes.
     * Emits the language id.
     */
    onChange = output<number>();

    /**
     * Output event emitted whenever the selected language changes.
     * Emits the full language object selected in the dropdown.
     */
    onLanguageChange = output<DotLanguage>();

    /**
     * Output event emitted when the select dropdown overlay is shown.
     */
    onShow = output<void>();

    /**
     * Output event emitted when the select dropdown overlay is hidden.
     */
    onHide = output<void>();

    /**
     * CSS class(es) applied to the select component wrapper.
     * Default is 'w-full'.
     */
    class = input<string>('w-auto');

    /**
     * The HTML id attribute for the select input.
     */
    id = input<string>('');

    /**
     * Reactive state of the component including loaded languages, loading status, and active filter.
     */
    readonly $state = signalState<DotLanguageSelectorState>({
        languages: [],
        pinnedOption: null,
        loading: false
    });

    // ControlValueAccessor callback functions
    private onChangeCallback = (_value: number | null) => {
        // Implementation provided by registerOnChange
    };

    private onTouchedCallback = () => {
        // Implementation provided by registerOnTouched
    };

    /**
     * Keeps internal state (`pinnedOption`) and the CVA value in sync.
     *
     * Notes:
     * - We load **all languages** once in `ngOnInit`, so we do not fetch individual languages by id.
     * - If `value` is null/undefined, we enforce selection by falling back to default language (or first).
     */
    constructor() {
        effect(() => {
            const id = this.extractId(this.value());
            this.onChangeCallback(id);

            // Enforce a selected language (fallback to default/first once loaded)
            if (!id) {
                const fallbackId = this.getFallbackLanguageId();
                if (fallbackId) {
                    // Avoid loops: only set if different
                    if (this.extractId(this.value()) !== fallbackId) {
                        this.value.set(fallbackId);
                    }
                } else {
                    patchState(this.$state, { pinnedOption: null });
                }
                return;
            }

            const currentPinned = this.$state.pinnedOption();
            if (currentPinned?.id === id) {
                return;
            }

            // Before the initial load finishes we may not have any options yet.
            // Avoid extra HTTP calls; `ngOnInit` will set `pinnedOption` once languages are loaded.
            if (this.$state.languages().length === 0) {
                return;
            }

            const inList = this.$state.languages().find((l) => l.id === id);
            if (inList) {
                patchState(this.$state, { pinnedOption: inList });
                return;
            }
        });
    }

    /**
     * Loads all system languages once and sets a default selection if needed.
     *
     * TODO: Add server-side lazy loading + server-side filtering once the backend provides an
     * endpoint that supports pagination/filtering for languages.
     */
    ngOnInit(): void {
        patchState(this.$state, { loading: true });
        this.languagesService.get().subscribe({
            next: (languages) => {
                const withLabels = languages.map((l) => this.withLabel(l));
                const sorted = this.sortLanguages(withLabels);
                patchState(this.$state, { languages: sorted, loading: false });

                // Ensure we always have a selected language (default first)
                const currentId = this.extractId(this.value());
                const nextId = currentId ?? this.getFallbackLanguageId();
                if (nextId) {
                    this.value.set(nextId);
                    const selected = sorted.find((l) => l.id === nextId);
                    patchState(this.$state, {
                        pinnedOption: selected ?? this.$state.pinnedOption()
                    });
                }
            },
            error: () => {
                patchState(this.$state, { loading: false });
            }
        });
    }

    /**
     * Handles the event when the selected language changes.
     *
     * @param language The selected language, or null (ignored; selection is enforced)
     */
    handleLanguageChange(language: DotLanguageWithLabel | null): void {
        if (!language) {
            // Enforce selection: revert to fallback language if user attempts to clear
            const fallbackId = this.getFallbackLanguageId();
            if (fallbackId) {
                this.value.set(fallbackId);
            }
            return;
        }

        patchState(this.$state, { pinnedOption: language });
        this.value.set(language.id);
        this.onTouchedCallback();
        this.onChange.emit(language.id);
        this.onLanguageChange.emit(language);
    }

    /**
     * Handles the event when the select overlay is hidden.
     */
    onSelectHide(): void {
        this.onHide.emit();
    }

    // ControlValueAccessor implementation
    /**
     * Writes a value from the form model into the component.
     */
    writeValue(value: number | DotLanguage | null): void {
        const id = this.extractId(value);
        this.value.set(id);
    }

    /**
     * Registers a callback to be invoked when the value changes.
     */
    registerOnChange(fn: (value: number | null) => void): void {
        this.onChangeCallback = fn;
    }

    /**
     * Registers a callback to be invoked when the component is touched.
     */
    registerOnTouched(fn: () => void): void {
        this.onTouchedCallback = fn;
    }

    /**
     * Sets the disabled state from the parent form control.
     */
    setDisabledState(isDisabled: boolean): void {
        this.$isDisabled.set(isDisabled);
    }

    /**
     * Sorts languages alphabetically by the computed label.
     */
    private sortLanguages(languages: DotLanguageWithLabel[]): DotLanguageWithLabel[] {
        return [...languages].sort((a, b) => a.label.localeCompare(b.label));
    }

    /**
     * Adds a `label` field used for display and filtering.
     */
    private withLabel(language: DotLanguage): DotLanguageWithLabel {
        return {
            ...language,
            label: this.createLanguageLabel(language)
        };
    }

    /**
     * Creates the display label for a language.
     * Format: `language - countryCode` (or just `language` when countryCode is empty).
     */
    private createLanguageLabel(lang: DotLanguage): string {
        return lang.countryCode?.trim()?.length
            ? `${lang.language} - ${lang.countryCode}`
            : lang.language;
    }

    /**
     * Extracts the language id from a value that can be a number, DotLanguage object, or null.
     * Handles backward compatibility with DotLanguage objects.
     */
    private extractId(value: number | DotLanguage | null): number | null {
        if (value === null || value === undefined) {
            return null;
        }

        if (typeof value === 'number') {
            return value;
        }

        if (typeof value === 'object' && 'id' in value && typeof value.id === 'number') {
            return value.id;
        }

        console.warn(
            `DotLanguageSelectorComponent: Invalid language value provided. Expected number or object with 'id' property of type number, but received:`,
            value
        );
        return null;
    }

    /**
     * Returns the language id that should be selected when there is no explicit value.
     * Prefers:
     * - Current pinned option (if any)
     * - `defaultLanguage === true` (from API)
     * - First available language
     */
    private getFallbackLanguageId(): number | null {
        const currentPinned = this.$state.pinnedOption();
        if (currentPinned?.id) {
            return currentPinned.id;
        }

        const langs = this.$state.languages();
        if (!langs.length) {
            return null;
        }

        const defaultLang = langs.find((l) => !!l.defaultLanguage);
        return (defaultLang ?? langs[0]).id;
    }
}
