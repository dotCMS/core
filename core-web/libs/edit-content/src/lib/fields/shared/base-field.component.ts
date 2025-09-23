import { ChangeDetectorRef, DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
    ControlValueAccessor,
    FormControl,
    Validators,
    TouchedChangeEvent,
    NgControl
} from '@angular/forms';

import { filter } from 'rxjs/operators';

/**
 * Base class for all field components that provides common functionality
 * for form control management, validation, and state handling.
 *
 * Note: Child components must define the $field input property.
 */
export abstract class BaseFieldComponent implements ControlValueAccessor {
    protected ngControl = inject(NgControl, { self: true });
    protected changeDetectorRef = inject(ChangeDetectorRef);
    protected destroyRef = inject(DestroyRef);

    constructor() {
        if (this.ngControl !== null) {
            this.ngControl.valueAccessor = this;
        }
    }

    protected onChange: (value: unknown) => void = () => {
        /* no-op */
    };
    protected onTouched: () => void = () => {
        /* no-op */
    };

    /**
     * Registers a callback function that is called when the control's value changes in the UI.
     * This function is passed to the {@link NG_VALUE_ACCESSOR} token.
     *
     * @param fn The callback function to register.
     */
    registerOnChange(fn: (value: string) => void) {
        this.onChange = fn;
    }

    /**
     * Registers a callback function that is called when the control is marked as touched in the UI.
     * This function is passed to the {@link NG_VALUE_ACCESSOR} token.
     *
     * @param fn The callback function to register.
     */
    registerOnTouched(fn: () => void) {
        this.onTouched = fn;
    }

    abstract writeValue(value: unknown): void;

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
        return this.ngControl.control as FormControl;
    }

    get statusChanges$() {
        return this.formControl.events.pipe(
            takeUntilDestroyed(this.destroyRef),
            filter((event) => event instanceof TouchedChangeEvent)
        );
    }
}
