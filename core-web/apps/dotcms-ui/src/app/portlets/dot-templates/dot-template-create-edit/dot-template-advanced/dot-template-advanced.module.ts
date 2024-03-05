import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';

import { DotTemplateAdvancedComponent } from './dot-template-advanced.component';

import { DotGlobalMessageModule } from '../../../../view/components/_common/dot-global-message/dot-global-message.module';
import { DotTextareaContentModule } from '../../../../view/components/_common/dot-textarea-content/dot-textarea-content.module';
import { DotContainerSelectorModule } from '../../../../view/components/dot-container-selector/dot-container-selector.module';
import { DotPortletBaseModule } from '../../../../view/components/dot-portlet-base/dot-portlet-base.module';

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
