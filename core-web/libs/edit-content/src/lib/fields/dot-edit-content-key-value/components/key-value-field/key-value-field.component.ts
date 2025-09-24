import { signalMethod } from '@ngrx/signals';

import { Component, ChangeDetectionStrategy, signal, input } from '@angular/core';

import { DotKeyValue, DotKeyValueComponent } from '@dotcms/ui';

import { BaseControlValueAccesor } from '../../../shared/base-control-value-accesor';

@Component({
    selector: 'dot-key-value-field',
    imports: [DotKeyValueComponent],
    templateUrl: './key-value-field.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotKeyValueFieldComponent extends BaseControlValueAccesor<Record<string, string>> {
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
        const keyValue = value.reduce((acc, item) => {
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
    private parseToDotKeyValue(data: Record<string, string>): DotKeyValue[] {
        if (!data) {
            return [];
        }

        return Object.keys(data).map((key: string) => ({
            key,
            value: data[key]
        }));
    }

    /**
     * Handles the change value of the component.
     * It is used to update the initial value of the component.
     */
    readonly handleChangeValue = signalMethod<Record<string, string>>((value) => {
        const initialValue = this.parseToDotKeyValue(value);
        this.$initialValue.set(initialValue);
    });
}
