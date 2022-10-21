import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { DotContainerCreateComponent } from './dot-container-create.component';
import { DotContainerCreateEditResolver } from './resolvers/dot-container-create.resolver';

const routes: Routes = [
    {
        path: '',
        component: DotContainerCreateComponent
    }
];

@NgModule({
    imports: [RouterModule.forChild(routes)],
    exports: [RouterModule],
    providers: [DotContainerCreateEditResolver]
})
export class DotContainerCreateRoutingModule {}
