import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { TabViewModule } from 'primeng/tabview';

import { DotTemplateBuilderComponent } from './dot-template-builder.component';
import { DotTemplateAdvancedModule } from '../dot-template-advanced/dot-template-advanced.module';
import { DotEditLayoutDesignerModule } from '../../../../../../apps/dotcms-ui/src/app/view/components/dot-edit-layout-designer/dot-edit-layout-designer.module';
import { DotMessagePipeModule } from '../../../../../../apps/dotcms-ui/src/app/view/pipes/dot-message/dot-message-pipe.module';
import { DotPortletBoxModule } from '../../../../../../apps/dotcms-ui/src/app/view/components/dot-portlet-base/components/dot-portlet-box/dot-portlet-box.module';
import { IFrameModule } from '../../../../../../apps/dotcms-ui/src/app/view/components/_common/iframe';

@NgModule({
    imports: [
        CommonModule,
        DotEditLayoutDesignerModule,
        DotMessagePipeModule,
        DotTemplateAdvancedModule,
        TabViewModule,
        IFrameModule,
        DotPortletBoxModule
    ],
    declarations: [DotTemplateBuilderComponent],
    exports: [DotTemplateBuilderComponent]
})
export class DotTemplateBuilderModule {}
