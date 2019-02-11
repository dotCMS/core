import { CommonModule } from '@angular/common';
import { DotFieldValidationMessageComponent } from './dot-field-validation-message';
import { NgModule } from '@angular/core';

@NgModule({
    bootstrap: [],
    declarations: [DotFieldValidationMessageComponent],
    exports: [DotFieldValidationMessageComponent],
    imports: [CommonModule],
    providers: []
})
export class DotFieldValidationMessageModule {}
