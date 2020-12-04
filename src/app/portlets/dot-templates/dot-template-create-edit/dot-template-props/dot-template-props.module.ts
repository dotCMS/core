import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { InputTextModule } from 'primeng/inputtext';
import { InputTextareaModule } from 'primeng/inputtextarea';

import { DotTemplatePropsComponent } from './dot-template-props.component';
import { DotFormDialogModule } from '@components/dot-form-dialog/dot-form-dialog.module';
import { DotFieldValidationMessageModule } from '@components/_common/dot-field-validation-message/dot-file-validation-message.module';
import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';
import { DotTemplateThumbnailFieldModule } from './dot-template-thumbnail-field/dot-template-thumbnail-field.module';

@NgModule({
    declarations: [DotTemplatePropsComponent],
    imports: [
        CommonModule,
        DotFieldValidationMessageModule,
        DotFormDialogModule,
        FormsModule,
        InputTextModule,
        InputTextareaModule,
        ReactiveFormsModule,
        DotMessagePipeModule,
        DotTemplateThumbnailFieldModule
    ]
})
export class DotTemplatePropsModule {}
