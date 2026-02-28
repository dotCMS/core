/* eslint-disable @nx/enforce-module-boundaries */

import { Routes } from '@angular/router';

import { DotContentTypeEditResolver } from '../shared/dot-content-types-edit/dot-content-types-edit-resolver.service';
import { DotContentTypesPortletComponent } from '../shared/dot-content-types-listing/dot-content-types.component';

export const dotContentTypesRoutes: Routes = [
    {
        component: DotContentTypesPortletComponent,
        path: ''
    },
    {
        path: 'create',
        redirectTo: '',
        pathMatch: 'full'
    },
    {
        loadChildren: () =>
            import('@portlets/shared/dot-content-types-edit/dot-content-types-edit.module').then(
                (m) => m.DotContentTypesEditModule
            ),
        path: 'create/:type',
        providers: [DotContentTypeEditResolver],
        resolve: {
            contentType: DotContentTypeEditResolver
        }
    },
    {
        path: 'edit',
        redirectTo: '',
        pathMatch: 'full'
    },
    {
        loadChildren: () =>
            import('@portlets/shared/dot-content-types-edit/dot-content-types-edit.module').then(
                (m) => m.DotContentTypesEditModule
            ),
        path: 'edit/:id',
        providers: [DotContentTypeEditResolver],
        resolve: {
            contentType: DotContentTypeEditResolver
        }
    }
];
