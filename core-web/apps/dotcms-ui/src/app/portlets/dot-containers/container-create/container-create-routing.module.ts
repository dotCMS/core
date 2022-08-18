import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { ContainerCreateComponent } from './container-create.component';

const routes: Routes = [
    {
        path: '',
        component: ContainerCreateComponent
    }
];

@NgModule({
    imports: [RouterModule.forChild(routes)],
    exports: [RouterModule]
})
export class ContainerCreateRoutingModule {}
