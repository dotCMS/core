import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { DotContainerListComponent } from './dot-container-list.component';
import { DotContainerListResolver } from '@portlets/dot-containers/dot-container-list/dot-container-list-resolver.service';

const routes: Routes = [
    {
        path: '',
        component: DotContainerListComponent,
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
