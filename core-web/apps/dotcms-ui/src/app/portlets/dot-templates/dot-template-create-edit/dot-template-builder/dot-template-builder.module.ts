import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { TabViewModule } from 'primeng/tabview';

import { IFrameModule } from '@components/_common/iframe';
import { DotEditLayoutDesignerModule } from '@components/dot-edit-layout-designer/dot-edit-layout-designer.module';
import { DotPortletBoxModule } from '@components/dot-portlet-base/components/dot-portlet-box/dot-portlet-box.module';
import { DotShowHideFeatureDirective } from '@dotcms/app/shared/directives/dot-show-hide-feature/dot-show-hide-feature.directive';
import { TemplateBuilderModule } from '@dotcms/template-builder';
import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';

import { DotTemplateBuilderComponent } from './dot-template-builder.component';

import { DotTemplateAdvancedModule } from '../dot-template-advanced/dot-template-advanced.module';

@NgModule({
    imports: [
        CommonModule,
        DotEditLayoutDesignerModule,
        DotMessagePipeModule,
        DotTemplateAdvancedModule,
        TabViewModule,
        IFrameModule,
        DotPortletBoxModule,
        DotShowHideFeatureDirective,
        TemplateBuilderModule
    ],
    declarations: [DotTemplateBuilderComponent],
    exports: [DotTemplateBuilderComponent]
})
export class DotTemplateBuilderModule {}
