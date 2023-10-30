import { NgModule } from '@angular/core';

import { DotEditContentCalendarFieldComponent } from './dot-edit-content-calendar-field/dot-edit-content-calendar-field.component';
import { DotEditContentCheckboxFieldComponent } from './dot-edit-content-checkbox-field/dot-edit-content-checkbox-field.component';
import { DotEditContentMultiSelectFieldComponent } from './dot-edit-content-multi-select-field/dot-edit-content-multi-select-field.component';
import { DotEditContentRadioFieldComponent } from './dot-edit-content-radio-field/dot-edit-content-radio-field.component';
import { DotEditContentSelectFieldComponent } from './dot-edit-content-select-field/dot-edit-content-select-field.component';
import { DotEditContentTagFieldComponent } from './dot-edit-content-tag-field/dot-edit-content-tag-field.component';
import { DotEditContentTextAreaComponent } from './dot-edit-content-text-area/dot-edit-content-text-area.component';
import { DotEditContentTextFieldComponent } from './dot-edit-content-text-field/dot-edit-content-text-field.component';

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
        DotEditContentMultiSelectFieldComponent
    ],
    exports: [
        DotEditContentTextAreaComponent,
        DotEditContentRadioFieldComponent,
        DotEditContentSelectFieldComponent,
        DotEditContentTextFieldComponent,
        DotEditContentCalendarFieldComponent,
        DotEditContentTagFieldComponent,
        DotEditContentCheckboxFieldComponent,
        DotEditContentMultiSelectFieldComponent
    ]
})
export class DotEditContentFieldsModule {}
