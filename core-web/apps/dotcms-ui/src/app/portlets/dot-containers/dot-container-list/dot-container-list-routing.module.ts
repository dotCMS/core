import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { DotContainerListComponent } from './dot-container-list.component';

const routes: Routes = [
    {
        path: '',
        component: DotContainerListComponent
    }
];

@NgModule({
    imports: [RouterModule.forChild(routes)],
    exports: [RouterModule]
})
export class ContainerListRoutingModule {}
