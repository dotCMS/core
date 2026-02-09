import { NgZone } from '@angular/core';
import { FormGroup } from '@angular/forms';

import {
    FieldCallback,
    FieldSubscription,
    FormBridge,
    FormFieldAPI,
    FormFieldValue
} from '../interfaces/form-bridge.interface';

/**
 * Bridge class that enables form editing interoperability in Angular environments.
 * Provides a unified API for getting/setting field values and handling field changes.
 *
 * Angular integration uses FormGroup for form state and NgZone for change detection.
 * Supports multiple callbacks per field, similar to addEventListener.
 *
 * Implements the Singleton pattern to ensure only one instance exists at a time.
 * Use getInstance() to obtain the singleton instance.
 */
export class AngularFormBridge implements FormBridge {
    private static instance: AngularFormBridge | null = null;
    private fieldSubscriptions: Map<string, FieldSubscription> = new Map();

    private constructor(
        private form: FormGroup,
        private zone: NgZone
    ) {}

    /**
     * Gets the singleton instance of AngularFormBridge.
     * If an instance already exists, returns it. Otherwise, creates a new one.
     *
     * @param form - The Angular FormGroup to bridge
     * @param zone - The NgZone for change detection
     * @returns The singleton instance of AngularFormBridge
     */
    static getInstance(form: FormGroup, zone: NgZone): AngularFormBridge {
        if (!AngularFormBridge.instance) {
            AngularFormBridge.instance = new AngularFormBridge(form, zone);
        } else if (
            AngularFormBridge.instance.form !== form ||
            AngularFormBridge.instance.zone !== zone
        ) {
            console.warn(
                'AngularFormBridge: Attempted to get instance with different form or zone. ' +
                    'Returning existing instance. Consider calling resetInstance() first if you need a new instance.'
            );
        }
        return AngularFormBridge.instance;
    }

    /**
     * Resets the singleton instance, allowing a new instance to be created.
     * This will destroy the current instance and clear all subscriptions.
     */
    static resetInstance(): void {
        if (AngularFormBridge.instance) {
            AngularFormBridge.instance.destroy();
            AngularFormBridge.instance = null;
        }
    }

    /**
     * Retrieves the value of a field from the Angular form.
     *
     * @param fieldId - The ID of the field to retrieve the value from.
     * @returns The value of the field, or null if the field is not found.
     */
    get(fieldId: string): FormFieldValue {
        return this.form.get(fieldId)?.value;
    }

    /**
     * Sets the value of a field in the Angular form.
     *
     * @param fieldId - The ID of the field to set the value for.
     * @param value - The value to set for the field.
     */
    set(fieldId: string, value: FormFieldValue): void {
        this.zone.run(() => {
            const control = this.form.get(fieldId);
            if (control && control.value !== value) {
                control.setValue(value, { emitEvent: true });
                control.markAsTouched();
                control.markAsDirty();
                control.updateValueAndValidity({ emitEvent: true });
            }
        });
    }

    /**
     * Subscribes to field changes in the Angular form.
     * Supports multiple callbacks per field.
     *
     * @param fieldId - The ID of the field to subscribe to.
     * @param callback - The callback function to execute when the field changes.
     * @returns A function to unsubscribe this specific callback.
     */
    onChangeField(fieldId: string, callback: (value: FormFieldValue) => void): () => void {
        const control = this.form.get(fieldId);
        if (!control) {
            console.warn(`Field '${fieldId}' not found in form`);

            // eslint-disable-next-line @typescript-eslint/no-empty-function
            return () => {};
        }

        const callbackId = Symbol('fieldCallback');
        const fieldCallback: FieldCallback = { id: callbackId, callback };

        let fieldSubscription = this.fieldSubscriptions.get(fieldId);

        if (!fieldSubscription) {
            // Create new subscription for this field
            const subscription = control.valueChanges.subscribe((value) => {
                const currentFieldSubscription = this.fieldSubscriptions.get(fieldId);
                if (currentFieldSubscription) {
                    // Execute all callbacks for this field
                    currentFieldSubscription.callbacks.forEach(({ callback: cb }) => {
                        this.zone.run(() => cb(value));
                    });
                }
            });

            fieldSubscription = {
                subscription,
                callbacks: [fieldCallback]
            };
            this.fieldSubscriptions.set(fieldId, fieldSubscription);
        } else {
            // Add callback to existing subscription
            fieldSubscription.callbacks.push(fieldCallback);
        }

        // Return unsubscribe function for this specific callback
        return () => this.unsubscribeCallback(fieldId, callbackId);
    }

    /**
     * Unsubscribes a specific callback from field changes.
     *
     * @param fieldId - The ID of the field.
     * @param callbackId - The ID of the callback to remove.
     */
    private unsubscribeCallback(fieldId: string, callbackId: symbol): void {
        const fieldSubscription = this.fieldSubscriptions.get(fieldId);
        if (!fieldSubscription) return;

        // Remove the specific callback
        fieldSubscription.callbacks = fieldSubscription.callbacks.filter(
            ({ id }) => id !== callbackId
        );

        // If no more callbacks, clean up the subscription
        if (fieldSubscription.callbacks.length === 0) {
            fieldSubscription.subscription.unsubscribe();
            this.fieldSubscriptions.delete(fieldId);
        }
    }

    /**
     * Cleans up all subscriptions when the bridge is destroyed.
     * Also resets the singleton instance.
     */
    destroy(): void {
        this.fieldSubscriptions.forEach((fieldSubscription) => {
            fieldSubscription.subscription.unsubscribe();
        });
        this.fieldSubscriptions.clear();

        // Reset singleton instance if this is the current instance
        if (AngularFormBridge.instance === this) {
            AngularFormBridge.instance = null;
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
                this.zone.run(() => {
                    const control = this.form.get(fieldId);
                    if (control) {
                        control.enable({ emitEvent: true });
                    }
                });
            },

            disable: (): void => {
                this.zone.run(() => {
                    const control = this.form.get(fieldId);
                    if (control) {
                        control.disable({ emitEvent: true });
                    }
                });
            }
        };
    }

    /**
     * Executes callback when bridge is ready, handling iframe load.
     *
     * @param callback - The callback function to execute when the bridge is ready.
     */
    ready(callback: (api: FormBridge) => void): void {
        callback(this);
    }
}
