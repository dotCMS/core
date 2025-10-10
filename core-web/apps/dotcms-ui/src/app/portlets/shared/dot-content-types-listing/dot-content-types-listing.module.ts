import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import {
    DotContentTypeService,
    DotContentTypesInfoService,
    DotCrudService
} from '@dotcms/data-access';
import { DotAddToBundleComponent } from '@dotcms/ui';

import { DotAddToMenuModule } from './components/dot-add-to-menu/dot-add-to-menu.module';
import { DotContentTypeCopyDialogModule } from './components/dot-content-type-copy-dialog/dot-content-type-copy-dialog.module';
import { DotContentTypesPortletComponent } from './dot-content-types.component';

import { DotBaseTypeSelectorModule } from '../../../view/components/dot-base-type-selector/dot-base-type-selector.module';
import { DotListingDataTableComponent } from '../../../view/components/dot-listing-data-table/dot-listing-data-table.component';
import { DotPortletBaseComponent } from '../../../view/components/dot-portlet-base/dot-portlet-base.component';

@NgModule({
    imports: [
        CommonModule,
        DotListingDataTableComponent,
        DotBaseTypeSelectorModule,
        DotAddToBundleComponent,
        DotAddToMenuModule,
        DotContentTypeCopyDialogModule,
        DotPortletBaseComponent
    ],
    declarations: [DotContentTypesPortletComponent],
    exports: [DotContentTypesPortletComponent],
    providers: [DotContentTypesInfoService, DotCrudService, DotContentTypeService]
})
export class DotContentTypesListingModule {}
