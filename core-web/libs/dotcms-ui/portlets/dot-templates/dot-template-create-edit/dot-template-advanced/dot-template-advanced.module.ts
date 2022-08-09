import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';

import { DotTemplateAdvancedComponent } from './dot-template-advanced.component';
import { DotContainerSelectorModule } from '../../../../../../apps/dotcms-ui/src/app/view/components/dot-container-selector/dot-container-selector.module';
import { DotTextareaContentModule } from '../../../../../../apps/dotcms-ui/src/app/view/components/_common/dot-textarea-content/dot-textarea-content.module';
import { DotPortletBaseModule } from '../../../../../../apps/dotcms-ui/src/app/view/components/dot-portlet-base/dot-portlet-base.module';
import { DotGlobalMessageModule } from '../../../../../../apps/dotcms-ui/src/app/view/components/_common/dot-global-message/dot-global-message.module';

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
