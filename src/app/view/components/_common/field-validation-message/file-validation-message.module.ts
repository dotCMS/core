import { CommonModule } from '@angular/common';
import { FieldValidationMessageComponent } from './field-validation-message';
import { NgModule } from '@angular/core';

@NgModule({
    bootstrap: [],
    declarations: [ FieldValidationMessageComponent ],
    exports: [ FieldValidationMessageComponent ],
    imports: [ CommonModule ],
    providers: []
})
export class FieldValidationMessageModule {}