import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { portletHaveLicenseResolver } from '@dotcms/ui';
import { DotContentTypeEditResolver } from '@portlets/shared/dot-content-types-edit/dot-content-types-edit-resolver.service';

import { DotFormBuilderComponent } from './dot-form-builder.component';

const routes: Routes = [
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
            import('@portlets/shared/dot-content-types-edit/dot-content-types-edit.module').then(
                (m) => m.DotContentTypesEditModule
            ),
        path: 'create',
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
