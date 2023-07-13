import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { TabViewModule } from 'primeng/tabview';

import { DotGlobalMessageModule } from '@components/_common/dot-global-message/dot-global-message.module';
import { IFrameModule } from '@components/_common/iframe';
import { DotEditLayoutDesignerModule } from '@components/dot-edit-layout-designer/dot-edit-layout-designer.module';
import { DotPortletBoxModule } from '@components/dot-portlet-base/components/dot-portlet-box/dot-portlet-box.module';
import { DotShowHideFeatureDirective } from '@dotcms/app/shared/directives/dot-show-hide-feature/dot-show-hide-feature.directive';
import { TemplateBuilderModule } from '@dotcms/template-builder';
import { DotMessagePipe } from '@dotcms/ui';

import { DotTemplateBuilderComponent } from './dot-template-builder.component';

import { DotTemplateAdvancedModule } from '../dot-template-advanced/dot-template-advanced.module';

@NgModule({
    imports: [
        CommonModule,
        DotEditLayoutDesignerModule,
        DotMessagePipe,
        DotTemplateAdvancedModule,
        TabViewModule,
        IFrameModule,
        DotPortletBoxModule,
        DotShowHideFeatureDirective,
        TemplateBuilderModule,
        ButtonModule,
        DotGlobalMessageModule
    ],
    declarations: [DotTemplateBuilderComponent],
    exports: [DotTemplateBuilderComponent]
})
export class DotTemplateBuilderModule {}
