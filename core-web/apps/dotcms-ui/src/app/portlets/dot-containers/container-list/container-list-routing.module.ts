import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { DotContainerListResolver } from '@portlets/dot-containers/container-list/dot-container-list-resolver.service';

import { ContainerListComponent } from './container-list.component';

const routes: Routes = [
    {
        path: '',
        component: ContainerListComponent,
        resolve: {
            dotContainerListResolverData: DotContainerListResolver
        }
    }
];

@NgModule({
    imports: [RouterModule.forChild(routes)],
    exports: [RouterModule]
})
export class ContainerListRoutingModule {}
