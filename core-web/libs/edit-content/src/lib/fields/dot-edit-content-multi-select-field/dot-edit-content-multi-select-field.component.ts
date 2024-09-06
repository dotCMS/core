import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { ControlContainer, ReactiveFormsModule } from '@angular/forms';

import { MultiSelectModule } from 'primeng/multiselect';

import { DotCMSContentTypeField } from '@dotcms/dotcms-models';

import { getSingleSelectableFieldOptions } from '../../utils/functions.util';
@Component({
    selector: 'dot-edit-content-multi-select-field',
    standalone: true,
    imports: [MultiSelectModule, ReactiveFormsModule],
    changeDetection: ChangeDetectionStrategy.OnPush,
    viewProviders: [
        {
            provide: ControlContainer,
            useFactory: () => inject(ControlContainer, { skipSelf: true })
        }
    ],
    template: `
        <p-multiSelect
            [options]="$options()"
            [formControlName]="$field().variable"
            optionLabel="label"
            optionValue="value" />
    `
})
export class DotEditContentMultiSelectFieldComponent {
    $field = input.required<DotCMSContentTypeField>({ alias: 'field' });
    $options = computed(() => {
        const field = this.$field();

        return getSingleSelectableFieldOptions(field.values || '', field.dataType);
    });
}
