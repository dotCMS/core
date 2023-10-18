import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, Input, OnInit, inject } from '@angular/core';
import { ControlContainer, ReactiveFormsModule } from '@angular/forms';

import { RadioButtonModule } from 'primeng/radiobutton';

import { DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { DotFieldRequiredDirective } from '@dotcms/ui';

import { mapOptions } from '../../utils/functions.util';

@Component({
    selector: 'dot-edit-content-radio-field',
    standalone: true,
    imports: [CommonModule, RadioButtonModule, ReactiveFormsModule, DotFieldRequiredDirective],
    templateUrl: './dot-edit-content-radio-field.component.html',
    styleUrls: ['./dot-edit-content-radio-field.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    viewProviders: [
        {
            provide: ControlContainer,
            useFactory: () => inject(ControlContainer, { skipSelf: true })
        }
    ]
})
export class DotEditContentRadioFieldComponent implements OnInit {
    @Input() field!: DotCMSContentTypeField;
    private readonly controlContainer = inject(ControlContainer);

    options = [];

    ngOnInit() {
        this.options = mapOptions(this.field.values || '', this.field.dataType);
        if (!this.formControl.value) {
            this.formControl.setValue(this.options[0]?.value);
        }
    }

    /**
     * Returns the form control for the radio field.
     * @returns {AbstractControl} The form control for the radio field.
     */
    get formControl() {
        return this.controlContainer.control.get(this.field.variable);
    }
}
