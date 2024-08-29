import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { ControlContainer, FormControl, FormsModule, ReactiveFormsModule } from '@angular/forms';

import { CheckboxModule } from 'primeng/checkbox';

import { DotCMSContentTypeField } from '@dotcms/dotcms-models';

import { getSingleSelectableFieldOptions } from '../../utils/functions.util';
@Component({
    selector: 'dot-edit-content-checkbox-field',
    standalone: true,
    imports: [CheckboxModule, ReactiveFormsModule, FormsModule],
    changeDetection: ChangeDetectionStrategy.OnPush,
    viewProviders: [
        {
            provide: ControlContainer,
            useFactory: () => inject(ControlContainer, { skipSelf: true })
        }
    ],
    template: `
        @for (option of $options(); track $index) {
            <p-checkbox
                [name]="$field().variable"
                [formControl]="formControl"
                [value]="option.value"
                [label]="option.label"
                [inputId]="option.value.toString() + $index" />
        }
    `,
    styleUrls: ['./dot-edit-content-checkbox-field.component.scss']
})
export class DotEditContentCheckboxFieldComponent {
    private readonly controlContainer = inject(ControlContainer);

    $field = input.required<DotCMSContentTypeField>({ alias: 'field' });
    $options = computed(() => {
        const field = this.$field();

        return getSingleSelectableFieldOptions(field.values || '', field.dataType);
    });

    /**
     * Returns the form control for the select field.
     * @returns {AbstractControl} The form control for the select field.
     */
    get formControl() {
        const field = this.$field();

        return this.controlContainer.control.get(field.variable) as FormControl;
    }
}
