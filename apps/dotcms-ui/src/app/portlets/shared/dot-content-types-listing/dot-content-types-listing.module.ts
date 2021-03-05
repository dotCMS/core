import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotContentTypesInfoService } from '@services/dot-content-types-info';
import { DotContentTypesPortletComponent } from './dot-content-types.component';
import { DotCrudService } from '@services/dot-crud';
import { DotContentTypeService } from '@services/dot-content-type';
import { DotListingDataTableModule } from '@components/dot-listing-data-table';
import { DotBaseTypeSelectorModule } from '@components/dot-base-type-selector';
import { DotAddToBundleModule } from '@components/_common/dot-add-to-bundle';
import { DotPipesModule } from '@pipes/dot-pipes.module';

@NgModule({
    imports: [
        CommonModule,
        DotListingDataTableModule,
        DotBaseTypeSelectorModule,
        DotAddToBundleModule,
        DotPipesModule
    ],
    declarations: [DotContentTypesPortletComponent],
    exports: [DotContentTypesPortletComponent],
    providers: [DotContentTypesInfoService, DotCrudService, DotContentTypeService]
})
export class DotContentTypesListingModule {}
