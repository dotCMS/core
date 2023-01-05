import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { DotEditLayoutDesignerModule } from '@components/dot-edit-layout-designer/dot-edit-layout-designer.module';
import { DotPortletBoxModule } from '@components/dot-portlet-base/components/dot-portlet-box/dot-portlet-box.module';
import { IFrameModule } from '@components/_common/iframe';
import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';
import { TabViewModule } from 'primeng/tabview';
import { DotTemplateAdvancedModule } from '../dot-template-advanced/dot-template-advanced.module';
import { DotTemplateBuilderComponent } from './dot-template-builder.component';

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
