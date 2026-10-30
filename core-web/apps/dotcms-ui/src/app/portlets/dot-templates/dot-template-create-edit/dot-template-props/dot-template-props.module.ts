import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { InputTextModule } from 'primeng/inputtext';
import { InputTextareaModule } from 'primeng/inputtextarea';

import {
    DotFieldRequiredDirective,
    DotFieldValidationMessageComponent,
    DotFormDialogComponent,
    DotMessagePipe
} from '@dotcms/ui';

import { DotTemplatePropsComponent } from './dot-template-props.component';
import { DotTemplateThumbnailFieldModule } from './dot-template-thumbnail-field/dot-template-thumbnail-field.module';

import { DotThemeSelectorDropdownModule } from '../../../../view/components/dot-theme-selector-dropdown/dot-theme-selector-dropdown.module';

@NgModule({
    declarations: [DotTemplatePropsComponent],
    imports: [
        CommonModule,
        DotFieldValidationMessageComponent,
        DotFormDialogComponent,
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
