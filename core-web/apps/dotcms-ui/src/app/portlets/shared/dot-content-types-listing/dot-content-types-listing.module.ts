import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import {
    DotContentTypeService,
    DotContentTypesInfoService,
    DotCrudService
} from '@dotcms/data-access';

import { DotAddToMenuModule } from './components/dot-add-to-menu/dot-add-to-menu.module';
import { DotContentTypeCopyDialogModule } from './components/dot-content-type-copy-dialog/dot-content-type-copy-dialog.module';
import { DotContentTypesPortletComponent } from './dot-content-types.component';

import { DotAddToBundleModule } from '../../../view/components/_common/dot-add-to-bundle/dot-add-to-bundle.module';
import { DotBaseTypeSelectorModule } from '../../../view/components/dot-base-type-selector/dot-base-type-selector.module';
import { DotListingDataTableModule } from '../../../view/components/dot-listing-data-table/dot-listing-data-table.module';
import { DotPortletBaseModule } from '../../../view/components/dot-portlet-base/dot-portlet-base.module';

@NgModule({
    imports: [
        CommonModule,
        DotListingDataTableModule,
        DotBaseTypeSelectorModule,
        DotAddToBundleModule,
        DotAddToMenuModule,
        DotContentTypeCopyDialogModule,
        DotPortletBaseModule
    ],
    declarations: [DotContentTypesPortletComponent],
    exports: [DotContentTypesPortletComponent],
    providers: [DotContentTypesInfoService, DotCrudService, DotContentTypeService]
})
export class DotContentTypesListingModule {}
