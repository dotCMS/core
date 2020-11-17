import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotTemplateCreateEditComponent } from './dot-template-create-edit.component';
import { DotTemplateCreateEditRoutingModule } from './dot-template-create-edit-routing.module';
import { TabViewModule } from 'primeng/tabview';
import { DotPortletBaseModule } from '@components/dot-portlet-base/dot-portlet-base.module';
import { DialogService, DynamicDialogModule } from 'primeng/dynamicdialog';
import { DotApiLinkModule } from '@components/dot-api-link/dot-api-link.module';
import { ButtonModule } from 'primeng/button';
import { DotEditLayoutDesignerModule } from '@components/dot-edit-layout-designer/dot-edit-layout-designer.module';
import { DotTemplatePropsModule } from './dot-template-props/dot-template-props.module';

@NgModule({
    imports: [
        ButtonModule,
        CommonModule,
        DotApiLinkModule,
        DotEditLayoutDesignerModule,
        DotPortletBaseModule,
        DotTemplateCreateEditRoutingModule,
        DotTemplatePropsModule,
        DynamicDialogModule,
        TabViewModule
    ],
    declarations: [DotTemplateCreateEditComponent],
    providers: [DialogService]
})
export class DotTemplateCreateEditModule {}
