import { Component, output, inject, OnInit, forwardRef, ChangeDetectionStrategy } from '@angular/core';
import {
    ControlValueAccessor,
    FormControl,
    NG_VALUE_ACCESSOR,
    ReactiveFormsModule
} from '@angular/forms';

import { DropdownModule } from 'primeng/dropdown';

import { DotLanguage } from '@dotcms/dotcms-models';

import { LanguageFieldStore } from './language-field.store';

@Component({
    selector: 'dot-language-field',
    standalone: true,
    imports: [DropdownModule, ReactiveFormsModule],
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
    protected readonly store = inject(LanguageFieldStore);

    /**
     * Output signal that emits when language selection changes
     */
    languageChange = output<DotLanguage>();

    /**
     * Form control for the language dropdown
     */
    readonly languageControl = new FormControl<DotLanguage | null>(null);

    ngOnInit(): void {
        this.store.loadLanguages();
    }

    private onChange = (_value: number | null): void => {
        // noop
    };

    private onTouched = (): void => {
        // noop
    };

    /**
     * Handles language selection change
     */
    handleLanguageChange(): void {
        const selectedLanguage = this.languageControl.value;

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
     * Writes a new value to the element
     */
    writeValue(value: number | null): void {
        const option = this.store.languages().find((lang) => lang.id === value);
        this.languageControl.setValue(option || null, { emitEvent: false });
        this.store.setSelectedLanguage(value || null);
    }

    /**
     * Registers a callback function that is called when the control's value changes
     */
    registerOnChange(fn: (value: number | null) => void): void {
        this.onChange = fn;
    }

    /**
     * Registers a callback function that is called when the control receives a blur event
     */
    registerOnTouched(fn: () => void): void {
        this.onTouched = fn;
    }

    /**
     * Function that is called when the control status changes to or from "DISABLED"
     */
    setDisabledState(isDisabled: boolean): void {
        if (isDisabled) {
            this.languageControl.disable();
        } else {
            this.languageControl.enable();
        }
    }
}
