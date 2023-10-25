import { NgFor } from '@angular/common';
import { ChangeDetectionStrategy, Component, Input, OnInit, inject } from '@angular/core';
import { ControlContainer, FormControl, FormsModule, ReactiveFormsModule } from '@angular/forms';

import { CheckboxModule } from 'primeng/checkbox';

import { distinctUntilChanged } from 'rxjs/operators';

import { DotCMSContentTypeField } from '@dotcms/dotcms-models';

import { getSingleSelectableFieldOptions } from '../../utils/functions.util';
@Component({
    selector: 'dot-edit-content-checkbox-field',
    standalone: true,
    imports: [NgFor, CheckboxModule, ReactiveFormsModule, FormsModule],
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
export class DotEditContentCheckboxFieldComponent implements OnInit {
    @Input() field!: DotCMSContentTypeField;
    selectedValues: string[] = [];
    private readonly controlContainer = inject(ControlContainer);
    options = [];

    ngOnInit() {
        this.options = getSingleSelectableFieldOptions(
            this.field.values || '',
            this.field.dataType
        );
        this.listenChangeValue();
    }

    listenChangeValue() {
        this.formControl.valueChanges.pipe(distinctUntilChanged()).subscribe((value: string[]) => {
            if (value.length > 0) {
                // this.formControl.setValue(value.join(','), { emitEvent: false });
            }
        });
    }
    onChange(event) {
        const value = event.checked;
        this.formControl.setValue(value.join(','), { emitEvent: false });
    }

    /**
     * Returns the form control for the select field.
     * @returns {AbstractControl} The form control for the select field.
     */
    get formControl() {
        return this.controlContainer.control.get(this.field.variable) as FormControl;
    }
}
