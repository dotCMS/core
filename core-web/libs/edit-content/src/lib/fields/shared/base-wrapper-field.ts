import { merge } from 'rxjs';

import { afterNextRender, computed, DestroyRef, inject, InputSignal, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ControlContainer, FormControl, TouchedChangeEvent, Validators } from '@angular/forms';

import { filter } from 'rxjs/operators';

import { DotCMSContentlet, DotCMSContentTypeField } from '@dotcms/dotcms-models';

/**
 * Base class for all wrapper field components that provides common functionality
 * for form control management, validation, and state handling.
 *
 * Note: Child components must define the $field input property.
 */
export abstract class BaseWrapperField {
    protected destroyRef = inject(DestroyRef);
    protected controlContainer = inject(ControlContainer);
    abstract $field: InputSignal<DotCMSContentTypeField>;
    abstract $contentlet: InputSignal<DotCMSContentlet>;

    /**
     * A signal that holds the error state of the field.
     * It is used to display the error state in the field component.
     */
    $hasError = signal(false);

    constructor() {
        afterNextRender(() => {
            const control = this.formControl;
            if (!control) return;

            const updateState = () => {
                this.$hasError.set(!!(control.invalid && control.touched));
            };

            // Initial state
            updateState();

            merge(control.valueChanges, control.statusChanges, control.events)
                .pipe(takeUntilDestroyed(this.destroyRef))
                .subscribe(() => {
                    updateState();
                });
        });
    }

    $showLabel = computed(() => {
        const field = this.$field();
        if (!field) return true;

        return field.fieldVariables.find(({ key }) => key === 'hideLabel')?.value !== 'true';
    });

    get isRequired(): boolean {
        const control = this.formControl;
        if (!control) {
            return false;
        }
        return control.hasValidator(Validators.required);
    }

    get isDisabled(): boolean {
        const control = this.formControl;
        if (!control) {
            return false;
        }
        return control.disabled;
    }

    get formControl(): FormControl {
        const { variable } = this.$field();
        return this.controlContainer.control.get(variable) as FormControl;
    }

    get statusChanges$() {
        return this.formControl.events.pipe(
            takeUntilDestroyed(this.destroyRef),
            filter((event) => event instanceof TouchedChangeEvent)
        );
    }
}
