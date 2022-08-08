import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Routes } from '@angular/router';

import { DotTemplateCreateEditComponent } from './dot-template-create-edit.component';
import { ButtonModule } from 'primeng/button';
import { DotApiLinkModule } from '@components/dot-api-link/dot-api-link.module';
import { DotPortletBaseModule } from '@components/dot-portlet-base/dot-portlet-base.module';
import { DotTemplatePropsModule } from './components/dot-template-props/dot-template-props.module';
import { DialogService, DynamicDialogModule } from 'primeng/dynamicdialog';
import { DotTemplateBuilderModule } from './components/dot-template-builder/dot-template-builder.module';
import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';

const routes: Routes = [
    {
        path: '',
        component: DotTemplateCreateEditComponent
    }
];

@NgModule({
    declarations: [DotTemplateCreateEditComponent],
    imports: [
        ButtonModule,
        CommonModule,
        DotApiLinkModule,
        DotPortletBaseModule,
        DotTemplatePropsModule,
        DynamicDialogModule,
        DotTemplateBuilderModule,
        DotMessagePipeModule,
        RouterModule.forChild(routes)
    ],
    providers: [DialogService]
})
export class DotcmsUiTemplatesFeatureCreateEditModule {}
