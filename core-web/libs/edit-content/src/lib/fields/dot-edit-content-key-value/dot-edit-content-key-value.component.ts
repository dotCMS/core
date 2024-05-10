import { Component, forwardRef } from '@angular/core';
import { NG_VALUE_ACCESSOR } from '@angular/forms';

import { DotKeyValue, DotKeyValueComponent } from '@dotcms/ui';

@Component({
    selector: 'dot-edit-content-key-value',
    standalone: true,
    imports: [DotKeyValueComponent],
    templateUrl: './dot-edit-content-key-value.component.html',
    styleUrl: './dot-edit-content-key-value.component.css',
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => DotEditContentKeyValueComponent),
            multi: true
        }
    ]
})
export class DotEditContentKeyValueComponent {
    protected initialValue: DotKeyValue[] = [];
    private onChange: (value: Record<string, string>) => void;

    updateField(value: DotKeyValue[]): void {
        const keyValue = value.reduce((acc, item) => {
            acc[item.key] = item.value;

            return acc;
        }, {});

        this.onChange(keyValue);
        this.onTouched();
    }

    writeValue(value: Record<string, string>): void {
        this.initialValue = this.parseToDotKeyValue(value);
    }

    registerOnChange(fn: (value: Record<string, string>) => void) {
        this.onChange = fn;
    }

    registerOnTouched(fn: () => void) {
        this.onTouched = fn;
    }

    private onTouched: () => void = () => {
        //
    };

    private parseToDotKeyValue(data: Record<string, string>): DotKeyValue[] {
        if (!data) {
            return [];
        }

        return Object.keys(data).map((key: string) => ({
            key,
            value: data[key]
        }));
    }
}
