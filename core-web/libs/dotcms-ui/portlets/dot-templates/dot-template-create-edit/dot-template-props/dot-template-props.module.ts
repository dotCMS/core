import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { InputTextModule } from 'primeng/inputtext';
import { InputTextareaModule } from 'primeng/inputtextarea';

import { DotTemplatePropsComponent } from './dot-template-props.component';
import { DotTemplateThumbnailFieldModule } from './dot-template-thumbnail-field/dot-template-thumbnail-field.module';
import { DotFormDialogModule } from '../../../../../../apps/dotcms-ui/src/app/view/components/dot-form-dialog/dot-form-dialog.module';
import { DotThemeSelectorDropdownModule } from '../../../../../../apps/dotcms-ui/src/app/view/components/dot-theme-selector-dropdown/dot-theme-selector-dropdown.module';
import { DotFieldValidationMessageModule } from '../../../../../../apps/dotcms-ui/src/app/view/components/_common/dot-field-validation-message/dot-file-validation-message.module';
import { DotMessagePipeModule } from '../../../../../../apps/dotcms-ui/src/app/view/pipes/dot-message/dot-message-pipe.module';

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
        DotTemplateThumbnailFieldModule,
        DotThemeSelectorDropdownModule
    ]
})
export class DotTemplatePropsModule {}
