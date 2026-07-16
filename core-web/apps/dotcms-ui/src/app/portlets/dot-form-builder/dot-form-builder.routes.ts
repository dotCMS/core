import { Routes } from '@angular/router';

import { portletHaveLicenseResolver } from '@dotcms/ui';

import { DotFormBuilderComponent } from './dot-form-builder.component';

import { DotContentTypeEditResolver } from '../shared/dot-content-types-edit/dot-content-types-edit-resolver.service';

export const dotFormBuilderRoutes: Routes = [
    {
        component: DotFormBuilderComponent,
        path: '',
        resolve: {
            haveLicense: portletHaveLicenseResolver
        },
        data: {
            filterBy: 'FORM'
        }
    },
    {
        loadChildren: () =>
            import('../shared/dot-content-types-edit/dot-content-types-edit.module').then(
                (m) => m.DotContentTypesEditModule
            ),
        path: 'create',
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
            import('../shared/dot-content-types-edit/dot-content-types-edit.module').then(
                (m) => m.DotContentTypesEditModule
            ),
        path: 'edit/:id',
        providers: [DotContentTypeEditResolver],
        resolve: {
            contentType: DotContentTypeEditResolver
        }
    }
];
