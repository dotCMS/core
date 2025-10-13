import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';

import { DotTemplateAdvancedComponent } from './dot-template-advanced.component';

import { DotGlobalMessageComponent } from '../../../../view/components/_common/dot-global-message/dot-global-message.component';
import { DotTextareaContentComponent } from '../../../../view/components/_common/dot-textarea-content/dot-textarea-content.component';
import { DotContainerSelectorModule } from '../../../../view/components/dot-container-selector/dot-container-selector.module';
import { DotPortletBaseComponent } from '../../../../view/components/dot-portlet-base/dot-portlet-base.component';

@NgModule({
    declarations: [DotTemplateAdvancedComponent],
    exports: [DotTemplateAdvancedComponent],
    imports: [
        CommonModule,
        DotContainerSelectorModule,
        DotTextareaContentComponent,
        DotPortletBaseComponent,
        ReactiveFormsModule,
        DotGlobalMessageComponent
    ],
    providers: []
})
export class DotTemplateAdvancedModule {}
