import { ContentTypesEditComponent } from './edit';
import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { ContentTypeEditResolver } from './edit/content-types-edit-resolver.service';

const contentTypesRoutes: Routes = [
    {
        loadChildren:
            '@portlets/shared/dot-content-types-listing/dot-content-types-listing.module#DotContentTypesListingModule',
        path: ''
    },
    {
        path: 'create',
        redirectTo: ''
    },
    {
        component: ContentTypesEditComponent,
        path: 'create/:type',
        resolve: {
            contentType: ContentTypeEditResolver
        }
    },
    {
        path: 'edit',
        redirectTo: ''
    },
    {
        component: ContentTypesEditComponent,
        path: 'edit/:id',
        resolve: {
            contentType: ContentTypeEditResolver
        }
    }
];

@NgModule({
    exports: [RouterModule],
    imports: [RouterModule.forChild(contentTypesRoutes)]
})
export class ContentTypesRoutingModule {}
