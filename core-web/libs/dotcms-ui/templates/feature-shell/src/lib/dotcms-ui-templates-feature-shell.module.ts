import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Routes } from '@angular/router';

import { LayoutEditorCanDeactivateGuardService } from '@services/guards/layout-editor-can-deactivate-guard.service';

import {
    DotcmsUiTemplatesFeatureListModule,
    DotTemplateListComponent,
    DotTemplateListResolver
} from '@dotcms/dotcms-ui/templates/feature-list';
import { DotTemplateCreateEditResolver } from '@dotcms/dotcms-ui/templates/feature-create-edit';

const routes: Routes = [
    {
        path: '',
        component: DotTemplateListComponent,
        resolve: {
            dotTemplateListResolverData: DotTemplateListResolver
        },
        data: {
            reuseRoute: false
        }
    },
    {
        path: 'new',
        loadChildren: () =>
            import('@dotcms/dotcms-ui/templates/feature-create-edit').then(
                (m) => m.DotcmsUiTemplatesFeatureCreateEditModule
            )
    },
    {
        path: 'edit/:id',
        canDeactivate: [LayoutEditorCanDeactivateGuardService],
        loadChildren: () =>
            import('@dotcms/dotcms-ui/templates/feature-create-edit').then(
                (m) => m.DotcmsUiTemplatesFeatureCreateEditModule
            ),
        resolve: {
            template: DotTemplateCreateEditResolver
        }
    },
    {
        path: 'edit/:id/inode/:inode',
        loadChildren: () =>
            import('@dotcms/dotcms-ui/templates/feature-create-edit').then(
                (m) => m.DotcmsUiTemplatesFeatureCreateEditModule
            ),
        data: {
            reuseRoute: false
        },
        resolve: {
            template: DotTemplateCreateEditResolver
        }
    }
];

@NgModule({
    imports: [
        CommonModule,
        DotcmsUiTemplatesFeatureListModule, // load here to not do 2 times lazy load
        RouterModule.forChild(routes)
    ],
    providers: [DotTemplateCreateEditResolver, DotTemplateListResolver]
})
export class DotcmsUiTemplatesFeatureShellModule {}
