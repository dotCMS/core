import {
    Component,
    computed,
    effect,
    output,
    inject,
    OnInit,
    forwardRef,
    ChangeDetectionStrategy
} from '@angular/core';
import {
    ControlValueAccessor,
    FormControl,
    NG_VALUE_ACCESSOR,
    ReactiveFormsModule
} from '@angular/forms';

import { SelectModule } from 'primeng/select';

import { DotLanguage } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { LanguageFieldStore } from './language-field.store';

import { LanguagePipe } from '../../../../../../../../pipes/language.pipe';

/**
 * Language field component that provides a dropdown for language selection.
 * Implements ControlValueAccessor for form integration and OnInit for initialization.
 *
 * @implements {ControlValueAccessor}
 * @implements {OnInit}
 */
@Component({
    selector: 'dot-language-field',
    imports: [SelectModule, ReactiveFormsModule, LanguagePipe, DotMessagePipe],
    providers: [
        LanguageFieldStore,
        {
            multi: true,
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => LanguageFieldComponent)
        }
    ],
    templateUrl: './language-field.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class LanguageFieldComponent implements ControlValueAccessor, OnInit {
    /** Store instance for managing language state */
    protected readonly store = inject(LanguageFieldStore);

    /**
     * Output signal that emits when language selection changes.
     * Emits the selected DotLanguage object.
     */
    languageChange = output<DotLanguage>();

    /**
     * Form control for the language dropdown.
     * Manages the selected language state in the form.
     */
    readonly languageControl = new FormControl<DotLanguage | null>(null);

    /**
     * Reactive ISO code label of the selected language.
     * Used by the parent SearchComponent to compute chip labels reactively.
     */
    readonly $selectedLanguageLabel = computed(
        () => this.store.selectedLanguage()?.isoCode ?? null
    );

    constructor() {
        // Sync languageControl when languages load and a selectedLanguageId exists
        // (handles the case where writeValue is called before languages are loaded)
        effect(() => {
            const languages = this.store.languages();
            const selectedId = this.store.selectedLanguageId();

            if (languages.length > 0 && selectedId) {
                const option = languages.find((l) => l.id === selectedId);

                if (option && this.languageControl.value?.id !== option.id) {
                    this.languageControl.setValue(option, { emitEvent: false });
                }
            }
        });
    }

    /**
     * Initializes the component by loading available languages.
     */
    ngOnInit(): void {
        this.store.loadLanguages();
    }

    /**
     * Internal callback function for handling value changes.
     * @param value - The new language ID or null
     */
    #onChange = (_value: number | null): void => {
        // noop
    };

    /**
     * Internal callback function for handling touched state.
     * Required by ControlValueAccessor, registered via registerOnTouched.
     */
    #onTouched = (): void => {
        // noop
    };

    /**
     * Handles language selection changes from the dropdown.
     * Updates the form control value, emits the change event, and marks as touched.
     */
    handleLanguageChange(): void {
        const selectedLanguage = this.languageControl.value;
        const disabled = this.languageControl.disabled;

        if (disabled) {
            return;
        }

        if (selectedLanguage) {
            this.store.setSelectedLanguage(selectedLanguage.id);
            this.#onChange(selectedLanguage.id);
            this.languageChange.emit(selectedLanguage);
        } else {
            this.store.setSelectedLanguage(null);
            this.#onChange(null);
        }

        this.#onTouched();
    }

    /**
     * Writes a new value to the form control.
     * Part of ControlValueAccessor implementation.
     * Handles the case where languages haven't loaded yet by setting a pending ID.
     *
     * @param value - The language ID to set or null
     */
    writeValue(value: number | null): void {
        if (value == null || value === -1) {
            this.languageControl.setValue(null, { emitEvent: false });
            this.store.setSelectedLanguage(null);

            return;
        }

        const option = this.store.languages().find((lang) => lang.id === value);

        if (option) {
            this.languageControl.setValue(option, { emitEvent: false });
            this.store.setSelectedLanguage(option.id);
        } else {
            // Languages not loaded yet — store as pending for auto-selection after load
            this.store.setPendingLanguageId(value);
        }
    }

    /**
     * Registers the callback function for value changes.
     * Part of ControlValueAccessor implementation.
     *
     * @param fn - The callback function to register
     */
    registerOnChange(fn: (value: number | null) => void): void {
        this.#onChange = fn;
    }

    /**
     * Registers the callback function for touched state.
     * Part of ControlValueAccessor implementation.
     *
     * @param fn - The callback function to register
     */
    registerOnTouched(fn: () => void): void {
        this.#onTouched = fn;
    }

    /**
     * Sets the disabled state of the form control.
     * Part of ControlValueAccessor implementation.
     *
     * @param isDisabled - Whether the control should be disabled
     */
    setDisabledState(isDisabled: boolean): void {
        if (isDisabled) {
            this.languageControl.disable();
        } else {
            this.languageControl.enable();
        }
    }
}
