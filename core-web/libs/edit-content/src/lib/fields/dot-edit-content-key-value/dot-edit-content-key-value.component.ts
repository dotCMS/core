import { Component, ChangeDetectionStrategy, signal, input } from '@angular/core';

import { DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { DotKeyValue, DotKeyValueComponent } from '@dotcms/ui';

import { BaseFieldComponent } from '../shared/base-field.component';

@Component({
    selector: 'dot-edit-content-key-value',
    imports: [DotKeyValueComponent],
    templateUrl: './dot-edit-content-key-value.component.html',
    styleUrl: './dot-edit-content-key-value.component.css',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotEditContentKeyValueComponent extends BaseFieldComponent {
    $initialValue = signal<DotKeyValue[]>([]);
    $field = input<DotCMSContentTypeField>(null, { alias: 'field' });

    updateField(value: DotKeyValue[]): void {
        const keyValue = value.reduce((acc, item) => {
            acc[item.key] = item.value;

            return acc;
        }, {});

        this.onChange(keyValue);
        this.onTouched();
    }

    writeValue(value: Record<string, string>): void {
        const initialValue = this.parseToDotKeyValue(value);
        this.$initialValue.set(initialValue);
    }

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
