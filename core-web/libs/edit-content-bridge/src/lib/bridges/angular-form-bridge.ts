import { Subscription } from 'rxjs';

import { NgZone } from '@angular/core';
import { FormGroup } from '@angular/forms';

import { FormBridge, FormFieldValue } from '../interfaces/form-bridge.interface';

/**
 * Bridge class that enables form editing interoperability in Angular environments.
 * Provides a unified API for getting/setting field values and handling field changes.
 *
 * Angular integration uses FormGroup for form state and NgZone for change detection.
 */
export class AngularFormBridge implements FormBridge {
    private subscriptions: Map<string, Subscription> = new Map();

    constructor(
        private form: FormGroup,
        private zone: NgZone
    ) {}

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
     *
     * @param fieldId - The ID of the field to subscribe to.
     * @param callback - The callback function to execute when the field changes.
     */
    onChangeField(fieldId: string, callback: (value: FormFieldValue) => void): void {
        const existingSubscription = this.subscriptions.get(fieldId);
        if (existingSubscription) {
            existingSubscription.unsubscribe();
            this.subscriptions.delete(fieldId);
        }

        const subscription = this.form.get(fieldId)?.valueChanges.subscribe((value) => {
            this.zone.run(() => callback(value));
        });

        if (subscription) {
            this.subscriptions.set(fieldId, subscription);
        }
    }

    /**
     * Cleans up all subscriptions when the bridge is destroyed.
     */
    destroy(): void {
        this.subscriptions.forEach((subscription) => subscription.unsubscribe());
        this.subscriptions.clear();
    }
}
