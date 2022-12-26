import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TabViewModule } from 'primeng/tabview';

import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';
import { DotPortletBaseModule } from '@components/dot-portlet-base/dot-portlet-base.module';
import { DotCategoriesCreateEditRoutingModule } from './dot-categories-create-edit-routing.module';
import { DotCategoriesCreateEditComponent } from './dot-categories-create-edit.component';
import { DotCategoriesPermissionsModule } from '../dot-categories-permissions/dot-categories-permissions.module';
import { DotCategoriesPropertiesModule } from './dot-categories-properties/dot-categories-properties.module';

@NgModule({
    declarations: [DotCategoriesCreateEditComponent],
    exports: [DotCategoriesCreateEditComponent],
    imports: [
        CommonModule,
        DotMessagePipeModule,
        TabViewModule,
        DotCategoriesCreateEditRoutingModule,
        DotPortletBaseModule,
        DotCategoriesPermissionsModule,
        DotCategoriesPropertiesModule
    ]
})
export class DotCategoriesCreateEditModule {}
