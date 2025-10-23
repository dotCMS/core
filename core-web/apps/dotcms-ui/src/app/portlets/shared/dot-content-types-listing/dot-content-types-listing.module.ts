import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import {
    DotContentTypeService,
    DotContentTypesInfoService,
    DotCrudService
} from '@dotcms/data-access';
import { DotAddToBundleComponent } from '@dotcms/ui';

import { DotAddToMenuComponent } from './components/dot-add-to-menu/dot-add-to-menu.component';
import { DotContentTypeCopyDialogModule } from './components/dot-content-type-copy-dialog/dot-content-type-copy-dialog.module';
import { DotContentTypesPortletComponent } from './dot-content-types.component';

import { DotAddToMenuService } from '../../../api/services/add-to-menu/add-to-menu.service';
import { DotMenuService } from '../../../api/services/dot-menu.service';
import { DotBaseTypeSelectorComponent } from '../../../view/components/dot-base-type-selector/dot-base-type-selector.component';
import { DotListingDataTableComponent } from '../../../view/components/dot-listing-data-table/dot-listing-data-table.component';
import { DotNavigationService } from '../../../view/components/dot-navigation/services/dot-navigation.service';
import { DotPortletBaseComponent } from '../../../view/components/dot-portlet-base/dot-portlet-base.component';

@NgModule({
    imports: [
        CommonModule,
        DotListingDataTableComponent,
        DotBaseTypeSelectorComponent,
        DotAddToBundleComponent,
        DotAddToMenuComponent,
        DotContentTypeCopyDialogModule,
        DotPortletBaseComponent
    ],
    declarations: [DotContentTypesPortletComponent],
    exports: [DotContentTypesPortletComponent],
    providers: [
        DotContentTypesInfoService,
        DotCrudService,
        DotContentTypeService,
        DotAddToMenuService,
        DotMenuService,
        DotNavigationService
    ]
})
export class DotContentTypesListingModule {}
