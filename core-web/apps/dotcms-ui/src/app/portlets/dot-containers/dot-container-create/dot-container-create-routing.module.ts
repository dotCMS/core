import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { DotContainerCreateComponent } from './dot-container-create.component';

const routes: Routes = [
    {
        path: '',
        component: DotContainerCreateComponent
    }
];

@NgModule({
    imports: [RouterModule.forChild(routes)],
    exports: [RouterModule]
})
export class ContainerCreateRoutingModule {}
