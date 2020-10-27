import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { DotAppsConfigurationDetailFormComponent } from './dot-apps-configuration-detail-form.component';
import { ReactiveFormsModule } from '@angular/forms';
import { DotIconModule } from '@components/_common/dot-icon/dot-icon.module';

import { CheckboxModule } from 'primeng/checkbox';
import { DropdownModule } from 'primeng/dropdown';
import { InputTextareaModule } from 'primeng/inputtextarea';
import { InputTextModule } from 'primeng/inputtext';
import { TooltipModule } from 'primeng/tooltip';
import { MarkdownModule } from 'ngx-markdown';
import { ButtonModule } from 'primeng/button';

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
