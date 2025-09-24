import { InputSignal, signal } from '@angular/core';
import { ControlValueAccessor } from '@angular/forms';

/**
 * Base class for all control value accesor components that provides common functionality
 * for form control management, validation, and state handling.
 *
 * Note: Child components must define the $value and $isDisabled signals.
 */
export abstract class BaseControlValueAccesor<T> implements ControlValueAccessor {
    $value = signal<T>(null);
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

    writeValue(value: T): void {
        this.$value.set(value);
    }

    setDisabledState(isDisabled: boolean): void {
        this.$isDisabled.set(isDisabled);
    }
}
