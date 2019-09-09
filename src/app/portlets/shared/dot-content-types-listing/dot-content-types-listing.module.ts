import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotContentTypesInfoService } from '@services/dot-content-types-info';
import { DotContentTypesPortletComponent } from './dot-content-types.component';
import { DotCrudService } from '@services/dot-crud';
import { DotContentTypeService } from '@services/dot-content-type';
import { DotListingDataTableModule } from '@components/dot-listing-data-table';
import { DotBaseTypeSelectorModule } from '@components/dot-base-type-selector';
import { DotPushPublishContentTypesDialogModule } from '@components/_common/dot-push-publish-dialog/dot-push-publish-dialog.module';
import { DotAddToBundleModule } from '@components/_common/dot-add-to-bundle';


@NgModule({
    imports: [
        CommonModule,
        DotListingDataTableModule,
        DotPushPublishContentTypesDialogModule,
        DotBaseTypeSelectorModule,
        DotAddToBundleModule
    ],
    declarations: [DotContentTypesPortletComponent],
    exports: [DotContentTypesPortletComponent],
    providers: [DotContentTypesInfoService, DotCrudService, DotContentTypeService]
})
export class DotContentTypesListingModule {}
