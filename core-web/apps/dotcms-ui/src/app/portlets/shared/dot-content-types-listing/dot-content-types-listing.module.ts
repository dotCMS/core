import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { DotAddToBundleModule } from '@components/_common/dot-add-to-bundle';
import { DotBaseTypeSelectorModule } from '@components/dot-base-type-selector';
import { DotListingDataTableModule } from '@components/dot-listing-data-table';
import {
    DotContentTypeService,
    DotContentTypesInfoService,
    DotCrudService
} from '@dotcms/data-access';
import { DotContentTypeCopyDialogModule } from '@portlets/shared/dot-content-types-listing/components/dot-content-type-copy-dialog/dot-content-type-copy-dialog.module';

import { DotAddToMenuModule } from './components/dot-add-to-menu/dot-add-to-menu.module';
import { DotContentTypesPortletComponent } from './dot-content-types.component';

@NgModule({
    imports: [
        CommonModule,
        DotListingDataTableModule,
        DotBaseTypeSelectorModule,
        DotAddToBundleModule,
        DotAddToMenuModule,
        DotContentTypeCopyDialogModule
    ],
    declarations: [DotContentTypesPortletComponent],
    exports: [DotContentTypesPortletComponent],
    providers: [DotContentTypesInfoService, DotCrudService, DotContentTypeService]
})
export class DotContentTypesListingModule {}
