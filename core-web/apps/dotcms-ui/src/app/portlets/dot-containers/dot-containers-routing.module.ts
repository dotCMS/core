import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

const routes: Routes = [
    {
        path: '',
        loadChildren: () =>
            import('./dot-container-list/dot-container-list.module').then(
                (m) => m.DotContainerListModule
            )
    },
    {
        path: 'create',
        loadChildren: () =>
            import('./dot-container-create/dot-container-create.module').then(
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
