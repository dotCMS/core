import { Subscription } from 'rxjs';

/**
 * Interface for bridging form functionality between different frameworks.
 * Provides a unified API for form operations like getting/setting values and handling changes.
 */
export interface FormBridge {
    /**
     * Gets the value of a form field by its ID.
     * @param fieldId - The unique identifier of the form field
     * @returns The current value of the field
     */
    get(fieldId: string): FormFieldValue;

    /**
     * Sets the value of a form field.
     * @param fieldId - The unique identifier of the form field
     * @param value - The value to set for the field
     */
    set(fieldId: string, value: FormFieldValue): void;

    /**
     * Subscribes to changes of a specific form field.
     * Supports multiple callbacks per field.
     * @param fieldId - The unique identifier of the form field to watch
     * @param callback - Function to execute when the field value changes
     * @returns Function to unsubscribe this specific callback
     */
    onChangeField(fieldId: string, callback: (value: FormFieldValue) => void): () => void;

    /**
     * Optional method to handle bridge initialization.
     * @param callback - Function to execute when the bridge is ready
     */
    ready?(callback: (api: FormBridge) => void): void;

    /**
     * Cleans up resources and event listeners when the bridge is destroyed.
     */
    destroy(): void;
}

/**
 * Valid types for form field values.
 */
export type FormFieldValue = string | number | boolean | null;

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
