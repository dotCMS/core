import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { TabViewModule } from 'primeng/tabview';

import { DotPortletBaseModule } from '@components/dot-portlet-base/dot-portlet-base.module';
import { DotMessagePipe } from '@dotcms/ui';

import { DotCategoriesCreateEditRoutingModule } from './dot-categories-create-edit-routing.module';
import { DotCategoriesCreateEditComponent } from './dot-categories-create-edit.component';

import { DotCategoriesListingModule } from '../dot-categories-list/dot-categories-list.module';
import { DotCategoriesPermissionsModule } from '../dot-categories-permissions/dot-categories-permissions.module';

@NgModule({
    declarations: [DotCategoriesCreateEditComponent],
    exports: [DotCategoriesCreateEditComponent],
    imports: [
        CommonModule,
        DotMessagePipe,
        TabViewModule,
        DotCategoriesListingModule,
        DotCategoriesCreateEditRoutingModule,
        DotPortletBaseModule,
        DotCategoriesPermissionsModule
    ]
})
export class DotCategoriesCreateEditModule {}
