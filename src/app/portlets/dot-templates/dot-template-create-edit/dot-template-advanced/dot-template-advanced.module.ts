import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { DotTemplateAdvancedComponent } from './dot-template-advanced.component';
import { DotTextareaContentModule } from '@components/_common/dot-textarea-content/dot-textarea-content.module';
import { DotContainerSelectorModule } from '@components/dot-container-selector/dot-container-selector.module';
import { DotPortletBaseModule } from '@components/dot-portlet-base/dot-portlet-base.module';
import { ReactiveFormsModule } from '@angular/forms';

@NgModule({
    declarations: [DotTemplateAdvancedComponent],
    exports: [DotTemplateAdvancedComponent],
    imports: [
        CommonModule,
        DotContainerSelectorModule,
        DotTextareaContentModule,
        DotPortletBaseModule,
        ReactiveFormsModule
    ],
    providers: []
})
export class DotTemplateAdvancedModule {}
