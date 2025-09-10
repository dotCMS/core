import { computed, inject } from '@angular/core';
import { ControlValueAccessor, FormControl, NgControl, Validators } from '@angular/forms';

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

    protected onChange: ((value: unknown) => void) | null = null;
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

    get hasError(): boolean {
        const control = this.$formControl();
        return !!(control.invalid && control.touched);
    }

    get isRequired(): boolean {
        const control = this.$formControl();
        if (control.hasValidator(Validators.required)) {
            return true;
        }
        return false;
    }

    get isDisabled(): boolean {
        return this.$formControl().disabled;
    }
}
