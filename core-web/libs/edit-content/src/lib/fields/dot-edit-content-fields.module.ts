import { NgModule } from '@angular/core';

import { DotEditContentCalendarFieldComponent } from './dot-edit-content-calendar-field/dot-edit-content-calendar-field.component';
import { DotEditContentRadioFieldComponent } from './dot-edit-content-radio-field/dot-edit-content-radio-field.component';
import { DotEditContentSelectFieldComponent } from './dot-edit-content-select-field/dot-edit-content-select-field.component';
import { DotEditContentTextAreaComponent } from './dot-edit-content-text-area/dot-edit-content-text-area.component';
import { DotEditContentTextFieldComponent } from './dot-edit-content-text-field/dot-edit-content-text-field.component';

@NgModule({
    declarations: [],
    imports: [
        DotEditContentTextAreaComponent,
        DotEditContentRadioFieldComponent,
        DotEditContentSelectFieldComponent,
        DotEditContentTextFieldComponent,
        DotEditContentCalendarFieldComponent
    ],
    exports: [
        DotEditContentTextAreaComponent,
        DotEditContentRadioFieldComponent,
        DotEditContentSelectFieldComponent,
        DotEditContentTextFieldComponent,
        DotEditContentCalendarFieldComponent
    ]
})
export class DotEditContentFieldsModule {}
