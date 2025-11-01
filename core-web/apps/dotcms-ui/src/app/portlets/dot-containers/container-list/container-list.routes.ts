import { Routes } from '@angular/router';

import { ContainerListComponent } from './container-list.component';
import { DotContainerListResolver } from './dot-container-list-resolver.service';

export const containerListRoutes: Routes = [
    {
        path: '',
        component: ContainerListComponent,
        providers: [DotContainerListResolver],
        resolve: {
            dotContainerListResolverData: DotContainerListResolver
        }
    }
];
