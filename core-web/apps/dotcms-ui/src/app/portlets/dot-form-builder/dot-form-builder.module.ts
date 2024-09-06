import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { DotNotLicenseComponent } from '@dotcms/ui';
import { DotContentTypesListingModule } from '@portlets/shared/dot-content-types-listing/dot-content-types-listing.module';

import { DotFormBuilderRoutingModule } from './dot-form-builder-routing.module';
import { DotFormBuilderComponent } from './dot-form-builder.component';

@NgModule({
    declarations: [DotFormBuilderComponent],
    imports: [
        CommonModule,
        DotContentTypesListingModule,
        DotFormBuilderRoutingModule,
        DotNotLicenseComponent
    ]
})
export class DotFormBuilderModule {}
