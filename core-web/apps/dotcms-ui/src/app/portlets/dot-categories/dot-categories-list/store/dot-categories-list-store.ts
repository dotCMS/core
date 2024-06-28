import { ComponentStore } from '@ngrx/component-store';
import { Observable } from 'rxjs';

import { Injectable } from '@angular/core';

import { LazyLoadEvent, MenuItem } from 'primeng/api';

import { map, take } from 'rxjs/operators';

import { DotCategoriesService } from '@dotcms/app/api/services/dot-categories/dot-categories.service';
import { DotMessageService, OrderDirection } from '@dotcms/data-access';
import { DotActionMenuItem, DotCategory, DotMenuItemCommandEvent } from '@dotcms/dotcms-models';
import { DataTableColumn } from '@models/data-table';

export interface DotCategoriesListState {
    categoriesBulkActions: MenuItem[];
    categoriesActions: DotActionMenuItem[];
    selectedCategories: DotCategory[];
    tableColumns: DataTableColumn[];
    categories: DotCategory[];
    categoryBreadCrumbs: MenuItem[];
    paginationPerPage: number;
    currentPage: number;
    totalRecords: number;
    sortField: string;
    sortOrder: OrderDirection;
}

@Injectable()
export class DotCategoriesListStore extends ComponentStore<DotCategoriesListState> {
    readonly vm$ = this.select((state: DotCategoriesListState) => state);
    /**
     * Get categories breadcrumbs
     * @memberof DotCategoriesListStore
     */
    readonly categoryBreadCrumbSselector$ = this.select(
        ({ categoryBreadCrumbs }: DotCategoriesListState) => {
            return {
                categoryBreadCrumbs
            };
        }
    );
    /**
     * A function that updates the state of the store.
     * @param state DotCategoryListState
     * @param selectedCategories DotCategory
     * @memberof DotCategoriesListStore
     */
    readonly updateSelectedCategories = this.updater<DotCategory[]>(
        (state: DotCategoriesListState, selectedCategories: DotCategory[]) => {
            return {
                ...state,
                selectedCategories
            };
        }
    );
    /** A function that updates the state of the store.
     * @param state DotCategoryListState
     * @param categories DotCategory[]
     * @memberof DotCategoriesListStore
     */
    readonly setCategories = this.updater<DotCategory[]>(
        (state: DotCategoriesListState, categories: DotCategory[]) => {
            return {
                ...state,
                paginationPerPage: this.categoryService.paginationPerPage,
                currentPage: this.categoryService.currentPage,
                totalRecords: this.categoryService.totalRecords,
                sortField: this.categoryService.sortField,
                sortOrder: this.categoryService.sortOrder,
                categories
            };
        }
    );
    /**
     * Add cateogry in breadcrumb
     * @memberof DotCategoriesListStore
     */
    readonly addCategoriesBreadCrumb = this.updater<MenuItem>(
        (state: DotCategoriesListState, categoryBreadCrumb: MenuItem) => {
            return {
                ...state,
                categoryBreadCrumbs: [
                    ...state.categoryBreadCrumbs,
                    { ...categoryBreadCrumb, tabindex: state.categoryBreadCrumbs.length.toString() }
                ]
            };
        }
    );
    /**
     * Update categories in store
     * @memberof DotCategoriesListStore
     */
    readonly updateCategoriesBreadCrumb = this.updater<MenuItem>(
        (state: DotCategoriesListState, categoryBreadCrumb: MenuItem) => {
            let { categoryBreadCrumbs } = this.get();
            categoryBreadCrumbs = categoryBreadCrumbs.filter(
                ({ tabindex }: MenuItem) => Number(tabindex) <= Number(categoryBreadCrumb.tabindex)
            );

            return {
                ...state,
                categoryBreadCrumbs: categoryBreadCrumbs
            };
        }
    );
    /**
     * > This function returns an observable of an array of DotCategory objects
     * @returns Observable<DotCategory[]>
     * @memberof DotCategoriesListStore
     */

    readonly getCategories = this.effect((filters: Observable<LazyLoadEvent>) => {
        return filters.pipe(
            map((filters: LazyLoadEvent) => {
                this.categoryService
                    .getCategories(filters)
                    .pipe(take(1))
                    .subscribe((items: DotCategory[]) => {
                        this.setCategories(items);
                    });
            })
        );
    });

    // EFFECTS
    /**
     * > This function returns an observable of an array of DotCategory objects
     * @returns Observable<DotCategory[]>
     * @memberof DotCategoriesListStore
     */

    readonly getChildrenCategories = this.effect((filters: Observable<LazyLoadEvent>) => {
        return filters.pipe(
            map((filters: LazyLoadEvent) => {
                this.categoryService
                    .getChildrenCategories(filters)
                    .pipe(take(1))
                    .subscribe((items: DotCategory[]) => {
                        this.setCategories(items);
                    });
            })
        );
    });

    constructor(
        private dotMessageService: DotMessageService,
        private categoryService: DotCategoriesService
    ) {
        super();
        this.setState({
            categoriesBulkActions: this.getCategoriesBulkActions(),
            categoriesActions: this.getCategoriesActions(),
            tableColumns: this.getCategoriesColumns(),
            selectedCategories: [],
            categories: [],
            categoryBreadCrumbs: [],
            currentPage: this.categoryService.currentPage,
            paginationPerPage: this.categoryService.paginationPerPage,
            totalRecords: this.categoryService.totalRecords,
            sortField: null,
            sortOrder: OrderDirection.ASC
        });
    }

    /**
     * It returns an array of objects with a label property
     * @returns An array of objects with a label property.
     */
    private getCategoriesBulkActions(): { label: string }[] {
        return [
            {
                label: this.dotMessageService.get('Add-To-Bundle')
            },
            {
                label: this.dotMessageService.get('Delete')
            }
        ];
    }

    private getCategoriesActions(): DotActionMenuItem[] {
        return [
            {
                menuItem: {
                    label: this.dotMessageService.get('Edit')
                }
            },
            {
                menuItem: {
                    label: this.dotMessageService.get('View Children'),
                    command: (event: DotMenuItemCommandEvent) => {
                        this.getChildrenCategories({
                            sortOrder: 1,
                            filters: {
                                inode: {
                                    value: event.inode,
                                    matchMode: null
                                }
                            }
                        });
                        this.addCategoriesBreadCrumb({
                            label: event.categoryName,
                            id: event.inode
                        });
                    }
                }
            },
            {
                menuItem: {
                    label: this.dotMessageService.get('Permissions')
                }
            },
            {
                menuItem: {
                    label: this.dotMessageService.get('Add-To-Bundle')
                }
            },
            {
                menuItem: {
                    label: this.dotMessageService.get('Delete')
                }
            }
        ];
    }

    /**
     * It returns an array of objects that describe the columns of the table
     * @returns An array of DataTableColumn objects.
     */
    private getCategoriesColumns(): DataTableColumn[] {
        return [
            {
                fieldName: 'categoryName',
                header: this.dotMessageService.get('message.category.fieldName.Name'),
                width: '30%',
                sortable: true
            },
            {
                fieldName: 'key',
                header: this.dotMessageService.get('message.category.fieldName.Key'),
                width: '20%',
                sortable: true
            },
            {
                fieldName: 'categoryVelocityVarName',
                header: this.dotMessageService.get('message.category.fieldName.Variable'),
                width: '20%',
                sortable: true
            },
            {
                fieldName: 'childrens',
                header: this.dotMessageService.get('message.category.fieldName.Childrens'),
                width: '15%',
                sortable: true
            },
            {
                fieldName: 'sortOrder',
                width: '10%',
                header: this.dotMessageService.get('message.category.fieldName.Order'),
                sortable: true
            },
            {
                fieldName: 'Actions',
                width: '5%',
                header: ''
            }
        ];
    }
}
