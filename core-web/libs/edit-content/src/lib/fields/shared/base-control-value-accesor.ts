import { InputSignal, signal } from '@angular/core';
import { ControlValueAccessor } from '@angular/forms';

/**
 * Base class for all control value accesor components that provides common functionality
 * for form control management, validation, and state handling.
 *
 * Note: Child components must define the $value and $isDisabled signals.
 */
export abstract class BaseControlValueAccessor<T> implements ControlValueAccessor {
    // `T | null`: every accessor starts with no value, and `writeValue(null)` is how Angular
    // clears a control. The seed below was already null.
    $value = signal<T | null>(null);
    $isDisabled = signal<boolean>(false);
    abstract $hasError?: InputSignal<boolean>;

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
    // `unknown`, matching the `onChange` field it is stored in. The old `(value: string) => void`
    // was unsound in the other direction: subclasses call `onChange` with records, string arrays
    // and numbers, none of which a `string`-only callback can accept.
    registerOnChange(fn: (value: unknown) => void) {
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

    writeValue(value: T): void {
        this.$value.set(value);
    }

    setDisabledState(isDisabled: boolean): void {
        this.$isDisabled.set(isDisabled);
    }
}
