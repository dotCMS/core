import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';

import { DotNotLicenseComponent } from '@dotcms/ui';

import { DotFormBuilderComponent } from './dot-form-builder.component';
import { dotFormBuilderRoutes } from './dot-form-builder.routes';

import { DotContentTypeEditResolver } from '../shared/dot-content-types-edit/dot-content-types-edit-resolver.service';
import { DotContentTypesListingModule } from '../shared/dot-content-types-listing/dot-content-types-listing.module';

@NgModule({
    declarations: [DotFormBuilderComponent],
    imports: [
        CommonModule,
        DotContentTypesListingModule,
        RouterModule.forChild(dotFormBuilderRoutes),
        DotNotLicenseComponent
    ],
    providers: [DotContentTypeEditResolver]
})
export class DotFormBuilderModule {}
