import { Component } from '@angular/core';
import { MenuItem } from 'primeng/api';
import { DotCategoriesCreateEditStore } from './store/dot-categories-create-edit.store';
@Component({
    selector: 'dot-categories-create-edit-list',
    templateUrl: './dot-categories-create-edit.component.html',
    styleUrls: ['./dot-categories-create-edit.component.scss'],
    providers: [DotCategoriesCreateEditStore]
})
export class DotCategoriesCreateEditComponent {
    vm$ = this.store.vm$;
    constructor(private store: DotCategoriesCreateEditStore) {}

    /**
     * The function takes a category object as a parameter, and then calls the updateCategory function
     * in the store service, passing the category object as a parameter
     * @param {MenuItem} category
     * @memberof DotCategoriesCreateEditComponent
     */
    updateCategory(category: MenuItem) {
        this.store.updateCategory(category);
    }
}
