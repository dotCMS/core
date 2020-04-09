import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CheckboxModule, InputTextareaModule, InputTextModule } from 'primeng/primeng';

import { DotAppsConfigurationDetailFormComponent } from './dot-apps-configuration-detail-form.component';
import { ReactiveFormsModule } from '@angular/forms';

@NgModule({
    imports: [
        CheckboxModule,
        CommonModule,
        InputTextareaModule,
        InputTextModule,
        ReactiveFormsModule
    ],
    declarations: [DotAppsConfigurationDetailFormComponent],
    exports: [DotAppsConfigurationDetailFormComponent],
    providers: []
})
export class DotAppsConfigurationDetailFormModule {}
