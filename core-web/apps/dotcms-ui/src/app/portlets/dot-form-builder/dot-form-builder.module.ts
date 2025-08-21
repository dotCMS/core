import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { DotNotLicenseComponent } from '@dotcms/ui';

import { DotFormBuilderRoutingModule } from './dot-form-builder-routing.module';
import { DotFormBuilderComponent } from './dot-form-builder.component';

import { DotContentTypesListingModule } from '../shared/dot-content-types-listing/dot-content-types-listing.module';

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
