import { DotBrowserHandle, DotBrowserOptions } from './asset-browser.interface';
import { FormFieldAPI, FormFieldValue } from './form-field.interface';

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
     * Gets a field API object for a specific field, providing a convenient interface
     * to interact with the field (get/set value, onChange, enable/disable, show/hide).
     * @param fieldId - The unique identifier of the form field
     * @returns A FormFieldAPI object for the specified field
     */
    getField(fieldId: string): FormFieldAPI;

    /**
     * Optional method to handle bridge initialization.
     * @param callback - Function to execute when the bridge is ready
     */
    ready?(callback: (api: FormBridge) => void): void;

    /**
     * Cleans up resources and event listeners when the bridge is destroyed.
     */
    destroy(): void;

    /**
     * Opens the asset browser so the user can pick content — an asset, a page, a folder or a menu
     * link.
     *
     * Only the Angular host opens anything: the legacy Dojo editor has never had this dialog, and
     * its bridge resolves `null` with a warning rather than pretending.
     *
     * @param options What to browse. Every field is optional; the defaults browse assets only.
     * @returns A handle whose `result` resolves with the selection, or `null` if cancelled.
     *
     * @example
     * const { result } = bridge.openBrowserModal({ kinds: ['page', 'link'], status: 'live' });
     * const selection = await result;
     * if (selection) field.setValue(selection.url);
     */
    openBrowserModal(options?: DotBrowserOptions): DotBrowserHandle;
}

// Re-export all interfaces for backwards compatibility
export * from './asset-browser.interface';
export * from './form-field.interface';
