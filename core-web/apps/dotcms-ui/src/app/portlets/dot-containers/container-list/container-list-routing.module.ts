import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { ContainerListComponent } from './container-list.component';
import { DotContainerListResolver } from './dot-container-list-resolver.service';

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
