import { NgModule } from '@angular/core';

import { DotEditContentBinaryFieldComponent } from './dot-edit-content-binary-field/dot-edit-content-binary-field.component';
import { DotEditContentCalendarFieldComponent } from './dot-edit-content-calendar-field/dot-edit-content-calendar-field.component';
import { DotEditContentCategoryFieldComponent } from './dot-edit-content-category-field/dot-edit-content-category-field.component';
import { DotEditContentCheckboxFieldComponent } from './dot-edit-content-checkbox-field/dot-edit-content-checkbox-field.component';
import { DotEditContentCustomFieldComponent } from './dot-edit-content-custom-field/dot-edit-content-custom-field.component';
import { DotEditContentHostFolderFieldComponent } from './dot-edit-content-host-folder-field/dot-edit-content-host-folder-field.component';
import { DotEditContentJsonFieldComponent } from './dot-edit-content-json-field/dot-edit-content-json-field.component';
import { DotEditContentMultiSelectFieldComponent } from './dot-edit-content-multi-select-field/dot-edit-content-multi-select-field.component';
import { DotEditContentRadioFieldComponent } from './dot-edit-content-radio-field/dot-edit-content-radio-field.component';
import { DotEditContentSelectFieldComponent } from './dot-edit-content-select-field/dot-edit-content-select-field.component';
import { DotEditContentTagFieldComponent } from './dot-edit-content-tag-field/dot-edit-content-tag-field.component';
import { DotEditContentTextAreaComponent } from './dot-edit-content-text-area/dot-edit-content-text-area.component';
import { DotEditContentTextFieldComponent } from './dot-edit-content-text-field/dot-edit-content-text-field.component';
import { DotEditContentWYSIWYGFieldComponent } from './dot-edit-content-wysiwyg-field/dot-edit-content-wysiwyg-field.component';

@NgModule({
    declarations: [],
    imports: [
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
        DotEditContentCategoryFieldComponent
    ],
    exports: [
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
        DotEditContentCategoryFieldComponent
    ]
})
export class DotEditContentFieldsModule {}
