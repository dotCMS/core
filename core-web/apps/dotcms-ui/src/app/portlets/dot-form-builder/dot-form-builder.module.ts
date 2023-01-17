import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { DotContentTypesListingModule } from '@portlets/shared/dot-content-types-listing/dot-content-types-listing.module';
import { DotUnlicensedPorletModule } from '@portlets/shared/dot-unlicensed-porlet';

import { DotFormBuilderRoutingModule } from './dot-form-builder-routing.module';
import { DotFormBuilderComponent } from './dot-form-builder.component';
import { DotFormResolver } from './resolvers/dot-form-resolver.service';

@NgModule({
    declarations: [DotFormBuilderComponent],
    imports: [
        CommonModule,
        DotContentTypesListingModule,
        DotFormBuilderRoutingModule,
        DotUnlicensedPorletModule
    ],
    providers: [DotFormResolver]
})
export class DotFormBuilderModule {}
