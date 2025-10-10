import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { TabViewModule } from 'primeng/tabview';

import { TemplateBuilderModule } from '@dotcms/template-builder';
import { DotMessagePipe } from '@dotcms/ui';

import { DotTemplateBuilderComponent } from './dot-template-builder.component';

import { DotGlobalMessageModule } from '../../../../view/components/_common/dot-global-message/dot-global-message.module';
import { IframeComponent } from '../../../../view/components/_common/iframe/iframe-component/iframe.component';
import { DotPortletBoxComponent } from '../../../../view/components/dot-portlet-base/components/dot-portlet-box/dot-portlet-box.component';
import { DotTemplateAdvancedModule } from '../dot-template-advanced/dot-template-advanced.module';

@NgModule({
    imports: [
        CommonModule,
        DotMessagePipe,
        DotTemplateAdvancedModule,
        TabViewModule,
        IframeComponent,
        DotPortletBoxComponent,
        TemplateBuilderModule,
        ButtonModule,
        DotGlobalMessageModule
    ],
    declarations: [DotTemplateBuilderComponent],
    exports: [DotTemplateBuilderComponent]
})
export class DotTemplateBuilderModule {}
