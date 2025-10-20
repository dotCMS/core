import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { DotCategoriesCreateEditComponent } from './dot-categories-create-edit.component';
import { DotCategoriesCreateEditStore } from './store/dot-categories-create-edit.store';

const routes: Routes = [
    {
        path: '',
        component: DotCategoriesCreateEditComponent,
        providers: [DotCategoriesCreateEditStore]
    }
];

@NgModule({
    imports: [RouterModule.forChild(routes)],
    exports: [RouterModule]
})
export class DotCategoriesCreateEditRoutingModule {}
