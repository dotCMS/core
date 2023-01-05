import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';

import { DotPortletBaseModule } from '@components/dot-portlet-base/dot-portlet-base.module';
import { TabViewModule } from 'primeng/tabview';
import { DotCategoriesListModule } from '../dot-categories-list/dot-categories-list.module';
import { DotCategoriesPermissionsModule } from '../dot-categories-permissions/dot-categories-permissions.module';
import { DotCategoriesCreateEditRoutingModule } from './dot-categories-create-edit-routing.module';
import { DotCategoriesCreateEditComponent } from './dot-categories-create-edit.component';

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
