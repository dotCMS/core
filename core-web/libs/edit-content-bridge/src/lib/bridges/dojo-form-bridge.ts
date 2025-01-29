import { FormBridge, FormFieldValue } from '../interfaces/form-bridge.interface';

interface FieldListener {
    element: HTMLElement;
    handlers: { event: string; handler: (e: Event) => void }[];
}

/**
 * Bridge class that enables form editing interoperability in Dojo environments.
 * Provides a unified API for getting/setting field values and handling field changes.
 *
 * Dojo integration uses DOM event listeners and iframe messaging.
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
     *
     * @param fieldId - The ID of the field to subscribe to.
     * @param callback - The callback function to execute when the field changes.
     */
    onChangeField(fieldId: string, callback: (value: FormFieldValue) => void): void {
        try {
            this.cleanupFieldListeners(fieldId);

            const element = document.getElementById(fieldId);
            if (element instanceof HTMLInputElement || element instanceof HTMLTextAreaElement) {
                const handlers = [
                    {
                        event: 'keyup',
                        handler: (e: Event) =>
                            callback((e.target as HTMLInputElement | HTMLTextAreaElement).value)
                    },
                    {
                        event: 'change',
                        handler: (e: Event) =>
                            callback((e.target as HTMLInputElement | HTMLTextAreaElement).value)
                    }
                ];

                handlers.forEach(({ event, handler }) => {
                    element.addEventListener(event, handler);
                });

                this.fieldListeners.set(fieldId, { element, handlers });
            }
        } catch (error) {
            console.warn('Error watching field:', error);
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
