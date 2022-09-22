import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { CategoriesCreateEditComponent } from './categories-create-edit.component';

const routes: Routes = [
    {
        path: '',
        component: CategoriesCreateEditComponent
    }
];

@NgModule({
    imports: [RouterModule.forChild(routes)],
    exports: [RouterModule]
})
export class CategoriesCreateEditRoutingModule {}
