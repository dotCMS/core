import { Component } from '@angular/core';
import { DotCategory } from '@dotcms/app/shared/models/dot-categories/dot-categories.model';
import { Observable } from 'rxjs';
import {
    DotCategoriesTableStore,
    DotCategoriescTableState
} from './store/dot-categories-table.store';

@Component({
    selector: 'dot-categories-table',
    templateUrl: './dot-categories-table.component.html',
    styleUrls: ['./dot-categories-table.component.scss'],
    providers: [DotCategoriesTableStore]
})
export class DotCategoriesTableComponent {
    vm$: Observable<DotCategoriescTableState> = this.store.vm$;
    selectedCategories: DotCategory[] = [];
    constructor(private store: DotCategoriesTableStore) {}

    /**
     * The function takes the value of the input field and passes it to the store
     * @param event - The event object that was triggered by the user.
     */
    onChange(event) {
        this.store.getCategories(event.target.value);
    }

    /**
     * A function that is called when the user clicks on a page number in the pagination component. It
     * calls the getCategories function in the store and passes the page number.
     * @param event - The event object that is passed to the function.
     */
    paginate(event) {
        this.store.getCategories(null, event.page + 1);
    }
}
