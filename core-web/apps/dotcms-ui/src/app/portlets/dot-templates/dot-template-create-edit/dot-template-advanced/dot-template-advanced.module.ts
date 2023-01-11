import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';

import { DotGlobalMessageModule } from '@components/_common/dot-global-message/dot-global-message.module';
import { DotTextareaContentModule } from '@components/_common/dot-textarea-content/dot-textarea-content.module';
import { DotContainerSelectorModule } from '@components/dot-container-selector/dot-container-selector.module';
import { DotPortletBaseModule } from '@components/dot-portlet-base/dot-portlet-base.module';

import { DotTemplateAdvancedComponent } from './dot-template-advanced.component';

@NgModule({
    declarations: [DotTemplateAdvancedComponent],
    exports: [DotTemplateAdvancedComponent],
    imports: [
        CommonModule,
        DotContainerSelectorModule,
        DotTextareaContentModule,
        DotPortletBaseModule,
        ReactiveFormsModule,
        DotGlobalMessageModule
    ],
    providers: []
})
export class DotTemplateAdvancedModule {}
