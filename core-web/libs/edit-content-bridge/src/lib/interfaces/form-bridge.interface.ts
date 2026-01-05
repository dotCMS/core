import { BrowserSelectorController, BrowserSelectorOptions } from './browser-selector.interface';
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
     * Opens a browser selector modal to allow the user to select content (pages, files, etc.).
     * The content type can be filtered using the mimeTypes option.
     *
     * @param options - Configuration options for the browser selector
     * @returns A controller object to manage the dialog
     *
     * @example
     * // Select a page
     * bridge.openBrowserModal({
     *   header: 'Select a Page',
     *   mimeTypes: ['application/dotpage'],
     *   onClose: (result) => console.log(result)
     * });
     *
     * @example
     * // Select an image
     * bridge.openBrowserModal({
     *   header: 'Select an Image',
     *   mimeTypes: ['image'],
     *   onClose: (result) => console.log(result)
     * });
     *
     * @example
     * // Select any file
     * bridge.openBrowserModal({
     *   header: 'Select a File',
     *   includeDotAssets: true,
     *   onClose: (result) => console.log(result)
     * });
     */
    openBrowserModal(options: BrowserSelectorOptions): BrowserSelectorController;
}

// Re-export all interfaces for backwards compatibility
export * from './browser-selector.interface';
export * from './form-field.interface';
