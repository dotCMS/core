import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { ControlContainer, FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';

import { MultiSelectModule } from 'primeng/multiselect';

import { ContentTypeMultiSelectField } from '@dotcms/dotcms-models';

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
    templateUrl: './dot-edit-content-multi-select-field.html'
})
export class DotEditContentMultiSelectFieldComponent {
    readonly #form = inject(ControlContainer).control as FormGroup;
    formControl = new FormControl<string[]>([]);

    $field = input.required<ContentTypeMultiSelectField>({ alias: 'field' });
    $options = computed(() => {
        const field = this.$field();

        return getSingleSelectableFieldOptions(field.values || '', field.dataType);
    });
}
