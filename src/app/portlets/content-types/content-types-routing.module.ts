import { ContentTypesCreateComponent } from './create';
import { ContentTypesEditComponent } from './edit';
import { ContentTypesPortletComponent } from './main';
import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { RoutingPrivateAuthService } from '../../api/services/routing-private-auth-service';

const contentTypesRoutes: Routes = [
    {
        canActivate: [RoutingPrivateAuthService],
        component: ContentTypesPortletComponent,
        path: '',
    },
    {
        canActivate: [RoutingPrivateAuthService],
        path: 'create',
        redirectTo: ''
    },
    {
        canActivate: [RoutingPrivateAuthService],
        component: ContentTypesCreateComponent,
        path: 'create/:type'
    },
    {
        canActivate: [RoutingPrivateAuthService],
        path: 'edit',
        redirectTo: ''
    },
    {
        canActivate: [RoutingPrivateAuthService],
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
