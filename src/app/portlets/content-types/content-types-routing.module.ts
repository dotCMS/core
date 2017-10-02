import { ContentTypesCreateComponent } from './create';
import { ContentTypesEditComponent } from './edit';
import { ContentTypesPortletComponent } from './main';
import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

const contentTypesRoutes: Routes = [
    {
        component: ContentTypesPortletComponent,
        path: '',
    },
    {
        path: 'create',
        redirectTo: ''
    },
    {
        component: ContentTypesCreateComponent,
        path: 'create/:type'
    },
    {
        path: 'edit',
        redirectTo: ''
    },
    {
        component: ContentTypesEditComponent,
        path: 'edit/:id'
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
