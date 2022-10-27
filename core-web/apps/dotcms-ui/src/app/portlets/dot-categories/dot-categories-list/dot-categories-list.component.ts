import { Component } from '@angular/core';
import { DotCategory } from '@dotcms/app/shared/models/dot-categories/dot-categories.model';
import { LazyLoadEvent } from 'primeng/api';
import { Observable } from 'rxjs';
import { DotCategoriesListStore, DotCategoriesListState } from './store/dot-categories-list-store';
@Component({
    selector: 'dot-categories-list',
    templateUrl: './dot-categories-list.component.html',
    styleUrls: ['./dot-categories-list.component.scss'],
    providers: [DotCategoriesListStore]
})
export class DotCategoriesListComponent {
    vm$: Observable<DotCategoriesListState> = this.store.vm$;
    selectedCategories: DotCategory[] = [];
    constructor(private store: DotCategoriesListStore) {}

    /**
     * update selected categories in store
     * @memberof DotCategoriesListComponent
     */
    handleRowCheck(): void {
        this.store.updateSelectedCategories(this.selectedCategories);
    }

    /**
     * get records according to pagination
     * @param {LazyLoadEvent} event
     * @memberof DotCategoriesListComponent
     */
    loadCategories(event: LazyLoadEvent) {
        this.store.getCategories(event);
    }
}
