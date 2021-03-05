import { CommonModule } from '@angular/common';
import { DotFieldValidationMessageComponent } from './dot-field-validation-message';
import { NgModule } from '@angular/core';
import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';

@NgModule({
    bootstrap: [],
    declarations: [DotFieldValidationMessageComponent],
    exports: [DotFieldValidationMessageComponent],
    imports: [CommonModule, DotMessagePipeModule],
    providers: []
})
export class DotFieldValidationMessageModule {}
