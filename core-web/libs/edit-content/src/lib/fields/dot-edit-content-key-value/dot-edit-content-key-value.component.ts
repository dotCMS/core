import {
    Component,
    ChangeDetectionStrategy,
    signal,
    input,
    forwardRef,
    inject
} from '@angular/core';
import { ControlContainer, NG_VALUE_ACCESSOR } from '@angular/forms';

import { DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { DotKeyValue, DotKeyValueComponent, DotMessagePipe } from '@dotcms/ui';

import { DotCardFieldContentComponent } from '../dot-card-field/components/dot-card-field-content.component';
import { DotCardFieldFooterComponent } from '../dot-card-field/components/dot-card-field-footer.component';
import { DotCardFieldComponent } from '../dot-card-field/dot-card-field.component';
import { BaseFieldComponent } from '../shared/base-field.component';

@Component({
    selector: 'dot-edit-content-key-value',
    imports: [
        DotKeyValueComponent,
        DotCardFieldComponent,
        DotCardFieldContentComponent,
        DotCardFieldFooterComponent,
        DotMessagePipe
    ],
    templateUrl: './dot-edit-content-key-value.component.html',
    styleUrl: './dot-edit-content-key-value.component.css',
    changeDetection: ChangeDetectionStrategy.OnPush,
    viewProviders: [
        {
            provide: ControlContainer,
            useFactory: () => inject(ControlContainer, { skipSelf: true })
        }
    ],
    providers: [
        {
            multi: true,
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => DotEditContentKeyValueComponent)
        }
    ]
})
export class DotEditContentKeyValueComponent extends BaseFieldComponent {
    $initialValue = signal<DotKeyValue[]>([]);
    $field = input.required<DotCMSContentTypeField>({ alias: 'field' });

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
