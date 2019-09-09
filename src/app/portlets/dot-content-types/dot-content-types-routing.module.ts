import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { DotContentTypeEditResolver } from '../shared/dot-content-types-edit/dot-content-types-edit-resolver.service';
import {
    DotContentTypesPortletComponent,
    DotContentTypesListingModule
} from '@portlets/shared/dot-content-types-listing';

const contentTypesRoutes: Routes = [
    {
        component: DotContentTypesPortletComponent,
        path: ''
    },
    {
        path: 'create',
        redirectTo: ''
    },
    {
        loadChildren:
            '@portlets/shared/dot-content-types-edit/dot-content-types-edit.module#DotContentTypesEditModule',
        path: 'create/:type',
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
    imports: [DotContentTypesListingModule, RouterModule.forChild(contentTypesRoutes)],
    providers: [DotContentTypeEditResolver]
})
export class DotContentTypesRoutingModule {}
