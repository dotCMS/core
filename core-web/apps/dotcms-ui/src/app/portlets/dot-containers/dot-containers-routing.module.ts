import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { DotContainerCreateEditResolver } from './dot-container-create/resolvers/dot-container-create.resolver';

const routes: Routes = [
    {
        path: '',
        loadChildren: () =>
            import('./container-list/container-list.module').then((m) => m.ContainerListModule)
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
