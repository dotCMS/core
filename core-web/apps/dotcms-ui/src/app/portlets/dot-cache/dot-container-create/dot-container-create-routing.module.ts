import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { DotContainerCreateComponent } from './dot-container-create.component';
import { DotContainerEditResolver } from './resolvers/dot-container-edit.resolver';

const routes: Routes = [
    {
        path: '',
        component: DotContainerCreateComponent
    }
];

@NgModule({
    imports: [RouterModule.forChild(routes)],
    exports: [RouterModule],
    providers: [DotContainerEditResolver]
})
export class DotContainerCreateRoutingModule {}
