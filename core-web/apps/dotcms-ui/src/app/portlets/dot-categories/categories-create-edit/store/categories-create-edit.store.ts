import { Injectable } from '@angular/core';
import { ComponentStore } from '@ngrx/component-store';
import { MenuItem } from 'primeng/api';

export interface DotCategoriesCreateEditState {
    category: MenuItem;
}

@Injectable()
export class DotCategoriesCreateEditStore extends ComponentStore<DotCategoriesCreateEditState> {
    constructor() {
        super(null);
        this.setState({ category: { label: 'Top', id: '' } });
    }

    readonly vm$ = this.select(({ category }: DotCategoriesCreateEditState) => {
        return {
            category
        };
    });

    readonly updateCategory = this.updater<MenuItem>(
        (state: DotCategoriesCreateEditState, category: MenuItem) => {
            return {
                ...state,
                category
            };
        }
    );
}
