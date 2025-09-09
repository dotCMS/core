import { computed, inject } from '@angular/core';
import { ControlContainer, FormControl, ValidationErrors } from '@angular/forms';

/**
 * Base class for all field components that provides common functionality
 * for form control management, validation, and state handling.
 *
 * Note: Child components must define the $field input property.
 */
export abstract class BaseFieldComponent {
    /**
     * Control container for accessing form controls
     */
    readonly #controlContainer = inject(ControlContainer);

    /**
     * Abstract property that child components must implement
     * This should be the $field input property
     */
    abstract $field: () => { variable: string };

    /**
     * Computed form control for the current field
     */
    $formControl = computed(
        () => this.#controlContainer.control.get(this.$field().variable) as FormControl
    );

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
        return this.errors?.['required'] ? true : false;
    }

    /**
     * Checks if the field is disabled
     * @returns true if the field is disabled
     */
    get isDisabled(): boolean {
        return this.$formControl().disabled;
    }

    /**
     * Sets the value of the form control
     * @param value - The value to set
     */
    setValue(
        value: string,
        options?: {
            onlySelf?: boolean;
            emitEvent?: boolean;
            emitModelToViewChange?: boolean;
            emitViewToModelChange?: boolean;
        }
    ): void {
        this.$formControl().setValue(value, options);
    }

    /**
     * Gets the current value of the form control
     * @returns The current value of the form control
     */
    getValue(): unknown {
        return this.$formControl().value;
    }

    /**
     * Marks the field as touched
     */
    markAsTouched(): void {
        this.$formControl().markAsTouched();
    }

    /**
     * Marks the field as dirty
     */
    markAsDirty(): void {
        this.$formControl().markAsDirty();
    }

    /**
     * Resets the field to its initial state
     */
    reset(): void {
        this.$formControl().reset();
    }
}
