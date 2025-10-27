import { Routes } from '@angular/router';

import { DotContainerEditResolver } from './dot-container-create/resolvers/dot-container-edit.resolver';

import { DotContainersService } from '../../api/services/dot-containers/dot-containers.service';

export const dotContainersRoutes: Routes = [
    {
        path: '',
        loadChildren: () =>
            import('./container-list/container-list.routes').then((m) => m.containerListRoutes)
    },
    {
        path: 'create',
        loadChildren: () =>
            import('./dot-container-create/dot-container-create.routes').then(
                (m) => m.dotContainerCreateRoutes
            )
    },
    {
        path: 'edit/:id',
        loadChildren: () =>
            import('./dot-container-create/dot-container-create.routes').then(
                (m) => m.dotContainerCreateRoutes
            ),
        providers: [DotContainersService, DotContainerEditResolver],
        resolve: {
            container: DotContainerEditResolver
        },
        data: {
            reuseRoute: false
        },
        runGuardsAndResolvers: 'always'
    }
];
