import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotContentTypesInfoService } from '@dotcms/data-access';
import { DotContentTypesPortletComponent } from './dot-content-types.component';
import { DotCrudService } from '@dotcms/data-access';
import { DotContentTypeService } from '@dotcms/data-access';
import { DotListingDataTableModule } from '@components/dot-listing-data-table';
import { DotBaseTypeSelectorModule } from '@components/dot-base-type-selector';
import { DotAddToBundleModule } from '@components/_common/dot-add-to-bundle';
import { DotContentTypeCopyDialogModule } from '@portlets/shared/dot-content-types-listing/components/dot-content-type-copy-dialog/dot-content-type-copy-dialog.module';
import { DotAddToMenuModule } from './components/dot-add-to-menu/dot-add-to-menu.module';

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
