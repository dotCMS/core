import { NgIf, NgSwitch, NgSwitchCase } from '@angular/common';
import { ChangeDetectionStrategy, Component, Input, inject } from '@angular/core';
import { ControlContainer, ReactiveFormsModule } from '@angular/forms';

import { DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { DotFieldRequiredDirective } from '@dotcms/ui';

import { FIELD_TYPES } from './utils';

import { DotEditContentFieldsModule } from '../../fields/dot-edit-content-fields.module';

@Component({
    selector: 'dot-edit-content-field',
    standalone: true,
    imports: [
        NgSwitch,
        NgSwitchCase,
        NgIf,
        ReactiveFormsModule,
        DotEditContentFieldsModule,
        DotFieldRequiredDirective
    ],
    templateUrl: './dot-edit-content-field.component.html',
    styleUrls: ['./dot-edit-content-field.component.scss'],
    viewProviders: [
        {
            provide: ControlContainer,
            useFactory: () => inject(ControlContainer, { skipSelf: true })
        }
    ],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotEditContentFieldComponent {
    @Input() field!: DotCMSContentTypeField;
    readonly fieldTypes = FIELD_TYPES;
}
