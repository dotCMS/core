import { Subscription } from 'rxjs';

import { NgZone } from '@angular/core';
import { FormGroup } from '@angular/forms';

interface AngularConfig {
    type: 'angular';
    form: FormGroup;
    iframe: HTMLIFrameElement;
    zone: NgZone;
}

interface DojoConfig {
    type: 'dojo';
}

type BridgeConfig = AngularConfig | DojoConfig;

/**
 * Bridge class that enables form editing interoperability between Angular and Dojo environments.
 * Provides a unified API for getting/setting field values and handling field changes across both frameworks.
 *
 * The public API exposed through createPublicApi() includes:
 * - get(fieldId): Gets field value from either Angular FormGroup or Dojo form
 * - set(fieldId, value): Sets field value in either framework's form
 * - onChangeField(fieldId, callback): Subscribes to field changes with framework-specific handlers
 * - ready(callback): Executes callback when bridge is ready, handling iframe load for Dojo
 *
 * Angular integration uses FormGroup for form state and NgZone for change detection.
 * Dojo integration uses iframe messaging and DOM event listeners.
 */
export class DotFormBridge {
    private environment: 'angular' | 'dojo';
    private form?: FormGroup;
    private iframe?: HTMLIFrameElement;
    private zone?: NgZone;
    private subscriptions: Map<string, Subscription> = new Map();
    private dojoFieldListeners: Map<
        string,
        { element: HTMLElement; handlers: { event: string; handler: (e: Event) => void }[] }
    > = new Map();
    private loadHandler: (() => void) | undefined;

    constructor(config: BridgeConfig) {
        this.environment = config.type;
        if (config.type === 'angular') {
            this.form = config.form;
            this.iframe = config.iframe;
            this.zone = config.zone;
        }
    }

    /**
     * Creates a public API object that exposes the bridge's functionality to the outside world.
     * This API is used by the Angular component to interact with the bridge.
     *
     * @returns An object with methods for getting, setting, and subscribing to field changes.
     */
    createPublicApi() {
        if (this.environment === 'dojo') {
            document.addEventListener('beforeunload', () => this.destroy());
        }

        return {
            get: (fieldId: string): string => {
                return this.environment === 'dojo'
                    ? this.getDojoFieldValue(fieldId)
                    : this.getAngularFieldValue(fieldId);
            },

            set: (fieldId: string, value: string): void => {
                this.environment === 'dojo'
                    ? this.setDojoFieldValue(fieldId, value)
                    : this.setAngularFieldValue(fieldId, value);
            },

            onChangeField: (fieldId: string, callback: (value: string) => void): void => {
                this.environment === 'dojo'
                    ? this.watchDojoField(fieldId, callback)
                    : this.watchAngularField(fieldId, callback);
            },

            ready: (callback: (api: any) => void): void => {
                if (this.environment === 'dojo') {
                    // Wait for iframe to be fully loaded
                    this.loadHandler = () => {
                        callback(this.createPublicApi());
                    };

                    window.addEventListener('load', this.loadHandler);
                }
            }
        };
    }

    /**
     * Retrieves the value of a field from the Dojo environment.
     *
     * @param fieldId - The ID of the field to retrieve the value from.
     * @returns The value of the field, or null if the field is not found or an error occurs.
     */
    private getDojoFieldValue(fieldId: string): any {
        try {
            const element = document.getElementById(fieldId);

            return element instanceof HTMLInputElement ? element.value : null;
        } catch (error) {
            console.warn('Unable to get field value:', error);

            return null;
        }
    }

    /**
     * Retrieves the value of a field from the Angular environment.
     *
     * @param fieldId - The ID of the field to retrieve the value from.
     * @returns The value of the field, or null if the field is not found or an error occurs.
     */
    private getAngularFieldValue(fieldId: string): any {
        return this.form?.get(fieldId)?.value;
    }

    /**
     * Sets the value of a field in the Dojo environment.
     *
     * @param fieldId - The ID of the field to set the value for.
     * @param value - The value to set for the field.
     */
    private setDojoFieldValue(fieldId: string, value: any): void {
        try {
            const element = document.getElementById(fieldId);
            if (element instanceof HTMLInputElement) {
                element.value = value;
                element.dispatchEvent(new Event('change', { bubbles: true }));
            }
        } catch (error) {
            console.warn('Error setting field value:', error);
        }
    }

    /**
     * Sets the value of a field in the Angular environment.
     *
     * @param fieldId - The ID of the field to set the value for.
     * @param value - The value to set for the field.
     */
    private setAngularFieldValue(fieldId: string, value: any): void {
        this.zone?.run(() => {
            const control = this.form?.get(fieldId);
            if (control && control.value !== value) {
                control.setValue(value, { emitEvent: false });
                control.markAsTouched();
                control.updateValueAndValidity({ emitEvent: false });
            }
        });
    }

    /**
     * Subscribes to field changes in the Dojo environment.
     *
     * @param fieldId - The ID of the field to subscribe to.
     * @param callback - The callback function to execute when the field changes.
     */
    private watchDojoField(fieldId: string, callback: (value: any) => void): void {
        try {
            // Clean up previous listeners if they exist
            this.cleanupDojoFieldListeners(fieldId);

            const element = document.getElementById(fieldId);
            if (element instanceof HTMLInputElement) {
                const handlers = [
                    {
                        event: 'keyup',
                        handler: (e: Event) => callback((e.target as HTMLInputElement).value)
                    },
                    {
                        event: 'change',
                        handler: (e: Event) => callback((e.target as HTMLInputElement).value)
                    }
                ];

                // Add new listeners
                handlers.forEach(({ event, handler }) => {
                    element.addEventListener(event, handler);
                });

                // Save reference for cleanup
                this.dojoFieldListeners.set(fieldId, { element, handlers });
            }
        } catch (error) {
            console.warn('Error watching field:', error);
        }
    }

    /**
     * Cleans up event listeners for a field in the Dojo environment.
     *
     * @param fieldId - The ID of the field to clean up listeners for.
     */
    private cleanupDojoFieldListeners(fieldId: string): void {
        const listener = this.dojoFieldListeners.get(fieldId);
        if (listener) {
            const { element, handlers } = listener;
            handlers.forEach(({ event, handler }) => {
                element.removeEventListener(event, handler);
            });
            this.dojoFieldListeners.delete(fieldId);
        }
    }

    /**
     * Subscribes to field changes in the Angular environment.
     *
     * @param fieldId - The ID of the field to subscribe to.
     * @param callback - The callback function to execute when the field changes.
     */
    private watchAngularField(fieldId: string, callback: (value: any) => void): void {
        const existingSubscription = this.subscriptions.get(fieldId);
        if (existingSubscription) {
            existingSubscription.unsubscribe();
            this.subscriptions.delete(fieldId);
        }

        const subscription = this.form?.get(fieldId)?.valueChanges.subscribe((value) => {
            this.zone?.run(() => callback(value));
        });

        if (subscription) {
            this.subscriptions.set(fieldId, subscription);
        }
    }

    /**
     * Cleans up all event listeners and subscriptions when the bridge is destroyed.
     */
    destroy(): void {
        // Clean up all Angular subscriptions
        this.subscriptions.forEach((subscription) => subscription.unsubscribe());
        this.subscriptions.clear();

        // Clean up all Dojo listeners
        Array.from(this.dojoFieldListeners.keys()).forEach((fieldId) => {
            this.cleanupDojoFieldListeners(fieldId);
        });

        // Clean up load handler if it exists
        if (this.loadHandler) {
            window.removeEventListener('load', this.loadHandler);
            this.loadHandler = undefined;
        }
    }
}
