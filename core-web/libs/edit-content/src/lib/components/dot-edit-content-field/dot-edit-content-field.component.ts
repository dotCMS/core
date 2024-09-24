import { ChangeDetectionStrategy, Component, HostBinding, inject, Input } from '@angular/core';
import { ControlContainer, ReactiveFormsModule } from '@angular/forms';

import { BlockEditorModule } from '@dotcms/block-editor';
import { DotCMSContentlet, DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { DotFieldRequiredDirective } from '@dotcms/ui';

import { DotEditContentBinaryFieldComponent } from '../../fields/dot-edit-content-binary-field/dot-edit-content-binary-field.component';
import { DotEditContentFieldsModule } from '../../fields/dot-edit-content-fields.module';
import { DotEditContentFileFieldComponent } from '../../fields/dot-edit-content-file-field/dot-edit-content-file-field.component';
import { DotEditContentKeyValueComponent } from '../../fields/dot-edit-content-key-value/dot-edit-content-key-value.component';
import { DotEditContentWYSIWYGFieldComponent } from '../../fields/dot-edit-content-wysiwyg-field/dot-edit-content-wysiwyg-field.component';
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
        ReactiveFormsModule,
        DotEditContentFieldsModule,
        DotFieldRequiredDirective,
        BlockEditorModule,
        DotEditContentBinaryFieldComponent,
        DotEditContentKeyValueComponent,
        DotEditContentWYSIWYGFieldComponent,
        DotEditContentFileFieldComponent
    ]
})
export class DotEditContentFieldComponent {
    @HostBinding('class') class = 'field';
    @Input() field!: DotCMSContentTypeField;
    @Input() contentlet: DotCMSContentlet | undefined;
    @Input() contentType!: string;

    readonly fieldTypes = FIELD_TYPES;
    readonly calendarTypes = CALENDAR_FIELD_TYPES as string[];
}
