import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';

import { dotContentTypesRoutes } from './dot-content-types.routes';

import { DotContentTypeEditResolver } from '../shared/dot-content-types-edit/dot-content-types-edit-resolver.service';
import { DotContentTypesListingModule } from '../shared/dot-content-types-listing/dot-content-types-listing.module';

@NgModule({
    imports: [
        CommonModule,
        DotContentTypesListingModule,
        RouterModule.forChild(dotContentTypesRoutes)
    ],
    providers: [DotContentTypeEditResolver]
})
export class DotContentTypesModule {}
