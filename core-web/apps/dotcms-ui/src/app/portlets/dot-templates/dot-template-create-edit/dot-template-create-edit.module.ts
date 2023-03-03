import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { DialogService, DynamicDialogModule } from 'primeng/dynamicdialog';

import { DotApiLinkModule } from '@components/dot-api-link/dot-api-link.module';
import { DotPortletBaseModule } from '@components/dot-portlet-base/dot-portlet-base.module';
import { DotFieldRequiredDirective } from '@dotcms/ui';
import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';

import { DotTemplateBuilderModule } from './dot-template-builder/dot-template-builder.module';
import { DotTemplateCreateEditRoutingModule } from './dot-template-create-edit-routing.module';
import { DotTemplateCreateEditComponent } from './dot-template-create-edit.component';
import { DotTemplatePropsModule } from './dot-template-props/dot-template-props.module';

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
        DotMessagePipeModule,
        DotFieldRequiredDirective
    ],
    declarations: [DotTemplateCreateEditComponent],
    providers: [DialogService]
})
export class DotTemplateCreateEditModule {}
