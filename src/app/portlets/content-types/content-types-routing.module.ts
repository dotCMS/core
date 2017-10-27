import { ContentTypesEditComponent } from './edit';
import { ContentTypesPortletComponent } from './main';
import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { ContentTypeResolver } from './content-types-resolver.service';

const contentTypesRoutes: Routes = [
    {
        component: ContentTypesPortletComponent,
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
            contentType: ContentTypeResolver
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
            contentType: ContentTypeResolver
        }
    }
];

@NgModule({
    exports: [
        RouterModule
    ],
    imports: [
        RouterModule.forChild(contentTypesRoutes)
    ]
})
export class ContentTypesRoutingModule { }
