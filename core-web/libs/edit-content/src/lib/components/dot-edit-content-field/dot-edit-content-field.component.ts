import { NgIf, NgSwitch, NgSwitchCase } from '@angular/common';
import { ChangeDetectionStrategy, Component, HostBinding, Input, inject } from '@angular/core';
import { ControlContainer, ReactiveFormsModule } from '@angular/forms';

import { BlockEditorModule } from '@dotcms/block-editor';
import { DotCMSContentTypeField, DotCMSContentlet } from '@dotcms/dotcms-models';
import { DotFieldRequiredDirective } from '@dotcms/ui';

import { DotEditContentBinaryFieldComponent } from '../../fields/dot-edit-content-binary-field/dot-edit-content-binary-field.component';
import { DotEditContentFieldsModule } from '../../fields/dot-edit-content-fields.module';
import { CALENDAR_FIELD_TYPES } from '../../models/dot-edit-content-field.constant';
import { FIELD_TYPES } from '../../models/dot-edit-content-field.enum';

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
        BlockEditorModule,
        DotEditContentBinaryFieldComponent
    ]
})
export class DotEditContentFieldComponent {
    @HostBinding('class') class = 'field';
    @Input() field!: DotCMSContentTypeField;
    @Input() contentlet!: DotCMSContentlet;
    readonly fieldTypes = FIELD_TYPES;
    readonly calendarTypes = CALENDAR_FIELD_TYPES as string[];
}
