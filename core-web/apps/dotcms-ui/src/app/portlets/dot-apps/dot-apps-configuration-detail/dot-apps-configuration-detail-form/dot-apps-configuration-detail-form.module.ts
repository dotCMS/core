import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { ReactiveFormsModule } from '@angular/forms';
import { DotIconModule } from '@dotcms/ui';
import { DotAppsConfigurationDetailFormComponent } from './dot-apps-configuration-detail-form.component';

import { MarkdownModule } from 'ngx-markdown';
import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { DropdownModule } from 'primeng/dropdown';
import { InputTextModule } from 'primeng/inputtext';
import { InputTextareaModule } from 'primeng/inputtextarea';
import { TooltipModule } from 'primeng/tooltip';

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
        MarkdownModule.forChild()
    ],
    declarations: [DotAppsConfigurationDetailFormComponent],
    exports: [DotAppsConfigurationDetailFormComponent],
    providers: []
})
export class DotAppsConfigurationDetailFormModule {}
