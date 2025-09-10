import { computed, inject } from '@angular/core';
import {
    ControlValueAccessor,
    FormControl,
    NgControl,
    ValidationErrors,
    Validators
} from '@angular/forms';

import { DotCMSContentTypeField } from '@dotcms/dotcms-models';

/**
 * Base class for all field components that provides common functionality
 * for form control management, validation, and state handling.
 *
 * Note: Child components must define the $field input property.
 */
export abstract class BaseFieldComponent implements ControlValueAccessor {
    ngControl = inject(NgControl, { self: true, optional: true });
    $formControl = computed(() => this.ngControl?.control as FormControl);

    $showLabel = computed(() => {
        const field = this.$field();
        if (!field) return true;

        return field.fieldVariables.find(({ key }) => key === 'hideLabel')?.value !== 'true';
    });

    constructor() {
        if (this.ngControl !== null) {
            this.ngControl.valueAccessor = this;
        }
    }
    /**
     * Abstract property that child components must implement
     * This should be the $field input property
     */
    abstract $field: () => DotCMSContentTypeField;

    /**
     * Checks if the field has validation errors and has been touched
     * @returns true if the field has errors and has been touched
     */
    get hasError(): boolean {
        const control = this.$formControl();
        return !!(control.invalid && control.touched);
    }

    /**
     * Gets the validation errors for the current field
     * @returns ValidationErrors object or null if no errors
     */
    get errors(): ValidationErrors | null {
        return this.$formControl().errors;
    }

    /**
     * Checks if the field is required based on validation errors
     * @returns true if the field is required
     */
    get isRequired(): boolean {
        const control = this.$formControl();
        if (control.hasValidator(Validators.required)) {
            return true;
        }
        return false;
    }

    /**
     * Checks if the field is disabled
     * @returns true if the field is disabled
     */
    get isDisabled(): boolean {
        return this.$formControl().disabled;
    }

    protected onChange: ((value: string) => void) | null = null;
    protected onTouched: (() => void) | null = null;

    /**
     * Registers a callback function that is called when the control's value changes in the UI.
     * This function is passed to the {@link NG_VALUE_ACCESSOR} token.
     *
     * @param fn The callback function to register.
     */
    registerOnChange(fn: (value: string) => void) {
        this.onChange = fn;
    }

    /**
     * Registers a callback function that is called when the control is marked as touched in the UI.
     * This function is passed to the {@link NG_VALUE_ACCESSOR} token.
     *
     * @param fn The callback function to register.
     */
    registerOnTouched(fn: () => void) {
        this.onTouched = fn;
    }

    abstract writeValue(value: unknown): void;
}
