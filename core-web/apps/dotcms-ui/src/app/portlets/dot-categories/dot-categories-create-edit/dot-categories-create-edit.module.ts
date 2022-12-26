import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';

import { DotPortletBaseModule } from '@components/dot-portlet-base/dot-portlet-base.module';
import { DotCategoriesCreateEditRoutingModule } from './dot-categories-create-edit-routing.module';
import { DotCategoriesCreateEditComponent } from './dot-categories-create-edit.component';
import { DotCategoriesListModule } from '../dot-categories-list/dot-categories-list.module';
import { DotCategoriesPermissionsModule } from '../dot-categories-permissions/dot-categories-permissions.module';
import { TabViewModule } from 'primeng/tabview';

@NgModule({
    declarations: [DotCategoriesCreateEditComponent],
    exports: [DotCategoriesCreateEditComponent],
    imports: [
        CommonModule,
        DotMessagePipeModule,
        TabViewModule,
        DotCategoriesListModule,
        DotCategoriesCreateEditRoutingModule,
        DotPortletBaseModule,
        DotCategoriesPermissionsModule
    ]
})
export class DotCategoriesCreateEditModule {}
