import { Subscription } from 'rxjs';

/**
 * Valid types for form field values.
 */
export type FormFieldValue = string | number | boolean | null;

/**
 * Validation state of a form field, mirroring Angular's AbstractControl state.
 * Custom field implementations (VTL or native) can read this to apply their
 * own visual feedback (red border, error icon, etc).
 */
export interface FieldValidationState {
    /** True when the field passes all validators. */
    valid: boolean;
    /** True when the field fails at least one validator. */
    invalid: boolean;
    /** True after the user has interacted with the field (blurred at least once, or marked touched on save). */
    touched: boolean;
    /** True when the user has changed the field value. */
    dirty: boolean;
    /**
     * Validation errors keyed by validator name (e.g. `{ required: true }`).
     * Null when the field is valid.
     */
    errors: Record<string, unknown> | null;
}

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
     * @returns A function to unsubscribe this specific callback
     */
    onChange(callback: (value: FormFieldValue) => void): () => void;

    /**
     * Returns the current validation state of the field.
     */
    getValidationState(): FieldValidationState;

    /**
     * Subscribes to validation state changes. Fires whenever the field's value,
     * status, errors, touched, or dirty state changes.
     * @param callback - Function to execute with the new validation state
     * @returns A function to unsubscribe this specific callback
     */
    onValidationChange(callback: (state: FieldValidationState) => void): () => void;

    /**
     * Enables the field, allowing user interaction.
     */
    enable(): void;

    /**
     * Disables the field, preventing user interaction.
     */
    disable(): void;

    /**
     * Shows the field, making it visible in the form.
     */
    show(): void;

    /**
     * Hides the field, making it invisible in the form while preserving its state.
     */
    hide(): void;
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
