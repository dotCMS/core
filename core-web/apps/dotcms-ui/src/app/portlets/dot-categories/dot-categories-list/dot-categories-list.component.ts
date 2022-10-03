import { Component } from '@angular/core';
import { DotCategory } from '@dotcms/app/shared/models/dot-categories/dot-categories.model';
import { Observable } from 'rxjs';
import { DotCategoriesListState, DotCategoriesListStore } from './store/dot-categories-list.store';

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
}
