import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

const routes: Routes = [
    {
        path: '',
        loadChildren: () =>
            import('./dot-categories-create-edit/dot-categories-create-edit.module').then(
                (m) => m.DotCategoriesCreateEditModule
            )
    }
];

@NgModule({
    declarations: [],
    imports: [RouterModule.forChild(routes)],
    exports: [RouterModule]
})
export class DotCategoriesRoutingModule {}
