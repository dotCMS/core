import { computed, DestroyRef, inject, InputSignal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, Validators, TouchedChangeEvent, ControlContainer } from '@angular/forms';

import { filter } from 'rxjs/operators';

import { DotCMSContentTypeField } from '@dotcms/dotcms-models';

/**
 * Base class for all field components that provides common functionality
 * for form control management, validation, and state handling.
 *
 * Note: Child components must define the $field input property.
 */
export abstract class BaseWrapperFieldComponent {
    protected destroyRef = inject(DestroyRef);
    protected controlContainer = inject(ControlContainer);
    abstract $field: InputSignal<DotCMSContentTypeField>;

    $showLabel = computed(() => {
        const field = this.$field();
        if (!field) return true;

        return field.fieldVariables.find(({ key }) => key === 'hideLabel')?.value !== 'true';
    });

    get hasError(): boolean {
        const control = this.formControl;
        if (!control) {
            return false;
        }
        return !!(control.invalid && control.touched);
    }

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
