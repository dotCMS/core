import { Subscription } from 'rxjs';

/**
 * Valid types for form field values.
 */
export type FormFieldValue = string | number | boolean | null;

/**
 * Interface for a form field API that provides methods to interact with a specific field.
 */
export interface FormFieldAPI {
    /**
     * Gets the current value of the field.
     * @returns The current value of the field
     */
    getValue(): FormFieldValue;

    /**
     * Sets the value of the field.
     * @param value - The value to set for the field
     */
    setValue(value: FormFieldValue): void;

    /**
     * Subscribes to changes of the field.
     * @param callback - Function to execute when the field value changes
     */
    onChange(callback: (value: FormFieldValue) => void): void;

    /**
     * Enables the field, allowing user interaction.
     */
    enable(): void;

    /**
     * Disables the field, preventing user interaction.
     */
    disable(): void;
}

/**
 * A callback function that is executed when the value of a form field changes.
 *
 * @param {FormFieldValue} value - The new value of the field.
 */
export interface FieldCallback {
    id: symbol;
    callback: (value: FormFieldValue) => void;
}

/**
 * A subscription to a form field.
 *
 * @param {Subscription} subscription - The subscription to the field.
 * @param {FieldCallback[]} callbacks - The callbacks to execute when the field value changes.
 */
export interface FieldSubscription {
    subscription: Subscription;
    callbacks: FieldCallback[];
}
