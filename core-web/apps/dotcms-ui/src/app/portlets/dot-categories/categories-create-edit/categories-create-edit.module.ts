import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';

import { DotPortletBaseModule } from '@components/dot-portlet-base/dot-portlet-base.module';
import { CategoriesCreateEditRoutingModule } from './categories-create-edit-routing.module';
import { CategoriesCreateEditComponent } from './categories-create-edit.component';
import { CategoriesListModule } from '../categories-list/categories-list.module';
import { TabViewModule } from 'primeng/tabview';

@NgModule({
    declarations: [CategoriesCreateEditComponent],
    exports: [CategoriesCreateEditComponent],
    imports: [
        CommonModule,
        DotMessagePipeModule,
        TabViewModule,
        CategoriesListModule,
        CategoriesCreateEditRoutingModule,
        DotPortletBaseModule
    ]
})
export class CategoriesCreateEditModule {}
