import { FormBridge, FormFieldAPI, FormFieldValue } from '../interfaces/form-bridge.interface';

interface FieldCallback {
    id: symbol;
    callback: (value: FormFieldValue) => void;
}

interface FieldListener {
    element: HTMLElement;
    handlers: { event: string; handler: (e: Event) => void }[];
    callbacks: FieldCallback[];
}

/**
 * Bridge class that enables form editing interoperability in Dojo environments.
 * Provides a unified API for getting/setting field values and handling field changes.
 *
 * Dojo integration uses DOM event listeners and iframe messaging.
 * Supports multiple callbacks per field, similar to addEventListener.
 */
export class DojoFormBridge implements FormBridge {
    private fieldListeners: Map<string, FieldListener> = new Map();
    private loadHandler?: () => void;

    constructor() {
        document.addEventListener('beforeunload', () => this.destroy());
    }

    /**
     * Retrieves the value of a field from the Dojo form.
     *
     * @param fieldId - The ID of the field to retrieve the value from.
     * @returns The value of the field, or null if the field is not found or an error occurs.
     */
    get(fieldId: string): FormFieldValue {
        try {
            const element = document.getElementById(fieldId);

            return element instanceof HTMLInputElement || element instanceof HTMLTextAreaElement
                ? element.value
                : null;
        } catch (error) {
            console.warn('Unable to get field value:', error);

            return null;
        }
    }

    /**
     * Sets the value of a field in the Dojo form.
     *
     * @param fieldId - The ID of the field to set the value for.
     * @param value - The value to set for the field.
     */
    set(fieldId: string, value: FormFieldValue): void {
        try {
            const element = document.getElementById(fieldId);
            if (element instanceof HTMLInputElement) {
                element.value = String(value ?? '');
                element.dispatchEvent(new Event('change', { bubbles: true }));
            } else if (element instanceof HTMLTextAreaElement) {
                element.textContent = String(value ?? '');
                element.dispatchEvent(new Event('change', { bubbles: true }));
            }
        } catch (error) {
            console.warn('Error setting field value:', error);
        }
    }

    /**
     * Subscribes to field changes in the Dojo form.
     * Supports multiple callbacks per field.
     *
     * @param fieldId - The ID of the field to subscribe to.
     * @param callback - The callback function to execute when the field changes.
     * @returns Function to unsubscribe this specific callback.
     */
    onChangeField(fieldId: string, callback: (value: FormFieldValue) => void): () => void {
        try {
            const callbackId = Symbol('fieldCallback');
            const fieldCallback: FieldCallback = { id: callbackId, callback };

            let listener = this.fieldListeners.get(fieldId);
            const element = document.getElementById(fieldId);

            if (
                !element ||
                !(element instanceof HTMLInputElement || element instanceof HTMLTextAreaElement)
            ) {
                // Return no-op unsubscribe function
                // eslint-disable-next-line @typescript-eslint/no-empty-function
                return () => {};
            }

            if (!listener) {
                // Create new listener for this field
                const handlers = [
                    {
                        event: 'keyup',
                        handler: (e: Event) => {
                            const value = (e.target as HTMLInputElement | HTMLTextAreaElement)
                                .value;
                            const currentListener = this.fieldListeners.get(fieldId);
                            if (currentListener) {
                                // Execute all callbacks for this field
                                currentListener.callbacks.forEach(({ callback: cb }) => cb(value));
                            }
                        }
                    },
                    {
                        event: 'change',
                        handler: (e: Event) => {
                            const value = (e.target as HTMLInputElement | HTMLTextAreaElement)
                                .value;
                            const currentListener = this.fieldListeners.get(fieldId);
                            if (currentListener) {
                                // Execute all callbacks for this field
                                currentListener.callbacks.forEach(({ callback: cb }) => cb(value));
                            }
                        }
                    }
                ];

                handlers.forEach(({ event, handler }) => {
                    element.addEventListener(event, handler);
                });

                listener = {
                    element,
                    handlers,
                    callbacks: [fieldCallback]
                };
                this.fieldListeners.set(fieldId, listener);
            } else {
                // Add callback to existing listener
                listener.callbacks.push(fieldCallback);
            }

            // Return unsubscribe function for this specific callback
            return () => this.unsubscribeCallback(fieldId, callbackId);
        } catch (error) {
            console.warn('Error watching field:', error);

            // eslint-disable-next-line @typescript-eslint/no-empty-function
            return () => {};
        }
    }

    /**
     * Unsubscribes a specific callback from field changes.
     *
     * @param fieldId - The ID of the field.
     * @param callbackId - The ID of the callback to remove.
     */
    private unsubscribeCallback(fieldId: string, callbackId: symbol): void {
        const listener = this.fieldListeners.get(fieldId);
        if (!listener) return;

        // Remove the specific callback
        listener.callbacks = listener.callbacks.filter(({ id }) => id !== callbackId);

        // If no more callbacks, clean up the entire listener
        if (listener.callbacks.length === 0) {
            const { element, handlers } = listener;
            handlers.forEach(({ event, handler }) => {
                element.removeEventListener(event, handler);
            });
            this.fieldListeners.delete(fieldId);
        }
    }

    private cleanupFieldListeners(fieldId: string): void {
        const listener = this.fieldListeners.get(fieldId);
        if (listener) {
            const { element, handlers } = listener;
            handlers.forEach(({ event, handler }) => {
                element.removeEventListener(event, handler);
            });
            this.fieldListeners.delete(fieldId);
        }
    }

    /**
     * Cleans up all event listeners when the bridge is destroyed.
     */
    destroy(): void {
        Array.from(this.fieldListeners.keys()).forEach((fieldId) => {
            this.cleanupFieldListeners(fieldId);
        });

        if (this.loadHandler) {
            window.removeEventListener('load', this.loadHandler);
            this.loadHandler = undefined;
        }
    }

    /**
     * Gets a field API object for a specific field, providing a convenient interface
     * to interact with the field (get/set value, onChange, enable/disable, show/hide).
     *
     * @param fieldId - The ID of the field to get the API for.
     * @returns A FormFieldAPI object for the specified field.
     */
    getField(fieldId: string): FormFieldAPI {
        return {
            getValue: (): FormFieldValue => {
                return this.get(fieldId);
            },

            setValue: (value: FormFieldValue): void => {
                this.set(fieldId, value);
            },

            onChange: (callback: (value: FormFieldValue) => void): void => {
                this.onChangeField(fieldId, callback);
            },

            enable: (): void => {
                try {
                    const element = document.getElementById(fieldId);
                    if (
                        element instanceof HTMLInputElement ||
                        element instanceof HTMLTextAreaElement
                    ) {
                        element.disabled = false;
                        element.removeAttribute('disabled');
                    }
                } catch (error) {
                    console.warn('Error enabling field:', error);
                }
            },

            disable: (): void => {
                try {
                    const element = document.getElementById(fieldId);
                    if (
                        element instanceof HTMLInputElement ||
                        element instanceof HTMLTextAreaElement
                    ) {
                        element.disabled = true;
                        element.setAttribute('disabled', 'disabled');
                    }
                } catch (error) {
                    console.warn('Error disabling field:', error);
                }
            },

            show: (): void => {
                try {
                    const element = document.getElementById(fieldId);
                    if (element) {
                        element.removeAttribute('data-bridge-hidden');
                        element.style.display = '';
                    }
                } catch (error) {
                    console.warn('Error showing field:', error);
                }
            },

            hide: (): void => {
                try {
                    const element = document.getElementById(fieldId);
                    if (element) {
                        element.setAttribute('data-bridge-hidden', 'true');
                        element.style.display = 'none';
                    }
                } catch (error) {
                    console.warn('Error hiding field:', error);
                }
            }
        };
    }

    /**
     * Executes callback when bridge is ready, handling iframe load.
     *
     * @param callback - The callback function to execute when the bridge is ready.
     */
    ready(callback: (api: FormBridge) => void): void {
        // Wait for iframe to be fully loaded
        this.loadHandler = () => {
            callback(this);
        };

        window.addEventListener('load', this.loadHandler);
    }
}
