import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { DotCategoriesListComponent } from './dot-categories-list.component';

const routes: Routes = [
    {
        path: '',
        component: DotCategoriesListComponent
    }
];

@NgModule({
    imports: [RouterModule.forChild(routes)],
    exports: [RouterModule]
})
export class DotCategoriesListRoutingModule {}
