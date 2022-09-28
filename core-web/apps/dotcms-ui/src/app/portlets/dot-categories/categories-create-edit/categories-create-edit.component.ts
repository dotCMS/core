import { Component } from '@angular/core';
import { MenuItem } from 'primeng/api';
import { DotCategoriesCreateEditStore } from './store/categories-create-edit.store';
@Component({
    selector: 'dot-categories-create-edit-list',
    templateUrl: './categories-create-edit.component.html',
    styleUrls: ['./categories-create-edit.component.scss'],
    providers: [DotCategoriesCreateEditStore]
})
export class CategoriesCreateEditComponent {
    vm$ = this.store.vm$;
    constructor(private store: DotCategoriesCreateEditStore) {}

    updateCategory(category: MenuItem) {
        this.store.updateCategory(category);
    }
}
