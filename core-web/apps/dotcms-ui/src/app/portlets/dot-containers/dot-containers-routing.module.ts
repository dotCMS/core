import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { DotContainerEditResolver } from './dot-container-create/resolvers/dot-container-edit.resolver';

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
            container: DotContainerEditResolver
        },
        data: {
            reuseRoute: false
        },
        runGuardsAndResolvers: 'always'
    }
];

@NgModule({
    declarations: [],
    imports: [RouterModule.forChild(routes)],
    exports: [RouterModule],
    providers: [DotContainerEditResolver]
})
export class DotContainersRoutingModule {}
