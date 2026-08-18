import { merge } from 'rxjs';

import { afterNextRender, computed, DestroyRef, inject, Signal, signal } from '@angular/core';
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
    /**
     * `Signal`, not `InputSignal`: this class only ever *reads* these, and `InputSignal<T>` is
     * invariant in `T` — declaring one here would force all 17 subclasses to use byte-identical
     * input types, which they legitimately do not. The custom and JSON fields default `field` to
     * null, the text area declares both as `input.required<T | null>`, and the other 14 use
     * `input.required<T>`. `Signal<T>` is covariant, so every one of those satisfies this.
     *
     * Nullable because this class's own members already assume it: `$showLabel` returns early on
     * `!field` and `isRequired` reads `field?.required`.
     */
    abstract $field: Signal<DotCMSContentTypeField | null>;
    abstract $contentlet: Signal<DotCMSContentlet | null>;

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
        // First check the field definition (source of truth)
        const field = this.$field();
        if (field?.required) {
            return true;
        }

        // Fallback to checking the validator (for fields using standard Validators.required)
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

    /**
     * `| null` states what the body always did: `ControlContainer.control` is nullable, `get()`
     * returns null for an unknown name, and the field itself may not be bound yet. The old
     * `as FormControl` hid all three — which is why every caller in this class and its subclasses
     * already checks the result before using it.
     */
    get formControl(): FormControl | null {
        const field = this.$field();

        if (!field) {
            return null;
        }

        return (this.controlContainer.control?.get(field.variable) as FormControl) ?? null;
    }

    get statusChanges$() {
        return this.formControl?.events.pipe(
            takeUntilDestroyed(this.destroyRef),
            filter((event) => event instanceof TouchedChangeEvent)
        );
    }
}
