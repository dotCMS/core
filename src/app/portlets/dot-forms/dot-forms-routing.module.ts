import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

const routes: Routes = [
    {
        loadChildren:
            '@portlets/shared/dot-content-types-listing/dot-content-types-listing.module#DotContentTypesListingModule',
        path: ''
    }
];

@NgModule({
    exports: [RouterModule],
    imports: [RouterModule.forChild(routes)]
})
export class DotFormsRoutingModule {}
