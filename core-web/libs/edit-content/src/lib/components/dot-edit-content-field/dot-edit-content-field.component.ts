import { NgIf, NgSwitch, NgSwitchCase } from '@angular/common';
import { ChangeDetectionStrategy, Component, HostBinding, Input, inject } from '@angular/core';
import { ControlContainer, ReactiveFormsModule } from '@angular/forms';

import { DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { DotFieldRequiredDirective } from '@dotcms/ui';

import { FIELD_TYPES } from './utils';

import { DotEditContentFieldsModule } from '../../fields/dot-edit-content-fields.module';
import { DotEditContentRadioFieldComponent } from '../../fields/dot-edit-content-radio-field/dot-edit-content-radio-field.component';
import { DotEditContentSelectFieldComponent } from '../../fields/dot-edit-content-select-field/dot-edit-content-select-field.component';

@Component({
    selector: 'dot-edit-content-field',
    standalone: true,
    templateUrl: './dot-edit-content-field.component.html',
    styleUrls: ['./dot-edit-content-field.component.scss'],
    viewProviders: [
        {
            provide: ControlContainer,
            useFactory: () => inject(ControlContainer, { skipSelf: true })
        }
    ],
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [
        NgSwitch,
        NgSwitchCase,
        NgIf,
        ReactiveFormsModule,
        DotEditContentFieldsModule,
        DotFieldRequiredDirective,
        DotEditContentSelectFieldComponent,
        DotEditContentRadioFieldComponent
    ]
})
export class DotEditContentFieldComponent {
    @HostBinding('class') class = 'field';
    @Input() field!: DotCMSContentTypeField;
    readonly fieldTypes = FIELD_TYPES;
}
