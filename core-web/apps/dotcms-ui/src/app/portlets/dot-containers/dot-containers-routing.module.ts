import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { DotContainerCreateEditResolver } from './container-create/resolvers/dot-container-create.resolver';

const routes: Routes = [
    {
        path: '',
        loadChildren: () =>
            import('./container-list/container-list.module').then((m) => m.ContainerListModule)
    },
    {
        path: 'create',
        loadChildren: () =>
            import('./container-create/container-create.module').then(
                (m) => m.ContainerCreateModule
            )
    },
    {
        path: 'edit/:id',
        loadChildren: () =>
            import('./container-create/container-create.module').then(
                (m) => m.ContainerCreateModule
            ),
        resolve: {
            container: DotContainerCreateEditResolver
        }
    }
];

@NgModule({
    declarations: [],
    imports: [RouterModule.forChild(routes)],
    exports: [RouterModule]
})
export class DotContainersRoutingModule {}
