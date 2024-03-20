import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { DotBaseTypeSelectorModule } from '@components/dot-base-type-selector';
import { DotListingDataTableModule } from '@components/dot-listing-data-table';
import { DotPortletBaseModule } from '@components/dot-portlet-base/dot-portlet-base.module';
import {
    DotContentTypeService,
    DotContentTypesInfoService,
    DotCrudService
} from '@dotcms/data-access';
import { DotAddToBundleComponent } from '@dotcms/ui';
import { DotContentTypeCopyDialogModule } from '@portlets/shared/dot-content-types-listing/components/dot-content-type-copy-dialog/dot-content-type-copy-dialog.module';

import { DotAddToMenuModule } from './components/dot-add-to-menu/dot-add-to-menu.module';
import { DotContentTypesPortletComponent } from './dot-content-types.component';

@NgModule({
    imports: [
        CommonModule,
        DotListingDataTableModule,
        DotBaseTypeSelectorModule,
        DotAddToBundleComponent,
        DotAddToMenuModule,
        DotContentTypeCopyDialogModule,
        DotPortletBaseModule
    ],
    declarations: [DotContentTypesPortletComponent],
    exports: [DotContentTypesPortletComponent],
    providers: [DotContentTypesInfoService, DotCrudService, DotContentTypeService]
})
export class DotContentTypesListingModule {}
