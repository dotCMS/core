import {
    Component,
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

import { DropdownModule } from 'primeng/dropdown';

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
    imports: [DropdownModule, ReactiveFormsModule, LanguagePipe, DotMessagePipe],
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
     * Initializes the component by loading available languages.
     */
    ngOnInit(): void {
        this.store.loadLanguages();
    }

    /**
     * Internal callback function for handling value changes.
     * @param value - The new language ID or null
     */
    private onChange = (_value: number | null): void => {
        // noop
    };

    /**
     * Internal callback function for handling touched state.
     */
    private onTouched = (): void => {
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
            this.onChange(selectedLanguage.id);
            this.languageChange.emit(selectedLanguage);
        } else {
            this.store.setSelectedLanguage(null);
            this.onChange(null);
        }

        this.onTouched();
    }

    /**
     * Writes a new value to the form control.
     * Part of ControlValueAccessor implementation.
     *
     * @param value - The language ID to set or null
     */
    writeValue(value: number | null): void {
        const option = this.store.languages().find((lang) => lang.id === value);
        this.languageControl.setValue(option || null, { emitEvent: false });
        this.store.setSelectedLanguage(option ? option.id : null);
    }

    /**
     * Registers the callback function for value changes.
     * Part of ControlValueAccessor implementation.
     *
     * @param fn - The callback function to register
     */
    registerOnChange(fn: (value: number | null) => void): void {
        this.onChange = fn;
    }

    /**
     * Registers the callback function for touched state.
     * Part of ControlValueAccessor implementation.
     *
     * @param fn - The callback function to register
     */
    registerOnTouched(fn: () => void): void {
        this.onTouched = fn;
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
