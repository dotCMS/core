import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { DialogService, DynamicDialogModule } from 'primeng/dynamicdialog';
import { ButtonModule } from 'primeng/button';

import { DotTemplateCreateEditComponent } from './dot-template-create-edit.component';
import { DotTemplateCreateEditRoutingModule } from './dot-template-create-edit-routing.module';
import { DotPortletBaseModule } from '@components/dot-portlet-base/dot-portlet-base.module';
import { DotApiLinkModule } from '@components/dot-api-link/dot-api-link.module';
import { DotTemplatePropsModule } from './dot-template-props/dot-template-props.module';
import { DotTemplateBuilderModule } from './dot-template-builder/dot-template-builder.module';
import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';

@NgModule({
    imports: [
        ButtonModule,
        CommonModule,
        DotApiLinkModule,
        DotPortletBaseModule,
        DotTemplateCreateEditRoutingModule,
        DotTemplatePropsModule,
        DynamicDialogModule,
        DotTemplateBuilderModule,
        DotMessagePipeModule
    ],
    declarations: [DotTemplateCreateEditComponent],
    providers: [DialogService]
})
export class DotTemplateCreateEditModule {}
