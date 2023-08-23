import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { DotMessagePipe } from '@dotcms/ui';

import { DotFieldValidationMessageComponent } from './dot-field-validation-message';

@NgModule({
    bootstrap: [],
    declarations: [DotFieldValidationMessageComponent],
    exports: [DotFieldValidationMessageComponent],
    imports: [CommonModule, DotMessagePipe],
    providers: []
})
export class DotFieldValidationMessageModule {}
