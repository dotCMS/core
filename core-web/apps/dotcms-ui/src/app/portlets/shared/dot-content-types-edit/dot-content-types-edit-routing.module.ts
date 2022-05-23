import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { DotContentTypesEditComponent } from '.';

const routes: Routes = [
    {
        component: DotContentTypesEditComponent,
        path: ''
    }
];

@NgModule({
    exports: [RouterModule],
    imports: [RouterModule.forChild(routes)]
})
export class DotContentTypesEditRoutingModule {}
