import { signalMethod } from '@ngrx/signals';

import { Component, ChangeDetectionStrategy, signal, input, forwardRef } from '@angular/core';
import { NG_VALUE_ACCESSOR } from '@angular/forms';

import { DotKeyValue, DotKeyValueComponent } from '@dotcms/ui';

import { parseOrderedKeyValue } from '../../../../utils/key-value-order.util';
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
     * Reports the pairs to the form as JSON **text**, assembled from the array
     * rather than from an object.
     *
     * A plain object cannot carry this order: ECMAScript enumerates integer-like
     * keys first, so a key such as `123` jumps to the front the moment it is
     * assigned, wherever the user dragged it. Building the text directly skips
     * that step, and the backend parses it into a `LinkedHashMap`, so the order
     * reaches storage intact.
     *
     * Each key and value still goes through `JSON.stringify` individually, so
     * quotes and backslashes are escaped exactly as they would be otherwise.
     */
    updateField(value: DotKeyValue[]): void {
        const entries = value.map(
            (item) => `${JSON.stringify(item.key)}:${JSON.stringify(item.value ?? '')}`
        );

        this.onChange(`{${entries.join(',')}}`);
        this.onTouched();
    }

    /**
     * Normalises whatever the form control holds into the editor's pair list.
     *
     * The control carries JSON **text** — `getContentById` puts it there in the
     * response's own order, and {@link updateField} writes it back the same way.
     * Text is parsed with the order-preserving reader so integer-like keys keep
     * their position; a plain object is still accepted for any caller outside that
     * path, at the cost of losing it.
     */
    private parseToDotKeyValue(
        data: string | Record<string, string | null> | DotKeyValue[]
    ): DotKeyValue[] {
        if (typeof data === 'string') {
            return parseOrderedKeyValue(data);
        }

        if (Array.isArray(data)) {
            // Only an array of actual pairs counts. Anything else is malformed data
            // — a bare `['a','b']` must not be read as two half-built entries.
            const pairs = data.filter(
                (entry): entry is DotKeyValue =>
                    !!entry && typeof entry === 'object' && typeof entry.key === 'string'
            );

            return pairs.length === data.length
                ? pairs.map(({ key, value }) => ({ key, value: value ?? 'null' }))
                : [];
        }

        if (!data || typeof data !== 'object') {
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
    readonly handleChangeValue = signalMethod<
        string | Record<string, string | null> | DotKeyValue[]
    >((value) => {
        this.$initialValue.set(this.parseToDotKeyValue(value));
    });
}
