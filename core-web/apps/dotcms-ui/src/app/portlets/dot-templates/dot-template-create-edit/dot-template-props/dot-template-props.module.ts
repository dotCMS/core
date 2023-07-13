import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { InputTextModule } from 'primeng/inputtext';
import { InputTextareaModule } from 'primeng/inputtextarea';

import { DotFieldValidationMessageModule } from '@components/_common/dot-field-validation-message/dot-file-validation-message.module';
import { DotFormDialogModule } from '@components/dot-form-dialog/dot-form-dialog.module';
import { DotThemeSelectorDropdownModule } from '@components/dot-theme-selector-dropdown/dot-theme-selector-dropdown.module';
import { DotFieldRequiredDirective, DotMessagePipe } from '@dotcms/ui';

import { DotTemplatePropsComponent } from './dot-template-props.component';
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
        DotMessagePipe,
        DotTemplateThumbnailFieldModule,
        DotThemeSelectorDropdownModule,
        DotFieldRequiredDirective
    ]
})
export class DotTemplatePropsModule {}
