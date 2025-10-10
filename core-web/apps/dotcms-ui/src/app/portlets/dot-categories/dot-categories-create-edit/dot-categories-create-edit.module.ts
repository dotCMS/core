import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { TabViewModule } from 'primeng/tabview';

import { DotMessagePipe } from '@dotcms/ui';

import { DotCategoriesCreateEditRoutingModule } from './dot-categories-create-edit-routing.module';
import { DotCategoriesCreateEditComponent } from './dot-categories-create-edit.component';

import { DotPortletBaseComponent } from '../../../view/components/dot-portlet-base/dot-portlet-base.component';
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
        DotPortletBaseComponent,
        DotCategoriesPermissionsModule
    ]
})
export class DotCategoriesCreateEditModule {}
