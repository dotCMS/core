import { NgModule } from '@angular/core';

import { DotEditContentTextAreaComponent } from './dot-edit-content-text-area/dot-edit-content-text-area.component';
import { DotEditContentTextFieldComponent } from './dot-edit-content-text-field/dot-edit-content-text-field.component';

@NgModule({
    declarations: [],
    imports: [DotEditContentTextAreaComponent, DotEditContentTextFieldComponent],
    exports: [DotEditContentTextAreaComponent, DotEditContentTextFieldComponent]
})
export class DotEditContentFieldsModule {}
