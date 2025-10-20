import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { DotCategoriesListComponent } from './dot-categories-list/dot-categories-list.component';
import { DotCategoriesListStore } from './dot-categories-list/store/dot-categories-list-store';

import { DotCategoriesService } from '../../api/services/dot-categories/dot-categories.service';

const routes: Routes = [
    {
        path: '',
        component: DotCategoriesListComponent,
        providers: [DotCategoriesListStore, DotCategoriesService]
    }
];

@NgModule({
    declarations: [],
    imports: [RouterModule.forChild(routes)],
    exports: [RouterModule]
})
export class DotCategoriesRoutingModule {}
