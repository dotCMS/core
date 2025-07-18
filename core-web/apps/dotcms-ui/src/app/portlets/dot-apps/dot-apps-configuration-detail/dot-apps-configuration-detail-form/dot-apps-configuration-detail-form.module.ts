import { MarkdownModule } from 'ngx-markdown';

import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormGroupDirective, ReactiveFormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { DropdownModule } from 'primeng/dropdown';
import { InputTextModule } from 'primeng/inputtext';
import { InputTextareaModule } from 'primeng/inputtextarea';
import { TooltipModule } from 'primeng/tooltip';

import { DotFieldRequiredDirective, DotIconModule, DotMessagePipe } from '@dotcms/ui';

import { DotAppsConfigurationDetailFormComponent } from './dot-apps-configuration-detail-form.component';

import { DotAppsConfigurationDetailGeneratedStringFieldComponent } from '../dot-apps-configuration-detail-generated-string-field/dot-apps-configuration-detail-generated-string-field.component';

@NgModule({
    imports: [
        ButtonModule,
        CheckboxModule,
        CommonModule,
        DotIconModule,
        DropdownModule,
        InputTextareaModule,
        InputTextModule,
        ReactiveFormsModule,
        TooltipModule,
        MarkdownModule.forChild(),
        DotFieldRequiredDirective,
        DotMessagePipe,
        DotAppsConfigurationDetailGeneratedStringFieldComponent
    ],
    declarations: [DotAppsConfigurationDetailFormComponent],
    exports: [DotAppsConfigurationDetailFormComponent],
    providers: [FormGroupDirective]
})
export class DotAppsConfigurationDetailFormModule {}
