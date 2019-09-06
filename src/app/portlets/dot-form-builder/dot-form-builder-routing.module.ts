import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { DotContentTypeEditResolver } from '@portlets/shared/dot-content-types-edit/dot-content-types-edit-resolver.service';

const routes: Routes = [
    {
        loadChildren:
            '@portlets/shared/dot-content-types-listing/dot-content-types-listing.module#DotContentTypesListingModule',
        path: ''
    },
    {
        loadChildren:
            '@portlets/shared/dot-content-types-edit/dot-content-types-edit.module#DotContentTypesEditModule',
        path: 'create',
        resolve: {
            contentType: DotContentTypeEditResolver
        }
    },
    {
        path: 'edit',
        redirectTo: ''
    },
    {
        loadChildren:
            '@portlets/shared/dot-content-types-edit/dot-content-types-edit.module#DotContentTypesEditModule',
        path: 'edit/:id',
        resolve: {
            contentType: DotContentTypeEditResolver
        }
    }
];

@NgModule({
    exports: [RouterModule],
    imports: [RouterModule.forChild(routes)],
    providers: [DotContentTypeEditResolver]
})
export class DotFormBuilderRoutingModule {}
