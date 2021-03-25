import { RouterModule, Routes } from '@angular/router';
import { DotGraphqlComponent } from './dot-graphql.component';
import { NgModule } from '@angular/core';

const routes: Routes = [
    {
        component: DotGraphqlComponent,
        path: ''
    }
];

@NgModule({
    imports: [RouterModule.forChild(routes)],
    exports: [RouterModule]
})
export class DotGraphqlRoutingModule {}
