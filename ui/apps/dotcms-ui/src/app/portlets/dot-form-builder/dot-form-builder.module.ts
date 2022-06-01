import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotFormBuilderRoutingModule } from './dot-form-builder-routing.module';
import { DotContentTypesListingModule } from '@portlets/shared/dot-content-types-listing/dot-content-types-listing.module';
import { DotFormBuilderComponent } from './dot-form-builder.component';
import { DotFormResolver } from './resolvers/dot-form-resolver.service';
import { DotUnlicensedPorletModule } from '@portlets/shared/dot-unlicensed-porlet';

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
