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

    options = [];

    ngOnInit() {
        this.options = mapOptions(this.field.values || '', this.field.dataType);
    }
}
