import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { DotPagesComponent } from './dot-pages.component';

const routes: Routes = [
    {
        component: DotPagesComponent,
        path: ''
    }
];

@NgModule({
    exports: [RouterModule],
    imports: [RouterModule.forChild(routes)]
})
export class DotPagesRoutingModule {}
