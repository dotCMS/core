import { ChangeDetectionStrategy, Component, HostBinding, inject, Input } from '@angular/core';
import { ControlContainer, ReactiveFormsModule } from '@angular/forms';

import { BlockEditorModule } from '@dotcms/block-editor';
import { DotCMSContentlet, DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { DotFieldRequiredDirective } from '@dotcms/ui';

import { DotEditContentBinaryFieldComponent } from '../../fields/dot-edit-content-binary-field/dot-edit-content-binary-field.component';
import { DotEditContentCalendarFieldComponent } from '../../fields/dot-edit-content-calendar-field/dot-edit-content-calendar-field.component';
import { DotEditContentCategoryFieldComponent } from '../../fields/dot-edit-content-category-field/dot-edit-content-category-field.component';
import { DotEditContentCheckboxFieldComponent } from '../../fields/dot-edit-content-checkbox-field/dot-edit-content-checkbox-field.component';
import { DotEditContentCustomFieldComponent } from '../../fields/dot-edit-content-custom-field/dot-edit-content-custom-field.component';
import { DotEditContentFileFieldComponent } from '../../fields/dot-edit-content-file-field/dot-edit-content-file-field.component';
import { DotEditContentHostFolderFieldComponent } from '../../fields/dot-edit-content-host-folder-field/dot-edit-content-host-folder-field.component';
import { DotEditContentJsonFieldComponent } from '../../fields/dot-edit-content-json-field/dot-edit-content-json-field.component';
import { DotEditContentKeyValueComponent } from '../../fields/dot-edit-content-key-value/dot-edit-content-key-value.component';
import { DotEditContentMultiSelectFieldComponent } from '../../fields/dot-edit-content-multi-select-field/dot-edit-content-multi-select-field.component';
import { DotEditContentRadioFieldComponent } from '../../fields/dot-edit-content-radio-field/dot-edit-content-radio-field.component';
import { DotEditContentSelectFieldComponent } from '../../fields/dot-edit-content-select-field/dot-edit-content-select-field.component';
import { DotEditContentTagFieldComponent } from '../../fields/dot-edit-content-tag-field/dot-edit-content-tag-field.component';
import { DotEditContentTextAreaComponent } from '../../fields/dot-edit-content-text-area/dot-edit-content-text-area.component';
import { DotEditContentTextFieldComponent } from '../../fields/dot-edit-content-text-field/dot-edit-content-text-field.component';
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
        DotEditContentTextAreaComponent,
        DotEditContentRadioFieldComponent,
        DotEditContentSelectFieldComponent,
        DotEditContentTextFieldComponent,
        DotEditContentCalendarFieldComponent,
        DotEditContentTagFieldComponent,
        DotEditContentCheckboxFieldComponent,
        DotEditContentMultiSelectFieldComponent,
        DotEditContentBinaryFieldComponent,
        DotEditContentJsonFieldComponent,
        DotEditContentCustomFieldComponent,
        DotEditContentWYSIWYGFieldComponent,
        DotEditContentHostFolderFieldComponent,
        DotEditContentCategoryFieldComponent,
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
