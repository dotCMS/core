import { ChangeDetectionStrategy, Component, Input, OnInit, inject } from '@angular/core';
import { AbstractControl, ControlContainer, ReactiveFormsModule } from '@angular/forms';

import { DropdownModule } from 'primeng/dropdown';

import { DotCMSContentTypeField } from '@dotcms/dotcms-models';

import { DotEditContentFieldSingleSelectableDataTypes } from '../../models/dot-edit-content-field.type';
import { getSingleSelectableFieldOptions } from '../../utils/functions.util';

@Component({
    selector: 'dot-edit-content-select-field',
    standalone: true,
    imports: [DropdownModule, ReactiveFormsModule],
    templateUrl: './dot-edit-content-select-field.component.html',
    styleUrls: ['./dot-edit-content-select-field.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    viewProviders: [
        {
            provide: ControlContainer,
            useFactory: () => inject(ControlContainer, { skipSelf: true })
        }
    ]
})
export class DotEditContentSelectFieldComponent implements OnInit {
    @Input() field!: DotCMSContentTypeField;
    private readonly controlContainer = inject(ControlContainer);

    options = [];

    ngOnInit() {
        this.options = getSingleSelectableFieldOptions(
            this.field?.values || '',
            this.field.dataType
        );

        if (this.formControl.value === null) {
            this.formControl.setValue(this.options[0]?.value);
        }
    }

    /**
     * Returns the form control for the select field.
     * @returns {AbstractControl} The form control for the select field.
     */
    get formControl() {
        return this.controlContainer.control.get(
            this.field.variable
        ) as AbstractControl<DotEditContentFieldSingleSelectableDataTypes>;
    }
}
