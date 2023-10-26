import { NgIf, NgSwitch, NgSwitchCase } from '@angular/common';
import { ChangeDetectionStrategy, Component, HostBinding, Input, inject } from '@angular/core';
import { ControlContainer, ReactiveFormsModule } from '@angular/forms';

import { DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { DotFieldRequiredDirective } from '@dotcms/ui';

import { DotEditContentFieldsModule } from '../../fields/dot-edit-content-fields.module';
import { FIELD_TYPES } from '../../models/dot-edit-content-field.enum';
import { CALENDAR_FIELD_TYPES } from '../../utils/mocks';

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
    @HostBinding('class') class = 'field';
    @Input() field!: DotCMSContentTypeField;
    readonly fieldTypes = FIELD_TYPES;
    readonly calendarTypes = CALENDAR_FIELD_TYPES as string[];
}
