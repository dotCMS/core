import { Routes } from '@angular/router';

import { CanDeactivateGuardService } from '@dotcms/data-access';

import { DotTemplateCreateEditResolver } from './dot-template-create-edit/resolvers/dot-template-create-edit.resolver';
import { DotTemplateListResolver } from './dot-template-list/dot-template-list-resolver.service';
import { DotTemplateListComponent } from './dot-template-list/dot-template-list.component';

import { DotTemplatesService } from '../../api/services/dot-templates/dot-templates.service';

export const DotTemplatesRoutes: Routes = [
    {
        path: '',
        component: DotTemplateListComponent,
        providers: [
            DotTemplatesService,
            DotTemplateListResolver,
            DotTemplateCreateEditResolver,
            CanDeactivateGuardService
        ],
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
            import('./dot-template-create-edit/dot-template-new/dot-template-new.routes').then(
                (m) => m.DotTemplateNewRoutes
            )
    },
    {
        path: 'edit/:id',
        loadComponent: () =>
            import('./dot-template-create-edit/dot-template-create-edit.component').then(
                (m) => m.DotTemplateCreateEditComponent
            ),
        providers: [DotTemplateCreateEditResolver, DotTemplatesService],
        resolve: {
            template: DotTemplateCreateEditResolver
        }
    },
    {
        path: 'edit/:id/inode/:inode',
        loadComponent: () =>
            import('./dot-template-create-edit/dot-template-create-edit.component').then(
                (m) => m.DotTemplateCreateEditComponent
            ),
        providers: [DotTemplateCreateEditResolver, DotTemplatesService],
        data: {
            reuseRoute: false
        },
        resolve: {
            template: DotTemplateCreateEditResolver
        }
    }
];
