import { signalMethod } from '@ngrx/signals';

import { Component, ChangeDetectionStrategy, signal, input, forwardRef } from '@angular/core';
import { NG_VALUE_ACCESSOR } from '@angular/forms';

import { DotKeyValue, DotKeyValueComponent } from '@dotcms/ui';

import { BaseControlValueAccessor } from '../../../shared/base-control-value-accesor';

@Component({
    selector: 'dot-key-value-field',
    imports: [DotKeyValueComponent],
    templateUrl: './key-value-field.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [
        {
            multi: true,
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => DotKeyValueFieldComponent)
        }
    ]
})
export class DotKeyValueFieldComponent extends BaseControlValueAccessor<
    Record<string, string | null>
> {
    /**
     * A signal that holds the initial value of the component.
     * It is used to display the initial value in the component.
     */
    $initialValue = signal<DotKeyValue[]>([]);
    /**
     * A signal that holds the error state of the component.
     * It is used to display the error state in the component.
     */
    $hasError = input.required<boolean>({ alias: 'hasError' });

    constructor() {
        super();
        this.handleChangeValue(this.$value);
    }

    /**
     * Updates the field.
     * It is used to update the field.
     */
    updateField(value: DotKeyValue[]): void {
        // The accumulator's type has to be given explicitly: from the `{}` seed alone TS infers
        // `{}`, which has no index signature to write to.
        const keyValue = value.reduce<Record<string, string>>((acc, item) => {
            acc[item.key] = item.value;

            return acc;
        }, {});

        this.onChange(keyValue);
        this.onTouched();
    }

    /**
     * Parses the data to a DotKeyValue array.
     * It is used to parse the data to a DotKeyValue array.
     */
    private parseToDotKeyValue(data: Record<string, string | null> | null): DotKeyValue[] {
        if (!data || typeof data !== 'object' || Array.isArray(data)) {
            return [];
        }

        return Object.keys(data).map((key: string) => ({
            key,
            value: data[key] === null ? 'null' : (data[key] ?? '')
        }));
    }

    /**
     * Handles the change value of the component.
     * It is used to update the initial value of the component.
     */
    // `| null` because this is called with the accessor's `$value`, which is null until the form
    // writes one — `parseToDotKeyValue` returns `[]` for it.
    readonly handleChangeValue = signalMethod<Record<string, string | null> | null>((value) => {
        const initialValue = this.parseToDotKeyValue(value);
        this.$initialValue.set(initialValue);
    });
}
