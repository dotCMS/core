import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { DotCategoriesCreateEditComponent } from './dot-categories-create-edit.component';

const routes: Routes = [
    {
        path: '',
        component: DotCategoriesCreateEditComponent
    }
];

@NgModule({
    imports: [RouterModule.forChild(routes)],
    exports: [RouterModule]
})
export class DotCategoriesCreateEditRoutingModule {}
