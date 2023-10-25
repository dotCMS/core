import { NgModule } from '@angular/core';

import { DotEditContentCheckboxFieldComponent } from './dot-edit-content-checkbox-field/dot-edit-content-checkbox-field.component';
import { DotEditContentTextAreaComponent } from './dot-edit-content-text-area/dot-edit-content-text-area.component';
import { DotEditContentTextFieldComponent } from './dot-edit-content-text-field/dot-edit-content-text-field.component';

@NgModule({
    declarations: [],
    imports: [
        DotEditContentTextAreaComponent,
        DotEditContentTextFieldComponent,
        DotEditContentCheckboxFieldComponent
    ],
    exports: [
        DotEditContentTextAreaComponent,
        DotEditContentTextFieldComponent,
        DotEditContentCheckboxFieldComponent
    ]
})
export class DotEditContentFieldsModule {}
