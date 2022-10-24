import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { DotCategoriesTableComponent } from './dot-categories-table.component';

const routes: Routes = [
    {
        path: '',
        component: DotCategoriesTableComponent
    }
];

@NgModule({
    imports: [RouterModule.forChild(routes)],
    exports: [RouterModule]
})
export class DotCategoriesTableRoutingModule {}
