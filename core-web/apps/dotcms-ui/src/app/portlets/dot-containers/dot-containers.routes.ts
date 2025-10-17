import { Routes } from '@angular/router';

import { DotContainerEditResolver } from './dot-container-create/resolvers/dot-container-edit.resolver';

export const dotContainersRoutes: Routes = [
    {
        path: '',
        loadChildren: () =>
            import('./container-list/container-list.routes').then((m) => m.containerListRoutes)
    },
    {
        path: 'create',
        loadChildren: () =>
            import('./dot-container-create/dot-container-create.module').then(
                (m) => m.DotContainerCreateModule
            )
    },
    {
        path: 'edit/:id',
        loadChildren: () =>
            import('./dot-container-create/dot-container-create.module').then(
                (m) => m.DotContainerCreateModule
            ),
        resolve: {
            container: DotContainerEditResolver
        },
        data: {
            reuseRoute: false
        },
        runGuardsAndResolvers: 'always'
    }
];
