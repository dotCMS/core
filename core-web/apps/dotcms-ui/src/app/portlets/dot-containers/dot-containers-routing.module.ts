import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

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
    }
];

@NgModule({
    declarations: [],
    imports: [RouterModule.forChild(routes)],
    exports: [RouterModule]
})
export class DotContainersRoutingModule {}
