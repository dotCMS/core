import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { DialogService, DynamicDialogModule } from 'primeng/dynamicdialog';

import { DotApiLinkComponent, DotFieldRequiredDirective, DotMessagePipe } from '@dotcms/ui';

import { DotTemplateBuilderModule } from './dot-template-builder/dot-template-builder.module';
import { DotTemplateCreateEditRoutingModule } from './dot-template-create-edit-routing.module';
import { DotTemplateCreateEditComponent } from './dot-template-create-edit.component';
import { DotTemplatePropsModule } from './dot-template-props/dot-template-props.module';

import { DotPortletBaseModule } from '../../../view/components/dot-portlet-base/dot-portlet-base.module';

@NgModule({
    imports: [
        ButtonModule,
        CommonModule,
        DotApiLinkComponent,
        DotPortletBaseModule,
        DotTemplateCreateEditRoutingModule,
        DotTemplatePropsModule,
        DynamicDialogModule,
        DotMessagePipe,
        DotFieldRequiredDirective,
        DotTemplateBuilderModule
    ],
    declarations: [DotTemplateCreateEditComponent],
    providers: [DialogService]
})
export class DotTemplateCreateEditModule {}
