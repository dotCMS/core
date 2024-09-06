import { ChangeDetectionStrategy, Component, OnInit, computed, inject, input } from '@angular/core';
import { AbstractControl, ControlContainer, ReactiveFormsModule } from '@angular/forms';

import { DropdownModule } from 'primeng/dropdown';

import { DotCMSContentTypeField } from '@dotcms/dotcms-models';

import { DotEditContentFieldSingleSelectableDataTypes } from '../../models/dot-edit-content-field.type';
import { getSingleSelectableFieldOptions } from '../../utils/functions.util';

@Component({
    selector: 'dot-edit-content-select-field',
    standalone: true,
    imports: [DropdownModule, ReactiveFormsModule],
    changeDetection: ChangeDetectionStrategy.OnPush,
    viewProviders: [
        {
            provide: ControlContainer,
            useFactory: () => inject(ControlContainer, { skipSelf: true })
        }
    ],
    template: `
        <p-dropdown
            [formControlName]="$field().variable"
            [options]="$options()"
            [attr.aria-labelledby]="'field-' + $field().variable"
            optionLabel="label"
            optionValue="value" />
    `
})
export class DotEditContentSelectFieldComponent implements OnInit {
    $field = input.required<DotCMSContentTypeField>({ alias: 'field' });
    private readonly controlContainer = inject(ControlContainer);

    $options = computed(() => {
        const field = this.$field();

        return getSingleSelectableFieldOptions(field?.values || '', field.dataType);
    });

    ngOnInit() {
        const options = this.$options();

        if (this.formControl.value === null && options.length > 0) {
            this.formControl.setValue(options[0]?.value);
        }
    }

    /**
     * Returns the form control for the select field.
     * @returns {AbstractControl} The form control for the select field.
     */
    get formControl() {
        const field = this.$field();

        return this.controlContainer.control.get(
            field.variable
        ) as AbstractControl<DotEditContentFieldSingleSelectableDataTypes>;
    }
}
