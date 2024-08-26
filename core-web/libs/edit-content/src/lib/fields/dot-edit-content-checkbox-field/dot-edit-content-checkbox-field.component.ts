import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { ControlContainer, FormControl, FormsModule, ReactiveFormsModule } from '@angular/forms';

import { CheckboxModule } from 'primeng/checkbox';

import { DotCMSContentTypeField } from '@dotcms/dotcms-models';

import { getSingleSelectableFieldOptions } from '../../utils/functions.util';
@Component({
    selector: 'dot-edit-content-checkbox-field',
    standalone: true,
    imports: [CheckboxModule, ReactiveFormsModule, FormsModule],
    templateUrl: './dot-edit-content-checkbox-field.component.html',
    styleUrls: ['./dot-edit-content-checkbox-field.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    viewProviders: [
        {
            provide: ControlContainer,
            useFactory: () => inject(ControlContainer, { skipSelf: true })
        }
    ]
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
