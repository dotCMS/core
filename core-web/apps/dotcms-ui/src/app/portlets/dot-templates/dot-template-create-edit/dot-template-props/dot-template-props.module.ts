import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { InputTextModule } from 'primeng/inputtext';
import { TextareaModule } from 'primeng/textarea';

import { DotThemeSelectorDropdownModule } from '@components/dot-theme-selector-dropdown/dot-theme-selector-dropdown.module';
import {
    DotFieldRequiredDirective,
    DotFieldValidationMessageComponent,
    DotFormDialogComponent,
    DotMessagePipe
} from '@dotcms/ui';

import { DotTemplatePropsComponent } from './dot-template-props.component';
import { DotTemplateThumbnailFieldModule } from './dot-template-thumbnail-field/dot-template-thumbnail-field.module';

@NgModule({
    declarations: [DotTemplatePropsComponent],
    imports: [
        CommonModule,
        DotFieldValidationMessageComponent,
        DotFormDialogComponent,
        FormsModule,
        InputTextModule,
        TextareaModule,
        ReactiveFormsModule,
        DotMessagePipe,
        DotTemplateThumbnailFieldModule,
        DotThemeSelectorDropdownModule,
        DotFieldRequiredDirective
    ]
})
export class DotTemplatePropsModule {}
